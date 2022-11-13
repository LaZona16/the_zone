package com.lazona.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.*
import android.os.Build.VERSION_CODES.S
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.lazona.core.bluetooth.BluetoothLowEnergyState
import com.lazona.core.permission.PermissionAskType
import com.lazona.core.permission.PermissionHelper
import com.lazona.databinding.ActivityMainBinding
import com.lazona.ui.connectdevice.ConnectWallAdapter
import com.lazona.ui.connectdevice.OnBluetoothOnClickListener
import java.util.*

private const val WALL_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
private const val WALL_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
private const val PERMISSIONS_CODE = 100
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE = 2
private const val SCAN_PERIOD = 10_000L


class MainActivity : AppCompatActivity(),OnBluetoothOnClickListener {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var binding: ActivityMainBinding
    private var isScanning = false
    private var userWantsToScan = false
    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    private val bluetoothLowEnergyScan: BluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private var permissionResultHandlers =
        mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()
    private val activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()
    private val discoveredDevices = mutableSetOf<String>()
    private val scanSettings: ScanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setReportDelay(0)
        .build()

    private val scanFilter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid.fromString(WALL_SERVICE_UUID)
    ).build()
    private var bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
    private lateinit var connectWallAdapter:ConnectWallAdapter
    private val wallsList:MutableList<BluetoothDevice> = mutableListOf()

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            if(!discoveredDevices.contains(result.device.address)){
                Log.d("DEVICE DETECTED", "onScanResult name=$name address= ${result.device?.address}")
                discoveredDevices.add(result.device.address)
                wallsList.add(result.device)
                connectWallAdapter.notifyDataSetChanged()
            }
            //bluetoothLifecycleState = BluetoothLowEnergyState.Connecting
            //result.device.connectGatt(this@MainActivity, false, gattCallback)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ERROR SCAN", "$errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            // TODO: timeout timer: if this callback not called - disconnect(), wait 120ms, close()

            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d("CONNECT DEVICE","Connected to $deviceAddress")

                    // TODO: bonding state

                    // recommended on UI thread https://punchthrough.com/android-ble-guide/
                    Handler(Looper.getMainLooper()).post {
                        bluetoothLifecycleState = BluetoothLowEnergyState.ConnectedDiscovering
                        gatt.discoverServices()
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("DISCONNECTED","Disconnected from $deviceAddress")
                    setConnectedGattToNull()
                    gatt.close()
                    bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
                    restartBluetoothLowEnergyLifecycle()
                }
            } else {
                // TODO: random error 133 - close() and try reconnect

                Log.e("ERROR","onConnectionStateChange status=$status deviceAddress=$deviceAddress, disconnecting")

                setConnectedGattToNull()
                gatt.close()
                bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
                restartBluetoothLowEnergyLifecycle()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d("DISCOVERED","onServicesDiscovered services.count=${gatt.services.size} status=$status")

            if (status == 129 /*GATT_INTERNAL_ERROR*/) {
                // it should be a rare case, this article recommends to disconnect:
                // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
                Log.d("ERROR:", "status=129 (GATT_INTERNAL_ERROR), disconnecting")
                gatt.disconnect()
                return
            }

            val service = gatt.getService(UUID.fromString(WALL_SERVICE_UUID)) ?: run {
                Log.d("ERROR:", "Service not found $WALL_SERVICE_UUID, disconnecting")
                gatt.disconnect()
                return
            }

            connectedGatt = gatt
            characteristicForRead = service.getCharacteristic(UUID.fromString(
                WALL_CHARACTERISTIC_UUID))
            characteristicForWrite = service.getCharacteristic(UUID.fromString(WALL_CHARACTERISTIC_UUID))
            characteristicForIndicate = service.getCharacteristic(UUID.fromString(WALL_CHARACTERISTIC_UUID))

            characteristicForIndicate?.let {
                bluetoothLifecycleState = BluetoothLowEnergyState.ConnectedSubscribing
                subscribeToIndications(it, gatt)
            } ?: run {
                Log.w("WARN:", "characteristic not found $WALL_CHARACTERISTIC_UUID")
                bluetoothLifecycleState = BluetoothLowEnergyState.Connected
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.d("onCharacteristicRead",characteristic.toString())
            if (characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
                val strValue = characteristic.value.toString(Charsets.UTF_8)
                val log = "onCharacteristicRead " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                    else -> "error $status"
                }
                //Log.d(log)
                Log.d("onCharacteristicRead",log)
                runOnUiThread {
                    Log.d("OnCharacteristicRead", strValue)
                    //textViewReadValue.text = strValue
                }
            } else {
                Log.e("onCharacteristicRead", "unknown uuid $characteristic.uuid")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
                val log: String = "onCharacteristicWrite " + when (status) {
                    BluetoothGatt.GATT_SUCCESS -> "OK"
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "not allowed"
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid length"
                    else -> "error $status"
                }
                //Log.d(log)
            } else {
                Log.e("onCharacteristicWrite", "unknown uuid $characteristic.uuid")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.d("OnCharacteristicChanged", characteristic.toString())
            if (characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
                val strValue = characteristic.value.toString(Charsets.UTF_8)
                Log.d("onCharacteristicChanged", "value=\"$strValue\"")
                runOnUiThread {
                    Log.d("OnCharacteristicChanged", strValue)
                    //textViewIndicateValue.text = strValue
                }
            } else {
                Log.d("onCharacteristicChanged", "unknown uuid $characteristic.uuid")
            }
        }

//        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
//            Log.d("OnCharacteristicChanged", characteristic.toString())
//            if (characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
//                val strValue = characteristic.value.toString(Charsets.UTF_8)
//                Log.d("onCharacteristicChanged", "value=\"$strValue\"")
//                runOnUiThread {
//                    Log.d("OnCharacteristicChanged", strValue)
//                    //textViewIndicateValue.text = strValue
//                }
//            } else {
//                Log.d("onCharacteristicChanged", "unknown uuid $characteristic.uuid")
//            }
//        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.characteristic.uuid == UUID.fromString(WALL_SERVICE_UUID)) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val value = descriptor.value
                    val isSubscribed = value.isNotEmpty() && value[0].toInt() != 0
//                    val subscriptionText = when (isSubscribed) {
//                        true -> getString(R.string.text_subscribed)
//                        false -> getString(R.string.text_not_subscribed)
//                    }
                    Log.d("onDescriptorWrite", isSubscribed.toString())
                    runOnUiThread {
                        // textViewSubscription.text = subscriptionText
                    }
                } else {
                    Log.d("ERROR", "onDescriptorWrite status=$status uuid=${descriptor.uuid} char=${descriptor.characteristic.uuid}")
                }

                // subscription processed, consider connection is ready for use
                bluetoothLifecycleState = BluetoothLowEnergyState.Connected
            } else {
                Log.d("onDescriptorWrite", "unknown uuid $descriptor.characteristic.uuid")
            }
        }
    }

    private var connectedGatt: BluetoothGatt? = null
    private var characteristicForRead: BluetoothGattCharacteristic? = null
    private var characteristicForWrite: BluetoothGattCharacteristic? = null
    private var characteristicForIndicate: BluetoothGattCharacteristic? = null
    private var bleOnOffListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> {
                    Log.d("", "onReceive: Bluetooth ON")
                    if (bluetoothLifecycleState == BluetoothLowEnergyState.Disconnected) {
                        restartBluetoothLowEnergyLifecycle()
                    }
                }
                BluetoothAdapter.STATE_OFF -> {
                    Log.d("Bluetooth","onReceive: Bluetooth OFF")
                    bluetoothLowEnergyEndLifecycle()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)

        binding = ActivityMainBinding.inflate(layoutInflater)

        connectWallAdapter = ConnectWallAdapter(wallsList,this)
        binding.rvScanDevices.adapter = connectWallAdapter
        setContentView(binding.root)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        binding.ibSearchBluetooth.setOnClickListener {
            userWantsToScan = !userWantsToScan
            if(userWantsToScan) {
                    val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                    registerReceiver(bleOnOffListener, filter)
            } else {
                unregisterReceiver(bleOnOffListener)
            }
            restartBluetoothLowEnergyLifecycle()
        }
        Log.d("INFO","MainActivity.onCreate")
    }

    override fun onDestroy() {
        bluetoothLowEnergyEndLifecycle()
        super.onDestroy()
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun subscribeToIndications(characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt) {
        val cccdUuid = UUID.fromString(WALL_CHARACTERISTIC_UUID)
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.e("ERROR", "setNotification(true) failed for ${characteristic.uuid}")
                return
            }

//            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
//            gatt.writeDescriptor(cccDescriptor)
            Log.d("Suscribe to Indications",
                gatt.writeDescriptor(cccDescriptor, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                    .toString())
        }
    }

    private fun setConnectedGattToNull() {
        connectedGatt = null
        characteristicForRead = null
        characteristicForWrite = null
        characteristicForIndicate = null
    }

    @SuppressLint("MissingPermission")
    private fun restartBluetoothLowEnergyLifecycle() {
        runOnUiThread {
            if (userWantsToScan) {
                if (connectedGatt == null) {
                    prepareAndStartBleScan()
                } else {
                    connectedGatt?.disconnect()
                }
            } else {
                bluetoothLowEnergyEndLifecycle()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun bluetoothLowEnergyEndLifecycle() {
        safeStopBleScan()
        connectedGatt?.close()
        setConnectedGattToNull()
        bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
    }

    private fun prepareAndStartBleScan() {
        ensureBluetoothCanBeUsed { isSuccess, message ->
            Log.d("Ensure Bluetooth can be used", message)
            if (isSuccess) {
                safeStartBleScan(isSuccess)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeStartBleScan(isBluetoothEnabled: Boolean) {
        if (isScanning) {
            Log.d("Ensure Bluetooth can be used", "Already scanning")
            return
        }
        when (isBluetoothEnabled) {
            true -> {
                // Stops scanning after a pre-defined scan period.
                Handler(Looper.getMainLooper()).postDelayed({
                    safeStopBleScan()
                    isScanning = false
                }, SCAN_PERIOD)
                isScanning = true
                val serviceFilter = scanFilter.serviceUuid?.uuid.toString()
                Log.d("Starting BLE", "scan, filter: $serviceFilter")
                bluetoothLifecycleState = BluetoothLowEnergyState.Scanning
                bluetoothLowEnergyScan.startScan(mutableListOf(scanFilter), scanSettings, scanCallback)
            }
            else -> {
                isScanning = false
                safeStopBleScan()
            }
        }


    }

    private fun ensureBluetoothCanBeUsed(completion: (Boolean, String) -> Unit) {
        grantBluetoothCentralPermissions(PermissionAskType.AskOnce) { isGranted ->
            if (!isGranted) {
                completion(false, "Bluetooth permissions denied")
                return@grantBluetoothCentralPermissions
            }

            enableBluetooth(PermissionAskType.AskOnce) { isEnabled ->
                if (!isEnabled) {
                    completion(false, "Bluetooth OFF")
                    return@enableBluetooth
                }

                grantLocationPermissionIfRequired(PermissionAskType.AskOnce) { isGranted ->
                    if (!isGranted) {
                        completion(false, "Location permission denied")
                        return@grantLocationPermissionIfRequired
                    }

                    completion(true, "Bluetooth ON, permissions OK, ready")
                }
            }
        }
    }

    private fun grantBluetoothCentralPermissions(
        askType: PermissionAskType,
        completion: (Boolean) -> Unit
    ) {
        if (permissionHelper.hasPermissions(permissionHelper.permissionBluetoothList)) {
            completion(true)
        } else {
            runOnUiThread {
                permissionResultHandlers[BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE] =
                    { permissions, grantResults ->
                        val isSuccess =
                            grantResults.firstOrNull() != PackageManager.PERMISSION_DENIED
                        if (isSuccess || askType != PermissionAskType.InsistUntilSuccess) {
                            permissionResultHandlers.remove(BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE)
                            completion(isSuccess)
                        } else {
                            // request again
                            requestPermissionArray(
                                permissionHelper.permissionBluetoothList,
                                BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE
                            )
                        }
                    }

                requestPermissionArray(
                    permissionHelper.permissionBluetoothList,
                    BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    private fun enableBluetooth(askType: PermissionAskType, completion: (Boolean) -> Unit) {
        if (bluetoothAdapter.isEnabled) {
            completion(true)
        } else {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val requestCode = ENABLE_BLUETOOTH_REQUEST_CODE
            val launcherBluetoothAdapter =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                    activityResultHandlers[requestCode]?.let { functionHandler ->
                        functionHandler(activityResult.resultCode)
                    } ?: runOnUiThread {
                        Log.d(
                            "ERROR",
                            "onActivityResult requestCode=$requestCode result=${activityResult.resultCode} not handled"
                        )
                    }
                }

            // set activity result handler
            activityResultHandlers[requestCode] = { result ->
                Unit
                val isSuccess = result == Activity.RESULT_OK
                if (isSuccess || askType != PermissionAskType.InsistUntilSuccess) {
                    activityResultHandlers.remove(requestCode)
                    completion(isSuccess)
                } else {
                    // start activity for the request again
                    launcherBluetoothAdapter.launch(enableBluetoothIntent)
                }
            }

            // start activity for the request
            launcherBluetoothAdapter.launch(enableBluetoothIntent)
        }
    }

    private fun grantLocationPermissionIfRequired(
        askType: PermissionAskType,
        completion: (Boolean) -> Unit
    ) {
        val wantedPermissions = permissionHelper.permissionLocationList
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            completion(true)
        }else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissionHelper.hasPermissions(wantedPermissions)) {
            completion(true)
        } else {
            runOnUiThread {
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.apply {
                    setTitle("Location and Bluetooth permissions required")
                    setMessage("Permissions are required to connect to the walls")
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        requestPermissionArray(
                            permissionHelper.permissionLocationList,
                            PERMISSIONS_CODE
                        )
                    }
                    setCancelable(false)
                }

                permissionResultHandlers[PERMISSIONS_CODE] = { permissions, grantResults ->
                    val isSuccess = grantResults.firstOrNull() != PackageManager.PERMISSION_DENIED
                    if (isSuccess || askType != PermissionAskType.InsistUntilSuccess) {
                        permissionResultHandlers.remove(PERMISSIONS_CODE)
                        completion(isSuccess)
                    } else {
                        // show motivation message again
                        dialogBuilder.create().show()
                    }
                }

                dialogBuilder.create().show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun safeStopBleScan() {
        if (!isScanning) {
            Log.d("DEVICE DISCONNECT","Already stopped")
            return
        }

        Log.d("DEVICE DISCONNECT","Stopping BLE scan")
        isScanning = false
        bluetoothLowEnergyScan.stopScan(scanCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionResultHandlers[requestCode]?.let { handler ->
            handler(permissions, grantResults)
        } ?: runOnUiThread {
            Log.d("ERROR", "onRequestPermissionsResult requestCode=$requestCode not handled")
        }
    }

    private fun requestPermissionArray(permissions: List<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), requestCode)
    }

    @SuppressLint("MissingPermission")
    override fun onClickListener(bluetoothDevice: BluetoothDevice) {
        Log.d("DEVICE_TAPPED","${bluetoothDevice.name}/${bluetoothDevice.address}")
        //bluetoothLifecycleState = BluetoothLowEnergyState.Connecting
        bluetoothDevice.connectGatt(this@MainActivity, false, gattCallback)
    }
}
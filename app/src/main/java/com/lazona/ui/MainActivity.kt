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
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lazona.core.bluetooth.BluetoothLowEnergyHelper
import com.lazona.core.bluetooth.BluetoothLowEnergyState
import com.lazona.core.permission.PermissionAskType
import com.lazona.core.permission.PermissionHelper
import com.lazona.databinding.ActivityMainBinding
import com.lazona.domain.BLERepositoryImpl
import com.lazona.presentation.ConnectWallViewModel
import com.lazona.presentation.ConnectWallViewModelFactory
import com.lazona.ui.connectdevice.ConnectWallAdapter
import com.lazona.ui.connectdevice.OnBluetoothOnClickListener
import java.util.*

private const val PERMISSIONS_CODE = 100
private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val BLUETOOTH_ALL_PERMISSIONS_REQUEST_CODE = 2


class MainActivity : AppCompatActivity(), OnBluetoothOnClickListener {

    private val viewModel by viewModels<ConnectWallViewModel> {
        ConnectWallViewModelFactory(BLERepositoryImpl(this))
    }
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var bluetoothLowEnergyHelper: BluetoothLowEnergyHelper
    private lateinit var binding: ActivityMainBinding
    private var userWantsToScan = false


    private var permissionResultHandlers =
        mutableMapOf<Int, (Array<out String>, IntArray) -> Unit>()
    private val activityResultHandlers = mutableMapOf<Int, (Int) -> Unit>()

    private lateinit var connectWallAdapter: ConnectWallAdapter
    private val wallsList: MutableList<BluetoothDevice> = mutableListOf()
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

    private var bleOnOffListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> {
                    Log.d("", "onReceive: Bluetooth ON")
                    viewModel.startScan()
                }
                BluetoothAdapter.STATE_OFF -> {
                    Log.d("Bluetooth", "onReceive: Bluetooth OFF")
                    viewModel.finishBluetoothLifeCycle()
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)

        binding = ActivityMainBinding.inflate(layoutInflater)

        connectWallAdapter = ConnectWallAdapter(wallsList, this)
        binding.rvScanDevices.adapter = connectWallAdapter
        setContentView(binding.root)

        bluetoothLowEnergyHelper = BluetoothLowEnergyHelper(this)

        binding.ibSearchBluetooth.setOnClickListener {
            userWantsToScan = !userWantsToScan
            if (userWantsToScan) {
                val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(bleOnOffListener, filter)
                prepareAndStartBleScan()
            } else {
                unregisterReceiver(bleOnOffListener)
                bluetoothLowEnergyHelper.stopBluetoothLowEnergyLifeCycle()
            }
        }
        Log.d("INFO", "MainActivity.onCreate")
    }

    override fun onDestroy() {
        viewModel.finishBluetoothLifeCycle()
        super.onDestroy()
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

    private fun prepareAndStartBleScan() {

        ensureBluetoothCanBeUsed { isSuccess, message ->
            Log.d("Ensure Bluetooth can be used", message)
            if (isSuccess) {
//                viewModel.restartBluetoothLowEnergyLifecycle()
                bluetoothLowEnergyHelper.startBluetoothLowEnergyLifeCycle()
            }
        }
    }

    private fun enableBluetooth(askType: PermissionAskType, completion: (Boolean) -> Unit) {
        if (viewModel.bluetoothAdapter.isEnabled) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            completion(true)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || permissionHelper.hasPermissions(
                wantedPermissions
            )
        ) {
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
        Log.d("DEVICE_TAPPED", "${bluetoothDevice.name}/${bluetoothDevice.address}")
        viewModel.connectToWall(bluetoothDevice)
    }
}
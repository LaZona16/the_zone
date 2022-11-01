package com.lazona.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lazona.core.parcelable
import com.lazona.databinding.ActivityMainBinding
import com.lazona.ui.connectdevice.ConnectWallAdapter

private const val BLUETOOTH_PERMISSIONS = 3
private const val LOCATION_PERMISSIONS = 2
private const val WALL_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
private const val WALL_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"

class MainActivity : AppCompatActivity() {


    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter by lazy { bluetoothManager.adapter }
    private lateinit var locationManager: LocationManager
    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>
    private lateinit var locationEnableLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: ActivityMainBinding
    private val deviceList = mutableListOf<String>()

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.parcelable(BluetoothDevice.EXTRA_DEVICE)
                    checkPermissions()
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                }
                else -> Toast.makeText(applicationContext, "Bluetooth ERROR", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d("HOLA2", result.device.toString())
            deviceList.add(result.device.name ?: "UNNAMED")
            binding.recyclerViewDevices.adapter?.notifyDataSetChanged()
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("HOLA2", "error: $errorCode")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.d("HOLA2", results.toString())
        }
    }
    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread {
                binding.searchBluetoothDevicesButton.text = if (value) "Stop Scan" else "Start Scan"
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpPermissions()
        // Register for broadcasts when a device is discovered.
        //val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        //registerReceiver(receiver, filter)
        binding.recyclerViewDevices.adapter = ConnectWallAdapter(deviceList)
        binding.searchBluetoothDevicesButton.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBluetoothScan()
            }
        }
    }

    private fun setUpPermissions() {
        bluetoothManager =
            applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        locationManager =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        bluetoothEnableLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    //  TODO Revisar la nueva forma de pedir permisos y que funcione para Android 12
                    promptEnableBluetooth()
                }
            }
        locationEnableLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                when {
                    permissions.getOrDefault(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        false
                    ) -> {
                        // Precise location access granted.
                    }
                    permissions.getOrDefault(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        false
                    ) -> {
                        // Only approximate location access granted.
                    }
                    else -> {
                        promptEnableLocation()
                    }
                }
            }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private fun promptEnableBluetooth() {
        when {
            !bluetoothAdapter.isEnabled -> {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
            }
        }
    }

    private fun promptEnableLocation() {
        locationEnableLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun checkPermissions() {
        when {
            !bluetoothAdapter.isEnabled -> promptEnableBluetooth()
            !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> promptEnableLocation()
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    LOCATION_PERMISSIONS
                )
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.BLUETOOTH_SCAN) -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH,
                        android.Manifest.permission.BLUETOOTH_SCAN,
                        android.Manifest.permission.BLUETOOTH_ADMIN
                    ),
                    BLUETOOTH_PERMISSIONS
                )
            }
            else -> startBluetoothScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        val filter = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString(WALL_SERVICE_UUID)
        ).build()
        Log.d("HOLA2", "START")
        isScanning = true
//        bleScanner.startScan(scanCallback)
//        bleScanner.startScan(null, scanSettings, scanCallback)
//        Handler(Looper.getMainLooper()).postDelayed({
//            isScanning = false
//            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
//        }, 10000)
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
//        bluetoothAdapter.stopLeScan(scanCallback)
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        isScanning = false
    }
}
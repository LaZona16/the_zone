package com.lazona.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lazona.core.Output


private const val SCAN_PERIOD = 10_000L
class BluetoothLowEnergyHelper(private val context: Context, private val bluetoothGattCallbackImpl: BluetoothGattCallbackImpl) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }

    private val bluetoothLowEnergyScan: BluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanFilter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid.fromString(WALL_SERVICE_UUID)
    ).build()

    private val scanSettings: ScanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE).setReportDelay(0).build()

    private var bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
    val deviceList: MutableList<BluetoothDevice> = mutableListOf()

    private val discoveredDevices = mutableSetOf<BluetoothDevice>()

    private val _listUpdate = MutableLiveData<Output<List<BluetoothDevice>?>>()
    val listUpdate : LiveData<Output<List<BluetoothDevice>?>>
        get() = _listUpdate

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name: String? = result.scanRecord?.deviceName ?: result.device.name
            if (!discoveredDevices.contains(result.device)) {
                Log.d(
                    "DEVICE DETECTED", "onScanResult name=$name address= ${result.device?.address}"
                )
                discoveredDevices.add(result.device)
                _listUpdate.postValue(Output.Success(discoveredDevices.toList()))
            }
//            bluetoothLifecycleState = BluetoothLowEnergyState.Connecting
            //result.device.connectGatt(this@MainActivity, false, gattCallback)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ERROR SCAN", "$errorCode")
        }
    }

    private var isScanning = MutableLiveData(false)
    private var userWantsToScan = false

    private val gattCallback = object : BluetoothGattCallback() {

    }



    fun startBluetoothLowEnergyLifeCycle() {
        userWantsToScan = true
        restartBluetoothLowEnergyLifecycle()
    }

    fun stopBluetoothLowEnergyLifeCycle() {
        userWantsToScan = false
        stopScan()
    }

    @SuppressLint("MissingPermission")
    fun restartBluetoothLowEnergyLifecycle() {
        if (userWantsToScan) {
            if (!bluetoothGattCallbackImpl.isConnectedGatt()) {
                startScan()
            } else {
                bluetoothGattCallbackImpl.disconnectConnectedGatt()
            }
        } else {
            bluetoothLowEnergyEndLifecycle()
        }
    }

    fun getBluetoothLowEnergyAdapter(): BluetoothAdapter = bluetoothAdapter

    @SuppressLint("MissingPermission")
    fun bluetoothLowEnergyEndLifecycle() {
        stopScan()
        bluetoothGattCallbackImpl.closeConnectedGatt()
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning.value == true) {
            Log.d("Ensure Bluetooth can be used", "Already scanning")
            return
        }

        // Stops scanning after a pre-defined scan period.
        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
            isScanning.postValue(false)
        }, SCAN_PERIOD)
        isScanning.postValue(true)
        val serviceFilter = scanFilter.serviceUuid?.uuid.toString()
        Log.d("Starting BLE", "scan, filter: $serviceFilter")
        bluetoothLifecycleState = BluetoothLowEnergyState.Scanning
        bluetoothLowEnergyScan.startScan(
            mutableListOf(scanFilter), scanSettings, scanCallback
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (isScanning.value == false) {
            Log.d("DEVICE DISCONNECT", "Already stopped")
            return
        }

        Log.d("DEVICE DISCONNECT", "Stopping BLE scan")
        isScanning.postValue(false)
        bluetoothLowEnergyScan.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        bluetoothDevice.connectGatt(context, false, BluetoothGattCallbackImpl())
    }

    fun sendData(data: String) {
        TODO("Not yet implemented")
    }

    fun setUserWantsToScan(userWantsToScan: Boolean) {
        this.userWantsToScan = userWantsToScan
    }

    fun isBluetoothEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    fun getLifeCycleState() = bluetoothLifecycleState
}
package com.lazona.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.lazona.core.bluetooth.BluetoothLowEnergyState
import kotlinx.coroutines.flow.MutableSharedFlow

interface BLERepository {
    val deviceList: MutableSharedFlow<List<BluetoothDevice>>
    fun startScan()
    fun stopScan()
    fun connectDevice(bluetoothDevice: BluetoothDevice)
    fun sendData(data: String)
    fun isBluetoothEnabled(): Boolean
    fun getLifeCycleState(): BluetoothLowEnergyState
    fun bluetoothLowEnergyEndLifecycle()
    fun restartBluetoothLowEnergyLifecycle()
    fun getBluetoothLowEnergyAdapter(): BluetoothAdapter
}
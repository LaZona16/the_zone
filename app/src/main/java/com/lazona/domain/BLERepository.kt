package com.lazona.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.lazona.core.bluetooth.BluetoothLowEnergyState

interface BLERepository {
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
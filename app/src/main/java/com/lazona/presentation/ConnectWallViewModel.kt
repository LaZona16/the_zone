package com.lazona.presentation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.*
import com.lazona.core.bluetooth.BluetoothLowEnergyState
import com.lazona.domain.BLERepository

class ConnectWallViewModel(private val repository: BLERepository) : ViewModel() {

    var bluetoothLifecycleState: BluetoothLowEnergyState = repository.getLifeCycleState()
    var bluetoothAdapter: BluetoothAdapter = repository.getBluetoothLowEnergyAdapter()
    fun connectToWall(bluetoothDevice: BluetoothDevice) {
        repository.connectDevice(bluetoothDevice)
    }

    fun startScan() {
        repository.startScan()
    }

    fun restartBluetoothLowEnergyLifecycle() = repository.restartBluetoothLowEnergyLifecycle()

    fun finishBluetoothLifeCycle() = repository.bluetoothLowEnergyEndLifecycle()
}
class ConnectWallViewModelFactory(private val repo: BLERepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(BLERepository::class.java).newInstance(repo)
    }
}

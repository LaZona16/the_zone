package com.lazona.presentation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.lazona.core.Output
import com.lazona.core.bluetooth.BluetoothLowEnergyState
import com.lazona.domain.BLERepository
import kotlinx.coroutines.Dispatchers

class ConnectWallViewModel(private val repository: BLERepository) : ViewModel() {

    var bluetoothLifecycleState: BluetoothLowEnergyState = repository.getLifeCycleState()
    var bluetoothAdapter: BluetoothAdapter = repository.getBluetoothLowEnergyAdapter()

    fun connectToWall(bluetoothDevice: BluetoothDevice) {
        repository.connectDevice(bluetoothDevice)
    }

    fun startScan() {
        repository.startScan()
    }

    fun restartBluetoothLowEnergyLifecycle() =
        liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
            Log.d("View Model","Trying to Scan")
            emit(Output.Loading())
            repository.restartBluetoothLowEnergyLifecycle()

           emit(Output.Success(""))

        }

    fun finishBluetoothLifeCycle() = repository.bluetoothLowEnergyEndLifecycle()

}

class ConnectWallViewModelFactory(private val repo: BLERepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(BLERepository::class.java).newInstance(repo)
    }
}

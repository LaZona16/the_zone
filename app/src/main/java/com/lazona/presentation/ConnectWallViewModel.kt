package com.lazona.presentation

import android.bluetooth.le.ScanFilter
import android.os.ParcelUuid
import androidx.lifecycle.ViewModel
import com.lazona.domain.ConnectWallRepository

private const val WALL_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
private const val WALL_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
class ConnectWallViewModel(private val repo: ConnectWallRepository) : ViewModel() {



    
}

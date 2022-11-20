package com.lazona.domain

import android.bluetooth.BluetoothDevice
import com.lazona.data.local.WallDatasource

class ConnectWallRespositoryImpl: ConnectWallRepository {
    private val listOfWallsConnected: MutableList<WallDatasource> = mutableListOf()

    override fun connectToWall(bluetoothDevice: BluetoothDevice) {

    }
}


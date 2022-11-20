package com.lazona.domain

import android.bluetooth.BluetoothDevice

interface ConnectWallRepository {
    fun connectToWall(bluetoothDevice: BluetoothDevice)
}

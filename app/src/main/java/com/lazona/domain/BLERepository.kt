package com.lazona.domain

interface BLERepository {
    fun startScan()
    fun stopScan()
    fun connectDevice()
    fun sendData(data: String)
    fun isBluetoothEnabled(): Boolean
}
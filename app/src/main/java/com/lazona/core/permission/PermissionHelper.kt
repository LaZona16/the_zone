package com.lazona.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

class PermissionHelper (val context: Context) {
    val permissionLocationList: List<String> = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val permissionBluetoothList: List<String> = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        listOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
        )
    }else{
        listOf(
            Manifest.permission.BLUETOOTH
        )
    }



    fun hasPermissions(permissions: List<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
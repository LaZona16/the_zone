package com.lazona.core

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_DENIED

class PermissionHelper (val context: Context) {
    private val permissionList: List<String> = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
        listOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }else{
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }



    fun getPermissionsUngranted(): List<String> {
        var deniedPermissionsList = mutableListOf<String>()
        if (!arePermissionsGranted()) {
             deniedPermissionsList = deniedPermissions()
        }
        return deniedPermissionsList
    }

    fun deniedPermissions(): MutableList<String> {
        val deniedPermissionsList = mutableListOf<String>()
        permissionList.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) == PERMISSION_DENIED) {
                deniedPermissionsList.add(permission)
            }
        }
        return deniedPermissionsList
    }



    fun arePermissionsGranted(): Boolean = permissionList.firstOrNull {
        ContextCompat.checkSelfPermission(context, it) == PERMISSION_DENIED
    }.isNullOrBlank()
}
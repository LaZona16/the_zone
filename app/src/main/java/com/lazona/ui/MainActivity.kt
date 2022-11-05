package com.lazona.ui

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lazona.R
import com.lazona.core.PermissionHelper
import com.lazona.databinding.ActivityMainBinding


private const val PERMISSIONS_CODE = 100

class MainActivity : AppCompatActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var binding: ActivityMainBinding
    private var isSearching = false
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val bluetoothLowEnergyScan: BluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.searchBluetoothDevicesButton.setOnClickListener {
            isSearching = true
            binding.searchBluetoothDevicesButton.text = getString(R.string.stop_scan_text)
        }
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()
        if(!permissionHelper.arePermissionsGranted()) {
            checkPermissions()
        }

    }

    private fun checkPermissions() {
        val ungrantedPermissionsList = permissionHelper.getPermissionsUngranted()
        ungrantedPermissionsList.forEach { permission ->
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSIONS_CODE)
            }

        }
    }

    // Show alert dialog to request permissions
    private fun showAlert(permission: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need permission(s)")
        builder.setMessage("Some permissions are required to do the task.")
        builder.setPositiveButton("OK", { dialog, which -> ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSIONS_CODE) })
        builder.setNeutralButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            PERMISSIONS_CODE -> if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                // Show an explanation asynchronously
                Toast.makeText(this, "This Permission is necessary for app functionality", Toast.LENGTH_SHORT).show()
                showAlert(permissions[0])
            } else {
                if(ContextCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_DENIED) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
            }
        }
    }
}
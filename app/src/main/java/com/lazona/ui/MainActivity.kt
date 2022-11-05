package com.lazona.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lazona.R
import com.lazona.core.PermissionHelper
import com.lazona.databinding.ActivityMainBinding

private const val WALL_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb"
private const val WALL_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
private const val PERMISSIONS_CODE = 100

class MainActivity : AppCompatActivity() {

    private lateinit var permissionHelper: PermissionHelper
    private lateinit var binding: ActivityMainBinding
    private var isSearching = false
    private lateinit var bluetoothManager: BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        bluetoothManager.adapter
    }
    private val bluetoothLowEnergyScan: BluetoothLeScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()
    private val filter = ScanFilter.Builder().setServiceUuid(
        ParcelUuid.fromString(WALL_SERVICE_UUID)
    ).build()
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (permissionHelper.arePermissionsGranted()
            ) {
                Log.i(
                    "ScanCallback",
                    "Found BLE device! Name: ${result.device.name ?: "Unnamed"}, address: $result.device.address"
                )
            }
            else {
                Log.e("ERROR PERMISSIONS", "PERMISSIONS NOT GRANTED")
                return
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("ERROR SCAN", "$errorCode")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            Log.e("ERROR SCAN", "$results")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchBluetoothDevicesButton.setOnClickListener {
            isSearching = !isSearching
            if(!isSearching) {
                binding.searchBluetoothDevicesButton.text = getString(R.string.stop_scan_text)
            } else {
                startBleScan()
            }

        }
        if (!permissionHelper.arePermissionsGranted()) {
            checkPermissions()
        }
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    override fun onResume() {
        super.onResume()
        if (!permissionHelper.arePermissionsGranted()) {
            checkPermissions()
        }

    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !permissionHelper.arePermissionsGranted()) {
            checkPermissions()
        } else {
            if (!permissionHelper.arePermissionsGranted()) {
                Log.e("ERROR PERMISSIONS", "PERMISSIONS NOT GRANTED")
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            bluetoothLowEnergyScan.startScan(null, scanSettings, scanCallback)
        }
    }

    private fun checkPermissions() {
        val ungrantedPermissionsList = permissionHelper.getPermissionsUngranted()
        ungrantedPermissionsList.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSIONS_CODE)
            }

        }
    }

    // Show alert dialog to request permissions
    private fun showAlert(permission: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need permission(s)")
        builder.setMessage("Some permissions are required to do the task.")
        builder.setPositiveButton(
            "OK",
            { dialog, which ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSIONS_CODE
                )
            })
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
        when (requestCode) {
            PERMISSIONS_CODE -> if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permissions[0]
                )
            ) {
                // Show an explanation asynchronously
                Toast.makeText(
                    this,
                    "This Permission is necessary for app functionality",
                    Toast.LENGTH_SHORT
                ).show()
                showAlert(permissions[0])
            } else {
                if (ContextCompat.checkSelfPermission(
                        this,
                        permissions[0]
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", packageName, null)
                    })
                }
            }
        }
    }
}
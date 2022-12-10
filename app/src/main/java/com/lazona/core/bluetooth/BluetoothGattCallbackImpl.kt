package com.lazona.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothGattCallbackImpl: BluetoothGattCallback() {

    private var characteristicForRead: BluetoothGattCharacteristic? = null
    private var characteristicForWrite: BluetoothGattCharacteristic? = null
    private var characteristicForIndicate: BluetoothGattCharacteristic? = null
    private var connectedGatt: BluetoothGatt? = null
    private var bluetoothLifecycleState: BluetoothLowEnergyState = BluetoothLowEnergyState.Disconnected

    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        // TODO: timeout timer: if this callback not called - disconnect(), wait 120ms, close()

        val deviceAddress = gatt.device.address
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("CONNECT DEVICE", "Connected to $deviceAddress")

                // TODO: bonding state

                // recommended on UI thread https://punchthrough.com/android-ble-guide/
                Handler(Looper.getMainLooper()).post {
                    bluetoothLifecycleState = BluetoothLowEnergyState.ConnectedDiscovering
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("DISCONNECTED", "Disconnected from $deviceAddress")
                setConnectedGattToNull()
                gatt.close()
                bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
                disconnectConnectedGatt()
            }
        } else {
            // TODO: random error 133 - close() and try reconnect

            Log.e(
                "ERROR",
                "onConnectionStateChange status=$status deviceAddress=$deviceAddress, disconnecting"
            )

            setConnectedGattToNull()
            gatt.close()
            bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
            disconnectConnectedGatt()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.d(
            "DISCOVERED",
            "onServicesDiscovered services.count=${gatt.services.size} status=$status"
        )

        if (status == 129 /*GATT_INTERNAL_ERROR*/) {
            // it should be a rare case, this article recommends to disconnect:
            // https://medium.com/@martijn.van.welie/making-android-ble-work-part-2-47a3cdaade07
            Log.d("ERROR:", "status=129 (GATT_INTERNAL_ERROR), disconnecting")
            gatt.disconnect()
            return
        }

        val service = gatt.getService(UUID.fromString(WALL_SERVICE_UUID)) ?: run {
            Log.d("ERROR:", "Service not found $WALL_SERVICE_UUID, disconnecting")
            gatt.disconnect()
            return
        }

        connectedGatt = gatt
        characteristicForRead = service.getCharacteristic(
            UUID.fromString(
                WALL_CHARACTERISTIC_UUID
            )
        )
        characteristicForWrite =
            service.getCharacteristic(UUID.fromString(WALL_CHARACTERISTIC_UUID))
        characteristicForIndicate =
            service.getCharacteristic(UUID.fromString(WALL_CHARACTERISTIC_UUID))
        Log.d("DEVICE_CHARACTERISTIC", characteristicForIndicate.toString())
        characteristicForIndicate?.let {
            bluetoothLifecycleState = BluetoothLowEnergyState.ConnectedSubscribing
            subscribeToIndications(it, gatt)
        } ?: run {
            Log.w("WARN:", "characteristic not found $WALL_CHARACTERISTIC_UUID")
            bluetoothLifecycleState = BluetoothLowEnergyState.Connected
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
    ) {
        if (characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
            val strValue = gatt.readCharacteristic(characteristic)
            val log = "onCharacteristicRead " + when (status) {
                BluetoothGatt.GATT_SUCCESS -> "OK, value=\"$strValue\""
                BluetoothGatt.GATT_READ_NOT_PERMITTED -> "not allowed"
                else -> "error $status"
            }
            //Log.d(log)
            Log.d("onCharacteristicRead", log)


        } else {
            Log.e("onCharacteristicRead", "unknown uuid $characteristic.uuid")
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int
    ) {
        if (characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
            val log: String = "onCharacteristicWrite " + when (status) {
                BluetoothGatt.GATT_SUCCESS -> "OK"
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "not allowed"
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "invalid length"
                else -> "error $status"
            }
            //Log.d(log)
        } else {
            Log.e("onCharacteristicWrite", "unknown uuid $characteristic.uuid")
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        Log.d("onCharacteristicChanged", "${value.size}")
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
    ) {
        if (characteristic?.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
            if (characteristic != null) {
                var strValue = ""
                for (byte in characteristic.value) {
                    strValue += " ${byte.toInt() and 0xFF}"
                }
                Log.d("WALL_CHARACTERISTIC_CHANGED", strValue)
            }
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int
    ) {
        if (descriptor.characteristic.uuid == UUID.fromString(WALL_CHARACTERISTIC_UUID)) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = descriptor.value
                val isSubscribed = value.isNotEmpty() && value[0].toInt() != 0
                Log.d("onDescriptorWrite", isSubscribed.toString())

            } else {
                Log.d(
                    "ERROR",
                    "onDescriptorWrite status=$status uuid=${descriptor.uuid} char=${descriptor.characteristic.uuid}"
                )
            }

            // subscription processed, consider connection is ready for use
            bluetoothLifecycleState = BluetoothLowEnergyState.Connected
        } else {
            Log.d("onDescriptorWrite", "unknown uuid $descriptor.characteristic.uuid")
        }
    }

    private fun setConnectedGattToNull() {
        connectedGatt = null
        characteristicForRead = null
        characteristicForWrite = null
        characteristicForIndicate = null
    }

    @SuppressLint("MissingPermission", "NewApi")
    private fun subscribeToIndications(
        characteristic: BluetoothGattCharacteristic, gatt: BluetoothGatt
    ) {
        val cccdUuid = UUID.fromString(CCC_DESCRIPTOR_UUID)
        Log.d("WALL_CHARACTERISTIC", "${characteristic.descriptors.size}")
        characteristic.getDescriptor(cccdUuid)?.let { cccDescriptor ->
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.e("ERROR", "setNotification(true) failed for ${characteristic.uuid}")
                return
            }
            cccDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccDescriptor)
        }
    }

    @SuppressLint("MissingPermission")
    fun closeConnectedGatt() {
        connectedGatt?.close()
        setConnectedGattToNull()
        bluetoothLifecycleState = BluetoothLowEnergyState.Disconnected
    }

    fun disconnectConnectedGatt() = connectedGatt?.disconnect()

    fun isConnectedGatt() = connectedGatt != null
}

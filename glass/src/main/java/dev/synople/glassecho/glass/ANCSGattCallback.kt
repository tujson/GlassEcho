package dev.synople.glassecho.glass

import android.bluetooth.*
import android.util.Log

class ANCSGattCallback(private val parser: ANCSParser) : BluetoothGattCallback() {
    private val TAG = "ANCSGattCallback"
    private var ancsService: BluetoothGattService? = null
    private var isNotificationSourceEnabled = false

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        val uuid = characteristic.uuid
        if (uuid == GattConstants.Apple.NOTIFICATION_SOURCE) {
            val data = characteristic.value
            Log.v(TAG, "Notification - Notification Source")
            parser.onNotification(data)
        } else if (uuid == GattConstants.Apple.DATA_SOURCE) {
            val data = characteristic.value
            Log.v(TAG, "Notification - Data Source")
            parser.onDSNotification(data)
        }
    }

    override fun onConnectionStateChange(
        gatt: BluetoothGatt, status: Int,
        newState: Int
    ) {
        Log.v(TAG, "onConnectionStateChange() $status $newState")
        if (newState == BluetoothProfile.STATE_CONNECTED
            && status == BluetoothGatt.GATT_SUCCESS
        ) {
            Log.v(TAG, "isDiscoveringServices: " + gatt.discoverServices())
        }
    }

    // New services discovered
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.v(TAG, "onServicesDiscovered() status=$status")
        val ancs = gatt.getService(GattConstants.Apple.ANCS)

        if (ancs == null) {
            Log.e(TAG, "ANCS not found.")
            return
        }

        val dataSourceCharacteristic = ancs.getCharacteristic(GattConstants.Apple.DATA_SOURCE)
        if (dataSourceCharacteristic == null) {
            Log.e(TAG, "Could not find Data Source characteristic")
            return
        }

        val isDataSourceSet = gatt.setCharacteristicNotification(dataSourceCharacteristic, true)
        if (!isDataSourceSet) {
            Log.e(TAG, " Failed to enable Data Source characteristic")
            return
        }

        val descriptor = dataSourceCharacteristic.getDescriptor(GattConstants.DESCRIPTOR_UUID)
        if (descriptor == null) {
            Log.e("Callback", "Could not find Data Source descriptor")
            return
        }
        val isDescriptorSetValue =
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        val isDescriptorWriteDescriptor = gatt.writeDescriptor(descriptor)
        Log.v(
            "Callback",
            "Data Source: Descriptor.setValue() $isDescriptorSetValue, writeDescriptor() $isDescriptorWriteDescriptor"
        )

        isNotificationSourceEnabled = false
        val controlPointCharacteristic = ancs.getCharacteristic(GattConstants.Apple.CONTROL_POINT)
        if (controlPointCharacteristic == null) {
            Log.e(TAG, "Could not find Control Point characteristic")
            return
        }

        ancsService = ancs
        parser.setService(ancs, gatt)
//        ANCSParser.get()?.reset()
        Log.v(TAG, "ANCS Data Source and Control Point successfully configured.")
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor, status: Int
    ) {
        Log.v(
            TAG,
            "onDescriptorWrite() " + GattConstants.getName(descriptor.uuid) + " -> "
                    + status
        )
        if (status != BluetoothGatt.GATT_SUCCESS) return

        if (ancsService != null && !isNotificationSourceEnabled) {    // set NS
            isNotificationSourceEnabled = true
            val notificationSourceCharacteristic = ancsService!!
                .getCharacteristic(GattConstants.Apple.NOTIFICATION_SOURCE)
            if (notificationSourceCharacteristic == null) {
                Log.e(TAG, "Could not find Notification Source characteristic.")
                return
            }

            val isNotificationCharacteristicSet = gatt.setCharacteristicNotification(
                notificationSourceCharacteristic, true
            )
            if (!isNotificationCharacteristicSet) {
                Log.e(TAG, "Failed to enable Notification Source characteristic.")
                return
            }

            val notificationSourceDescriptor =
                notificationSourceCharacteristic.getDescriptor(GattConstants.DESCRIPTOR_UUID)
            if (notificationSourceDescriptor == null) {
                Log.e(TAG, "Could not find Notification Source descriptor.")
            }
            val isEnableDescriptorNotification =
                notificationSourceDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            val isWriteDescriptor = gatt.writeDescriptor(notificationSourceDescriptor)

            Log.v(
                TAG,
                "Notification Source: Descriptor.setValue() $isEnableDescriptorNotification, writeDescriptor() $isWriteDescriptor"
            )
        }
    }
}
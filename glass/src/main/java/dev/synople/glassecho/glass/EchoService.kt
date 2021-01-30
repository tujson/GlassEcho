package dev.synople.glassecho.glass

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import android.util.Log
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.common.models.EchoNotification
import java.io.IOException
import java.io.ObjectInputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

private val TAG = EchoService::class.java.simpleName

class EchoService : Service() {

    private lateinit var bluetoothDevice: BluetoothDevice
    private var phoneConnection: ConnectedThread? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (phoneConnection == null || phoneConnection?.isConnected() == false) {
            intent?.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_BLUETOOTH_DEVICE)?.let {
                bluetoothDevice = it
                phoneConnection = ConnectedThread(it).apply {
                    start()
                }
            } ?: run {
                Log.v(TAG, "No BluetoothDevice found in extras")
            }
        } else {
            phoneConnection?.broadcastDeviceStatus()
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneConnection?.cancel()
    }

    inner class ConnectedThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)
        private var bluetoothSocket: BluetoothSocket? = null

        override fun run() {
            var isConnected = init()

            while (isConnected && isRunning.get()) {
                try {
                    val objectInputStream = ObjectInputStream(bluetoothSocket?.inputStream)
                    Intent().also { intent ->
                        Log.v(TAG, "Broadcasting received message")
                        intent.action = Constants.INTENT_FILTER_NOTIFICATION
                        intent.putExtra(
                            Constants.MESSAGE,
                            (objectInputStream.readObject() as EchoNotification) as Parcelable
                        )
                        sendBroadcast(intent)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Potential socket disconnect. Attempting to reconnect", e)
                    // Attempt to reconnect
                    isConnected = init()
                }
            }

            cancel()
        }

        private fun init(): Boolean {
            val isConnected: Boolean = establishConnection()?.let {
                bluetoothSocket = it
                bluetoothSocket?.isConnected
            } ?: run {
                false
            }

            broadcastDeviceStatus()

            return isConnected
        }

        private fun establishConnection(): BluetoothSocket? {
            try {
                val socket = bluetoothDevice.createRfcommSocketToServiceRecord(glassEchoUUID)
                socket.connect()
                Log.v(
                    TAG,
                    "Connection status to ${socket.remoteDevice.name}: ${socket.isConnected}"
                )
                return socket
            } catch (e: IOException) {
                Log.v(TAG, "Failed to accept", e)
            }

            return null
        }

        fun cancel() {
            try {
                bluetoothSocket?.close()
                isRunning.set(false)
                Log.v(TAG, "Cancelling ConnectedThread")
            } catch (e: IOException) {
                Log.e(TAG, "cancel", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "cancel", e)
            }
        }

        fun isConnected() = isRunning.get() && bluetoothSocket?.isConnected == true

        fun broadcastDeviceStatus() {
            Intent().also { statusIntent ->
                Log.v(TAG, "broadcastDeviceStatus: ${bluetoothSocket?.isConnected}")
                statusIntent.action = Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS
                statusIntent.putExtra(
                    Constants.EXTRA_DEVICE_IS_CONNECTED,
                    bluetoothSocket?.isConnected
                )
                if (bluetoothSocket?.isConnected == true) {
                    statusIntent.putExtra(
                        Constants.EXTRA_DEVICE_NAME,
                        bluetoothSocket?.remoteDevice?.name
                    )
                    statusIntent.putExtra(
                        Constants.EXTRA_DEVICE_ADDRESS,
                        bluetoothSocket?.remoteDevice?.address
                    )
                }
                sendBroadcast(statusIntent)
            }
        }
    }
}

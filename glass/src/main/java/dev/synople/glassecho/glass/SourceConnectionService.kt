package dev.synople.glassecho.glass

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dev.synople.glassecho.common.NOTIFICATION
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.common.models.EchoNotification
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

private val TAG = SourceConnectionService::class.java.simpleName

class SourceConnectionService : Service() {

    private lateinit var bluetoothDevice: BluetoothDevice
    private var createSocketThread: ConnectedThread? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createSocketThread?.broadcastDeviceStatus()
            ?: (intent?.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_BLUETOOTH_DEVICE)?.let {
                bluetoothDevice = it

                createSocketThread = ConnectedThread(bluetoothDevice).apply {
                    start()
                }
            } ?: run {
                Log.e(TAG, "No BluetoothDevice found in extras")
            })
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        createSocketThread?.cancel()
    }

    inner class ConnectedThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)
        var bluetoothSocket: BluetoothSocket? = null

        private var chunks = mutableListOf<String>()
        private var numChunks = -1

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            var isConnected = init()

            while (isConnected && isRunning.get()) {
                try {
                    bytes = bluetoothSocket?.inputStream?.read(buffer) ?: 0

                    val incomingMessage = String(buffer, 0, bytes)
                    Log.v(TAG, "incomingMessage: $incomingMessage")

                    if (incomingMessage.startsWith(NOTIFICATION)) {
                        numChunks = incomingMessage.substring(NOTIFICATION.length).toInt()
                    } else {
                        chunks.add(incomingMessage)

                        if (chunks.size == numChunks) {
                            val message = chunks.joinToString(separator = "")
                            Intent().also { intent ->
                                intent.action = Constants.INTENT_FILTER_NOTIFICATION
                                intent.putExtra(
                                    Constants.MESSAGE,
                                    EchoNotification.messageToEchoNotification(message)
                                )
                                sendBroadcast(intent)
                            }
                            chunks.clear()
                            numChunks = -1
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Potential socket disconnect. Attempting to reconnect", e)
                    // Attempt to reconnect
                    isConnected = init()
                }
            }
            Log.v(TAG, "$isConnected ${isRunning.get()}")

            cancel()
        }

        private fun init(): Boolean {
            val isConnected: Boolean = establishConnection()?.let {
                bluetoothSocket = it
                Log.v(TAG, "init: connected to ${bluetoothSocket?.remoteDevice?.name}")
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

        fun write(bytes: ByteArray?) {
            val text = String(bytes!!, Charset.defaultCharset())
            try {
                bluetoothSocket?.outputStream?.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "write()", e)
            }
        }

        fun cancel() {
            try {
                bluetoothSocket?.close()
                isRunning.set(false)
                Log.v(TAG, "Cancel")
            } catch (e: IOException) {
                Log.e(TAG, "cancel", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "cancel", e)
            }
        }

        fun broadcastDeviceStatus() {
            Intent().also { statusIntent ->
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

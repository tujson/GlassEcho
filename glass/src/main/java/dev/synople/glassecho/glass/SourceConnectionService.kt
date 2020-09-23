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
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.concurrent.atomic.AtomicBoolean

private val TAG = SourceConnectionService::class.java.simpleName

class SourceConnectionService : Service() {

    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var createSocketThread: ConnectedThread

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_BLUETOOTH_DEVICE)?.let {
            bluetoothDevice = it
            startConnecting()
        } ?: run {
            Log.e(TAG, "No BluetoothDevice found in extras")
        }
        return START_REDELIVER_INTENT
    }

    private fun startConnecting() {
        Log.v(TAG, "Starting to connect to ${bluetoothDevice.name}")
        if (::createSocketThread.isInitialized) {
            createSocketThread.cancel()
        }
        createSocketThread = ConnectedThread(bluetoothDevice)
        createSocketThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        createSocketThread.cancel()
    }

    inner class ConnectedThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)

        private lateinit var bluetoothSocket: BluetoothSocket

        private var chunks = mutableListOf<String>()
        private var numChunks = -1

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            var isConnected = init()

            while (isConnected && isRunning.get()) {
                try {
                    bytes = bluetoothSocket.inputStream.read(buffer)

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

            cancel()
        }

        private fun init(): Boolean {
            val isConnected: Boolean = establishConnection()?.let {
                bluetoothSocket = it
                bluetoothSocket.isConnected
            } ?: run {
                false
            }

            Intent().also { intent ->
                intent.action = Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS
                intent.putExtra(Constants.EXTRA_DEVICE_IS_CONNECTED, isConnected)
                if (isConnected) {
                    intent.putExtra(Constants.EXTRA_DEVICE_NAME, bluetoothSocket.remoteDevice.name)
                    intent.putExtra(
                        Constants.EXTRA_DEVICE_ADDRESS,
                        bluetoothSocket.remoteDevice.address
                    )
                }
                sendBroadcast(intent)
            }

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
                bluetoothSocket.outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "write()", e)
            }
        }

        fun cancel() {
            try {
                bluetoothSocket.close()
                isRunning.set(false)
            } catch (e: IOException) {
                Log.e(TAG, "cancel", e)
            }
        }
    }
}

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

private val TAG = SourceConnectionService::class.java.simpleName

class SourceConnectionService : Service() {

    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var createSocketThread: CreateSocketThread

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
        if (::createSocketThread.isInitialized) {
            createSocketThread.cancel()
        }
        createSocketThread = CreateSocketThread(bluetoothDevice)
        createSocketThread.start()
    }

    override fun onDestroy() {
        super.onDestroy()

        createSocketThread.cancel()
    }

    inner class CreateSocketThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val TAG = "AcceptThread"
        private var connectedThread: ConnectedThread? = null

        override fun run() {
            try {
                val socket = bluetoothDevice
                    .createRfcommSocketToServiceRecord(glassEchoUUID)

                socket.connect()
                connectedThread = ConnectedThread(socket)
                connectedThread?.start()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to accept", e)
            }
        }

        fun cancel() {
            connectedThread?.cancel()
        }
    }

    inner class ConnectedThread(private val bluetoothSocket: BluetoothSocket) : Thread() {
        private val TAG = "ConnectedThread"
        private var inputStream: InputStream = bluetoothSocket.inputStream
        private var outputStream: OutputStream = bluetoothSocket.outputStream

        private var chunks = mutableListOf<String>()
        private var numChunks = -1

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            Log.v(TAG, "Connected: " + bluetoothSocket.isConnected)

            Intent().also { intent ->
                intent.action = Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS
                intent.putExtra(Constants.EXTRA_DEVICE_IS_CONNECTED, bluetoothSocket.isConnected)
                sendBroadcast(intent)
            }

            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream

            // TODO: Implement reconnection logic
            while (bluetoothSocket.isConnected) {
                try {
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.v(TAG, "incomingMessage: $incomingMessage")

                    if (incomingMessage.startsWith(NOTIFICATION)) {
                        numChunks = incomingMessage.substring(NOTIFICATION.length).toInt()
                    } else {
                        chunks.add(incomingMessage)

                        if (chunks.size == numChunks) {
                            // TODO: Construct message
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
                    Log.e(TAG, "run()", e)
                    connectionClosed()
                    break
                }
            }
        }

        private fun connectionClosed() {
            this.cancel()
        }

        fun write(bytes: ByteArray?) {
            val text = String(bytes!!, Charset.defaultCharset())
            try {
                outputStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "write()", e)
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                bluetoothSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "cancel", e)
            }
        }
    }
}

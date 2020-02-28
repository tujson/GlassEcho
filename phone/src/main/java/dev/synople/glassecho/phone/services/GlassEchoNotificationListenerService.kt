package dev.synople.glassecho.phone.services

import android.app.Notification
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.synople.glassecho.common.glassEchoUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext


class GlassEchoNotificationListenerService : NotificationListenerService(), CoroutineScope {
    private val TAG = "GlassEchoService"
    private var coroutineJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var glass: ConnectedThread

    override fun onBind(intent: Intent?): IBinder? {
        Log.v(TAG, "onBind")
        runBlocking {
            getGlass()?.let {
                glass = it
            } ?: run {
                Log.e(TAG, "Couldn't connect to Glass")
            }
        }
        return super.onBind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(TAG, "onStartCommand")
        runBlocking {
            getGlass()?.let {
                glass = it
                Log.v(TAG, "Got Glass")
            } ?: run {
                Log.e(TAG, "Couldn't connect to Glass")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.v(
            TAG,
            "Content (${sbn?.packageName}): " + sbn?.notification?.extras?.get(Notification.EXTRA_TEXT).toString()
        )

        glass.write(sbn?.notification?.extras?.get(Notification.EXTRA_TEXT).toString().toByteArray())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // TODO: Retrieve from SharedPref since there could be multiple Glass connected
    private fun getGlass(): ConnectedThread? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.bondedDevices.forEach {
            if (it.name.contains("Glass")) {
                Log.v(TAG, "Trying to connect to ${it.name}")
                val socket = it.createRfcommSocketToServiceRecord(glassEchoUUID)
                try {
                    socket.connect()
                } catch (e: IOException) {
                    try {
                        socket.close()
                    } catch (e1: IOException) {
                        Log.e(TAG, "socket.close() failed", e1)
                    }
                    Log.e(TAG, "socket.connect() failed", e)
                }
                val thread = ConnectedThread(socket)
                thread.start()
                return thread
            }
        }
        return null
    }

    class ConnectedThread(private val bluetoothSocket: BluetoothSocket) : Thread() {
        private val TAG = "ConnectedThread"
        private var inputStream: InputStream = bluetoothSocket.inputStream
        private var outputStream: OutputStream = bluetoothSocket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            Log.v(TAG, "Connected: " + bluetoothSocket.isConnected)
            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.v(TAG, "incomingMessage: $incomingMessage")
//                    UiThreadStatement.runOnUiThread(Runnable { view_data.setText(incomingMessage) })
                } catch (e: IOException) {
                    Log.e(TAG, "run()", e)
                    break
                }
            }
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
package dev.synople.glassecho.phone.services

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext


class GlassEchoNotificationListenerService : NotificationListenerService(), CoroutineScope {
    private val TAG = "GlassEchoService"
    private var coroutineJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private lateinit var glass: ConnectedThread

    val uuid = UUID.fromString("f0ca1b7a-eea3-48fa-8ccb-ea6434a11de8")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runBlocking {
            getGlass()?.let {
                glass = it
            } ?: run {
                // TODO: Couldn't connect to Glass
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
            "android.textLines: " + sbn?.notification?.extras?.getString("android.textLines")
        )
        Log.v(TAG, "android.text: " + sbn?.notification?.extras?.getString("android.text"))
        Log.v(TAG, "notif toString: " + sbn?.notification?.toString())
        glass.write(null)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    // TODO: Retrieve from SharedPref since there could be multiple Glass connected
    private suspend fun getGlass(): ConnectedThread? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter.bondedDevices.forEach {
            if (it.name.contains("Glass")) {
                return ConnectedThread(it.createRfcommSocketToServiceRecord(uuid))
            }
        }
        return null
    }

    class ConnectedThread(private val bluetoothSocket: BluetoothSocket) : Thread() {
        private val TAG = "ConnectedThread"
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int
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
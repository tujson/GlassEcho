package dev.synople.glassecho.phone.services

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.common.models.echoNotificationToString
import dev.synople.glassecho.phone.MainActivity
import dev.synople.glassecho.phone.MainActivity.Companion.SHARED_PREFS
import dev.synople.glassecho.phone.R
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
    private lateinit var sharedPref: SharedPreferences

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(TAG, "onStartCommand")

        createNotificationChannel()
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        sharedPref = applicationContext.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        runBlocking {
            getGlass()?.let {
                glass = it
                Log.v(TAG, "Got Glass")
            } ?: run {
                Log.e(TAG, "Couldn't connect to Glass")
            }
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "GlassEcho Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                serviceChannel
            )
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.v(
            TAG,
            "Content (${sbn?.packageName}): " + sbn?.notification?.extras?.get(Notification.EXTRA_TEXT).toString()
        )

        if (!::sharedPref.isInitialized) {
            sharedPref = applicationContext.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        }

        if (!::glass.isInitialized) {
            getGlass()?.let {
                glass = it
                Log.v(TAG, "Got Glass")
            } ?: run {
                Log.e(TAG, "Couldn't connect to Glass")
            }
        }
        Log.v("NotificationPosted", "Notif")

        if (sharedPref.getBoolean(sbn?.packageName, false)) {
            sbn?.notification?.let {
                val appIcon = getBitmapFromDrawable(packageManager.getApplicationIcon(sbn.packageName))
                val appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(
                        sbn.packageName,
                        0
                    )
                ).toString()
                val title = it.extras.get(Notification.EXTRA_TITLE).toString()
                val text = it.extras.get(Notification.EXTRA_TEXT).toString()
                glass.write(EchoNotification(appIcon, appName, title, text))
            }
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return Bitmap.createScaledBitmap(bmp, 20, 20, false)
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

        fun write(echoNotification: EchoNotification) {
            write(echoNotificationToString(echoNotification).toByteArray())
        }

        private fun write(bytes: ByteArray?) {
            val text = String(bytes!!, Charset.defaultCharset())
            try {
                Log.v("ConnectedThread", "Write: $text")
                outputStream.write(bytes)
                outputStream.flush()
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

    companion object {
        const val CHANNEL_ID = "GlassEchoServiceChannel"
        const val ONGOING_NOTIFICATION_ID = 281
    }
}
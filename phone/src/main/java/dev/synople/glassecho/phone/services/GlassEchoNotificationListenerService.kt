package dev.synople.glassecho.phone.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.synople.glassecho.common.APP_ICON_SIZE
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.GlassEchoBroadcastReceiver
import dev.synople.glassecho.phone.MainActivity
import dev.synople.glassecho.phone.MainActivity.Companion.SHARED_PREFS
import dev.synople.glassecho.phone.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

private val TAG = GlassEchoNotificationListenerService::class.java.simpleName

class GlassEchoNotificationListenerService : NotificationListenerService(), CoroutineScope {

    private var coroutineJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    private var glass: ConnectedThread? = null
    private var sharedPref: SharedPreferences? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra(Constants.FROM_NOTIFICATION) == Constants.NOTIFICATION_ACTION_STOP) {
            stopSelf()
            stopForeground(true)

            return Service.START_NOT_STICKY
        }
        sharedPref = applicationContext.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

        showNotification()

        if (glass == null) {
            glass = ConnectedThread()
            glass?.start()
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
        glass?.cancel()
        stopSelf()
        stopForeground(true)
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val stopPendingIntent: PendingIntent =
            Intent(this, GlassEchoBroadcastReceiver::class.java).let { notificationIntent ->
                notificationIntent.putExtra(
                    Constants.FROM_NOTIFICATION,
                    Constants.NOTIFICATION_ACTION_STOP
                )
                PendingIntent.getBroadcast(
                    this,
                    16510,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val stopAction =
            NotificationCompat.Action(R.drawable.ic_stop, "Stop", stopPendingIntent)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(stopAction)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    @SuppressLint("WrongConstant") // Android Studio is incorrectly complaining...
    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "GlassEcho Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            serviceChannel
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.v(
            TAG,
            "Content (${sbn?.packageName}): " + sbn?.notification?.extras?.get(Notification.EXTRA_TEXT)
                .toString()
        )

        if (sharedPref?.getBoolean(sbn?.packageName, false) == true) {
            sbn?.let {
                glass?.write(convertNotificationToEcho(it))
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sharedPref?.getBoolean(sbn?.packageName, false) == true) {
            sbn?.let {
                glass?.write(convertNotificationToEcho(it, isRemoved = true))
            }
        }
    }

    private fun convertNotificationToEcho(
        sbn: StatusBarNotification,
        isRemoved: Boolean = false
    ): EchoNotification {
        val id = sbn.packageName + sbn.id
        val appIcon =
            getBitmapFromDrawable(packageManager.getApplicationIcon(sbn.packageName))
        val appName = packageManager.getApplicationLabel(
            packageManager.getApplicationInfo(
                sbn.packageName,
                0
            )
        ).toString()
        val title = sbn.notification.extras.get(Notification.EXTRA_TITLE).toString()
        val text = sbn.notification.extras.get(Notification.EXTRA_TEXT).toString()
        val largeIcon = sbn.notification.getLargeIcon()?.let {
            getBitmapFromDrawable(it.loadDrawable(this))
        } ?: run {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val actions = mutableListOf<String>()
        sbn.notification.actions?.forEach {
            actions.add(it.title.toString())
        }

        return EchoNotification(
            id,
            appIcon,
            appName,
            largeIcon,
            title,
            text,
            actions,
            isRemoved = isRemoved,
        )
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
        return Bitmap.createScaledBitmap(bmp, APP_ICON_SIZE, APP_ICON_SIZE, false)
    }

    inner class ConnectedThread : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)
        private var bluetoothSocket: BluetoothSocket? = null

        override fun run() {
            val buffer = ByteArray(1024)

            bluetoothSocket = establishConnection()
            Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")

            while (bluetoothSocket?.isConnected == true && isRunning.get()) {
                try {
                    bluetoothSocket?.inputStream?.let { inputStream ->
                        val bytes = inputStream.read(buffer)
                        val incomingMessage = String(buffer, 0, bytes)
                        Log.v(TAG, "incomingMessage: $incomingMessage")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "run()", e)

                    // Attempt to reconnect
                    Log.v(TAG, "Attempting to reconnect")
                    bluetoothSocket = establishConnection()
                    Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")
                }
            }

            cancel()
        }

        private fun establishConnection(): BluetoothSocket? {
            val serverSocket = BluetoothAdapter.getDefaultAdapter()
                .listenUsingRfcommWithServiceRecord("dev.synople.glassecho", glassEchoUUID)
            val socket = try {
                serverSocket.accept()
            } catch (e: IOException) {
                Log.e(TAG, "socket.connect() failed", e)
                return null
            }
            serverSocket.close()
            return socket
        }

        fun write(echoNotification: EchoNotification) {
            try {
                bluetoothSocket?.let {
                    val objectOutputStream = ObjectOutputStream(it.outputStream)
                    objectOutputStream.writeObject(echoNotification)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Write", e)
            }
        }

        fun cancel() {
            try {
                isRunning.set(false)
                bluetoothSocket?.close()
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
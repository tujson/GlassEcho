package dev.synople.glassecho.phone.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dev.synople.glassecho.common.APP_ICON_SIZE
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.common.models.EchoNotificationAction
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.Database
import dev.synople.glassecho.phone.GlassEchoBroadcastReceiver
import dev.synople.glassecho.phone.MainActivity
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.models.EchoApp
import dev.synople.glassecho.phone.models.EchoAppQueries
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private val TAG = EchoNotificationListenerService::class.java.simpleName

class EchoNotificationListenerService : NotificationListenerService() {

    private var glassConnection: ConnectedThread? = null
    private var echoAppQueries: EchoAppQueries? = null
    private val notifications: MutableMap<String, StatusBarNotification> = mutableMapOf()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getStringExtra(Constants.FROM_NOTIFICATION) == Constants.NOTIFICATION_ACTION_STOP) {
            stopForeground(true)
            stopSelf()

            return Service.START_NOT_STICKY
        }

        val driver =
            AndroidSqliteDriver(Database.Schema, applicationContext, Constants.DATABASE_ECHO_APP)
        val echoAppsDatabase = Database(driver)
        echoAppQueries = echoAppsDatabase.echoAppQueries

        showNotification()

        if (glassConnection == null) {
            glassConnection = ConnectedThread()
            glassConnection?.start()
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        glassConnection?.cancel()
        notifications.clear()

        stopForeground(true)
        stopSelf()
    }

    private fun showNotification() {
        createNotificationChannel()

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)
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
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val stopAction =
            NotificationCompat.Action(R.drawable.ic_stop, "Stop", stopPendingIntent)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_notif_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(stopAction)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "GlassEcho Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for GlassEcho"
            }

            getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                serviceChannel
            )
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (glassConnection?.isConnected() == true && isWantedNotification(sbn)) {
            val echoNotification = convertNotificationToEcho(sbn)
            notifications[echoNotification.id] = sbn
            glassConnection?.write(convertNotificationToEcho(sbn))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (glassConnection?.isConnected() == true && isWantedNotification(sbn)) {
            val echoNotification = convertNotificationToEcho(sbn, isRemoved = true)
            notifications.remove(echoNotification.id)
            glassConnection?.write(echoNotification)
        }
    }

    private fun isWantedNotification(sbn: StatusBarNotification): Boolean {
        if (sbn.notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0
            || sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0
            || sbn.notification.flags and Notification.FLAG_LOCAL_ONLY != 0
            || sbn.notification.flags and NotificationCompat.FLAG_GROUP_SUMMARY != 0
        ) {
            return false
        }

        echoAppQueries?.getEchoApp(sbn.packageName)?.executeAsOneOrNull()?.let {
            if (!it.isNotify) return@isWantedNotification false
        } ?: run {
            echoAppQueries?.insert(
                EchoApp(
                    sbn.packageName,
                    Constants.IS_NOTIFY_DEFAULT,
                    Constants.IS_WAKE_SCREEN_DEFAULT
                )
            )

            if (!Constants.IS_NOTIFY_DEFAULT) return false
        }

        // Because Facebook Messenger is weird.
        if (sbn.packageName == "com.facebook.orca" && sbn.id == 10012 && sbn.notification.tickerText == null) {
            return false
        }

        // Apparently low battery is repeatedly shown
        if (sbn.packageName == "com.android.systemui" && sbn.tag == "low_battery") {
            return false
        }

        // No need to show our service
        if (sbn.packageName == "dev.synople.glassecho.phone" && sbn.isOngoing) {
            return false
        }

        return true
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
            getBitmapFromDrawable(it.loadDrawable(this)!!)
        } ?: run {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val actions = mutableListOf<String>()
        sbn.notification.actions?.forEach {
            actions.add(it.title.toString())
        }

        val isWakeScreen =
            echoAppQueries?.getEchoApp(sbn.packageName)?.executeAsOneOrNull()?.isWakeScreen
                ?: Constants.IS_WAKE_SCREEN_DEFAULT

        return EchoNotification(
            id,
            appIcon,
            appName,
            largeIcon,
            title,
            text,
            actions,
            isWakeScreen = isWakeScreen,
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

    private fun handleNotificationAction(action: EchoNotificationAction) {
        val sbn = notifications[action.id]

        if (sbn == null) {
            Log.e(TAG, "Notification not found: ${action.id}")
            return
        }

        if (action.isDismiss) {
            cancelNotification(sbn.key)
            return
        }

        sbn.notification.actions?.forEach {
            if (it.title == action.actionName) {
                if (!action.remoteInput.isNullOrEmpty()) {
                    handleNotificationReply(it, action.remoteInput.toString())
                } else {
                    it.actionIntent.send()
                }
            }
        }
    }

    private fun handleNotificationReply(action: Notification.Action, reply: String) {
        val intent = Intent()
        val bundle = Bundle()
        val replyInputs = mutableListOf<RemoteInput>()

        action.remoteInputs.forEach {
            bundle.putCharSequence(it.resultKey, reply)

            val builder = RemoteInput.Builder(it.resultKey).apply {
                setLabel(it.resultKey)
                setChoices(it.choices)
                setAllowFreeFormInput(it.allowFreeFormInput)
                addExtras(it.extras)
            }.build()
            replyInputs.add(builder)
        }

        RemoteInput.addResultsToIntent(replyInputs.toTypedArray(), intent, bundle)
        action.actionIntent.send(applicationContext, 0, intent)
    }

    inner class ConnectedThread : Thread() {
        private val TAG = "ConnectedThread"
        private val isRunning = AtomicBoolean(true)
        private var bluetoothSocket: BluetoothSocket? = null

        override fun run() {
            bluetoothSocket = establishConnection()
            Log.v(TAG, "bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}")

            // TODO: On connect, should send over existing notifications retrieved from getActiveNotifications()

            while (bluetoothSocket?.isConnected == true && isRunning.get()) {
                try {
                    val objectInputStream = ObjectInputStream(bluetoothSocket?.inputStream)
                    val message = objectInputStream.readObject()

                    if (message is EchoNotificationAction) {
                        handleNotificationAction(message)
                    } else {
                        Log.v(TAG, "Received unknown message: $message")
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

        fun write(message: Serializable) {
            thread {
                if (isRunning.get()) {
                    try {
                        bluetoothSocket?.let {
                            val objectOutputStream = ObjectOutputStream(it.outputStream)
                            objectOutputStream.writeObject(message)
                        }
                    } catch (e: IOException) {
                        Log.e(
                            TAG,
                            "Write (bluetoothSocket isConnected: ${bluetoothSocket?.isConnected})",
                            e
                        )
                        bluetoothSocket = establishConnection()
                    }
                }
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

        fun isConnected() = isRunning.get() && bluetoothSocket?.isConnected == true
    }

    companion object {
        const val CHANNEL_ID = "GlassEcho Service"
        const val ONGOING_NOTIFICATION_ID = 281
    }
}
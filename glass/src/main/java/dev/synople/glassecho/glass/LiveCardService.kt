package dev.synople.glassecho.glass

import android.app.*
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import com.google.android.glass.timeline.LiveCard
import com.google.android.glass.timeline.LiveCard.PublishMode
import dev.synople.glassecho.common.*
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.LiveCardMenuActivity.Companion.CONNECT
import dev.synople.glassecho.glass.LiveCardMenuActivity.Companion.UNPUBLISH_LIVE_CARD
import java.io.IOException
import kotlin.concurrent.thread

class LiveCardService : Service() {

    private var liveCard: LiveCard? = null
    private lateinit var remoteViews: RemoteViews
    private lateinit var broadcastReceiver: BroadcastReceiver
    private val notifListener: ANCSParser.NotificationListener =
        object : ANCSParser.NotificationListener {
            override fun onNotificationAdd(notif: IOSNotification?) {
                Log.v(
                    "GlassEcho",
                    "iOS notification added: ${notif?.title}\n${notif?.subtitle}\n${notif?.message}"
                )
                Intent().also { intent ->
                    intent.action = MESSAGE
                    intent.putExtra(MESSAGE, notif)
                    sendBroadcast(intent)
                }
            }

            // TODO: Implement
            override fun onNotificationRemove(uid: Int) {
                Log.v("GlassEcho", "iOS notification removed $uid")
            }
        }
    private var acceptThread = AcceptThread(notifListener)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (liveCard == null) {
            liveCard = LiveCard(this, LIVE_CARD_TAG)

            remoteViews = RemoteViews(packageName, R.layout.live_card)
            liveCard!!.setViews(remoteViews)

            remoteViews.setTextViewText(R.id.tvText, "GlassEcho\nStatus: Not connected")

            val menuIntent = Intent(this, LiveCardMenuActivity::class.java)
            liveCard!!.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0))
            liveCard!!.publish(PublishMode.REVEAL)

            broadcastReceiver = MyBroadcastReceiver()
            registerReceiver(broadcastReceiver, IntentFilter(CONNECT))
            registerReceiver(broadcastReceiver, IntentFilter(UNPUBLISH_LIVE_CARD))
            registerReceiver(broadcastReceiver, IntentFilter(MESSAGE))
            registerReceiver(broadcastReceiver, IntentFilter(STATUS_MESSAGE))

            thread {
                startConnecting()
            }
        } else {
            liveCard!!.navigate()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (liveCard != null && liveCard!!.isPublished) {
            liveCard!!.unpublish()
            liveCard = null
        }

        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private fun processMessage(notif: IOSNotification) {
        val audio =
            this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.playSoundEffect(GLASS_SOUND_TAP)

        remoteViews.setTextViewText(R.id.tvAppName, notif.date)
        remoteViews.setTextViewText(R.id.tvTitle, notif.title)
        remoteViews.setTextViewText(R.id.tvText, notif.message)

        liveCard?.setViews(remoteViews)
        liveCard?.navigate()
    }

    private fun processStatusMessage(message: String) {
        Log.v(LIVE_CARD_TAG, "processStatusMessage: $message")

        val audio =
            this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.playSoundEffect(GLASS_SOUND_SUCCESS)

        remoteViews.setTextViewText(R.id.tvText, message)
        liveCard?.setViews(remoteViews)
        liveCard?.navigate()
    }

    private fun startConnecting() {
        remoteViews.setImageViewBitmap(
            R.id.ivAppIcon,
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        )
        remoteViews.setTextViewText(R.id.tvAppName, "")
        remoteViews.setTextViewText(R.id.tvTitle, "")
        remoteViews.setTextViewText(R.id.tvText, "GlassEcho\nStatus: Connecting")
        liveCard?.setViews(remoteViews)

        thread {
            acceptThread = AcceptThread(notifListener)
            acceptThread.start()
        }
    }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.v("BroadcastReceiver", "Received")
            intent?.let { it ->
                when (it.action) {
                    CONNECT -> {
                        startConnecting()
                    }
                    UNPUBLISH_LIVE_CARD -> {
                        onDestroy()
                    }
                    MESSAGE -> {
                        processMessage(it.getParcelableExtra<IOSNotification>(MESSAGE))
                    }
                    STATUS_MESSAGE -> {
                        processStatusMessage(it.getStringExtra(STATUS_MESSAGE))
                    }
                    else -> {
                        // Do nothing. Unsupported.
                    }
                }
            }
        }
    }

    inner class AcceptThread(private val notifListener: ANCSParser.NotificationListener) :
        Thread() {
        private val TAG = "AcceptThread"

        override fun run() {
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val device = adapter.bondedDevices.first()
                Log.v(TAG, "Connected to ${device.name}")

                val parser = ANCSParser(applicationContext)
                parser.addNotificationListener(notifListener)
                device.connectGatt(applicationContext, true, ANCSGattCallback(parser))
            } catch (e: IOException) {
                Log.e(TAG, "Failed to accept", e)
            }
        }
    }

    companion object {
        const val MESSAGE = "message"
        const val STATUS_MESSAGE = "statusMessage"
        private const val LIVE_CARD_TAG = "LiveCardService"
    }
}

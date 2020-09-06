package dev.synople.glassecho.glass

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
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
import dev.synople.glassecho.common.models.messageToEchoNotification
import dev.synople.glassecho.glass.LiveCardMenuActivity.Companion.CONNECT
import dev.synople.glassecho.glass.LiveCardMenuActivity.Companion.UNPUBLISH_LIVE_CARD
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.NullPointerException
import java.nio.charset.Charset
import kotlin.concurrent.thread

class LiveCardService : Service() {

    private var liveCard: LiveCard? = null
    private lateinit var remoteViews: RemoteViews
    private lateinit var broadcastReceiver: BroadcastReceiver
    private var acceptThread = AcceptThread()

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

        acceptThread.cancel()

        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    companion object {
        const val MESSAGE = "message"
        const val STATUS_MESSAGE = "statusMessage"
        private const val LIVE_CARD_TAG = "LiveCardService"
    }

    private fun processMessage(receivedMessage: String) {
        val audio =
            this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.playSoundEffect(GLASS_SOUND_TAP)

        val message = messageToEchoNotification(receivedMessage)
        remoteViews.setImageViewBitmap(R.id.ivAppIcon, message.appIcon)
        remoteViews.setTextViewText(R.id.tvAppName, message.appName)
        remoteViews.setTextViewText(R.id.tvTitle, message.title)
        remoteViews.setTextViewText(R.id.tvText, message.text)

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
            acceptThread.cancel()
            acceptThread = AcceptThread()
            acceptThread.start()
        }
    }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { it ->
                when (it.action) {
                    CONNECT -> {
                        startConnecting()
                    }
                    UNPUBLISH_LIVE_CARD -> {
                        onDestroy()
                    }
                    MESSAGE -> {
                        processMessage(it.getStringExtra(MESSAGE))
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

    inner class AcceptThread() : Thread() {
        private val TAG = "AcceptThread"
        private var connectedThread: ConnectedThread? = null

        override fun run() {
            try {
                val bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter()
                    .listenUsingRfcommWithServiceRecord("dev.synople.glassecho", glassEchoUUID)

                val socket = bluetoothServerSocket.accept()
                bluetoothServerSocket.close()
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
                intent.action = STATUS_MESSAGE
                intent.putExtra(STATUS_MESSAGE, "GlassEcho\nStatus: Connected")
                sendBroadcast(intent)
            }

            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream

            while (true) {
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
                                intent.action = MESSAGE
                                intent.putExtra(MESSAGE, message)
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
            startConnecting()
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

package dev.synople.glassecho.glass

import android.app.Activity
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.google.android.glass.timeline.LiveCard
import com.google.android.glass.timeline.LiveCard.PublishMode
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.glass.LiveCardMenuActivity.Companion.CONNECT
import dev.synople.glassecho.glass.LiveCardMenuActivity.Companion.UNPUBLISH_LIVE_CARD
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.concurrent.thread

class LiveCardService : Service() {

    private var liveCard: LiveCard? = null
    private lateinit var remoteViews: RemoteViews
    private lateinit var broadcastReceiver: BroadcastReceiver
    private val acceptThread = AcceptThread()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (liveCard == null) {
            liveCard = LiveCard(this, LIVE_CARD_TAG)

            remoteViews = RemoteViews(packageName, R.layout.live_card)
            liveCard!!.setViews(remoteViews)

            remoteViews.setTextViewText(R.id.tvTempo, "GlassEcho\nStatus: Not connected")

            val menuIntent = Intent(this, LiveCardMenuActivity::class.java)
            liveCard!!.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0))
            liveCard!!.publish(PublishMode.REVEAL)

            broadcastReceiver = MyBroadcastReceiver()
            registerReceiver(broadcastReceiver, IntentFilter(CONNECT))
            registerReceiver(broadcastReceiver, IntentFilter(UNPUBLISH_LIVE_CARD))
            registerReceiver(broadcastReceiver, IntentFilter(MESSAGE))

            thread {
                acceptThread.start()
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

    companion object {
        const val MESSAGE = "message"
        private const val LIVE_CARD_TAG = "LiveCardService"
    }

    private fun processMessage(message: String) {
        Log.v(LIVE_CARD_TAG, "processMessage: $message")

        val audio =
            this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.playSoundEffect(13)

        remoteViews.setTextViewText(R.id.tvTempo, message)
        liveCard?.setViews(remoteViews)
    }

    private fun startConnecting() {
        remoteViews.setTextViewText(R.id.tvTempo, "GlassEcho\nStatus: Connecting")
        liveCard?.setViews(remoteViews)

        thread {
            val acceptThread = AcceptThread()
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
                    else -> {
                        // Do nothing. Unsupported.
                    }
                }
            }
        }
    }

    inner class AcceptThread() : Thread() {
        private val TAG = "AcceptThread"
        override fun run() {
            try {
                val bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter()
                    .listenUsingRfcommWithServiceRecord("dev.synople.glassecho", glassEchoUUID)

                val socket = bluetoothServerSocket.accept()
                val connectedThread = ConnectedThread(socket)
                connectedThread.start()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to accept", e)
            }
        }
    }

    inner class ConnectedThread(private val bluetoothSocket: BluetoothSocket) : Thread() {
        private val TAG = "ConnectedThread"
        private var inputStream: InputStream = bluetoothSocket.inputStream
        private var outputStream: OutputStream = bluetoothSocket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            Log.v(TAG, "Connected: " + bluetoothSocket.isConnected)

            Intent().also { intent ->
                intent.action = MESSAGE
                intent.putExtra(MESSAGE, "GlassEcho\nStatus: Connected")
                sendBroadcast(intent)
            }

            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.v(TAG, "incomingMessage: $incomingMessage")

                    Intent().also { intent ->
                        intent.action = MESSAGE
                        intent.putExtra(MESSAGE, incomingMessage)
                        sendBroadcast(intent)
                    }

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

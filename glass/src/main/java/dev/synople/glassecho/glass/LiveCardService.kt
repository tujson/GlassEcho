package dev.synople.glassecho.glass

import com.google.android.glass.timeline.LiveCard
import com.google.android.glass.timeline.LiveCard.PublishMode

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import dev.synople.glassecho.common.glassEchoUUID
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import kotlin.concurrent.thread

class LiveCardService : Service() {

    private var liveCard: LiveCard? = null
    private lateinit var remoteViews: RemoteViews
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (liveCard == null) {
            liveCard = LiveCard(this, LIVE_CARD_TAG)

            remoteViews = RemoteViews(packageName, R.layout.live_card)
            liveCard!!.setViews(remoteViews)

            remoteViews.setTextViewText(R.id.tvTempo, message)

            val menuIntent = Intent(this, LiveCardMenuActivity::class.java)
            liveCard!!.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0))
            liveCard!!.publish(PublishMode.REVEAL)

            broadcastReceiver = MyBroadcastReceiver()
            val filter = IntentFilter(CONFIG_CHANGE)
            registerReceiver(broadcastReceiver, filter)

            // TODO: Executor? Make sure only one of these exist.
            thread {
                val acceptThread = AcceptThread()
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
        const val CONFIG_CHANGE = "dev.synople.glassecho.CONFIG_CHANGE"
        private const val LIVE_CARD_TAG = "LiveCardService"
        var message = "Hello, world!"
    }

    class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            (context as LiveCardService).remoteViews.setTextViewText(R.id.tvTempo, message)
            context.liveCard?.setViews(context.remoteViews)
        }
    }

    inner class AcceptThread(): Thread() {
        private val TAG = "AcceptThread"
        override fun run() {
            try {
                Log.v(TAG, "Accepted")

                val bluetoothServerSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("dev.synople.glassecho", glassEchoUUID)
                Log.v(TAG, "Accepted2")

                val socket = bluetoothServerSocket.accept()
                Log.v(TAG, "Accepted3")
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
            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    Log.v(TAG, "incomingMessage: $incomingMessage")

                    Intent().also { intent ->
                        message = incomingMessage
                        intent.action = CONFIG_CHANGE
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

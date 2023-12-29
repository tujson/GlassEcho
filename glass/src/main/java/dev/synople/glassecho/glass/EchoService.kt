package dev.synople.glassecho.glass

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import dev.synople.glassecho.common.glassEchoUUID
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.common.models.EchoNotificationAction
import dev.synople.glassecho.glass.utils.Constants
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private val TAG = EchoService::class.java.simpleName

class EchoService : Service() {

    private lateinit var bluetoothDevice: BluetoothDevice
    private var phoneConnection: ConnectedThread? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (phoneConnection == null || phoneConnection?.isConnected() == false) {
            intent?.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_BLUETOOTH_DEVICE)?.let {
                bluetoothDevice = it
                phoneConnection = ConnectedThread(it).apply {
                    start()
                }
            } ?: run {
                Log.v(TAG, "No BluetoothDevice found in extras")
            }
        } else {
            intent?.getParcelableExtra<EchoNotificationAction>(Constants.EXTRA_NOTIFICATION_ACTION)
                ?.let {
                    phoneConnection?.write(it)
                } ?: run {
                phoneConnection?.broadcastDeviceStatus()
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneConnection?.cancel()
    }

    private fun handleNotification(echoNotification: EchoNotification) {
        (applicationContext as EchoApplication).getRepository()
            .handleNotification(echoNotification)

        if (!echoNotification.isRemoved) {
            val audio = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audio.playSoundEffect(Constants.GLASS_SOUND_TAP)
        }

        if (echoNotification.isWakeScreen) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager

            @Suppress("DEPRECATION")
            if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && !powerManager.isInteractive)
                || (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT && !powerManager.isScreenOn)
            ) {
                val wakelock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "glassecho:wakelocktag"
                )
                wakelock.acquire(3000L)
                val mainActivityIntent = Intent(applicationContext, MainActivity::class.java)
                mainActivityIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainActivityIntent)
            }
        }
    }

    inner class ConnectedThread(private val bluetoothDevice: BluetoothDevice) : Thread() {
        private val TAG = "ConnectedThread"
        private var isRunning = AtomicBoolean(true)
        private var bluetoothSocket: BluetoothSocket? = null

        override fun run() {
            var isConnected = init()

            while (isConnected && isRunning.get()) {
                try {
                    val objectInputStream = ObjectInputStream(bluetoothSocket?.inputStream)
                    val echoMessage = objectInputStream.readObject()

                    if (echoMessage is EchoNotification) {
                        handleNotification(echoMessage)
                    } else {
                        Log.v(
                            TAG,
                            "Received unknown message of type ${echoMessage.javaClass.simpleName}: $echoMessage"
                        )
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Potential socket disconnect. Attempting to reconnect", e)
                    // Attempt to reconnect
                    isConnected = init()
                }
            }

            cancel()
        }

        fun write(message: Serializable) {
            thread {
                if (isRunning.get()) {
                    try {
                        Log.v(TAG, "Writing: $message")
                        bluetoothSocket?.let {
                            val objectOutputStream = ObjectOutputStream(it.outputStream)
                            objectOutputStream.writeObject(message)
                        }
                    } catch (e: IOException) {
                        Log.e(
                            TAG,
                            "Write (bluetoothSocket isConnected: ${bluetoothSocket?.isConnected}",
                            e
                        )
                        bluetoothSocket = establishConnection()
                    }
                }
            }
        }

        private fun init(): Boolean {
            val isConnected: Boolean = establishConnection()?.let {
                bluetoothSocket = it
                bluetoothSocket?.isConnected
            } ?: run {
                false
            }

            broadcastDeviceStatus()

            return isConnected
        }

        private fun establishConnection(): BluetoothSocket? {
            try {
                val socket = bluetoothDevice.createRfcommSocketToServiceRecord(glassEchoUUID)
                socket.connect()
                Log.v(
                    TAG,
                    "Connection status to ${socket.remoteDevice.name}: ${socket.isConnected}"
                )
                return socket
            } catch (e: IOException) {
                Log.v(TAG, "Failed to accept", e)
            }

            return null
        }

        fun cancel() {
            try {
                bluetoothSocket?.close()
                isRunning.set(false)
                Log.v(TAG, "Cancelling ConnectedThread")
            } catch (e: IOException) {
                Log.e(TAG, "cancel", e)
            } catch (e: RuntimeException) {
                Log.e(TAG, "cancel", e)
            }
        }

        fun isConnected() = isRunning.get() && bluetoothSocket?.isConnected == true

        fun broadcastDeviceStatus() {
            Intent().also { statusIntent ->
                Log.v(TAG, "broadcastDeviceStatus: ${bluetoothSocket?.isConnected}")
                statusIntent.action = Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS
                statusIntent.putExtra(
                    Constants.EXTRA_DEVICE_IS_CONNECTED,
                    bluetoothSocket?.isConnected
                )
                if (bluetoothSocket?.isConnected == true) {
                    statusIntent.putExtra(
                        Constants.EXTRA_DEVICE_NAME,
                        bluetoothSocket?.remoteDevice?.name
                    )
                    statusIntent.putExtra(
                        Constants.EXTRA_DEVICE_ADDRESS,
                        bluetoothSocket?.remoteDevice?.address
                    )
                }
                sendBroadcast(statusIntent)
            }
        }
    }
}

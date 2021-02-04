package dev.synople.glassecho.phone.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dev.synople.glassecho.phone.NotificationActionReceiver
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.databinding.FragmentStatusBinding
import dev.synople.glassecho.phone.services.EchoNotificationListenerService

private val TAG = StatusFragment::class.java.simpleName
const val TEST_NOTIFICATION_ACTION = "dev.synople.glassecho.phone.TEST_NOTIFICATION_ACTION"
const val TEST_NOTIFICATION_ACTION_REPLY =
    "dev.synople.glassecho.phone.TEST_NOTIFICATION_ACTION_REPLY"
const val TEST_NOTIFICATION_ID = 157
const val TEST_NOTIFICATION_CHANNEL_ID = "dev.synople.glassecho.phone.test"

class StatusFragment : Fragment(R.layout.fragment_status) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FragmentStatusBinding.bind(view).apply {
            btnConnect.setOnClickListener {
                val discoverableIntent: Intent =
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                startActivity(discoverableIntent)

                startEchoService()

                setQrCode(ivQrCode)
            }

            btnTestNotif.setOnClickListener {
                showTestNotification()
            }
        }
    }

    private fun showTestNotification() {
        createNotificationChannel()

        val remoteInput = RemoteInput.Builder(TEST_NOTIFICATION_ACTION_REPLY).run {
            setLabel("Reply")
            build()
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            requireContext().applicationContext,
            0,
            Intent(requireContext(), NotificationActionReceiver::class.java).apply {
                action = TEST_NOTIFICATION_ACTION_REPLY
                putExtra(TEST_NOTIFICATION_ACTION_REPLY, "REPLY")
            },
            0
        )
        val replyAction = NotificationCompat.Action.Builder(null, "Reply", replyPendingIntent)
            .addRemoteInput(remoteInput).build()

        val firstAction = PendingIntent.getBroadcast(
            requireContext().applicationContext,
            1,
            Intent(requireContext(), NotificationActionReceiver::class.java).apply {
                action = TEST_NOTIFICATION_ACTION
                putExtra(TEST_NOTIFICATION_ACTION, "One")
            },
            0
        )
        val secondAction = PendingIntent.getBroadcast(
            requireContext().applicationContext,
            2,
            Intent(requireContext(), NotificationActionReceiver::class.java).apply {
                action = TEST_NOTIFICATION_ACTION
                putExtra(TEST_NOTIFICATION_ACTION, "Two")
            },
            0
        )

        val builder =
            NotificationCompat.Builder(
                requireContext().applicationContext,
                TEST_NOTIFICATION_CHANNEL_ID
            )
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_notif_icon))
                .setSmallIcon(R.drawable.ic_notif_icon)
                .setContentTitle("Test Notification")
                .setContentText("GlassEcho Content: ${System.currentTimeMillis() / 1000}")
                .addAction(replyAction)
                .addAction(R.drawable.ic_stop, "One", firstAction)
                .addAction(R.drawable.ic_stop, "Two", secondAction)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(requireContext())) {
            notify(TEST_NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_NOTIFICATION_CHANNEL_ID,
                "GlassEcho Testing Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for GlassEcho's test notifications"
            }

            requireContext().getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(
                    channel
                )
        }
    }

    private fun setQrCode(imageView: ImageView) {
        val bluetoothName = BluetoothAdapter.getDefaultAdapter().name
        val qrCodeBitmap =
            BarcodeEncoder().encodeBitmap(bluetoothName, BarcodeFormat.QR_CODE, 800, 800)
        imageView.setImageBitmap(qrCodeBitmap)
    }

    private fun startEchoService() {
        val serviceIntent =
            Intent(requireContext(), EchoNotificationListenerService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }
}
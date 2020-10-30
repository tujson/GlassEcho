package dev.synople.glassecho.phone.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dev.synople.glassecho.phone.NotificationActionReceiver
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.databinding.FragmentHomeBinding
import dev.synople.glassecho.phone.services.GlassEchoNotificationListenerService


private val TAG = HomeFragment::class.java.simpleName
const val TEST_NOTIFICATION_ACTION = "dev.synople.glassecho.phone.TEST_NOTIFICATION_ACTION"
const val TEST_NOTIFICATION_ACTION_REPLY =
    "dev.synople.glassecho.phone.TEST_NOTIFICATION_ACTION_REPLY"

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val CHANNEL_ID = "dev.synople.glassecho.phone"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)
        binding.apply {
            btnConnect.setOnClickListener {
                val discoverableIntent: Intent =
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                        putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    }
                startActivity(discoverableIntent)

                startEchoService()

                setQrCode()
            }

            btnNotifications.setOnClickListener {
                it.findNavController()
                    .navigate(HomeFragmentDirections.actionHomeFragmentToNotificationPickerFragment())
            }

            btnTestNotif.setOnClickListener {
                showNotification()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun showNotification() {
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
            NotificationCompat.Builder(requireContext().applicationContext, CHANNEL_ID)
                .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_stop))
                .setContentTitle("GlassEcho Test Title")
                .setContentText("GlassEcho Content Text\n${System.currentTimeMillis() / 1000}")
                .addAction(replyAction)
                .addAction(R.drawable.ic_stop, "One", firstAction)
                .addAction(R.drawable.ic_stop, "Two", secondAction)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(requireContext())) {
            notify(156, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Channel name"
            val descriptionText = "GlassEcho description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            // Register the channel with the system
            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setQrCode() {
        val bluetoothName = BluetoothAdapter.getDefaultAdapter().name
        val qrCodeBitmap =
            BarcodeEncoder().encodeBitmap(bluetoothName, BarcodeFormat.QR_CODE, 800, 800)
        binding.ivQrCode.setImageBitmap(qrCodeBitmap)
    }

    private fun startEchoService() {
        val serviceIntent =
            Intent(requireContext(), GlassEchoNotificationListenerService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }
}

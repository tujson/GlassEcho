package dev.synople.glassecho.phone.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.databinding.FragmentHomeBinding
import dev.synople.glassecho.phone.services.GlassEchoNotificationListenerService


private val TAG = HomeFragment::class.java.simpleName

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
                createNotificationChannel()
                val builder =
                    NotificationCompat.Builder(requireContext().applicationContext, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stop)
                        .setContentTitle("GlassEcho Test Title")
                        .setContentText("GlassEcho Content Text\n${System.currentTimeMillis() / 1000}")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                with(NotificationManagerCompat.from(requireContext())) {
                    Log.v(TAG, "About to notify")
                    notify(156, builder.build())
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
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
            Log.v(TAG, "Created notification channel")
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

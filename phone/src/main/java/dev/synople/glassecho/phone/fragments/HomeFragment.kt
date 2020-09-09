package dev.synople.glassecho.phone.fragments

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.services.GlassEchoNotificationListenerService
import kotlinx.android.synthetic.main.fragment_home.*


private val TAG = HomeFragment::class.java.simpleName

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            this.findNavController()
                .navigate(HomeFragmentDirections.actionHomeFragmentToNotificationPickerFragment())
        }
    }

    private fun setQrCode() {
        val bluetoothName = BluetoothAdapter.getDefaultAdapter().name
        val qrCodeBitmap =
            BarcodeEncoder().encodeBitmap(bluetoothName, BarcodeFormat.QR_CODE, 800, 800)
        ivQrCode.setImageBitmap(qrCodeBitmap)
    }

    private fun startEchoService() {
        val serviceIntent =
            Intent(requireContext(), GlassEchoNotificationListenerService::class.java)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }
}

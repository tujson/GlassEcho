package dev.synople.glassecho.phone.fragments

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.services.GlassEchoNotificationListenerService
import kotlinx.android.synthetic.main.fragment_home.*


private val TAG = HomeFragment::class.java.simpleName

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnConnect.setOnClickListener {
            selectPairedDevice()
        }

        btnNotifications.setOnClickListener {
            this.findNavController()
                .navigate(HomeFragmentDirections.actionHomeFragmentToNotificationPickerFragment())
        }
    }

    private fun selectPairedDevice() {
        val pairedDevicesDialogFragment = PairedDevicesDialogFragment.newInstance()
        pairedDevicesDialogFragment.setTargetFragment(this, Constants.PAIRED_DEVICE_REQUEST_CODE)
        pairedDevicesDialogFragment
            .show(parentFragmentManager, PairedDevicesDialogFragment::class.java.simpleName)
    }

    private fun startPairing() {
        val deviceManager = requireActivity().getSystemService(CompanionDeviceManager::class.java)
        val deviceFilter = BluetoothDeviceFilter.Builder()
//            .setNamePattern(Pattern.compile("^Glass"))
            .build()

        val pairingRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .build()

        deviceManager?.associate(
            pairingRequest,
            object : CompanionDeviceManager.Callback() {
                override fun onDeviceFound(chooserLauncher: IntentSender?) {
                    Log.v(TAG, "onDeviceFound")
                    startIntentSenderForResult(
                        chooserLauncher,
                        Constants.SELECT_DEVICE_REQUEST_CODE,
                        null, 0, 0, 0, null
                    )
                }

                override fun onFailure(error: CharSequence?) {
                    Log.e(TAG, "CompanionDeviceManager associate onFailure: $error")
                }
            }, null
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            Constants.SELECT_DEVICE_REQUEST_CODE -> when (resultCode) {
                Activity.RESULT_OK -> {
                    val device =
                        data?.getParcelableExtra<BluetoothDevice>(CompanionDeviceManager.EXTRA_DEVICE)
                    device?.createBond()
                    device?.let {
                        startEchoService(it)
                    }
                }
            }
            Constants.PAIRED_DEVICE_REQUEST_CODE -> when (resultCode) {
                Constants.PAIRED_DEVICE_PAIRED_RESULT_CODE -> {
                    val device =
                        data?.getParcelableExtra<BluetoothDevice>(Constants.EXTRA_BLUETOOTH_DEVICE)

                    device?.let {
                        startEchoService(it)
                    } ?: run {
                        Log.e(
                            TAG,
                            "BluetoothDevice from PairedDevicesDialogFragment is null. Defaulting to pairing."
                        )
                        startPairing()
                    }
                }
                Constants.PAIRED_DEVICE_PAIR_RESULT_CODE -> {
                    startPairing()
                }
            }
        }
    }

    private fun startEchoService(bluetoothDevice: BluetoothDevice) {
        val serviceIntent =
            Intent(requireContext(), GlassEchoNotificationListenerService::class.java)
        serviceIntent.putExtra(Constants.EXTRA_BLUETOOTH_DEVICE, bluetoothDevice)
        ContextCompat.startForegroundService(requireContext(), serviceIntent)
    }
}

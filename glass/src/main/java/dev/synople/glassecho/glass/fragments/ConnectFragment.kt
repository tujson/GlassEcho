package dev.synople.glassecho.glass.fragments

import android.app.Fragment
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.integration.android.IntentIntegrator
import dev.synople.glassecho.common.GLASS_SOUND_SUCCESS
import dev.synople.glassecho.glass.Constants
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.SourceConnectionService
import kotlinx.android.synthetic.main.fragment_connect.*

private val TAG = ConnectFragment::class.java.simpleName

class ConnectFragment : Fragment() {

    private var deviceName = ""

    private var pairDeviceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val bluetoothDevice =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if (bluetoothDevice.name == deviceName) {
                        foundBluetoothDevice(bluetoothDevice)
                    }
                }
            }
        }

        private fun foundBluetoothDevice(bluetoothDevice: BluetoothDevice) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
            activity.unregisterReceiver(this)

            startBluetoothService(bluetoothDevice)
        }
    }

    private var deviceStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getBooleanExtra(Constants.EXTRA_DEVICE_IS_CONNECTED, false)
                ?.let { isConnected ->
                    if (isConnected) {
                        val audio =
                            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audio.playSoundEffect(GLASS_SOUND_SUCCESS)

                        activity.runOnUiThread {
                            tvConnectStatus.text = "Connected!"

                            activity.fragmentManager
                                .beginTransaction()
                                .replace(
                                    R.id.frameLayoutMain,
                                    NotificationTimelineFragment.newInstance()
                                )
                                .commit()
                        }
                    } else {
                        tvConnectStatus.text =
                            "Failed to connect. Please quit both apps and try again."
                    }
                }

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.fragment_connect, container, false)!!

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        IntentIntegrator.forFragment(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Open GlassEcho on your phone and scan the QR code")
            .setBeepEnabled(false)
            .setBarcodeImageEnabled(false)
            .initiateScan()
    }

    private fun connectToDevice(deviceName: String) {
        checkPairedDevices(deviceName)?.let {
            startBluetoothService(it)
        } ?: run {
            pairDevice(deviceName)
        }
    }

    private fun checkPairedDevices(deviceName: String): BluetoothDevice? {
        BluetoothAdapter.getDefaultAdapter()
            .bondedDevices.iterator()
            .forEach {
                if (it.name == deviceName) return it
            }
        return null
    }

    private fun pairDevice(deviceName: String) {
        val isDiscovering = BluetoothAdapter.getDefaultAdapter().startDiscovery()

        if (isDiscovering) {
            this.deviceName = deviceName
            activity.registerReceiver(
                pairDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
        } else {
            Log.e(TAG, "pairDevice startDiscovery false")
        }
    }

    private fun startBluetoothService(bluetoothDevice: BluetoothDevice) {
        activity.registerReceiver(
            deviceStatusReceiver,
            IntentFilter(Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS)
        )
        activity.startService(Intent(activity, SourceConnectionService::class.java).apply {
            putExtra(
                Constants.EXTRA_BLUETOOTH_DEVICE, bluetoothDevice
            )
        })
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            activity.unregisterReceiver(pairDeviceReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "pairDeviceReceiver was never registered", e)
        }
        try {
            activity.unregisterReceiver(deviceStatusReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "deviceStatusReceiver was never registered", e)
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.let { barcodeResult ->
            barcodeResult.contents?.let { barcodeContents ->
                activity.runOnUiThread {
                    tvConnectStatus.text = "Attempting to connect to \"$barcodeContents...\""
                }
                connectToDevice(barcodeContents)
            } ?: run {
                activity.runOnUiThread {
                    tvConnectStatus.text = "Error while scanning QR code."
                }
            }
        }
    }

    companion object {
        fun newInstance() = ConnectFragment()
    }
}
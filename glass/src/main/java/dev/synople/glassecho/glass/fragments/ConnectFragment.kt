package dev.synople.glassecho.glass.fragments

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.zxing.integration.android.IntentIntegrator
import dev.synople.glassecho.common.GLASS_SOUND_SUCCESS
import dev.synople.glassecho.glass.Constants
import dev.synople.glassecho.glass.GlassGesture
import dev.synople.glassecho.glass.GlassGestureDetector
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.SourceConnectionService
import kotlinx.android.synthetic.main.fragment_connect.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

private val TAG = ConnectFragment::class.java.simpleName

class ConnectFragment : Fragment(R.layout.fragment_connect) {

    private var deviceName = ""
    private var sharedPref: SharedPreferences? = null

    private var pairDeviceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let {
                    if (it.name == deviceName) {
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                        requireActivity().unregisterReceiver(this)

                        startBluetoothService(it)
                    }
                }
            }
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

                        requireActivity().getSharedPreferences(
                            Constants.SHARED_PREF,
                            Context.MODE_PRIVATE
                        )
                            .edit()?.apply {
                                putString(
                                    Constants.SHARED_PREF_DEVICE_NAME,
                                    intent.getStringExtra(Constants.EXTRA_DEVICE_NAME)
                                )
                                putString(
                                    Constants.SHARED_PREF_DEVICE_ADDRESS,
                                    intent.getStringExtra(Constants.EXTRA_DEVICE_ADDRESS)
                                )
                                apply()
                            }

                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.frameLayoutMain,
                                NotificationTimelineFragment.newInstance()
                            )
                            .commit()

                        requireActivity().unregisterReceiver(this)
                    } else {
                        tvConnectStatus.text =
                            "Failed to connect. Please make sure GlassEcho is running on your phone."
                    }
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.connect_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                sharedPref?.edit()?.apply {
                    putString(
                        Constants.SHARED_PREF_DEVICE_NAME, ""
                    )
                    putString(
                        Constants.SHARED_PREF_DEVICE_ADDRESS, ""
                    )
                    apply()
                }

                startQrCodeScanner()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Subscribe
    fun onKeyEvent(glassGesture: GlassGesture) {
        if (glassGesture.gesture == GlassGestureDetector.Gesture.TAP) {
            requireActivity().openOptionsMenu()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPref =
            requireActivity().getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
        val deviceName = sharedPref?.getString(Constants.SHARED_PREF_DEVICE_NAME, "") ?: ""
        val deviceAddress = sharedPref?.getString(Constants.SHARED_PREF_DEVICE_ADDRESS, "") ?: ""

        if (deviceName.isNotEmpty() && deviceAddress.isNotEmpty()) {
            val pairedDevice = checkPairedDevices(deviceName)
            pairedDevice?.let {
                startBluetoothService(it)
                return
            } ?: run {
                // Device is no longer in paired devices list
                sharedPref?.edit()?.apply {
                    putString(Constants.SHARED_PREF_DEVICE_NAME, "")
                    putString(Constants.SHARED_PREF_DEVICE_ADDRESS, "")
                    apply()
                }
            }
        }

        startQrCodeScanner()
    }

    private fun startQrCodeScanner() {
        IntentIntegrator.forSupportFragment((this as Fragment))
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
            requireActivity().registerReceiver(
                pairDeviceReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
        } else {
            Log.e(TAG, "pairDevice startDiscovery false")
        }
    }

    private fun startBluetoothService(bluetoothDevice: BluetoothDevice) {
        requireActivity().registerReceiver(
            deviceStatusReceiver,
            IntentFilter(Constants.INTENT_FILTER_DEVICE_CONNECT_STATUS)
        )

        requireActivity().startService(Intent(activity, SourceConnectionService::class.java).apply {
            putExtra(
                Constants.EXTRA_BLUETOOTH_DEVICE, bluetoothDevice
            )
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            requireActivity().unregisterReceiver(pairDeviceReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "pairDeviceReceiver was never registered", e)
        }
        try {
            requireActivity().unregisterReceiver(deviceStatusReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "deviceStatusReceiver was never registered", e)
        }
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        IntentIntegrator.parseActivityResult(requestCode, resultCode, data)?.let { barcodeResult ->
            barcodeResult.contents?.let { barcodeContents ->
                requireActivity().runOnUiThread {
                    tvConnectStatus.text = "Attempting to connect to \"$barcodeContents...\""
                }
                connectToDevice(barcodeContents)
            } ?: run {
                requireActivity().runOnUiThread {
                    tvConnectStatus.text = "Error while scanning QR code."
                }
            }
        }
    }

    companion object {
        fun newInstance() = ConnectFragment()
    }
}
package dev.synople.glassecho.phone.fragments

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.adapters.PairedDeviceAdapter
import kotlinx.android.synthetic.main.dialog_fragment_paired_devices.*


class PairedDevicesDialogFragment : DialogFragment() {

    private lateinit var adapter: PairedDeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.dialog_fragment_paired_devices, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter =
            PairedDeviceAdapter(BluetoothAdapter.getDefaultAdapter().bondedDevices.toList()) {
                targetFragment?.onActivityResult(
                    Constants.PAIRED_DEVICE_REQUEST_CODE,
                    Constants.PAIRED_DEVICE_PAIRED_RESULT_CODE,
                    Intent().apply {
                        putExtra(Constants.EXTRA_BLUETOOTH_DEVICE, it)
                    })
                dismiss()
            }
        rvPairedDevices.adapter = adapter

        tvCancel.setOnClickListener {
            targetFragment?.onActivityResult(
                Constants.PAIRED_DEVICE_REQUEST_CODE,
                Constants.PAIRED_DEVICE_CANCEL_RESULT_CODE, null
            )
            dismiss()
        }

        tvPairNewDevice.setOnClickListener {
            targetFragment?.onActivityResult(
                Constants.PAIRED_DEVICE_REQUEST_CODE,
                Constants.PAIRED_DEVICE_PAIR_RESULT_CODE, null
            )
            dismiss()
        }
    }

    companion object {
        fun newInstance(): PairedDevicesDialogFragment = PairedDevicesDialogFragment()
    }
}
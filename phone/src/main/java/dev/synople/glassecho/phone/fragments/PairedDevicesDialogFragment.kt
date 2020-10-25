package dev.synople.glassecho.phone.fragments

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.adapters.PairedDeviceAdapter
import dev.synople.glassecho.phone.databinding.DialogFragmentPairedDevicesBinding


class PairedDevicesDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentPairedDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PairedDeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogFragmentPairedDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

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

        binding.apply {
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
    }

    companion object {
        fun newInstance(): PairedDevicesDialogFragment = PairedDevicesDialogFragment()
    }
}
package dev.synople.glassecho.phone.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.phone.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.card_paired_device.view.*

class PairedDeviceAdapter(
    private val pairedDevices: List<BluetoothDevice>,
    private val pairedDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder>() {
    class ViewHolder(
        override val containerView: View,
        private val pairedDeviceClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindPairedDevice(bluetoothDevice: BluetoothDevice) {
            containerView.tvName.text = bluetoothDevice.name
            containerView.tvAddress.text = bluetoothDevice.address

            containerView.setOnClickListener { pairedDeviceClick(bluetoothDevice) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.card_paired_device, parent, false),
            pairedDeviceClick
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindPairedDevice(pairedDevices[position])
    }

    override fun getItemCount() = pairedDevices.size
}
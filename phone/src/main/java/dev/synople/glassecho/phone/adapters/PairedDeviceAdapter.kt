package dev.synople.glassecho.phone.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.phone.databinding.CardPairedDeviceBinding

class PairedDeviceAdapter(
    private val pairedDevices: List<BluetoothDevice>,
    private val pairedDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<PairedDeviceAdapter.ViewHolder>() {
    class ViewHolder(
        private val cardPairedDeviceBinding: CardPairedDeviceBinding,
        private val pairedDeviceClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.ViewHolder(cardPairedDeviceBinding.root) {
        fun bindPairedDevice(bluetoothDevice: BluetoothDevice) {
            cardPairedDeviceBinding.apply {
                tvName.text = bluetoothDevice.name
                tvAddress.text = bluetoothDevice.address

                this.root.setOnClickListener { pairedDeviceClick(bluetoothDevice) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            CardPairedDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            pairedDeviceClick
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindPairedDevice(pairedDevices[position])
    }

    override fun getItemCount() = pairedDevices.size
}
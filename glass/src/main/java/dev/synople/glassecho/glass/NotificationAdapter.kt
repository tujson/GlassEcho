package dev.synople.glassecho.glass

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.databinding.LiveCardBinding

class NotificationAdapter(private val notifications: MutableList<EchoNotification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(private val liveCardBinding: LiveCardBinding) :
        RecyclerView.ViewHolder(liveCardBinding.root) {
        fun bindNotification(echoNotification: EchoNotification) {
            liveCardBinding.apply {
                ivAppIcon.setImageBitmap(EchoNotification.byteArrayToBitmap(echoNotification.appIcon))
                tvAppName.text = echoNotification.appName
                ivLargeIcon.setImageBitmap(EchoNotification.byteArrayToBitmap(echoNotification.largeIcon))
                ivLargeIcon.setImageBitmap(EchoNotification.byteArrayToBitmap(echoNotification.largeIcon))
                tvTitle.text = echoNotification.title
                tvText.text = echoNotification.text
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LiveCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindNotification(notifications[position])
    }

    override fun getItemCount() = notifications.size
}
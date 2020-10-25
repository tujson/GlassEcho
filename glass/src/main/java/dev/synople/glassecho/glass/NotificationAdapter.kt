package dev.synople.glassecho.glass

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.common.models.EchoNotification
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.live_card.view.*

class NotificationAdapter(private val notifications: MutableList<EchoNotification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
        LayoutContainer {
        fun bindNotification(echoNotification: EchoNotification) {
            containerView.ivAppIcon.setImageBitmap(echoNotification.appIcon)
            containerView.tvTitle.text = echoNotification.title
            containerView.tvText.text = echoNotification.text
            containerView.tvAppName.text = echoNotification.text
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.live_card, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindNotification(notifications[position])
    }

    override fun getItemCount() = notifications.size
}
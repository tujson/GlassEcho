package dev.synople.glassecho.glass.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.databinding.ItemNotificationBinding

class NotificationAdapter :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val notifications: MutableList<EchoNotification> = mutableListOf()
    private val viewPool = RecyclerView.RecycledViewPool()

    fun setNotifications(notifications: List<EchoNotification>) {
        this.notifications.clear()
        this.notifications.addAll(notifications)
        notifyDataSetChanged()
    }

    class ViewHolder(val liveCardBinding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(liveCardBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_notification,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.liveCardBinding.apply {
            echoNotification = notifications[position]
            rvActions.setRecycledViewPool(viewPool)

            executePendingBindings()
        }
    }

    override fun getItemCount() = notifications.size
}
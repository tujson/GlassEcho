package dev.synople.glassecho.glass.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.databinding.LiveCardBinding

class NotificationAdapter(
    private val notifications: MutableList<EchoNotification>,
    private val itemClick: (EchoNotification) -> Unit
) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(private val liveCardBinding: LiveCardBinding) :
        RecyclerView.ViewHolder(liveCardBinding.root) {

        fun bindNotification(
            echoNotification: EchoNotification,
            itemClick: (EchoNotification) -> Unit
        ) {
            val actionAdapter = NotificationActionAdapter(echoNotification.actions) {

            }

            liveCardBinding.apply {
                this.echoNotification = echoNotification
                executePendingBindings()

                rvActions.adapter = actionAdapter
                rvActions.setHasFixedSize(true)

                this.root.setOnClickListener {
                    itemClick(echoNotification)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LiveCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindNotification(notifications[position], itemClick)
    }

    override fun getItemCount() = notifications.size
}
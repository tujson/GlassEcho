package dev.synople.glassecho.glass.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.glass.databinding.ItemNotificationActionBinding

class NotificationActionAdapter(
    private val notificationActions: List<String>,
) : RecyclerView.Adapter<NotificationActionAdapter.ViewHolder>() {

    class ViewHolder(val rowActionBinding: ItemNotificationActionBinding) :
        RecyclerView.ViewHolder(rowActionBinding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemNotificationActionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.rowActionBinding.tvAction.text = notificationActions[position]
    }

    override fun getItemCount() = notificationActions.size
}
package dev.synople.glassecho.glass.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.glass.databinding.RowActionBinding

class NotificationActionAdapter(
    private val notificationActions: List<String>,
    private val actionClick: (String) -> Unit
) : RecyclerView.Adapter<NotificationActionAdapter.ViewHolder>() {

    class ViewHolder(private val rowActionBinding: RowActionBinding) :
        RecyclerView.ViewHolder(rowActionBinding.root) {
        fun bindAction(action: String, actionClick: (String) -> Unit) {
            rowActionBinding.apply {
                tvAction.text = action
                this.root.setOnClickListener { actionClick(action) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(RowActionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindAction(notificationActions[position], actionClick)
    }

    override fun getItemCount() = notificationActions.size
}
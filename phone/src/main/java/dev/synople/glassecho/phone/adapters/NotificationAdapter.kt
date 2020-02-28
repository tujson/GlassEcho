package dev.synople.glassecho.phone.adapters

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.phone.R
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.row_notification.view.*

class NotificationAdapter(
    private val apps: List<ResolveInfo>,
    private val packageManager: PackageManager,
    private val sharedPref: SharedPreferences,
    private val onSwitch: (String, Boolean) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(
        override val containerView: View
    ) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        fun bindApp(
            resolveInfo: ResolveInfo,
            packageManager: PackageManager,
            sharedPref: SharedPreferences,
            onSwitch: (String, Boolean) -> Unit
        ) {
            containerView.ivAppIcon.setImageDrawable(resolveInfo.loadIcon(packageManager))
            containerView.tvAppName.text = resolveInfo.loadLabel(packageManager)

            containerView.ivSwitch.isChecked =
                sharedPref.getBoolean(resolveInfo.activityInfo.packageName, false)

            containerView.ivSwitch.setOnClickListener {
                sharedPref.edit()
                    .putBoolean(
                        resolveInfo.activityInfo.packageName,
                        containerView.ivSwitch.isChecked
                    )
                    .apply()

                onSwitch(resolveInfo.activityInfo.packageName, containerView.ivSwitch.isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.row_notification, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindApp(apps[position], packageManager, sharedPref, onSwitch)
    }

    override fun getItemCount() = apps.size
}


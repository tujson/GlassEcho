package dev.synople.glassecho.phone.adapters

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.phone.databinding.RowNotificationBinding

class NotificationAdapter(
    private val apps: List<ResolveInfo>,
    private val packageManager: PackageManager,
    private val sharedPref: SharedPreferences,
    private val onSwitch: (String, Boolean) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    class ViewHolder(
        private val rowNotificationBinding: RowNotificationBinding
    ) : RecyclerView.ViewHolder(rowNotificationBinding.root) {
        fun bindApp(
            resolveInfo: ResolveInfo,
            packageManager: PackageManager,
            sharedPref: SharedPreferences,
            onSwitch: (String, Boolean) -> Unit
        ) {
            rowNotificationBinding.apply {
                ivAppIcon.setImageDrawable(resolveInfo.loadIcon(packageManager))
                tvAppName.text = resolveInfo.loadLabel(packageManager)

                ivSwitch.isChecked =
                    sharedPref.getBoolean(resolveInfo.activityInfo.packageName, false)

                ivSwitch.setOnClickListener {
                    sharedPref.edit()
                        .putBoolean(
                            resolveInfo.activityInfo.packageName,
                            ivSwitch.isChecked
                        )
                        .apply()

                    onSwitch(resolveInfo.activityInfo.packageName, ivSwitch.isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            RowNotificationBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindApp(apps[position], packageManager, sharedPref, onSwitch)
    }

    override fun getItemCount() = apps.size
}


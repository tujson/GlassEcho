package dev.synople.glassecho.phone.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import dev.synople.glassecho.phone.MainActivity.Companion.SHARED_PREFS
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.adapters.NotificationAdapter
import dev.synople.glassecho.phone.databinding.FragmentNotificationPickerBinding
import java.util.Collections

/**
 * Lets users pick which apps show notifications on Glass
 */
class NotificationPickerFragment : Fragment(R.layout.fragment_notification_picker),
    CompoundButton.OnCheckedChangeListener {

    private lateinit var installedApps: List<ResolveInfo>
    private lateinit var adapter: NotificationAdapter

    private lateinit var sharedPref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        installedApps = getInstalledApps()
        sharedPref = activity?.getSharedPreferences(
            SHARED_PREFS,
            Context.MODE_PRIVATE
        )!!

        FragmentNotificationPickerBinding.bind(view).apply {
            adapter = NotificationAdapter(
                installedApps,
                requireContext().packageManager,
                sharedPref
            ) { packageName, isChecked ->
                switchAllOn.setOnCheckedChangeListener(null)
                if (switchAllOn.isChecked && !isChecked) {
                    switchAllOn.isChecked = false
                } else if (!switchAllOn.isChecked && isChecked && areAllAppsChecked(installedApps)) {
                    switchAllOn.isChecked = true
                }
                switchAllOn.setOnCheckedChangeListener(this@NotificationPickerFragment)
            }
            rvApps.adapter = adapter

            switchAllOn.isChecked = areAllAppsChecked(installedApps)

            switchAllOn.setOnCheckedChangeListener(this@NotificationPickerFragment)
        }
    }

    private fun getInstalledApps(): List<ResolveInfo> {
        val pm = requireContext().packageManager
        val main = Intent(Intent.ACTION_MAIN, null)
        main.addCategory(Intent.CATEGORY_LAUNCHER)
        val launchables = pm.queryIntentActivities(main, 0)
        Collections.sort(launchables, ResolveInfo.DisplayNameComparator(pm))
        return launchables
    }

    private fun areAllAppsChecked(installedApps: List<ResolveInfo>): Boolean {
        installedApps.forEach {
            if (!sharedPref.getBoolean(it.activityInfo.packageName, false)) {
                return false
            }
        }

        return true
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        installedApps.forEach {
            sharedPref.edit()
                .putBoolean(it.activityInfo.packageName, isChecked)
                .apply()
        }
        adapter.notifyDataSetChanged()
    }

}

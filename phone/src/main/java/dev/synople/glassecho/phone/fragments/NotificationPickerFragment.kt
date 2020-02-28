package dev.synople.glassecho.phone.fragments

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.adapters.NotificationAdapter
import kotlinx.android.synthetic.main.fragment_notification_picker.*
import java.util.*

/**
 * Lets users pick which apps show notifications on Glass
 */
class NotificationPickerFragment : Fragment() {

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.fragment_notification_picker, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = NotificationAdapter(
            getInstalledApps(), context!!.packageManager, activity?.getPreferences(
                Context.MODE_PRIVATE
            )!!
        )
        rvApps.adapter = adapter
    }

    private fun getInstalledApps(): List<ResolveInfo> {
        val pm = context!!.packageManager
        val main = Intent(Intent.ACTION_MAIN, null)
        main.addCategory(Intent.CATEGORY_LAUNCHER)
        val launchables = pm.queryIntentActivities(main, 0)
        Collections.sort(launchables, ResolveInfo.DisplayNameComparator(pm))
        return launchables
    }

}

package dev.synople.glassecho.phone.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.squareup.sqldelight.android.AndroidSqliteDriver
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.Database
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.adapters.AppAdapter
import dev.synople.glassecho.phone.databinding.FragmentNotificationListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


private val TAG = NotificationListFragment::class.java.simpleName

/**
 * Lets users pick which apps show notifications on Glass
 */
class NotificationListFragment : Fragment(R.layout.fragment_notification_list), CoroutineScope {

    private lateinit var adapter: AppAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val driver =
            AndroidSqliteDriver(Database.Schema, requireContext(), Constants.DATABASE_ECHO_APP)
        val echoAppsDatabase = Database(driver)
        val echoAppQueries = echoAppsDatabase.echoAppQueries

        val allApps = echoAppQueries.selectAll().executeAsList().toMutableList()

        Log.v(TAG, "f$allApps")

        FragmentNotificationListBinding.bind(view).apply {
            adapter = AppAdapter(
                allApps
            ) { app, position ->
                launch {
                    echoAppQueries.insert(app)
                }

                rvApps.post {
                    allApps[position] = app
                    adapter.notifyItemChanged(position)
                }
            }

            rvApps.adapter = adapter

            rvApps.addItemDecoration(
                DividerItemDecoration(
                    rvApps.context,
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
}

package dev.synople.glassecho.phone

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
    private val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isNotificationServiceEnabled()) {
            buildNotificationServiceAlertDialog()?.show()
        }

        val navController = findNavController(R.id.nav_host_fragment)
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)
        val appBarConfiguration = AppBarConfiguration(
            topLevelDestinationIds = setOf(
                R.id.statusFragment,
                R.id.notificationListFragment,
                R.id.settingsFragment,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (!isNotificationServiceEnabled()) {
            buildNotificationServiceAlertDialog()?.show()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            ENABLED_NOTIFICATION_LISTENERS
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            names.indices.forEach { index ->
                ComponentName.unflattenFromString(names[index])?.let { componentName ->
                    if (TextUtils.equals(packageName, componentName.packageName)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun buildNotificationServiceAlertDialog(): AlertDialog? {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("GlassEcho")
        alertDialogBuilder.setMessage("Needs permission to read notifications.")
        alertDialogBuilder.setPositiveButton(
            "Yes"
        ) { _, _ ->
            startActivity(
                Intent(
                    ACTION_NOTIFICATION_LISTENER_SETTINGS
                )
            )
        }
        alertDialogBuilder.setNegativeButton(
            "No"
        ) { _, _ ->
        }
        return alertDialogBuilder.create()
    }
}

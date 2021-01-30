package dev.synople.glassecho.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.synople.glassecho.phone.services.EchoNotificationListenerService

class GlassEchoBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            if (intent?.extras?.getString(Constants.FROM_NOTIFICATION) == Constants.NOTIFICATION_ACTION_STOP) {
                val serviceIntent =
                    Intent(context, EchoNotificationListenerService::class.java)
                serviceIntent.putExtra(
                    Constants.FROM_NOTIFICATION,
                    Constants.NOTIFICATION_ACTION_STOP
                )
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
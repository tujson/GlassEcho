package dev.synople.glassecho.phone

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import dev.synople.glassecho.phone.fragments.TEST_NOTIFICATION_ACTION
import dev.synople.glassecho.phone.fragments.TEST_NOTIFICATION_ACTION_REPLY
import dev.synople.glassecho.phone.fragments.TEST_NOTIFICATION_CHANNEL_ID
import dev.synople.glassecho.phone.fragments.TEST_NOTIFICATION_ID

private val TAG = NotificationActionReceiver::class.java.simpleName

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "onReceive: ${intent?.extras?.getString(TEST_NOTIFICATION_ACTION)}")

        if (intent?.action == TEST_NOTIFICATION_ACTION) {
            intent.extras?.getString(TEST_NOTIFICATION_ACTION)?.apply {
                Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
            }
        } else if (intent?.action == TEST_NOTIFICATION_ACTION_REPLY) {
            val replyText = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(
                TEST_NOTIFICATION_ACTION_REPLY
            )
            Toast.makeText(context, replyText, Toast.LENGTH_SHORT).show()

            val repliedNotification = Notification.Builder(context, TEST_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif_icon)
                .setContentText("Reply: $replyText")
                .build()

            context?.let {
                NotificationManagerCompat.from(it).apply {
                    notify(TEST_NOTIFICATION_ID, repliedNotification)
                }
            }
        }
    }
}
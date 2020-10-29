package dev.synople.glassecho.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import dev.synople.glassecho.phone.fragments.TEST_NOTIFICATION_ACTION
import dev.synople.glassecho.phone.fragments.TEST_NOTIFICATION_ACTION_REPLY

private val TAG = NotificationActionReceiver::class.java.simpleName

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "onReceive: ${intent?.extras?.getString(TEST_NOTIFICATION_ACTION)}")

        if (intent?.action == TEST_NOTIFICATION_ACTION) {
            intent.extras?.getString(TEST_NOTIFICATION_ACTION)?.apply {
                Toast.makeText(context, this, Toast.LENGTH_SHORT).show()
            }
        } else if (intent?.action == TEST_NOTIFICATION_ACTION_REPLY) {
            Toast.makeText(
                context, RemoteInput.getResultsFromIntent(intent)?.getCharSequence(
                    TEST_NOTIFICATION_ACTION_REPLY
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
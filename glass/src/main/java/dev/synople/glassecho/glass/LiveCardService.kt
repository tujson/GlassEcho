package dev.synople.glassecho.glass

import com.google.android.glass.timeline.LiveCard
import com.google.android.glass.timeline.LiveCard.PublishMode

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.widget.RemoteViews

class LiveCardService : Service() {

    private var liveCard: LiveCard? = null
    private lateinit var remoteViews: RemoteViews
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (liveCard == null) {
            liveCard = LiveCard(this, LIVE_CARD_TAG)

            remoteViews = RemoteViews(packageName, R.layout.live_card)
            liveCard!!.setViews(remoteViews)

            remoteViews.setTextViewText(R.id.tvTempo, "$bpm")

            val menuIntent = Intent(this, LiveCardMenuActivity::class.java)
            liveCard!!.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0))
            liveCard!!.publish(PublishMode.REVEAL)

            broadcastReceiver = MyBroadcastReceiver()
            val filter = IntentFilter(CONFIG_CHANGE)
            registerReceiver(broadcastReceiver, filter)
        } else {
            liveCard!!.navigate()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (liveCard != null && liveCard!!.isPublished) {
            liveCard!!.unpublish()
            liveCard = null
        }
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    companion object {
        const val CONFIG_CHANGE = "dev.synople.glassecho.CONFIG_CHANGE"
        private const val LIVE_CARD_TAG = "LiveCardService"
        var bpm = 120
    }

    class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            (context as LiveCardService).remoteViews.setTextViewText(R.id.tvTempo, "$bpm")
            context.liveCard?.setViews(context.remoteViews)
        }
    }
}

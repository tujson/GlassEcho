package dev.synople.glassecho.glass.fragments

import android.app.Fragment
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.glass.widget.CardBuilder
import com.google.android.glass.widget.CardScrollAdapter
import dev.synople.glassecho.common.GLASS_SOUND_TAP
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.Constants
import dev.synople.glassecho.glass.R
import kotlinx.android.synthetic.main.fragment_notification_timeline.*

private val TAG = NotificationTimelineFragment::class.java.simpleName

class NotificationTimelineFragment : Fragment() {

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<EchoNotification>(Constants.MESSAGE)?.let {
                Log.v(TAG, "EchoNotification\n${it.title}: ${it.text}")

                val audio =
                    context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.playSoundEffect(GLASS_SOUND_TAP)

                val cardBuilder = echoNotificationToCard(it)
                notificationCards.add(0, cardBuilder)
                activity.runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
    private lateinit var adapter: CardScrollAdapter
    private var notificationCards = mutableListOf<CardBuilder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.fragment_notification_timeline, container, false)!!

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity.registerReceiver(
            notificationReceiver,
            IntentFilter(Constants.INTENT_FILTER_NOTIFICATION)
        )

        adapter = object : CardScrollAdapter() {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?) =
                notificationCards[position].getView(convertView, parent)

            override fun getPosition(item: Any) = notificationCards.indexOf(item)

            override fun getItem(position: Int) = notificationCards[position]

            override fun getCount() = notificationCards.size
        }
        cardScrollView.adapter = adapter
    }

    private fun echoNotificationToCard(echoNotification: EchoNotification): CardBuilder {
        val card = CardBuilder(activity, CardBuilder.Layout.AUTHOR)
        card.setIcon(echoNotification.largeIcon)
        card.setAttributionIcon(echoNotification.appIcon)
        card.setHeading(echoNotification.title)
        card.setSubheading(echoNotification.appName)
        card.setText(echoNotification.text)
        return card
    }

    override fun onResume() {
        super.onResume()
        cardScrollView.activate()
    }

    override fun onPause() {
        super.onPause()
        cardScrollView.deactivate()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            activity.unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "notificationReceiver not registered", e)
        }
    }

    companion object {
        fun newInstance() = NotificationTimelineFragment()
    }
}
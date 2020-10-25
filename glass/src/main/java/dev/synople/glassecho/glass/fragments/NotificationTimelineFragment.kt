package dev.synople.glassecho.glass.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import dev.synople.glassecho.common.GLASS_SOUND_TAP
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.Constants
import dev.synople.glassecho.glass.GlassGesture
import dev.synople.glassecho.glass.GlassGestureDetector
import dev.synople.glassecho.glass.NotificationAdapter
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.databinding.FragmentNotificationTimelineBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

private val TAG = NotificationTimelineFragment::class.java.simpleName

class NotificationTimelineFragment : Fragment(R.layout.fragment_notification_timeline) {

    private var _binding: FragmentNotificationTimelineBinding? = null
    private val binding get() = _binding!!

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<EchoNotification>(Constants.MESSAGE)?.let {
                Log.v(TAG, "EchoNotification\n${it.title}: ${it.text}")

                val audio =
                    context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audio.playSoundEffect(GLASS_SOUND_TAP)

                notifications.add(it)
                requireActivity().runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private val notifications: MutableList<EchoNotification> = mutableListOf()
    private val adapter: NotificationAdapter = NotificationAdapter(notifications)
    private var rvPosition = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotificationTimelineBinding.bind(view)

        requireActivity().registerReceiver(
            notificationReceiver,
            IntentFilter(Constants.INTENT_FILTER_NOTIFICATION)
        )

        binding.apply {
            rvNotifications.adapter = adapter
            rvNotifications.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    @Subscribe
    fun onGesture(glassGesture: GlassGesture) {
        when (glassGesture.gesture) {
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                if (rvPosition != notifications.size) {
                    rvPosition++
                }
                binding.rvNotifications.smoothScrollToPosition(rvPosition)
            }
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                if (rvPosition != 0) {
                    rvPosition--
                }
                binding.rvNotifications.smoothScrollToPosition(rvPosition)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            requireActivity().unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "notificationReceiver not registered", e)
        }
    }

    companion object {
        fun newInstance() = NotificationTimelineFragment()
    }
}
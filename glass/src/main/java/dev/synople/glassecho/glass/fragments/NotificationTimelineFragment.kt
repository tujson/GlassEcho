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
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.common.GLASS_SOUND_TAP
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.Constants
import dev.synople.glassecho.glass.GlassGesture
import dev.synople.glassecho.glass.GlassGestureDetector
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.adapters.NotificationAdapter
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
                Log.v(TAG, "EchoNotification\n${it}")

                if (it.isRemoved) {
                    notifications.remove(it)
                } else {
                    if (notifications.contains(it)) {
                        notifications[notifications.indexOf(it)] = it
                    } else {
                        val audio =
                            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audio.playSoundEffect(GLASS_SOUND_TAP)

                        notifications.add(it)
                    }
                }

                requireActivity().runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private val notifications: MutableList<EchoNotification> = mutableListOf()
    private var adapter: NotificationAdapter = NotificationAdapter(notifications) {
        // TODO: Stuff
    }
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

        try {
            requireActivity().unregisterReceiver(notificationReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "notificationReceiver not registered", e)
        }

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
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_UP -> {
                scrollActions(true)
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_DOWN -> {
                scrollActions(false)
            }
        }
    }

    private fun scrollActions(isScrollUp: Boolean) {
        val actionRecyclerView = binding.rvNotifications.getChildAt(rvPosition)
            .findViewById<RecyclerView>(R.id.rvActions)

        var index =
            (actionRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        if (isScrollUp) {
            index--
        } else {
            index++
        }

        if (0 <= index && index < notifications[rvPosition].actions.size) {
            actionRecyclerView.smoothScrollToPosition(index)
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

    companion object {
        fun newInstance() = NotificationTimelineFragment()
    }
}
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
import dev.synople.glassecho.common.GLASS_SOUND_DISMISS
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

                var index = notifications.indexOf(it)

                if (it.isRemoved && index != -1) {
                    notifications.removeAt(index)

                    requireActivity().runOnUiThread {
                        adapter.notifyItemRemoved(index)
                    }
                } else if (!it.isRemoved) {
                    if (index == -1) {
                        notifications.add(it)
                        index = 0

                        requireActivity().runOnUiThread {
                            adapter.notifyItemInserted(0)
                        }
                    } else {
                        notifications[index] = it

                        requireActivity().runOnUiThread {
                            adapter.notifyItemChanged(0)
                        }
                    }

                    playSoundEffect(GLASS_SOUND_TAP)

                    requireActivity().runOnUiThread {
                        binding.rvNotifications.scrollToPosition(index)
                    }
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
            Log.v(TAG, "notificationReceiver not registered", e)
        }

        super.onDestroyView()
    }

    @Subscribe
    fun onGesture(glassGesture: GlassGesture) {
        when (glassGesture.gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                // TODO: Notif action
                playSoundEffect(GLASS_SOUND_TAP)
            }
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
            GlassGestureDetector.Gesture.SWIPE_UP -> {
                notifications.removeAt(rvPosition)
                adapter.notifyItemRemoved(rvPosition)

                playSoundEffect(GLASS_SOUND_DISMISS)

                // TODO: Notify phone that notif is dismissed
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_FORWARD -> {
                scrollActions(true)
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_BACKWARD -> {
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

    private fun playSoundEffect(soundEffect: Int) {
        val audio =
            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.playSoundEffect(soundEffect)
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
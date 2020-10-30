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
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.Constants
import dev.synople.glassecho.glass.GlassGesture
import dev.synople.glassecho.glass.GlassGestureDetector
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.adapters.NotificationAdapter
import dev.synople.glassecho.glass.databinding.FragmentNotificationTimelineBinding
import dev.synople.glassecho.glass.viewmodels.NotificationsViewModel
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

                notificationsViewModel.handleNotification(it)

                if (!it.isRemoved) {
                    playSoundEffect(Constants.GLASS_SOUND_TAP)
                }
            }
        }
    }

    private val notificationsViewModel: NotificationsViewModel by activityViewModels()
    private var adapter: NotificationAdapter = NotificationAdapter()
    private var rvPosition = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotificationTimelineBinding.bind(view)
        binding.rvNotifications.adapter = adapter

        notificationsViewModel.getNotifications().observe(viewLifecycleOwner, {
            adapter.setNotifications(it)

            if (it.size == 0) {
                binding.tvNumNotifications.text = getString(R.string.no_notifications)
            } else {
                binding.tvNumNotifications.text = "${rvPosition + 1}/${it.size}"
            }

            binding.executePendingBindings()
        })

        requireActivity().registerReceiver(
            notificationReceiver,
            IntentFilter(Constants.INTENT_FILTER_NOTIFICATION)
        )

        EventBus.getDefault().register(this)

    }

    override fun onDestroyView() {
        _binding = null
        EventBus.getDefault().unregister(this)

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
                executeNotification()
            }
            GlassGestureDetector.Gesture.SWIPE_UP -> {
                dismissNotification()
            }
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                scrollNotifications(true)
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                scrollNotifications(false)
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_FORWARD -> {
                scrollActions(true)
            }
            GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_BACKWARD -> {
                scrollActions(false)
            }
        }
    }

    private fun executeNotification() {
        playSoundEffect(Constants.GLASS_SOUND_TAP)
        // TODO: Notif action
    }

    private fun dismissNotification() {
        notificationsViewModel.remove(rvPosition)
        playSoundEffect(Constants.GLASS_SOUND_DISMISS)

        // TODO: Notify phone that notif is dismissed
    }

    private fun scrollNotifications(isScrollForward: Boolean) {
        if (isScrollForward && rvPosition - 1 >= 0) {
            rvPosition -= 1
        } else if (!isScrollForward && rvPosition + 1 < notificationsViewModel.size().toInt()) {
            rvPosition += 1
        } else {
            playSoundEffect(Constants.GLASS_SOUND_DISALLOWED)
        }

        if (notificationsViewModel.size().toInt() == 0) {
            binding.tvNumNotifications.text = getString(R.string.no_notifications)
        } else {
            binding.tvNumNotifications.text = "${rvPosition + 1}/${notificationsViewModel.size()}"
        }
        binding.rvNotifications.smoothScrollToPosition(rvPosition)
    }

    private fun scrollActions(isScrollForward: Boolean) {
        val actionRecyclerView = binding.rvNotifications.getChildAt(rvPosition)
            .findViewById<RecyclerView>(R.id.rvActions)

        var index =
            (actionRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        if (isScrollForward) {
            index--
        } else {
            index++
        }

        val notifActionsSize =
            notificationsViewModel.getNotifications().value?.get(rvPosition)?.actions?.size ?: 0
        if (index in 0 until notifActionsSize) {
            actionRecyclerView.smoothScrollToPosition(index)
        } else {
            playSoundEffect(Constants.GLASS_SOUND_DISALLOWED)
        }
    }

    private fun playSoundEffect(soundEffect: Int) {
        val audio =
            context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.playSoundEffect(soundEffect)
    }

    companion object {
        fun newInstance() = NotificationTimelineFragment()
    }
}
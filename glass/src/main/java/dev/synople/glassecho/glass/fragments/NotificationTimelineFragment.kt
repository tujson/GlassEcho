package dev.synople.glassecho.glass.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.utils.Constants
import dev.synople.glassecho.glass.EchoService
import dev.synople.glassecho.glass.utils.GlassGesture
import dev.synople.glassecho.glass.utils.GlassGestureDetector
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

    private val notificationsViewModel: NotificationsViewModel by activityViewModels()
    private var adapter: NotificationAdapter = NotificationAdapter()
    private var rvPosition = 0

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotificationTimelineBinding.bind(view)
        binding.rvNotifications.adapter = adapter

        notificationsViewModel.notifications.observe(viewLifecycleOwner, {
            adapter.setNotifications(it)

            if (it.size == 0) {
                binding.tvNumNotifications.text = getString(R.string.no_notifications)
            } else {
                binding.tvNumNotifications.text = "${rvPosition + 1}/${it.size}"
            }

            binding.executePendingBindings()
        })

        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onGesture(glassGesture: GlassGesture) {
        when (glassGesture.gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                executeNotification()
            }
            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                scrollNotifications(true)
            }
            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                scrollNotifications(false)
            }
        }
    }

    private fun executeNotification() {
        if (notificationsViewModel.notifications.value?.size == 0) {
            playSoundEffect(Constants.GLASS_SOUND_DISALLOWED)
            return
        }

        playSoundEffect(Constants.GLASS_SOUND_TAP)

        val notification = notificationsViewModel.notifications.value?.get(rvPosition) ?: return

        val navAction =
            NotificationTimelineFragmentDirections.actionNotificationTimelineFragmentToNotificationActionsFragment(
                notification.actions.toTypedArray(), notification.id
            )

        findNavController().navigate(navAction)
    }

    private fun write(message: Parcelable) {
        requireActivity().startService(Intent(activity, EchoService::class.java).apply {
            putExtra(
                Constants.EXTRA_NOTIFICATION_ACTION, message
            )
        })
    }

    @SuppressLint("SetTextI18n")
    private fun scrollNotifications(isScrollForward: Boolean) {
        if (isScrollForward && rvPosition - 1 >= 0) {
            rvPosition -= 1
        } else if (!isScrollForward && rvPosition + 1 < notificationsViewModel.size()) {
            rvPosition += 1
        } else {
            playSoundEffect(Constants.GLASS_SOUND_DISALLOWED)
        }

        if (notificationsViewModel.size() == 0) {
            binding.tvNumNotifications.text = getString(R.string.no_notifications)
        } else {
            binding.tvNumNotifications.text = "${rvPosition + 1}/${notificationsViewModel.size()}"
        }
        binding.rvNotifications.smoothScrollToPosition(rvPosition)
    }

    private fun playSoundEffect(soundEffect: Int) {
        (context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager)?.playSoundEffect(soundEffect)
    }

    companion object {
        fun newInstance() = NotificationTimelineFragment()
    }
}
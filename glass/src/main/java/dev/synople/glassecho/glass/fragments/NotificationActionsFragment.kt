package dev.synople.glassecho.glass.fragments

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.synople.glassecho.common.models.EchoNotificationAction
import dev.synople.glassecho.glass.EchoService
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.adapters.NotificationActionAdapter
import dev.synople.glassecho.glass.databinding.FragmentNotificationActionsBinding
import dev.synople.glassecho.glass.utils.Constants
import dev.synople.glassecho.glass.utils.GlassGesture
import dev.synople.glassecho.glass.utils.GlassGestureDetector
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import androidx.activity.OnBackPressedCallback as OnBackPressedCallback1

class NotificationActionsFragment : Fragment(R.layout.fragment_notification_actions) {

    private val args: NotificationActionsFragmentArgs by navArgs()
    private var _binding: FragmentNotificationActionsBinding? = null
    private val binding get() = _binding!!

    private var rvPosition = 0

    private lateinit var adapter: NotificationActionAdapter
    private lateinit var actions: MutableList<String>
    private lateinit var notificationId: String

    private val onBackPressed = object : OnBackPressedCallback1(true) {
        override fun handleOnBackPressed() {
            playSoundEffect(Constants.GLASS_SOUND_DISMISS)
            findNavController().popBackStack()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotificationActionsBinding.bind(view)

        actions = args.actions.toMutableList()
        actions.add("Dismiss")
        notificationId = args.notificationId
        adapter = NotificationActionAdapter(actions.toList())

        binding.rvNotificationActions.adapter = adapter
        binding.rvNotificationActions.setHasFixedSize(true)

        requireActivity().onBackPressedDispatcher.addCallback(onBackPressed)

        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        onBackPressed.remove()

        EventBus.getDefault().unregister(this)
    }

    @Subscribe
    fun onGesture(glassGesture: GlassGesture) {
        when (glassGesture.gesture) {
            GlassGestureDetector.Gesture.TAP -> {
                executeAction()
            }

            GlassGestureDetector.Gesture.SWIPE_FORWARD -> {
                scrollActions(true)
            }

            GlassGestureDetector.Gesture.SWIPE_BACKWARD -> {
                scrollActions(false)
            }

            else -> {}
        }
    }

    private fun executeAction() {
        if (actions[rvPosition].toLowerCase() == Constants.REPLY_KEYWORD) {
            findNavController().navigate(
                NotificationActionsFragmentDirections.actionNotificationActionsFragmentToNotificationReplyFragment(
                    notificationId,
                    actions[rvPosition]
                )
            )
            return
        }

        val echoNotificationAction = if (rvPosition == actions.size - 1) {
            playSoundEffect(Constants.GLASS_SOUND_DISMISS)
            EchoNotificationAction(notificationId, isDismiss = true)
        } else {
            playSoundEffect(Constants.GLASS_SOUND_SUCCESS)
            EchoNotificationAction(notificationId, actions[rvPosition])
        }

        write(echoNotificationAction)
        findNavController().popBackStack()
    }

    private fun scrollActions(isScrollForward: Boolean) {
        if (isScrollForward && rvPosition - 1 >= 0) {
            rvPosition -= 1
        } else if (!isScrollForward && rvPosition + 1 < actions.size) {
            rvPosition += 1
        } else {
            playSoundEffect(Constants.GLASS_SOUND_DISALLOWED)
        }

        binding.rvNotificationActions.smoothScrollToPosition(rvPosition)
    }

    private fun write(message: Parcelable) {
        requireActivity().startService(Intent(activity, EchoService::class.java).apply {
            putExtra(
                Constants.EXTRA_NOTIFICATION_ACTION, message
            )
        })
    }

    private fun playSoundEffect(soundEffect: Int) {
        (context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?)?.playSoundEffect(
            soundEffect
        )
    }
}
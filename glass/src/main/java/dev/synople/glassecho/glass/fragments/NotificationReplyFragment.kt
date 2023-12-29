package dev.synople.glassecho.glass.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dev.synople.glassecho.common.models.EchoNotificationAction
import dev.synople.glassecho.glass.EchoService
import dev.synople.glassecho.glass.R
import dev.synople.glassecho.glass.databinding.FragmentNotificationReplyBinding
import dev.synople.glassecho.glass.utils.Constants

// TODO: If we can access suggested replies, would be great to show them here

private val TAG = NotificationReplyFragment::class.java.simpleName

class NotificationReplyFragment : Fragment(R.layout.fragment_notification_reply) {

    private val args: NotificationReplyFragmentArgs by navArgs()
    private var _binding: FragmentNotificationReplyBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentNotificationReplyBinding.bind(view)

        binding.etReply.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_NULL && event.action == KeyEvent.ACTION_DOWN) {
                Log.v(TAG, "Reply: ${binding.etReply.text}")
                sendReply(binding.etReply.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }
    }

    private fun sendReply(message: String) {
        val echoNotificationAction =
            EchoNotificationAction(
                args.notificationId,
                actionName = args.action,
                remoteInput = message
            )

        requireActivity().startService(Intent(activity, EchoService::class.java).apply {
            putExtra(
                Constants.EXTRA_NOTIFICATION_ACTION, echoNotificationAction as Parcelable
            )
        })

        findNavController().navigate(NotificationReplyFragmentDirections.actionNotificationReplyFragmentToNotificationTimelineFragment())
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
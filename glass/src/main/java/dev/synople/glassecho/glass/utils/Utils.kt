package dev.synople.glassecho.glass.utils

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.glass.adapters.NotificationActionAdapter

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("bind:imageBitmap")
    fun loadImage(imageView: ImageView, bitmap: Bitmap) = imageView.setImageBitmap(bitmap)

    @JvmStatic
    @BindingAdapter("bind:notificationActionAdapter")
    fun RecyclerView.setActions(actions: List<String>) {
        adapter = NotificationActionAdapter(actions)
        setHasFixedSize(true)
    }
}

object Constants {
    const val SHARED_PREF = "dev.synople.glassecho.phone.PREFERENCE_FILE_KEY"
    const val SHARED_PREF_DEVICE_NAME = "dev.synople.glassecho.phone.sharedpref.DEVICE_NAME"
    const val SHARED_PREF_DEVICE_ADDRESS = "dev.synople.glassecho.phone.sharedpref.DEVICE_ADDRESS"

    const val EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME"
    const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS"
    const val EXTRA_BLUETOOTH_DEVICE = "EXTRA_BLUETOOTH_DEVICE"
    const val EXTRA_NOTIFICATION_ACTION = "EXTRA_NOTIFICATION_ACTION"
    const val EXTRA_DEVICE_IS_CONNECTED = "DEVICE_IS_CONNECTED"

    const val INTENT_FILTER_DEVICE_CONNECT_STATUS =
        "dev.synople.glassecho.glass.DEVICE_CONNECT_STATUS"
    const val INTENT_FILTER_NOTIFICATION = "dev.synople.glassecho.glass.NOTIFICATION"

    const val MESSAGE = "message"
    const val STATUS_MESSAGE = "statusMessage"

    const val GLASS_SOUND_SUCCESS = 12
    const val GLASS_SOUND_TAP = 13
    const val GLASS_SOUND_DISMISS = 15
    const val GLASS_SOUND_DISALLOWED = 10

    const val REPLY_KEYWORD = "reply"
}


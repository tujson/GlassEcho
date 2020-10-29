package dev.synople.glassecho.glass

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

fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}

const val GLASS_SOUND_SUCCESS = 12
const val GLASS_SOUND_TAP = 13
const val GLASS_SOUND_DISMISS = 15
const val GLASS_SOUND_DISALLOWED = 10

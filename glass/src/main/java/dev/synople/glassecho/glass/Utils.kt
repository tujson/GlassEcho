package dev.synople.glassecho.glass

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.glass.adapters.NotificationActionAdapter

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("bind:imageBitmap")
    fun loadImage(imageView: ImageView, bitmap: Bitmap) = imageView.setImageBitmap(bitmap)

    @JvmStatic
    @BindingAdapter("bind:notificationActions")
    fun RecyclerView.setActions(actions: List<String>) {
        adapter = NotificationActionAdapter(actions)
        setHasFixedSize(true)
    }
}
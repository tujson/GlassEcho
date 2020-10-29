package dev.synople.glassecho.glass

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @JvmStatic
    @BindingAdapter("bind:imageBitmap")
    fun loadImage(imageView: ImageView, bitmap: Bitmap) = imageView.setImageBitmap(bitmap)
}
package dev.synople.glassecho.common.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import android.util.Base64
import android.util.Log
import dev.synople.glassecho.common.*
import kotlinx.android.parcel.Parcelize
import java.io.ByteArrayOutputStream
import java.io.Serializable

@Parcelize
data class EchoNotification(
    val appIcon: ByteArray,
    val appName: String,
    val largeIcon: ByteArray,
    val title: String,
    val text: String
) : Parcelable, Serializable {

    constructor(
        appIcon: Bitmap,
        appName: String,
        largeIcon: Bitmap,
        title: String,
        text: String
    ) : this(
        bitmapToByteArray(appIcon), appName, bitmapToByteArray(largeIcon), title, text
    )
    
    fun getAppIconBitmap(): Bitmap =
        BitmapFactory.decodeByteArray(appIcon, 0, appIcon.size)
    
    fun getLargeIconBitmap(): Bitmap =
        BitmapFactory.decodeByteArray(largeIcon, 0, largeIcon.size)

    companion object {

        private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            return baos.toByteArray()
        }

    }
}
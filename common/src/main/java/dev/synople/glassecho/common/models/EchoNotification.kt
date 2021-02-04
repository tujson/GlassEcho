package dev.synople.glassecho.common.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.ByteArrayOutputStream
import java.io.Serializable

@Parcelize
data class EchoNotification(
    val id: String,
    val appIcon: ByteArray,
    val appName: String,
    val largeIcon: ByteArray,
    val title: String,
    val text: String,
    val actions: List<String>,
    val isRemoved: Boolean = false,
) : Parcelable, Serializable {

    constructor(
        id: String,
        appIcon: Bitmap,
        appName: String,
        largeIcon: Bitmap,
        title: String,
        text: String,
        actions: List<String>,
        isRemoved: Boolean = false,
    ) : this(
        id,
        bitmapToByteArray(appIcon),
        appName,
        bitmapToByteArray(largeIcon),
        title,
        text,
        actions,
        isRemoved,
    )

    fun getAppIconBitmap(): Bitmap =
        BitmapFactory.decodeByteArray(appIcon, 0, appIcon.size)

    fun getLargeIconBitmap(): Bitmap =
        BitmapFactory.decodeByteArray(largeIcon, 0, largeIcon.size)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EchoNotification

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString() =
        """
            ID: $id
            AppIcon size: ${appIcon.size}
            AppName: $appName
            LargeIcon size: ${largeIcon.size}
            Title: $title
            Text: $text
            Actions: $actions
            isRemoved: $isRemoved
        """.trimIndent()

    companion object {

        private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            return baos.toByteArray()
        }

    }
}
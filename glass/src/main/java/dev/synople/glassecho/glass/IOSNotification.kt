package dev.synople.glassecho.glass

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class IOSNotification(
    var uid: Int = 0,
    var title: String = "",
    var subtitle: String = "",
    var message: String = "",
    var messageSize: String = "",
    var date: String = "",
    val isAllInit: Boolean = title.isNotEmpty() && subtitle.isNotEmpty() && message.isNotEmpty() && messageSize.isNotEmpty() && date.isNotEmpty()
) : Parcelable
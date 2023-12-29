package dev.synople.glassecho.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
data class EchoNotificationAction(
    val id: String,
    val isDismiss: Boolean = false,
    val actionName: String? = null,
    val remoteInput: String? = null,
) : Parcelable, Serializable {
    constructor(id: String, actionName: String) : this(id, false, actionName, null)
}
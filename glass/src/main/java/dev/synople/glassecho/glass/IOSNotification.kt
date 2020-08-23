package dev.synople.glassecho.glass

import android.util.Log

/**
 * a notice from iPhone ANCS<br></br>
 */
class IOSNotification {
    /**
     * the unique identifier (UID) for the iOS notification
     */
    var uid = 0

    /**
     * title for the iOS notification
     */
    var title: String? = null

    /**
     * subtitle  for the iOS notification
     */
    var subtitle: String? = null

    /**
     * message(content) for the iOS notification
     */
    var message: String? = null

    /**
     * size (how many byte) of message
     */
    var messageSize: String? = null

    /**
     * the time  for the iOS notification
     */
    var date: String? = null

    constructor() {}
    constructor(t: String?, s: String?, m: String?, ms: String?, d: String?) {
        title = t
        subtitle = s
        message = m
        messageSize = ms
        date = d
    }

    val isAllInit: Boolean
        get() = title != null && subtitle != null && message != null && messageSize != null && date != null

    companion object {
        fun log(s: String?) {
            Log.i("ble", s)
        }

        fun logw(s: String?) {
            Log.w("ble", s)
        }

        fun loge(s: String?) {
            Log.e("ble", s)
        }
    }
}
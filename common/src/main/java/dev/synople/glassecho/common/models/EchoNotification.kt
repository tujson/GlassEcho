package dev.synople.glassecho.common.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import dev.synople.glassecho.common.*
import java.io.ByteArrayOutputStream

data class EchoNotification(
    val appIcon: Bitmap,
    val appName: String,
    val title: String,
    val text: String
) {
    companion object {

        fun echoNotificationToString(echoNotification: EchoNotification): String {
            val sb = StringBuilder()

            val appIconBitmap = echoNotification.appIcon
            val appIcon = bitmapToString(appIconBitmap)

            sb.append(NOTIFICATION_APP_ICON)
            sb.append(appIcon.length)
            sb.append(TAG_SEPARATOR)
            sb.append(appIcon)

            sb.append(NOTIFICATION_APP_LABEL)
            sb.append(echoNotification.appName.length)
            sb.append(TAG_SEPARATOR)
            sb.append(echoNotification.appName)

            sb.append(NOTIFICATION_TITLE)
            sb.append(echoNotification.title.length)
            sb.append(TAG_SEPARATOR)
            sb.append(echoNotification.title)

            sb.append(NOTIFICATION_TEXT)
            sb.append(echoNotification.text.length)
            sb.append(TAG_SEPARATOR)
            sb.append(echoNotification.text)

            return sb.toString()
        }

        private fun bitmapToString(bitmap: Bitmap): String {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val b: ByteArray = baos.toByteArray()
            return Base64.encodeToString(b, Base64.DEFAULT)
        }

        private fun stringToBitmap(encodedString: String): Bitmap? {
            return try {
                val encodeByte =
                    Base64.decode(encodedString, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
            } catch (e: Exception) {
                e.message
                null
            }
        }

        fun messageToEchoNotification(receivedMessage: String): EchoNotification {
            var message = receivedMessage
            var separator = message.indexOf(TAG_SEPARATOR)
            val appIconLength = message.substring(NOTIFICATION_APP_ICON.length, separator).toInt()
            val appIconTag = NOTIFICATION_APP_ICON + appIconLength.toString() + TAG_SEPARATOR
            Log.v("AppIcon", "Length: $appIconLength\nTag: $appIconTag\nlen: ${message.length}")
            val appIcon = message.substring(appIconTag.length, appIconTag.length + appIconLength)

            message = message.substring((appIconTag + appIcon).length)
            separator = message.indexOf(TAG_SEPARATOR)
            val appNameLength = message.substring(NOTIFICATION_APP_LABEL.length, separator).toInt()
            val appNameTag = NOTIFICATION_APP_LABEL + appNameLength.toString() + TAG_SEPARATOR
            val appName = message.substring(appNameTag.length, appNameTag.length + appNameLength)

            message = message.substring((appNameTag + appName).length)

            separator = message.indexOf(TAG_SEPARATOR)
            val titleLength = message.substring(NOTIFICATION_TITLE.length, separator).toInt()
            val titleTag = NOTIFICATION_TITLE + titleLength.toString() + TAG_SEPARATOR
            val title = message.substring(titleTag.length, titleTag.length + titleLength)

            message = message.substring((titleTag + title).length)

            separator = message.indexOf(TAG_SEPARATOR)
            val text = message.substring(separator + 1)
            return EchoNotification(stringToBitmap(appIcon)!!, appName, title, text)
        }

    }
}

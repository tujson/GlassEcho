package dev.synople.glassecho.phone

object Constants {
    const val SELECT_DEVICE_REQUEST_CODE = 2024

    const val PAIRED_DEVICE_REQUEST_CODE = 2311
    const val PAIRED_DEVICE_CANCEL_RESULT_CODE = 1
    const val PAIRED_DEVICE_PAIRED_RESULT_CODE = 2
    const val PAIRED_DEVICE_PAIR_RESULT_CODE = 3

    const val EXTRA_BLUETOOTH_DEVICE = "EXTRA_BLUETOOTH_DEVICE"

    const val FROM_NOTIFICATION = "fromNotification"
    const val NOTIFICATION_ACTION_STOP = "notificationActionStop"

    const val DATABASE_ECHO_APP = "echoApp.db"

    const val IS_NOTIFY_DEFAULT = true // TODO: Make user configurable
    const val IS_WAKE_SCREEN_DEFAULT = false // TODO: Make user configurable
}
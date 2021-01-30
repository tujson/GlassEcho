package dev.synople.glassecho.glass.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dev.synople.glassecho.glass.EchoApplication
import dev.synople.glassecho.glass.NotificationRepository

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private var notificationRepository: NotificationRepository =
        (application as EchoApplication).getRepository()

    val notifications = notificationRepository.notifications

    fun remove(index: Int) {
        notificationRepository.remove(index)
    }

    fun size() = (notificationRepository.notifications.value?.size ?: 0)
}


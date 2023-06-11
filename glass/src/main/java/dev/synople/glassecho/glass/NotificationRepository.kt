package dev.synople.glassecho.glass

import androidx.lifecycle.MutableLiveData
import dev.synople.glassecho.common.models.EchoNotification

class NotificationRepository {

    val notifications: MutableLiveData<MutableList<EchoNotification>> = MutableLiveData(
        mutableListOf()
    )

    fun handleNotification(echoNotification: EchoNotification) {
        val index = notifications.value?.indexOf(echoNotification) ?: -1

        if (echoNotification.isRemoved && index != -1) {
            notifications.value?.removeAt(index)
        } else {
            if (index == -1) {
                notifications.value?.add(0, echoNotification)
            } else {
                notifications.value?.set(index, echoNotification)
            }
        }

        notifications.postValue(notifications.value)
    }


    fun remove(index: Int) {
        notifications.value?.removeAt(index)
        notifications.postValue(notifications.value)
    }

    companion object {

        private var instance: NotificationRepository? = null

        fun getInstance(): NotificationRepository {
            if (instance == null) {
                instance = NotificationRepository()
            }

            return instance!!
        }
    }
}
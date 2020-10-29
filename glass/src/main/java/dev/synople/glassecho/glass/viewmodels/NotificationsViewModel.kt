package dev.synople.glassecho.glass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.synople.glassecho.common.models.EchoNotification

class NotificationsViewModel : ViewModel() {

    private val notifications: MutableLiveData<MutableList<EchoNotification>> = MutableLiveData()

    fun getNotifications(): LiveData<MutableList<EchoNotification>> = notifications

    fun handleNotification(echoNotification: EchoNotification) {
        val index = notifications.value?.indexOf(echoNotification) ?: -1

        if (echoNotification.isRemoved && index != -1) {
            notifications.value?.removeAt(index)
        } else {
            if (index == -1) {
                notifications.value?.add(echoNotification)
            } else {
                notifications.value?.set(index, echoNotification)
            }
        }
    }
}
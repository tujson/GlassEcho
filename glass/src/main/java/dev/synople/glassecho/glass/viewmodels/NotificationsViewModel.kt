package dev.synople.glassecho.glass.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.synople.glassecho.common.models.EchoNotification
import dev.synople.glassecho.glass.notifyObserver

class NotificationsViewModel : ViewModel() {

    private val notifications: MutableLiveData<MutableList<EchoNotification>> = MutableLiveData()

    init {
        notifications.value = mutableListOf()
    }

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

        notifications.notifyObserver()
    }

    fun remove(index: Int) {
        notifications.value?.removeAt(index)
        notifications.notifyObserver()
    }

    fun size() = (notifications.value?.size ?: 0).toString()
}


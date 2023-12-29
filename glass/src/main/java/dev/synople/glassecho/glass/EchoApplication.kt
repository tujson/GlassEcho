package dev.synople.glassecho.glass

import android.app.Application

class EchoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    fun getRepository() = NotificationRepository.getInstance()
}
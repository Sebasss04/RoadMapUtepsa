package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.di.AppContainer

import android.app.Application
import com.google.firebase.FirebaseApp

class RoadMapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        checkNotNull(FirebaseApp.initializeApp(this)) { "Firebase configuration is missing." }
        container = AppContainer(this)
        com.example.roadmap_utepsa.platform.notification.NotificationHelper.createChannel(this)
        com.example.roadmap_utepsa.platform.notification.RoadMapMessagingService.fetchTokenForDebug(this)
    }
}

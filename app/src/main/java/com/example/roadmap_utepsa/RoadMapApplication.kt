package com.example.roadmap_utepsa

import android.app.Application
import com.google.firebase.FirebaseApp

class RoadMapApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        checkNotNull(FirebaseApp.initializeApp(this)) { "Firebase configuration is missing." }
        container = AppContainer()
        com.example.roadmap_utepsa.services.NotificationHelper.createChannel(this)
        com.example.roadmap_utepsa.services.RoadMapMessagingService.fetchTokenForDebug(this)
    }
}

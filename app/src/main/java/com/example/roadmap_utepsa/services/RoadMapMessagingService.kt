package com.example.roadmap_utepsa.services

import android.util.Log
import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.roadmap_utepsa.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Recibe mensajes reales enviados por Firebase Cloud Messaging. */
class RoadMapMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Solo para la prueba dirigida desde Firebase Console; nunca en release.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d("FCM_TOKEN", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        NotificationHelper.show(this,
            message.notification?.title ?: message.data["titulo"] ?: message.data["title"]
                ?: getString(R.string.app_name),
            message.notification?.body ?: message.data["mensaje"] ?: message.data["body"]
                ?: getString(R.string.notification_default_body), message.messageId)
    }

    companion object {
        fun fetchTokenForDebug(context: Context) {
            val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> if (debug) Log.d("FCM_TOKEN", token) }
                .addOnFailureListener { Log.w("RoadMapFCM", "No se pudo obtener el token FCM; se reintentará al abrir la app.") }
        }
    }
}

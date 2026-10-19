package com.example.roadmap_utepsa.platform.notification

import android.util.Log
import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.roadmap_utepsa.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Recibe mensajes reales enviados por Firebase Cloud Messaging. */
class RoadMapMessagingService : FirebaseMessagingService() {
    // Keep legacy-token delivery until a separately validated FID migration.
    // Firebase 35 deprecates this callback; changing the registration model is outside local reminders.
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Solo para la prueba dirigida desde Firebase Console; nunca en release.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d("RoadMapFCM", "Registro de avisos actualizado")
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
        // register() targets FID delivery, not the existing Console token workflow.
        @Suppress("DEPRECATION")
        fun fetchTokenForDebug(context: Context) {
            val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { _ -> if (debug) Log.d("RoadMapFCM", "Registro de avisos actualizado") }
                .addOnFailureListener { Log.w("RoadMapFCM", "No se pudo obtener el token FCM; se reintentará al abrir la app.") }
        }
    }
}

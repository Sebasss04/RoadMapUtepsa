package com.example.roadmap_utepsa.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.roadmap_utepsa.MainActivity
import com.example.roadmap_utepsa.R
import java.util.concurrent.atomic.AtomicInteger

object NotificationHelper {
    const val CHANNEL_ID = "roadmap_general"
    private val nextId = AtomicInteger(1000)

    fun createChannel(context: Context) {
        // minSdk 35: todas las versiones admitidas soportan canales y permiso runtime.
        val channel = NotificationChannel(CHANNEL_ID,
            context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
        channel.description = context.getString(R.string.notification_channel_description)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showWelcome(context: Context) = show(context,
        context.getString(R.string.notification_welcome_title),
        context.getString(R.string.notification_welcome_body), "welcome")

    fun show(context: Context, title: String, body: String, tag: String? = null) {
        createChannel(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true).setContentIntent(pendingIntent).build()
        try {
            NotificationManagerCompat.from(context).notify(tag, if (tag == "welcome") 1 else nextId.incrementAndGet(), notification)
        } catch (_: SecurityException) {
            // El usuario puede revocar el permiso mientras se construye el aviso.
        }
    }
}

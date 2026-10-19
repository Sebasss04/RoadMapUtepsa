package com.example.roadmap_utepsa.platform.notification

import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.domain.notification.ReminderDelivery

import android.Manifest
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.roadmap_utepsa.R

class AndroidReminderDelivery(context: Context) : ReminderDelivery {
    private val context = context.applicationContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    companion object { const val CHANNEL = "roadmap_pending"; const val TAG = "pending_task"; const val ID = 17 }
    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.pending_channel),
            NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.pending_channel_detail)
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
    }
    override fun available() = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        manager.getNotificationChannel(CHANNEL)?.let { it.importance != NotificationManager.IMPORTANCE_NONE } == true
    override fun cancel() { manager.cancel(TAG, ID); manager.cancel("welcome", 1) }
    override fun keepOnly(route: ReminderRoute?) {
        val active = manager.activeNotifications.firstOrNull { it.tag == TAG && it.id == ID } ?: return
        if (route == null || active.notification.extras.getString(ReminderIntents.UID) != route.uid ||
            active.notification.extras.getString(ReminderIntents.KIND) != route.kind.name) cancel()
    }
    override fun show(route: ReminderRoute): Boolean {
        if (!available()) return false
        val title = context.getString(if (route.kind == ReminderKind.HISTORY) R.string.pending_history_title else R.string.pending_offer_title)
        val body = context.getString(if (route.kind == ReminderKind.HISTORY) R.string.pending_history_body else R.string.pending_offer_body)
        val pending = PendingIntent.getActivity(context, ID, ReminderIntents.create(context, route),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSilent(true).setOnlyAlertOnce(true).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(pending)
            .addExtras(android.os.Bundle().apply { putString(ReminderIntents.UID, route.uid); putString(ReminderIntents.KIND, route.kind.name) }).build()
        return try { manager.notify(TAG, ID, notification); true } catch (_: SecurityException) { false }
    }
}

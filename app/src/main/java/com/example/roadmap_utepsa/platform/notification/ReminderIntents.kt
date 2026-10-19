package com.example.roadmap_utepsa.platform.notification

import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.roadmap_utepsa.MainActivity

object ReminderIntents {
    private const val ACTION = "com.example.roadmap_utepsa.OPEN_PENDING_TASK"
    const val UID = "reminder_uid"
    const val KIND = "reminder_kind"
    fun create(context: Context, route: ReminderRoute) = Intent(context, MainActivity::class.java).apply {
        action = ACTION
        data = Uri.Builder().scheme("roadmap-internal").authority("pending").appendPath(route.uid).appendPath(route.kind.name).build()
        putExtra(UID, route.uid); putExtra(KIND, route.kind.name)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    fun parse(intent: Intent?): ReminderRoute? {
        if (intent?.action != ACTION) return null
        val uid = intent.getStringExtra(UID)?.takeIf { it.isNotBlank() } ?: return null
        val kind = ReminderKind.entries.find { it.name == intent.getStringExtra(KIND) } ?: return null
        return ReminderRoute(uid, kind)
    }
}

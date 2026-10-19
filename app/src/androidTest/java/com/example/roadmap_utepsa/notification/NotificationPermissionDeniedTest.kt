package com.example.roadmap_utepsa.notification

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.platform.notification.AndroidReminderDelivery
import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import org.junit.Assert.*
import org.junit.Test

/** Runner revokes the permission before starting this process, then restores it afterward. */
class NotificationPermissionDeniedTest {
    @Test fun permissionDeniedPreventsDeliveryWithoutRequestingIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(PackageManager.PERMISSION_DENIED, ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS))
        val delivery = AndroidReminderDelivery(context)
        assertFalse(delivery.available())
        assertFalse(delivery.show(ReminderRoute("synthetic-notification", ReminderKind.HISTORY)))
    }
}

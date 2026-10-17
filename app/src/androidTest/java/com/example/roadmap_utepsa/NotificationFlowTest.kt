package com.example.roadmap_utepsa

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.services.NotificationHelper
import org.junit.Assert.*
import org.junit.Test

class NotificationFlowTest {
    @Test
    fun welcomeHasChannelTitleIconAndAppIntent() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(NotificationManager::class.java)
        assertEquals("Conceder POST_NOTIFICATIONS antes de ejecutar esta prueba.",
            PackageManager.PERMISSION_GRANTED, ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS))
        try {
            manager.cancel("welcome", 1)
            NotificationHelper.showWelcome(context)
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000
            while (manager.activeNotifications.none { it.tag == "welcome" } &&
                android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            val welcome = manager.activeNotifications.single { it.tag == "welcome" }.notification
            assertEquals(context.getString(R.string.notification_welcome_title),
                welcome.extras.getString(android.app.Notification.EXTRA_TITLE))
            assertNotNull(welcome.contentIntent)
            assertEquals(NotificationHelper.CHANNEL_ID, welcome.channelId)
            assertTrue(welcome.smallIcon != null)
        } finally {
            manager.cancel("welcome", 1)
        }
    }
}

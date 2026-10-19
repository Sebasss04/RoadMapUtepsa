package com.example.roadmap_utepsa.notification

import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.MainActivity

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.platform.notification.AndroidReminderDelivery
import com.example.roadmap_utepsa.platform.notification.NotificationHelper
import com.example.roadmap_utepsa.platform.notification.ReminderIntents
import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.presentation.notification.ReminderRouter
import org.junit.Assert.*
import org.junit.Test

class NotificationFlowTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private fun active(tag: String, kind: String? = null): android.service.notification.StatusBarNotification {
        val end = android.os.SystemClock.elapsedRealtime() + 5000
        while (android.os.SystemClock.elapsedRealtime() < end) {
            manager.activeNotifications.firstOrNull { it.tag == tag && (kind == null ||
                it.notification.extras.getString(ReminderIntents.KIND) == kind) }?.let { return it }
            Thread.sleep(50)
        }
        error("Expected synthetic notification $tag")
    }
    private fun grant() = instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    @Test fun silentPendingChannelReplacesOneNoticeAndCarriesExpectedUidAndDestination() {
        grant(); val delivery = AndroidReminderDelivery(context)
        try {
            assertTrue(delivery.show(ReminderRoute("synthetic-notification", ReminderKind.HISTORY)))
            assertTrue(delivery.show(ReminderRoute("synthetic-notification", ReminderKind.OFFER)))
            val notification = active(AndroidReminderDelivery.TAG, "OFFER").notification
            assertEquals(1, manager.activeNotifications.count { it.tag == AndroidReminderDelivery.TAG })
            assertEquals(context.getString(R.string.pending_offer_title), notification.extras.getString(Notification.EXTRA_TITLE))
            assertEquals(context.getString(R.string.pending_offer_body), notification.extras.getString(Notification.EXTRA_TEXT))
            assertEquals("synthetic-notification", notification.extras.getString(ReminderIntents.UID))
            assertEquals("OFFER", notification.extras.getString(ReminderIntents.KIND))
            assertTrue(notification.contentIntent.isImmutable); assertNotNull(notification.smallIcon)
            val channel = manager.getNotificationChannel(notification.channelId)
            assertEquals(AndroidReminderDelivery.CHANNEL, channel.id)
            assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
            assertNull(channel.sound); assertFalse(channel.shouldVibrate())
            delivery.keepOnly(ReminderRoute("another-account", ReminderKind.OFFER))
            val end = android.os.SystemClock.elapsedRealtime() + 5000
            while (manager.activeNotifications.any { it.tag == AndroidReminderDelivery.TAG } &&
                android.os.SystemClock.elapsedRealtime() < end) Thread.sleep(50)
            assertTrue(manager.activeNotifications.none { it.tag == AndroidReminderDelivery.TAG })
        } finally { delivery.cancel() }
    }
    @Test fun remoteReceptionHelperStillUsesItsOwnChannel() {
        grant()
        try {
            NotificationHelper.show(context, "Aviso remoto sintético", "Contenido de prueba", "remote-fixture")
            val notification = active("remote-fixture").notification
            assertEquals(NotificationHelper.CHANNEL_ID, notification.channelId)
            assertNotNull(notification.contentIntent); assertNotNull(notification.smallIcon)
        } finally { manager.activeNotifications.filter { it.tag == "remote-fixture" }.forEach { manager.cancel(it.tag, it.id) } }
    }
    @Test fun activityHandlesColdIntentNewIntentAndConsumedRotationWithoutPrivateSession() {
        assertNull("Use only the isolated synthetic AVD with no primary Auth session", com.google.firebase.auth.FirebaseAuth.getInstance().currentUser)
        val first = ReminderRoute("synthetic-notification", ReminderKind.HISTORY)
        val activity = instrumentation.startActivitySync(ReminderIntents.create(context, first)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        try {
            instrumentation.runOnMainSync { assertEquals(first, ViewModelProvider(activity)[ReminderRouter::class.java].route.value) }
            val second = ReminderRoute("synthetic-notification", ReminderKind.OFFER)
            context.startActivity(ReminderIntents.create(context, second).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().single()
                assertSame(activity, resumed)
                val router = ViewModelProvider(resumed)[ReminderRouter::class.java]
                assertEquals(second, router.route.value); router.consume(); resumed.recreate()
            }
            // ActivityScenario filters lifecycle callbacks by the original Intent URI. A real
            // onNewIntent changes that URI, so observe the actual Activity lifecycle instead.
            var recreated: MainActivity? = null
            val end = android.os.SystemClock.elapsedRealtime() + 10000
            while (recreated == null && android.os.SystemClock.elapsedRealtime() < end) {
                instrumentation.runOnMainSync {
                    recreated = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .filterIsInstance<MainActivity>().firstOrNull { it !== activity }
                }
                if (recreated == null) Thread.sleep(50)
            }
            assertNotNull("Expected a new resumed Activity after real recreation", recreated)
            instrumentation.runOnMainSync { assertNull(ViewModelProvider(recreated!!)[ReminderRouter::class.java].route.value) }
        } finally {
            instrumentation.runOnMainSync {
                listOf(Stage.RESUMED, Stage.STARTED, Stage.PAUSED).flatMap {
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it)
                }.filterIsInstance<MainActivity>().distinct().forEach { it.finish() }
            }
            instrumentation.waitForIdleSync()
        }
    }

    @Test fun invalidNotificationRoutesAreIgnored() {
        assertNull(ReminderIntents.parse(Intent()))
        val intent = ReminderIntents.create(context, ReminderRoute("synthetic", ReminderKind.HISTORY))
        intent.putExtra(ReminderIntents.KIND, "UNKNOWN"); assertNull(ReminderIntents.parse(intent))
        intent.putExtra(ReminderIntents.KIND, "HISTORY"); intent.putExtra(ReminderIntents.UID, "")
        assertNull(ReminderIntents.parse(intent))
    }
}

package com.example.roadmap_utepsa.notification

import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.data.notification.DeviceReminderPreferences
import com.example.roadmap_utepsa.domain.notification.ReminderMemory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Run the same test twice with an app force-stop between seed/check, only on the synthetic AVD. */
class ReminderProcessPersistenceTest {
    @Test fun memorySurvivesARealProcessRestart(): Unit = runBlocking {
        val prefs = DeviceReminderPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        val uid = "notification-process-fixture"
        when (InstrumentationRegistry.getArguments().getString("reminder_persistence_phase")) {
            "seed" -> { prefs.update(uid) { ReminderMemory(true, true, true) }; assertTrue(prefs.read(uid).historySeen) }
            "check" -> { assertEquals(ReminderMemory(true, true, true), prefs.read(uid)); prefs.update(uid) { ReminderMemory() } }
            else -> error("Select seed/check explicitly; do not run the entire Android suite")
        }
    }
}

package com.example.roadmap_utepsa.notification

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.presentation.account.AccountScreen
import com.example.roadmap_utepsa.presentation.account.AccountState
import com.example.roadmap_utepsa.presentation.notification.PendingReminders
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.domain.notification.ReminderMemory
import com.example.roadmap_utepsa.domain.notification.ReminderPreferences
import com.example.roadmap_utepsa.domain.notification.ReminderDelivery
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import org.junit.*
import org.junit.Assert.*

class ReminderPreferenceUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun optInExplainsBeforeEnablingAndFitsLargeFont() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.POST_NOTIFICATIONS)
        var memory = ReminderMemory()
        lateinit var controller: PendingReminders
        compose.runOnUiThread {
            controller = PendingReminders(RecomendacionesRepository { error("No academic reads from Account") },
                object : ReminderPreferences {
                    override suspend fun read(uid: String) = memory
                    override suspend fun update(uid: String, change: (ReminderMemory) -> ReminderMemory) = change(memory).also { memory = it }
                }, object : ReminderDelivery {
                    override fun available() = true
                    override fun show(route: ReminderRoute) = error("No delivery from Account")
                    override fun cancel() {}
                    override fun keepOnly(route: ReminderRoute?) {}
                }) { "preference-demo" to 1L }
            controller.bind("preference-demo", 1)
        }
        compose.setContent { RoadMap_UtepsaTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                AccountScreen(AuthIdentity("preference-demo", "demo@example.test", true, setOf("google.com")), null,
                    AccountState(), null, "Malla", {}, {}, {}, {}, { _, _ -> }, {}, {}, reminders = controller)
            }
        } }
        compose.onNodeWithTag("pending-toggle").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithText("Activar avisos").assertIsDisplayed()
        compose.runOnIdle { assertFalse(memory.enabled) }
        compose.onNodeWithText("Ahora no").performClick()
        compose.onNodeWithTag("pending-toggle").assertIsOff().performClick()
        compose.onNodeWithText("Activar avisos").performClick()
        compose.onNodeWithTag("pending-toggle").assertIsOn().performClick()
        compose.onNodeWithTag("pending-toggle").assertIsOff()
        compose.onNodeWithText("Recibe un aviso para completar tu historial o registrar tu oferta.").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertFalse(memory.enabled) }
        val dir = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "quality-reminder-captures").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(dir, "account-reminders-font-1.5.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}

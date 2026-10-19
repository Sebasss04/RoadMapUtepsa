package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.platform.biometric.BiometricUnlock
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import android.view.WindowManager
import com.example.roadmap_utepsa.presentation.session.SessionLock
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.collectAsState
import com.example.roadmap_utepsa.presentation.notification.PendingReminders
import com.example.roadmap_utepsa.presentation.notification.ReminderRouter
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme

class MainActivity : FragmentActivity() {
    private lateinit var sessionLock: SessionLock
    private lateinit var biometricUnlock: BiometricUnlock
    private lateinit var reminders: PendingReminders
    private lateinit var reminderRouter: ReminderRouter
    private var foreground by mutableStateOf(false)
    private var resumeVersion by mutableLongStateOf(0)

    override fun onResume() { super.onResume(); foreground = true; resumeVersion++ }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        reminderRouter.receive(intent)
    }

    override fun onStop() {
        foreground = false
        if (::reminders.isInitialized) reminders.pause()
        if (::sessionLock.isInitialized) sessionLock.background(isChangingConfigurations)
        super.onStop()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        val container = (application as RoadMapApplication).container
        val googleAuth = container.googleAuth(this)
        sessionLock = ViewModelProvider(this, container.sessionLockFactory())[SessionLock::class.java]
        biometricUnlock = BiometricUnlock(this, sessionLock)
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModel.Factory(container.authRepository)
        )[AuthViewModel::class.java]
        reminders = ViewModelProvider(this, container.remindersFactory {
            authViewModel.state.value.let { it.uid to it.sessionVersion }
        })[PendingReminders::class.java]
        reminderRouter = ViewModelProvider(this, viewModelFactory { initializer {
            ReminderRouter(createSavedStateHandle())
        } })[ReminderRouter::class.java]
        if (savedInstanceState == null) reminderRouter.receive(intent)
        lifecycleScope.launch {
            authViewModel.state.collect {
                sessionLock.bind(it.uid, it.sessionVersion, it.interactiveEntry)
                reminders.bind(it.uid, it.sessionVersion)
            }
        }
        lifecycleScope.launch {
            sessionLock.state.collect {
                if (it.enabled || it.loading || it.locked) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        setContent {
            RoadMap_UtepsaTheme {
                val route by reminderRouter.route.collectAsState()
                AppNavigation(authViewModel, this@MainActivity, container.profileRepository,
                    container.academicRepository, container.authRepository, container.preparacionRepository, container.ofertaRepository,
                    container.recomendacionesRepository, sessionLock, biometricUnlock::authenticate,
                    reminders = reminders, notificationRoute = route, onNotificationHandled = reminderRouter::consume,
                    foreground = foreground, resumeVersion = resumeVersion,
                    getGoogleToken = googleAuth::getGoogleIdToken)
            }
        }
    }
}

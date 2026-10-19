package com.example.roadmap_utepsa.account

import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.offer.ofertaFixture

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.auth.EmailFormScreen
import com.example.roadmap_utepsa.presentation.account.AccountScreen
import com.example.roadmap_utepsa.presentation.session.LockScreen
import com.example.roadmap_utepsa.presentation.account.AccountState
import com.example.roadmap_utepsa.presentation.auth.AuthUiState
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.session.LockState
import com.example.roadmap_utepsa.presentation.session.SessionLock
import com.example.roadmap_utepsa.domain.session.LockPreferences
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Synthetic UI only. No real credentials, providers, or personal writes. */
class AccountFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class Auth(verified: Boolean = true) : AuthRepository {
        val identities = MutableStateFlow<AuthIdentity?>(AuthIdentity("account-demo", "real-fixture@example.test", verified, setOf("password")))
        override val currentIdentity get() = identities.value
        override val identityChanges = identities
        override val currentUid get() = currentIdentity?.uid
        override val isAuthenticated get() = currentUid != null
        override val uidChanges = identities.map { it?.uid }
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) = error("No real Google")
        var verificationSends = 0
        override suspend fun register(email: String, password: String) {
            identities.value = AuthIdentity("account-demo", email, false, setOf("password"))
            kotlinx.coroutines.delay(100)
        }
        override suspend fun sendVerification() { verificationSends++ }
        override fun signOut() { identities.value = null }
    }
    private fun start(auth: Auth = Auth(), lock: SessionLock? = null): () -> Int {
        var profileLoads = 0
        val profile = object : ProfileRepository {
            override suspend fun load(uid: String): ProfileData { profileLoads++; return ProfileData("Nombre real", "Apellido real",
                AssignedStudyPlan("sistemas-rm-615-2016", "Ingeniería de Sistemas", "615/2016"), ProfileRegistration.COMPLETE) }
            override suspend fun save(uid: String, names: String, surnames: String) = load(uid).copy(names = names, surnames = surnames)
        }
        val academic = object : AcademicRepository {
            override suspend fun load(uid: String) = ofertaFixture().copy(uid = uid)
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> = error("No writes")
        }
        compose.setContent { RoadMap_UtepsaTheme {
            AppNavigation(remember { AuthViewModel(auth) }, compose.activity, profile, academic, auth, sessionLock = lock)
        } }
        return { profileLoads }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "account-ui-captures").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
    }
    @Test fun accountUsesRealIdentityAndProfileReturnsToOriginWithSearch() {
        start()
        compose.onNodeWithText("Buscar materia o sigla").performTextReplacement("algebra")
        compose.onNodeWithContentDescription("Tu cuenta").performClick()
        compose.onNodeWithText("Tu cuenta").assertIsDisplayed()
        compose.onNodeWithText("Nombre real Apellido real").assertIsDisplayed()
        compose.onNodeWithText("real-fixture@example.test").assertIsDisplayed()
        compose.onNodeWithText("Google · Sin vincular").assertIsDisplayed(); capture("01-cuenta")
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithText("Volver a Cuenta").performClick()
        compose.onNodeWithText("Tu cuenta").assertIsDisplayed(); compose.onNodeWithTag("account-back").performClick()
        compose.onNodeWithText("Buscar materia o sigla").assertTextContains("algebra")
    }
    @Test fun verificationPrecedesAllPrivateReads() {
        val auth = Auth(false); val loads = start(auth)
        compose.onNodeWithText("Verifica tu correo").assertIsDisplayed(); assertEquals(0, loads())
        compose.onNodeWithText("Ya verifiqué").assertIsDisplayed()
        compose.onNodeWithText("Reenviar correo").assertIsDisplayed(); capture("02-verificacion")
        compose.runOnIdle { auth.identities.value = auth.currentIdentity!!.copy(emailVerified = true) }
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed(); assertEquals(1, loads())
    }
    @Test fun registrationValidatesWithoutSendingInvalidData() {
        var calls = 0
        compose.setContent { RoadMap_UtepsaTheme { EmailFormScreen(true, AuthUiState(), {}) { _, _ -> calls++ } } }
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Introduce un correo válido.").assertExists(); assertEquals(0, calls)
        capture("03-registro-validacion")
    }
    @Test fun recoveryDisplaysNeutralResponseAndGoogleHelp() {
        compose.setContent { RoadMap_UtepsaTheme { EmailFormScreen(false,
            AuthUiState(notice = R.string.auth_recovery_neutral), {}) { _, _ -> } } }
        compose.onNodeWithText("Si ese correo admite recuperación, recibirás un enlace. Revisa también spam.").assertIsDisplayed()
        compose.onNodeWithText("¿Ingresas con Google?").assertExists(); capture("04-recuperacion")
    }
    @Test fun conflictOffersRefreshAndDoesNotClaimSuccess() {
        compose.setContent { RoadMap_UtepsaTheme { AccountScreen(AuthIdentity("a", "a@example.test", true, setOf("password")), null,
            AccountState(message = R.string.access_method_in_use, error = true, reconciled = false), null,
            "Malla", {}, {}, {}, {}, { _, _ -> }, {}, {}) } }
        compose.onNodeWithText("Ese método de acceso ya está asociado a otra cuenta. Vuelve o elige otra cuenta.").assertIsDisplayed()
        compose.onNodeWithText("Vincular Google").assertIsNotEnabled(); capture("05-conflicto")
    }
    @Test fun protectedSessionShowsOnlyUnlockAndSignOut() {
        compose.setContent { RoadMap_UtepsaTheme { LockScreen(LockState(uid = "a", enabled = true, locked = true,
            message = R.string.biometric_cancelled), {}, {}) } }
        compose.onNodeWithText("Desbloquear").assertIsEnabled()
        compose.onNodeWithText("Cerrar sesión e ir al acceso").assertIsDisplayed(); capture("06-bloqueo")
    }
    @Test fun cancellingProfileDiscardsAndReturnsToAccount() {
        start(); compose.onNodeWithContentDescription("Tu cuenta").performClick()
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithText("Nombres").performTextReplacement("Borrador")
        compose.onNodeWithText("Descartar cambios").performScrollTo().performClick()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Tu cuenta").assertIsDisplayed()
        compose.onNodeWithText("Nombre real Apellido real").assertIsDisplayed()
    }
    @Test fun savingProfileReturnsToAccountWithSavedNames() {
        start(); compose.onNodeWithContentDescription("Tu cuenta").performClick()
        compose.onNodeWithText("Editar perfil").performClick()
        compose.onNodeWithText("Nombres").performTextReplacement("Nombre actualizado")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.onNodeWithText("Tu cuenta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nombre actualizado Apellido real").assertIsDisplayed()
    }
    @Test fun localLockKeepsMountedNavigationAndSearch() {
        val prefs = object : LockPreferences {
            override suspend fun enabled(uid: String) = true
            override suspend fun setEnabled(uid: String, enabled: Boolean) { }
        }
        lateinit var lock: SessionLock
        compose.runOnUiThread { lock = SessionLock(prefs); lock.bind("account-demo", 0, 1) }
        start(lock = lock)
        compose.onNodeWithText("Buscar materia o sigla").performTextReplacement("algebra")
        compose.runOnUiThread { lock.background(false) }
        compose.onNodeWithText("Desbloquea RoadMap").assertIsDisplayed()
        compose.runOnUiThread { lock.beginPrompt(false); lock.authenticated() }
        compose.onNodeWithText("Buscar materia o sigla").assertTextContains("algebra")
    }

    @Test fun neutralLinkFailureAtLargeFontKeepsCurrentAccessAndRefreshAction() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                RoadMap_UtepsaTheme {
                    AccountScreen(AuthIdentity("a", "a@example.test", true, setOf("password")), null,
                        AccountState(message = R.string.access_email_exists, error = true, reconciled = false),
                        null, "Malla", {}, {}, {}, {}, { _, _ -> }, {}, {})
                }
            }
        }
        compose.onNodeWithText("No pudimos añadir este método de acceso. Puedes seguir entrando como antes. Actualiza tus métodos antes de volver a intentarlo.")
            .performScrollTo().assertIsDisplayed()
        capture("07-email-exists-fuente-15")
        compose.onNodeWithText("Actualizar métodos").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Vincular Google").assertIsNotEnabled()
    }

    @Test fun linkDialogAtLargeFontValidatesBeforeSendingAndCanReturn() {
        var calls = 0
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 1.5f)) {
                RoadMap_UtepsaTheme {
                    AccountScreen(AuthIdentity("a", "a@example.test", true, setOf("google.com")), null,
                        AccountState(), null, "Malla", {}, {}, {}, {}, { _, _ -> calls++ }, {}, {})
                }
            }
        }
        compose.onNodeWithText("Añadir contraseña").performScrollTo().performClick()
        compose.onNodeWithText("Confirmar acceso y vincular").assertIsDisplayed().performClick()
        assertEquals(0, calls)
        compose.onAllNodesWithText("Usa al menos ocho caracteres. Si la contraseña no se acepta, prueba con una más segura.")
            .assertCountEquals(2)
        compose.onAllNodesWithText("Usa al menos ocho caracteres. Si la contraseña no se acepta, prueba con una más segura.")[1]
            .performScrollTo().assertIsDisplayed()
        capture("08-vinculacion-dialogo-fuente-15")
        compose.onNodeWithText("Volver").performClick()
        compose.onNodeWithText("Tu cuenta").performScrollTo().assertIsDisplayed()
    }

    @Test fun registrationFinishesVerificationSendAfterEntryNavigationLeaves() {
        val auth = Auth().apply { identities.value = null }
        val loads = start(auth)
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Correo electrónico").performTextInput("a@example.test")
        compose.onNodeWithText("Contraseña").performTextInput("12345678")
        compose.onNodeWithText("Confirmar contraseña").performTextInput("12345678")
        compose.onNodeWithText("Crear cuenta").performScrollTo().performClick()
        compose.waitUntil(5_000) { auth.verificationSends == 1 }
        compose.onNodeWithText("Verifica tu correo").assertIsDisplayed()
        assertEquals(0, loads())
    }
}

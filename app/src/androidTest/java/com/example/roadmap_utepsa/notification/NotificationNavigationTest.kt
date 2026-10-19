package com.example.roadmap_utepsa.notification

import com.example.roadmap_utepsa.offer.ofertaFixture

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.offer.OfertaRepository
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.domain.offer.DatosOferta
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.offer.OfertaViewModel
import com.example.roadmap_utepsa.presentation.session.SessionLock
import com.example.roadmap_utepsa.domain.session.LockPreferences
import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*

/** Entirely synthetic repositories; no personal Firebase session or writes. */
class NotificationNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = ofertaFixture().copy(uid = "notice-demo")
    private var route by mutableStateOf<ReminderRoute?>(null)
    private var loads = 0
    private var offerLoads = 0
    private lateinit var authVm: AuthViewModel
    private class Auth(verified: Boolean) : AuthRepository {
        val identities = MutableStateFlow<AuthIdentity?>(AuthIdentity("notice-demo", "demo@example.test", verified, setOf("password")))
        override val currentIdentity get() = identities.value
        override val identityChanges = identities
        override val currentUid get() = currentIdentity?.uid
        override val uidChanges = identities.map { it?.uid }
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) = error("No Google")
        override fun signOut() { identities.value = null }
    }
    private fun start(kind: ReminderKind?, verified: Boolean = true, lock: SessionLock? = null,
        expectedUid: String = fixture.uid, authenticated: Boolean = true, completeProfile: Boolean = true): Auth {
        route = kind?.let { ReminderRoute(expectedUid, it) }
        val auth = Auth(verified)
        if (!authenticated) auth.identities.value = null
        val academic = object : AcademicRepository {
            override suspend fun load(uid: String) = fixture.copy(uid = uid)
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> = error("No writes")
        }
        val profile = object : ProfileRepository {
            override suspend fun load(uid: String): ProfileData { loads++; return ProfileData("Prueba", "Local",
                AssignedStudyPlan(fixture.idPlan, "Sistemas", "615/2016"),
                if (completeProfile) ProfileRegistration.COMPLETE else ProfileRegistration.INCOMPLETE) }
            override suspend fun save(uid: String, names: String, surnames: String) = ProfileData(names, surnames,
                AssignedStudyPlan(fixture.idPlan, "Sistemas", "615/2016"), ProfileRegistration.COMPLETE)
        }
        val prep = object : PreparacionRepository {
            override suspend fun load(uid: String) = LecturaPreparacion(fixture.copy(uid = uid), RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1"), null)
            override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion = error("No writes")
            override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion = error("No writes")
            override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No writes")
            override suspend fun reanudar(uid: String, idOperacion: String, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No writes")
        }
        val offer = object : OfertaRepository {
            override suspend fun load(uid: String): DatosOferta { offerLoads++; return DatosOferta(uid, fixture.idPlan, fixture.materias, Oferta(fixture.idPlan, emptySet(), 1)) }
            override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta = error("No writes")
        }
        compose.setContent { RoadMap_UtepsaTheme {
            authVm = remember { AuthViewModel(auth) }
            AppNavigation(authVm, compose.activity, profile, academic, auth, prep, offer, sessionLock = lock,
                notificationRoute = route, onNotificationHandled = { route = null })
        } }
        return auth
    }
    @Test fun coldHistoryRouteOpensHistoryInsteadOfDefaultCurriculum() {
        start(ReminderKind.HISTORY)
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        compose.runOnIdle { assertNull(route) }
    }
    @Test fun coldOfferRouteOpensTheSavedOffer() {
        start(ReminderKind.OFFER)
        compose.onNodeWithText("Añadir materias").assertExists()
        compose.runOnIdle { assertNull(route) }
    }
    @Test fun routeWaitsForEmailVerificationBeforePrivateReads() {
        val auth = start(ReminderKind.OFFER, verified = false)
        compose.onNodeWithText("Verifica tu correo").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, loads); assertNotNull(route); auth.identities.value = auth.currentIdentity!!.copy(emailVerified = true) }
        compose.onNodeWithText("Añadir materias").assertExists()
        compose.runOnIdle { assertNull(route) }
    }
    @Test fun anotherAccountsRouteIsDiscarded() {
        start(ReminderKind.OFFER, expectedUid = "another-account")
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        compose.onNodeWithText("Añadir materias").assertDoesNotExist()
        compose.runOnIdle { assertNull(route) }
    }
    @Test fun routeWaitsForMatchingAuthentication() {
        val auth = start(ReminderKind.OFFER, authenticated = false)
        compose.runOnIdle {
            assertEquals(0, loads); assertEquals(0, offerLoads); assertNotNull(route)
            auth.identities.value = AuthIdentity(fixture.uid, "demo@example.test", true, setOf("password"))
        }
        compose.onNodeWithText("Añadir materias").assertExists()
        compose.runOnIdle { assertNull(route) }
    }
    @Test fun incompleteProfileIsCompletedBeforeRoutingToOffer() {
        start(ReminderKind.OFFER, completeProfile = false)
        compose.onNodeWithText("Completa tu perfil").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, offerLoads); assertNotNull(route) }
        compose.onNodeWithText("Guardar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Añadir materias").assertExists()
        compose.runOnIdle { assertNull(route); assertTrue(offerLoads > 0) }
    }
    @Test fun lockedRouteStaysPendingUntilBiometricGateOpens() {
        lateinit var lock: SessionLock
        compose.runOnUiThread {
            lock = SessionLock(object : LockPreferences {
                override suspend fun enabled(uid: String) = true
                override suspend fun setEnabled(uid: String, enabled: Boolean) {}
            })
            lock.bind(fixture.uid, 0, 0)
        }
        start(ReminderKind.OFFER, lock = lock)
        compose.onNodeWithText("Desbloquear").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, loads); assertNotNull(route); assertTrue(lock.beginPrompt(false)); lock.authenticated() }
        compose.onNodeWithText("Añadir materias").assertExists()
        compose.runOnIdle { assertNull(route) }
    }
    @Test fun warmHistoryAndOfferRoutesKeepOfferDraftSearchAndExpectedRevision() {
        start(ReminderKind.OFFER)
        compose.onNodeWithText("Añadir materias").performScrollTo().performClick()
        val subject = fixture.materias.first()
        compose.onNodeWithTag("offer-search").performTextReplacement(subject.codigo)
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText(subject.nombre))
        compose.onNodeWithText(subject.nombre).assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.runOnIdle { route = ReminderRoute(fixture.uid, ReminderKind.HISTORY) }
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        compose.runOnIdle { route = ReminderRoute(fixture.uid, ReminderKind.OFFER) }
        compose.onNodeWithTag("offer-search").assertTextContains(subject.codigo)
        compose.runOnIdle {
            val vm = ViewModelProvider(compose.activity).get(OfertaViewModel.key(fixture.uid, authVm.state.value.sessionVersion), OfertaViewModel::class.java)
            assertEquals(setOf(subject.id), vm.state.value.draft)
            assertEquals(1L, vm.state.value.data?.oferta?.revision)
            assertTrue(vm.state.value.dirty)
        }
    }
}

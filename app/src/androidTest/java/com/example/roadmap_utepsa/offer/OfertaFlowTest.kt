package com.example.roadmap_utepsa.offer

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.offer.OfertaRepository
import com.example.roadmap_utepsa.domain.offer.ConflictoOferta
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.domain.offer.DatosOferta
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.presentation.offer.OfertaScreen
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.offer.OfertaUiState
import com.example.roadmap_utepsa.presentation.offer.OfertaViewModel
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** No default Firebase auth or real student data. */
class OfertaFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val data = ofertaFixture()
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("offer-test")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) {}
        override fun signOut() { uidChanges.value = null }
    }
    private inner class Repo : OfertaRepository {
        var offer: Oferta? = null
        var failRead = false
        var failSave = false
        var gate: CompletableDeferred<Unit>? = null
        var writes = 0
        var states: Map<String, EstadoAcademico> = emptyMap()
        override suspend fun load(uid: String): DatosOferta {
            if (failRead) error("offline")
            return DatosOferta(uid, data.idPlan, data.materias, offer, states)
        }
        override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta {
            writes++; gate?.await()
            if (failSave) error("offline")
            if (offer?.revision != expectedRevision) throw ConflictoOferta(offer)
            return Oferta(data.idPlan, materias.toSet(), (expectedRevision ?: 0) + 1).also { offer = it }
        }
    }
    private fun start(repo: Repo, font: Float = 1f): OfertaViewModel {
        val auth = Auth()
        lateinit var vm: OfertaViewModel
        compose.runOnUiThread {
            compose.activity.enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(0,0), navigationBarStyle = SystemBarStyle.light(0,0))
            vm = OfertaViewModel("offer-test", 1, repo, auth) { 1L }
        }
        compose.setContent {
            RoadMap_UtepsaTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, font)) {
                    val s by vm.state.collectAsState()
                    OfertaScreen(s, vm::reload, vm::edit, vm::query, vm::toggle, vm::save, vm::cancel,
                        vm::resolveConflict, {}, {}, auth::signOut)
                }
            }
        }
        compose.waitForIdle()
        return vm
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(400)
        val i = InstrumentationRegistry.getInstrumentation()
        val folder = File(i.targetContext.getExternalFilesDir(null), "offer-ui-captures").apply { mkdirs() }
        val bitmap = i.uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
    }
    @Test fun absenceEmptyEditorSavingErrorAndSuccess() {
        val repo = Repo(); val vm = start(repo)
        compose.onNodeWithText("Añade tu primera oferta").assertIsDisplayed(); screenshot("01-ausente")
        compose.onNodeWithText("Seleccionar materias").performClick()
        compose.onNodeWithTag("offer-save").performClick()
        compose.onNodeWithText("Tu oferta está vacía").assertIsDisplayed(); screenshot("02-vacia")
        compose.onNodeWithText("Añadir materias").performClick()
        compose.onNodeWithTag("offer-search").performTextInput("bms-300")
        compose.onNodeWithTag("offer-search").performImeAction()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText("Introducción a las Matemáticas"))
        compose.onNodeWithText("Introducción a las Matemáticas").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }; screenshot("03-editor")
        repo.gate = CompletableDeferred(); compose.onNodeWithTag("offer-save").performClick()
        compose.onNodeWithText("Guardando…").assertIsNotEnabled(); screenshot("04-guardando")
        repo.failSave = true; repo.gate?.complete(Unit); compose.waitForIdle()
        compose.onNodeWithText("No pudimos guardar tu oferta").assertIsDisplayed(); screenshot("05-error")
        assertEquals(1, vm.state.value.draft.size)
        repo.failSave = false; compose.onNodeWithTag("offer-save").performClick()
        compose.onNodeWithText("Oferta guardada").assertIsDisplayed(); screenshot("06-guardada")
        assertEquals(1, repo.offer?.materias?.size)
    }
    @Test fun conflictPreservesDraftAndLoadsRemoteExplicitly() {
        val repo = Repo(); val vm = start(repo)
        compose.onNodeWithText("Seleccionar materias").performClick()
        compose.runOnUiThread { vm.toggle(data.materias.first().id) }
        repo.offer = Oferta(data.idPlan, setOf(data.materias.last().id), 1)
        compose.onNodeWithTag("offer-save").performClick()
        compose.onNodeWithText("La oferta cambió en otra sesión").assertIsDisplayed(); screenshot("07-conflicto")
        assertEquals(setOf(data.materias.first().id), vm.state.value.draft)
        compose.onNodeWithText("Usar la oferta guardada").performClick()
        assertEquals(repo.offer?.materias, vm.state.value.draft); assertEquals(1, repo.writes)
    }
    @Test fun cancelModifiedSelectionRequiresConfirmation() {
        val repo = Repo(); val vm = start(repo)
        compose.onNodeWithText("Seleccionar materias").performClick()
        compose.runOnUiThread { vm.toggle(data.materias.last().id) }
        compose.onNodeWithText("Cancelar edición").performClick()
        compose.onNodeWithText("¿Descartar los cambios?").assertIsDisplayed()
        compose.onNodeWithText("Seguir aquí").performClick(); assertTrue(vm.state.value.dirty)
        compose.onNodeWithText("Cancelar edición").performClick(); compose.onNodeWithText("Confirmar").performClick()
        compose.onNodeWithText("Añade tu primera oferta").assertIsDisplayed(); assertEquals(0, repo.writes)
    }
    @Test fun largeFontLongNamesAndSearchEmptyRemainUsable() {
        val repo = Repo(); start(repo, 1.5f)
        compose.onNodeWithText("Seleccionar materias").performScrollTo().performClick()
        compose.onNodeWithTag("offer-search").performTextInput("organizacion")
        compose.onNodeWithTag("offer-search").performImeAction()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText("Organización y Arquitectura de Computadoras"))
        compose.onNodeWithText("Organización y Arquitectura de Computadoras").performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        screenshot("08-fuente-ampliada")
        compose.onNodeWithTag("offer-search").performTextReplacement("zzzz")
        compose.onNodeWithTag("offer-search").performImeAction()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText("No hay materias que coincidan con tu búsqueda."))
        compose.onNodeWithText("No hay materias que coincidan con tu búsqueda.").assertIsDisplayed()
        compose.onNodeWithTag("offer-save").assertIsEnabled().performClick()
        assertEquals(1, repo.offer?.materias?.size)
    }
    @Test fun loadingHasExplicitProgressAndNoAbsentOffer() {
        compose.setContent { RoadMap_UtepsaTheme {
            OfertaScreen(OfertaUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Cargando oferta…").assertIsDisplayed()
        compose.onNodeWithText("Añade tu primera oferta").assertDoesNotExist()
        screenshot("10-carga")
    }
    @Test fun readErrorDoesNotShowAbsence() {
        start(Repo().apply { failRead = true })
        compose.onNodeWithText("No pudimos leer tu oferta. Reintenta para conocer su estado.").assertIsDisplayed()
        compose.onNodeWithText("Añade tu primera oferta").assertDoesNotExist(); screenshot("09-error-lectura")
    }
    @Test fun approvedSearchExcludedWhileCurrentAndPendingRemainSelectable() {
        val approved = data.materias.first()
        val current = data.materias[1]
        val repo = Repo().apply { states = mapOf(approved.id to EstadoAcademico.APROBADA, current.id to EstadoAcademico.EN_CURSO) }
        val vm = start(repo)
        compose.onNodeWithText("Seleccionar materias").performScrollTo().performClick()
        compose.onNodeWithTag("offer-search").performTextInput(approved.codigo)
        compose.onNodeWithTag("offer-search").performImeAction()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText("No hay materias que coincidan con tu búsqueda."))
        compose.onNodeWithText(approved.nombre).assertDoesNotExist()
        compose.onNodeWithTag("offer-search").performTextReplacement(current.codigo)
        compose.onNodeWithTag("offer-search").performImeAction()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText(current.nombre))
        compose.onNodeWithText(current.nombre).performClick()
        compose.onNodeWithTag("offer-save").performClick()
        assertEquals(setOf(current.id), repo.offer?.materias)
        assertTrue(vm.state.value.saved)
    }
    @Test fun savedApprovedRemovalIsExplainedAndAllApprovedShowsEmptySelector() {
        val approved = data.materias.first()
        val repo = Repo().apply {
            states = mapOf(approved.id to EstadoAcademico.APROBADA)
            offer = Oferta(data.idPlan, setOf(approved.id), 1)
        }
        val vm = start(repo, 1.5f)
        compose.onNodeWithText("Editar materias").performScrollTo().performClick()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText("Tu historial se actualizó"))
        compose.onNodeWithText("Tu historial se actualizó").assertIsDisplayed()
        assertTrue(vm.state.value.draft.isEmpty())
        compose.onNodeWithText("Cancelar edición").performClick()
        compose.onNodeWithText("Confirmar").performClick()
        assertEquals(setOf(approved.id), repo.offer?.materias); assertEquals(0, repo.writes)
        repo.states = data.materias.associate { it.id to EstadoAcademico.APROBADA }
        compose.runOnUiThread { vm.reload() }; compose.waitForIdle()
        compose.onNodeWithText("Editar materias").performScrollTo().performClick()
        val message = "Ya aprobaste todas las materias del plan. No quedan materias para seleccionar."
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText(message))
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithTag("offer-save").performClick()
        assertTrue(repo.offer!!.materias.isEmpty()); assertEquals(1, repo.writes)
    }
    @Test fun navigationPreservesDraftAndHistoryRemainsOperational() {
        val auth = Auth(); val offer = Repo()
        val academic = object : AcademicRepository {
            override suspend fun load(uid: String) = data
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String,EstadoAcademico> = error("No writes")
        }
        val profile = object : ProfileRepository {
            override suspend fun load(uid: String) = ProfileData("Prueba", "Temporal", AssignedStudyPlan(data.idPlan,"Ingeniería de Sistemas","615/2016"),ProfileRegistration.COMPLETE)
            override suspend fun save(uid: String, names: String, surnames: String): ProfileData = error("No writes")
        }
        val history = object : PreparacionRepository {
            override suspend fun load(uid: String) = LecturaPreparacion(data, RegistroPreparacion(DecisionPreparacion.CONFIRMADO,"1:0"),null)
            override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial, progreso: (Int,Int)->Unit): LecturaPreparacion = error("No writes")
            override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion = error("No writes")
            override suspend fun reanudar(uid: String,idOperacion: String,progreso: (Int,Int)->Unit): LecturaPreparacion = error("No writes")
            override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion = error("No writes")
        }
        compose.setContent { RoadMap_UtepsaTheme { AppNavigation(remember { AuthViewModel(auth) },compose.activity,profile,academic,auth,history,offer) } }
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        compose.onNodeWithText("Oferta").performClick(); compose.onNodeWithText("Seleccionar materias").performClick()
        compose.onNodeWithTag("offer-search").performTextInput("STS-300")
        compose.onNodeWithTag("offer-search").performImeAction()
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText("Taller de Graduación (Sistemas)"))
        compose.onNodeWithText("Taller de Graduación (Sistemas)").performClick()
        compose.onNodeWithContentDescription("Tu cuenta").performClick()
        compose.onNodeWithText("Editar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Tu perfil").assertIsDisplayed()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.onNodeWithText("Hay cambios sin guardar. Se descartarán al cerrar sesión.").assertIsDisplayed()
        compose.onNodeWithText("Seguir aquí").performClick()
        compose.onNodeWithText("Volver a Cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("account-back").performClick()
        compose.onNodeWithText("1 materia seleccionada").assertIsDisplayed()
        compose.onNodeWithText("Malla").performClick(); compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasText("Cerrar sesión"))
        compose.onNodeWithText("Cerrar sesión").performClick()
        compose.onNodeWithText("La selección modificada se perderá. Todavía no se ha guardado.").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasText("Ver historial"))
        compose.onNodeWithText("Ver historial").performClick()
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        compose.onNodeWithText("Volver a Malla").performClick()
        compose.onNodeWithText("Oferta").performClick()
        compose.onNodeWithText("1 materia seleccionada").assertIsDisplayed()
        compose.onNodeWithTag("offer-save").performClick(); assertEquals(1, offer.offer?.materias?.size)
    }
}

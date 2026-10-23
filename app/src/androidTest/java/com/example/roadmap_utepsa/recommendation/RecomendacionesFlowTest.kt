package com.example.roadmap_utepsa.recommendation

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.offer.OfertaRepository
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.recommendation.DatosRecomendaciones
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.AreaMateria
import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.domain.offer.DatosOferta
import com.example.roadmap_utepsa.domain.academic.Prerrequisito
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.domain.recommendation.Recomendacion
import com.example.roadmap_utepsa.domain.recommendation.ResultadoRecomendaciones
import com.example.roadmap_utepsa.domain.recommendation.MotorRecomendaciones
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.recommendation.RecomendacionesScreen
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.recommendation.EstadoRecomendaciones
import com.example.roadmap_utepsa.presentation.recommendation.RecomendacionesUiState
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import java.io.File

/** Todas las entradas son sintéticas; los repositorios de consulta fallan al escribir. */
class RecomendacionesFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val uid = "recommendations-ui"
    private val ms = listOf(
        Materia("a", "plan", "AAA-001", "Algoritmos y Programación", 1, AreaMateria.ESPECIALIDAD, horasTotales = 170),
        Materia("b", "plan", "BBB-002", "Organización y Arquitectura de Computadoras con un nombre de ejemplo especialmente largo", 2, AreaMateria.ESPECIALIDAD, horasTotales = 85),
        Materia("c", "plan", "CCC-003", "Materia que se desbloquearía", 3, AreaMateria.ESPECIALIDAD, horasTotales = 85))
    private val data = AcademicData(uid, "plan", ms, listOf(Prerrequisito("c", "a")), emptyMap())
    private fun result(states: Map<String, EstadoAcademico> = emptyMap(), ids: Set<String> = setOf("a", "b")) =
        MotorRecomendaciones().calcular(data.copy(estados = states), Oferta("plan", ids, 1))
    private fun capture(name: String) {
        compose.waitForIdle()
        val i = InstrumentationRegistry.getInstrumentation()
        val folder = File(i.targetContext.getExternalFilesDir(null), "recommendations-ui-captures").apply { mkdirs() }
        val modal = compose.onAllNodes(isRoot() and hasAnyDescendant(hasText("Detalle de la recomendación")))
        val systemWindow = i.uiAutomation.rootInActiveWindow
        if (systemWindow?.findAccessibilityNodeInfosByText("System UI isn't responding")?.isNotEmpty() == true) {
            check(systemWindow.findAccessibilityNodeInfosByText("Wait").firstOrNull()
                ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true)
        }
        i.waitForIdleSync()
        android.os.SystemClock.sleep(250)
        // PixelCopy of the Activity misses the separate Dialog window used by ModalBottomSheet.
        val bitmap = if (modal.fetchSemanticsNodes().isEmpty()) compose.onRoot().captureToImage().asAndroidBitmap()
            else i.uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
    }
    @Test fun allBlockingStatesAndActionsAreDistinct() {
        val current = mutableStateOf(RecomendacionesUiState())
        var retry = 0; var history = 0; var offer = 0; var malla = 0
        compose.setContent { RoadMap_UtepsaTheme { RecomendacionesScreen(current.value, { retry++ }, { malla++ }, { offer++ }, { history++ }, {}) } }
        compose.onNodeWithText("Preparando recomendaciones…").assertIsDisplayed(); capture("01-carga")
        val cases = listOf(
            Triple(EstadoRecomendaciones.ERROR, "No pudimos generar la lista", "Reintentar"),
            Triple(EstadoRecomendaciones.HISTORIAL_PENDIENTE, "Confirma tu historial", "Revisar historial"),
            Triple(EstadoRecomendaciones.HISTORIAL_INCOHERENTE, "Revisa tu historial", "Revisar historial"),
            Triple(EstadoRecomendaciones.RECUPERACION_PENDIENTE, "Recuperación pendiente", "Retomar historial"),
            Triple(EstadoRecomendaciones.OFERTA_AUSENTE, "Primero registra tu oferta", "Registrar oferta SAE"),
            Triple(EstadoRecomendaciones.OFERTA_VACIA, "Tu oferta está vacía", "Editar oferta SAE"),
            Triple(EstadoRecomendaciones.SIN_CANDIDATAS, "No hay candidatas por ahora", "Revisar mi malla"))
        cases.forEachIndexed { i, (state, title, action) ->
            compose.runOnIdle { current.value = RecomendacionesUiState(state) }
            compose.onNodeWithTag("recommendations-list").performScrollToIndex(0)
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed(); capture("state-$i")
            compose.onNodeWithText(action).performScrollTo().performClick()
        }
        assertEquals(1, retry); assertEquals(3, history); assertEquals(2, offer); assertEquals(1, malla)
    }
    @Test fun principalExplanationZeroAndLongNamesAtFont15() {
        val current = mutableStateOf(RecomendacionesUiState(EstadoRecomendaciones.LISTO, result()))
        compose.setContent { RoadMap_UtepsaTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                RecomendacionesScreen(current.value, {}, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithText("Recomendación principal", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText("Ver detalle"))
        compose.onAllNodesWithText("Ver detalle")[0].performClick()
        compose.onNodeWithText("Detalle de la recomendación").assertIsDisplayed(); capture("detail-font15")
        compose.onNodeWithText("Materia que se desbloquearía · CCC-003").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Entendido").performScrollTo().performClick()
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText(ms[1].nombre))
        compose.onNodeWithText(ms[1].nombre).assertIsDisplayed(); capture("long-name-font15")
        compose.runOnIdle { current.value = RecomendacionesUiState(EstadoRecomendaciones.LISTO, result(ids = setOf("b"))) }
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText("Ver detalle"))
        compose.onNodeWithText("Ver detalle").performClick()
        compose.onAllNodesWithText("Suma una materia aprobada a tu carrera").onLast().performScrollTo().assertIsDisplayed(); capture("zero-font15")
    }
    @Test fun priorityAndAlternativeExplainEarlierPending() {
        val current = mutableStateOf(RecomendacionesUiState(EstadoRecomendaciones.LISTO,
            result(mapOf("b" to EstadoAcademico.EN_CURSO))))
        compose.setContent { RoadMap_UtepsaTheme { RecomendacionesScreen(current.value, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("Recomendación principal", substring = true).assertIsDisplayed(); capture("priority")
        compose.onNodeWithText("Ver pendientes anteriores").performClick()
        compose.onNodeWithText("${ms[0].nombre} · ${ms[0].codigo}").assertIsDisplayed()
        compose.onNodeWithText("Ocultar pendientes anteriores").performClick()
        compose.onNodeWithText("${ms[0].nombre} · ${ms[0].codigo}").assertDoesNotExist()
        val alt = ResultadoRecomendaciones(listOf(Recomendacion(ms[1], emptyList(), 0, emptyList(), false)), listOf(ms[0]), 2)
        compose.runOnIdle { current.value = RecomendacionesUiState(EstadoRecomendaciones.LISTO, alt) }
        compose.onNodeWithText("Recomendación principal", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Tus pendientes anteriores no están disponibles en esta oferta.").assertIsDisplayed(); capture("alternative")
    }
    @Test fun partialSecondaryAndOptionalChoicesAtFont15() {
        fun materia(id: String, name: String, semester: Int = 7) =
            Materia(id, "plan", id, name, semester, AreaMateria.ESPECIALIDAD, horasTotales = 85)
        val subjects = listOf(materia("I", "Inglés Técnico I"), materia("II", "Inglés Técnico II", 8),
            materia("Q", "Calidad y Prueba de Software"), materia("D", "Análisis de Datos", 8),
            materia("P", "Proyecto Integrador de Desarrollo de Soluciones Empresariales", 9),
            materia("C", "Diseño de Compiladores", 8), materia("L", "Legislación para Ingenieros Tecnológicos", 8),
            materia("E", "Desarrollo Empresarial Tecnológico", 8))
        val academic = AcademicData(uid, "plan", subjects, listOf(Prerrequisito("II", "I"),
            Prerrequisito("P", "Q"), Prerrequisito("P", "D")), emptyMap())
        val current = mutableStateOf(RecomendacionesUiState(EstadoRecomendaciones.LISTO,
            MotorRecomendaciones().calcular(academic, Oferta("plan", setOf("I", "Q", "C", "L", "E"), 1))))
        compose.setContent { RoadMap_UtepsaTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                RecomendacionesScreen(current.value, {}, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithText("Al aprobarla, habilitas Inglés Técnico II").performScrollTo().assertIsDisplayed()
        capture("english-principal-font15")
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText("Recomendación secundaria"))
        compose.onNodeWithText("Recomendación secundaria").assertIsDisplayed()
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText("Calidad y Prueba de Software"))
        compose.onNode(hasText("Ver detalle") and hasAnyAncestor(hasTestTag("recommendation-Q")))
            .performScrollTo().performClick()
        compose.onNodeWithText("Cumplirías 1 de 2 requisitos.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Análisis de Datos · D").performScrollTo().assertIsDisplayed(); capture("partial-detail-font15")
        compose.onNodeWithText("Entendido").performScrollTo().performClick()
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText("Otras opciones para completar tu elección"))
        capture("optional-choices-font15")
        compose.runOnIdle { current.value = RecomendacionesUiState(EstadoRecomendaciones.LISTO,
            MotorRecomendaciones().calcular(academic, Oferta("plan", setOf("C", "L", "E"), 2))) }
        compose.onNodeWithTag("recommendations-list").performScrollToIndex(0)
        compose.onNodeWithText("Recomendación secundaria").assertDoesNotExist()
        compose.onNodeWithText("Si quieres cursar dos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("recommendations-list").performScrollToNode(hasText("Opcional"))
        compose.onAllNodesWithText("Opcional").onFirst().assertIsDisplayed(); capture("zero-no-secondary-font15")
    }
    @Test fun mixedBenefitsUseCountsAndDetailKeepsBothTargets() {
        val subjects = listOf("A", "B", "C", "D").map {
            Materia(it, "plan", it, "Materia $it", 1, AreaMateria.ESPECIALIDAD, horasTotales = 85)
        }
        val academic = AcademicData(uid, "plan", subjects, listOf(Prerrequisito("C", "A"),
            Prerrequisito("D", "A"), Prerrequisito("D", "B")), emptyMap())
        val calculated = MotorRecomendaciones().calcular(academic, Oferta("plan", setOf("A"), 1))
        compose.setContent { RoadMap_UtepsaTheme {
            RecomendacionesScreen(RecomendacionesUiState(EstadoRecomendaciones.LISTO, calculated), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithText("Al aprobarla, habilitas 1 materia\nAvanzas requisitos de 1 materia")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Si quieres cursar dos").assertDoesNotExist()
        compose.onNodeWithText("Ver detalle").performScrollTo().performClick()
        compose.onNodeWithText("Materia C · C").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Materia D").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cumplirías 1 de 2 requisitos.").performScrollTo().assertIsDisplayed()
    }

    @Test fun navigationRetainsOfferDraftQueryAndRevisionAndReloadsRecommendations() {
        val auth = object : AuthRepository {
            override val uidChanges = MutableStateFlow<String?>(uid)
            override val currentUid get() = uidChanges.value
            override val isAuthenticated get() = currentUid != null
            override val authenticationChanges = uidChanges.map { it != null }
            override suspend fun signInWithGoogle(idToken: String) {}
            override fun signOut() { uidChanges.value = null }
        }
        val academic = object : AcademicRepository {
            override suspend fun load(uid: String) = data
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> = error("No escribir")
        }
        val profile = object : ProfileRepository {
            override suspend fun load(uid: String) = ProfileData("Prueba", "Sintética", AssignedStudyPlan("plan", "Sistemas", "615/2016"), ProfileRegistration.COMPLETE)
            override suspend fun save(uid: String, names: String, surnames: String): ProfileData = error("No escribir")
        }
        val prep = object : PreparacionRepository {
            override suspend fun load(uid: String) = LecturaPreparacion(data, RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1"), null)
            override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion = error("No escribir")
            override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion = error("No escribir")
            override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No escribir")
            override suspend fun reanudar(uid: String, idOperacion: String, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No escribir")
        }
        var saved = Oferta("plan", setOf("a"), 1)
        var expected: Long? = null
        val offer = object : OfertaRepository {
            override suspend fun load(uid: String) = DatosOferta(uid, "plan", ms, saved)
            override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta {
                expected = expectedRevision
                check(expectedRevision == saved.revision)
                return Oferta("plan", materias, saved.revision + 1).also { saved = it }
            }
        }
        var loads = 0
        val rec = RecomendacionesRepository { loads++; DatosRecomendaciones(prep.load(uid), saved, 1) }
        compose.setContent { RoadMap_UtepsaTheme { AppNavigation(remember { AuthViewModel(auth) }, compose.activity, profile, academic, auth, prep, offer, rec) } }
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        compose.onNodeWithText("Oferta").performClick(); compose.onNodeWithText("Editar materias").performScrollTo().performClick()
        compose.onNodeWithTag("offer-search").performTextInput("BBB-002"); compose.onNodeWithTag("offer-search").performImeAction()
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("offer-search").assertTextContains("BBB-002")
        // Filtering and IME dismissal can dispose lazy rows during touch injection.
        // Resolve through the lazy container, then invoke the row's accessible action.
        compose.onNodeWithTag("offer-list").performScrollToNode(hasText(ms[1].nombre))
        compose.onNodeWithText(ms[1].nombre).performScrollTo().assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("Para ti").performClick()
        compose.onNodeWithText("Recomendadas para ti").assertIsDisplayed()
        assertEquals(1, loads); assertEquals(setOf("a"), saved.materias)
        compose.onNodeWithText("Oferta").performClick()
        compose.onNodeWithTag("offer-search").assertTextContains("BBB-002")
        compose.onNodeWithText("2 materias seleccionadas").assertIsDisplayed()
        compose.onNodeWithTag("offer-save").performClick()
        assertEquals(1L, expected)
        compose.onNodeWithText("Oferta guardada").assertIsDisplayed()
        compose.onNodeWithText("Para ti").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { loads == 2 }
        compose.onNodeWithContentDescription("Tu cuenta").performClick()
        compose.onNodeWithText("Editar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Tu perfil").assertIsDisplayed()
        compose.onNodeWithText("Volver a Cuenta").performScrollTo().performClick()
        compose.onNodeWithTag("account-back").performClick()
        compose.onNodeWithText("Recomendadas para ti").assertIsDisplayed()
        compose.waitUntil(10_000) { loads >= 3 }
        compose.runOnIdle { assertEquals("One reload after returning from Cuenta/Profile", 3, loads) }
    }
}

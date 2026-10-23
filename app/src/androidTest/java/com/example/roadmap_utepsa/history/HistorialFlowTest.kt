package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.R

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.academic.ConflictoEstadoAcademico
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.history.ConflictoHistorial
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.AreaMateria
import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.Prerrequisito
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.presentation.academic.AcademicUiState
import com.example.roadmap_utepsa.presentation.academic.AcademicViewModel
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.history.PasoHistorial
import com.example.roadmap_utepsa.presentation.history.ModoHistorial
import com.example.roadmap_utepsa.presentation.history.HistorialUiState
import com.example.roadmap_utepsa.presentation.history.HistorialViewModel
import com.example.roadmap_utepsa.presentation.history.HistorialActions
import com.example.roadmap_utepsa.presentation.history.HistorialScreen
import com.example.roadmap_utepsa.presentation.academic.CurriculumScreen
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** UI de historial en Android temporal; repositorios y autenticación exclusivamente en memoria. */
class HistorialFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("history-test")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) { uidChanges.value = "history-test" }
        override fun signOut() { uidChanges.value = null }
    }
    private class Repository(data: AcademicData) : PreparacionRepository {
        var lectura = LecturaPreparacion(data, null, null)
        var fail = false
        var conflict = false
        var writes = 0
        var loads = 0
        override suspend fun load(uid: String): LecturaPreparacion { loads++; return lectura }
        override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial,
                                       progreso: (Int, Int) -> Unit): LecturaPreparacion {
            writes++
            if (conflict) throw ConflictoHistorial(lectura)
            if (fail) error("Fallo de guardado simulado")
            progreso(borrador.cambios().size, borrador.cambios().size)
            lectura = lectura.copy(academic = lectura.academic.copy(estados = borrador.seleccion),
                registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1:0"))
            return lectura
        }
        override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion {
            lectura = lectura.copy(registro = RegistroPreparacion(DecisionPreparacion.POSPUESTO, "1:0"))
            return lectura
        }
        override suspend fun reanudar(uid: String, idOperacion: String,
                                      progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No existe journal en esta prueba")
        override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion = error("No existe journal en esta prueba")
    }
    private fun fixture(): AcademicData {
        val context = InstrumentationRegistry.getInstrumentation().context
        val json = JSONObject(context.assets.open("sistemas-rm-615-2016.json").bufferedReader().use { it.readText() })
        val subjects = json.getJSONArray("materias")
        val relations = json.getJSONArray("prerrequisitos")
        return AcademicData("history-test", json.getString("id_plan"), (0 until subjects.length()).map { i ->
            val row = subjects.getJSONObject(i)
            Materia(row.getString("id"), row.getString("id_plan"), row.getString("codigo"), row.getString("nombre"),
                row.getInt("semestre"), AreaMateria.valueOf(row.getString("area")), row.getInt("min_aprobadas"), row.getInt("horas_totales"))
        }, (0 until relations.length()).map { i -> val row = relations.getJSONObject(i)
            Prerrequisito(row.getString("id_objetivo"), row.getString("id_requisito")) }, emptyMap())
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // El árbol Compose listo no garantiza que SurfaceFlinger haya presentado el frame.
        // QA 2026-10-17: captura01 blanca con barras visibles; dar 400 ms al compositor
        // antes de capturar la ventana. Esta espera solo estabiliza la evidencia visual.
        android.os.SystemClock.sleep(400)
        instrumentation.waitForIdleSync()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "history-ui-captures").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun start(repo: Repository, auth: Auth = Auth()): HistorialViewModel {
        lateinit var vm: HistorialViewModel
        compose.runOnUiThread { vm = HistorialViewModel("history-test", repo, auth) }
        compose.runOnUiThread { compose.activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)) }
        compose.setContent {
            val state by vm.state.collectAsState()
            RoadMap_UtepsaTheme {
                HistorialScreen(state, HistorialActions(
                    onEdit = vm::editar, onCancel = vm::cancelarEdicion,
                    onRetry = vm::reload, onProfile = {}, onSignIn = {},
                    onOpenStep = vm::abrirPaso, onApproved = vm::seleccionarAprobada,
                    onCourse = vm::seleccionarEnCurso, onReview = vm::revisar,
                    onSkipCourse = vm::omitirNuevasEnCurso, onConfirm = vm::confirmar,
                    onPostpone = vm::posponer, onRecover = vm::recuperarResultado,
                    onDiscard = vm::cancelarEdicion, onExit = {}, onReadConflict = {},
                    onResolveConflict = vm::resolverConflictoConSeleccion
                ))
            }
        }
        compose.waitUntil(10_000) { vm.state.value is HistorialUiState.Ready }
        compose.waitForIdle()
        return vm
    }
    private fun ready(vm: HistorialViewModel) = vm.state.value as HistorialUiState.Ready

    @Test fun waitingKeepsConfirmedProgressVisibleWithoutAllowingResubmit() {
        val data = fixture()
        val state = HistorialUiState.Ready(LecturaPreparacion(data, null, null),
            BorradorHistorial(data.uid, data.estados), paso = PasoHistorial.REVISION,
            guardando = true, esperando = true, progresoAplicado = 5, progresoTotal = 35)
        compose.setContent { RoadMap_UtepsaTheme { HistorialScreen(state, HistorialActions()) } }
        compose.onNodeWithText("Seguimos esperando").assertExists()
        compose.onNodeWithTag("history-content").performScrollToNode(hasText("5 de 35 cambios confirmados"))
        compose.onNodeWithText("5 de 35 cambios confirmados").assertIsDisplayed()
        compose.onNodeWithTag("history-primary").assertIsNotEnabled()
    }
    private fun click(tag: String) { compose.onNodeWithTag(tag).performClick(); compose.waitForIdle() }
    private fun select(id: String) {
        compose.onNodeWithTag("history-content").performScrollToNode(hasTestTag("history-subject-$id"))
        click("history-subject-$id")
    }
    private fun toReview() { click("history-primary"); click("history-primary") }

    private fun startNavigation(repo: Repository) {
        val auth = Auth()
        val profile = object : ProfileRepository {
            override suspend fun load(uid: String) = ProfileData("Prueba", "Temporal",
                AssignedStudyPlan(repo.lectura.academic.idPlan, "Ingeniería de Sistemas", "615/2016"),
                ProfileRegistration.COMPLETE)
            override suspend fun save(uid: String, names: String, surnames: String): ProfileData = error("No debe cambiar Perfil")
        }
        val academic = object : AcademicRepository {
            override suspend fun load(uid: String) = repo.lectura.academic
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> =
                error("Esta prueba no guarda estados desde Malla")
        }
        compose.runOnUiThread { compose.activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)) }
        compose.setContent {
            RoadMap_UtepsaTheme {
                AppNavigation(remember { AuthViewModel(auth) }, compose.activity, profile, academic, auth, repo)
            }
        }
        compose.waitForIdle()
    }

    @Test fun confirmedPreparationSkipsWelcomeAndAllowsExplicitReview() {
        val repo = Repository(fixture()).apply {
            lectura = lectura.copy(registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1:0"))
        }
        startNavigation(repo)
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        compose.onNodeWithText("Ver historial").performScrollTo().performClick()
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        compose.onNodeWithText("Configurar después").assertDoesNotExist()
        compose.onNodeWithText("Volver a Malla").performClick()
        compose.onNodeWithText("Configuración pendiente").assertDoesNotExist()
        assertEquals(DecisionPreparacion.CONFIRMADO, repo.lectura.registro!!.decision)
        assertEquals(0, repo.writes)
    }

    @Test fun confirmedEditCancelPreservesStatesAndNames() {
        val data = fixture()
        val subject = data.materias.first { it.codigo == "BMS-300" }
        val states = mapOf(subject.id to EstadoAcademico.APROBADA)
        val repo = Repository(data.copy(estados = states)).apply {
            lectura = lectura.copy(registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1:0"))
        }
        startNavigation(repo)
        compose.onNodeWithText("Ver historial").performScrollTo().performClick()
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        compose.onNodeWithTag("history-content").performScrollToNode(hasText(subject.nombre))
        compose.onNodeWithText(subject.nombre).assertIsDisplayed()
        screenshot("18-consulta-historial")
        compose.onNodeWithText("Editar historial").performClick()
        select(subject.id)
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("¿Salir sin guardar?").assertIsDisplayed()
        screenshot("19-descartar-edicion")
        compose.onNodeWithText(compose.activity.getString(com.example.roadmap_utepsa.R.string.history_discard_to_summary)).performClick()
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        assertEquals(states, repo.lectura.academic.estados)
        assertEquals(DecisionPreparacion.CONFIRMADO, repo.lectura.registro!!.decision)
        assertEquals(0, repo.writes)
    }

    @Test fun postponedHistoryConfirmsExistingSelectionWithoutReentry() {
        val data = fixture()
        val subject = data.materias.first { it.semestre == 1 }
        val states = mapOf(subject.id to EstadoAcademico.APROBADA)
        val repo = Repository(data.copy(estados = states)).apply {
            lectura = lectura.copy(registro = RegistroPreparacion(DecisionPreparacion.POSPUESTO, "1:0"))
        }
        startNavigation(repo)
        compose.onNodeWithText("Completar historial").performScrollTo().performClick()
        compose.onNodeWithText("Editar historial").performClick()
        click("history-primary")
        click("history-primary")
        compose.onNodeWithTag("history-content").performScrollToNode(hasText("No has modificado ninguna materia."))
        click("history-primary")
        compose.onNodeWithText("Tu historial está listo").assertIsDisplayed()
        assertEquals(states, repo.lectura.academic.estados)
        assertEquals(DecisionPreparacion.CONFIRMADO, repo.lectura.registro!!.decision)
        assertEquals(1, repo.writes)
    }

    @Test fun summarySupportsLargeFontLongNamesAndPendingFilter() {
        val data = fixture()
        val subject = data.materias.single { it.codigo == "SIS-302" }
        val states = mapOf(subject.id to EstadoAcademico.APROBADA)
        val state = HistorialUiState.Ready(LecturaPreparacion(data.copy(estados = states),
            RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1:0"), null),
            BorradorHistorial(data.uid, states), modo = ModoHistorial.CONSULTA)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, 2f)) {
                RoadMap_UtepsaTheme { HistorialScreen(state, HistorialActions()) }
            }
        }
        compose.onNodeWithTag("history-content").performScrollToNode(hasText(subject.nombre))
        compose.onNodeWithText(subject.nombre).assertIsDisplayed()
        compose.onNodeWithText("Editar historial").assertIsDisplayed()
        screenshot("20-consulta-fuente-ampliada")
        compose.onNodeWithTag("history-content").performScrollToNode(hasText("Pendiente"))
        compose.onNodeWithText("Pendiente").performClick()
        compose.onNodeWithText(subject.nombre).assertDoesNotExist()
        compose.onNodeWithTag("history-content").performScrollToNode(hasText(data.materias.first().nombre))
        compose.onNodeWithText(data.materias.first().nombre).assertExists()
    }

    @Test fun approvedAndCurrentCourseAreVisibleInCurriculumAfterConfirmation() {
        val repo = Repository(fixture())
        val roots = repo.lectura.academic.materias.filter { it.semestre == 1 }.take(2)
        startNavigation(repo)
        click("history-approved-start")
        select(roots[0].id)
        click("history-primary")
        select(roots[1].id)
        click("history-primary")
        click("history-primary")
        compose.onNodeWithText("Tu historial está listo").assertIsDisplayed()
        compose.onNodeWithText("Ir a Malla").performClick()
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasTestTag("subject-${roots[1].id}"))
        compose.onNodeWithTag("subject-${roots[1].id}").assertTextContains("En curso", substring = true)
        assertEquals(EstadoAcademico.APROBADA, repo.lectura.academic.estados[roots[0].id])
        assertEquals(EstadoAcademico.EN_CURSO, repo.lectura.academic.estados[roots[1].id])
        assertEquals(1, repo.writes)
    }

    @Test fun absentPreparationCanPostponeAndResumeWithoutChangingStates() {
        val initial = fixture()
        val id = initial.materias.first { it.semestre == 1 }.id
        val states = mapOf(id to EstadoAcademico.APROBADA)
        val repo = Repository(initial.copy(estados = states))
        startNavigation(repo)
        click("history-postpone")
        compose.onNodeWithText("Ir a Malla").performClick()
        compose.onNodeWithText("Configuración pendiente").assertIsDisplayed()
        screenshot("10-malla-pospuesta")
        compose.onNodeWithText("Completar historial").performClick()
        compose.onNodeWithText("Tu historial").assertIsDisplayed()
        assertEquals(states, repo.lectura.academic.estados)
        assertEquals(DecisionPreparacion.POSPUESTO, repo.lectura.registro!!.decision)
    }

    @Test fun welcomeAndSelectionPreserveExistingStates() {
        val data = fixture()
        val ids = data.materias.filter { it.semestre == 1 }.take(2).map { it.id }
        val existing = mapOf(ids[0] to EstadoAcademico.APROBADA, ids[1] to EstadoAcademico.EN_CURSO)
        val repo = Repository(data.copy(estados = existing))
        val vm = start(repo)
        compose.onNodeWithText("Preparemos tu historial").assertIsDisplayed()
        screenshot("01-bienvenida")
        click("history-approved-start")
        compose.onNodeWithTag("history-content").performScrollToNode(hasTestTag("history-subject-${ids[0]}"))
        compose.onNodeWithTag("history-subject-${ids[0]}").assertExists()
        screenshot("02-seleccion-existente")
        assertEquals(existing, ready(vm).borrador.seleccion)
        toReview()
        assertEquals(existing, ready(vm).borrador.seleccion)
        assertEquals(0, repo.writes)
    }

    @Test fun selectionReviewAndConfirmUseMemoryRepository() {
        val repo = Repository(fixture())
        val vm = start(repo)
        val id = repo.lectura.academic.materias.first { it.semestre == 1 }.id
        click("history-approved-start")
        select(id)
        screenshot("03-aprobada-seleccionada")
        toReview()
        screenshot("04-revision")
        assertEquals(PasoHistorial.REVISION, ready(vm).paso)
        click("history-primary")
        compose.onNodeWithText("Tu historial está listo").assertIsDisplayed()
        screenshot("05-confirmacion")
        assertEquals(1, repo.writes)
        assertTrue(ready(vm).confirmado)
        assertEquals(EstadoAcademico.APROBADA, repo.lectura.academic.estados[id])
    }

    @Test fun saveFailureAndRecoveryPreserveDraft() {
        val repo = Repository(fixture()).apply { fail = true }
        val vm = start(repo)
        val id = repo.lectura.academic.materias.first { it.semestre == 1 }.id
        click("history-approved-start")
        select(id)
        toReview()
        click("history-primary")
        compose.onNodeWithText("No pudimos confirmar").assertIsDisplayed()
        screenshot("06-error-guardado")
        assertEquals(EstadoAcademico.APROBADA, ready(vm).borrador.seleccion[id])
        repo.fail = false
        // Acción de recuperación real de VM; la UI sigue mostrando el borrador.
        compose.runOnUiThread { vm.recuperarResultado() }
        compose.waitForIdle()
        assertFalse(ready(vm).recuperacionPendiente)
        assertEquals(EstadoAcademico.APROBADA, ready(vm).borrador.seleccion[id])
        click("history-primary")
        compose.onNodeWithText("Tu historial está listo").assertIsDisplayed()
        assertEquals(2, repo.writes)
    }

    @Test fun enCursoExplainsBothMissingRequirements() {
        val repo = Repository(fixture())
        val vm = start(repo)
        val id = repo.lectura.academic.materias.single { it.codigo == "SIS-306" }.id
        click("history-approved-start")
        click("history-primary")
        compose.onNodeWithTag("history-search").performTextReplacement("SIS-306")
        compose.onNodeWithTag("history-content").performScrollToNode(hasTestTag("history-subject-$id"))
        compose.onNodeWithTag("history-subject-$id").assertIsNotEnabled()
        screenshot("07-curso-bloqueado")
        assertNull(ready(vm).borrador.seleccion[id])
        assertEquals(0, repo.writes)
    }

    @Test fun conflictKeepsLocalSelectionUntilExplicitReview() {
        val repo = Repository(fixture())
        val vm = start(repo)
        val ids = repo.lectura.academic.materias.filter { it.semestre == 1 }.take(2).map { it.id }
        click("history-approved-start")
        select(ids[0])
        toReview()
        repo.lectura = repo.lectura.copy(academic = repo.lectura.academic.copy(estados = mapOf(ids[1] to EstadoAcademico.APROBADA)))
        repo.conflict = true
        click("history-primary")
        compose.onNodeWithTag("history-conflict").assertExists()
        screenshot("08-conflicto")
        assertTrue(ready(vm).borrador.base.isEmpty())
        assertEquals(mapOf(ids[0] to EstadoAcademico.APROBADA), ready(vm).borrador.seleccion)
        assertNotNull(ready(vm).conflicto)
        assertEquals(1, repo.writes)
    }

    @Test fun sessionSwitchDropsHistory() {
        val auth = Auth()
        val repo = Repository(fixture())
        val vm = start(repo, auth)
        click("history-approved-start")
        compose.runOnUiThread { auth.uidChanges.value = "otra-cuenta-simulada" }
        compose.onNodeWithText("Vuelve a iniciar sesión").assertIsDisplayed()
        screenshot("09-sesion-expirada")
        assertEquals(HistorialUiState.SessionExpired, vm.state.value)
        assertEquals(0, repo.writes)
    }

    private class AcademicMemory(var data: AcademicData) : AcademicRepository {
        var writes = 0
        override suspend fun load(uid: String) = data
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> =
            error("Esta UI debe usar CAS")
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico,
                                       expectedStates: Map<String, EstadoAcademico>): Map<String, EstadoAcademico> {
            if (data.estados != expectedStates.filterValues { it != EstadoAcademico.PENDIENTE })
                throw ConflictoEstadoAcademico(data.estados)
            writes++
            data = data.copy(estados = data.estados.toMutableMap().apply {
                if (estado == EstadoAcademico.PENDIENTE) remove(idMateria) else put(idMateria, estado)
            })
            return data.estados
        }
    }
    private fun startAcademic(repo: AcademicMemory, code: String): AcademicViewModel {
        lateinit var vm: AcademicViewModel
        compose.runOnUiThread { vm = AcademicViewModel("history-test", repo, Auth()) }
        compose.runOnUiThread { compose.activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)) }
        compose.setContent {
            val state by vm.state.collectAsState()
            RoadMap_UtepsaTheme {
                CurriculumScreen(state, vm::reload, {}, vm::openSubject, vm::selectState,
                    vm::saveState, vm::closeSubject, vm::dismissSaved, onReviewHistory = {},
                    onReviewConflict = vm::reviewConflict)
            }
        }
        compose.waitUntil(10_000) { vm.state.value is AcademicUiState.Ready }
        compose.runOnUiThread { vm.openSubject(repo.data.materias.single { it.codigo == code }.id) }
        compose.waitForIdle()
        return vm
    }
    @Test fun h21ShowsBothRequirementStatesWithoutSaving() {
        val data = fixture()
        val approved = data.materias.single { it.codigo == "SCC-305" }.id
        val repo = AcademicMemory(data.copy(estados = mapOf(approved to EstadoAcademico.APROBADA)))
        val vm = startAcademic(repo, "SIS-306")
        compose.onNodeWithTag("state-EN_CURSO").performScrollTo().performClick()
        compose.onNodeWithText("Análisis de Datos: Aprobada").assertExists()
        compose.onAllNodesWithText("Calidad y Prueba de Software: Pendiente").onFirst().assertExists()
        compose.onNodeWithText("No puedes guardar En curso").assertIsNotEnabled()
        screenshot("11-h21-dos-requisitos")
        assertEquals(0, repo.writes)
        assertEquals(EstadoAcademico.EN_CURSO, (vm.state.value as AcademicUiState.Ready).editor!!.selected)
    }
    @Test fun h23ReturnsCourseToPendingManually() {
        val data = fixture()
        val subject = data.materias.single { it.codigo == "BMS-301" }.id
        val prerequisite = data.materias.single { it.codigo == "BMS-300" }.id
        val repo = AcademicMemory(data.copy(estados = mapOf(subject to EstadoAcademico.EN_CURSO, prerequisite to EstadoAcademico.APROBADA)))
        startAcademic(repo, "BMS-301")
        compose.onNodeWithTag("state-PENDIENTE").performScrollTo().performClick()
        compose.onNodeWithText("Volver a cursarla").assertIsDisplayed()
        compose.onNodeWithText("Guardar como Pendiente").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
        screenshot("12-h23-pendiente-manual")
        compose.onNodeWithText("Guardar como Pendiente").performClick()
        compose.waitForIdle()
        assertFalse(repo.data.estados.containsKey(subject))
        assertEquals(EstadoAcademico.APROBADA, repo.data.estados[prerequisite])
        assertEquals(1, repo.writes)
    }
    @Test fun h27ThresholdFortyFiveRequiresExplicitSave() {
        val data = fixture()
        val subject = data.materias.single { it.codigo == "STS-300" }.id
        val states = data.materias.filter { it.id != subject }.sortedWith(compareBy({ it.semestre }, { it.codigo }))
            .take(45).associate { it.id to EstadoAcademico.APROBADA }
        val repo = AcademicMemory(data.copy(estados = states))
        startAcademic(repo, "STS-300")
        compose.onNodeWithTag("state-EN_CURSO").performScrollTo().performClick()
        compose.onNodeWithText("Umbral cumplido").assertIsDisplayed()
        assertEquals(0, repo.writes)
        assertFalse(repo.data.estados.containsKey(subject))
        compose.onNodeWithText("Guardar como En curso").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
        screenshot("13-h27-umbral-cumplido")
        compose.onNodeWithText("Guardar como En curso").performClick()
        compose.waitForIdle()
        assertEquals(EstadoAcademico.EN_CURSO, repo.data.estados[subject])
        assertEquals(1, repo.writes)
    }
    @Test fun jointCorrectionAddsOmittedRequirementWithoutRemovingApproval() {
        val data = fixture()
        val omitted = data.materias.single { it.codigo == "SIS-305" }.id
        val dependent = data.materias.single { it.codigo == "SIS-306" }.id
        val repo = Repository(data.copy(estados = data.materias.filter { it.id != omitted }.associate { it.id to EstadoAcademico.APROBADA }))
        val vm = start(repo)
        click("history-approved-start")
        compose.onNodeWithTag("history-search").performTextReplacement("SIS-305")
        select(omitted)
        assertEquals(EstadoAcademico.APROBADA, ready(vm).borrador.seleccion[dependent])
        toReview()
        assertTrue(ready(vm).diagnosticos.isEmpty())
        screenshot("14-correccion-conjunta")
        click("history-primary")
        compose.onNodeWithText("Tu historial está listo").assertIsDisplayed()
        assertEquals(EstadoAcademico.APROBADA, repo.lectura.academic.estados[omitted])
        assertEquals(EstadoAcademico.APROBADA, repo.lectura.academic.estados[dependent])
        assertEquals(1, repo.writes)
    }

    @Test fun academicConflictRequiresReviewBeforeSecondSave() {
        val data = fixture()
        val subject = data.materias.single { it.codigo == "TRBA-330" }.id
        val other = data.materias.single { it.codigo == "BMS-300" }.id
        val repo = AcademicMemory(data)
        val vm = startAcademic(repo, "TRBA-330")
        compose.onNodeWithTag("state-EN_CURSO").performScrollTo().performClick()
        repo.data = repo.data.copy(estados = mapOf(other to EstadoAcademico.APROBADA))
        compose.onNodeWithText("Guardar estado").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("El historial cambió").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Guardar estado").assertIsNotEnabled()
        assertEquals(0, repo.writes)
        screenshot("15-conflicto-academico")
        compose.onNodeWithText("Revisar cambios").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(EstadoAcademico.EN_CURSO, (vm.state.value as AcademicUiState.Ready).editor!!.selected)
        assertEquals(0, repo.writes)
        compose.onNodeWithText("Guardar estado").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(EstadoAcademico.APROBADA, repo.data.estados[other])
        assertEquals(EstadoAcademico.EN_CURSO, repo.data.estados[subject])
        assertEquals(1, repo.writes)
    }

    private fun profileRoundTripPreservesExplicitReview(decision: DecisionPreparacion, capture: String) {
        val repo = Repository(fixture()).apply {
            lectura = lectura.copy(registro = RegistroPreparacion(decision, "1:0"))
        }
        val subject = repo.lectura.academic.materias.single { it.codigo == "BMS-300" }.id
        startNavigation(repo)
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        compose.onNodeWithText(if (decision == DecisionPreparacion.CONFIRMADO) "Ver historial" else "Completar historial")
            .performScrollTo().performClick()
        compose.onNodeWithText("Editar historial").performClick()
        select(subject)
        compose.onNodeWithTag("history-subject-$subject").assertIsOn()
        compose.onNodeWithContentDescription("Tu cuenta").performClick()
        compose.onNodeWithText("Editar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Volver a Cuenta").assertIsDisplayed().performClick()
        compose.onNodeWithTag("account-back").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Editar aprobadas").assertExists()
        compose.onNodeWithText("Tu malla curricular").assertDoesNotExist()
        compose.onNodeWithTag("history-content").performScrollToNode(hasTestTag("history-subject-$subject"))
        compose.onNodeWithTag("history-subject-$subject").assertIsOn()
        screenshot(capture)
        assertEquals(0, repo.writes)
        assertTrue(repo.lectura.academic.estados.isEmpty())
        assertEquals(decision, repo.lectura.registro!!.decision)
    }

    @Test fun confirmedReviewSurvivesProfileRoundTripWithDraft() {
        profileRoundTripPreservesExplicitReview(DecisionPreparacion.CONFIRMADO, "16-revision-confirmada-regreso-perfil")
    }

    @Test fun postponedReviewSurvivesProfileRoundTripWithDraft() {
        profileRoundTripPreservesExplicitReview(DecisionPreparacion.POSPUESTO, "17-revision-pospuesta-regreso-perfil")
    }

}

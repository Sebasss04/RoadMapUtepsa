package com.example.roadmap_utepsa

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.model.*
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import com.example.roadmap_utepsa.ui.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Interfaz real con catálogo aprobado y repositorios en memoria; nunca conecta Firebase. */
class AcademicFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("test-alice")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) { uidChanges.value = "test-alice" }
        override fun signOut() { uidChanges.value = null }
    }
    private class Profile : ProfileRepository {
        var registration = ProfileRegistration.COMPLETE
        private var names = "Estudiante de prueba"
        override suspend fun load(uid: String) = ProfileData(names, "Prueba local",
            AssignedStudyPlan("sistemas-rm-615-2016", "Ingeniería de Sistemas", "615/2016"), registration)
        override suspend fun save(uid: String, names: String, surnames: String): ProfileData {
            this.names = names; registration = ProfileRegistration.COMPLETE
            return load(uid).copy(surnames = surnames)
        }
    }
    private class Repository(var data: AcademicData) : AcademicRepository {
        var writes = 0
        var failure = false
        var loadFailure = false
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun load(uid: String): AcademicData {
            if (loadFailure) error("Error de lectura simulado")
            return data.copy(uid = uid)
        }
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> =
            error("El ViewModel debe usar CAS")
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico,
                                       expectedStates: Map<String, EstadoAcademico>): Map<String, EstadoAcademico> {
            writes++
            gate?.await()
            if (failure) error("Guardado rechazado simulado")
            if (data.estados != expectedStates.filterValues { it != EstadoAcademico.PENDIENTE })
                throw ConflictoEstadoAcademico(data.estados)
            val states = data.estados.toMutableMap()
            if (estado == EstadoAcademico.PENDIENTE) states.remove(idMateria) else states[idMateria] = estado
            data = data.copy(estados = states)
            return states
        }
    }
    private fun fixture(): AcademicData {
        val context = InstrumentationRegistry.getInstrumentation().context
        val json = JSONObject(context.assets.open("sistemas-rm-615-2016.json").bufferedReader().use { it.readText() })
        val subjects = json.getJSONArray("materias")
        val relations = json.getJSONArray("prerrequisitos")
        return AcademicData("test-alice", json.getString("id_plan"), (0 until subjects.length()).map { i ->
            val row = subjects.getJSONObject(i)
            Materia(row.getString("id"), row.getString("id_plan"), row.getString("codigo"), row.getString("nombre"),
                row.getInt("semestre"), AreaMateria.valueOf(row.getString("area")), row.getInt("min_aprobadas"))
        }, (0 until relations.length()).map { i -> val row = relations.getJSONObject(i)
            Prerrequisito(row.getString("id_objetivo"), row.getString("id_requisito")) }, emptyMap())
    }
    private fun start(repo: Repository = Repository(fixture()), profile: Profile = Profile(), auth: Auth = Auth()): Repository {
        compose.runOnUiThread { compose.activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)) }
        compose.setContent { RoadMap_UtepsaTheme {
            AppNavigation(AuthViewModel(auth), compose.activity, profile, repo, auth)
        } }
        compose.waitForIdle()
        return repo
    }
    private fun find(code: String) {
        compose.onNodeWithTag("curriculum-list").performScrollToIndex(0)
        compose.onNodeWithText("Buscar materia o sigla").performScrollTo().performTextReplacement(code)
        compose.waitForIdle()
    }
    private fun open(repo: Repository, code: String = "BMS-300") {
        find(code)
        val id = repo.data.materias.single { it.codigo == code }.id
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasTestTag("subject-$id"))
        compose.onNodeWithTag("subject-$id").performClick()
    }
    private fun save() = compose.onNodeWithText("Guardar estado").performScrollTo().performClick()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "academic-verification").apply { mkdirs() }
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun rendersApprovedCatalogAndSearchesWithoutAccents() {
        start()
        compose.onNodeWithText("50 materias · 9 semestres curriculares").assertIsDisplayed()
        screenshot("01-malla")
        find("algebra")
        compose.onNodeWithText("Álgebra Lineal").assertIsDisplayed()
        find("materia inexistente")
        compose.onNodeWithText("No encontramos materias").assertIsDisplayed()
        screenshot("02-sin-resultados")
        compose.onNodeWithText("Limpiar filtros").performScrollTo().performClick()
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasText("Introducción a las Matemáticas"))
        compose.onNodeWithText("Introducción a las Matemáticas").assertIsDisplayed()
    }

    @Test fun savesReopensAndReturnsToPending() {
        val repo = start()
        open(repo)
        screenshot("03-detalle")
        compose.onNodeWithTag("state-APROBADA").performScrollTo().performClick()
        save()
        compose.waitUntil(5000) { repo.writes == 1 && repo.data.estados.isNotEmpty() }
        open(repo)
        compose.onNodeWithTag("state-APROBADA").assertIsSelected()
        compose.onNodeWithTag("state-PENDIENTE").performClick()
        save()
        compose.waitUntil(5000) { repo.writes == 2 && repo.data.estados.isEmpty() }
    }

    @Test fun saveFailureKeepsSelectionAndRetries() {
        val repo = start(Repository(fixture()).apply { failure = true })
        open(repo)
        compose.onNodeWithTag("state-APROBADA").performScrollTo().performClick(); save()
        compose.onNodeWithText("No pudimos confirmar el cambio. Conservamos tu selección para reintentar.").performScrollTo().assertIsDisplayed()
        assertTrue(repo.data.estados.isEmpty())
        screenshot("04-error-guardado")
        compose.runOnIdle { repo.failure = false }
        save()
        compose.waitUntil(5000) { repo.data.estados.isNotEmpty() }
        assertEquals(2, repo.writes)
    }

    @Test fun cancellingDirtyPanelRequiresConfirmationAndDoesNotWrite() {
        val repo = start(); open(repo)
        compose.onNodeWithTag("state-APROBADA").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performScrollTo().performClick()
        compose.onNodeWithText("¿Descartar el cambio de estado?").assertIsDisplayed()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Actualizar estado").assertDoesNotExist()
        assertEquals(0, repo.writes)
    }

    @Test fun profileReturnKeepsSearchAndProtectsDirtyNames() {
        start(); find("algebra")
        compose.onNodeWithContentDescription("Editar perfil").performClick()
        compose.onNodeWithText("Nombres").performTextReplacement("Cambio local")
        compose.onNodeWithText("Volver a Malla").performClick()
        compose.onNodeWithText("¿Descartar los cambios?").assertIsDisplayed()
        compose.onNodeWithText("Descartar").performClick()
        compose.onNodeWithText("Buscar materia o sigla").assertTextContains("algebra")
        compose.onNodeWithText("Álgebra Lineal").assertIsDisplayed()
    }

    @Test fun profileCompletionEntersCurriculumOnlyAfterSave() {
        start(profile = Profile().apply { registration = ProfileRegistration.ABSENT })
        compose.onNodeWithText("Completa tu perfil").assertIsDisplayed()
        compose.onNodeWithText("Guardar perfil").performScrollTo().performClick()
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
    }

    @Test fun readErrorOffersRetryWithoutShowingFakeProgress() {
        val repo = start(Repository(fixture()).apply { loadFailure = true })
        compose.onNodeWithText("No pudimos cargar tu malla").assertIsDisplayed()
        screenshot("05-error-lectura")
        compose.runOnIdle { repo.loadFailure = false }
        compose.onNodeWithText("Reintentar").performClick()
        compose.onNodeWithText("50 materias · 9 semestres curriculares").assertIsDisplayed()
    }

    @Test fun signingOutDropsCurriculumAndShowsLogin() {
        val auth = Auth(); start(auth = auth)
        compose.runOnIdle { auth.signOut() }
        compose.onNodeWithText("Continuar con Google").assertIsDisplayed()
        compose.onNodeWithText("Tu malla curricular").assertDoesNotExist()
    }
}

package com.example.roadmap_utepsa.academic

import com.example.roadmap_utepsa.R

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.AreaMateria
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.domain.academic.Prerrequisito
import com.example.roadmap_utepsa.presentation.academic.ResumenScreen
import com.example.roadmap_utepsa.presentation.academic.AcademicUiState
import com.example.roadmap_utepsa.presentation.history.EstadoPreparacionUi
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMap_UtepsaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File

/** Render real con estados sintéticos: no solicita Auth ni usa repositorios. */
class ResumenVisualTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun ready(approved: Int = 0, course: Int = 0, incoherent: Boolean = false): AcademicUiState.Ready {
        val subjects = (0 until 50).map {
            Materia("t1-subject-$it", "t1-synthetic-plan", "TST-${(it + 1).toString().padStart(3, '0')}",
                "Materia sintética $it", it / 6 + 1, AreaMateria.ESPECIALIDAD)
        }
        val states = subjects.take(approved + course).mapIndexed { i, m ->
            m.id to if (i < approved) EstadoAcademico.APROBADA else EstadoAcademico.EN_CURSO
        }.toMap()
        val requirements = if (incoherent) listOf(Prerrequisito(subjects[0].id, subjects[49].id)) else emptyList()
        return AcademicUiState.Ready(AcademicData("t1-synthetic-user", "t1-synthetic-plan", subjects, requirements, states), emptyMap())
    }
    private fun start(state: AcademicUiState, history: EstadoPreparacionUi = EstadoPreparacionUi.CONFIRMADO,
                      onRetry: () -> Unit = {}, onCurriculum: () -> Unit = {}, onHistory: () -> Unit = {}) {
        compose.runOnUiThread { compose.activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)) }
        compose.setContent { RoadMap_UtepsaTheme {
            ResumenScreen(state, history, onRetry, {}, onCurriculum, onHistory, {})
        } }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "summary-verification").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun approvedDesignExampleAndFutureActionsRemainDisabled() {
        start(ready(5, 2))
        compose.onNodeWithText("10 %").assertIsDisplayed()
        compose.onNodeWithText("5 de 50 aprobadas").assertIsDisplayed()
        compose.onNodeWithText("¿Qué cursar después?").assertIsDisplayed()
        compose.onNodeWithText("Revisar recomendaciones").assertIsNotEnabled()
        compose.onNodeWithText("Oferta").assertIsNotEnabled()
        compose.onNodeWithText("Para ti").assertIsNotEnabled()
        screenshot("01-ejemplo-figma")
    }
    @Test fun firstEntryShowsSuccessfulZeroAndCurriculumAction() {
        var entries = 0
        start(ready(), onCurriculum = { entries++ })
        compose.onNodeWithText("0 de 50 aprobadas").assertIsDisplayed()
        compose.onNodeWithText("Empieza por tu malla").assertIsDisplayed()
        screenshot("02-primer-ingreso")
        compose.onNodeWithText("Actualizar mi malla").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, entries) }
    }
    @Test fun loadingDoesNotExposeCounts() {
        start(AcademicUiState.Loading)
        compose.onNodeWithText("Cargando tu avance…").assertIsDisplayed()
        compose.onNodeWithText("0 de 50 aprobadas").assertDoesNotExist()
        screenshot("03-carga")
    }
    @Test fun errorOffersRetryWithoutFalseZero() {
        var retries = 0
        start(AcademicUiState.Error, onRetry = { retries++ })
        compose.onNodeWithText("No pudimos cargar tu avance").assertIsDisplayed()
        compose.onNodeWithText("0 de 50 aprobadas").assertDoesNotExist()
        screenshot("04-error")
        compose.onNodeWithText("Reintentar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
    @Test fun pendingPreparationOffersHistoryReview() {
        var reviews = 0
        start(ready(), EstadoPreparacionUi.CONFIGURACION_PENDIENTE, onHistory = { reviews++ })
        compose.onNodeWithText("Estos conteos reflejan los estados guardados. Revisa tu historial antes de usarlos para planificar.").performScrollTo().assertIsDisplayed()
        screenshot("05-historial-pendiente")
        compose.onNodeWithText("Ver historial").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, reviews) }
    }
    @Test fun partialSaveWarningTakesPriorityOverIncoherenceAndKeepsHistoryAction() {
        var reviews = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        start(ready(course = 1, incoherent = true), EstadoPreparacionUi.GUARDADO_PENDIENTE,
            onHistory = { reviews++ })
        compose.onNodeWithText(context.getString(R.string.history_flow_partial)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.history_flow_review_data)).assertDoesNotExist()
        compose.onNodeWithText("0 de 50 aprobadas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.history_flow_partial)).performScrollTo()
        screenshot("07-guardado-parcial-incoherente")
        compose.onNodeWithText("Ver historial").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, reviews) }
    }
    @Test fun incoherentHistoryIsVisibleEvenWithConfirmedMarker() {
        start(ready(1, incoherent = true))
        compose.onNodeWithText("Estos conteos reflejan los estados guardados. Revisa tu historial antes de usarlos para planificar.").performScrollTo().assertIsDisplayed()
        screenshot("06-historial-incoherente")
    }
}

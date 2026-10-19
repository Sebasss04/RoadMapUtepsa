package com.example.roadmap_utepsa

import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.history.*
import com.example.roadmap_utepsa.domain.model.*
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import com.example.roadmap_utepsa.ui.theme.RoadMap_UtepsaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Integración T1/T2/T4 con catálogo de pruebas y repositorios exclusivamente en memoria. */
class ResumenIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = ofertaFixture()
    private val first = fixture.materias.single { it.codigo == "BMS-300" }
    private val second = fixture.materias.single { it.codigo == "SCC-300" }
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("summary-first")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) = error("No Google real")
        override fun signOut() { uidChanges.value = null }
    }
    private inner class Environment {
        val auth = Auth()
        var approved = false
        var writes = 0
        var expected: Long? = null
        val saved = mutableMapOf("summary-first" to Oferta(fixture.idPlan, setOf(first.id), 1))
        val recommendationOffers = mutableListOf<Set<String>?>()
        fun data(uid: String) = fixture.copy(uid = uid, estados =
            if (approved && uid == "summary-first") mapOf(first.id to EstadoAcademico.APROBADA) else emptyMap())
        val academic = object : AcademicRepository {
            override suspend fun load(uid: String) = data(uid)
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> = error("No escrituras académicas")
        }
        val profile = object : ProfileRepository {
            override suspend fun load(uid: String) = ProfileData("Prueba", "Local",
                AssignedStudyPlan(fixture.idPlan, "Sistemas", "615/2016"), ProfileRegistration.COMPLETE)
            override suspend fun save(uid: String, names: String, surnames: String): ProfileData = error("No escritura de perfil")
        }
        val prep = object : PreparacionRepository {
            override suspend fun load(uid: String) = LecturaPreparacion(data(uid), RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1"), null)
            override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion = error("No escribir")
            override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion = error("No escribir")
            override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No escribir")
            override suspend fun reanudar(uid: String, idOperacion: String, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No escribir")
        }
        val offer = object : OfertaRepository {
            override suspend fun load(uid: String) = DatosOferta(uid, fixture.idPlan, fixture.materias, saved[uid], data(uid).estados)
            override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta {
                check(saved[uid]?.revision == expectedRevision)
                writes++; expected = expectedRevision
                return Oferta(fixture.idPlan, materias, (expectedRevision ?: 0) + 1).also { saved[uid] = it }
            }
        }
        val recommendations = RecomendacionesRepository { uid ->
            recommendationOffers += saved[uid]?.materias
            DatosRecomendaciones(prep.load(uid), saved[uid], 1)
        }
    }
    private fun start(env: Environment) {
        compose.setContent { RoadMap_UtepsaTheme {
            AppNavigation(remember { AuthViewModel(env.auth) }, compose.activity, env.profile,
                env.academic, env.auth, env.prep, env.offer, env.recommendations)
        } }
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
    }
    private fun tab(label: String) = compose.onNodeWithText(label).assertIsEnabled().performClick()
    private fun editOffer() {
        tab("Oferta")
        compose.onNodeWithText("Editar materias").performScrollTo().performClick()
        compose.onNodeWithTag("offer-search").performTextInput(second.codigo)
        compose.onNodeWithTag("offer-search").performImeAction()
        compose.onNodeWithText(second.nombre).performScrollTo().performClick()
    }
    @Test fun fourDestinationsPreserveFiltersDraftRevisionAndSavedRecommendationInput() {
        val env = Environment(); start(env)
        compose.onNodeWithText("Buscar materia o sigla").performScrollTo().performTextReplacement(first.codigo)
        Espresso.closeSoftKeyboard()
        editOffer()
        tab("Resumen")
        compose.onNodeWithTag("summary-screen").assertIsDisplayed()
        compose.onNodeWithText("0 %").assertIsDisplayed()
        tab("Para ti")
        compose.onNodeWithText("Recomendadas para ti").assertIsDisplayed()
        compose.runOnIdle { assertEquals(setOf(first.id), env.recommendationOffers.last()) }
        tab("Resumen"); tab("Malla")
        compose.onNodeWithText("Buscar materia o sigla").assertTextContains(first.codigo)
        tab("Oferta")
        compose.onNodeWithTag("offer-search").assertTextContains(second.codigo)
        compose.onNodeWithText("2 materias seleccionadas").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, env.writes) }
        compose.onNodeWithTag("offer-save").performClick()
        compose.onNodeWithText("Oferta guardada").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1L, env.expected); assertEquals(1, env.writes) }
        tab("Para ti")
        compose.runOnIdle { assertEquals(setOf(first.id, second.id), env.recommendationOffers.last()) }
    }
    @Test fun summaryReturnsFromProfileAndKeepsOfferDiscardWarning() {
        val env = Environment(); start(env); editOffer(); tab("Resumen")
        compose.onNodeWithContentDescription("Editar perfil").performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.academic_return)).performScrollTo().performClick()
        compose.onNodeWithTag("summary-screen").assertIsDisplayed()
        compose.onNodeWithContentDescription("Editar perfil").performClick()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.offer_discard_detail)).assertIsDisplayed()
        compose.onNodeWithText("Seguir aquí").performClick()
        compose.runOnIdle { assertEquals("summary-first", env.auth.currentUid); assertEquals(0, env.writes) }
    }
    @Test fun accountChangeDropsSummaryProgressAndOfferDraft() {
        val env = Environment(); env.approved = true; start(env)
        editOffer(); tab("Resumen")
        compose.onNodeWithText("2 %").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.summary_recommendations)).performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Recomendadas para ti").assertIsDisplayed()
        tab("Resumen")
        compose.runOnIdle { env.auth.uidChanges.value = "summary-second" }
        compose.onNodeWithText("Tu malla curricular").assertIsDisplayed()
        tab("Resumen"); compose.onNodeWithText("0 %").assertIsDisplayed()
        tab("Oferta"); compose.onNodeWithText("Añade tu primera oferta").assertIsDisplayed()
        compose.onNodeWithTag("offer-search").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, env.writes) }
    }
}

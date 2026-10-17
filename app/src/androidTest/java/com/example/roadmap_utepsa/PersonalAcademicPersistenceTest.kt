package com.example.roadmap_utepsa

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.ReglasAcademicas
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.domain.model.ProfileRegistration
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

/**
 * REAL, opt-in y limitada a TRBA-330 ausente -> EN_CURSO -> ausencia original.
 * No inicia sesión, no cambia cuentas ni usa Admin SDK. Nunca ejecutar como suite.
 * Baseline con UID solo en files privados; los informes exportables no contienen UID.
 */
class PersonalAcademicPersistenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val container get() = (context.applicationContext as RoadMapApplication).container
    private val baseline get() = File(context.filesDir, "personal-trba330-baseline.json")
    private val runId get() = checkNotNull(InstrumentationRegistry.getArguments().getString("personal_run"))
        .also { check(UUID.fromString(it).toString() == it) { "Identificador de ejecución inválido" } }
    private data class Guard(val uid: String, val id: String, val session: Any, val doc: DocumentReference)

    private fun authorization() {
        runId
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("personal_project") == "roadmap-utepsa-c9409" &&
            args.getString("personal_subject") == "TRBA-330" &&
            args.getString("personal_permission") == "TRBA330_PENDING_ENCURSO_RESTORE") {
            "Prueba personal no autorizada: se requieren los tres argumentos exactos"
        }
        check(FirebaseApp.getInstance().options.projectId == "roadmap-utepsa-c9409") { "Proyecto diferente; sin escrituras" }
    }

    private suspend fun guard(requireEligible: Boolean = false): Guard {
        authorization()
        val session = checkNotNull(FirebaseAuth.getInstance().currentUser) { "No hay sesión existente; sin escrituras" }
        val uid = session.uid
        val profile = container.profileRepository.load(uid)
        check(profile.registration == ProfileRegistration.COMPLETE && profile.plan.id == "sistemas-rm-615-2016") {
            "Perfil no compatible; sin escrituras"
        }
        val data = container.academicRepository.load(uid)
        check(data.uid == uid) { "Identidad académica diferente" }
        val materia = data.materias.single { it.codigo == "TRBA-330" }
        check(!requireEligible || ReglasAcademicas(data.materias, data.prerrequisitos).evaluar(materia.id, data.estados).habilitada) {
            "TRBA-330 no está habilitada; sin escrituras"
        }
        return Guard(uid, materia.id, session, FirebaseFirestore.getInstance()
            .collection("estudiantes").document(uid).collection("estados").document(materia.id)).also(::sameSession)
    }

    private fun sameSession(guard: Guard) {
        val current = FirebaseAuth.getInstance().currentUser
        check(current != null && current === guard.session && current.uid == guard.uid) { "La sesión cambió; no se escribirá en otra cuenta" }
    }

    private suspend fun server(guard: Guard): DocumentSnapshot {
        sameSession(guard)
        return guard.doc.get(Source.SERVER).await().also {
            sameSession(guard)
            check(!it.metadata.isFromCache && !it.metadata.hasPendingWrites()) { "Lectura sin confirmación del servidor" }
        }
    }

    private fun requireBaseline(guard: Guard, requireAttempt: Boolean = true) {
        check(baseline.isFile) { "Falta baseline privado; no se permite inferir el estado original" }
        val saved = JSONObject(baseline.readText())
        check(saved.getString("uid") == guard.uid && saved.getString("id") == guard.id &&
            saved.getString("subject") == "TRBA-330" && saved.getBoolean("original_absent") &&
            saved.getString("run_id") == runId && (!requireAttempt || saved.getBoolean("write_attempted"))) {
            "Baseline o cuenta diferentes; sin escrituras"
        }
    }

    private fun evidence(stage: String, status: String, restored: Boolean? = null) {
        val folder = File(context.getExternalFilesDir(null), "personal-history-evidence/$runId").apply { mkdirs() }
        val json = JSONObject().put("stage", stage).put("status", status)
            .put("run_id", runId)
            .put("project", "roadmap-utepsa-c9409").put("subject", "TRBA-330")
            .put("environment", "Firebase real; SDK Android autenticado; sin Admin")
            .put("client_time_ms", System.currentTimeMillis())
        restored?.let { json.put("original_absence_verified_server", it) }
        // Solo estos booleanos de la ejecución actual salen del almacenamiento privado.
        runCatching {
            if (baseline.isFile) {
                val saved = JSONObject(baseline.readText())
                if (saved.optString("run_id") == runId) {
                    json.put("write_attempted", saved.getBoolean("write_attempted"))
                        .put("write_confirmed", saved.getBoolean("write_confirmed"))
                }
            }
        }
        File(folder, "$stage.json").writeText(json.toString(2))
    }

    private fun openSubject(id: String) {
        compose.waitUntil(45_000) { compose.onAllNodesWithTag("curriculum-list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("curriculum-list").performScrollToIndex(0)
        val readySubtitle = context.getString(R.string.academic_subtitle, 50)
        try {
            compose.waitUntil(90_000) {
                compose.onAllNodesWithText(readySubtitle).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (_: Throwable) {
            throw AssertionError("La malla no confirmó catálogo completo antes de abrir la materia")
        }
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasText("Buscar materia o sigla"))
        compose.onNodeWithText("Buscar materia o sigla").performTextReplacement("TRBA-330")
        compose.onNodeWithTag("curriculum-list").performScrollToNode(hasTestTag("subject-$id"))
        compose.onNodeWithTag("subject-$id").performClick()
        compose.waitForIdle()
    }

    private fun selectAndSave(state: EstadoAcademico) {
        compose.onNodeWithTag("state-${state.name}").performScrollTo().performClick()
        // TRBA-330 no tiene umbral; H23 cambia la etiqueta al devolver EN_CURSO a PENDIENTE.
        val saveLabel = if (state == EstadoAcademico.PENDIENTE) "Guardar como Pendiente" else "Guardar estado"
        val saveButton = compose.onNodeWithText(saveLabel)
        if (state == EstadoAcademico.PENDIENTE) saveButton.assertIsDisplayed().performClick()
        else saveButton.performScrollTo().performClick()
        compose.waitUntil(45_000) { compose.onAllNodesWithTag("academic-editor").fetchSemanticsNodes().isEmpty() }
    }

    /**
     * Rollback limitado al intento registrado: compara y borra en una sola transacción.
     * El esquema sin versión/autor no distingue otro EN_CURSO idéntico anterior a la lectura;
     * un cambio durante la transacción provoca reintento y se vuelve a comprobar el valor.
     */
    private suspend fun restore(guard: Guard): Boolean {
        requireBaseline(guard, requireAttempt = false)
        val saved = JSONObject(baseline.readText())
        if (!saved.getBoolean("write_attempted") || !saved.getBoolean("write_confirmed")) {
            // Un CAS rechazado podría encontrar EN_CURSO escrito por otro cliente.
            // Un envío ambiguo tampoco demuestra autoría: conservarlo para revisión.
            return !server(guard).exists()
        }
        sameSession(guard)
        FirebaseFirestore.getInstance().runTransaction { transaction ->
            sameSession(guard)
            val actual = transaction.get(guard.doc)
            if (actual.exists()) {
                check(actual.data == mapOf("estado" to "EN_CURSO")) {
                    "Estado remoto inesperado; restauración requiere revisión"
                }
                sameSession(guard)
                transaction.delete(guard.doc)
            }
        }.await()
        sameSession(guard)
        return !server(guard).exists()
    }

    @Test fun stage1SaveEnCursoFromApp() = runBlocking {
        authorization()
        evidence("stage1", "STARTED_NO_WRITE")
        val g = try {
            guard(requireEligible = true).also {
                check(!baseline.exists()) { "Baseline previo" }
                check(!server(it).exists()) { "Original diferente" }
            }
        } catch (_: Throwable) {
            evidence("stage1", "ABORTED_NO_WRITE_AUTH_PROFILE_BASELINE_OR_ORIGINAL_GUARD")
            throw AssertionError("Prueba personal abortada sin escrituras: guarda de sesión, perfil, baseline u original")
        }
        baseline.writeText(JSONObject().put("uid", g.uid).put("id", g.id).put("subject", "TRBA-330")
            .put("original_absent", true).put("run_id", runId)
            .put("write_attempted", false).put("write_confirmed", false).toString())
        var savingAttempted = false
        try {
            openSubject(g.id)
            compose.onNodeWithTag("state-PENDIENTE").assertIsSelected()
            sameSession(g)
            check(!server(g).exists()) { "El estado original cambió antes de guardar" }
            requireBaseline(g, requireAttempt = false)
            // Persistir antes de enviar: permite recuperar si muere el proceso tras el envío.
            baseline.writeText(JSONObject(baseline.readText()).put("write_attempted", true).toString())
            savingAttempted = true
            selectAndSave(EstadoAcademico.EN_CURSO)
            check(server(g).data == mapOf("estado" to "EN_CURSO")) { "EN_CURSO no confirmado por servidor" }
            baseline.writeText(JSONObject(baseline.readText()).put("write_confirmed", true).toString())
            evidence("stage1", "EN_CURSO_CONFIRMED_SERVER")
        } catch (_: Throwable) {
            val restored = if (savingAttempted) runCatching { restore(g) }.getOrDefault(false) else true
            evidence("stage1", "FAILED", restored)
            throw AssertionError("Prueba personal etapa 1 fallida; revisar informe de restauración sin datos personales")
        }
    }

    @Test fun stage2ReopenAndRestoreFromApp() = runBlocking {
        authorization()
        evidence("stage2", "STARTED")
        val g = guard()
        requireBaseline(g)
        check(JSONObject(baseline.readText()).getBoolean("write_confirmed")) {
            "Guardado anterior sin confirmación propia; conservar estado para revisión"
        }
        try {
            check(server(g).data == mapOf("estado" to "EN_CURSO")) { "La reapertura no conserva EN_CURSO" }
            openSubject(g.id)
            compose.onNodeWithTag("state-EN_CURSO").assertIsSelected()
            sameSession(g)
            selectAndSave(EstadoAcademico.PENDIENTE)
            check(!server(g).exists()) { "La restauración no recuperó la ausencia original" }
            evidence("stage2", "REOPEN_EN_CURSO_AND_UI_RESTORE_CONFIRMED", true)
        } catch (_: Throwable) {
            val restored = runCatching { restore(g) }.getOrDefault(false)
            evidence("stage2", "FAILED", restored)
            throw AssertionError("Prueba personal etapa 2 fallida; revisar informe de restauración sin datos personales")
        }
    }

    @Test fun stage3ReopenVerifyExactOriginal() = runBlocking {
        var absenceVerified: Boolean? = null
        try {
            authorization()
            evidence("stage3", "STARTED")
            val g = guard()
            requireBaseline(g, requireAttempt = false)
            check(!server(g).exists()) { "Ausencia original no confirmada tras reapertura" }
            absenceVerified = true
            openSubject(g.id)
            compose.onNodeWithTag("state-PENDIENTE").assertIsSelected()
            evidence("stage3", "REOPEN_ORIGINAL_PENDING_AND_SERVER_ABSENCE_CONFIRMED", true)
            check(baseline.delete()) { "La prueba acabó; no se pudo retirar el baseline privado" }
        } catch (_: Throwable) {
            runCatching { evidence("stage3", "FAILED", absenceVerified) }
            // No propagar la excepción Compose: puede incluir el árbol de UI personal.
            throw AssertionError("Prueba personal etapa 3 fallida; revisar evidencia saneada de ausencia y conservar baseline privado")
        }
    }

    @Test fun emergencyRestoreKnownBaseline() = runBlocking {
        authorization()
        val g = guard()
        val restored = restore(g)
        evidence("emergency", if (restored) "RESTORED_BY_AUTHENTICATED_APP_SDK" else "RESTORATION_INCOMPLETE", restored)
        check(restored) { "Restauración no confirmada; requiere revisión" }
    }
}

package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.RoadMapApplication
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.MainActivity

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Opt-in de solo lectura con sesión existente. No confirma, pospone ni recupera operaciones.
 * Comparaciones personales exclusivamente en memoria; exportación solo booleanos y conteos.
 * Ejecutar únicamente después de inyectar preparación en producción y restaurar TRBA-330.
 */
class ProductionPreparationReadOnlyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private data class ReadDocument(val exists: Boolean, val data: Map<String, Any>?)

    private fun confirmed(snapshot: DocumentSnapshot): ReadDocument {
        check(!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()) {
            "Lectura sin confirmación de servidor"
        }
        return ReadDocument(snapshot.exists(), snapshot.data)
    }

    private fun evidence(result: JSONObject) {
        val folder = File(context.getExternalFilesDir(null), "production-preparation-read-only-evidence")
        check(folder.isDirectory || folder.mkdirs()) { "No se pudo guardar evidencia saneada" }
        File(folder, "read-only-${System.currentTimeMillis()}.json").writeText(result.toString(2))
    }

    @Test fun injectedPreparationLoadsWithoutChangingPreparationOrJournal() = runBlocking {
        val result = JSONObject().put("success", false).put("write_actions_invoked", false)
        try {
            check(InstrumentationRegistry.getArguments().getString("preparation_read_only") == "roadmap-utepsa-c9409") {
                "Lectura real sin argumento opt-in exacto"
            }
            check(FirebaseApp.getInstance().options.projectId == "roadmap-utepsa-c9409") {
                "Proyecto incompatible"
            }
            result.put("stage", "session")
            val auth = FirebaseAuth.getInstance()
            val session = checkNotNull(auth.currentUser) { "Se requiere sesión existente" }
            val uid = session.uid
            fun sameSession() {
                check(auth.currentUser === session && auth.currentUser?.uid == uid) { "La sesión cambió" }
            }
            val container = (context.applicationContext as RoadMapApplication).container
            val db = FirebaseFirestore.getInstance()
            val student = db.collection("estudiantes").document(uid)
            suspend fun preparation() = confirmed(student.collection("configuracion_academica")
                .document("historial").get(Source.SERVER).await()).also { sameSession() }
            suspend fun journal() = confirmed(student.collection("operaciones_historial")
                .document("actual").get(Source.SERVER).await()).also { sameSession() }

            suspend fun states(): Map<String, Map<String, Any>> {
                val snapshot = student.collection("estados").get(Source.SERVER).await()
                check(!snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites())
                sameSession()
                return snapshot.documents.associate { it.id to checkNotNull(confirmed(it).data) }
            }
            withTimeout(150_000) {
                result.put("stage", "profile")
                val profile = container.profileRepository.load(uid)
                sameSession()
                check(profile.registration == ProfileRegistration.COMPLETE && profile.plan.id == "sistemas-rm-615-2016") {
                    "Perfil incompatible"
                }
                result.put("stage", "before-snapshot")
                val beforeStates = states()
                val beforePreparation = preparation()
                val beforeJournal = journal()
                // Reutiliza el adaptador productivo; no instala repositorios simulados en MainActivity.
                result.put("stage", "preparation-load")
                val readStart = android.os.SystemClock.elapsedRealtime()
                val loaded = container.preparacionRepository.load(uid)
                result.put("preparation_load_ms", android.os.SystemClock.elapsedRealtime() - readStart)
                sameSession()
                check(loaded.academic.uid == uid && loaded.academic.materias.size == 50) {
                    "Lectura académica incompleta"
                }
                val operation = loaded.operacion
                val activeOperation = operation != null && !operation.completada && !operation.cancelada
                val hasPreparation = loaded.registro != null
                val welcome = context.getString(R.string.history_flow_welcome)
                val recover = context.getString(R.string.history_flow_partial)
                result.put("stage", "expected-ui")
                compose.waitUntil(90_000) {
                    when {
                        activeOperation -> compose.onAllNodesWithText(recover).fetchSemanticsNodes().isNotEmpty()
                        hasPreparation -> compose.onAllNodesWithTag("curriculum-list").fetchSemanticsNodes().isNotEmpty()
                        else -> compose.onAllNodesWithText(welcome).fetchSemanticsNodes().isNotEmpty()
                    }
                }
                compose.waitForIdle()
                sameSession()
                result.put("stage", "after-snapshot")
                val afterStates = states()
                val afterPreparation = preparation()
                val afterJournal = journal()
                check(beforeStates == afterStates) { "Estados cambiaron durante la lectura" }
                check(beforePreparation == afterPreparation) { "Preparación cambió durante la lectura" }
                check(beforeJournal == afterJournal) { "Diario cambió durante la lectura" }
                check(beforePreparation.exists == (loaded.registro != null) &&
                    beforeJournal.exists == (loaded.operacion != null)) { "Lecturas concurrentes diferentes" }
                result.put("subject_count", loaded.academic.materias.size)
                    .put("preparation_present", hasPreparation)
                    .put("active_operation_present", activeOperation)
                    .put("expected_ui_verified", true)
                    .put("states_unchanged", true)
                    .put("states_fingerprint", java.security.MessageDigest.getInstance("SHA-256")
                        .digest(beforeStates.toSortedMap().entries.joinToString("|") { (id, fields) -> "$id:${fields.toSortedMap()}" }.toByteArray())
                        .joinToString("") { "%02x".format(it) })
                    .put("preparation_unchanged", true)
                    .put("journal_unchanged", true)
                    .put("same_session_verified", true)
                    .put("server_confirmed_reads", true)
                    .put("stage", "complete").put("success", true)
            }
            evidence(result)
        } catch (error: Throwable) {
            result.put("failure_type", error.javaClass.simpleName)
            val firestore = generateSequence(error) { it.cause }
                .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>().firstOrNull()
            firestore?.let { result.put("firestore_code", it.code.name) }
            // Solo clase/código y etapa; no mensajes SDK, mapas ni información de cuenta.
            InstrumentationRegistry.getInstrumentation().sendStatus(2, android.os.Bundle().apply {
                putString("readonly_failure", result.toString())
            })
            runCatching { evidence(result) }
            // Evita que fallos de Compose/SDK impriman mapas, UID o el árbol de UI personal.
            throw AssertionError("Lectura de preparación sin completar; revisar evidencia de booleanos sin datos personales")
        }
    }
}

package com.example.roadmap_utepsa

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Compatible con las APK anterior y nueva. Solo lecturas; sin Activity ni modelos propios. */
class SaeMigrationReadOnlyAuditTest {
    private fun canonical(value: Any?): String = when (value) {
        is Map<*, *> -> value.entries.sortedBy { it.key.toString() }
            .joinToString(prefix = "{", postfix = "}") { "${it.key}:${canonical(it.value)}" }
        is List<*> -> value.joinToString(prefix = "[", postfix = "]") { canonical(it) }
        else -> value.toString()
    }
    private fun fingerprint(value: Any?): String = MessageDigest.getInstance("SHA-256")
        .digest(canonical(value).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    @Test fun snapshotPersonalAcademicDataWithoutWrites(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val result = JSONObject().put("success", false).put("write_actions_invoked", false)
        try {
            check(InstrumentationRegistry.getArguments().getString("sae_read_only_audit") == "roadmap-utepsa-c9409")
            check(FirebaseApp.getInstance().options.projectId == "roadmap-utepsa-c9409")
            val auth = FirebaseAuth.getInstance()
            val session = checkNotNull(auth.currentUser)
            val student = FirebaseFirestore.getInstance().collection("estudiantes").document(session.uid)
            suspend fun snapshot(): Map<String, Any?> {
                val states = student.collection("estados").get(Source.SERVER).await()
                check(!states.metadata.isFromCache && !states.metadata.hasPendingWrites())
                suspend fun control(collection: String, document: String): Map<String, Any>? {
                    val read = student.collection(collection).document(document).get(Source.SERVER).await()
                    check(!read.metadata.isFromCache && !read.metadata.hasPendingWrites())
                    return read.data
                }
                return mapOf("states" to states.documents.associate { it.id to it.data },
                    "preparation" to control("configuracion_academica", "historial"),
                    "journal" to control("operaciones_historial", "actual"))
            }
            withTimeout(120_000) {
                val before = snapshot()
                val after = snapshot()
                check(auth.currentUser === session && before == after)
                val journal = before["journal"] as? Map<*, *>
                check(journal?.get("estado") != "EN_PROGRESO")
                result.put("states_fingerprint", fingerprint(before["states"]))
                    .put("preparation_fingerprint", fingerprint(before["preparation"]))
                    .put("journal_fingerprint", fingerprint(before["journal"]))
                    .put("same_session_verified", true).put("stable_server_reads", true)
                    .put("active_operation_present", false).put("success", true)
            }
        } catch (_: Throwable) {
            // No imprimir UID, estados, tokens ni árboles de UI personales.
            throw AssertionError("Auditoría de solo lectura sin completar; no se modificaron datos académicos")
        } finally {
            val folder = File(instrumentation.targetContext.getExternalFilesDir(null), "sae-read-only-audit")
            check(folder.isDirectory || folder.mkdirs())
            File(folder, "audit-${System.currentTimeMillis()}.json").writeText(result.toString(2))
        }
    }
}

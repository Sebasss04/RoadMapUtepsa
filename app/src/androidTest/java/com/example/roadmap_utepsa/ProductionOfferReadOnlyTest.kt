package com.example.roadmap_utepsa

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.hasText
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Explicit opt-in. Reads the existing session; never saves or changes selections. */
class ProductionOfferReadOnlyTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun currentOfferLoadsAndEditorShowsCatalogWithoutWrites(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val result = JSONObject().put("success", false).put("write_actions_invoked", false)
        try {
            check(InstrumentationRegistry.getArguments().getString("offer_read_only") == "roadmap-utepsa-c9409")
            check(FirebaseApp.getInstance().options.projectId == "roadmap-utepsa-c9409")
            val auth = FirebaseAuth.getInstance()
            val session = checkNotNull(auth.currentUser)
            val student = FirebaseFirestore.getInstance().collection("estudiantes").document(session.uid)
            suspend fun snapshot(): Map<String, Any?> {
                val states = student.collection("estados").get(Source.SERVER).await()
                check(!states.metadata.isFromCache && !states.metadata.hasPendingWrites())
                suspend fun document(collection: String, id: String): Any? {
                    val doc = student.collection(collection).document(id).get(Source.SERVER).await()
                    check(!doc.metadata.isFromCache && !doc.metadata.hasPendingWrites())
                    return doc.data
                }
                return mapOf("states" to states.documents.associate { it.id to it.data },
                    "history" to document("configuracion_academica", "historial"),
                    "journal" to document("operaciones_historial", "actual"),
                    "revision" to document("control_academico", "revision"),
                    "offer" to document("ofertas", "actual"))
            }
            withTimeout(150_000) {
                result.put("stage", "before")
                val before = snapshot()
                result.put("stage", "repository")
                val container = (context.applicationContext as RoadMapApplication).container
                val loaded = container.ofertaRepository.load(session.uid)
                check(loaded.uid == session.uid && loaded.catalogo.size == 50)
                result.put("stage", "navigation")
                val offerLabel = context.getString(R.string.academic_offer)
                compose.waitUntil(60_000) { compose.onAllNodesWithText(offerLabel).fetchSemanticsNodes().isNotEmpty() }
                compose.onNodeWithText(offerLabel).performClick()
                val expected = context.getString(if (loaded.oferta == null) R.string.offer_absent
                    else if (loaded.oferta.materias.isEmpty()) R.string.offer_empty else R.string.offer_saved)
                compose.waitUntil(60_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
                check(compose.onAllNodesWithText(context.getString(R.string.offer_read_error)).fetchSemanticsNodes().isEmpty())
                result.put("stage", "editor")
                val editLabel = context.getString(if (loaded.oferta == null) R.string.offer_select
                    else if (loaded.oferta.materias.isEmpty()) R.string.offer_add else R.string.offer_edit)
                compose.onNodeWithTag("offer-list").performScrollToNode(hasText(editLabel))
                compose.onNodeWithText(editLabel).performClick()
                compose.waitUntil(10_000) { compose.onAllNodesWithTag("offer-search").fetchSemanticsNodes().isNotEmpty() }
                val approved = loaded.catalogo.firstOrNull { it.id in loaded.aprobadas }
                if (approved != null) {
                    compose.onNodeWithTag("offer-search").performTextReplacement(approved.codigo)
                    compose.onNodeWithTag("offer-search").performImeAction()
                    val emptyMessage = context.getString(if (loaded.aprobadas.size == loaded.catalogo.size)
                        R.string.offer_all_approved else R.string.offer_no_results)
                    compose.onNodeWithTag("offer-list").performScrollToNode(hasText(emptyMessage))
                    check(compose.onAllNodesWithText(approved.nombre).fetchSemanticsNodes().isEmpty())
                    result.put("approved_search_excluded", true)
                }
                val available = loaded.catalogo.firstOrNull { it.id !in loaded.aprobadas }
                if (available != null) {
                    compose.onNodeWithTag("offer-search").performTextReplacement(available.codigo)
                    compose.onNodeWithTag("offer-search").performImeAction()
                    compose.onNodeWithTag("offer-list").performScrollToNode(hasText(available.nombre))
                    check(compose.onAllNodesWithText(available.nombre).fetchSemanticsNodes().isNotEmpty())
                    result.put("unapproved_search_available", true)
                }
                compose.onNodeWithText(context.getString(R.string.offer_cancel)).performClick()
                if (loaded.oferta?.materias?.any { it in loaded.aprobadas } == true) {
                    compose.onNodeWithText(context.getString(R.string.offer_confirm)).performClick()
                }
                result.put("stage", "after")
                check(before == snapshot())
                check(auth.currentUser === session)
                result.put("subject_count", loaded.catalogo.size).put("offer_present", loaded.oferta != null)
                    .put("server_read_verified", true).put("offer_screen_verified", true)
                    .put("editor_verified", true).put("approved_count", loaded.aprobadas.size)
                    .put("academic_and_offer_unchanged", true)
                    .put("same_session_verified", true).put("stage", "complete").put("success", true)
            }
        } catch (error: Throwable) {
            result.put("failure_type", error.javaClass.simpleName)
            generateSequence(error) { it.cause }
                .filterIsInstance<com.google.firebase.firestore.FirebaseFirestoreException>().firstOrNull()
                ?.let { result.put("firestore_code", it.code.name) }
            instrumentation.sendStatus(2, android.os.Bundle().apply { putString("offer_readonly_result", result.toString()) })
            throw AssertionError("Offer read-only verification incomplete; inspect sanitized evidence")
        } finally {
            val folder = File(context.getExternalFilesDir(null), "production-offer-read-only")
            check(folder.isDirectory || folder.mkdirs())
            File(folder, "result-${System.currentTimeMillis()}.json").writeText(result.toString(2))
        }
    }
}

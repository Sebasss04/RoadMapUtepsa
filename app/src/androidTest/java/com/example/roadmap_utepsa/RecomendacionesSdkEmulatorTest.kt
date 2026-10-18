package com.example.roadmap_utepsa

import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.domain.recommendation.MotorRecomendaciones
import com.google.firebase.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

/** Secondary FirebaseApp, synthetic accounts, demo project and local emulators only. */
class RecomendacionesSdkEmulatorTest {
    private lateinit var app: FirebaseApp
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var uid: String
    private lateinit var academic: FirestoreAcademicRepository
    private lateinit var prep: FirestorePreparacionRepository
    private lateinit var offer: FirestoreOfertaRepository
    private lateinit var repo: RecomendacionesRepository
    @Before fun connectDemoOnly() = runBlocking {
        check(InstrumentationRegistry.getArguments().getString("recommendations_sdk_emulator") == "demo-roadmap-utepsa:8089:9099")
        app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
            FirebaseOptions.Builder().setProjectId("demo-roadmap-utepsa").setApplicationId("1:1234567890:android:1234567890abcdef")
                .setApiKey("local-emulator-only-key").build(), "recommendations-${UUID.randomUUID()}")
        auth = FirebaseAuth.getInstance(app); auth.useEmulator("10.0.2.2", 9099)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        db.useEmulator("10.0.2.2", 8089)
        withTimeout(60_000) {
            uid = checkNotNull(auth.createUserWithEmailAndPassword("recommendations-${UUID.randomUUID()}@example.test", "Demo-only-password-2026").await().user).uid
            val profile = FirestoreProfileRepository(db, auth); profile.save(uid, "Prueba", "Recomendaciones")
            academic = FirestoreAcademicRepository(profile, db, auth)
            prep = FirestorePreparacionRepository(academic, db, auth)
            offer = FirestoreOfertaRepository(academic, db, auth)
            repo = ConsultaRecomendacionesRepository(prep, offer, EstadosVersionados(db, auth)::revisionConsulta) { auth.currentUser }
        }
    }
    private suspend fun snapshot(): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for (path in listOf("configuracion_academica/historial", "operaciones_historial/actual", "control_academico/revision", "ofertas/actual")) {
            val doc = db.document("estudiantes/$uid/$path").get(Source.SERVER).await()
            result[path] = doc.data
        }
        result["estados"] = db.collection("estudiantes/$uid/estados").get(Source.SERVER).await().documents.associate { it.id to it.data }
        return result
    }
    @Test fun guardedReadsRecalculateAfterSavedChangesAndNeverModifySnapshots() = runBlocking {
        withTimeout(150_000) {
            val first = prep.load(uid)
            prep.confirmar(first, BorradorHistorial(uid, first.academic.estados))
            val data = academic.load(uid)
            assertEquals(50, data.materias.size)
            offer.save(uid, data.materias.map { it.id }.toSet(), null)
            val before = snapshot()
            val loaded = repo.load(uid)
            val result = MotorRecomendaciones().calcular(loaded.preparacion.academic, checkNotNull(loaded.oferta))
            assertNotNull(result.principal)
            assertEquals(before, snapshot())
            val candidate = checkNotNull(result.principal).materia.id
            // Fixture changes occur outside the read under test.
            academic.saveState(uid, candidate, EstadoAcademico.APROBADA)
            offer.save(uid, setOf(candidate), 1)
            val changed = snapshot()
            val reloaded = repo.load(uid)
            assertEquals(2L, reloaded.oferta?.revision)
            assertTrue(reloaded.revisionAcademica > loaded.revisionAcademica)
            assertTrue(MotorRecomendaciones().calcular(reloaded.preparacion.academic, checkNotNull(reloaded.oferta)).candidatas.isEmpty())
            assertEquals(changed, snapshot())
            val original = auth.currentUser
            auth.signOut()
            auth.createUserWithEmailAndPassword("other-${UUID.randomUUID()}@example.test", "Demo-only-password-2026").await()
            assertNotSame(original, auth.currentUser)
            try { repo.load(uid); fail("A different account cannot read the previous student's data") } catch (_: IllegalStateException) {}
        }
    }
    @After fun closeSecondaryOnly() = runBlocking {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate().await()
        if (::app.isInitialized) app.delete()
    }
}

package com.example.roadmap_utepsa

import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.data.ConflictoHistorial
import com.example.roadmap_utepsa.data.FirestoreAcademicRepository
import com.example.roadmap_utepsa.data.FirestorePreparacionRepository
import com.example.roadmap_utepsa.data.FirestoreProfileRepository
import com.example.roadmap_utepsa.data.CoordinadorPreparacionRepository
import com.example.roadmap_utepsa.data.FirebasePreparacionSource
import com.example.roadmap_utepsa.data.PreparacionSource
import com.example.roadmap_utepsa.data.InstantaneaPreparacion
import com.example.roadmap_utepsa.data.EscrituraPreparacion
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/** SDK Android real, exclusivamente Auth/Firestore emulados. Sin Activity ni default auth. */
class OfertaSdkEmulatorTest {
    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var academic: FirestoreAcademicRepository
    private lateinit var repository: com.example.roadmap_utepsa.data.FirestoreOfertaRepository
    private lateinit var uid: String

    @Before fun connectOnlyToLocalDemo() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("offer_sdk_emulator") == "demo-roadmap-utepsa:8089:9099") {
            "Solo ejecución opt-in contra emuladores locales"
        }
        val options = FirebaseOptions.Builder().setProjectId("demo-roadmap-utepsa")
            .setApplicationId("1:1234567890:android:1234567890abcdef")
            .setApiKey("local-emulator-only-key").build()
        app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
            options, "offer-sdk-${UUID.randomUUID()}")
        auth = FirebaseAuth.getInstance(app)
        val host = args.getString("offer_sdk_host") ?: "10.0.2.2"
        require(host in setOf("10.0.2.2", "127.0.0.1"))
        auth.useEmulator(host, 9099)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        db.useEmulator(host, 8089)
        withTimeout(60_000) {
            uid = checkNotNull(auth.createUserWithEmailAndPassword("offer-${UUID.randomUUID()}@example.test", "Demo-only-password-2026").await().user).uid
            val profile = FirestoreProfileRepository(db, auth)
            profile.save(uid, "Prueba", "SDK Local")
            academic = FirestoreAcademicRepository(profile, db, auth)
            repository = com.example.roadmap_utepsa.data.FirestoreOfertaRepository(academic, db, auth)
        }
    }

    @After fun closeSecondaryAppOnly() = runBlocking {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate().await()
        if (::app.isInitialized) app.delete()
    }

    @Test fun createReplaceEmptyRecreateAndConcurrentEditors() = runBlocking {
        withTimeout(150_000) {
            val base = repository.load(uid)
            assertNull(base.oferta)
            assertEquals(50, base.catalogo.size)
            val first = base.catalogo.first().id
            val last = base.catalogo.last().id
            val created = repository.save(uid, setOf(first), null)
            assertEquals(1L, created.revision)
            val editor2 = com.example.roadmap_utepsa.data.FirestoreOfertaRepository(academic, db, auth)
            assertEquals(created, editor2.load(uid).oferta)
            val replaced = repository.save(uid, setOf(last), 1)
            assertEquals(setOf(last), replaced.materias)
            try {
                editor2.save(uid, setOf(first), 1)
                fail("Stale editor must conflict")
            } catch (e: com.example.roadmap_utepsa.data.ConflictoOferta) {
                assertEquals(replaced, e.remota)
            }
            val empty = editor2.save(uid, emptySet(), 2)
            assertEquals(3L, empty.revision)
            assertTrue(empty.materias.isEmpty())
            assertEquals(empty, com.example.roadmap_utepsa.data.FirestoreOfertaRepository(academic, db, auth).load(uid).oferta)
            val all = repository.save(uid, base.catalogo.map { it.id }.toSet(), 3)
            assertEquals(50, all.materias.size)
            val results = kotlinx.coroutines.coroutineScope {
                listOf(setOf(first), setOf(last)).map { selected -> async {
                    try { repository.save(uid, selected, 4); "saved" }
                    catch (_: com.example.roadmap_utepsa.data.ConflictoOferta) { "conflict" }
                } }.map { it.await() }
            }
            assertEquals(1, results.count { it == "saved" })
            assertEquals(1, results.count { it == "conflict" })
            assertEquals(5L, repository.load(uid).oferta?.revision)
            val revision = db.document("estudiantes/$uid/control_academico/revision").get(Source.SERVER).await()
            assertFalse(revision.exists())
            assertTrue(academic.load(uid).estados.isEmpty())
        }
    }
}

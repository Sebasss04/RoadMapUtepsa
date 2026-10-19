package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.auth.verifyDemoUser
import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.history.ConflictoHistorial
import com.example.roadmap_utepsa.data.academic.FirestoreAcademicRepository
import com.example.roadmap_utepsa.data.history.FirestorePreparacionRepository
import com.example.roadmap_utepsa.data.profile.FirestoreProfileRepository
import com.example.roadmap_utepsa.data.history.CoordinadorPreparacionRepository
import com.example.roadmap_utepsa.data.history.FirebasePreparacionSource
import com.example.roadmap_utepsa.data.history.PreparacionSource
import com.example.roadmap_utepsa.data.history.InstantaneaPreparacion
import com.example.roadmap_utepsa.data.history.EscrituraPreparacion
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.Source
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/** SDK Android real, exclusivamente Auth/Firestore emulados. Sin Activity ni default auth. */
class PreparacionSdkEmulatorTest {
    private lateinit var app: FirebaseApp
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var academic: FirestoreAcademicRepository
    private lateinit var repository: FirestorePreparacionRepository
    private lateinit var uid: String

    @Before fun connectOnlyToLocalDemo() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        check(args.getString("history_sdk_emulator") == "demo-roadmap-utepsa:8089:9099") {
            "Solo ejecución opt-in contra emuladores locales"
        }
        val options = FirebaseOptions.Builder().setProjectId("demo-roadmap-utepsa")
            .setApplicationId("1:1234567890:android:1234567890abcdef")
            .setApiKey("local-emulator-only-key").build()
        app = FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().targetContext,
            options, "history-sdk-${UUID.randomUUID()}")
        auth = FirebaseAuth.getInstance(app)
        val host = args.getString("history_sdk_host") ?: "10.0.2.2"
        require(host in setOf("10.0.2.2", "127.0.0.1"))
        auth.useEmulator(host, 9099)
        db = FirebaseFirestore.getInstance(app)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build()).build()
        db.useEmulator(host, 8089)
        withTimeout(60_000) {
            uid = checkNotNull(auth.createUserWithEmailAndPassword("history-${UUID.randomUUID()}@example.test", "Demo-only-password-2026").await().user).uid
            verifyDemoUser(auth, host)
            val profile = FirestoreProfileRepository(db, auth)
            profile.save(uid, "Prueba", "SDK Local")
            academic = FirestoreAcademicRepository(profile, db, auth)
            repository = FirestorePreparacionRepository(academic, db, auth)
        }
    }

    @After fun closeSecondaryAppOnly() = runBlocking {
        if (::auth.isInitialized) auth.signOut()
        if (::db.isInitialized) db.terminate().await()
        if (::app.isInitialized) app.delete()
    }

    @Test fun postponePersistsServerTimestampAndRepositoryRecreation() = runBlocking {
        withTimeout(90_000) {
            val base = repository.load(uid)
            assertEquals(50, base.academic.materias.size)
            assertTrue(base.academic.estados.isEmpty())
            assertNull(base.registro)
            val saved = repository.posponer(base)
            assertEquals(DecisionPreparacion.POSPUESTO, saved.registro?.decision)
            assertTrue(checkNotNull(saved.registro).revision.matches(Regex("-?\\d+:\\d+")))
            val reopened = FirestorePreparacionRepository(academic, db, auth).load(uid)
            assertEquals(saved.registro, reopened.registro)
            assertTrue(reopened.academic.estados.isEmpty())
            assertNull(reopened.operacion)
        }
    }

    @Test fun firstHistoryPersistsApprovedAndCurrentCourseAfterRepositoryRecreation() = runBlocking {
        withTimeout(90_000) {
            val base = repository.load(uid)
            val roots = base.academic.materias.filter { it.semestre == 1 }.take(2)
            val selected = mapOf(roots[0].id to EstadoAcademico.APROBADA,
                roots[1].id to EstadoAcademico.EN_CURSO)
            val saved = repository.confirmar(base, BorradorHistorial(uid, base.academic.estados, selected))
            assertEquals(DecisionPreparacion.CONFIRMADO, saved.registro?.decision)
            val reopened = FirestorePreparacionRepository(academic, db, auth).load(uid)
            assertEquals(selected, reopened.academic.estados)
            assertEquals(saved.registro, reopened.registro)
            assertTrue(checkNotNull(reopened.operacion).completada)
        }
    }

    @Test fun fiftyChangesUseElevenTransactionsAndOneFinalSnapshot() = runBlocking {
        withTimeout(150_000) {
            val base = repository.load(uid)
            val sdk = FirebasePreparacionSource(db, auth)
            var reads = 0
            var transactions = 0
            val counting = object : PreparacionSource by sdk {
                override suspend fun leer(uid: String, ids: Set<String>): InstantaneaPreparacion {
                    reads++
                    return sdk.leer(uid, ids)
                }
                override suspend fun transaccion(uid: String, ids: Set<String>,
                    bloque: (InstantaneaPreparacion) -> EscrituraPreparacion): EscrituraPreparacion {
                    transactions++
                    return sdk.transaccion(uid, ids, bloque)
                }
            }
            val optimized = CoordinadorPreparacionRepository(academic, counting, 5)
            val selection = base.academic.materias.associate { it.id to EstadoAcademico.APROBADA }
            val start = android.os.SystemClock.elapsedRealtime()
            val result = optimized.confirmar(base, BorradorHistorial(uid, base.academic.estados, selection))
            val elapsed = android.os.SystemClock.elapsedRealtime() - start
            assertEquals(selection, result.academic.estados)
            assertEquals(DecisionPreparacion.CONFIRMADO, result.registro?.decision)
            assertTrue(checkNotNull(result.operacion).completada)
            assertEquals(11, transactions)
            assertEquals(1, reads)
            val metrics = android.os.Bundle().apply {
                putString("history_performance", "changes=50;transactions=$transactions;final_snapshots=$reads;local_elapsed_ms=$elapsed")
            }
            InstrumentationRegistry.getInstrumentation().sendStatus(2, metrics)
            assertEquals(selection, repository.load(uid).academic.estados)
        }
    }

    @Test fun fiveStateGroupInterruptionRecoversWithNewRepositoryAndDeletesPending() = runBlocking {
        withTimeout(150_000) {
            val base = repository.load(uid)
            val dependentIds = base.academic.prerrequisitos.map { it.idObjetivo }.toSet()
            val roots = base.academic.materias.filter { it.id !in dependentIds && it.minAprobadas == 0 }.take(6)
            assertEquals(6, roots.size)
            val selection = roots.associate { it.id to EstadoAcademico.APROBADA }
            try {
                repository.confirmar(base, BorradorHistorial(uid, base.academic.estados, selection)) { done, _ ->
                    if (done == 5) error("SIMULATED_CLIENT_INTERRUPTION")
                }
                fail("Se esperaba interrupción del cliente después del primer grupo confirmado")
            } catch (expected: IllegalStateException) {
                assertEquals("SIMULATED_CLIENT_INTERRUPTION", expected.message)
            }
            val reopened = FirestorePreparacionRepository(academic, db, auth)
            val partial = reopened.load(uid)
            val operation = checkNotNull(partial.operacion)
            assertEquals(5, operation.aplicados.size)
            assertEquals(5, partial.academic.estados.size)
            assertFalse(operation.completada)
            assertNull(partial.registro)
            val finished = reopened.reanudar(uid, operation.id)
            assertEquals(selection, finished.academic.estados)
            assertEquals(DecisionPreparacion.CONFIRMADO, finished.registro?.decision)
            assertTrue(checkNotNull(finished.operacion).completada)
            assertEquals(6, checkNotNull(finished.operacion).aplicados.size)
            val subject = roots.first().id
            val correction = BorradorHistorial(uid, finished.academic.estados)
                .seleccionar(subject, EstadoAcademico.PENDIENTE)
            val corrected = reopened.confirmar(finished, correction)
            assertFalse(subject in corrected.academic.estados)
            val absent = db.document("estudiantes/$uid/estados/$subject").get(Source.SERVER).await()
            assertFalse(absent.exists())
            assertFalse(absent.metadata.isFromCache)
            assertFalse(absent.metadata.hasPendingWrites())
        }
    }

    @Test fun catalogIsReusedButStatesStayFresh() = runBlocking {
        withTimeout(90_000) {
            val start = android.os.SystemClock.elapsedRealtime()
            val cold = academic.load(uid)
            val coldMs = android.os.SystemClock.elapsedRealtime() - start
            val subject = cold.materias.first().id
            val another = FirestoreAcademicRepository(FirestoreProfileRepository(db, auth), db, auth)
            another.saveState(uid, subject, EstadoAcademico.APROBADA)
            val warmStart = android.os.SystemClock.elapsedRealtime()
            val warm = academic.load(uid)
            val warmMs = android.os.SystemClock.elapsedRealtime() - warmStart
            assertEquals(EstadoAcademico.APROBADA, warm.estados[subject])
            assertEquals(cold.materias, warm.materias)
            InstrumentationRegistry.getInstrumentation().sendStatus(2, android.os.Bundle().apply {
                putString("catalog_performance", "cold_ms=$coldMs;warm_ms=$warmMs;environment=local-demo-on-device")
            })
        }
    }

    @Test fun concurrentMallaEditBetweenGroupsPreservesRemoteStateAndJournal() = runBlocking {
        withTimeout(90_000) {
            val base = repository.load(uid)
            val roots = base.academic.materias.filter { m -> m.minAprobadas == 0 &&
                base.academic.prerrequisitos.none { it.idObjetivo == m.id } }.take(7)
            assertEquals(7, roots.size)
            val selected = roots.take(6).associate { it.id to EstadoAcademico.APROBADA }
            val concurrentId = roots.last().id
            var edited = false
            try {
                repository.confirmar(base, BorradorHistorial(uid, base.academic.estados, selected)) { done, _ ->
                    if (done == 5 && !edited) {
                        edited = true
                        runBlocking { FirestoreAcademicRepository(FirestoreProfileRepository(db, auth), db, auth)
                            .saveState(uid, concurrentId, EstadoAcademico.EN_CURSO) }
                    }
                }
                fail("La edición ajena debe bloquear el siguiente grupo")
            } catch (expected: Exception) {
                assertTrue(generateSequence<Throwable>(expected) { it.cause }.any { it is ConflictoHistorial })
            }
            val actual = FirestorePreparacionRepository(academic, db, auth).load(uid)
            assertEquals(EstadoAcademico.EN_CURSO, actual.academic.estados[concurrentId])
            assertEquals(5, checkNotNull(actual.operacion).aplicados.size)
            assertFalse(checkNotNull(actual.operacion).completada)
            assertNull(actual.registro)
        }
    }

    @Test fun stalePreparationBaseRejectsWithoutOverwritingConcurrentSdkState() = runBlocking {
        withTimeout(90_000) {
            val base = repository.load(uid)
            val subject = base.academic.materias.first().id
            academic.saveState(uid, subject, EstadoAcademico.EN_CURSO)
            try {
                repository.posponer(base)
                fail("La base obsoleta debe producir conflicto")
            } catch (expected: ConflictoHistorial) {
                assertEquals(EstadoAcademico.EN_CURSO, expected.remoto.academic.estados[subject])
            }
            val actual = repository.load(uid)
            assertEquals(EstadoAcademico.EN_CURSO, actual.academic.estados[subject])
            assertNull(actual.registro)
            assertNull(actual.operacion)
        }
    }
}

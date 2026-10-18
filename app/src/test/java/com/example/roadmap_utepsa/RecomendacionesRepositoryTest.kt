package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.history.*
import com.example.roadmap_utepsa.domain.model.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

internal open class PreparacionSoloLectura(var lectura: LecturaPreparacion) : PreparacionRepository {
    var reads = 0
    override suspend fun load(uid: String): LecturaPreparacion { reads++; return lectura }
    override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion = error("No escribir")
    override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion = error("No escribir")
    override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No escribir")
    override suspend fun reanudar(uid: String, idOperacion: String, progreso: (Int, Int) -> Unit): LecturaPreparacion = error("No escribir")
}

class RecomendacionesRepositoryTest {
    private val data = AcademicFixtures().data()
    private val prep = PreparacionSoloLectura(LecturaPreparacion(data, RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1"), null))
    private var selection: Oferta? = null
    private var session: Any? = Any()
    private var offerReads = 0
    private var offerFailure = false
    private var afterOffer: () -> Unit = {}
    private val offer = object : OfertaRepository {
        override suspend fun load(uid: String): DatosOferta {
            offerReads++; if (offerFailure) error("offline"); afterOffer()
            return DatosOferta(uid, data.idPlan, data.materias, selection)
        }
        override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta = error("No escribir")
    }
    private fun repo(revision: suspend (String) -> Long = { 1L }) = ConsultaRecomendacionesRepository(prep, offer, revision) { session }
    @Test fun absentAndEmptyAreDistinctAndConsultingNeverWrites() = runTest {
        assertNull(repo().load("alice").oferta)
        selection = Oferta(data.idPlan, emptySet(), 1)
        assertEquals(emptySet<String>(), repo().load("alice").oferta?.materias)
        assertEquals(2, prep.reads)
    }
    @Test fun readErrorPropagatesRatherThanAbsence() = runTest {
        offerFailure = true
        try { repo().load("alice"); fail() } catch (_: IllegalStateException) {}
        assertEquals(1, offerReads)
    }
    @Test fun changeDuringPreparationRetriesWholePair() = runTest {
        val values = ArrayDeque(listOf(1L, 2L, 2L, 2L, 2L, 2L))
        assertEquals(2L, repo { values.removeFirst() }.load("alice").revisionAcademica)
        assertEquals(2, prep.reads); assertEquals(2, offerReads)
    }
    @Test fun changeDuringOfferRetriesWholePair() = runTest {
        val values = ArrayDeque(listOf(1L, 1L, 2L, 2L, 2L, 2L))
        repo { values.removeFirst() }.load("alice")
        assertEquals(2, prep.reads)
    }
    @Test fun continuousChangesStopAtThreeAttempts() = runTest {
        var rev = 0L
        try { repo { ++rev }.load("alice"); fail() } catch (_: IllegalStateException) {}
        assertEquals(3, prep.reads); assertEquals(3, offerReads)
    }
    @Test fun changingAccountDuringReadDiscardsPair() = runTest {
        afterOffer = { session = Any() }
        try { repo().load("alice"); fail() } catch (_: IllegalStateException) {}
    }
    @Test fun wrongUidIsRejected() = runTest {
        try { repo().load("bob"); fail() } catch (_: IllegalStateException) {}
    }
}

package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.academic.AcademicFixtures

import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.OperacionHistorial
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.history.ConflictoHistorial
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.domain.history.huellaHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.CambioEstadoHistorial
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico.*
import com.example.roadmap_utepsa.presentation.history.PasoHistorial
import com.example.roadmap_utepsa.presentation.history.ModoHistorial
import com.example.roadmap_utepsa.presentation.history.HistorialUiState
import com.example.roadmap_utepsa.presentation.history.HistorialViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Presentación simulada: no SDK, emulador Firestore, Firebase real ni Compose. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {
    @Test fun comparisonEvaluationCannotChangeNormalDraftEligibility() {
        val data = AcademicFixtures().data()
        val prerequisite = data.materias.first { subject -> data.prerrequisitos.any { it.idRequisito == subject.id } }
        val target = data.prerrequisitos.first { it.idRequisito == prerequisite.id }.idObjetivo
        val ready = HistorialUiState.Ready(LecturaPreparacion(data, null, null), BorradorHistorial(data.uid, emptyMap()),
            comparisonDraft = mapOf(prerequisite.id to APROBADA))
        assertFalse(ready.eligibility.getValue(target).habilitada)
        assertTrue(ready.comparisonEligibility.getValue(target).habilitada)
        assertTrue(ready.borrador.seleccion.isEmpty())
    }
    private val dispatcher = StandardTestDispatcher()
    private val fixture = AcademicFixtures()
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("alice")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) { uidChanges.value = "alice" }
        override fun signOut() { uidChanges.value = null }
    }
    private inner class Repository : PreparacionRepository {
        var lectura = LecturaPreparacion(fixture.data(), null, null)
        var failLoad = false
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var loads = 0
        var saves = 0
        var postpones = 0
        var resumes = 0
        var discards = 0
        var callback: ((Int, Int) -> Unit)? = null
        override suspend fun load(uid: String): LecturaPreparacion {
            loads++
            if (failLoad) error("Lectura fallida")
            return lectura
        }
        override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial,
                                       progreso: (Int, Int) -> Unit): LecturaPreparacion {
            saves++
            callback = progreso
            gate?.await()
            failure?.let { throw it }
            lectura = lectura.copy(academic = lectura.academic.copy(estados = borrador.seleccion),
                registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1:0"))
            return lectura
        }
        override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion {
            postpones++
            gate?.await()
            failure?.let { throw it }
            lectura = lectura.copy(registro = RegistroPreparacion(DecisionPreparacion.POSPUESTO, "1:0"))
            return lectura
        }
        override suspend fun reanudar(uid: String, idOperacion: String,
                                      progreso: (Int, Int) -> Unit): LecturaPreparacion {
            resumes++
            failure?.let { throw it }
            val op = requireNotNull(lectura.operacion)
            val estados = lectura.academic.estados + op.cambios.mapValues { it.value.nuevo }
            lectura = lectura.copy(academic = lectura.academic.copy(estados = estados),
                registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "2:0"),
                operacion = op.copy(aplicados = op.cambios.keys, completada = true))
            return lectura
        }
        override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion {
            discards++
            failure?.let { throw it }
            lectura = lectura.copy(operacion = lectura.operacion?.copy(cancelada = true))
            return lectura
        }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun ready(vm: HistorialViewModel) = vm.state.value as HistorialUiState.Ready

    @Test fun consultaConfirmadaYCancelarNoEscribenNiPosponen() = runTest(dispatcher) {
        val id = fixture.documents[0].id
        val repo = Repository().apply {
            lectura = lectura.copy(academic = lectura.academic.copy(estados = mapOf(id to APROBADA)),
                registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "confirmed"))
        }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertEquals(ModoHistorial.CONSULTA, ready(vm).modo)
        vm.posponer()
        assertEquals(0, repo.postpones)
        vm.editar()
        assertEquals(ModoHistorial.EDICION, ready(vm).modo)
        assertEquals(PasoHistorial.APROBADAS, ready(vm).paso)
        vm.seleccionarAprobada(id, false)
        assertTrue(ready(vm).borrador.cambios().isNotEmpty())
        vm.cancelarEdicion()
        assertEquals(ModoHistorial.CONSULTA, ready(vm).modo)
        assertEquals(APROBADA, ready(vm).borrador.seleccion[id])
        assertEquals(DecisionPreparacion.CONFIRMADO, ready(vm).lectura.registro!!.decision)
        assertEquals(0, repo.saves)
        assertEquals(0, repo.postpones)
        vm.iniciarRevision()
        advanceUntilIdle()
        assertEquals(ModoHistorial.CONSULTA, ready(vm).modo)
        assertEquals(0, repo.saves)
    }

    @Test fun historialPospuestoSeConfirmaSinReseleccionarMaterias() = runTest(dispatcher) {
        val id = fixture.documents[0].id
        val repo = Repository().apply {
            lectura = lectura.copy(academic = lectura.academic.copy(estados = mapOf(id to APROBADA)),
                registro = RegistroPreparacion(DecisionPreparacion.POSPUESTO, "postponed"))
        }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertEquals(ModoHistorial.CONSULTA, ready(vm).modo)
        vm.editar()
        vm.revisar()
        assertTrue(ready(vm).borrador.cambios().isEmpty())
        vm.confirmar()
        advanceUntilIdle()
        assertTrue(ready(vm).confirmado)
        assertEquals(mapOf(id to APROBADA), repo.lectura.academic.estados)
        assertEquals(1, repo.saves)
    }

    @Test fun wrappedTransactionConflictKeepsDraftAndShowsRemoteReview() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val local = fixture.documents[0].id
        vm.seleccionarAprobada(local, true)
        val remoto = repo.lectura.copy(academic = repo.lectura.academic.copy(
            estados = mapOf(fixture.documents[28].id to APROBADA)))
        repo.failure = IllegalStateException("SDK wrapper", ConflictoHistorial(remoto))
        vm.confirmar()
        advanceUntilIdle()
        assertEquals(remoto, ready(vm).conflicto)
        assertEquals(mapOf(local to APROBADA), ready(vm).borrador.seleccion)
        assertFalse(ready(vm).guardando)
        vm.confirmar()
        assertEquals(1, repo.saves)
    }

    @Test fun omitirEnCursoSoloRetiraNuevasSelecciones() = runTest(dispatcher) {
        val existente = fixture.documents[28].id
        val nueva = fixture.documents[29].id
        val repo = Repository().apply {
            lectura = lectura.copy(academic = lectura.academic.copy(estados = mapOf(existente to EN_CURSO)))
        }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.seleccionarEnCurso(nueva, true)
        assertEquals(EN_CURSO, ready(vm).borrador.seleccion[nueva])
        vm.omitirNuevasEnCurso()
        assertEquals(EN_CURSO, ready(vm).borrador.seleccion[existente])
        assertEquals(PENDIENTE, ready(vm).borrador.seleccion[nueva])
        assertEquals(PasoHistorial.REVISION, ready(vm).paso)
        assertEquals(0, repo.saves)
    }

    @Test fun lecturaFallidaNoEquivaleACeroYPermiteReintento() = runTest(dispatcher) {
        val repo = Repository().apply { failLoad = true }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertEquals(HistorialUiState.Error, vm.state.value)
        repo.failLoad = false
        vm.reload()
        advanceUntilIdle()
        assertTrue(ready(vm).borrador.seleccion.isEmpty())
        assertEquals(2, repo.loads)
    }

    @Test fun crearYAvanzarNoBorraElHistorialExistente() = runTest(dispatcher) {
        val id = fixture.documents[0].id
        val curso = fixture.documents[28].id
        val repo = Repository().apply {
            lectura = lectura.copy(academic = lectura.academic.copy(estados = mapOf(id to APROBADA, curso to EN_CURSO)))
        }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.abrirPaso(PasoHistorial.EN_CURSO)
        vm.revisar()
        assertEquals(mapOf(id to APROBADA, curso to EN_CURSO), ready(vm).borrador.seleccion)
        assertEquals(0, repo.saves)
        assertEquals(0, repo.postpones)
    }

    @Test fun desmarcarAprobadaRestauraCursoExistente() = runTest(dispatcher) {
        val id = fixture.documents[0].id
        val repo = Repository().apply { lectura = lectura.copy(academic = lectura.academic.copy(estados = mapOf(id to EN_CURSO))) }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.seleccionarAprobada(id, true)
        vm.seleccionarAprobada(id, false)
        assertEquals(EN_CURSO, ready(vm).borrador.seleccion[id])
    }

    @Test fun enCursoBloqueaRequisitosConDiagnosticos() = runTest(dispatcher) {
        val id = fixture.documents[49].id
        val vm = HistorialViewModel("alice", Repository(), Auth())
        advanceUntilIdle()
        vm.seleccionarEnCurso(id, true)
        assertNull(ready(vm).borrador.seleccion[id])
        assertEquals(setOf(fixture.documents[46].id, fixture.documents[47].id),
            ready(vm).diagnosticos.single().requisitosSinAprobar)
    }

    @Test fun aprobadasSinOrdenSeValidanAlConfirmar() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.seleccionarAprobada(fixture.documents[49].id, true)
        vm.confirmar()
        assertEquals(0, repo.saves)
        assertEquals(PasoHistorial.REVISION, ready(vm).paso)
        assertFalse(ready(vm).diagnosticos.isEmpty())
        fixture.documents.reversed().forEach { vm.seleccionarAprobada(it.id, true) }
        vm.confirmar()
        advanceUntilIdle()
        assertEquals(1, repo.saves)
        assertTrue(ready(vm).confirmado)
    }

    @Test fun duplicadosBloqueadosYEsperaNoDeclaraExito() = runTest(dispatcher) {
        val repo = Repository().apply { gate = CompletableDeferred() }
        val vm = HistorialViewModel("alice", repo, Auth())
        runCurrent()
        vm.confirmar()
        vm.confirmar()
        runCurrent()
        assertEquals(1, repo.saves)
        repo.callback!!(3, 8)
        assertEquals(3, ready(vm).progresoAplicado)
        advanceTimeBy(10_001)
        runCurrent()
        assertTrue(ready(vm).esperando)
        assertFalse(ready(vm).confirmado)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(ready(vm).confirmado)
    }

    @Test fun falloConservaBorradorYExigeRelecturaAntesDeReintentar() = runTest(dispatcher) {
        val repo = Repository().apply { failure = IllegalStateException("Fallo") }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val id = fixture.documents[0].id
        vm.seleccionarAprobada(id, true)
        vm.confirmar()
        advanceUntilIdle()
        assertEquals(APROBADA, ready(vm).borrador.seleccion[id])
        assertTrue(ready(vm).errorGuardado)
        vm.confirmar()
        advanceUntilIdle()
        assertEquals(1, repo.saves)
        vm.recuperarResultado()
        advanceUntilIdle()
        assertEquals(APROBADA, ready(vm).borrador.seleccion[id])
        assertFalse(ready(vm).recuperacionPendiente)
        repo.failure = null
        vm.confirmar()
        advanceUntilIdle()
        assertEquals(2, repo.saves)
    }

    @Test fun conflictoMantieneBaseYSeleccionHastaRevisionExplicita() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val local = fixture.documents[0].id
        val remoto = fixture.documents[28].id
        vm.seleccionarAprobada(local, true)
        repo.lectura = repo.lectura.copy(academic = repo.lectura.academic.copy(estados = mapOf(remoto to APROBADA)))
        repo.failure = ConflictoHistorial(repo.lectura)
        vm.confirmar()
        advanceUntilIdle()
        assertNotNull(ready(vm).conflicto)
        assertTrue(ready(vm).borrador.base.isEmpty())
        assertEquals(mapOf(local to APROBADA), ready(vm).borrador.seleccion)
        repo.failure = null
        vm.resolverConflictoConSeleccion(mapOf(local to APROBADA, remoto to APROBADA))
        advanceUntilIdle()
        assertNull(ready(vm).conflicto)
        assertEquals(mapOf(remoto to APROBADA), ready(vm).borrador.base)
        assertEquals(2, ready(vm).borrador.seleccion.size)
        assertEquals(1, repo.saves)
    }

    @Test fun recuperarResultadoNoOcultaCambiosConcurrentes() = runTest(dispatcher) {
        val repo = Repository().apply { failure = IllegalStateException("Fallo") }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val local = fixture.documents[0].id
        vm.seleccionarAprobada(local, true)
        vm.confirmar()
        advanceUntilIdle()
        repo.lectura = repo.lectura.copy(academic = repo.lectura.academic.copy(estados = mapOf(fixture.documents[28].id to APROBADA)))
        vm.recuperarResultado()
        advanceUntilIdle()
        assertNotNull(ready(vm).conflicto)
        assertEquals(mapOf(local to APROBADA), ready(vm).borrador.seleccion)
        assertEquals(0, repo.resumes)
    }

    @Test fun sesionCambiadaBorraVistaYBloqueaReuso() = runTest(dispatcher) {
        val auth = Auth()
        val repo = Repository().apply { gate = CompletableDeferred() }
        val vm = HistorialViewModel("alice", repo, auth)
        runCurrent()
        vm.confirmar()
        runCurrent()
        auth.uidChanges.value = "bob"
        runCurrent()
        assertEquals(HistorialUiState.SessionExpired, vm.state.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        auth.uidChanges.value = "alice"
        vm.reload()
        assertEquals(HistorialUiState.SessionExpired, vm.state.value)
    }

    @Test fun posponerNoModificaEstados() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.seleccionarAprobada(fixture.documents[0].id, true)
        vm.posponer()
        advanceUntilIdle()
        assertEquals(1, repo.postpones)
        assertEquals(0, repo.saves)
        assertTrue(repo.lectura.academic.estados.isEmpty())
        assertTrue(ready(vm).pospuesto)
    }

    @Test fun recargaNoDescartaUnaSeleccionExistente() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.seleccionarAprobada(fixture.documents[0].id, true)
        vm.reload()
        advanceUntilIdle()
        assertEquals(1, repo.loads)
        assertEquals(1, ready(vm).borrador.seleccion.size)
    }

    @Test fun journalDeLaSeleccionSeRecuperaSoloTrasAccionExplicita() = runTest(dispatcher) {
        val repo = Repository().apply { failure = IllegalStateException("Confirmación incierta") }
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val id = fixture.documents[0].id
        vm.seleccionarAprobada(id, true)
        vm.confirmar()
        advanceUntilIdle()
        val borrador = ready(vm).borrador
        val ids = fixture.documents.map { it.id }.toSet()
        repo.lectura = repo.lectura.copy(operacion = OperacionHistorial("operacion-simulada",
            huellaHistorial(borrador.base, ids), huellaHistorial(borrador.seleccion, ids), null, borrador.cambios()))
        repo.failure = null
        assertEquals(0, repo.resumes)
        vm.recuperarResultado()
        advanceUntilIdle()
        assertEquals(1, repo.resumes)
        assertTrue(ready(vm).confirmado)
        assertEquals(APROBADA, repo.lectura.academic.estados[id])
    }

    @Test fun descartarEnConflictoCierraJournalSinRevertirEstadosAplicados() = runTest(dispatcher) {
        val repo = Repository()
        val vm = HistorialViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val id = fixture.documents[0].id
        val remoto = fixture.documents[28].id
        vm.seleccionarAprobada(id, true)
        repo.lectura = repo.lectura.copy(academic = repo.lectura.academic.copy(estados = mapOf(remoto to APROBADA)),
            operacion = OperacionHistorial("operacion-simulada", "base", "objetivo", null,
                mapOf(remoto to CambioEstadoHistorial(PENDIENTE, APROBADA)), setOf(remoto)))
        repo.failure = ConflictoHistorial(repo.lectura)
        vm.confirmar()
        advanceUntilIdle()
        assertEquals(0, repo.discards)
        repo.failure = null
        vm.descartarBorradorYUsarRemoto()
        advanceUntilIdle()
        assertEquals(1, repo.discards)
        assertTrue(repo.lectura.operacion!!.cancelada)
        assertEquals(mapOf(remoto to APROBADA), ready(vm).borrador.seleccion)
        assertEquals(mapOf(remoto to APROBADA), repo.lectura.academic.estados)
        assertFalse(ready(vm).confirmado)
    }
}

package com.example.roadmap_utepsa.recommendation

import com.example.roadmap_utepsa.academic.AcademicFixtures

import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.OperacionHistorial
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.recommendation.DatosRecomendaciones
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.presentation.recommendation.EstadoRecomendaciones
import com.example.roadmap_utepsa.presentation.recommendation.RecomendacionesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecomendacionesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val data = AcademicFixtures().data()
    private var currentVersion = 1L
    private var failRead = false
    private var lectura = LecturaPreparacion(data, RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "1"), null)
    private var offer: Oferta? = Oferta(data.idPlan, setOf(data.materias.first().id), 1)
    private val auth = object : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("alice")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) {}
        override fun signOut() { uidChanges.value = null }
    }
    private fun vm(repo: RecomendacionesRepository = RecomendacionesRepository {
        if (failRead) error("offline")
        DatosRecomendaciones(lectura, offer, 3)
    }) = RecomendacionesViewModel("alice", 1, repo, auth) { currentVersion }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun loadingReadyErrorRetryAndLeavingRemoveOldResult() = runTest(dispatcher) {
        val vm = vm(); vm.reload(); assertEquals(EstadoRecomendaciones.CARGANDO, vm.state.value.estado)
        runCurrent(); assertEquals(EstadoRecomendaciones.LISTO, vm.state.value.estado)
        assertEquals(3L, vm.state.value.revisionAcademica)
        failRead = true; vm.reload(); assertNull(vm.state.value.resultado); runCurrent()
        assertEquals(EstadoRecomendaciones.ERROR, vm.state.value.estado)
        failRead = false; vm.reload(); runCurrent(); assertEquals(EstadoRecomendaciones.LISTO, vm.state.value.estado)
        vm.leave(); assertNull(vm.state.value.resultado)
    }
    @Test fun historyGatesHavePriorityAndDoNotCompute() = runTest(dispatcher) {
        val vm = vm()
        lectura = lectura.copy(registro = null); vm.reload(); runCurrent()
        assertEquals(EstadoRecomendaciones.HISTORIAL_PENDIENTE, vm.state.value.estado)
        lectura = lectura.copy(operacion = OperacionHistorial("op", "b", "t", null, emptyMap()))
        vm.reload(); runCurrent(); assertEquals(EstadoRecomendaciones.RECUPERACION_PENDIENTE, vm.state.value.estado)
        lectura = lectura.copy(operacion = null, registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "2"),
            academic = data.copy(estados = mapOf(data.materias[1].id to EstadoAcademico.APROBADA)))
        vm.reload(); runCurrent(); assertEquals(EstadoRecomendaciones.HISTORIAL_INCOHERENTE, vm.state.value.estado)
        assertNull(vm.state.value.resultado)
    }
    @Test fun absentEmptyAndNoCandidatesAreSeparate() = runTest(dispatcher) {
        val vm = vm(); offer = null; vm.reload(); runCurrent()
        assertEquals(EstadoRecomendaciones.OFERTA_AUSENTE, vm.state.value.estado)
        offer = Oferta(data.idPlan, emptySet(), 1); vm.reload(); runCurrent()
        assertEquals(EstadoRecomendaciones.OFERTA_VACIA, vm.state.value.estado)
        offer = Oferta(data.idPlan, setOf(data.materias[1].id), 2); vm.reload(); runCurrent()
        assertEquals(EstadoRecomendaciones.SIN_CANDIDATAS, vm.state.value.estado)
    }
    @Test fun sameUidNewSessionExpiresAndAccountChangeClearsData() = runTest(dispatcher) {
        val vm = vm(); vm.reload(); runCurrent(); auth.uidChanges.value = "bob"; runCurrent()
        assertEquals(EstadoRecomendaciones.SESION_CADUCADA, vm.state.value.estado); assertNull(vm.state.value.resultado)
        auth.uidChanges.value = "alice"; currentVersion = 2; vm.reload(); runCurrent()
        assertEquals(EstadoRecomendaciones.SESION_CADUCADA, vm.state.value.estado)
    }
    @Test fun sameAccountNewVersionWhileLoadingDiscardsResult() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = vm(RecomendacionesRepository { gate.await(); DatosRecomendaciones(lectura, offer, 1) })
        vm.reload(); runCurrent(); currentVersion = 2; gate.complete(Unit); runCurrent()
        assertEquals(EstadoRecomendaciones.SESION_CADUCADA, vm.state.value.estado)
        assertNull(vm.state.value.resultado)
    }
    @Test fun supersededNonCancellableResponseCannotReplaceLatest() = runTest(dispatcher) {
        var count = 0
        val gate = CompletableDeferred<Unit>()
        val vm = vm(RecomendacionesRepository {
            val index = ++count
            if (index == 1) withContext(NonCancellable) { gate.await() }
            DatosRecomendaciones(lectura, if (index == 1) null else offer, index.toLong())
        })
        vm.reload(); runCurrent(); vm.reload(); runCurrent()
        assertEquals(EstadoRecomendaciones.LISTO, vm.state.value.estado)
        gate.complete(Unit); runCurrent()
        assertEquals(2L, vm.state.value.revisionAcademica)
        assertEquals(EstadoRecomendaciones.LISTO, vm.state.value.estado)
    }
}

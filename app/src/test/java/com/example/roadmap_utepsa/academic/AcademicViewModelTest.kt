package com.example.roadmap_utepsa.academic

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.academic.ConflictoEstadoAcademico
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.presentation.academic.AcademicUiState
import com.example.roadmap_utepsa.presentation.academic.AcademicViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AcademicViewModelTest {
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
    private inner class Repository : AcademicRepository {
        var calls = 0
        var fail = false
        var data = fixture.data()
        var gate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        var writes = 0
        var writeGate: CompletableDeferred<Unit>? = null
        var writeFailure = false
        var expected: Map<String, EstadoAcademico>? = null
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> =
            error("El ViewModel debe usar CAS")
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico,
                                       expectedStates: Map<String, EstadoAcademico>): Map<String, EstadoAcademico> {
            writes++
            expected = expectedStates.toMap()
            writeGate?.await()
            if (writeFailure) error("Guardado denegado")
            if (data.estados != expectedStates.filterValues { it != EstadoAcademico.PENDIENTE })
                throw ConflictoEstadoAcademico(data.estados)
            val states = data.estados.toMutableMap()
            if (estado == EstadoAcademico.PENDIENTE) states.remove(idMateria) else states[idMateria] = estado
            data = data.copy(estados = states)
            return states
        }
        override suspend fun load(uid: String): AcademicData {
            calls++
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            if (fail) error("No se pudo leer")
            return data
        }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun returnFromHistoryReplacesAnOlderReadStillInFlight() = runTest(dispatcher) {
        val oldRead = CompletableDeferred<AcademicData>()
        val fresh = fixture.data().let { it.copy(estados = mapOf(it.materias.first().id to EstadoAcademico.EN_CURSO)) }
        var calls = 0
        val repo = object : AcademicRepository {
            override suspend fun load(uid: String): AcademicData {
                calls++
                return if (calls == 1) withContext(NonCancellable) { oldRead.await() } else fresh
            }
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> = error("No escribe")
        }
        val vm = AcademicViewModel("alice", repo, Auth())
        runCurrent()
        vm.refreshFromHistory()
        runCurrent()
        assertEquals(fresh.estados, (vm.state.value as AcademicUiState.Ready).data.estados)
        oldRead.complete(fixture.data())
        advanceUntilIdle()
        assertEquals(2, calls)
        assertEquals(fresh.estados, (vm.state.value as AcademicUiState.Ready).data.estados)
    }

    @Test fun missingRequirementsBlockInCourseWithoutRepositoryWrite() = runTest(dispatcher) {
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.openSubject(fixture.documents[49].id)
        vm.selectState(EstadoAcademico.EN_CURSO)
        vm.saveState()
        advanceUntilIdle()
        val ready = vm.state.value as AcademicUiState.Ready
        assertEquals(0, repo.writes)
        assertFalse(ready.editor!!.transition!!.permitida)
        assertEquals(2, ready.editor.transition.bloqueos.single().requisitosSinAprobar.size)
    }

    @Test fun withdrawingUsedApprovalIsBlockedWithoutCascade() = runTest(dispatcher) {
        val requirement = fixture.documents[47].id
        val dependent = fixture.documents[49].id
        val repo = Repository().apply { data = data.copy(estados = mapOf(requirement to EstadoAcademico.APROBADA,
            dependent to EstadoAcademico.APROBADA)) }
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        vm.openSubject(requirement)
        vm.selectState(EstadoAcademico.PENDIENTE)
        vm.saveState()
        advanceUntilIdle()
        val ready = vm.state.value as AcademicUiState.Ready
        assertEquals(0, repo.writes)
        assertEquals(dependent, ready.editor!!.transition!!.dependientesAfectados.single().idMateria)
        assertEquals(EstadoAcademico.APROBADA, ready.data.estados[requirement])
    }

    @Test fun declaredApprovalKeepsContradictionsVisibleAfterSave() = runTest(dispatcher) {
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val id = fixture.documents[49].id
        vm.openSubject(id)
        vm.selectState(EstadoAcademico.APROBADA)
        assertTrue((vm.state.value as AcademicUiState.Ready).editor!!.transition!!.permitida)
        vm.saveState()
        advanceUntilIdle()
        val ready = vm.state.value as AcademicUiState.Ready
        assertEquals(1, repo.writes)
        assertEquals(EstadoAcademico.APROBADA, ready.data.estados[id])
        assertFalse(ready.coherencia.coherente)
    }

    @Test fun loadingDoesNotExposeEmptyOrPartialDataAndPreventsDuplicateLoads() = runTest(dispatcher) {
        val auth = Auth()
        val repo = Repository().apply { gate = CompletableDeferred() }
        val vm = AcademicViewModel("alice", repo, auth)
        runCurrent()
        assertEquals(AcademicUiState.Loading, vm.state.value)
        vm.reload()
        runCurrent()
        assertEquals(1, repo.calls)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        val ready = vm.state.value as AcademicUiState.Ready
        assertEquals(50, ready.data.materias.size)
        assertTrue(ready.data.estados.isEmpty())
        assertFalse(ready.habilitaciones.getValue(fixture.documents[49].id).habilitada)
        assertEquals(40, ready.habilitaciones.getValue(fixture.documents[48].id).aprobadasFaltantes)
    }

    @Test fun errorDoesNotBecomeZeroProgressAndRetryUsesSameAccount() = runTest(dispatcher) {
        val repo = Repository().apply { fail = true }
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertEquals(AcademicUiState.Error, vm.state.value)
        repo.fail = false
        vm.reload()
        assertEquals(AcademicUiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertEquals("alice", (vm.state.value as AcademicUiState.Ready).data.uid)
        assertEquals(2, repo.calls)
    }

    @Test fun failedRefreshDoesNotKeepOldDataAsIfCurrent() = runTest(dispatcher) {
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertTrue(vm.state.value is AcademicUiState.Ready)
        repo.fail = true
        vm.reload()
        assertEquals(AcademicUiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertEquals(AcademicUiState.Error, vm.state.value)
    }

    @Test fun signOutImmediatelyDropsLoadedDataAndOldInstanceCannotReload() = runTest(dispatcher) {
        val auth = Auth()
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, auth)
        advanceUntilIdle()
        auth.signOut()
        runCurrent()
        assertEquals(AcademicUiState.SessionExpired, vm.state.value)
        auth.uidChanges.value = "alice"
        runCurrent()
        vm.reload()
        runCurrent()
        assertEquals(AcademicUiState.SessionExpired, vm.state.value)
        assertEquals(1, repo.calls)
    }

    @Test fun accountChangeDiscardsEvenLateNonCancellableResponse() = runTest(dispatcher) {
        val auth = Auth()
        val repo = Repository().apply { gate = CompletableDeferred(); ignoreCancellation = true }
        val vm = AcademicViewModel("alice", repo, auth)
        runCurrent()
        auth.uidChanges.value = "bob"
        runCurrent()
        assertEquals(AcademicUiState.SessionExpired, vm.state.value)
        repo.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(AcademicUiState.SessionExpired, vm.state.value)
    }

    @Test fun noAuthenticatedAccountDoesNotRead() = runTest(dispatcher) {
        val auth = Auth().apply { signOut() }
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, auth)
        advanceUntilIdle()
        assertEquals(AcademicUiState.SessionExpired, vm.state.value)
        assertEquals(0, repo.calls)
    }

    @Test fun repositoryCannotPublishAnotherAccountsData() = runTest(dispatcher) {
        val repo = Repository().apply { data = fixture.data("bob") }
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertEquals(AcademicUiState.Error, vm.state.value)
    }

    @Test fun invalidGraphFromRepositoryDoesNotPublishReady() = runTest(dispatcher) {
        val repo = Repository().apply { data = data.copy(prerrequisitos = data.prerrequisitos + data.prerrequisitos.first()) }
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        assertEquals(AcademicUiState.Error, vm.state.value)
    }

    @Test fun rotationReusesInstanceAndNewSessionGetsFreshLoad() = runTest(dispatcher) {
        val store = ViewModelStore()
        val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
        val auth = Auth()
        val repo = Repository()
        fun instance(version: Long) = ViewModelProvider(owner, AcademicViewModel.Factory("alice", repo, auth))
            .get(AcademicViewModel.key("alice", version), AcademicViewModel::class.java)
        val first = instance(1)
        advanceUntilIdle()
        assertSame(first, instance(1))
        assertEquals(1, repo.calls)
        val next = instance(2)
        advanceUntilIdle()
        assertNotSame(first, next)
        assertEquals(2, repo.calls)
        store.clear()
    }

    @Test fun pendingSaveKeepsOldStateAndBlocksDuplicateOrCancel() = runTest(dispatcher) {
        val repo = Repository().apply { writeGate = CompletableDeferred() }
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val id = fixture.documents[0].id
        vm.openSubject(id); vm.selectState(EstadoAcademico.APROBADA); vm.saveState()
        runCurrent()
        vm.saveState(); vm.reload(); vm.closeSubject(); vm.selectState(EstadoAcademico.EN_CURSO)
        advanceTimeBy(10_001); runCurrent()
        val pending = vm.state.value as AcademicUiState.Ready
        assertTrue(pending.editor!!.saving)
        assertTrue(pending.editor.awaitingConfirmation)
        assertEquals(EstadoAcademico.APROBADA, pending.editor.selected)
        assertTrue(pending.data.estados.isEmpty())
        assertEquals(1, repo.writes)
        repo.writeGate!!.complete(Unit); advanceUntilIdle()
        val saved = vm.state.value as AcademicUiState.Ready
        assertNull(saved.editor)
        assertEquals(EstadoAcademico.APROBADA, saved.data.estados[id])
        assertTrue(saved.habilitaciones.getValue(fixture.documents[1].id).habilitada)
        assertTrue(saved.saved)
    }

    @Test fun failedSavePreservesDraftAndPreviousStateThenRetries() = runTest(dispatcher) {
        val repo = Repository().apply { writeFailure = true }
        val vm = AcademicViewModel("alice", repo, Auth())
        advanceUntilIdle()
        val id = fixture.documents[0].id
        vm.openSubject(id); vm.selectState(EstadoAcademico.APROBADA); vm.saveState()
        advanceUntilIdle()
        val failed = vm.state.value as AcademicUiState.Ready
        assertTrue(failed.editor!!.failed)
        assertEquals(EstadoAcademico.APROBADA, failed.editor.selected)
        assertTrue(failed.data.estados.isEmpty())
        repo.writeFailure = false; vm.saveState(); advanceUntilIdle()
        assertEquals(EstadoAcademico.APROBADA, (vm.state.value as AcademicUiState.Ready).data.estados[id])
        assertEquals(2, repo.writes)
    }

    @Test fun returnToPendingRemovesStateAndRecalculates() = runTest(dispatcher) {
        val id = fixture.documents[0].id
        val repo = Repository().apply { data = data.copy(estados = mapOf(id to EstadoAcademico.APROBADA)) }
        val vm = AcademicViewModel("alice", repo, Auth()); advanceUntilIdle()
        vm.openSubject(id); vm.selectState(EstadoAcademico.PENDIENTE); vm.saveState(); advanceUntilIdle()
        val result = vm.state.value as AcademicUiState.Ready
        assertFalse(id in result.data.estados)
        assertFalse(result.habilitaciones.getValue(fixture.documents[1].id).habilitada)
    }

    @Test fun unchangedSelectionAndDiscardDoNotWrite() = runTest(dispatcher) {
        val repo = Repository(); val vm = AcademicViewModel("alice", repo, Auth()); advanceUntilIdle()
        vm.openSubject(fixture.documents[0].id); vm.saveState()
        vm.openSubject(fixture.documents[0].id); vm.selectState(EstadoAcademico.APROBADA); vm.closeSubject()
        assertEquals(0, repo.writes)
        assertNull((vm.state.value as AcademicUiState.Ready).editor)
    }

    @Test fun sessionChangeDuringSaveNeverPublishesOldAccount() = runTest(dispatcher) {
        val repo = Repository().apply { writeGate = CompletableDeferred() }
        val auth = Auth(); val vm = AcademicViewModel("alice", repo, auth); advanceUntilIdle()
        vm.openSubject(fixture.documents[0].id); vm.selectState(EstadoAcademico.APROBADA); vm.saveState(); runCurrent()
        auth.signOut(); runCurrent(); repo.writeGate!!.complete(Unit); advanceUntilIdle()
        assertEquals(AcademicUiState.SessionExpired, vm.state.value)
    }

    @Test fun conflictKeepsDraftAndRequiresReviewBeforeRetry() = runTest(dispatcher) {
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, Auth()); advanceUntilIdle()
        val id = fixture.documents[28].id
        val other = fixture.documents[29].id
        vm.openSubject(id); vm.selectState(EstadoAcademico.EN_CURSO)
        repo.data = repo.data.copy(estados = mapOf(other to EstadoAcademico.APROBADA))
        vm.saveState(); advanceUntilIdle()
        val conflict = vm.state.value as AcademicUiState.Ready
        assertTrue(conflict.editor!!.conflict)
        assertFalse(conflict.editor.failed)
        assertEquals(EstadoAcademico.EN_CURSO, conflict.editor.selected)
        assertTrue(conflict.data.estados.isEmpty())
        assertEquals(emptyMap<String, EstadoAcademico>(), repo.expected)
        vm.saveState(); advanceUntilIdle()
        assertEquals(1, repo.writes)
        vm.reviewConflict()
        val reviewed = vm.state.value as AcademicUiState.Ready
        assertEquals(repo.data.estados, reviewed.data.estados)
        assertEquals(EstadoAcademico.EN_CURSO, reviewed.editor!!.selected)
        assertFalse(reviewed.editor.conflict)
        assertFalse(reviewed.saved)
        assertEquals(1, repo.writes)
        vm.saveState(); advanceUntilIdle()
        val saved = vm.state.value as AcademicUiState.Ready
        assertTrue(saved.saved)
        assertEquals(mapOf(other to EstadoAcademico.APROBADA, id to EstadoAcademico.EN_CURSO), saved.data.estados)
    }

    @Test fun reviewReevaluatesRequirementsAndPreventsNowInvalidSelection() = runTest(dispatcher) {
        val dependent = fixture.documents[49].id
        val quality = fixture.documents[46].id
        val data = fixture.documents[47].id
        val repo = Repository().apply { this.data = this.data.copy(estados = mapOf(
            quality to EstadoAcademico.APROBADA, data to EstadoAcademico.APROBADA)) }
        val vm = AcademicViewModel("alice", repo, Auth()); advanceUntilIdle()
        vm.openSubject(dependent); vm.selectState(EstadoAcademico.EN_CURSO)
        repo.data = repo.data.copy(estados = mapOf(data to EstadoAcademico.APROBADA))
        vm.saveState(); advanceUntilIdle(); vm.reviewConflict()
        val reviewed = vm.state.value as AcademicUiState.Ready
        assertEquals(EstadoAcademico.EN_CURSO, reviewed.editor!!.selected)
        assertFalse(reviewed.editor.transition!!.permitida)
        vm.saveState(); advanceUntilIdle()
        assertEquals(1, repo.writes)
        assertFalse((vm.state.value as AcademicUiState.Ready).saved)
    }

    @Test fun anotherChangeAfterConflictReviewRequiresAnotherReview() = runTest(dispatcher) {
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, Auth()); advanceUntilIdle()
        val id = fixture.documents[28].id
        vm.openSubject(id); vm.selectState(EstadoAcademico.EN_CURSO)
        repo.data = repo.data.copy(estados = mapOf(id to EstadoAcademico.APROBADA))
        vm.saveState(); advanceUntilIdle(); vm.reviewConflict()
        repo.data = repo.data.copy(estados = emptyMap())
        vm.saveState(); advanceUntilIdle()
        val conflict = vm.state.value as AcademicUiState.Ready
        assertTrue(conflict.editor!!.conflict)
        assertEquals(EstadoAcademico.EN_CURSO, conflict.editor.selected)
        assertTrue(repo.data.estados.isEmpty())
        assertFalse(conflict.saved)
    }

    @Test fun sameStateAfterReviewClosesWithoutOverwriting() = runTest(dispatcher) {
        val repo = Repository()
        val vm = AcademicViewModel("alice", repo, Auth()); advanceUntilIdle()
        val id = fixture.documents[28].id
        vm.openSubject(id); vm.selectState(EstadoAcademico.EN_CURSO)
        repo.data = repo.data.copy(estados = mapOf(id to EstadoAcademico.EN_CURSO))
        vm.saveState(); advanceUntilIdle(); vm.reviewConflict(); vm.saveState(); advanceUntilIdle()
        assertNull((vm.state.value as AcademicUiState.Ready).editor)
        assertEquals(1, repo.writes)
    }
}

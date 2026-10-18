package com.example.roadmap_utepsa

import androidx.lifecycle.*
import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.model.*
import com.example.roadmap_utepsa.presentation.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class OfertaViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val fixture = AcademicFixtures().data()
    private val first get() = fixture.materias.first().id
    private val last get() = fixture.materias.last().id
    private class Auth : AuthRepository {
        override val uidChanges = MutableStateFlow<String?>("alice")
        override val currentUid get() = uidChanges.value
        override val isAuthenticated get() = currentUid != null
        override val authenticationChanges = uidChanges.map { it != null }
        override suspend fun signInWithGoogle(idToken: String) { uidChanges.value = "alice" }
        override fun signOut() { uidChanges.value = null }
    }
    private inner class Repo : OfertaRepository {
        var catalog = fixture.materias
        var remote: Oferta? = null
        var writes = 0
        var reads = 0
        var readFail = false
        var fail: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        override suspend fun load(uid: String): DatosOferta {
            reads++
            if (readFail) error("offline")
            return DatosOferta(uid, fixture.idPlan, catalog, remote)
        }
        override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta {
            writes++
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            fail?.let { throw it }
            if (remote?.revision != expectedRevision) throw ConflictoOferta(remote)
            return Oferta(fixture.idPlan, materias.toSet(), (expectedRevision ?: 0) + 1).also { remote = it }
        }
    }
    private val auth = Auth()
    private var version = 1L
    private fun vm(repo: Repo) = OfertaViewModel("alice", version, repo, auth) { version }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun absentAndRegisteredEmptyRemainDistinct() = runTest(dispatcher) {
        val repo = Repo(); val vm = vm(repo); runCurrent()
        assertNull(vm.state.value.data?.oferta)
        vm.edit(); vm.save(); runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals(emptySet<String>(), vm.state.value.data?.oferta?.materias)
        assertEquals(1L, vm.state.value.data?.oferta?.revision)
    }
    @Test fun readErrorNeverMeansAbsence() = runTest(dispatcher) {
        val repo = Repo().apply { readFail = true }; val vm = vm(repo); runCurrent()
        assertTrue(vm.state.value.error); assertNull(vm.state.value.data)
        vm.edit(); vm.save(); runCurrent(); assertEquals(0, repo.writes)
        repo.readFail = false; vm.reload(); runCurrent(); assertNotNull(vm.state.value.data)
    }
    @Test fun saveReplacesAllAndAllowsAnyCatalogSubject() = runTest(dispatcher) {
        val repo = Repo().apply { remote = Oferta(fixture.idPlan, setOf(first), 1) }
        val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(first); vm.toggle(last); vm.save(); runCurrent()
        assertEquals(setOf(last), repo.remote?.materias); assertEquals(2L, repo.remote?.revision)
        vm.edit(); vm.toggle(last); vm.save(); runCurrent(); assertEquals(emptySet<String>(), repo.remote?.materias)
    }
    @Test fun selectionRejectsUnknownAndToggleRemoves() = runTest(dispatcher) {
        val vm = vm(Repo()); runCurrent(); vm.edit(); vm.toggle("foreign"); assertTrue(vm.state.value.draft.isEmpty())
        vm.toggle(first); vm.toggle(first); assertTrue(vm.state.value.draft.isEmpty())
    }
    @Test fun searchIgnoresCaseAccentsAndSupportsCode() = runTest(dispatcher) {
        val repo = Repo()
        val subject = fixture.materias.first().copy(nombre = "Organización y Matemáticas")
        repo.catalog = listOf(subject) + fixture.materias.drop(1)
        val vm = vm(repo); runCurrent(); vm.edit()
        vm.query(java.text.Normalizer.normalize(subject.nombre, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").uppercase())
        assertTrue(vm.state.value.visible.any { it.id == subject.id })
        vm.query(subject.codigo.lowercase()); assertEquals(listOf(subject), vm.state.value.visible)
    }
    @Test fun cancelDoesNotWriteAndEditRestoresSavedSelection() = runTest(dispatcher) {
        val repo = Repo(); val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(last)
        assertTrue(vm.state.value.dirty); vm.cancel(); assertFalse(vm.state.value.editing)
        vm.edit(); assertTrue(vm.state.value.draft.isEmpty()); assertEquals(0, repo.writes)
    }
    @Test fun failedSavePreservesSelection() = runTest(dispatcher) {
        val repo = Repo().apply { fail = IllegalStateException("offline") }; val vm = vm(repo)
        runCurrent(); vm.edit(); vm.toggle(first); vm.save(); runCurrent()
        assertEquals(setOf(first), vm.state.value.draft); assertTrue(vm.state.value.error)
        repo.fail = null; vm.save(); runCurrent(); assertTrue(vm.state.value.saved)
    }
    @Test fun savingRejectsDuplicatesAndMutation() = runTest(dispatcher) {
        val repo = Repo().apply { gate = CompletableDeferred() }; val vm = vm(repo)
        runCurrent(); vm.edit(); vm.toggle(first); vm.save(); vm.save(); runCurrent()
        vm.toggle(last); vm.cancel(); assertEquals(setOf(first), vm.state.value.draft)
        assertTrue(vm.state.value.saving); assertEquals(1, repo.writes)
        repo.gate?.complete(Unit); runCurrent(); assertTrue(vm.state.value.saved)
    }
    @Test fun conflictRequiresExplicitDecisionAndKeepsDraft() = runTest(dispatcher) {
        val repo = Repo(); val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(first)
        repo.remote = Oferta(fixture.idPlan, setOf(last), 1)
        vm.save(); runCurrent(); assertTrue(vm.state.value.conflict)
        assertEquals(setOf(first), vm.state.value.draft); assertEquals(repo.remote, vm.state.value.remote)
        vm.save(); runCurrent(); assertEquals(1, repo.writes)
        vm.resolveConflict(true); runCurrent()
        assertEquals(setOf(first), repo.remote?.materias); assertEquals(2L, repo.remote?.revision)
    }
    @Test fun conflictCanLoadRemoteWithoutWrite() = runTest(dispatcher) {
        val repo = Repo(); val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(first)
        repo.remote = Oferta(fixture.idPlan, emptySet(), 1); vm.save(); runCurrent()
        vm.resolveConflict(false); assertTrue(vm.state.value.draft.isEmpty()); assertEquals(1, repo.writes)
        assertEquals(1L, vm.state.value.data?.oferta?.revision)
    }
    @Test fun uncertainSuccessReconcilesBeforeRetryWithoutSecondWrite() = runTest(dispatcher) {
        val repo = Repo().apply { fail = ResultadoOfertaIncierto(IllegalStateException()) }
        val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(first); vm.save(); runCurrent()
        assertTrue(vm.state.value.uncertain)
        repo.remote = Oferta(fixture.idPlan, setOf(first), 1)
        vm.save(); runCurrent(); assertTrue(vm.state.value.saved); assertEquals(1, repo.writes)
        assertEquals(2, repo.reads)
    }
    @Test fun uncertainRetryCannotWriteWhileServerReadFails() = runTest(dispatcher) {
        val repo = Repo().apply { fail = ResultadoOfertaIncierto(IllegalStateException()) }
        val vm = vm(repo); runCurrent(); vm.edit(); vm.save(); runCurrent()
        repo.readFail = true; vm.save(); runCurrent(); assertEquals(1, repo.writes); assertTrue(vm.state.value.uncertain)
    }
    @Test fun changedSessionDropsDraftAndLateResponse() = runTest(dispatcher) {
        val repo = Repo().apply { gate = CompletableDeferred(); ignoreCancellation = true }
        val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(first); vm.save(); runCurrent()
        auth.signOut(); runCurrent(); assertTrue(vm.state.value.expired); assertTrue(vm.state.value.draft.isEmpty())
        auth.uidChanges.value = "alice"; version++; repo.gate?.complete(Unit); runCurrent()
        assertTrue(vm.state.value.expired); assertNull(vm.state.value.data)
    }
    @Test fun sameUidNewSessionVersionRejectsAsyncResponse() = runTest(dispatcher) {
        val repo = Repo().apply { gate = CompletableDeferred() }; val vm = vm(repo)
        runCurrent(); vm.edit(); vm.save(); runCurrent(); version++; repo.gate?.complete(Unit); runCurrent()
        assertTrue(vm.state.value.expired)
    }
    @Test fun viewModelStoreRetainsDraftAcrossRecreationAndNavigation() = runTest(dispatcher) {
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        val repo = Repo(); val factory = OfertaViewModel.Factory("alice", version, repo, auth) { version }
        val key = OfertaViewModel.key("alice", version)
        val a = ViewModelProvider(owner, factory).get(key, OfertaViewModel::class.java)
        runCurrent(); a.edit(); a.toggle(first)
        val b = ViewModelProvider(owner, factory).get(key, OfertaViewModel::class.java)
        assertSame(a, b); assertEquals(setOf(first), b.state.value.draft)
        assertNotEquals(key, OfertaViewModel.key("alice", version + 1)); owner.viewModelStore.clear()
    }
    @Test fun uncertainSelectionCannotChangeBeforeReconciliation() = runTest(dispatcher) {
        val repo = Repo().apply { fail = ResultadoOfertaIncierto(IllegalStateException()) }
        val vm = vm(repo); runCurrent(); vm.edit(); vm.toggle(first); vm.save(); runCurrent()
        vm.toggle(last); assertEquals(setOf(first), vm.state.value.draft)
    }
    @Test fun invalidOfferRevisionAndOversizeRejected() {
        for (revision in listOf(0L, -1L, Long.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { Oferta(fixture.idPlan, emptySet(), revision) }
        }
        assertThrows(IllegalArgumentException::class.java) { Oferta(fixture.idPlan, (1..51).map { "$it" }.toSet(), 1) }
    }
}

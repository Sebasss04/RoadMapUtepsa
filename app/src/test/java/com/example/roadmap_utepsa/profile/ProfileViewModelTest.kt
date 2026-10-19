package com.example.roadmap_utepsa.profile

import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.domain.profile.validProfileName
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import com.example.roadmap_utepsa.presentation.profile.ProfileLoad
import com.example.roadmap_utepsa.presentation.profile.ProfileViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val initial = ProfileData("", "", AssignedStudyPlan("test-plan", "Carrera de prueba", "Prueba"), ProfileRegistration.ABSENT)

    private class Repository(var data: ProfileData) : ProfileRepository {
        var loadFailure = false
        var saveFailure = false
        var saves = 0
        var lastUid: String? = null
        var confirmation: CompletableDeferred<Unit>? = null
        override suspend fun load(uid: String): ProfileData {
            lastUid = uid
            if (loadFailure) error("Lectura denegada")
            return data
        }
        override suspend fun save(uid: String, names: String, surnames: String): ProfileData {
            lastUid = uid
            saves++
            confirmation?.await()
            if (saveFailure) error("Guardado denegado")
            data = data.copy(names = names, surnames = surnames, registration = ProfileRegistration.COMPLETE)
            return data
        }
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun readFailureDoesNotBecomeAnAbsentProfileAndCanRetry() = runTest(dispatcher) {
        val repository = Repository(initial).apply { loadFailure = true }
        val vm = ProfileViewModel("same-uid", repository)
        runCurrent()
        assertEquals(ProfileLoad.ERROR, vm.state.value.load)
        assertNull(vm.state.value.profile)
        vm.save()
        assertEquals(0, repository.saves)
        repository.loadFailure = false
        vm.retryLoad()
        runCurrent()
        assertEquals(ProfileRegistration.ABSENT, vm.state.value.profile?.registration)
    }

    @Test fun invalidInputsNeverReachTheRepository() = runTest(dispatcher) {
        val repository = Repository(initial)
        val vm = ProfileViewModel("same-uid", repository)
        runCurrent()
        vm.changeNames("   ")
        vm.changeSurnames("Pérez")
        vm.save()
        runCurrent()
        assertTrue(vm.state.value.invalidNames)
        assertEquals(0, repository.saves)
        assertFalse(validProfileName("a".repeat(81)))
        assertFalse(validProfileName("Nombre\nApellido"))
        assertFalse(validProfileName("Nombre\tApellido"))
        assertTrue(validProfileName("María José"))
        assertTrue(validProfileName("O’Connor-Díaz"))
    }

    @Test fun failedSavePreservesInputsAndRetriesWithTheSameUid() = runTest(dispatcher) {
        val repository = Repository(initial).apply { saveFailure = true }
        val vm = ProfileViewModel("same-uid", repository)
        runCurrent()
        vm.changeNames("María José")
        vm.changeSurnames("Pérez Díaz")
        vm.save()
        runCurrent()
        assertTrue(vm.state.value.saveFailed)
        assertFalse(vm.state.value.saved)
        assertEquals("María José", vm.state.value.names)
        repository.saveFailure = false
        vm.save()
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals("same-uid", repository.lastUid)
        assertEquals(2, repository.saves)
    }

    @Test fun pendingSaveDoesNotReportSuccessOrAllowDuplicateSubmissions() = runTest(dispatcher) {
        val confirmation = CompletableDeferred<Unit>()
        val repository = Repository(initial).apply { this.confirmation = confirmation }
        val vm = ProfileViewModel("same-uid", repository)
        runCurrent()
        vm.changeNames("María")
        vm.changeSurnames("Pérez")
        vm.save()
        vm.save()
        runCurrent()
        advanceTimeBy(10_001)
        runCurrent()
        assertEquals(1, repository.saves)
        assertTrue(vm.state.value.saving)
        assertTrue(vm.state.value.awaitingConfirmation)
        assertFalse(vm.state.value.saved)
        vm.changeNames("Otro nombre")
        assertEquals("María", vm.state.value.names)
        confirmation.complete(Unit)
        runCurrent()
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.saving)
        assertFalse(vm.state.value.awaitingConfirmation)
    }

    @Test fun confirmedValuesCanBeReadAgainCorrectedAndDiscarded() = runTest(dispatcher) {
        val repository = Repository(initial)
        val vm = ProfileViewModel("same-uid", repository)
        runCurrent()
        vm.changeNames("  María José  ")
        vm.changeSurnames("Pérez")
        vm.save()
        runCurrent()
        val reopened = ProfileViewModel("same-uid", repository)
        runCurrent()
        assertEquals("María José", reopened.state.value.names)
        assertEquals(ProfileRegistration.COMPLETE, reopened.state.value.profile?.registration)
        reopened.changeSurnames("Pérez Díaz")
        reopened.save()
        runCurrent()
        assertEquals("Pérez Díaz", repository.data.surnames)
        reopened.changeNames("Borrador")
        reopened.discardChanges()
        assertEquals("María José", reopened.state.value.names)
    }

    @Test fun incompleteProfileRemainsDistinctFromAnAbsentOne() = runTest(dispatcher) {
        val repository = Repository(initial.copy(names = "María", registration = ProfileRegistration.INCOMPLETE))
        val vm = ProfileViewModel("same-uid", repository)
        runCurrent()
        assertEquals(ProfileRegistration.INCOMPLETE, vm.state.value.profile?.registration)
        assertEquals("María", vm.state.value.names)
        assertFalse(vm.state.value.saved)
    }

    @Test fun changingOrReopeningAnAuthSessionSelectsAFreshProfileInstance() = runTest(dispatcher) {
        val repository = object : AuthRepository {
            override val uidChanges = MutableStateFlow<String?>("alice")
            override val currentUid get() = uidChanges.value
            override val isAuthenticated get() = currentUid != null
            override val authenticationChanges = uidChanges.map { it != null }
            override suspend fun signInWithGoogle(idToken: String) { uidChanges.value = "bob" }
            override fun signOut() { uidChanges.value = null }
        }
        val auth = AuthViewModel(repository)
        runCurrent()
        assertEquals("alice", auth.state.value.uid)
        val firstSession = auth.state.value.sessionVersion
        repository.uidChanges.value = "bob"
        runCurrent()
        assertEquals("bob", auth.state.value.uid)
        assertTrue(auth.state.value.sessionVersion > firstSession)
        auth.signOut()
        val signedOutSession = auth.state.value.sessionVersion
        runCurrent()
        assertFalse(auth.state.value.isAuthenticated)
        assertNull(auth.state.value.uid)
        assertEquals(signedOutSession, auth.state.value.sessionVersion)
        repository.uidChanges.value = "bob"
        runCurrent()
        assertTrue(auth.state.value.sessionVersion > signedOutSession)
    }
}

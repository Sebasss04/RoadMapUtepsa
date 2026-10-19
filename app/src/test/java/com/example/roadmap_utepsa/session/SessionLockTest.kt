package com.example.roadmap_utepsa.session

import com.example.roadmap_utepsa.R

import com.example.roadmap_utepsa.presentation.session.SessionLock
import com.example.roadmap_utepsa.domain.session.LockPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SessionLockTest {
    private val dispatcher = StandardTestDispatcher()
    private class Preferences : LockPreferences {
        val values = mutableMapOf("a" to true)
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun enabled(uid: String): Boolean { gate?.await(); return values[uid] ?: false }
        override suspend fun setEnabled(uid: String, enabled: Boolean) { values[uid] = enabled }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun coldStartLocksAndOnlySuccessfulPromptUnlocks() = runTest(dispatcher) {
        val vm = SessionLock(Preferences()); vm.bind("a", 0, 0); runCurrent()
        assertTrue(vm.state.value.locked); assertTrue(vm.beginPrompt(false))
        vm.failed(R.string.biometric_cancelled); assertTrue(vm.state.value.locked)
        vm.beginPrompt(false); vm.authenticated(); assertFalse(vm.state.value.locked)
    }
    @Test fun interactiveLoginWinsRaceWithPreferenceLoading() = runTest(dispatcher) {
        val prefs = Preferences(); val gate = CompletableDeferred<Unit>(); prefs.gate = gate
        val vm = SessionLock(prefs); vm.bind("a", 1, 0); runCurrent()
        vm.bind("a", 1, 1); gate.complete(Unit); runCurrent()
        assertFalse(vm.state.value.locked); assertTrue(vm.state.value.enabled)
    }
    @Test fun backgroundDuringPreferenceLoadingRevokesInteractiveUnlock() = runTest(dispatcher) {
        val prefs = Preferences(); val gate = CompletableDeferred<Unit>(); prefs.gate = gate
        val vm = SessionLock(prefs); vm.bind("a", 1, 1); runCurrent()
        vm.background(false)
        gate.complete(Unit); runCurrent()
        assertTrue(vm.state.value.enabled)
        assertTrue("Returning from background must require biometrics even if preferences were still loading", vm.state.value.locked)
    }
    @Test fun backgroundLocksButRotationAndExternalFlowDoNot() = runTest(dispatcher) {
        val vm = SessionLock(Preferences()); vm.bind("a", 1, 1); runCurrent()
        vm.background(true); assertFalse(vm.state.value.locked)
        vm.beginExternal(); vm.background(false); assertFalse(vm.state.value.locked)
        vm.endExternal(); vm.background(false); assertTrue(vm.state.value.locked)
    }
    @Test fun preferenceEnablesOnlyAfterSuccessAndIsScopedByUid() = runTest(dispatcher) {
        val prefs = Preferences(); val vm = SessionLock(prefs); vm.bind("b", 1, 1); runCurrent()
        vm.beginPrompt(true); vm.failed(R.string.biometric_cancelled); assertFalse(prefs.values["b"] == true)
        vm.beginPrompt(true); vm.authenticated(); runCurrent(); assertTrue(prefs.values["b"] == true)
        vm.bind("c", 2, 1); runCurrent(); assertFalse(vm.state.value.enabled)
    }
    @Test fun latePromptCannotUnlockAnotherAccount() = runTest(dispatcher) {
        val vm = SessionLock(Preferences()); vm.bind("a", 1, 0); runCurrent(); vm.beginPrompt(false)
        vm.bind("b", 2, 0); vm.authenticated(); runCurrent(); assertEquals("b", vm.state.value.uid)
        assertFalse(vm.state.value.enabled)
    }
    @Test fun noDuplicatePromptAndRejectedScanStaysLocked() = runTest(dispatcher) {
        val vm = SessionLock(Preferences()); vm.bind("a", 0, 0); runCurrent()
        assertTrue(vm.beginPrompt(false)); assertFalse(vm.beginPrompt(false))
        vm.rejected(R.string.biometric_cancelled); assertTrue(vm.state.value.locked); assertTrue(vm.state.value.prompting)
    }
    @Test fun processRecreationDoesNotRestoreUnlockedState() = runTest(dispatcher) {
        val prefs = Preferences(); val first = SessionLock(prefs); first.bind("a", 0, 1); runCurrent()
        assertFalse(first.state.value.locked)
        val recreated = SessionLock(prefs); recreated.bind("a", 0, 0); runCurrent(); assertTrue(recreated.state.value.locked)
    }
    @Test fun signingOutRemovesPrivateIdentity() = runTest(dispatcher) {
        val vm = SessionLock(Preferences()); vm.bind("a", 1, 1); runCurrent()
        vm.bind(null, 2, 1); assertNull(vm.state.value.uid); assertFalse(vm.state.value.locked)
    }
    @Test fun unreadablePreferenceNeverDisablesProtectionAndInteractiveEntryStillWorks() = runTest(dispatcher) {
        val prefs = object : LockPreferences {
            override suspend fun enabled(uid: String): Boolean = error("Unreadable")
            override suspend fun setEnabled(uid: String, enabled: Boolean) { }
        }
        val restored = SessionLock(prefs); restored.bind("a", 1, 0); runCurrent()
        assertTrue(restored.state.value.locked); assertTrue(restored.state.value.enabled)
        val interactive = SessionLock(prefs); interactive.bind("a", 1, 1); runCurrent()
        assertFalse(interactive.state.value.locked); interactive.background(false); assertTrue(interactive.state.value.locked)
    }
}

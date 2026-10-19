package com.example.roadmap_utepsa.notification

import com.example.roadmap_utepsa.academic.AcademicFixtures

import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.OperacionHistorial
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.recommendation.DatosRecomendaciones
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.presentation.notification.PendingReminders
import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.domain.notification.ReminderMemory
import com.example.roadmap_utepsa.domain.notification.ReminderPreferences
import com.example.roadmap_utepsa.domain.notification.ReminderDelivery
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class PendingRemindersTest {
    private val dispatcher = StandardTestDispatcher()
    private val academic = AcademicFixtures().data()
    private var session: Pair<String?, Long> = "alice" to 1L
    private var data = DatosRecomendaciones(LecturaPreparacion(academic, null, null), null, 1)
    private var failRead = false
    private class Preferences : ReminderPreferences {
        val values = mutableMapOf<String, ReminderMemory>()
        var fail = false
        override suspend fun read(uid: String): ReminderMemory { check(!fail); return values[uid] ?: ReminderMemory() }
        override suspend fun update(uid: String, change: (ReminderMemory) -> ReminderMemory) =
            change(read(uid)).also { values[uid] = it }
    }
    private class Delivery : ReminderDelivery {
        var allowed = true; var succeeds = true
        var visible: ReminderRoute? = null
        val posts = mutableListOf<ReminderRoute>()
        override fun available() = allowed
        override fun show(route: ReminderRoute): Boolean {
            if (!succeeds) return false
            posts += route; visible = route; return true
        }
        override fun cancel() { visible = null }
        override fun keepOnly(route: ReminderRoute?) { if (visible != route) cancel() }
    }
    private val prefs = Preferences()
    private val delivery = Delivery()
    private fun vm(repo: RecomendacionesRepository = RecomendacionesRepository {
        if (failRead) error("server read failed") else data
    }) = PendingReminders(repo, prefs, delivery) { session }
    private fun confirmed(offer: Oferta? = null) {
        data = data.copy(preparacion = data.preparacion.copy(
            registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "confirmed")), oferta = offer)
    }
    private fun TestScope.start(enabled: Boolean = true): PendingReminders {
        val vm = vm(); vm.bind("alice", 1); runCurrent()
        if (enabled) { vm.setEnabled(true); runCurrent() }
        return vm
    }
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }

    @Test fun defaultIsOffAndHistoryHasPriorityWhenOptedIn() = runTest(dispatcher) {
        val vm = start(false); vm.evaluate(); runCurrent(); assertTrue(delivery.posts.isEmpty())
        vm.setEnabled(true); runCurrent(); vm.evaluate(); runCurrent()
        assertEquals(listOf(ReminderRoute("alice", ReminderKind.HISTORY)), delivery.posts)
    }
    @Test fun confirmedHistoryAndAbsentOfferNotifyButSavedEmptyOfferResolves() = runTest(dispatcher) {
        confirmed(); val vm = start(); vm.evaluate(); runCurrent()
        assertEquals(ReminderKind.OFFER, delivery.visible?.kind)
        confirmed(Oferta(academic.idPlan, emptySet(), 1)); vm.evaluate(); runCurrent()
        assertNull(delivery.visible); assertFalse(prefs.read("alice").offerSeen)
        confirmed(); vm.evaluate(); runCurrent(); assertEquals(2, delivery.posts.size)
    }
    @Test fun activeHistoryOperationDoesNotNotifyOrRearm() = runTest(dispatcher) {
        prefs.values["alice"] = ReminderMemory(true, historySeen = true)
        confirmed(); data = data.copy(preparacion = data.preparacion.copy(
            operacion = OperacionHistorial("op", "base", "goal", null, emptyMap())))
        val vm = start(); vm.evaluate(); runCurrent()
        assertTrue(delivery.posts.isEmpty()); assertTrue(prefs.read("alice").historySeen)
    }
    @Test fun repeatedEntryRotationAndControllerRecreationDoNotRepeat() = runTest(dispatcher) {
        val vm = start(); vm.evaluate(); runCurrent(); vm.evaluate(); runCurrent()
        vm.pause(); vm.evaluate(); runCurrent()
        val recreated = vm(); recreated.bind("alice", 1); runCurrent(); recreated.evaluate(); runCurrent()
        assertEquals(1, delivery.posts.size)
    }
    @Test fun historyRearmsOnlyAfterSuccessfulResolutionAndReappearance() = runTest(dispatcher) {
        val vm = start(); vm.evaluate(); runCurrent()
        confirmed(Oferta(academic.idPlan, emptySet(), 1)); failRead = true; vm.evaluate(); runCurrent()
        assertTrue(prefs.read("alice").historySeen)
        failRead = false; vm.evaluate(); runCurrent(); assertFalse(prefs.read("alice").historySeen)
        data = data.copy(preparacion = data.preparacion.copy(registro = null)); vm.evaluate(); runCurrent()
        assertEquals(2, delivery.posts.size)
    }
    @Test fun disablingCancelsVisibleButKeepsDeduplication() = runTest(dispatcher) {
        val vm = start(); vm.evaluate(); runCurrent(); vm.setEnabled(false); runCurrent()
        assertNull(delivery.visible); assertTrue(prefs.read("alice").historySeen)
        vm.setEnabled(true); runCurrent(); vm.evaluate(); runCurrent(); assertEquals(1, delivery.posts.size)
    }
    @Test fun deniedRevokedPermissionAndBlockedChannelDoNotConsumePendingTask() = runTest(dispatcher) {
        val vm = start(); delivery.allowed = false; vm.evaluate(); runCurrent()
        assertFalse(prefs.read("alice").historySeen); assertTrue(delivery.posts.isEmpty())
        delivery.allowed = true; vm.evaluate(); runCurrent(); assertEquals(1, delivery.posts.size)
        delivery.allowed = false; vm.evaluate(); runCurrent(); assertNull(delivery.visible)
        assertTrue(prefs.read("alice").historySeen)
    }
    @Test fun failedReadNeverInventsAPendingTask() = runTest(dispatcher) {
        val vm = start(); failRead = true; vm.evaluate(); runCurrent()
        assertTrue(delivery.posts.isEmpty()); assertFalse(prefs.read("alice").historySeen)
    }
    @Test fun switchingOrSigningOutCancelsAndIgnoresLateNonCancellableReads() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        prefs.values["alice"] = ReminderMemory(true)
        val vm = vm(RecomendacionesRepository { withContext(NonCancellable) { gate.await() }; data })
        vm.bind("alice", 1); runCurrent(); vm.evaluate(); runCurrent()
        session = "bob" to 2; vm.bind("bob", 2); runCurrent(); gate.complete(Unit); runCurrent()
        assertTrue(delivery.posts.isEmpty()); assertFalse(prefs.read("bob").enabled)
        session = null to 3; vm.bind(null, 3); assertNull(delivery.visible)
    }
    @Test fun lockOrLeavingContentSuppressesAnInFlightRead() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); prefs.values["alice"] = ReminderMemory(true)
        val vm = vm(RecomendacionesRepository { withContext(NonCancellable) { gate.await() }; data })
        vm.bind("alice", 1); runCurrent(); vm.evaluate(); runCurrent(); vm.pause()
        gate.complete(Unit); runCurrent(); assertTrue(delivery.posts.isEmpty())
        assertFalse(prefs.read("alice").historySeen)
    }
    @Test fun ownTaskFormDoesNotConsumeOrDisplayItsReminder() = runTest(dispatcher) {
        confirmed(); val vm = start(); vm.evaluate(ReminderKind.OFFER); runCurrent()
        assertTrue(delivery.posts.isEmpty()); assertFalse(prefs.read("alice").offerSeen)
        vm.evaluate(); runCurrent(); assertEquals(ReminderKind.OFFER, delivery.visible?.kind)
    }
    @Test fun deliveryRaceRollsBackReservationBeforeRetry() = runTest(dispatcher) {
        val vm = start(); delivery.succeeds = false; vm.evaluate(); runCurrent()
        assertFalse(prefs.read("alice").historySeen)
        delivery.succeeds = true; vm.evaluate(); runCurrent(); assertEquals(1, delivery.posts.size)
    }
    @Test fun unreadablePreferencesNeverEnableOrPost() = runTest(dispatcher) {
        prefs.fail = true; val vm = vm(); vm.bind("alice", 1); runCurrent(); vm.evaluate(); runCurrent()
        assertFalse(vm.state.value.ready); assertNotNull(vm.state.value.message); assertTrue(delivery.posts.isEmpty())
    }
    @Test fun sameUidNewSessionDiscardsOldResult() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); prefs.values["alice"] = ReminderMemory(true)
        val vm = vm(RecomendacionesRepository { withContext(NonCancellable) { gate.await() }; data })
        vm.bind("alice", 1); runCurrent(); vm.evaluate(); runCurrent()
        session = "alice" to 2; gate.complete(Unit); runCurrent(); assertTrue(delivery.posts.isEmpty())
    }
    @Test fun eachAccountKeepsIndependentOptInAndDeduplication() = runTest(dispatcher) {
        val vm = vm(RecomendacionesRepository { uid -> data.copy(preparacion = data.preparacion.copy(academic = academic.copy(uid = uid))) })
        prefs.values["alice"] = ReminderMemory(true)
        vm.bind("alice", 1); runCurrent(); vm.evaluate(); runCurrent()
        session = "bob" to 2; vm.bind("bob", 2); runCurrent(); assertNull(delivery.visible)
        assertFalse(vm.state.value.enabled); vm.setEnabled(true); runCurrent(); vm.evaluate(); runCurrent()
        assertEquals(listOf("alice", "bob"), delivery.posts.map { it.uid })
        session = "alice" to 3; vm.bind("alice", 3); runCurrent(); vm.evaluate(); runCurrent()
        assertEquals(2, delivery.posts.size); assertTrue(prefs.read("alice").historySeen)
    }
}

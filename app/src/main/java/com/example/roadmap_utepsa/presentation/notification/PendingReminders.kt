package com.example.roadmap_utepsa.presentation.notification

import androidx.lifecycle.ViewModel
import com.example.roadmap_utepsa.R
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.domain.notification.ReminderPreferences
import com.example.roadmap_utepsa.domain.notification.ReminderDelivery

data class ReminderSettings(val uid: String? = null, val loading: Boolean = true,
    val ready: Boolean = false, val enabled: Boolean = false, val message: Int? = null)

/** Only reads the existing stable academic query. Never writes academic data. */
class PendingReminders(private val repository: RecomendacionesRepository,
    private val preferences: ReminderPreferences, private val delivery: ReminderDelivery,
    private val currentSession: () -> Pair<String?, Long>) : ViewModel() {
    private val mutable = MutableStateFlow(ReminderSettings())
    val state = mutable.asStateFlow()
    private var session: Pair<String?, Long> = null to -1L
    private var generation = 0L
    private var readVersion = 0L
    private var eligible = false
    private var load: Job? = null
    private var evaluation: Job? = null
    private val mutex = Mutex()
    private fun current(expected: Long) = generation == expected && session == currentSession()

    fun bind(uid: String?, version: Long) {
        if (session == (uid to version)) return
        session = uid to version; generation++; pause(); load?.cancel(); delivery.cancel()
        mutable.value = ReminderSettings(uid, loading = uid != null)
        if (uid != null) loadPreference()
    }
    fun retry() { if (session.first != null && !state.value.loading) loadPreference() }
    private fun loadPreference() {
        val uid = session.first ?: return; val expected = generation
        mutable.update { it.copy(loading = true, message = null) }
        load = viewModelScope.launch {
            try {
                val memory = mutex.withLock { preferences.read(uid) }
                if (current(expected)) mutable.value = ReminderSettings(uid, loading = false, ready = true, enabled = memory.enabled)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (current(expected)) mutable.value = ReminderSettings(uid, loading = false,
                message = R.string.notification_preference_read_failed) }
        }
    }
    fun setEnabled(enabled: Boolean) {
        val uid = session.first ?: return
        if (!state.value.ready || state.value.loading || session != currentSession()) return
        val expected = generation; pause()
        if (!enabled) delivery.cancel()
        mutable.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            try {
                val memory = mutex.withLock { preferences.update(uid) { it.copy(enabled = enabled) } }
                if (current(expected)) mutable.update { it.copy(loading = false, enabled = memory.enabled) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (current(expected)) mutable.update { it.copy(loading = false,
                message = R.string.notification_preference_save_failed) } }
        }
    }
    fun deliveryAvailable(): Boolean = delivery.available()

    fun pause() { eligible = false; readVersion++; evaluation?.cancel() }

    /** Called only on entering/resuming academic content after all access gates. */
    fun evaluate(completing: ReminderKind? = null) {
        if (!state.value.ready || state.value.loading || session != currentSession()) return
        eligible = true; evaluation?.cancel()
        val uid = session.first ?: return; val expected = generation; val request = ++readVersion
        fun active() = current(expected) && eligible && request == readVersion
        evaluation = viewModelScope.launch {
            try { mutex.withLock {
                val data = repository.load(uid)
                if (!active()) return@withLock
                check(data.preparacion.academic.uid == uid)
                check(data.oferta == null || data.oferta.idPlan == data.preparacion.academic.idPlan)
                val operation = data.preparacion.operacion
                if (operation != null && !operation.completada && !operation.cancelada) {
                    delivery.cancel(); return@withLock
                }
                val confirmed = data.preparacion.registro?.decision == DecisionPreparacion.CONFIRMADO
                val kind = when { !confirmed -> ReminderKind.HISTORY; data.oferta == null -> ReminderKind.OFFER; else -> null }
                // Only a successful stable observation can rearm a resolved task.
                val memory = preferences.update(uid) {
                    it.copy(historySeen = it.historySeen && !confirmed, offerSeen = it.offerSeen && data.oferta == null)
                }
                if (!active()) return@withLock
                val route = kind?.let { ReminderRoute(uid, it) }
                delivery.keepOnly(route)
                if (!memory.enabled || kind == null || kind == completing || !delivery.available()) {
                    delivery.cancel(); return@withLock
                }
                if (memory.seen(kind)) return@withLock
                var reserved = false; var posted = false
                try {
                    // Reserve atomically before Android delivery: process death cannot repeat a posted task.
                    preferences.update(uid) {
                        if (it.enabled && !it.seen(kind)) { reserved = true; it.mark(kind, true) } else it
                    }
                    if (reserved && active() && delivery.available()) posted = delivery.show(ReminderRoute(uid, kind))
                } finally {
                    if (reserved && !posted) withContext(NonCancellable) {
                        preferences.update(uid) { it.mark(kind, false) }
                    }
                }
            } } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (active()) delivery.cancel() }
        }
    }
}

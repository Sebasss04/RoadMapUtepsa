package com.example.roadmap_utepsa.presentation.session

import androidx.lifecycle.ViewModel
import com.example.roadmap_utepsa.R
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.roadmap_utepsa.domain.session.LockPreferences

data class LockState(val uid: String? = null, val loading: Boolean = false, val enabled: Boolean = false,
    val locked: Boolean = false, val prompting: Boolean = false, val message: Int? = null)

class SessionLock(private val preferences: LockPreferences) : ViewModel() {
    private val mutable = MutableStateFlow(LockState())
    val state = mutable.asStateFlow()
    private var sessionVersion = -1L
    private var interactiveEntry = 0L
    private var generation = 0L
    private var externalFlows = 0
    private var enabling = false
    private var promptGeneration = -1L
    private var interactiveSession = false

    fun bind(uid: String?, version: Long, entry: Long) {
        val interactive = entry > interactiveEntry
        interactiveEntry = entry
        if (uid == state.value.uid && version == sessionVersion) {
            if (interactive) { interactiveSession = true; mutable.update { it.copy(locked = false) } }
            return
        }
        sessionVersion = version; generation++; externalFlows = 0; interactiveSession = interactive
        val expected = generation
        mutable.value = LockState(uid = uid, loading = uid != null, locked = uid != null)
        if (uid == null) return
        viewModelScope.launch {
            try {
                val enabled = preferences.enabled(uid)
                if (generation == expected) mutable.value = LockState(uid, enabled = enabled, locked = enabled && !interactiveSession)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generation == expected) mutable.update { it.copy(loading = false, enabled = true,
                    locked = !interactiveSession, message = R.string.session_preference_read_failed) }
            }
        }
    }
    fun background(changingConfiguration: Boolean) {
        if (!changingConfiguration && externalFlows == 0 && !state.value.prompting) {
            // Revoke interactive entry even while the persisted preference is still loading.
            // Its eventual result must not reopen a session that already left the foreground.
            interactiveSession = false
            if (state.value.enabled) mutable.update { it.copy(locked = true) }
        }
    }
    fun beginExternal() { externalFlows++ }
    fun endExternal() { externalFlows = (externalFlows - 1).coerceAtLeast(0) }
    fun beginPrompt(enable: Boolean): Boolean {
        if (state.value.prompting || state.value.loading || state.value.uid == null) return false
        enabling = enable; promptGeneration = generation
        mutable.update { it.copy(prompting = true, message = null) }
        return true
    }
    fun rejected(message: Int) {
        if (promptGeneration == generation) mutable.update { it.copy(message = message) }
    }
    fun failed(message: Int) {
        if (promptGeneration == generation) mutable.update { it.copy(prompting = false, message = message) }
    }
    fun authenticated() {
        if (promptGeneration != generation || !state.value.prompting) return
        val expected = generation; val uid = state.value.uid ?: return
        if (!enabling) { mutable.update { it.copy(locked = false, prompting = false, message = null) }; return }
        viewModelScope.launch {
            try {
                preferences.setEnabled(uid, true)
                if (generation == expected) mutable.update { it.copy(enabled = true, locked = false, prompting = false, message = null) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == expected) failed(R.string.session_preference_save_failed) }
        }
    }
    fun disable() {
        if (state.value.locked || state.value.prompting) return
        val uid = state.value.uid ?: return; val expected = generation
        viewModelScope.launch {
            try {
                preferences.setEnabled(uid, false)
                if (expected == generation) mutable.update { it.copy(enabled = false, message = null) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (expected == generation) mutable.update { it.copy(message = R.string.session_disable_failed) } }
        }
    }
}

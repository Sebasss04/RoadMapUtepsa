package com.example.roadmap_utepsa.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.profile.validProfileName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProfileLoad { LOADING, READY, ERROR }

data class ProfileUiState(
    val load: ProfileLoad = ProfileLoad.LOADING,
    val profile: ProfileData? = null,
    val names: String = "",
    val surnames: String = "",
    val submitted: Boolean = false,
    val saving: Boolean = false,
    val awaitingConfirmation: Boolean = false,
    val saved: Boolean = false,
    val saveFailed: Boolean = false
) {
    val invalidNames get() = submitted && !validProfileName(names)
    val invalidSurnames get() = submitted && !validProfileName(surnames)
    val hasChanges get() = profile?.let { names != it.names || surnames != it.surnames } ?: false
}

class ProfileViewModel(
    private val uid: String,
    private val repository: ProfileRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state = mutableState.asStateFlow()

    init {
        require(uid.isNotBlank())
        load()
    }

    fun retryLoad() {
        if (state.value.load == ProfileLoad.ERROR) load()
    }

    private fun load() {
        mutableState.update { it.copy(load = ProfileLoad.LOADING) }
        viewModelScope.launch {
            try {
                val profile = repository.load(uid)
                mutableState.value = ProfileUiState(
                    load = ProfileLoad.READY, profile = profile,
                    names = profile.names, surnames = profile.surnames
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Failure never opens an empty form or triggers registration.
                mutableState.update { it.copy(load = ProfileLoad.ERROR) }
            }
        }
    }

    fun changeNames(value: String) {
        if (!state.value.saving) mutableState.update {
            it.copy(names = value, saved = false, saveFailed = false)
        }
    }

    fun changeSurnames(value: String) {
        if (!state.value.saving) mutableState.update {
            it.copy(surnames = value, saved = false, saveFailed = false)
        }
    }

    fun discardChanges() {
        val current = state.value
        if (current.saving) return
        val profile = current.profile ?: return
        mutableState.update {
            it.copy(names = profile.names, surnames = profile.surnames,
                submitted = false, saved = false, saveFailed = false)
        }
    }

    fun save() {
        val current = state.value
        if (current.load != ProfileLoad.READY || current.saving) return
        mutableState.update { it.copy(submitted = true, saved = false) }
        if (!validProfileName(current.names) || !validProfileName(current.surnames)) return
        mutableState.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            // A slow/offline write may remain queued. Do not cancel it and offer a duplicate.
            val confirmationNotice = launch {
                delay(10_000)
                mutableState.update { it.copy(awaitingConfirmation = true) }
            }
            try {
                val profile = repository.save(uid, current.names.trim(), current.surnames.trim())
                mutableState.update {
                    it.copy(profile = profile, names = profile.names, surnames = profile.surnames,
                        saved = true, submitted = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(saveFailed = true) }
            } finally {
                confirmationNotice.cancel()
                mutableState.update { it.copy(saving = false, awaitingConfirmation = false) }
            }
        }
    }

    class Factory(private val uid: String, private val repository: ProfileRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(uid, repository) as T
        }
    }
}

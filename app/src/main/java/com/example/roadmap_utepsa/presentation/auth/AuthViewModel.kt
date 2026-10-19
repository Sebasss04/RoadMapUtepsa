package com.example.roadmap_utepsa.presentation.auth

import com.example.roadmap_utepsa.presentation.auth.accountError

import androidx.lifecycle.ViewModel
import com.example.roadmap_utepsa.R
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: Int? = null,
    val uid: String? = null,
    val sessionVersion: Long = 0,
    val errorMessageRes: Int? = null,
    val identity: AuthIdentity? = null,
    val interactiveEntry: Long = 0,
    val notice: Int? = null,
    val resendAvailableAt: Long = 0
)

class AuthViewModel(private val repository: AuthRepository,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000 }) : ViewModel() {
    private var attempt = 0L
    private val mutableState = MutableStateFlow(
        AuthUiState(isAuthenticated = repository.isAuthenticated, uid = repository.currentUid, identity = repository.currentIdentity)
    )
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.identityChanges.collect {
                synchronizeIdentity(repository.currentUid)
            }
        }
    }

    private fun synchronizeIdentity(uid: String?) {
        mutableState.update {
            it.copy(isAuthenticated = uid != null, uid = uid, identity = repository.currentIdentity,
                sessionVersion = if (it.uid != uid) it.sessionVersion + 1 else it.sessionVersion)
        }
    }

    // El proveedor se usa solo durante la operación; el ViewModel no guarda un Context.
    suspend fun signInWithGoogle(getIdToken: suspend () -> Result<String>) {
        if (state.value.isLoading || state.value.isAuthenticated) return
        val expected = ++attempt
        mutableState.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null) }
        try {
            val token = getIdToken().getOrThrow()
            if (expected != attempt) return
            repository.signInWithGoogle(token)
            if (expected != attempt) return
            synchronizeIdentity(repository.currentUid)
            mutableState.update { it.copy(interactiveEntry = it.interactiveEntry + 1) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (expected == attempt) mutableState.update {
                it.copy(errorMessage = null, errorMessageRes = googleSignInErrorMessage(e))
            }
        } finally {
            if (expected == attempt) mutableState.update { it.copy(isLoading = false) }
        }
    }

    fun signOut() {
        attempt++
        repository.signOut()
        synchronizeIdentity(repository.currentUid)
        mutableState.update { it.copy(isLoading = false, errorMessage = null, errorMessageRes = null, notice = null, resendAvailableAt = 0) }
    }

    suspend fun emailAccess(email: String, password: String, registration: Boolean) {
        if (state.value.isLoading || state.value.isAuthenticated) return
        val expected = ++attempt
        val version = state.value.sessionVersion
        mutableState.update { it.copy(isLoading = true, errorMessage = null, errorMessageRes = null, notice = null) }
        try {
            if (registration) repository.register(email, password) else repository.signInWithEmail(email, password)
            if (expected != attempt) return
            synchronizeIdentity(repository.currentUid)
            mutableState.update { it.copy(interactiveEntry = it.interactiveEntry + 1) }
            if (registration) verifyEmail()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (expected == attempt && version == state.value.sessionVersion) mutableState.update { it.copy(errorMessage = accountError(e)) }
        } finally { if (expected == attempt) mutableState.update { it.copy(isLoading = false) } }
    }

    suspend fun verifyEmail() {
        val uid = state.value.uid ?: return
        val version = state.value.sessionVersion
        if (clockMillis() < state.value.resendAvailableAt) return
        mutableState.update { it.copy(isLoading = true, errorMessage = null,
            resendAvailableAt = clockMillis() + 60_000) }
        try {
            repository.sendVerification()
            if (state.value.uid == uid && state.value.sessionVersion == version) mutableState.update { it.copy(notice = R.string.auth_verification_sent) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (state.value.uid == uid && state.value.sessionVersion == version) mutableState.update { it.copy(errorMessage = accountError(e)) } }
        finally { if (state.value.uid == uid && state.value.sessionVersion == version) mutableState.update { it.copy(isLoading = false) } }
    }

    suspend fun checkVerification() {
        val uid = state.value.uid ?: return
        val version = state.value.sessionVersion
        if (state.value.isLoading) return
        mutableState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val identity = repository.refreshIdentity()
            if (state.value.uid == uid && state.value.sessionVersion == version) mutableState.update { it.copy(identity = identity,
                notice = if (identity?.requiresVerification == true) R.string.auth_email_not_verified else null) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (state.value.uid == uid && state.value.sessionVersion == version) mutableState.update { it.copy(errorMessage = accountError(e)) } }
        finally { if (state.value.uid == uid && state.value.sessionVersion == version) mutableState.update { it.copy(isLoading = false) } }
    }

    suspend fun recover(email: String) {
        if (state.value.isLoading) return
        val version = state.value.sessionVersion
        mutableState.update { it.copy(isLoading = true, errorMessage = null, notice = null) }
        try {
            repository.sendPasswordReset(email)
            if (version == state.value.sessionVersion) mutableState.update { it.copy(notice =
                R.string.auth_recovery_neutral) }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (version == state.value.sessionVersion) mutableState.update { it.copy(errorMessage = accountError(e)) } }
        finally { if (version == state.value.sessionVersion) mutableState.update { it.copy(isLoading = false) } }
    }

    fun clearFeedback() { mutableState.update { it.copy(errorMessage = null, errorMessageRes = null, notice = null) } }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
    }
}

package com.example.roadmap_utepsa.presentation.account

import androidx.lifecycle.ViewModel
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.presentation.auth.accountError
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AccountState(val busy: Boolean = false, val message: Int? = null, val error: Boolean = false,
    val reconciled: Boolean = true)

class AccountViewModel(private val auth: AuthRepository, private val uid: String,
    private val version: Long, private val sessionVersion: () -> Long) : ViewModel() {
    private val mutableState = MutableStateFlow(AccountState())
    val state = mutableState.asStateFlow()
    private fun active() = auth.currentUid == uid && sessionVersion() == version

    fun link(password: String?, newPassword: String?, google: suspend () -> Result<String>) {
        if (state.value.busy || !state.value.reconciled || !active()) return
        mutableState.value = AccountState(busy = true)
        viewModelScope.launch {
            try {
                if (password != null) auth.reauthenticatePassword(password)
                else {
                    val token = google().getOrThrow()
                    if (!active()) return@launch
                    auth.reauthenticateGoogle(token)
                }
                if (!active()) return@launch
                if (newPassword != null) auth.linkPassword(newPassword)
                else {
                    val token = google().getOrThrow()
                    if (!active()) return@launch
                    auth.linkGoogle(token)
                }
                if (!active()) return@launch
                val refreshed = auth.refreshIdentity()
                val provider = if (newPassword == null) "google.com" else "password"
                if (active()) mutableState.value = if (refreshed?.uid == uid && provider in refreshed.providers)
                    AccountState(message = R.string.account_method_linked)
                else AccountState(message = R.string.account_link_unconfirmed, error = true)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (active()) {
                    var known = false
                    try { auth.refreshIdentity(); known = true } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { }
                    if (active()) mutableState.value = AccountState(message = accountError(e), error = true, reconciled = known)
                }
            } finally { if (active()) mutableState.update { it.copy(busy = false) } }
        }
    }
    fun refresh() {
        if (state.value.busy || !active()) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                auth.refreshIdentity()
                if (active()) mutableState.value = AccountState()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (active()) mutableState.value = AccountState(message = accountError(e), error = true, reconciled = false) }
        }
    }
    class Factory(private val auth: AuthRepository, private val uid: String, private val version: Long,
        private val sessionVersion: () -> Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountViewModel(auth, uid, version, sessionVersion) as T
    }
}

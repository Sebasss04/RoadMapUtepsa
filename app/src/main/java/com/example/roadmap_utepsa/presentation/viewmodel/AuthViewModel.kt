package com.example.roadmap_utepsa.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val uid: String? = null,
    val sessionVersion: Long = 0
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(
        AuthUiState(isAuthenticated = repository.isAuthenticated, uid = repository.currentUid)
    )
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.uidChanges.collect { uid ->
                synchronizeIdentity(uid)
            }
        }
    }

    private fun synchronizeIdentity(uid: String?) {
        mutableState.update {
            it.copy(isAuthenticated = uid != null, uid = uid,
                sessionVersion = if (it.uid != uid) it.sessionVersion + 1 else it.sessionVersion)
        }
    }

    // El proveedor se usa solo durante la operación; el ViewModel no guarda un Context.
    suspend fun signInWithGoogle(getIdToken: suspend () -> Result<String>) {
        if (state.value.isLoading || state.value.isAuthenticated) return
        mutableState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            repository.signInWithGoogle(getIdToken().getOrThrow())
            synchronizeIdentity(repository.currentUid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutableState.update {
                it.copy(errorMessage = "No se pudo iniciar sesión con Google. Inténtalo de nuevo.")
            }
        } finally {
            mutableState.update { it.copy(isLoading = false) }
        }
    }

    fun signOut() {
        repository.signOut()
        synchronizeIdentity(repository.currentUid)
        mutableState.update { it.copy(isLoading = false, errorMessage = null) }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AuthViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
    }
}

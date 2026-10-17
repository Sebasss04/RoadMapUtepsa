package com.example.roadmap_utepsa.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.data.AcademicRepository
import com.example.roadmap_utepsa.data.AuthRepository
import com.example.roadmap_utepsa.data.ConflictoEstadoAcademico
import com.example.roadmap_utepsa.domain.academic.ReglasAcademicas
import com.example.roadmap_utepsa.domain.academic.ResultadoHabilitacion
import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.academic.ResultadoTransicionHistorial
import com.example.roadmap_utepsa.domain.model.AcademicData
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

data class AcademicEditor(
    val idMateria: String, val selected: EstadoAcademico,
    val saving: Boolean = false, val awaitingConfirmation: Boolean = false, val failed: Boolean = false,
    val transition: ResultadoTransicionHistorial? = null,
    val conflictStates: Map<String, EstadoAcademico>? = null
) {
    val conflict get() = conflictStates != null
}

sealed interface AcademicUiState {
    data object Loading : AcademicUiState
    data class Ready(val data: AcademicData, val habilitaciones: Map<String, ResultadoHabilitacion>,
                     val editor: AcademicEditor? = null, val saved: Boolean = false) : AcademicUiState {
        val coherencia get() = ReglasHistorial(data.materias, data.prerrequisitos).validar(data.estados)
    }
    data object Error : AcademicUiState
    data object SessionExpired : AcademicUiState
}

class AcademicViewModel(
    private val uid: String,
    private val repository: AcademicRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow<AcademicUiState>(AcademicUiState.Loading)
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var expired = false

    init {
        require(uid.isNotBlank())
        viewModelScope.launch {
            authRepository.uidChanges.collect { if (it != uid) expireSession() }
        }
        reload()
    }

    private fun expireSession() {
        expired = true
        loadJob?.cancel()
        saveJob?.cancel()
        mutableState.value = AcademicUiState.SessionExpired
    }

    /** Borra datos anteriores durante una nueva lectura; no publica resultados parciales. */
    fun reload() {
        if (expired || authRepository.currentUid != uid) {
            expireSession()
            return
        }
        if (loadJob?.isActive == true || saveJob?.isActive == true) return
        mutableState.value = AcademicUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val data = repository.load(uid)
                kotlin.coroutines.coroutineContext.ensureActive()
                check(data.uid == uid) { "El repositorio devolvió datos de otra cuenta" }
                val rules = ReglasAcademicas(data.materias, data.prerrequisitos)
                val evaluations = data.materias.associate { it.id to rules.evaluar(it.id, data.estados) }
                if (!expired && authRepository.currentUid == uid) {
                    mutableState.value = AcademicUiState.Ready(data, evaluations)
                } else expireSession()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (expired || authRepository.currentUid != uid) expireSession()
                else mutableState.value = AcademicUiState.Error
            }
        }
    }

    /** Una lectura iniciada antes del asistente no puede ocultar su guardado. */
    fun refreshFromHistory() {
        if (saveJob?.isActive == true) return
        loadJob?.cancel()
        loadJob = null
        reload()
    }

    fun openSubject(id: String) {
        val current = state.value as? AcademicUiState.Ready ?: return
        if (current.editor?.saving == true || current.data.materias.none { it.id == id }) return
        mutableState.value = current.copy(editor = AcademicEditor(id,
            current.data.estados[id] ?: EstadoAcademico.PENDIENTE), saved = false)
    }

    fun selectState(value: EstadoAcademico) {
        val current = state.value as? AcademicUiState.Ready ?: return
        val editor = current.editor ?: return
        if (!editor.saving) mutableState.value = current.copy(editor = editor.copy(selected = value, failed = false,
            transition = ReglasHistorial(current.data.materias, current.data.prerrequisitos)
                .evaluarTransicion(editor.idMateria, value, current.data.estados)))
    }

    fun closeSubject() {
        val current = state.value as? AcademicUiState.Ready ?: return
        if (current.editor?.saving != true) mutableState.value = current.copy(editor = null)
    }

    fun dismissSaved() {
        val current = state.value as? AcademicUiState.Ready ?: return
        mutableState.value = current.copy(saved = false)
    }

    /** Revisa la lectura que causó el conflicto; conservar selección no autoriza guardarla. */
    fun reviewConflict() {
        val current = state.value as? AcademicUiState.Ready ?: return
        val editor = current.editor ?: return
        if (editor.saving) return
        val states = editor.conflictStates ?: return
        if (expired || authRepository.currentUid != uid) { expireSession(); return }
        val data = current.data.copy(estados = states.toMap())
        val rules = ReglasAcademicas(data.materias, data.prerrequisitos)
        mutableState.value = AcademicUiState.Ready(data,
            data.materias.associate { it.id to rules.evaluar(it.id, data.estados) },
            editor = editor.copy(failed = false, awaitingConfirmation = false, conflictStates = null,
                transition = ReglasHistorial(data.materias, data.prerrequisitos)
                    .evaluarTransicion(editor.idMateria, editor.selected, data.estados)))
    }

    fun saveState() {
        val current = state.value as? AcademicUiState.Ready ?: return
        val editor = current.editor ?: return
        if (editor.saving || editor.conflict || saveJob?.isActive == true) return
        if (expired || authRepository.currentUid != uid) { expireSession(); return }
        if ((current.data.estados[editor.idMateria] ?: EstadoAcademico.PENDIENTE) == editor.selected) {
            closeSubject()
            return
        }
        val transicion = ReglasHistorial(current.data.materias, current.data.prerrequisitos)
            .evaluarTransicion(editor.idMateria, editor.selected, current.data.estados)
        if (!transicion.permitida) {
            mutableState.value = current.copy(editor = editor.copy(transition = transicion))
            return
        }
        mutableState.value = current.copy(editor = editor.copy(saving = true, failed = false))
        saveJob = viewModelScope.launch {
            val notice = launch {
                delay(10_000)
                val waiting = state.value as? AcademicUiState.Ready
                if (waiting?.editor?.saving == true) mutableState.value = waiting.copy(
                    editor = waiting.editor.copy(awaitingConfirmation = true))
            }
            try {
                val states = repository.saveState(uid, editor.idMateria, editor.selected, current.data.estados.toMap())
                check((states[editor.idMateria] ?: EstadoAcademico.PENDIENTE) == editor.selected)
                val data = current.data.copy(estados = states.toMap())
                val rules = ReglasAcademicas(data.materias, data.prerrequisitos)
                val evaluations = data.materias.associate { it.id to rules.evaluar(it.id, states) }
                if (!expired && authRepository.currentUid == uid) {
                    mutableState.value = AcademicUiState.Ready(data, evaluations, saved = true)
                } else expireSession()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (expired || authRepository.currentUid != uid) expireSession()
                else {
                    // El SDK puede envolver la excepción emitida por la transacción.
                    val conflict = generateSequence<Throwable>(e) { it.cause }
                        .filterIsInstance<ConflictoEstadoAcademico>().firstOrNull()
                    mutableState.value = current.copy(editor = editor.copy(failed = conflict == null,
                        conflictStates = conflict?.estadosActuales?.toMap()))
                }
            } finally { notice.cancel() }
        }
    }

    class Factory(private val uid: String, private val repository: AcademicRepository,
                  private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AcademicViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return AcademicViewModel(uid, repository, authRepository) as T
        }
    }

    companion object {
        /** Se usará al integrar navegación: rotación conserva; sesión nueva relee. */
        fun key(uid: String, sessionVersion: Long) = "academic:$uid:$sessionVersion"
    }
}

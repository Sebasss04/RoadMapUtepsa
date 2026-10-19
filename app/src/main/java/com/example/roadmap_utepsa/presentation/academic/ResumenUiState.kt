package com.example.roadmap_utepsa.presentation.academic

import com.example.roadmap_utepsa.domain.academic.ResumenAcademico
import com.example.roadmap_utepsa.presentation.academic.AcademicUiState
import com.example.roadmap_utepsa.presentation.history.EstadoPreparacionUi

sealed interface ResumenUiState {
    data object Loading : ResumenUiState
    data object Error : ResumenUiState
    data object SessionExpired : ResumenUiState
    data class Ready(val resumen: ResumenAcademico, val historial: EstadoPreparacionUi) : ResumenUiState
}

internal fun resumenState(state: AcademicUiState, history: EstadoPreparacionUi): ResumenUiState = when (state) {
    AcademicUiState.Loading -> ResumenUiState.Loading
    AcademicUiState.Error -> ResumenUiState.Error
    AcademicUiState.SessionExpired -> ResumenUiState.SessionExpired
    is AcademicUiState.Ready -> when (history) {
        EstadoPreparacionUi.CARGANDO -> ResumenUiState.Loading
        EstadoPreparacionUi.ERROR -> ResumenUiState.Error
        else -> try {
            ResumenUiState.Ready(requireNotNull(state.resumen),
                if (history != EstadoPreparacionUi.GUARDADO_PENDIENTE && state.coherencia.contradicciones.isNotEmpty())
                    EstadoPreparacionUi.REQUIERE_REVISION else history)
        } catch (_: IllegalArgumentException) { ResumenUiState.Error }
    }
}

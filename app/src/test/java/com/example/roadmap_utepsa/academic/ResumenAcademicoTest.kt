package com.example.roadmap_utepsa.academic

import com.example.roadmap_utepsa.domain.academic.ResumenAcademico
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.presentation.academic.ResumenUiState
import com.example.roadmap_utepsa.presentation.academic.resumenState
import com.example.roadmap_utepsa.presentation.academic.AcademicUiState
import com.example.roadmap_utepsa.presentation.history.EstadoPreparacionUi
import org.junit.Assert.*
import org.junit.Test

class ResumenAcademicoTest {
    private val base = AcademicFixtures().data()
    private fun data(approved: Int, course: Int = 0) = base.copy(estados = base.materias.take(approved + course).mapIndexed { i, m ->
        m.id to if (i < approved) EstadoAcademico.APROBADA else EstadoAcademico.EN_CURSO
    }.toMap())
    private fun ready(approved: Int = 0) = AcademicUiState.Ready(data(approved), emptyMap())

    @Test fun zeroApprovedIsSuccessfulEmptyHistory() {
        assertEquals(ResumenAcademico(0, 0, 50), ResumenAcademico.calcular(base))
        assertEquals(0, ResumenAcademico.calcular(base).porcentaje)
    }
    @Test fun oneAndSeveralApprovedUseFixedDenominator() {
        assertEquals(2, ResumenAcademico.calcular(data(1)).porcentaje)
        assertEquals(10, ResumenAcademico.calcular(data(5, 2)).porcentaje)
        assertEquals(ResumenAcademico(5, 2, 43), ResumenAcademico.calcular(data(5, 2)))
    }
    @Test fun inCourseDoesNotIncreaseApprovedProgress() {
        assertEquals(ResumenAcademico(0, 7, 43), ResumenAcademico.calcular(data(0, 7)))
        assertEquals(0, ResumenAcademico.calcular(data(0, 7)).porcentaje)
    }
    @Test fun allApprovedAndEveryCombinationSumToFifty() {
        assertEquals(100, ResumenAcademico.calcular(data(50)).porcentaje)
        for (approved in 0..50) for (course in 0..50-approved) {
            val result = ResumenAcademico.calcular(data(approved, course))
            assertEquals(50, result.aprobadas + result.enCurso + result.pendientes)
            assertEquals(approved * 2, result.porcentaje)
        }
    }
    @Test fun explicitPendingStillCountsOnce() {
        assertEquals(ResumenAcademico(0, 0, 50), ResumenAcademico.calcular(base.copy(estados = mapOf(base.materias[0].id to EstadoAcademico.PENDIENTE))))
    }
    @Test(expected = IllegalArgumentException::class) fun incompleteCatalogIsNotZeroProgress() {
        ResumenAcademico.calcular(base.copy(materias = base.materias.dropLast(1)))
    }
    @Test(expected = IllegalArgumentException::class) fun extraElectiveCannotBeAddedToThePlan() {
        ResumenAcademico.calcular(base.copy(materias = base.materias + base.materias[0].copy(id = "extra-elective")))
    }
    @Test(expected = IllegalArgumentException::class) fun foreignSubjectStateIsNotSilentlyCounted() {
        ResumenAcademico.calcular(base.copy(estados = mapOf("foreign" to EstadoAcademico.APROBADA)))
    }
    @Test fun loadingErrorAndExpiredNeverExposeCounts() {
        assertEquals(ResumenUiState.Loading, resumenState(AcademicUiState.Loading, EstadoPreparacionUi.CONFIRMADO))
        assertEquals(ResumenUiState.Error, resumenState(AcademicUiState.Error, EstadoPreparacionUi.CONFIRMADO))
        assertEquals(ResumenUiState.SessionExpired, resumenState(AcademicUiState.SessionExpired, EstadoPreparacionUi.CONFIRMADO))
        assertEquals(ResumenUiState.Loading, resumenState(ready(5), EstadoPreparacionUi.CARGANDO))
        assertEquals(ResumenUiState.Error, resumenState(ready(5), EstadoPreparacionUi.ERROR))
    }
    @Test fun pendingAndPartialHistoryKeepExplicitWarning() {
        listOf(EstadoPreparacionUi.CONFIGURACION_PENDIENTE, EstadoPreparacionUi.GUARDADO_PENDIENTE).forEach {
            assertEquals(it, (resumenState(ready(), it) as ResumenUiState.Ready).historial)
        }
    }
    @Test fun partialSaveKeepsRecoveryWarningEvenWithIncoherence() {
        val invalid = base.copy(estados = mapOf(base.materias[1].id to EstadoAcademico.EN_CURSO))
        val state = AcademicUiState.Ready(invalid, emptyMap())
        assertTrue(state.coherencia.contradicciones.isNotEmpty())
        val result = resumenState(state, EstadoPreparacionUi.GUARDADO_PENDIENTE) as ResumenUiState.Ready
        assertEquals(EstadoPreparacionUi.GUARDADO_PENDIENTE, result.historial)
        assertEquals(ResumenAcademico(0, 1, 49), result.resumen)
    }
    @Test fun confirmedMarkerDoesNotHideIncoherence() {
        val invalid = base.copy(estados = mapOf(base.materias[1].id to EstadoAcademico.EN_CURSO))
        val result = resumenState(AcademicUiState.Ready(invalid, emptyMap()), EstadoPreparacionUi.CONFIRMADO) as ResumenUiState.Ready
        assertEquals(EstadoPreparacionUi.REQUIERE_REVISION, result.historial)
        assertEquals(0, result.resumen.porcentaje)
    }
    @Test fun malformedReadyStateIsReadError() {
        assertEquals(ResumenUiState.Error, resumenState(AcademicUiState.Ready(base.copy(materias = emptyList()), emptyMap()), EstadoPreparacionUi.CONFIRMADO))
    }
}

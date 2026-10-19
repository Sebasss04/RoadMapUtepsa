package com.example.roadmap_utepsa.academic

import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.filterCurriculum
import org.junit.Assert.*
import org.junit.Test

class CurriculumFilterTest {
    private val original = AcademicFixtures().data()
    @Test fun searchesNamesIgnoringAccentsCaseAndOuterSpaces() {
        val data = original.copy(materias = listOf(original.materias.first().copy(nombre = "Álgebra Lineal")))
        assertEquals(1, filterCurriculum(data, " ALGEBRA ", null, null).size)
    }
    @Test fun searchesCodeAndCombinesStateAndSemester() {
        val materia = original.materias.first()
        val data = original.copy(estados = mapOf(materia.id to EstadoAcademico.APROBADA))
        assertEquals(listOf(materia), filterCurriculum(data, materia.codigo.lowercase(), EstadoAcademico.APROBADA, materia.semestre))
        assertTrue(filterCurriculum(data, materia.codigo, EstadoAcademico.PENDIENTE, null).isEmpty())
        assertTrue(filterCurriculum(data, materia.codigo, null, 9).isEmpty())
    }
    @Test fun missingStatesArePendingAndUnmatchedSearchIsEmpty() {
        assertEquals(50, filterCurriculum(original, "", EstadoAcademico.PENDIENTE, null).size)
        assertTrue(filterCurriculum(original, "inexistente", null, null).isEmpty())
    }
}

package com.example.roadmap_utepsa.domain.academic

import com.example.roadmap_utepsa.domain.model.AcademicData
import com.example.roadmap_utepsa.domain.model.EstadoAcademico

/** Valores derivados de una lectura completa; nunca persiste otro historial. */
data class ResumenAcademico(val aprobadas: Int, val enCurso: Int, val pendientes: Int) {
    val porcentaje: Int get() = aprobadas * 100 / TOTAL
    companion object {
        const val TOTAL = 50
        fun calcular(data: AcademicData): ResumenAcademico {
            val ids = data.materias.map { it.id }.toSet()
            require(data.materias.size == TOTAL && ids.size == TOTAL)
            require(data.materias.all { it.idPlan == data.idPlan })
            require(data.estados.keys.all { it in ids })
            val aprobadas = data.estados.values.count { it == EstadoAcademico.APROBADA }
            val curso = data.estados.values.count { it == EstadoAcademico.EN_CURSO }
            return ResumenAcademico(aprobadas, curso, TOTAL - aprobadas - curso)
        }
    }
}

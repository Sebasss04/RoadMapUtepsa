package com.example.roadmap_utepsa.domain.offer

import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia

/** null Oferta means a confirmed absent document; an empty set is a registered offer. */
data class Oferta(val idPlan: String, val materias: Set<String>, val revision: Long) {
    init { require(idPlan.isNotBlank() && revision in 1 until Long.MAX_VALUE && materias.size <= 50) }
}
data class DatosOferta(val uid: String, val idPlan: String, val catalogo: List<Materia>, val oferta: Oferta?,
    val estados: Map<String, EstadoAcademico> = emptyMap()) {
    val aprobadas get() = estados.filterValues { it == EstadoAcademico.APROBADA }.keys
    fun sinAprobadas(ids: Set<String>) = ids - aprobadas
}

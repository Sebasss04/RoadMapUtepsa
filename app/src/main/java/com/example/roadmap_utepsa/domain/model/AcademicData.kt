package com.example.roadmap_utepsa.domain.model

/** Lectura completa de un plan y los estados de una sola cuenta, sin datos de oferta. */
data class AcademicData(
    val uid: String,
    val idPlan: String,
    val materias: List<Materia>,
    val prerrequisitos: List<Prerrequisito>,
    val estados: Map<String, EstadoAcademico>
)

package com.example.roadmap_utepsa.domain.academic

import com.example.roadmap_utepsa.domain.model.EstadoAcademico

/** Explica una contradicción sin inferir fechas ni modificar estados. */
data class DiagnosticoMateriaHistorial(
    val idMateria: String,
    val estado: EstadoAcademico,
    val requisitosSinAprobar: Set<String>,
    val minAprobadas: Int,
    val aprobadasComputables: Int
) {
    val aprobadasFaltantes: Int
        get() = (minAprobadas - aprobadasComputables).coerceAtLeast(0)
}

/** El conjunto final debe ser coherente para confirmar preparación o recomendar. */
data class ResultadoCoherenciaHistorial(
    val contradicciones: List<DiagnosticoMateriaHistorial>
) {
    val coherente: Boolean
        get() = contradicciones.isEmpty()
}

/**
 * [propuesto] describe el mapa solicitado incluso si se rechaza la transición.
 * [bloqueos] explica el rechazo; [dependientesAfectados] incluye solo otras
 * materias activas cuya incoherencia se introduce o agrava al retirar aprobación.
 * Una transición permitida puede conservar contradicciones autodeclaradas.
 */
data class ResultadoTransicionHistorial(
    val permitida: Boolean,
    val anterior: ResultadoCoherenciaHistorial,
    val propuesto: ResultadoCoherenciaHistorial,
    val bloqueos: List<DiagnosticoMateriaHistorial>,
    val dependientesAfectados: List<DiagnosticoMateriaHistorial>
)

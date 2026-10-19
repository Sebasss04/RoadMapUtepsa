package com.example.roadmap_utepsa.domain.academic

/** Una relación: idRequisito debe estar aprobada para habilitar idObjetivo. */
data class Prerrequisito(val idObjetivo: String, val idRequisito: String) {
    init {
        require(idObjetivo.isNotBlank() && idRequisito.isNotBlank())
        require(idObjetivo != idRequisito) { "Una materia no puede requerirse a sí misma" }
    }
}

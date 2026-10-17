package com.example.roadmap_utepsa.domain.academic

/** Resultado calculado; los IDs permiten obtener nombres desde el mismo catálogo. */
data class ResultadoHabilitacion(
    val requisitosSinAprobar: Set<String>,
    val aprobadasFaltantes: Int
) {
    init {
        require(aprobadasFaltantes >= 0)
    }

    val habilitada: Boolean
        get() = requisitosSinAprobar.isEmpty() && aprobadasFaltantes == 0
}

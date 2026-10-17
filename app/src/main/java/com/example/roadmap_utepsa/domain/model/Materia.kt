package com.example.roadmap_utepsa.domain.model

/** El ID es estable y distinto del código académico visible. */
data class Materia(
    val id: String,
    val idPlan: String,
    val codigo: String,
    val nombre: String,
    val semestre: Int,
    val area: AreaMateria,
    val minAprobadas: Int = 0,
    val horasTotales: Int? = null
) {
    val duracion: DuracionMateria? get() = when (horasTotales) {
        85 -> DuracionMateria.MODULAR
        170 -> DuracionMateria.BIMODULAR
        400 -> DuracionMateria.ESPECIAL
        else -> null
    }

    init {
        require(id.isNotBlank() && idPlan.isNotBlank())
        require(codigo.isNotBlank() && nombre.isNotBlank())
        require(semestre in 1..9)
        require(minAprobadas >= 0)
        require(horasTotales == null || horasTotales in setOf(85, 170, 400))
        require(horasTotales != 400 || codigo == "STS-300")
    }
}

enum class DuracionMateria { MODULAR, BIMODULAR, ESPECIAL }

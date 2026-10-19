package com.example.roadmap_utepsa.presentation.shared.text
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.DuracionMateria

internal fun durationLabel(materia: Materia): Int = when (materia.duracion) {
    DuracionMateria.MODULAR -> R.string.subject_modular
    DuracionMateria.BIMODULAR -> R.string.subject_bimodular
    DuracionMateria.ESPECIAL -> R.string.subject_special
    null -> R.string.subject_duration_unavailable
}

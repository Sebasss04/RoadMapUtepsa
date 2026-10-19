package com.example.roadmap_utepsa.domain.academic

import java.text.Normalizer
import java.util.Locale

private fun String.searchKey() = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

fun filterCurriculum(data: AcademicData, query: String, state: EstadoAcademico?, semester: Int?): List<Materia> {
    val search = query.searchKey()
    return data.materias.filter { materia ->
        (semester == null || materia.semestre == semester) &&
            (state == null || (data.estados[materia.id] ?: EstadoAcademico.PENDIENTE) == state) &&
            (search.isEmpty() || materia.nombre.searchKey().contains(search) || materia.codigo.searchKey().contains(search))
    }.sortedWith(compareBy(Materia::semestre, Materia::codigo))
}

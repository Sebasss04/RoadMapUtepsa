package com.example.roadmap_utepsa.domain.academic

import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.Prerrequisito

/**
 * Evalúa un catálogo completo de un solo plan, sin Android ni persistencia.
 * Los estados pertenecen a un único estudiante y proceden de una lectura completa
 * y exitosa. Solo bajo esa condición, un ID ausente significa PENDIENTE.
 * La habilitación no decide si una materia está ofertada o es candidata.
 */
class ReglasAcademicas(materias: List<Materia>, prerrequisitos: List<Prerrequisito>) {
    private val materiasPorId = materias.associateBy { it.id }
    private val requisitosPorObjetivo = prerrequisitos.groupBy { it.idObjetivo }
        .mapValues { (_, relaciones) -> relaciones.map { it.idRequisito }.toSet() }

    init {
        require(materias.isNotEmpty()) { "El catálogo no puede estar vacío" }
        require(materiasPorId.size == materias.size) { "IDs de materias duplicados" }
        require(materias.map { it.idPlan }.distinct().size == 1) { "El catálogo mezcla planes" }
        require(materias.map { it.codigo }.distinct().size == materias.size) { "Códigos duplicados" }
        require(materias.all { it.minAprobadas < materias.size }) { "Umbral inalcanzable" }
        require(prerrequisitos.distinct().size == prerrequisitos.size) { "Relaciones duplicadas" }
        require(prerrequisitos.all {
            it.idObjetivo in materiasPorId && it.idRequisito in materiasPorId
        }) { "Prerrequisito con referencia inexistente" }

        val visitadas = mutableSetOf<String>()
        val enRecorrido = mutableSetOf<String>()
        fun visitar(id: String) {
            if (id in visitadas) return
            require(enRecorrido.add(id)) { "El catálogo contiene un ciclo" }
            requisitosPorObjetivo[id].orEmpty().forEach(::visitar)
            enRecorrido.remove(id)
            visitadas.add(id)
        }
        materiasPorId.keys.forEach(::visitar)
    }

    /** No modifica ni exige coherencia histórica de los estados autodeclarados. */
    fun evaluar(idMateria: String, estados: Map<String, EstadoAcademico>): ResultadoHabilitacion {
        val materia = requireNotNull(materiasPorId[idMateria]) { "Materia desconocida" }
        require(estados.keys.all { it in materiasPorId }) { "Estados de materias desconocidas" }
        val sinAprobar = requisitosPorObjetivo[idMateria].orEmpty().filterTo(linkedSetOf()) {
            estados[it] != EstadoAcademico.APROBADA
        }
        val aprobadas = estados.count { (id, estado) -> id != idMateria && estado == EstadoAcademico.APROBADA }
        return ResultadoHabilitacion(
            requisitosSinAprobar = sinAprobar,
            aprobadasFaltantes = (materia.minAprobadas - aprobadas).coerceAtLeast(0)
        )
    }
}

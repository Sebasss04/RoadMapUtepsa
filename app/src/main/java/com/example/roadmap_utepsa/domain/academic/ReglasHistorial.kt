package com.example.roadmap_utepsa.domain.academic

import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.Prerrequisito

/**
 * Coherencia de estados autodeclarados de un único estudiante, sin persistencia.
 * Requiere catálogo completo y lectura exitosa: un ID ausente es PENDIENTE.
 * No reconstruye orden histórico, aprueba requisitos ni cambia dependientes.
 * La selección de aprobadas puede hacerse en cualquier orden; [validar] revisa
 * el conjunto final antes de confirmar preparación o calcular recomendaciones.
 */
class ReglasHistorial(materias: List<Materia>, prerrequisitos: List<Prerrequisito>) {
    private val catalogo = materias.toList()
    private val materiasPorId = catalogo.associateBy { it.id }
    private val reglasAcademicas = ReglasAcademicas(catalogo, prerrequisitos.toList())

    fun validar(estados: Map<String, EstadoAcademico>): ResultadoCoherenciaHistorial {
        val copia = estados.toMap()
        exigirIdsConocidos(copia)
        return validarCopia(copia)
    }

    /**
     * APROBADA permite declarar el dato aunque requiera corregir el conjunto.
     * EN_CURSO exige habilitación propia. Al retirar APROBADA se bloquean solo
     * contradicciones nuevas o agravadas en otras materias activas, comparando
     * cada requisito y el déficit del umbral por separado.
     * Repetir el estado actual es un no-op permitido, aun con datos incoherentes.
     */
    fun evaluarTransicion(
        idMateria: String,
        nuevoEstado: EstadoAcademico,
        estados: Map<String, EstadoAcademico>
    ): ResultadoTransicionHistorial {
        require(idMateria in materiasPorId) { "Materia desconocida" }
        val copia = estados.toMap()
        exigirIdsConocidos(copia)
        val estadoAnterior = copia[idMateria] ?: EstadoAcademico.PENDIENTE
        val anterior = validarCopia(copia)
        if (estadoAnterior == nuevoEstado) {
            return ResultadoTransicionHistorial(true, anterior, anterior, emptyList(), emptyList())
        }

        val propuesto = validarCopia(copia + (idMateria to nuevoEstado))
        val dependientesAfectados = if (estadoAnterior == EstadoAcademico.APROBADA) {
            val anterioresPorId = anterior.contradicciones.associateBy { it.idMateria }
            propuesto.contradicciones.filter { diagnostico ->
                diagnostico.idMateria != idMateria &&
                    agrava(diagnostico, anterioresPorId[diagnostico.idMateria])
            }
        } else {
            emptyList()
        }
        val bloqueoDestino = if (nuevoEstado == EstadoAcademico.EN_CURSO) {
            propuesto.contradicciones.filter { it.idMateria == idMateria }
        } else {
            emptyList()
        }
        val bloqueos = bloqueoDestino + dependientesAfectados
        return ResultadoTransicionHistorial(
            permitida = bloqueos.isEmpty(),
            anterior = anterior,
            propuesto = propuesto,
            bloqueos = bloqueos,
            dependientesAfectados = dependientesAfectados
        )
    }

    private fun exigirIdsConocidos(estados: Map<String, EstadoAcademico>) {
        require(estados.keys.all { it in materiasPorId }) { "Estados de materias desconocidas" }
    }

    private fun validarCopia(estados: Map<String, EstadoAcademico>): ResultadoCoherenciaHistorial {
        val totalAprobadas = estados.values.count { it == EstadoAcademico.APROBADA }
        val contradicciones = catalogo.mapNotNull { materia ->
            val estado = estados[materia.id] ?: EstadoAcademico.PENDIENTE
            if (estado == EstadoAcademico.PENDIENTE) return@mapNotNull null

            // Una aprobación no puede satisfacer su propio umbral curricular.
            val estadosComputables = if (estado == EstadoAcademico.APROBADA) {
                estados - materia.id
            } else {
                estados
            }
            val habilitacion = reglasAcademicas.evaluar(materia.id, estadosComputables)
            if (habilitacion.requisitosSinAprobar.isEmpty() && habilitacion.aprobadasFaltantes == 0) {
                return@mapNotNull null
            }
            DiagnosticoMateriaHistorial(
                idMateria = materia.id,
                estado = estado,
                requisitosSinAprobar = habilitacion.requisitosSinAprobar.toSet(),
                minAprobadas = materia.minAprobadas,
                aprobadasComputables = totalAprobadas - if (estado == EstadoAcademico.APROBADA) 1 else 0
            )
        }
        return ResultadoCoherenciaHistorial(contradicciones)
    }

    private fun agrava(
        propuesto: DiagnosticoMateriaHistorial,
        anterior: DiagnosticoMateriaHistorial?
    ): Boolean =
        (propuesto.requisitosSinAprobar - anterior?.requisitosSinAprobar.orEmpty()).isNotEmpty() ||
            propuesto.aprobadasFaltantes > (anterior?.aprobadasFaltantes ?: 0)
}

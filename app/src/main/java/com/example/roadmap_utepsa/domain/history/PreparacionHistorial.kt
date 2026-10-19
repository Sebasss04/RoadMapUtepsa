package com.example.roadmap_utepsa.domain.history

import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.academic.ResultadoCoherenciaHistorial
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico

enum class DecisionPreparacion { POSPUESTO, CONFIRMADO }

data class CambioEstadoHistorial(
    val anterior: EstadoAcademico,
    val nuevo: EstadoAcademico
)

/** Diferencia con la lectura que originó el borrador; no resuelve el conflicto. */
data class DiferenciaHistorial(
    val idMateria: String,
    val base: EstadoAcademico,
    val remoto: EstadoAcademico,
    val seleccionado: EstadoAcademico
)

/**
 * Selección en memoria perteneciente a un único UID y una lectura completa exitosa.
 * Crear el borrador conserva los estados existentes. No escribe, confirma, mezcla
 * lecturas posteriores ni promete recuperación después de terminar el proceso.
 * El propietario de pantalla debe conservarlo durante su recreación y descartarlo
 * al cambiar de sesión; el UID permite comprobar esa pertenencia explícitamente.
 */
class BorradorHistorial(
    val uid: String,
    base: Map<String, EstadoAcademico>,
    seleccion: Map<String, EstadoAcademico> = base
) {
    private val baseInterna = base.toMap()
    private val seleccionInterna = seleccion.toMap()

    init {
        require(uid.isNotBlank()) { "El borrador requiere un UID" }
        require((baseInterna.keys + seleccionInterna.keys).all { it.isNotBlank() }) {
            "El borrador requiere IDs de materia no vacíos"
        }
    }

    // Copias: un consumidor que convierta Map a MutableMap no puede alterar el borrador.
    val base: Map<String, EstadoAcademico> get() = baseInterna.toMap()
    val seleccion: Map<String, EstadoAcademico> get() = seleccionInterna.toMap()

    /** La validación se aplica al conjunto final, permitiendo seleccionar sin orden. */
    fun seleccionar(idMateria: String, estado: EstadoAcademico): BorradorHistorial {
        require(idMateria.isNotBlank()) { "La materia requiere un ID" }
        return BorradorHistorial(uid, baseInterna, seleccionInterna + (idMateria to estado))
    }

    fun validar(reglas: ReglasHistorial): ResultadoCoherenciaHistorial =
        reglas.validar(seleccionInterna)

    /** Ausencia y PENDIENTE representan el mismo estado académico. */
    fun cambios(): Map<String, CambioEstadoHistorial> =
        (baseInterna.keys + seleccionInterna.keys).mapNotNull { id ->
            val anterior = baseInterna.estado(id)
            val nuevo = seleccionInterna.estado(id)
            if (anterior == nuevo) null else id to CambioEstadoHistorial(anterior, nuevo)
        }.toMap()

    /**
     * Compara todo el historial, incluso materias que el estudiante no editó.
     * Un cambio externo exige revisión antes de guardar; no hay rebase silencioso.
     */
    fun diferenciasRemotas(actuales: Map<String, EstadoAcademico>): List<DiferenciaHistorial> {
        val remoto = actuales.toMap()
        return (baseInterna.keys + remoto.keys).mapNotNull { id ->
            val inicial = baseInterna.estado(id)
            val actual = remoto.estado(id)
            if (inicial == actual) null else DiferenciaHistorial(
                idMateria = id,
                base = inicial,
                remoto = actual,
                seleccionado = seleccionInterna.estado(id)
            )
        }
    }

    private fun Map<String, EstadoAcademico>.estado(id: String): EstadoAcademico =
        this[id] ?: EstadoAcademico.PENDIENTE
}

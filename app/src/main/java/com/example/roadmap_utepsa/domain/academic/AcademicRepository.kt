package com.example.roadmap_utepsa.domain.academic

import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico

interface AcademicRepository {
    /** Solo devuelve datos completos confirmados por servidor, para el UID autenticado. */
    suspend fun load(uid: String): AcademicData

    /** Devuelve los estados actuales solo tras confirmar la escritura y releer servidor. */
    suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico>

    /** Compara la lectura revisada antes de guardar. Nunca degrada a escritura sin comparación. */
    suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico,
                          expectedStates: Map<String, EstadoAcademico>): Map<String, EstadoAcademico> =
        throw UnsupportedOperationException("El repositorio no implementa comparación de estados")
}

/** La comparación falló: conservar el borrador y revisar la lectura actual. */
class ConflictoEstadoAcademico(estadosActuales: Map<String, EstadoAcademico>) :
    IllegalStateException("El historial cambió; revisa las diferencias antes de guardar") {
    val estadosActuales: Map<String, EstadoAcademico> = estadosActuales.toMap()
}

package com.example.roadmap_utepsa.data.history

import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.OperacionHistorial

import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico

internal data class InstantaneaPreparacion(
    val estados: Map<String, EstadoAcademico>,
    val registro: RegistroPreparacion?,
    val operacion: OperacionHistorial?
)

internal data class EscrituraPreparacion(
    val estados: Map<String, EstadoAcademico> = emptyMap(),
    val decision: DecisionPreparacion? = null,
    val operacion: OperacionHistorial? = null
)

/** El bloque puede repetirse por contención; debe ser puro y validar el snapshot completo antes de escribir. */
internal interface PreparacionSource {
    val session: Any?
    val currentUid: String?
    suspend fun leer(uid: String, ids: Set<String>): InstantaneaPreparacion
    /** Devuelve solo la escritura cuyo commit terminó; no predice timestamps de servidor. */
    suspend fun transaccion(uid: String, ids: Set<String>,
                            bloque: (InstantaneaPreparacion) -> EscrituraPreparacion): EscrituraPreparacion
}

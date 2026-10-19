package com.example.roadmap_utepsa.domain.history

import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.CambioEstadoHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.academic.AcademicData

/** Revisión opaca del timestamp de servidor, conserva segundos y nanosegundos. */
data class RegistroPreparacion(val decision: DecisionPreparacion, val revision: String)

/** Diario recuperable de deltas; estados/{id} sigue siendo la fuente de verdad. */
data class OperacionHistorial(
    val id: String,
    val huellaBase: String,
    val huellaObjetivo: String,
    val revisionPreparacion: String?,
    val cambios: Map<String, CambioEstadoHistorial>,
    val aplicados: Set<String> = emptySet(),
    val completada: Boolean = false,
    val cancelada: Boolean = false
)

data class LecturaPreparacion(
    val academic: AcademicData,
    val registro: RegistroPreparacion?,
    val operacion: OperacionHistorial?,
    internal val sesionOrigen: Any? = null
)

/** Conflicto conserva la lectura remota para comparar; nunca cambia la base silenciosamente. */
class ConflictoHistorial(val remoto: LecturaPreparacion) : IllegalStateException("El historial cambió; revisa las diferencias")

interface PreparacionRepository {
    suspend fun load(uid: String): LecturaPreparacion
    suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion
    /** Cierra solo el diario tras revisión explícita; no revierte estados ya confirmados. */
    suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion
    suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial,
                          progreso: (Int, Int) -> Unit = { _, _ -> }): LecturaPreparacion
    /** Recupera una operación ya enviada; no promete persistir borradores no enviados. */
    suspend fun reanudar(uid: String, idOperacion: String,
                         progreso: (Int, Int) -> Unit = { _, _ -> }): LecturaPreparacion
}

package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.model.*

interface OfertaRepository {
    suspend fun load(uid: String): DatosOferta
    /** null revision requires absence. Returns only a confirmed server read. */
    suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta
}
class ConflictoOferta(val remota: Oferta?) : IllegalStateException("La oferta cambió en otra sesión")
class ResultadoOfertaIncierto(cause: Throwable) : IllegalStateException("Releer antes de reintentar", cause)

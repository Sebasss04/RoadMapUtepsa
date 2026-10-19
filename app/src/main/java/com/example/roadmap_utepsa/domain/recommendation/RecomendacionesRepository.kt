package com.example.roadmap_utepsa.domain.recommendation

import com.example.roadmap_utepsa.domain.history.LecturaPreparacion

import com.example.roadmap_utepsa.domain.offer.Oferta

data class DatosRecomendaciones(val preparacion: LecturaPreparacion, val oferta: Oferta?, val revisionAcademica: Long)

/** Contrato de consulta: no expone métodos de escritura. */
fun interface RecomendacionesRepository { suspend fun load(uid: String): DatosRecomendaciones }

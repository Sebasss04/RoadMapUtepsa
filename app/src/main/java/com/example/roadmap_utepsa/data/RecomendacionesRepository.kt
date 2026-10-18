package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.model.Oferta
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class DatosRecomendaciones(val preparacion: LecturaPreparacion, val oferta: Oferta?, val revisionAcademica: Long)

/** Contrato de consulta: no expone métodos de escritura. */
fun interface RecomendacionesRepository { suspend fun load(uid: String): DatosRecomendaciones }

class ConsultaRecomendacionesRepository(
    private val preparacion: PreparacionRepository, private val oferta: OfertaRepository,
    private val revision: suspend (String) -> Long, private val session: () -> Any?
) : RecomendacionesRepository {
    override suspend fun load(uid: String): DatosRecomendaciones {
        val origen = checkNotNull(session()) { "Sesión ausente" }
        fun comprobar() { check(session() === origen) { "La sesión cambió" } }
        repeat(3) {
            coroutineContext.ensureActive(); comprobar()
            val antes = revision(uid)
            val historial = preparacion.load(uid)
            val intermedia = revision(uid)
            comprobar(); coroutineContext.ensureActive()
            val seleccion = oferta.load(uid)
            val despues = revision(uid)
            comprobar(); coroutineContext.ensureActive()
            if (antes != intermedia || intermedia != despues) return@repeat
            check(historial.academic.uid == uid && seleccion.uid == uid)
            check(historial.academic.idPlan == seleccion.idPlan)
            check(historial.academic.materias.toSet() == seleccion.catalogo.toSet())
            return DatosRecomendaciones(historial, seleccion.oferta, despues)
        }
        error("El historial sigue cambiando; reintenta")
    }
}

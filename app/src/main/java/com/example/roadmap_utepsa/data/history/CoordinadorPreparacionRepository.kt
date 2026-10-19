package com.example.roadmap_utepsa.data.history

import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.history.OperacionHistorial
import com.example.roadmap_utepsa.domain.history.LecturaPreparacion
import com.example.roadmap_utepsa.domain.history.ConflictoHistorial
import com.example.roadmap_utepsa.domain.history.PreparacionRepository

import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.huellaHistorial
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico.PENDIENTE
import java.util.UUID

/** Coordinación comprobable sin SDK, usada por el adaptador productivo de preparación. */
internal class CoordinadorPreparacionRepository(
    private val academico: AcademicRepository,
    private val source: PreparacionSource,
    private val tamanoGrupo: Int
) : PreparacionRepository {
    init { require(tamanoGrupo in 1..5) }

    private fun sesion(uid: String, esperada: Any?) {
        check(uid.isNotBlank() && source.currentUid == uid && esperada != null && source.session === esperada) {
            "La sesión cambió"
        }
    }
    override suspend fun load(uid: String): LecturaPreparacion {
        val session = source.session
        sesion(uid, session)
        val academic = academico.load(uid)
        require(academic.uid == uid)
        sesion(uid, session)
        val lectura = source.leer(uid, academic.materias.map { it.id }.toSet())
        sesion(uid, session)
        return lectura.con(academic)
    }

    override suspend fun posponer(base: LecturaPreparacion): LecturaPreparacion {
        val session = base.sesionOrigen
        val uid = base.academic.uid
        val ids = base.ids()
        sesion(uid, session)
        // Consultar o abandonar una revisión nunca revoca una confirmación anterior.
        require(base.registro?.decision != DecisionPreparacion.CONFIRMADO) { "Un historial confirmado no se puede posponer" }
        source.transaccion(uid, ids) { actual ->
            sesion(uid, session)
            if (!mismaBase(base, actual) || actual.operacion.activa()) throw ConflictoHistorial(actual.con(base.academic))
            EscrituraPreparacion(decision = DecisionPreparacion.POSPUESTO)
        }
        sesion(uid, session)
        val resultado = source.leer(uid, ids).con(base.academic)
        sesion(uid, session)
        check(resultado.registro?.decision == DecisionPreparacion.POSPUESTO)
        if (huella(resultado.academic.estados, ids) != huella(base.academic.estados, ids) ||
            resultado.operacion != base.operacion || resultado.operacion.activa()) throw ConflictoHistorial(resultado)
        return resultado
    }

    override suspend fun confirmar(base: LecturaPreparacion, borrador: BorradorHistorial,
                                   progreso: (Int, Int) -> Unit): LecturaPreparacion {
        val uid = base.academic.uid
        require(uid == borrador.uid && normalizar(base.academic.estados) == normalizar(borrador.base))
        val ids = base.ids()
        require(borrador.seleccion.keys.all { it in ids })
        require(ReglasHistorial(base.academic.materias, base.academic.prerrequisitos).validar(borrador.seleccion).coherente) {
            "Resuelve las contradicciones antes de confirmar"
        }
        val session = base.sesionOrigen
        sesion(uid, session)
        val operacion = OperacionHistorial(UUID.randomUUID().toString(), huella(borrador.base, ids),
            huella(borrador.seleccion, ids), base.registro?.revision, borrador.cambios().toMap())
        progreso(0, operacion.cambios.size)
        source.transaccion(uid, ids) { actual ->
            sesion(uid, session)
            if (!mismaBase(base, actual) || actual.operacion.activa()) throw ConflictoHistorial(actual.con(base.academic))
            EscrituraPreparacion(operacion = operacion)
        }
        sesion(uid, session)
        return continuar(base.academic, operacion.id, session, progreso)
    }

    override suspend fun descartarOperacion(base: LecturaPreparacion): LecturaPreparacion {
        val uid = base.academic.uid
        val session = base.sesionOrigen
        sesion(uid, session)
        source.transaccion(uid, base.ids()) { actual ->
            sesion(uid, session)
            if (!mismaBase(base, actual) || !actual.operacion.activa()) throw ConflictoHistorial(actual.con(base.academic))
            EscrituraPreparacion(operacion = actual.operacion!!.copy(cancelada = true))
        }
        val resultado = source.leer(uid, base.ids()).con(base.academic)
        sesion(uid, session)
        check(resultado.operacion?.id == base.operacion?.id && resultado.operacion?.cancelada == true)
        if (normalizar(resultado.academic.estados) != normalizar(base.academic.estados)) throw ConflictoHistorial(resultado)
        return resultado
    }

    override suspend fun reanudar(uid: String, idOperacion: String, progreso: (Int, Int) -> Unit): LecturaPreparacion {
        val session = source.session
        val base = load(uid)
        sesion(uid, session)
        if (base.operacion?.id == idOperacion && base.operacion.completada) {
            verificarObjetivo(InstantaneaPreparacion(base.academic.estados, base.registro, base.operacion), base.academic)
            return base
        }
        return continuar(base.academic, idOperacion, session, progreso)
    }

    private suspend fun continuar(academic: AcademicData, idOperacion: String, session: Any?,
                                   progreso: (Int, Int) -> Unit): LecturaPreparacion {
        val uid = academic.uid
        val ids = academic.materias.map { it.id }.toSet()
        while (true) {
            sesion(uid, session)
            // La misma lectura transaccional valida y prepara el siguiente grupo.
            // No volver a leer los 50 estados fuera de ella entre commits.
            val confirmada = source.transaccion(uid, ids) { remoto ->
                sesion(uid, session)
                val operacion = requireNotNull(remoto.operacion) { "No hay operación para recuperar" }
                if (operacion.id != idOperacion || operacion.cancelada) throw ConflictoHistorial(remoto.con(academic))
                if (operacion.completada) {
                    verificarObjetivo(remoto, academic)
                    return@transaccion EscrituraPreparacion()
                }
                verificarEnCurso(remoto, academic)
                val pendientes = (operacion.cambios.keys - operacion.aplicados).sorted().take(tamanoGrupo)
                val cambios = pendientes.associateWith { operacion.cambios.getValue(it).nuevo }
                val aplicados = operacion.aplicados + pendientes
                val terminada = aplicados == operacion.cambios.keys
                if (terminada) {
                    val objetivo = remoto.estados + cambios
                    require(ReglasHistorial(academic.materias, academic.prerrequisitos).validar(objetivo).coherente)
                    require(huella(objetivo, ids) == operacion.huellaObjetivo)
                }
                EscrituraPreparacion(estados = cambios,
                    decision = DecisionPreparacion.CONFIRMADO.takeIf { terminada },
                    operacion = operacion.copy(aplicados = aplicados, completada = terminada))
            }
            sesion(uid, session)
            val operacion = confirmada.operacion
            if (operacion != null) progreso(operacion.aplicados.size, operacion.cambios.size)
            if (operacion == null || operacion.completada) {
                // El último grupo confirma marcador y diario atómicamente. Aun así,
                // solo una relectura confirmada permite comunicar éxito al usuario.
                val final = source.leer(uid, ids)
                sesion(uid, session)
                val operacionFinal = final.operacion
                if (operacionFinal == null || operacionFinal.id != idOperacion || !operacionFinal.completada)
                    throw ConflictoHistorial(final.con(academic))
                verificarObjetivo(final, academic)
                return final.con(academic)
            }
        }
    }

    private fun verificarEnCurso(actual: InstantaneaPreparacion, academic: AcademicData) {
        val operacion = requireNotNull(actual.operacion)
        val ids = academic.materias.map { it.id }.toSet()
        require(operacion.cambios.keys.all { it in ids } && operacion.aplicados.all { it in operacion.cambios })
        if (actual.registro?.revision != operacion.revisionPreparacion) throw ConflictoHistorial(actual.con(academic))
        val reconstruida = actual.estados.toMutableMap()
        for ((id, cambio) in operacion.cambios) {
            val esperado = if (id in operacion.aplicados) cambio.nuevo else cambio.anterior
            // Verificar ANTES de reconstruir evita ocultar una edición de una materia ya aplicada.
            if ((actual.estados[id] ?: PENDIENTE) != esperado) throw ConflictoHistorial(actual.con(academic))
            reconstruida[id] = cambio.anterior
        }
        if (huella(reconstruida, ids) != operacion.huellaBase) throw ConflictoHistorial(actual.con(academic))
        val objetivo = reconstruida + operacion.cambios.mapValues { it.value.nuevo }
        require(huella(objetivo, ids) == operacion.huellaObjetivo)
        require(ReglasHistorial(academic.materias, academic.prerrequisitos).validar(objetivo).coherente)
    }

    private fun verificarObjetivo(actual: InstantaneaPreparacion, academic: AcademicData) {
        if (actual.registro?.decision != DecisionPreparacion.CONFIRMADO ||
            huella(actual.estados, academic.materias.map { it.id }.toSet()) != actual.operacion?.huellaObjetivo ||
            !ReglasHistorial(academic.materias, academic.prerrequisitos).validar(actual.estados).coherente) {
            throw ConflictoHistorial(actual.con(academic))
        }
    }

    private fun mismaBase(base: LecturaPreparacion, actual: InstantaneaPreparacion) =
        normalizar(base.academic.estados) == normalizar(actual.estados) && base.registro == actual.registro &&
            base.operacion == actual.operacion

    private fun InstantaneaPreparacion.con(academic: AcademicData) =
        LecturaPreparacion(academic.copy(estados = estados.toMap()), registro, operacion, source.session)
    private fun LecturaPreparacion.ids() = academic.materias.map { it.id }.toSet()
    private fun OperacionHistorial?.activa() = this != null && !completada && !cancelada

    companion object {
        internal fun normalizar(estados: Map<String, EstadoAcademico>) = estados.filterValues { it != PENDIENTE }
        internal fun huella(estados: Map<String, EstadoAcademico>, ids: Set<String>) = huellaHistorial(estados, ids)
    }
}

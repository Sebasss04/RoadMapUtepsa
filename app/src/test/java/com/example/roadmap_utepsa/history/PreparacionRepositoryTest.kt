package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.data.history.CoordinadorPreparacionRepository
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.OperacionHistorial
import com.example.roadmap_utepsa.domain.history.ConflictoHistorial
import com.example.roadmap_utepsa.data.history.InstantaneaPreparacion
import com.example.roadmap_utepsa.data.history.EscrituraPreparacion
import com.example.roadmap_utepsa.data.history.PreparacionSource
import com.example.roadmap_utepsa.domain.history.huellaHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.CambioEstadoHistorial
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Fuente transaccional simulada: estas pruebas no acreditan persistencia Firebase. */
class PreparacionRepositoryTest {
    private val catalogo = CatalogoHistorialFixture()
    private val ids = catalogo.materias.map { it.id }.toSet()
    private val libre = catalogo.materias.first { m -> catalogo.prerrequisitos.none { it.idObjetivo == m.id } }.id
    private val otroLibre = catalogo.materias.first { m -> m.id != libre && catalogo.prerrequisitos.none { it.idObjetivo == m.id } }.id
    private val proyecto = catalogo.id("SIS-306")

    private class Source : PreparacionSource {
        override var session: Any? = Any()
        override var currentUid: String? = "alice"
        var actual = InstantaneaPreparacion(emptyMap(), null, null)
        var escrituras = 0
        var lecturas = 0
        val grupos = mutableListOf<Set<String>>()
        var before: () -> Unit = {}
        var after: () -> Unit = {}
        var read: () -> Unit = {}
        override suspend fun leer(uid: String, ids: Set<String>): InstantaneaPreparacion {
            lecturas++
            read()
            return actual.copy(estados = actual.estados.toMap())
        }
        override suspend fun transaccion(uid: String, ids: Set<String>, bloque: (InstantaneaPreparacion) -> EscrituraPreparacion): EscrituraPreparacion {
            before()
            val escritura = bloque(actual.copy(estados = actual.estados.toMap()))
            escrituras++
            val estados = (actual.estados + escritura.estados).filterValues { it != PENDIENTE }
            if (escritura.estados.isNotEmpty()) grupos += escritura.estados.keys.toSet()
            actual = InstantaneaPreparacion(estados,
                escritura.decision?.let { RegistroPreparacion(it, "revision-$escrituras") } ?: actual.registro,
                escritura.operacion ?: actual.operacion)
            after()
            return escritura
        }
    }
    private inner class Equipo {
        val source = Source()
        val academico = object : AcademicRepository {
            override suspend fun load(uid: String) = AcademicData(uid, "sistemas-rm-615-2016",
                catalogo.materias, catalogo.prerrequisitos, source.actual.estados)
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> = error("No debe usar guardado individual")
        }
        val repo = CoordinadorPreparacionRepository(academico, source, 5)
        suspend fun base() = repo.load("alice")
    }

    @Test fun ausenciaDePreparacionNoBorraEstadosExistentes() = runTest {
        val e = Equipo()
        e.source.actual = e.source.actual.copy(estados = mapOf(libre to APROBADA))
        val base = e.base()
        assertNull(base.registro)
        assertEquals(APROBADA, base.academic.estados[libre])
        assertEquals(0, e.source.escrituras)
    }

    @Test fun confirmadoNoPuedeVolverAPospuesto() = runTest {
        val e = Equipo()
        e.source.actual = e.source.actual.copy(estados = mapOf(libre to APROBADA),
            registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "previous"))
        val base = e.base()
        try { e.repo.posponer(base); fail("No debe degradar la preparación") }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, e.source.escrituras)
        assertEquals(base.registro, e.source.actual.registro)
        assertEquals(base.academic.estados, e.source.actual.estados)
    }

    @Test fun posponerSoloGuardaDecision() = runTest {
        val e = Equipo()
        e.source.actual = e.source.actual.copy(estados = mapOf(libre to APROBADA))
        val guardado = e.repo.posponer(e.base())
        assertEquals(DecisionPreparacion.POSPUESTO, guardado.registro!!.decision)
        assertEquals(mapOf(libre to APROBADA), guardado.academic.estados)
        assertNull(guardado.operacion)
    }

    @Test fun operacionConcurrenteTrasPosponerNoSeAnunciaComoExito() = runTest {
        val e = Equipo()
        e.source.actual = e.source.actual.copy(estados = mapOf(libre to APROBADA))
        val base = e.base()
        val estados = base.academic.estados
        e.source.after = {
            val objetivo = estados + (otroLibre to APROBADA)
            val operacion = OperacionHistorial("otra-operacion", huellaHistorial(estados, ids),
                huellaHistorial(objetivo, ids), e.source.actual.registro!!.revision,
                mapOf(otroLibre to CambioEstadoHistorial(PENDIENTE, APROBADA)))
            e.source.actual = e.source.actual.copy(operacion = operacion)
        }
        try {
            e.repo.posponer(base)
            fail("La operación concurrente debe exigir revisión")
        } catch (conflicto: ConflictoHistorial) {
            assertEquals("otra-operacion", conflicto.remoto.operacion!!.id)
            assertFalse(conflicto.remoto.operacion.completada)
            assertEquals(DecisionPreparacion.POSPUESTO, conflicto.remoto.registro!!.decision)
            assertEquals(estados, conflicto.remoto.academic.estados)
        }
        assertEquals(1, e.source.escrituras)
        assertEquals(estados, e.source.actual.estados)
        assertEquals("otra-operacion", e.source.actual.operacion!!.id)
    }

    @Test fun ceroAprobadasSeConfirmaConRelectura() = runTest {
        val e = Equipo()
        val base = e.base()
        var lecturas = 0
        e.source.read = { lecturas++ }
        val r = e.repo.confirmar(base, BorradorHistorial("alice", base.academic.estados))
        assertTrue(r.operacion!!.completada)
        assertEquals(DecisionPreparacion.CONFIRMADO, r.registro!!.decision)
        assertTrue(r.academic.estados.isEmpty())
        assertEquals(1, lecturas) // Una sola relectura final, también con cero cambios.
    }

    @Test fun cincuentaCambiosSeAplicanEnGruposSinDuplicarYConfirmanAlFinal() = runTest {
        val e = Equipo()
        val base = e.base()
        val objetivo = ids.associateWith { APROBADA }
        val progreso = mutableListOf<Pair<Int, Int>>()
        val r = e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), objetivo)) { a, b -> progreso += a to b }
        assertEquals(objetivo, r.academic.estados)
        assertEquals(List(10) { 5 }, e.source.grupos.map { it.size })
        assertEquals(50, e.source.grupos.flatten().distinct().size)
        assertEquals(ids, r.operacion!!.aplicados)
        assertEquals(50 to 50, progreso.last())
        assertEquals(11, e.source.escrituras) // inicio + diez grupos; el último confirma.
        assertEquals(2, e.source.lecturas) // carga inicial + resultado; ninguna entre grupos.
    }

    @Test fun conjuntoIncoherenteNoIniciaOperacion() = runTest {
        val e = Equipo()
        val base = e.base()
        try {
            e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), mapOf(proyecto to APROBADA)))
            fail()
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, e.source.escrituras)
    }

    @Test fun borradorDeOtroUidNoSeEnvia() = runTest {
        val e = Equipo()
        try { e.repo.confirmar(e.base(), BorradorHistorial("bob", emptyMap())); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, e.source.escrituras)
    }

    @Test fun cambioRemotoFueraDeSeleccionBloqueaAntesDeEnviar() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.actual = e.source.actual.copy(estados = mapOf(otroLibre to APROBADA))
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), mapOf(libre to APROBADA))); fail() }
        catch (c: ConflictoHistorial) { assertEquals(APROBADA, c.remoto.academic.estados[otroLibre]) }
        assertEquals(0, e.source.escrituras)
    }

    @Test fun cambioConcurrenteDeDecisionNoSeSobrescribe() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.actual = e.source.actual.copy(registro = RegistroPreparacion(DecisionPreparacion.CONFIRMADO, "remota"))
        try { e.repo.posponer(base); fail() } catch (_: ConflictoHistorial) { }
        assertEquals(0, e.source.escrituras)
    }

    @Test fun perdidaDeRespuestaTrasGrupoSeRecuperaSinReaplicar() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.after = { if (e.source.escrituras == 2) error("Confirmación perdida") }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), ids.associateWith { APROBADA })); fail() }
        catch (_: IllegalStateException) { }
        val parcial = e.base()
        assertNull(parcial.registro)
        assertEquals(5, parcial.operacion!!.aplicados.size)
        assertEquals(5, parcial.academic.estados.size)
        e.source.after = {}
        // Otra instancia simula reapertura: toda recuperación proviene del diario y servidor simulados.
        val nueva = CoordinadorPreparacionRepository(e.academico, e.source, 5)
        val final = nueva.reanudar("alice", parcial.operacion.id)
        assertEquals(50, final.academic.estados.size)
        assertEquals(50, e.source.grupos.flatten().size)
        assertTrue(final.operacion!!.completada)
    }

    @Test fun errorAntesDeCommitConservaDiarioSinDarExito() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.before = { if (e.source.escrituras == 1) error("Sin red") }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), mapOf(libre to APROBADA))); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(0, e.source.actual.operacion!!.aplicados.size)
        assertTrue(e.source.actual.estados.isEmpty())
        assertNull(e.source.actual.registro)
    }

    @Test fun otraEdicionSobreMateriaYaAplicadaNoSeOcultaAlReconstruirBase() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.after = {
            if (e.source.escrituras == 2) {
                val id = e.source.actual.operacion!!.aplicados.first()
                e.source.actual = e.source.actual.copy(estados = e.source.actual.estados + (id to EN_CURSO))
            }
        }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), ids.associateWith { APROBADA })); fail() }
        catch (_: ConflictoHistorial) { }
        assertNull(e.source.actual.registro)
        assertEquals(2, e.source.escrituras)
        assertEquals(1, e.source.actual.estados.values.count { it == EN_CURSO })
    }

    @Test fun otraEdicionSobreMateriaNoAplicadaBloqueaSiguienteGrupo() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.after = {
            if (e.source.escrituras == 2) {
                val id = (ids - e.source.actual.operacion!!.aplicados).first()
                e.source.actual = e.source.actual.copy(estados = e.source.actual.estados + (id to EN_CURSO))
            }
        }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), ids.associateWith { APROBADA })); fail() }
        catch (_: ConflictoHistorial) { }
        assertEquals(2, e.source.escrituras)
        assertNull(e.source.actual.registro)
    }

    @Test fun journalActivoImpideSegundoBorradorYPosponer() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.before = { if (e.source.escrituras == 1) error("Pausa simulada") }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap())); fail() } catch (_: IllegalStateException) { }
        e.source.before = {}
        val parcial = e.base()
        try { e.repo.posponer(parcial); fail() } catch (_: ConflictoHistorial) { }
        try { e.repo.confirmar(parcial, BorradorHistorial("alice", emptyMap())); fail() } catch (_: ConflictoHistorial) { }
        assertEquals(1, e.source.escrituras)
    }

    @Test fun descarteDeOperacionTrasRevisionConservaTodosLosEstadosYPermiteNuevoBorrador() = runTest {
        val e = Equipo()
        val base = e.base()
        val objetivo = catalogo.materias.filter { m -> m.minAprobadas == 0 &&
            catalogo.prerrequisitos.none { it.idObjetivo == m.id } }.take(6).associate { it.id to APROBADA }
        e.source.after = { if (e.source.escrituras == 2) error("Respuesta perdida") }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), objetivo)); fail() }
        catch (_: IllegalStateException) { }
        e.source.after = {}
        val parcial = e.base()
        val descartada = e.repo.descartarOperacion(parcial)
        assertEquals(parcial.academic.estados, descartada.academic.estados)
        assertTrue(descartada.operacion!!.cancelada)
        val r = e.repo.confirmar(descartada, BorradorHistorial("alice", descartada.academic.estados))
        assertEquals(parcial.academic.estados, r.academic.estados)
        assertTrue(r.operacion!!.completada)
    }

    @Test fun cambioDeSesionTrasCommitNoExponeExitoNiContinua() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.after = { e.source.session = Any() }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap())); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(1, e.source.escrituras)
        assertNull(e.source.actual.registro)
    }

    @Test fun baseDeSesionAnteriorDelMismoUidNoAutorizaNuevoEnvio() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.session = Any()
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap())); fail() }
        catch (_: IllegalStateException) { }
        try { e.repo.posponer(base); fail() } catch (_: IllegalStateException) { }
        assertEquals(0, e.source.escrituras)
    }

    @Test fun lecturaFallidaNoEquivaleAPreparacionAusente() = runTest {
        val e = Equipo()
        e.source.read = { error("Sin confirmación del servidor") }
        try { e.base(); fail() } catch (_: IllegalStateException) { }
    }

    @Test fun operacionTerminadaEsIdempotenteYDetectaEdicionPosterior() = runTest {
        val e = Equipo()
        val base = e.base()
        val fin = e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), mapOf(libre to APROBADA)))
        val n = e.source.escrituras
        e.repo.reanudar("alice", fin.operacion!!.id)
        assertEquals(n, e.source.escrituras)
        e.source.actual = e.source.actual.copy(estados = mapOf(libre to EN_CURSO))
        try { e.repo.reanudar("alice", fin.operacion.id); fail() } catch (_: ConflictoHistorial) { }
    }

    @Test fun respuestaPerdidaDelUltimoGrupoRecuperaConfirmacionSinReescribir() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.after = { if (e.source.escrituras == 2) error("Respuesta perdida al finalizar") }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), mapOf(libre to APROBADA))); fail() }
        catch (_: IllegalStateException) { }
        assertTrue(e.source.actual.operacion!!.completada)
        assertEquals(DecisionPreparacion.CONFIRMADO, e.source.actual.registro?.decision)
        e.source.after = {}
        val final = e.repo.reanudar("alice", e.source.actual.operacion!!.id)
        assertEquals(mapOf(libre to APROBADA), final.academic.estados)
        assertEquals(2, e.source.escrituras)
    }

    @Test fun edicionAjenaDespuesDelUltimoCommitImpideAnunciarExito() = runTest {
        val e = Equipo()
        val base = e.base()
        e.source.after = {
            if (e.source.escrituras == 2) e.source.actual = e.source.actual.copy(
                estados = e.source.actual.estados + (otroLibre to EN_CURSO))
        }
        try { e.repo.confirmar(base, BorradorHistorial("alice", emptyMap(), mapOf(libre to APROBADA))); fail() }
        catch (c: ConflictoHistorial) { assertEquals(EN_CURSO, c.remoto.academic.estados[otroLibre]) }
    }
}

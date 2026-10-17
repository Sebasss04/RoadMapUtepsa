package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.domain.model.EstadoAcademico.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** CAS simulado: no ejecuta SDK, emulador Firestore ni Firebase real. */
class AcademicStateConcurrencyTest {
    private val fixture = AcademicFixtures()
    private val libre = fixture.documents[28].id
    private val otra = fixture.documents[29].id
    private val proyecto = fixture.documents[49].id
    private val calidad = fixture.documents[46].id
    private val datos = fixture.documents[47].id

    private inner class CasSource(val lectura: AcademicFixtures.Source) : AcademicReadSource by lectura {
        var identidad: Any = Any()
        override val session: Any get() = identidad
        var intentos = 0
        var commits = 0
        var beforeCommit: () -> Unit = {}
        var afterCommit: () -> Unit = {}
        fun estadosActuales() = lectura.states.associate {
            it.id to EstadoAcademico.valueOf(it.fields.getValue("estado") as String)
        }
        fun poner(estados: Map<String, EstadoAcademico>) {
            lectura.states = estados.filterValues { it != PENDIENTE }.map { (id, estado) ->
                AcademicDocument(id, mapOf("estado" to estado.name))
            }
        }
        override suspend fun saveStateComparado(uid: String, idMateria: String, estado: EstadoAcademico,
                                               ids: Set<String>, expectedStates: Map<String, EstadoAcademico>) {
            intentos++
            assertEquals(50, ids.size)
            val origen = identidad
            beforeCommit()
            check(currentUid == uid && identidad === origen)
            val actuales = estadosActuales()
            if (actuales != expectedStates.filterValues { it != PENDIENTE }) throw ConflictoEstadoAcademico(actuales)
            poner(actuales + (idMateria to estado))
            commits++
            afterCommit()
            check(currentUid == uid && identidad === origen)
        }
    }
    private inner class Equipo {
        val lectura = fixture.Source()
        val source = CasSource(lectura)
        val repo = FirestoreAcademicRepository(source, AcademicFixtures.Profile())
    }

    @Test fun baseAntiguaBloqueaAntesDeTransaccionSinSobrescribir() = runTest {
        val e = Equipo()
        e.source.poner(mapOf(otra to APROBADA))
        try { e.repo.saveState("alice", libre, EN_CURSO, emptyMap()); fail() }
        catch (c: ConflictoEstadoAcademico) { assertEquals(mapOf(otra to APROBADA), c.estadosActuales) }
        assertEquals(0, e.source.intentos)
        assertEquals(0, e.source.commits)
    }

    @Test fun cambioConcurrenteDelObjetivoAntesDelCommitSeConserva() = runTest {
        val e = Equipo()
        e.source.beforeCommit = { e.source.poner(mapOf(libre to APROBADA)) }
        try { e.repo.saveState("alice", libre, EN_CURSO, emptyMap()); fail() }
        catch (_: ConflictoEstadoAcademico) { }
        assertEquals(mapOf(libre to APROBADA), e.source.estadosActuales())
        assertEquals(1, e.source.intentos)
        assertEquals(0, e.source.commits)
        assertEquals(0, e.lectura.writes)
    }

    @Test fun cambioConcurrenteAjenoTambienBloqueaElCommit() = runTest {
        val e = Equipo()
        e.source.beforeCommit = { e.source.poner(mapOf(otra to APROBADA)) }
        try { e.repo.saveState("alice", libre, APROBADA, emptyMap()); fail() }
        catch (_: ConflictoEstadoAcademico) { }
        assertEquals(mapOf(otra to APROBADA), e.source.estadosActuales())
        assertEquals(0, e.source.commits)
    }

    @Test fun enCursoConRequisitosFaltantesNoLlegaALaFuenteDeEscritura() = runTest {
        val e = Equipo()
        e.source.poner(mapOf(datos to APROBADA))
        try { e.repo.saveState("alice", proyecto, EN_CURSO, mapOf(datos to APROBADA)); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, e.source.intentos)
        assertEquals(mapOf(datos to APROBADA), e.source.estadosActuales())
    }

    @Test fun aprobadaAutodeclaradaSeGuardaSinAprobarRequisitosAutomaticamente() = runTest {
        val e = Equipo()
        val guardados = e.repo.saveState("alice", proyecto, APROBADA, emptyMap())
        assertEquals(mapOf(proyecto to APROBADA), guardados)
        assertEquals(1, e.source.commits)
        assertEquals(0, e.lectura.writes)
    }

    @Test fun retiradaQueRompeDependienteNoLlegaATransaccion() = runTest {
        val e = Equipo()
        val base = mapOf(proyecto to APROBADA, calidad to APROBADA, datos to APROBADA)
        e.source.poner(base)
        try { e.repo.saveState("alice", datos, PENDIENTE, base); fail() }
        catch (_: IllegalArgumentException) { }
        assertEquals(0, e.source.intentos)
        assertEquals(base, e.source.estadosActuales())
    }

    @Test fun pendienteExplicitoEnBaseEquivaleAAusencia() = runTest {
        val e = Equipo()
        assertEquals(mapOf(libre to EN_CURSO),
            e.repo.saveState("alice", libre, EN_CURSO, mapOf(libre to PENDIENTE)))
        assertEquals(1, e.source.commits)
    }

    @Test fun reloginMismoUidDuranteLecturaRechazaElGuardado() = runTest {
        val e = Equipo()
        e.lectura.beforeRead = { if (it == "estados") e.source.identidad = Any() }
        try { e.repo.saveState("alice", libre, APROBADA, emptyMap()); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(0, e.source.intentos)
    }

    @Test fun reloginDuranteCommitSimuladoImpideEscritura() = runTest {
        val e = Equipo()
        e.source.beforeCommit = { e.source.identidad = Any() }
        try { e.repo.saveState("alice", libre, APROBADA, emptyMap()); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(0, e.source.commits)
        assertTrue(e.source.estadosActuales().isEmpty())
    }

    @Test fun lecturaFinalPendienteNoAnunciaExitoNiRevierteCommit() = runTest {
        val e = Equipo()
        e.source.afterCommit = {
            e.lectura.response = { stage, docs -> AcademicDocuments(docs, pendingWrites = stage == "estados") }
        }
        try { e.repo.saveState("alice", libre, APROBADA, emptyMap()); fail() }
        catch (_: IllegalStateException) { }
        assertEquals(1, e.source.commits)
        assertEquals(mapOf(libre to APROBADA), e.source.estadosActuales())
    }

    @Test fun fuenteSinCasNoDegradaSilenciosamenteAEscrituraIndividual() = runTest {
        val lectura = fixture.Source()
        val repo = FirestoreAcademicRepository(lectura, AcademicFixtures.Profile())
        try { repo.saveState("alice", libre, APROBADA, emptyMap()); fail() }
        catch (_: UnsupportedOperationException) { }
        assertEquals(0, lectura.writes)
    }

    @Test fun repositorioSinCasNoDegradaSilenciosamenteAEscrituraIndividual() = runTest {
        var escrituras = 0
        val repo = object : AcademicRepository {
            override suspend fun load(uid: String) = fixture.data(uid)
            override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> {
                escrituras++
                return emptyMap()
            }
        }
        try { repo.saveState("alice", libre, APROBADA, emptyMap()); fail() }
        catch (_: UnsupportedOperationException) { }
        assertEquals(0, escrituras)
    }

    @Test fun restauracionComparadaEliminaSoloConBaseSinCambios() = runTest {
        val e = Equipo()
        val base = mapOf(libre to EN_CURSO)
        e.source.poner(base)
        assertTrue(e.repo.saveState("alice", libre, PENDIENTE, base).isEmpty())
        assertEquals(1, e.source.commits)
        assertTrue(e.source.estadosActuales().isEmpty())
    }

    @Test fun restauracionComparadaConCambioAjenoNoBorraObjetivo() = runTest {
        val e = Equipo()
        val base = mapOf(libre to EN_CURSO)
        e.source.poner(base)
        e.source.beforeCommit = { e.source.poner(base + (otra to APROBADA)) }
        try { e.repo.saveState("alice", libre, PENDIENTE, base); fail() }
        catch (_: ConflictoEstadoAcademico) { }
        assertEquals(base + (otra to APROBADA), e.source.estadosActuales())
        assertEquals(0, e.source.commits)
    }
}

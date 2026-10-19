package com.example.roadmap_utepsa.academic

import com.example.roadmap_utepsa.history.CatalogoHistorialFixture

import com.example.roadmap_utepsa.data.academic.AcademicDocument
import com.example.roadmap_utepsa.data.academic.AcademicDocuments
import com.example.roadmap_utepsa.data.academic.AcademicReadSource
import com.example.roadmap_utepsa.data.academic.FirestoreAcademicRepository
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.DuracionMateria
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FirestoreAcademicRepositoryTest {
    private val fixture = AcademicFixtures()
    private val source = fixture.Source()
    private val profile = AcademicFixtures.Profile()
    private val repository = FirestoreAcademicRepository(source, profile)
    private suspend fun fails(block: suspend () -> Unit) {
        try { block(); fail("Se esperaba error") }
        catch (e: Exception) { if (e is CancellationException) throw e }
    }

    private fun saeDocuments(): List<AcademicDocument> = CatalogoHistorialFixture().materias.map { m ->
        AcademicDocument(m.id, mapOf("id_plan" to m.idPlan, "codigo" to m.codigo, "nombre" to m.nombre,
            "semestre" to m.semestre.toLong(), "area" to m.area.name, "min_aprobadas" to m.minAprobadas.toLong(),
            "horas_totales" to m.horasTotales!!.toLong()))
    }

    @Test fun catalogoSaeConservaEstadosYClasificaLasCincuentaMaterias() = runTest {
        val catalogo = CatalogoHistorialFixture()
        source.subjects = saeDocuments()
        source.relations = catalogo.materias.associate { m -> m.id to catalogo.prerrequisitos
            .filter { it.idObjetivo == m.id }.map { AcademicDocument(it.idRequisito, emptyMap()) } }
        val id = catalogo.id("SDA-303")
        source.states = listOf(AcademicDocument(id, mapOf("estado" to "APROBADA")))
        val data = repository.load("alice")
        assertEquals(28, data.prerrequisitos.size)
        assertEquals(40, data.materias.count { it.duracion == DuracionMateria.MODULAR })
        assertEquals(9, data.materias.count { it.duracion == DuracionMateria.BIMODULAR })
        assertEquals(1, data.materias.count { it.duracion == DuracionMateria.ESPECIAL })
        assertEquals(mapOf(id to EstadoAcademico.APROBADA), data.estados)
        val diagnostico = com.example.roadmap_utepsa.domain.academic.ReglasHistorial(data.materias, data.prerrequisitos)
            .validar(data.estados).contradicciones.single()
        assertEquals(setOf(catalogo.id("SIS-302")), diagnostico.requisitosSinAprobar)
        assertEquals(0, source.writes)
    }

    @Test fun rechazaCatalogosMezcladosHorasInvalidasYUmbralAntiguoEnSae() = runTest {
        val docs = saeDocuments()
        for (fields in listOf(docs.first().fields - "horas_totales",
            docs.first().fields + ("horas_totales" to 85.0),
            docs.first().fields + ("horas_totales" to 400L),
            docs.first().fields + ("horas_totales" to 999L))) {
            source.subjects = listOf(docs.first().copy(fields = fields)) + docs.drop(1)
            fails { repository.load("alice") }
        }
        source.subjects = docs.map { if (it.fields["codigo"] == "STS-300") it.copy(fields = it.fields + ("min_aprobadas" to 40L)) else it }
        fails { repository.load("alice") }
    }

    @Test fun completeReadIncludesEmptyRelationsAndNoInventedStates() = runTest {
        val data = repository.load("alice")
        assertEquals("alice", data.uid)
        assertEquals(fixture.plan, data.idPlan)
        assertEquals(50, data.materias.size)
        assertEquals(29, data.prerrequisitos.size)
        assertTrue(data.estados.isEmpty())
        assertEquals(52, source.calls)
        assertEquals(1, profile.calls)
    }

    @Test fun reutilizaCatalogoValidadoPeroSiempreReleeEstadosYPerfil() = runTest {
        val first = repository.load("alice")
        val id = fixture.documents.first().id
        source.states = listOf(AcademicDocument(id, mapOf("estado" to "APROBADA")))
        val second = repository.load("alice")
        assertEquals(first.materias, second.materias)
        assertEquals(EstadoAcademico.APROBADA, second.estados[id])
        assertEquals(53, source.calls)
        assertEquals(2, profile.calls)
        source.beforeRead = { error("Sin conexión") }
        fails { repository.load("alice") }
    }

    @Test fun catalogoExpiraYNoSeComparteEntreSesionesDeLaMismaCuenta() = runTest {
        var now = 0L
        var identidad: Any = Any()
        val keyed = object : AcademicReadSource by source { override val session: Any get() = identidad }
        val cached = FirestoreAcademicRepository(keyed, profile) { now }
        cached.load("alice")
        cached.load("alice")
        assertEquals(53, source.calls)
        now = 5L * 60 * 1_000_000_000
        cached.load("alice")
        assertEquals(105, source.calls)
        identidad = Any()
        cached.load("alice")
        assertEquals(157, source.calls)
    }

    @Test fun falloDeCatalogoNuncaSePublicaEnCache() = runTest {
        source.relations = emptyMap()
        fails { repository.load("alice") }
        source.relations = fixture.requirements
        assertEquals(50, repository.load("alice").materias.size)
    }

    @Test fun readsOnlyDeclaredStatesAndKeepsStableIds() = runTest {
        source.states = listOf(AcademicDocument(fixture.documents[49].id, mapOf("estado" to "APROBADA")))
        val data = repository.load("alice")
        assertEquals(mapOf(fixture.documents[49].id to EstadoAcademico.APROBADA), data.estados)
        assertEquals(fixture.documents[49].id, data.materias.single { it.codigo == "SIS-306" }.id)
    }

    @Test fun refusesWrongOrAbsentIdentityBeforeReading() = runTest {
        for (uid in listOf(null, "bob")) {
            source.currentUid = uid
            fails { repository.load("alice") }
        }
        assertEquals(0, profile.calls)
        assertEquals(0, source.calls)
    }

    @Test fun absentOrIncompleteProfileNeverLoadsCatalog() = runTest {
        for (status in listOf(ProfileRegistration.ABSENT, ProfileRegistration.INCOMPLETE)) {
            profile.registration = status
            fails { repository.load("alice") }
        }
        assertEquals(0, source.calls)
    }

    @Test fun profileReadFailurePropagates() = runTest {
        profile.fail = true
        fails { repository.load("alice") }
        assertEquals(0, source.calls)
    }

    @Test fun otherProfilePlanRejected() = runTest {
        fails { FirestoreAcademicRepository(source, AcademicFixtures.Profile("otro-plan")).load("alice") }
        assertEquals(0, source.calls)
    }

    @Test fun emptyOrPartialCatalogIsAnError() = runTest {
        for (docs in listOf(emptyList(), fixture.documents.dropLast(1))) {
            source.subjects = docs
            fails { repository.load("alice") }
        }
    }

    @Test fun rejectsInvalidFieldsAndLossyIntegerConversions() = runTest {
        val original = fixture.documents.first()
        for (changes in listOf(
            mapOf("semestre" to 1.0), mapOf("semestre" to "1"), mapOf("semestre" to Long.MAX_VALUE),
            mapOf("min_aprobadas" to -1L), mapOf("area" to "OTRA"), mapOf("id_plan" to "otro"),
            mapOf("codigo" to "BMA303"), mapOf("nombre" to " "), mapOf("extra" to true)
        )) {
            source.subjects = listOf(original.copy(fields = original.fields + changes)) + fixture.documents.drop(1)
            fails { repository.load("alice") }
        }
        source.subjects = listOf(original.copy(fields = original.fields - "nombre")) + fixture.documents.drop(1)
        fails { repository.load("alice") }
    }

    @Test fun duplicatedSubjectIdOrCodeRejected() = runTest {
        source.subjects = fixture.documents.dropLast(1) + fixture.documents.first()
        fails { repository.load("alice") }
        val second = fixture.documents[1]
        source.subjects = listOf(fixture.documents[0], second.copy(fields = second.fields +
            ("codigo" to fixture.documents[0].fields["codigo"]))) + fixture.documents.drop(2)
        fails { repository.load("alice") }
    }

    @Test fun missingOrPollutedRequirementsRejected() = runTest {
        source.relations = fixture.requirements + (fixture.documents[1].id to emptyList())
        fails { repository.load("alice") }
        source.relations = fixture.requirements + (fixture.documents[1].id to listOf(
            AcademicDocument(fixture.documents[0].id, mapOf("id_requisito" to fixture.documents[0].id))))
        fails { repository.load("alice") }
    }

    @Test fun orphanStatesAreErrorsInsteadOfSilentlyDiscarded() = runTest {
        source.states = listOf(AcademicDocument("retirada", mapOf("estado" to "APROBADA")))
        fails { repository.load("alice") }
    }

    @Test fun invalidOrDuplicatedStatesRejected() = runTest {
        for (fields in listOf(emptyMap(), mapOf("estado" to "PENDIENTE"), mapOf("estado" to "aprobada"),
            mapOf("estado" to 1L), mapOf("estado" to "APROBADA", "uid" to "bob"))) {
            source.states = listOf(AcademicDocument(fixture.documents[0].id, fields))
            fails { repository.load("alice") }
        }
        val state = AcademicDocument(fixture.documents[0].id, mapOf("estado" to "APROBADA"))
        source.states = listOf(state, state)
        fails { repository.load("alice") }
    }

    @Test fun cacheOrPendingWritesFailAtEveryReadStage() = runTest {
        for (stage in listOf("materias", "estados", "requisitos")) {
            for (cache in listOf(true, false)) {
                source.response = { reading, docs -> AcademicDocuments(docs,
                    fromCache = reading == stage && cache, pendingWrites = reading == stage && !cache) }
                fails { repository.load("alice") }
            }
        }
    }

    @Test fun anyQueryFailurePreventsPartialSuccess() = runTest {
        for (stage in listOf("materias", "estados", "requisitos")) {
            source.beforeRead = { if (it == stage) error("Red o permiso denegado") }
            fails { repository.load("alice") }
        }
    }

    @Test fun sessionChangeDuringReadDiscardsResult() = runTest {
        source.beforeRead = { if (it == "estados") source.currentUid = "bob" }
        fails { repository.load("alice") }
    }

    @Test fun cancellationIsNotConvertedIntoSuccess() = runTest {
        source.beforeRead = { throw CancellationException("Cancelada") }
        try { repository.load("alice"); fail("Se esperaba cancelación") }
        catch (_: CancellationException) { /* Propagada al llamador. */ }
    }

    @Test fun saveThenReadAndDeleteSameStateWithoutDuplicates() = runTest {
        val id = fixture.documents[49].id
        val saved = repository.saveState("alice", id, EstadoAcademico.APROBADA)
        assertEquals(mapOf(id to EstadoAcademico.APROBADA), saved)
        assertEquals(saved, repository.load("alice").estados)
        repository.saveState("alice", id, EstadoAcademico.EN_CURSO)
        assertEquals(1, source.states.size)
        assertTrue(repository.saveState("alice", id, EstadoAcademico.PENDIENTE).isEmpty())
    }

    @Test fun invalidIdentityProfileOrSubjectCannotWrite() = runTest {
        fails { repository.saveState("alice", "unknown", EstadoAcademico.APROBADA) }
        source.currentUid = "bob"
        fails { repository.saveState("alice", fixture.documents[0].id, EstadoAcademico.APROBADA) }
        source.currentUid = "alice"; profile.registration = ProfileRegistration.INCOMPLETE
        fails { repository.saveState("alice", fixture.documents[0].id, EstadoAcademico.APROBADA) }
        assertEquals(0, source.writes)
    }

    @Test fun writeFailureAndUnconfirmedReadNeverReportSuccess() = runTest {
        val id = fixture.documents[0].id
        source.writeFailure = true
        fails { repository.saveState("alice", id, EstadoAcademico.APROBADA) }
        source.writeFailure = false
        source.response = { stage, docs -> AcademicDocuments(docs, pendingWrites = stage == "estados") }
        fails { repository.saveState("alice", id, EstadoAcademico.APROBADA) }
    }

    @Test fun sessionChangeWhileSavingDiscardsConfirmation() = runTest {
        source.beforeWrite = { source.currentUid = "bob" }
        fails { repository.saveState("alice", fixture.documents[0].id, EstadoAcademico.APROBADA) }
    }
}

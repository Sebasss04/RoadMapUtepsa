package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.model.*
import java.util.UUID

/** Datos sintéticos con el tamaño de v1; no son una copia del catálogo institucional. */
internal class AcademicFixtures {
    val plan = ProfileStudyPlan.PLAN_ID
    private val semesters = listOf(5, 6, 7, 4, 6, 6, 6, 7, 3).flatMapIndexed { i, n -> List(n) { i + 1 } }
    val documents = (0..49).map { index -> AcademicDocument(
        UUID.nameUUIDFromBytes("academic-test-$index".toByteArray()).toString(),
        mapOf("id_plan" to plan, "codigo" to when (index) {
            46 -> "SIS-305"; 47 -> "SCC-305"; 48 -> "STS-300"; 49 -> "SIS-306"
            else -> "TST-${(index + 1).toString().padStart(3, '0')}"
        }, "nombre" to "Materia de prueba $index", "semestre" to semesters[index].toLong(),
            "area" to "ESPECIALIDAD", "min_aprobadas" to if (index == 48) 40L else 0L)
    ) }
    val requirements = documents.associate { it.id to emptyList<AcademicDocument>() }.toMutableMap().also { map ->
        for (i in 1..27) map[documents[i].id] = listOf(AcademicDocument(documents[i - 1].id, emptyMap()))
        map[documents[49].id] = listOf(46, 47).map { AcademicDocument(documents[it].id, emptyMap()) }
    }
    fun data(uid: String = "alice", states: List<AcademicDocument> = emptyList()): AcademicData =
        AcademicDataMapper.assemble(uid, plan, AcademicDataMapper.materias(documents, plan), requirements, states)

    class Profile(private val plan: String = ProfileStudyPlan.PLAN_ID) : ProfileRepository {
        var registration = ProfileRegistration.COMPLETE
        var fail = false
        var calls = 0
        override suspend fun load(uid: String): ProfileData {
            calls++
            if (fail) error("Error de lectura del perfil")
            return ProfileData("Nombre", "Apellido", AssignedStudyPlan(plan, "Carrera", "615/2016"), registration)
        }
        override suspend fun save(uid: String, names: String, surnames: String): ProfileData = error("No debe escribir")
    }

    inner class Source : AcademicReadSource {
        override var currentUid: String? = "alice"
        var subjects = documents
        var states = emptyList<AcademicDocument>()
        var relations: Map<String, List<AcademicDocument>> = requirements.toMap()
        var calls = 0
        var writes = 0
        var writeFailure = false
        var beforeWrite: suspend () -> Unit = {}
        var beforeRead: suspend (String) -> Unit = {}
        var response: (String, List<AcademicDocument>) -> AcademicDocuments = { _, docs -> AcademicDocuments(docs) }
        private suspend fun read(stage: String, docs: List<AcademicDocument>): AcademicDocuments {
            calls++
            beforeRead(stage)
            return response(stage, docs)
        }
        override suspend fun materias(plan: String): AcademicDocuments {
            check(plan == this@AcademicFixtures.plan)
            return read("materias", subjects)
        }
        override suspend fun estados(uid: String): AcademicDocuments {
            check(uid == "alice")
            return read("estados", states)
        }
        override suspend fun requisitos(idMateria: String) = read("requisitos", relations.getValue(idMateria))
        override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico) {
            writes++
            beforeWrite()
            if (writeFailure) error("Escritura denegada")
            states = states.filterNot { it.id == idMateria } + if (estado == EstadoAcademico.PENDIENTE) emptyList()
                else listOf(AcademicDocument(idMateria, mapOf("estado" to estado.name)))
        }
    }
}

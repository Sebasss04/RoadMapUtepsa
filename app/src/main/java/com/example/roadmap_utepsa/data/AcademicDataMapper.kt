package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.academic.ReglasAcademicas
import com.example.roadmap_utepsa.domain.model.*
import java.util.UUID

/** Representación de lectura sin SDK para comprobar el mapeo y los fallos localmente. */
internal data class AcademicDocument(val id: String, val fields: Map<String, Any?>)
internal data class AcademicDocuments(
    val documents: List<AcademicDocument>,
    val fromCache: Boolean = false,
    val pendingWrites: Boolean = false
) {
    fun confirmed(): List<AcademicDocument> {
        check(!fromCache && !pendingWrites) { "Lectura académica sin confirmación de servidor" }
        return documents
    }
}

internal object AcademicDataMapper {
    private val subjectFields = setOf("id_plan", "codigo", "nombre", "semestre", "area", "min_aprobadas")

    fun materias(documents: List<AcademicDocument>, plan: String): List<Materia> {
        require(documents.size == 50) { "El catálogo del plan debe contener 50 materias" }
        val result = documents.map { doc ->
            require(UUID.fromString(doc.id).toString() == doc.id) { "ID de materia inválido" }
            val fields = doc.fields
            require(fields.keys == subjectFields || fields.keys == subjectFields + "horas_totales") { "Campos de materia inválidos" }
            require(fields["id_plan"] == plan) { "Materia de otro plan" }
            fun text(key: String) = (fields[key] as? String)?.also {
                require(it.isNotBlank() && it == it.trim()) { "Texto de materia inválido" }
            } ?: error("Tipo de materia inválido: $key")
            // El SDK devuelve enteros Firestore como Long. No truncar Double ni aceptar texto.
            fun integer(key: String): Int {
                val value = fields[key] as? Long ?: error("Entero de materia inválido: $key")
                require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
                return value.toInt()
            }
            val code = text("codigo")
            require(code.matches(Regex("[A-Z]+-\\d{3}"))) { "Código inválido" }
            Materia(doc.id, plan, code, text("nombre"), integer("semestre"),
                AreaMateria.valueOf(text("area")), integer("min_aprobadas"),
                if ("horas_totales" in fields) integer("horas_totales") else null)
        }.sortedWith(compareBy(Materia::semestre, Materia::codigo))
        require(result.groupingBy { it.semestre }.eachCount() ==
            mapOf(1 to 5, 2 to 6, 3 to 7, 4 to 4, 5 to 6, 6 to 6, 7 to 6, 8 to 7, 9 to 3))
        require(result.count { it.codigo == "STS-300" } == 1)
        val sae = result.all { it.horasTotales != null }
        require(sae || result.all { it.horasTotales == null }) { "Catálogo de transición incompleto" }
        require(result.all { it.minAprobadas == if (it.codigo == "STS-300") { if (sae) 45 else 40 } else 0 })
        if (sae) {
            val bimodulares = setOf("BMS-300", "SCC-300", "SAL-301", "SIS-302", "SRF-301", "SIS-303", "SIS-304", "SDA-304", "SIS-306")
            require(result.all { it.horasTotales == if (it.codigo == "STS-300") 400 else if (it.codigo in bimodulares) 170 else 85 })
        }
        ReglasAcademicas(result, emptyList()) // IDs, códigos, plan y umbrales válidos.
        return result
    }

    fun assemble(uid: String, plan: String, materias: List<Materia>,
                 requisitos: Map<String, List<AcademicDocument>>,
                 estados: List<AcademicDocument>): AcademicData {
        val ids = materias.map { it.id }.toSet()
        require(requisitos.keys == ids) { "Lectura incompleta de prerrequisitos" }
        val relations = requisitos.flatMap { (target, documents) -> documents.map { doc ->
            require(doc.fields.isEmpty()) { "La relación debe estar vacía" }
            Prerrequisito(target, doc.id)
        } }
        require(relations.size == if (materias.all { it.horasTotales != null }) 28 else 29) { "Lectura incompleta de relaciones" }
        val reglas = ReglasAcademicas(materias, relations)
        val byCode = materias.associateBy { it.codigo }
        val taller = byCode.getValue("STS-300")
        require(relations.none { it.idObjetivo == taller.id })
        val integrador = byCode.getValue("SIS-306")
        require(relations.filter { it.idObjetivo == integrador.id }.map { it.idRequisito }.toSet() ==
            setOf(byCode.getValue("SIS-305").id, byCode.getValue("SCC-305").id))
        if (materias.all { it.horasTotales != null }) {
            val expected = mapOf("SDA-303" to setOf("SIS-302"), "SOI-304" to setOf("SOI-303"),
                "SRA-304" to emptySet(), "SES-302" to emptySet(), "SES-303" to emptySet())
            expected.forEach { (code, required) ->
                val target = byCode.getValue(code)
                require(relations.filter { it.idObjetivo == target.id }.map { it.idRequisito }.toSet() == required.map { byCode.getValue(it).id }.toSet()) {
                    "Los requisitos difieren de SAE"
                }
            }
        }
        val states = estados(estados, ids)
        reglas.evaluar(taller.id, states)
        return AcademicData(uid, plan, materias.toList(), relations.toList(), states.toMap())
    }

    fun estados(documents: List<AcademicDocument>, ids: Set<String>): Map<String, EstadoAcademico> {
        require(documents.map { it.id }.distinct().size == documents.size)
        return documents.associate { doc ->
            require(doc.id in ids) { "Estado de materia desconocida; no se descartará silenciosamente" }
            require(doc.fields.keys == setOf("estado")) { "Campos de estado inválidos" }
            val state = EstadoAcademico.valueOf(doc.fields["estado"] as? String ?: error("Tipo de estado inválido"))
            require(state != EstadoAcademico.PENDIENTE) { "Pendiente debe representarse por ausencia" }
            doc.id to state
        }
    }
}

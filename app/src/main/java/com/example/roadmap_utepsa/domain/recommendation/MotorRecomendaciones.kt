package com.example.roadmap_utepsa.domain.recommendation

import com.example.roadmap_utepsa.domain.academic.ReglasAcademicas
import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.offer.Oferta

data class AvanceParcial(val materia: Materia, val requisitosAprobados: List<Materia>,
    val requisitosPendientes: List<Materia>, val aprobadasFaltantes: Int)

data class Recomendacion(val materia: Materia, val requisitosSatisfechos: List<Materia>,
    val umbralSatisfecho: Int, val desbloqueadas: List<Materia>, val pendienteAnterior: Boolean,
    val avancesParciales: List<AvanceParcial> = emptyList()) {
    val tieneBeneficio get() = desbloqueadas.isNotEmpty() || avancesParciales.isNotEmpty()
}

data class ResultadoRecomendaciones(val candidatas: List<Recomendacion>,
    val pendientesAnteriores: List<Materia>, val semestreReferencia: Int?) {
    val principal get() = candidatas.firstOrNull()
    val alternativas get() = candidatas.drop(1)
    val secundaria get() = alternativas.firstOrNull { it.tieneBeneficio }
    val opcionales get() = alternativas.filter { it.materia.id != secundaria?.materia?.id }
    val alternativaDisponible get() = pendientesAnteriores.isNotEmpty() && principal?.pendienteAnterior == false
}

/** Simulación local de una sola aprobación; jamás modifica las entradas. */
class MotorRecomendaciones {
    fun calcular(data: AcademicData, oferta: Oferta): ResultadoRecomendaciones {
        require(data.idPlan == oferta.idPlan && data.materias.all { it.idPlan == data.idPlan })
        val ids = data.materias.map { it.id }.toSet()
        require(oferta.materias.all { it in ids })
        val reglas = ReglasAcademicas(data.materias, data.prerrequisitos)
        val requisitosPorObjetivo = data.prerrequisitos.groupBy { it.idObjetivo }
            .mapValues { (_, relaciones) -> relaciones.map { it.idRequisito }.toSet() }
        require(ReglasHistorial(data.materias, data.prerrequisitos).validar(data.estados).contradicciones.isEmpty())
        val orden = compareBy<Materia> { it.semestre }.thenBy { it.codigo }
        val pendientes = data.materias.filter { (data.estados[it.id] ?: EstadoAcademico.PENDIENTE) == EstadoAcademico.PENDIENTE }
        val referencia = data.materias.filter { data.estados[it.id] in setOf(EstadoAcademico.APROBADA, EstadoAcademico.EN_CURSO) }
            .maxOfOrNull { it.semestre }
        val anteriores = pendientes.filter { referencia != null && it.semestre < referencia }.sortedWith(orden)
        val anterioresIds = anteriores.map { it.id }.toSet()
        val habilitadas = pendientes.filter { reglas.evaluar(it.id, data.estados).habilitada }.map { it.id }.toSet()
        val candidatas = pendientes.filter { it.id in oferta.materias && it.id in habilitadas }.map { materia ->
            val simulados = data.estados + (materia.id to EstadoAcademico.APROBADA)
            val desbloqueadas = pendientes.filter { it.id != materia.id && it.id !in habilitadas && reglas.evaluar(it.id, simulados).habilitada }
                .sortedWith(orden)
            val avances = pendientes.filter { objetivo ->
                objetivo.id != materia.id && objetivo.id !in habilitadas &&
                    materia.id in requisitosPorObjetivo[objetivo.id].orEmpty() &&
                    !reglas.evaluar(objetivo.id, simulados).habilitada
            }.sortedWith(orden).map { objetivo ->
                val evaluacion = reglas.evaluar(objetivo.id, simulados)
                val requisitosObjetivo = data.materias.filter { it.id in requisitosPorObjetivo[objetivo.id].orEmpty() }.sortedWith(orden)
                AvanceParcial(objetivo,
                    requisitosObjetivo.filter { simulados[it.id] == EstadoAcademico.APROBADA },
                    requisitosObjetivo.filter { simulados[it.id] != EstadoAcademico.APROBADA },
                    evaluacion.aprobadasFaltantes)
            }
            val requisitos = requisitosPorObjetivo[materia.id].orEmpty()
            Recomendacion(materia, data.materias.filter { it.id in requisitos }.sortedWith(orden),
                materia.minAprobadas, desbloqueadas, materia.id in anterioresIds, avances)
        }.sortedWith(compareByDescending<Recomendacion> { it.pendienteAnterior }
            .thenByDescending { it.desbloqueadas.size }.thenByDescending { it.avancesParciales.size }
            .thenBy { it.materia.semestre }.thenBy { it.materia.codigo })
        return ResultadoRecomendaciones(candidatas, anteriores, referencia)
    }
}

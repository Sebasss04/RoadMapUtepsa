package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.domain.academic.ReglasAcademicas
import com.example.roadmap_utepsa.domain.model.AreaMateria
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.domain.model.EstadoAcademico.APROBADA
import com.example.roadmap_utepsa.domain.model.EstadoAcademico.EN_CURSO
import com.example.roadmap_utepsa.domain.model.EstadoAcademico.PENDIENTE
import com.example.roadmap_utepsa.domain.model.Materia
import com.example.roadmap_utepsa.domain.model.Prerrequisito
import org.junit.Assert.*
import org.junit.Test

class ReglasAcademicasTest {
    private fun materia(id: String, semestre: Int = 1, minimo: Int = 0) = Materia(
        id, "plan-prueba", id, "Materia $id", semestre, AreaMateria.ESPECIALIDAD, minimo
    )

    private val calidad = materia("id-calidad").copy(codigo = "SIS-305")
    private val datos = materia("id-datos").copy(codigo = "SCC-305")
    private val proyecto = materia("id-proyecto", semestre = 9).copy(codigo = "SIS-306")
    private val integrador = ReglasAcademicas(
        listOf(calidad, datos, proyecto),
        listOf(Prerrequisito(proyecto.id, calidad.id), Prerrequisito(proyecto.id, datos.id))
    )

    // 49 materias sintéticas y Taller: no es una segunda copia del catálogo real.
    private val ordinarias = (1..49).map { materia("id-$it") }
    private val taller = materia("id-taller", semestre = 9, minimo = 40).copy(codigo = "STS-300")
    private val reglasTaller = ReglasAcademicas(ordinarias + taller, emptyList())
    private fun aprobadas(cantidad: Int): Map<String, EstadoAcademico> =
        ordinarias.take(cantidad).associate { it.id to APROBADA }

    @Test fun sinRequisitosSeHabilitaInclusoEnSemestreAvanzado() {
        assertTrue(integrador.evaluar(calidad.id, emptyMap()).habilitada)
        val avanzada = materia("avanzada", semestre = 9)
        assertTrue(ReglasAcademicas(listOf(avanzada), emptyList()).evaluar(avanzada.id, emptyMap()).habilitada)
    }

    @Test fun integradorInformaAmbosRequisitosAusentes() {
        val resultado = integrador.evaluar(proyecto.id, emptyMap())
        assertFalse(resultado.habilitada)
        assertEquals(setOf(calidad.id, datos.id), resultado.requisitosSinAprobar)
        assertEquals(0, resultado.aprobadasFaltantes)
    }

    @Test fun unSoloRequisitoAprobadoNoHabilitaIntegrador() {
        val resultado = integrador.evaluar(proyecto.id, mapOf(calidad.id to APROBADA))
        assertFalse(resultado.habilitada)
        assertEquals(setOf(datos.id), resultado.requisitosSinAprobar)
    }

    @Test fun requisitoEnCursoNoEquivaleAAprobado() {
        val resultado = integrador.evaluar(proyecto.id, mapOf(calidad.id to APROBADA, datos.id to EN_CURSO))
        assertFalse(resultado.habilitada)
        assertEquals(setOf(datos.id), resultado.requisitosSinAprobar)
    }

    @Test fun todosLosRequisitosAprobadosHabilitanIntegrador() {
        val resultado = integrador.evaluar(proyecto.id, mapOf(calidad.id to APROBADA, datos.id to APROBADA))
        assertTrue(resultado.habilitada)
        assertTrue(resultado.requisitosSinAprobar.isEmpty())
    }

    @Test fun ausenciaYEstadoPendienteSonEquivalentesTrasLecturaExitosa() {
        assertEquals(integrador.evaluar(proyecto.id, emptyMap()),
            integrador.evaluar(proyecto.id, mapOf(calidad.id to PENDIENTE, datos.id to PENDIENTE)))
    }

    @Test fun tallerCon39AprobadasPermaneceBloqueado() {
        val resultado = reglasTaller.evaluar(taller.id, aprobadas(39))
        assertFalse(resultado.habilitada)
        assertEquals(1, resultado.aprobadasFaltantes)
        assertTrue(resultado.requisitosSinAprobar.isEmpty())
    }

    @Test fun tallerCon40AprobadasSeHabilita() {
        val resultado = reglasTaller.evaluar(taller.id, aprobadas(40))
        assertTrue(resultado.habilitada)
        assertEquals(0, resultado.aprobadasFaltantes)
    }

    @Test fun tallerNoCuentaEnCursoNiPendientesComoAprobadas() {
        val estados = aprobadas(39) + mapOf(ordinarias[39].id to EN_CURSO, ordinarias[40].id to PENDIENTE)
        assertEquals(1, reglasTaller.evaluar(taller.id, estados).aprobadasFaltantes)
    }

    @Test fun deficitDeTallerNuncaEsNegativo() {
        assertEquals(40, reglasTaller.evaluar(taller.id, emptyMap()).aprobadasFaltantes)
        assertEquals(0, reglasTaller.evaluar(taller.id, aprobadas(49)).aprobadasFaltantes)
    }

    @Test fun corregirAprobadaAEnCursoVuelveABloquearSinGuardarResultados() {
        val estados = aprobadas(40).toMutableMap()
        assertTrue(reglasTaller.evaluar(taller.id, estados).habilitada)
        estados[ordinarias[39].id] = EN_CURSO
        assertFalse(reglasTaller.evaluar(taller.id, estados).habilitada)
    }

    @Test fun umbralYFlechasSeCumplenConjuntamente() {
        val objetivo = materia("objetivo", minimo = 2)
        val reglas = ReglasAcademicas(ordinarias.take(3) + objetivo,
            listOf(Prerrequisito(objetivo.id, ordinarias[2].id)))
        val faltaFlecha = reglas.evaluar(objetivo.id, aprobadas(2))
        assertFalse(faltaFlecha.habilitada)
        assertEquals(0, faltaFlecha.aprobadasFaltantes)
        assertEquals(setOf(ordinarias[2].id), faltaFlecha.requisitosSinAprobar)
        val faltaUmbral = reglas.evaluar(objetivo.id, mapOf(ordinarias[2].id to APROBADA))
        assertFalse(faltaUmbral.habilitada)
        assertTrue(faltaUmbral.requisitosSinAprobar.isEmpty())
        assertEquals(1, faltaUmbral.aprobadasFaltantes)
        assertTrue(reglas.evaluar(objetivo.id, aprobadas(3)).habilitada)
    }

    @Test fun historialAutodeclaradoNoExigeAprobarRequisitosEnOrden() {
        // A -> B -> C: B autodeclarada aprobada habilita C, aunque falte A.
        val a = materia("a")
        val b = materia("b")
        val c = materia("c")
        val reglas = ReglasAcademicas(listOf(a, b, c),
            listOf(Prerrequisito(b.id, a.id), Prerrequisito(c.id, b.id)))
        val estados = mutableMapOf(b.id to APROBADA)
        val antes = estados.toMap()
        assertFalse(reglas.evaluar(b.id, estados).habilitada)
        assertTrue(reglas.evaluar(c.id, estados).habilitada)
        assertEquals(antes, estados)
    }

    @Test fun habilitacionNoFiltraPorEstadoPropioNiSignificaCandidatura() {
        val estados = mapOf(calidad.id to APROBADA, datos.id to APROBADA)
        for (estado in EstadoAcademico.entries) {
            assertTrue(integrador.evaluar(proyecto.id, estados + (proyecto.id to estado)).habilitada)
        }
    }

    @Test fun referenciasUsanIdAunqueCambieCodigoVisible() {
        val renombrada = calidad.copy(codigo = "CODIGO-CORREGIDO")
        val reglas = ReglasAcademicas(listOf(renombrada, proyecto),
            listOf(Prerrequisito(proyecto.id, calidad.id)))
        assertTrue(reglas.evaluar(proyecto.id, mapOf(calidad.id to APROBADA)).habilitada)
    }

    @Test fun rechazaMateriaOEstadosDesconocidos() {
        assertThrows(IllegalArgumentException::class.java) { integrador.evaluar("otro", emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) {
            reglasTaller.evaluar(taller.id, aprobadas(39) + ("otra-cuenta-o-plan" to APROBADA))
        }
    }

    @Test fun rechazaCatalogoVacioIdsOCodigosDuplicadosYPlanesMezclados() {
        for (catalogo in listOf(emptyList(), listOf(calidad, calidad),
            listOf(calidad, datos.copy(codigo = calidad.codigo)),
            listOf(calidad, datos.copy(idPlan = "otro")))) {
            assertThrows(IllegalArgumentException::class.java) { ReglasAcademicas(catalogo, emptyList()) }
        }
    }

    @Test fun rechazaReferenciasRotasRelacionesDuplicadasYAutorrequisitos() {
        assertThrows(IllegalArgumentException::class.java) { Prerrequisito(calidad.id, calidad.id) }
        assertThrows(IllegalArgumentException::class.java) {
            ReglasAcademicas(listOf(calidad), listOf(Prerrequisito(calidad.id, "inexistente")))
        }
        val relacion = Prerrequisito(proyecto.id, calidad.id)
        assertThrows(IllegalArgumentException::class.java) {
            ReglasAcademicas(listOf(calidad, proyecto), listOf(relacion, relacion))
        }
    }

    @Test fun rechazaCiclos() {
        assertThrows(IllegalArgumentException::class.java) {
            ReglasAcademicas(listOf(calidad, datos, proyecto), listOf(
                Prerrequisito(calidad.id, datos.id), Prerrequisito(datos.id, proyecto.id),
                Prerrequisito(proyecto.id, calidad.id)))
        }
    }

    @Test fun rechazaDatosBasicosInvalidosYUmbralInalcanzable() {
        for (crear in listOf<() -> Materia>(
            { calidad.copy(id = "") }, { calidad.copy(idPlan = " ") },
            { calidad.copy(nombre = "") }, { calidad.copy(codigo = "") },
            { calidad.copy(semestre = 0) }, { calidad.copy(semestre = 10) },
            { calidad.copy(minAprobadas = -1) }
        )) {
            assertThrows(IllegalArgumentException::class.java) { crear() }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReglasAcademicas(listOf(calidad.copy(minAprobadas = 1)), emptyList())
        }
    }
}

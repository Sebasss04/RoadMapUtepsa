package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico.*
import org.junit.Assert.*
import org.junit.Test

/** Pruebas JVM de dominio con catálogo local; no consultan ni escriben Firebase. */
class ReglasHistorialTest {
    private val catalogo = CatalogoHistorialFixture()
    private val reglas = ReglasHistorial(catalogo.materias, catalogo.prerrequisitos)
    private val proyecto = catalogo.id("SIS-306")
    private val calidad = catalogo.id("SIS-305")
    private val datos = catalogo.id("SCC-305")
    private val taller = catalogo.id("STS-300")
    // Orden topológico construido desde las relaciones existentes, sin inferir fechas.
    private val orden: List<String> = buildList {
        fun visitar(id: String) {
            if (id in this) return
            catalogo.prerrequisitos.filter { it.idObjetivo == id }.forEach { visitar(it.idRequisito) }
            add(id)
        }
        catalogo.materias.filter { it.id != taller && it.id != proyecto }.forEach { visitar(it.id) }
    }
    private fun aprobadas(n: Int): Map<String, EstadoAcademico> = orden.take(n).associateWith { APROBADA }
    private fun completa() = catalogo.materias.associate { it.id to APROBADA }
    private fun cambiar(id: String, estado: EstadoAcademico, mapa: Map<String, EstadoAcademico>) =
        reglas.evaluarTransicion(id, estado, mapa)

    @Test fun catalogoExistenteConservaRelacionesYUmbral() {
        assertEquals(setOf(calidad, datos), catalogo.prerrequisitos
            .filter { it.idObjetivo == proyecto }.map { it.idRequisito }.toSet())
        assertEquals(45, catalogo.materias.single { it.id == taller }.minAprobadas)
    }

    @Test fun ceroAprobadasEsConjuntoCoherente() {
        assertTrue(reglas.validar(emptyMap()).coherente)
        assertTrue(reglas.validar(catalogo.materias.associate { it.id to PENDIENTE }).coherente)
    }

    @Test fun requisitosMultiplesFaltantesSeExplicanPorId() {
        val diagnostico = reglas.validar(mapOf(proyecto to APROBADA)).contradicciones.single()
        assertEquals(proyecto, diagnostico.idMateria)
        assertEquals(APROBADA, diagnostico.estado)
        assertEquals(setOf(calidad, datos), diagnostico.requisitosSinAprobar)
        assertEquals(0, diagnostico.aprobadasFaltantes)
    }

    @Test fun enCursoRechazaUnSoloRequisitoAprobado() {
        val r = cambiar(proyecto, EN_CURSO, mapOf(datos to APROBADA))
        assertFalse(r.permitida)
        assertEquals(setOf(calidad), r.bloqueos.single().requisitosSinAprobar)
        assertEquals(EN_CURSO, r.bloqueos.single().estado)
    }

    @Test fun enCursoNoSatisfaceRequisito() {
        val r = cambiar(proyecto, EN_CURSO, mapOf(calidad to APROBADA, datos to EN_CURSO))
        assertFalse(r.permitida)
        assertEquals(setOf(datos), r.bloqueos.single().requisitosSinAprobar)
    }

    @Test fun enCursoExigeAmbosRequisitosAunqueHayaOtrasAprobadas() {
        val estados = completa() - calidad - datos - proyecto
        val r = cambiar(proyecto, EN_CURSO, estados)
        assertFalse(r.permitida)
        assertEquals(setOf(calidad, datos), r.bloqueos.single { it.idMateria == proyecto }.requisitosSinAprobar)
    }

    @Test fun enCursoConAmbosRequisitosNoSeBloqueaPorOmisionesAjenas() {
        val r = cambiar(proyecto, EN_CURSO, mapOf(calidad to APROBADA, datos to APROBADA))
        assertTrue(r.permitida)
        assertFalse(r.propuesto.coherente)
        assertTrue(r.bloqueos.isEmpty())
    }

    @Test fun declararAprobadaPermiteDatoSinAprobarRequisitos() {
        val estados = mutableMapOf<String, EstadoAcademico>()
        val r = cambiar(proyecto, APROBADA, estados)
        assertTrue(r.permitida)
        assertFalse(r.propuesto.coherente)
        assertEquals(setOf(calidad, datos), r.propuesto.contradicciones.single().requisitosSinAprobar)
        assertTrue(estados.isEmpty())
    }

    @Test fun enCursoAAprobadaReevaluaHistorialAntiguo() {
        val r = cambiar(proyecto, APROBADA, mapOf(proyecto to EN_CURSO))
        assertTrue(r.permitida)
        assertFalse(r.propuesto.coherente)
        assertEquals(APROBADA, r.propuesto.contradicciones.single().estado)
    }

    @Test fun seleccionEnCualquierOrdenValidaSoloConjuntoFinal() {
        val directa = completa()
        val inversa = directa.entries.reversed().associate { it.toPair() }
        assertTrue(reglas.validar(directa).coherente)
        assertTrue(reglas.validar(inversa).coherente)
        assertFalse(reglas.validar(mapOf(proyecto to APROBADA)).coherente)
    }

    @Test fun tallerEnCursoCon44NoCon45Si() {
        val bloqueado = cambiar(taller, EN_CURSO, aprobadas(44))
        assertFalse(bloqueado.permitida)
        val diagnostico = bloqueado.bloqueos.single()
        assertEquals(45, diagnostico.minAprobadas)
        assertEquals(44, diagnostico.aprobadasComputables)
        assertEquals(1, diagnostico.aprobadasFaltantes)
        val permitido = cambiar(taller, EN_CURSO, aprobadas(45))
        assertTrue(permitido.permitida)
        assertTrue(permitido.propuesto.coherente)
    }

    @Test fun tallerAprobadaNoSeCuentaASiMismaCon44() {
        val r = reglas.validar(aprobadas(44) + (taller to APROBADA))
        val d = r.contradicciones.single { it.idMateria == taller }
        assertEquals(44, d.aprobadasComputables)
        assertEquals(1, d.aprobadasFaltantes)
        assertEquals(1, com.example.roadmap_utepsa.domain.academic.ReglasAcademicas(catalogo.materias, catalogo.prerrequisitos)
            .evaluar(taller, aprobadas(44) + (taller to APROBADA)).aprobadasFaltantes)
        assertTrue(reglas.validar(aprobadas(45) + (taller to APROBADA)).coherente)
    }

    @Test fun retirarAprobacionParaEnCursoNoPermiteAutohabilitarTaller() {
        val r = cambiar(taller, EN_CURSO, aprobadas(44) + (taller to APROBADA))
        assertFalse(r.permitida)
        assertEquals(1, r.bloqueos.single { it.idMateria == taller }.aprobadasFaltantes)
    }

    @Test fun enCursoYPendienteNoCuentanEnUmbral() {
        val estados = aprobadas(44) + (orden[44] to EN_CURSO) + (orden[45] to PENDIENTE)
        assertFalse(cambiar(taller, EN_CURSO, estados).permitida)
    }

    @Test fun retirarAprobacionUsadaPorDependienteAprobadaSeBloquea() {
        val estados = completa()
        val r = cambiar(datos, PENDIENTE, estados)
        assertFalse(r.permitida)
        assertEquals(setOf(datos), r.dependientesAfectados.single { it.idMateria == proyecto }.requisitosSinAprobar)
        assertEquals(APROBADA, estados[datos])
        assertEquals(APROBADA, estados[proyecto])
    }

    @Test fun retirarAprobacionUsadaPorDependienteEnCursoSeBloquea() {
        val estados = completa() + (proyecto to EN_CURSO)
        val r = cambiar(datos, EN_CURSO, estados)
        assertFalse(r.permitida)
        assertEquals(EN_CURSO, r.dependientesAfectados.single { it.idMateria == proyecto }.estado)
    }

    @Test fun retiradaAgravaRequisitosAunqueDisminuyaNumeroDeMateriasContradictorias() {
        val estados = mapOf(proyecto to APROBADA, datos to APROBADA)
        val r = cambiar(datos, PENDIENTE, estados)
        assertFalse(r.permitida)
        assertEquals(2, r.anterior.contradicciones.size)
        assertEquals(1, r.propuesto.contradicciones.size)
        assertEquals(setOf(calidad, datos), r.dependientesAfectados.single().requisitosSinAprobar)
    }

    @Test fun retirarAprobadaIncoherenteSinDependientesReduceContradiccionesPeseAOtraAjena() {
        val r = cambiar(datos, PENDIENTE, mapOf(datos to APROBADA, calidad to APROBADA))
        assertTrue(r.permitida)
        assertEquals(2, r.anterior.contradicciones.size)
        assertEquals(listOf(calidad), r.propuesto.contradicciones.map { it.idMateria })
        assertTrue(r.dependientesAfectados.isEmpty())
        assertTrue(r.bloqueos.isEmpty())
    }

    @Test fun retirarAprobadaAfectaUmbralSinFlechaDirecta() {
        val estados = aprobadas(45) + (taller to EN_CURSO)
        val r = cambiar(orden.last { it in estados }, PENDIENTE, estados)
        assertFalse(r.permitida)
        assertEquals(1, r.dependientesAfectados.single { it.idMateria == taller }.aprobadasFaltantes)
    }

    @Test fun retiradaAgravaUmbralYaIncumplido() {
        val estados = aprobadas(44) + (taller to APROBADA)
        val r = cambiar(orden[43], PENDIENTE, estados)
        assertFalse(r.permitida)
        assertEquals(2, r.dependientesAfectados.single { it.idMateria == taller }.aprobadasFaltantes)
    }

    @Test fun retirarAprobadaNoUsadaPermiteCorregirSinBloqueoAjeno() {
        val libre = orden.first { id -> catalogo.prerrequisitos.none { it.idRequisito == id } }
        val estados = mapOf(proyecto to APROBADA, libre to APROBADA)
        val r = cambiar(libre, PENDIENTE, estados)
        assertTrue(r.permitida)
        assertFalse(r.propuesto.coherente)
        assertTrue(r.dependientesAfectados.isEmpty())
    }

    @Test fun corregirOmisionNoRequiereBorrarDependienteNiCoherenciaAjena() {
        val estados = completa() - calidad - datos
        val r = cambiar(calidad, APROBADA, estados)
        assertTrue(r.permitida)
        assertEquals(setOf(datos), r.propuesto.contradicciones.single().requisitosSinAprobar)
        assertFalse(r.propuesto.coherente)
        val corregido = estados + (calidad to APROBADA) + (datos to APROBADA)
        assertTrue(reglas.validar(corregido).coherente)
        assertEquals(APROBADA, corregido[proyecto])
    }

    @Test fun correccionConjuntaSeEvaluaSinForzarTransicionesIntermedias() {
        val estados = completa()
        assertFalse(cambiar(datos, PENDIENTE, estados).permitida)
        assertTrue(reglas.validar(estados - datos - proyecto).coherente)
    }

    @Test fun enCursoAPendientePermiteCorregirContradiccionSinCascada() {
        val estados = mapOf(proyecto to EN_CURSO, calidad to APROBADA)
        val r = cambiar(proyecto, PENDIENTE, estados)
        assertTrue(r.permitida)
        assertFalse(r.propuesto.coherente)
        assertTrue(r.propuesto.contradicciones.none { it.idMateria == proyecto })
        assertEquals(APROBADA, estados[calidad])
    }

    @Test fun noOpNoBloqueaHistorialAntiguoIncoherente() {
        for (estado in EstadoAcademico.entries) {
            val estados = mapOf(proyecto to estado)
            val r = cambiar(proyecto, estado, estados)
            assertTrue(r.permitida)
            assertEquals(r.anterior, r.propuesto)
            assertTrue(r.bloqueos.isEmpty())
        }
    }

    @Test fun ausenteYPendienteExplicitoSonEquivalentes() {
        assertEquals(reglas.validar(mapOf(proyecto to APROBADA)),
            reglas.validar(mapOf(proyecto to APROBADA, calidad to PENDIENTE, datos to PENDIENTE)))
    }

    @Test fun rechazaIdsDesconocidosEnValidacionYTransicion() {
        assertThrows(IllegalArgumentException::class.java) { reglas.validar(mapOf("ajeno" to APROBADA)) }
        assertThrows(IllegalArgumentException::class.java) { cambiar("ajeno", APROBADA, emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) { cambiar(proyecto, APROBADA, mapOf("ajeno" to PENDIENTE)) }
    }

    @Test fun evaluacionesNoMutanHistorialNiGuardanResultadosEntreLlamadas() {
        val estados = (completa() - calidad).toMutableMap()
        val copia = estados.toMap()
        val primero = reglas.validar(estados)
        cambiar(datos, PENDIENTE, estados)
        assertEquals(copia, estados)
        assertEquals(primero, reglas.validar(estados))
        estados[calidad] = APROBADA
        assertTrue(reglas.validar(estados).coherente)
        assertFalse(primero.coherente)
    }
}

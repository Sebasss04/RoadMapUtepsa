package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.CambioEstadoHistorial
import com.example.roadmap_utepsa.domain.history.DiferenciaHistorial
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.domain.model.EstadoAcademico.*
import org.junit.Assert.*
import org.junit.Test

/** Pruebas de borrador en memoria: no usan Firestore ni simulan persistencia durable. */
class PreparacionHistorialTest {
    private val catalogo = CatalogoHistorialFixture()
    private val reglas = ReglasHistorial(catalogo.materias, catalogo.prerrequisitos)

    @Test fun crearConservaElHistorialExistenteSinCambios() {
        val existente = mapOf("a" to APROBADA, "b" to EN_CURSO)
        val borrador = BorradorHistorial("uid-a", existente)
        assertEquals(existente, borrador.base)
        assertEquals(existente, borrador.seleccion)
        assertTrue(borrador.cambios().isEmpty())
    }

    @Test fun cuentaNuevaPuedeValidarCeroAprobadas() {
        val borrador = BorradorHistorial("nueva", emptyMap())
        assertTrue(borrador.validar(reglas).coherente)
        assertTrue(borrador.cambios().isEmpty())
    }

    @Test fun seleccionSinOrdenSeValidaSoloComoConjunto() {
        val proyecto = catalogo.id("SIS-306")
        val inicial = BorradorHistorial("uid", emptyMap()).seleccionar(proyecto, APROBADA)
        assertFalse(inicial.validar(reglas).coherente)
        val completo = catalogo.materias.reversed().fold(inicial) { borrador, materia ->
            borrador.seleccionar(materia.id, APROBADA)
        }
        assertTrue(completo.validar(reglas).coherente)
        assertEquals(mapOf(proyecto to APROBADA), inicial.seleccion)
    }

    @Test fun uidYSeleccionesPermanecenAisladosEntreCuentas() {
        val primera = BorradorHistorial("primera", mapOf("a" to APROBADA))
        val segunda = BorradorHistorial("segunda", emptyMap()).seleccionar("b", EN_CURSO)
        assertEquals("primera", primera.uid)
        assertEquals("segunda", segunda.uid)
        assertEquals(mapOf("a" to APROBADA), primera.seleccion)
        assertEquals(mapOf("b" to EN_CURSO), segunda.seleccion)
        assertEquals("segunda", segunda.seleccionar("b", PENDIENTE).uid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun uidVacioSeRechaza() { BorradorHistorial("  ", emptyMap()) }

    @Test fun mapasRecibidosNoPuedenModificarLasCopiasInternas() {
        val base = mutableMapOf("a" to APROBADA, "b" to EN_CURSO)
        val seleccion = mutableMapOf("a" to APROBADA, "b" to PENDIENTE)
        val borrador = BorradorHistorial("uid", base, seleccion)
        base.clear()
        seleccion.clear()
        assertEquals(2, borrador.base.size)
        assertEquals(2, borrador.seleccion.size)
        (borrador.base as MutableMap<String, EstadoAcademico>).clear()
        (borrador.seleccion as MutableMap<String, EstadoAcademico>).clear()
        assertEquals(APROBADA, borrador.base["a"])
        assertEquals(PENDIENTE, borrador.seleccion["b"])
    }

    @Test fun cambiosSoloIncluyenDiferenciasAcademicasYPermitenDeshacerSeleccion() {
        val original = BorradorHistorial("uid", mapOf("a" to APROBADA))
        val editado = original.seleccionar("a", PENDIENTE).seleccionar("b", EN_CURSO)
        assertEquals(mapOf(
            "a" to CambioEstadoHistorial(APROBADA, PENDIENTE),
            "b" to CambioEstadoHistorial(PENDIENTE, EN_CURSO)
        ), editado.cambios())
        assertTrue(editado.seleccionar("a", APROBADA).seleccionar("b", PENDIENTE).cambios().isEmpty())
        assertEquals(APROBADA, original.seleccion["a"])
    }

    @Test fun ausenciaYPendienteNoCreanCambiosNiConflictos() {
        val borrador = BorradorHistorial("uid", mapOf("a" to PENDIENTE))
            .seleccionar("b", PENDIENTE)
        assertTrue(borrador.cambios().isEmpty())
        assertTrue(borrador.diferenciasRemotas(mapOf("b" to PENDIENTE)).isEmpty())
    }

    @Test fun detectaCambioRemotoInclusoFueraDeSeleccionLocal() {
        val borrador = BorradorHistorial("uid", mapOf("a" to APROBADA))
            .seleccionar("a", EN_CURSO)
        assertEquals(listOf(DiferenciaHistorial("b", PENDIENTE, APROBADA, PENDIENTE)),
            borrador.diferenciasRemotas(mapOf("a" to APROBADA, "b" to APROBADA)))
    }

    @Test fun detectaEliminacionRemotaYNoRebasaNiResuelveConflicto() {
        val borrador = BorradorHistorial("uid", mapOf("a" to APROBADA))
            .seleccionar("a", EN_CURSO)
        val esperado = listOf(DiferenciaHistorial("a", APROBADA, PENDIENTE, EN_CURSO))
        assertEquals(esperado, borrador.diferenciasRemotas(emptyMap()))
        assertEquals(esperado, borrador.diferenciasRemotas(emptyMap()))
        assertEquals(APROBADA, borrador.base["a"])
        assertEquals(EN_CURSO, borrador.seleccion["a"])
    }

    @Test fun cambioRemotoIgualALaSeleccionTambienExigeRevision() {
        val borrador = BorradorHistorial("uid", emptyMap()).seleccionar("a", APROBADA)
        assertEquals(listOf(DiferenciaHistorial("a", PENDIENTE, APROBADA, APROBADA)),
            borrador.diferenciasRemotas(mapOf("a" to APROBADA)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun validarRechazaMateriaFueraDelCatalogo() {
        BorradorHistorial("uid", emptyMap()).seleccionar("desconocida", APROBADA).validar(reglas)
    }
}

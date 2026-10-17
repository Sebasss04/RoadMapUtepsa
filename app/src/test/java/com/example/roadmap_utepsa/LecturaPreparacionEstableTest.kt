package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.data.leerPreparacionEstable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LecturaPreparacionEstableTest {
    @Test fun datosEntreMetadatosEstablesSeLeenUnaSolaVez() = runTest {
        var consultas = 0
        var controles = 0
        val (version, datos) = leerPreparacionEstable(
            { controles++; "revision-1" }, { consultas++; listOf("snapshot") })
        assertEquals("revision-1", version)
        assertEquals(listOf("snapshot"), datos)
        assertEquals(1, consultas)
        assertEquals(2, controles)
    }

    @Test fun cambioDeDiarioDuranteConsultaDescartaSnapshotYReintenta() = runTest {
        val versiones = ArrayDeque(listOf("inicio", "grupo-1", "grupo-1", "grupo-1"))
        var consultas = 0
        val (version, snapshot) = leerPreparacionEstable(
            { versiones.removeFirst() }, { ++consultas })
        assertEquals("grupo-1", version)
        assertEquals(2, snapshot)
        assertEquals(2, consultas)
    }

    @Test fun cambiosContinuosNoDevuelvenMezclaNiReintentanSinLimite() = runTest {
        var revision = 0
        var consultas = 0
        try {
            leerPreparacionEstable({ ++revision }, { ++consultas })
            fail("No debe entregar datos de versiones distintas")
        } catch (_: IllegalStateException) { }
        assertEquals(3, consultas)
        assertEquals(6, revision)
    }

    @Test fun errorDeLecturaNoSeConvierteEnColeccionVacia() = runTest {
        try {
            leerPreparacionEstable({ "v1" }, { error("Sin red") })
            fail("No debe ocultar la lectura fallida")
        } catch (e: IllegalStateException) { assertEquals("Sin red", e.message) }
    }
}

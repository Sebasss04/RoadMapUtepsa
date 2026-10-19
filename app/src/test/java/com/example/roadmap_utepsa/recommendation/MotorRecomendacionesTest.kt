package com.example.roadmap_utepsa.recommendation

import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.AreaMateria
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.offer.Oferta
import com.example.roadmap_utepsa.domain.academic.Prerrequisito
import com.example.roadmap_utepsa.domain.recommendation.MotorRecomendaciones
import org.junit.Test
import org.junit.Assert.*

class MotorRecomendacionesTest {
    private fun m(id: String, semester: Int = 1, threshold: Int = 0) = Materia(id, "plan", id, "Materia $id", semester, AreaMateria.ESPECIALIDAD, threshold)
    private fun calculate(ms: List<Materia>, requirements: List<Prerrequisito> = emptyList(), states: Map<String, EstadoAcademico> = emptyMap(), offered: Set<String> = ms.map { it.id }.toSet()) =
        MotorRecomendaciones().calcular(AcademicData("u", "plan", ms, requirements, states), Oferta("plan", offered, 1))
    @Test fun earlierPendingBeatsMoreUnlocksAndAlternativesKeepGroups() {
        val ms = listOf(m("A"), m("B", 3), m("C", 4), m("D", 4), m("E", 2), m("F", 1))
        val result = calculate(ms, listOf(Prerrequisito("C", "B"), Prerrequisito("D", "B")), mapOf("E" to EstadoAcademico.EN_CURSO), setOf("A", "B", "F"))
        assertEquals(listOf("A", "F", "B"), result.candidatas.map { it.materia.id })
        assertEquals(2, result.semestreReferencia)
        assertEquals(2, result.candidatas.last().desbloqueadas.size)
    }
    @Test fun noActiveHistoryDoesNotInferEarlierPendingAndKeepsZero() {
        val result = calculate(listOf(m("A"), m("B", 2), m("C", 3)), listOf(Prerrequisito("C", "B")))
        assertNull(result.semestreReferencia); assertTrue(result.pendientesAnteriores.isEmpty())
        assertEquals(listOf("B", "A"), result.candidatas.map { it.materia.id })
        assertTrue(result.candidatas.last().desbloqueadas.isEmpty())
    }
    @Test fun unavailableEarlierPendingYieldsAvailableAlternative() {
        val result = calculate(listOf(m("A"), m("B", 2), m("C", 3)), states = mapOf("B" to EstadoAcademico.APROBADA), offered = setOf("C"))
        assertTrue(result.alternativaDisponible); assertEquals("C", result.principal?.materia?.id)
        assertEquals(listOf("A"), result.pendientesAnteriores.map { it.id })
    }
    @Test fun tieBreaksBySemesterThenCode() {
        val result = calculate(listOf(m("C", 2), m("B"), m("A")))
        assertEquals(listOf("A", "B", "C"), result.candidatas.map { it.materia.id })
    }
    @Test fun largestUnlockWinsWithinEarlierPendingGroup() {
        val result = calculate(listOf(m("A"), m("B", 2), m("C", 3), m("D", 3), m("E", 3), m("F", 4)),
            listOf(Prerrequisito("C", "A"), Prerrequisito("D", "B"), Prerrequisito("E", "B")),
            mapOf("F" to EstadoAcademico.EN_CURSO), setOf("A", "B"))
        assertEquals("B", result.principal?.materia?.id)
        assertEquals(listOf("D", "E"), result.principal?.desbloqueadas?.map { it.id })
    }
    @Test fun multipleRequirementsSimulatedIndependentlyAndChainExcluded() {
        val ms = listOf(m("A"), m("B"), m("C"), m("D"))
        val req = listOf(Prerrequisito("C", "A"), Prerrequisito("C", "B"), Prerrequisito("D", "C"))
        val initial = calculate(ms, req, offered = setOf("A", "B"))
        assertTrue(initial.candidatas.all { it.desbloqueadas.isEmpty() })
        val result = calculate(ms, req, mapOf("B" to EstadoAcademico.APROBADA), setOf("A", "B", "C"))
        assertEquals(listOf("A"), result.candidatas.map { it.materia.id })
        assertEquals(listOf("C"), result.principal?.desbloqueadas?.map { it.id })
    }
    @Test fun approvedAndInCourseExcludedAndAlreadyEnabledNotCounted() {
        val result = calculate(listOf(m("A"), m("B"), m("C"), m("D")), states = mapOf("A" to EstadoAcademico.APROBADA, "B" to EstadoAcademico.EN_CURSO))
        assertEquals(listOf("C", "D"), result.candidatas.map { it.materia.id })
        assertTrue(result.candidatas.all { it.desbloqueadas.isEmpty() })
    }
    @Test fun threshold44To45CountsImmediateNonOfferedUnlockWithoutCountingSelf() {
        val ms = (0..48).map { m("M$it") } + m("STS-300", 9, 45)
        val states = ms.take(44).associate { it.id to EstadoAcademico.APROBADA }
        val copy = states.toMap()
        val result = calculate(ms, states = states, offered = setOf("M44"))
        assertEquals(listOf("STS-300"), result.principal?.desbloqueadas?.map { it.id })
        assertEquals(copy, states)
        val ready = calculate(ms, states = states + ("M44" to EstadoAcademico.APROBADA), offered = setOf("STS-300"))
        assertEquals(45, ready.principal?.umbralSatisfecho); assertTrue(ready.principal!!.desbloqueadas.isEmpty())
    }
    @Test fun incoherentHistoryRejected() {
        try { calculate(listOf(m("A"), m("B")), listOf(Prerrequisito("B", "A")), mapOf("B" to EstadoAcademico.APROBADA)); fail() } catch (_: IllegalArgumentException) {}
    }
    @Test fun unknownOfferedIdRejected() {
        try { calculate(listOf(m("A")), offered = setOf("foreign")); fail() } catch (_: IllegalArgumentException) {}
    }

    @Test fun partialProgressIncludesApprovedAndMissingRequirementsWithoutChains() {
        val ms = listOf(m("A"), m("B"), m("C", 2), m("D", 3))
        val req = listOf(Prerrequisito("C", "A"), Prerrequisito("C", "B"), Prerrequisito("D", "C"))
        val states = mapOf("B" to EstadoAcademico.EN_CURSO)
        val result = calculate(ms, req, states, setOf("A", "B", "C"))
        val avance = result.principal!!.avancesParciales.single()
        assertEquals("C", avance.materia.id)
        assertEquals(listOf("A"), avance.requisitosAprobados.map { it.id })
        assertEquals(listOf("B"), avance.requisitosPendientes.map { it.id })
        assertEquals(0, avance.aprobadasFaltantes)
        assertTrue(result.principal!!.desbloqueadas.isEmpty())
        assertEquals(listOf("A"), result.candidatas.map { it.materia.id })
        assertEquals(mapOf("B" to EstadoAcademico.EN_CURSO), states)
    }

    @Test fun bothPendingSimulatedSeparatelyThenPartialBecomesComplete() {
        val ms = listOf(m("A"), m("B"), m("C", 2))
        val req = listOf(Prerrequisito("C", "A"), Prerrequisito("C", "B"))
        val initial = calculate(ms, req, offered = setOf("A", "B"))
        assertEquals("B", initial.secundaria!!.materia.id)
        initial.candidatas.forEach { candidate ->
            assertEquals(listOf(candidate.materia.id), candidate.avancesParciales.single().requisitosAprobados.map { it.id })
            assertTrue(candidate.desbloqueadas.isEmpty())
        }
        val later = calculate(ms, req, mapOf("B" to EstadoAcademico.APROBADA), setOf("A"))
        assertEquals(listOf("C"), later.principal!!.desbloqueadas.map { it.id })
        assertTrue(later.principal!!.avancesParciales.isEmpty())
    }

    @Test fun fullUnlockBeatsPartialAndPartialBreaksTiesBeforeSemester() {
        val ms = listOf(m("A"), m("B", 2), m("C", 3), m("X", 4), m("Y", 4), m("Z", 4))
        val req = listOf(Prerrequisito("X", "B"), Prerrequisito("X", "Z"), Prerrequisito("Y", "C"))
        val result = calculate(ms, req, offered = setOf("A", "B", "C"))
        assertEquals(listOf("C", "B", "A"), result.candidatas.map { it.materia.id })
        assertEquals("B", result.secundaria!!.materia.id)
        assertEquals(listOf("A"), result.opcionales.map { it.materia.id })
    }

    @Test fun morePartialTargetsWinsBeforeSemesterAndCode() {
        val ms = listOf(m("A"), m("B", 2), m("C", 3), m("D", 3), m("E", 3))
        val req = listOf(Prerrequisito("C", "A"), Prerrequisito("C", "B"),
            Prerrequisito("D", "B"), Prerrequisito("D", "E"))
        val result = calculate(ms, req, offered = setOf("A", "B"))
        assertEquals("B", result.principal!!.materia.id)
        assertEquals(2, result.principal!!.avancesParciales.size)
        assertEquals("A", result.secundaria!!.materia.id)
        assertEquals(1, result.secundaria!!.avancesParciales.size)
    }

    @Test fun earlierPendingStillBeatsPartialAndSecondaryCanSkipZeroBenefit() {
        val ms = listOf(m("A"), m("B"), m("C", 3), m("D", 4), m("E", 4), m("F", 2))
        val result = calculate(ms, listOf(Prerrequisito("D", "C"), Prerrequisito("D", "E")),
            mapOf("F" to EstadoAcademico.EN_CURSO), setOf("A", "B", "C"))
        assertEquals(listOf("A", "B", "C"), result.candidatas.map { it.materia.id })
        assertEquals("C", result.secundaria!!.materia.id)
        assertEquals(listOf("B"), result.opcionales.map { it.materia.id })
    }

    @Test fun zeroBenefitSingleAndEmptyHaveNoInventedSecondary() {
        val ms = listOf(m("A"), m("B"), m("C"))
        val all = calculate(ms)
        assertNull(all.secundaria)
        assertEquals(listOf("B", "C"), all.opcionales.map { it.materia.id })
        val single = calculate(ms, offered = setOf("A"))
        assertNotNull(single.principal); assertNull(single.secundaria); assertTrue(single.opcionales.isEmpty())
        val empty = calculate(ms, offered = emptySet())
        assertNull(empty.principal); assertNull(empty.secundaria); assertTrue(empty.opcionales.isEmpty())
        assertEquals(all.candidatas.map { it.materia.id }.toSet(),
            (listOfNotNull(all.principal, all.secundaria) + all.opcionales).map { it.materia.id }.toSet())
    }

    @Test fun thresholdsOnlyCountAsPartialForExplicitDirectRequirements() {
        val ms = listOf(m("A"), m("B"), m("C", 3, 3), m("T", 3, 3), m("E"))
        val result = calculate(ms, listOf(Prerrequisito("C", "A")), offered = setOf("A", "B"))
        val a = result.principal!!
        assertEquals("A", a.materia.id)
        val avance = a.avancesParciales.single()
        assertEquals("C", avance.materia.id); assertEquals(2, avance.aprobadasFaltantes)
        assertTrue(avance.requisitosPendientes.isEmpty())
        assertTrue(result.candidatas.single { it.materia.id == "B" }.avancesParciales.isEmpty())
    }

    @Test fun partialTargetsSortDeterministicallyAndPreserveUnrelatedActiveStates() {
        val ms = listOf(m("A"), m("B"), m("X", 3), m("Y", 2), m("Z", 4))
        val req = listOf(Prerrequisito("X", "A"), Prerrequisito("X", "B"),
            Prerrequisito("Y", "A"), Prerrequisito("Y", "B"))
        val result = calculate(ms, req, offered = setOf("A"))
        assertEquals(listOf("Y", "X"), result.principal!!.avancesParciales.map { it.materia.id })
        val active = calculate(ms, req, mapOf("Z" to EstadoAcademico.EN_CURSO), setOf("A"))
        assertEquals(listOf("Y", "X"), active.principal!!.avancesParciales.map { it.materia.id })
    }

    @Test fun studentOfferRecommendsEnglishThenQualityAndKeepsThreeOptional() {
        val ms = listOf(m("RIG-361", 7), m("RIG-362", 8), m("SIS-305", 7), m("SCC-305", 8),
            m("SIS-306", 9), m("SCC-303", 8), m("LEG", 7), m("TPPY-301", 8))
        val req = listOf(Prerrequisito("RIG-362", "RIG-361"), Prerrequisito("SIS-306", "SIS-305"),
            Prerrequisito("SIS-306", "SCC-305"))
        val offered = setOf("RIG-361", "SIS-305", "SCC-303", "LEG", "TPPY-301")
        val result = calculate(ms, req, offered = offered)
        assertEquals("RIG-361", result.principal!!.materia.id)
        assertEquals("SIS-305", result.secundaria!!.materia.id)
        assertEquals(listOf("SCC-305"), result.secundaria!!.avancesParciales.single().requisitosPendientes.map { it.id })
        assertEquals(3, result.opcionales.size)
        val displayed = listOfNotNull(result.principal, result.secundaria) + result.opcionales
        assertEquals(5, displayed.map { it.materia.id }.distinct().size)
        val later = calculate(ms, req, mapOf("SCC-305" to EstadoAcademico.APROBADA), offered)
        val quality = later.candidatas.single { it.materia.id == "SIS-305" }
        assertEquals(listOf("SIS-306"), quality.desbloqueadas.map { it.id })
        assertTrue(quality.avancesParciales.isEmpty())
    }

    @Test fun oneApprovalCanUnlockAndPartiallyAdvanceDifferentTargets() {
        val ms = listOf(m("A"), m("B"), m("C", 2), m("D", 2))
        val req = listOf(Prerrequisito("C", "A"), Prerrequisito("D", "A"), Prerrequisito("D", "B"))
        val candidate = calculate(ms, req, offered = setOf("A")).principal!!
        assertEquals(listOf("C"), candidate.desbloqueadas.map { it.id })
        assertEquals(listOf("D"), candidate.avancesParciales.map { it.materia.id })
        assertTrue(candidate.desbloqueadas.map { it.id }.intersect(candidate.avancesParciales.map { it.materia.id }.toSet()).isEmpty())
    }
}

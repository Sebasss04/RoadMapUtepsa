package com.example.roadmap_utepsa.offer

import androidx.test.platform.app.InstrumentationRegistry
import com.example.roadmap_utepsa.domain.academic.AcademicData
import com.example.roadmap_utepsa.domain.academic.AreaMateria
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.Prerrequisito
import org.json.JSONObject
internal fun ofertaFixture(): AcademicData {
    val json = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
        .open("sistemas-rm-615-2016.json").bufferedReader().use { it.readText() })
    val rows = json.getJSONArray("materias")
    val relations = json.getJSONArray("prerrequisitos")
    return AcademicData("offer-test", json.getString("id_plan"), (0 until rows.length()).map { i ->
        val r = rows.getJSONObject(i)
        Materia(r.getString("id"), r.getString("id_plan"), r.getString("codigo"), r.getString("nombre"),
            r.getInt("semestre"), AreaMateria.valueOf(r.getString("area")), r.getInt("min_aprobadas"), r.getInt("horas_totales"))
    }, (0 until relations.length()).map { i -> val r = relations.getJSONObject(i)
        Prerrequisito(r.getString("id_objetivo"), r.getString("id_requisito")) }, emptyMap())
}

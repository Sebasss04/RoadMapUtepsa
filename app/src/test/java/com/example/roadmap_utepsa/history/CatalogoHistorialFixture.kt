package com.example.roadmap_utepsa.history

import com.example.roadmap_utepsa.domain.academic.AreaMateria
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.Prerrequisito
import java.io.File

/** Lee exclusivamente el fixture JSON plano aprobado; no es un parser de producción. */
internal class CatalogoHistorialFixture {
    private val archivo = listOf(
        File("../firebase/catalogo/sistemas-rm-615-2016.json"),
        File("firebase/catalogo/sistemas-rm-615-2016.json")
    ).first { it.isFile }
    private val texto = archivo.readText(Charsets.UTF_8)
    private fun filas(nombre: String): List<String> {
        val bloque = Regex("\"$nombre\"\\s*:\\s*\\[([\\s\\S]*?)\\]")
            .find(texto)!!.groupValues[1]
        return Regex("\\{([^{}]*)\\}").findAll(bloque).map { it.groupValues[1] }.toList()
    }
    private fun String.campo(nombre: String): String =
        Regex("\"$nombre\"\\s*:\\s*\"([^\"]*)\"").find(this)!!.groupValues[1]
    private fun String.numero(nombre: String): Int =
        Regex("\"$nombre\"\\s*:\\s*(\\d+)").find(this)!!.groupValues[1].toInt()

    val materias = filas("materias").map { fila ->
        Materia(fila.campo("id"), fila.campo("id_plan"), fila.campo("codigo"),
            fila.campo("nombre"), fila.numero("semestre"), AreaMateria.valueOf(fila.campo("area")),
            fila.numero("min_aprobadas"), fila.numero("horas_totales"))
    }
    val prerrequisitos = filas("prerrequisitos").map { fila ->
        Prerrequisito(fila.campo("id_objetivo"), fila.campo("id_requisito"))
    }
    init {
        check(materias.size == 50 && prerrequisitos.size == 28)
    }
    fun id(codigo: String) = materias.single { it.codigo == codigo }.id
}

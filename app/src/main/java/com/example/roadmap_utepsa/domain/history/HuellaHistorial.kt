package com.example.roadmap_utepsa.domain.history

import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import java.security.MessageDigest

/** Identifica un conjunto para reconciliar una operación; no sustituye los estados. */
fun huellaHistorial(estados: Map<String, EstadoAcademico>, ids: Set<String>): String {
    require(estados.keys.all { it in ids })
    val texto = ids.sorted().joinToString("\n") { "$it=${(estados[it] ?: EstadoAcademico.PENDIENTE).name}" }
    return MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}

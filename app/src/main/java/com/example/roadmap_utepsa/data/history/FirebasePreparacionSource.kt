package com.example.roadmap_utepsa.data.history

import com.example.roadmap_utepsa.data.academic.EstadosVersionados
import com.example.roadmap_utepsa.domain.history.RegistroPreparacion
import com.example.roadmap_utepsa.domain.history.OperacionHistorial

import com.example.roadmap_utepsa.domain.history.CambioEstadoHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** Adaptador sin conexión a navegación: todas las decisiones se toman en el repositorio. */
internal class FirebasePreparacionSource(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PreparacionSource {
    override val session: Any? get() = auth.currentUser
    override val currentUid: String? get() = auth.currentUser?.uid

    private val versionados = EstadosVersionados(db, auth)

    override suspend fun leer(uid: String, ids: Set<String>): InstantaneaPreparacion {
        val identidad = capturarSesion(uid, ids)
        val (estados, documentos) = versionados.leer(uid, ids) {
            coroutineScope {
                val registro = async { configuracion(uid).get(Source.SERVER).await().also(::confirmado) }
                val operacion = async { diario(uid).get(Source.SERVER).await().also(::confirmado) }
                registro.await() to operacion.await()
            }
        }
        comprobarSesion(uid, identidad)
        return InstantaneaPreparacion(estados, leerRegistro(documentos.first), leerOperacion(documentos.second, ids))
    }

    private fun confirmado(doc: DocumentSnapshot) {
        check(!doc.metadata.isFromCache && !doc.metadata.hasPendingWrites())
    }

    override suspend fun transaccion(
        uid: String,
        ids: Set<String>,
        bloque: (InstantaneaPreparacion) -> EscrituraPreparacion
    ): EscrituraPreparacion {
        val identidad = capturarSesion(uid, ids)
        val confirmada = versionados.transaccion(uid, ids) { tx, estados ->
            comprobarSesion(uid, identidad)
            // La revisión cubre también estados ausentes y cambios de otros escritores.
            val lectura = InstantaneaPreparacion(estados, leerRegistro(tx.get(configuracion(uid))),
                leerOperacion(tx.get(diario(uid)), ids))
            val escritura = bloque(lectura)
            require(escritura.estados.keys.all { it in ids }) { "Escritura de materia desconocida" }
            val operacion = escritura.operacion?.let { serializarOperacion(it, ids) }
            comprobarSesion(uid, identidad)
            escritura.estados.forEach { (id, estado) ->
                val referencia = this.estado(uid, id)
                if (estado == EstadoAcademico.PENDIENTE) tx.delete(referencia)
                else tx.set(referencia, mapOf("estado" to estado.name))
            }
            escritura.decision?.let {
                tx.set(configuracion(uid), mapOf(
                    "version_formato" to 1L,
                    "decision" to it.name,
                    "actualizado_en" to FieldValue.serverTimestamp()
                ))
            }
            operacion?.let { tx.set(diario(uid), it) }
            escritura to escritura.estados
        }
        comprobarSesion(uid, identidad)
        return confirmada
    }

    private fun capturarSesion(uid: String, ids: Set<String>): Any {
        require(uid.isNotBlank() && '/' !in uid) { "UID inválido" }
        require(ids.size == 50 && ids.all(::esUuid)) { "Se requiere el catálogo completo" }
        return checkNotNull(session) { "No hay sesión" }.also { comprobarSesion(uid, it) }
    }

    private fun comprobarSesion(uid: String, identidad: Any) {
        check(session === identidad && currentUid == uid) { "La sesión cambió" }
    }

    private fun estudiante(uid: String) = db.collection("estudiantes").document(uid)
    private fun estado(uid: String, id: String): DocumentReference =
        estudiante(uid).collection("estados").document(id)
    private fun configuracion(uid: String) =
        estudiante(uid).collection("configuracion_academica").document("historial")
    private fun diario(uid: String) =
        estudiante(uid).collection("operaciones_historial").document("actual")

    private fun leerRegistro(doc: DocumentSnapshot): RegistroPreparacion? {
        if (!doc.exists()) return null
        val campos = checkNotNull(doc.data)
        require(campos.keys == setOf("version_formato", "decision", "actualizado_en")) {
            "Campos de preparación inválidos"
        }
        require(campos["version_formato"] == 1L) { "Versión de preparación inválida" }
        val decision = DecisionPreparacion.valueOf(campos["decision"] as? String
            ?: error("Decisión de preparación inválida"))
        val fecha = campos["actualizado_en"] as? Timestamp ?: error("Fecha de preparación inválida")
        return RegistroPreparacion(decision, "${fecha.seconds}:${fecha.nanoseconds}")
    }

    private fun leerOperacion(doc: DocumentSnapshot, ids: Set<String>): OperacionHistorial? {
        if (!doc.exists()) return null
        val campos = checkNotNull(doc.data)
        require(campos.keys == setOf("version_formato", "id_operacion", "huella_base",
            "huella_objetivo", "revision_preparacion", "cambios", "aplicados", "estado", "actualizado_en")) {
            "Campos de operación inválidos"
        }
        require(campos["version_formato"] == 1L) { "Versión de operación inválida" }
        require(campos["actualizado_en"] is Timestamp) { "Fecha de operación inválida" }
        fun texto(nombre: String) = campos[nombre] as? String ?: error("Tipo inválido: $nombre")
        val cambios = campos["cambios"] as? Map<*, *> ?: error("Cambios inválidos")
        val decodificados = cambios.entries.associate { (id, codigo) ->
            require(id is String && id in ids) { "Materia de operación inválida" }
            id to decodificar(codigo as? String ?: error("Código de cambio inválido"))
        }
        val aplicados = campos["aplicados"] as? List<*> ?: error("Aplicados inválidos")
        require(aplicados.all { it is String } && aplicados.distinct().size == aplicados.size) {
            "Aplicados repetidos o de tipo inválido"
        }
        val estado = texto("estado")
        require(estado in setOf("EN_PROGRESO", "COMPLETADA", "CANCELADA")) { "Estado de operación inválido" }
        val revision = texto("revision_preparacion")
        return OperacionHistorial(
            id = texto("id_operacion"), huellaBase = texto("huella_base"),
            huellaObjetivo = texto("huella_objetivo"),
            revisionPreparacion = revision.takeUnless { it == "AUSENTE" },
            cambios = decodificados,
            aplicados = aplicados.map { it as String }.toSet(),
            completada = estado == "COMPLETADA", cancelada = estado == "CANCELADA"
        ).also { validarOperacion(it, ids) }
    }

    private fun serializarOperacion(op: OperacionHistorial, ids: Set<String>): Map<String, Any> {
        validarOperacion(op, ids)
        return mapOf(
            "version_formato" to 1L,
            "id_operacion" to op.id,
            "huella_base" to op.huellaBase,
            "huella_objetivo" to op.huellaObjetivo,
            "revision_preparacion" to (op.revisionPreparacion ?: "AUSENTE"),
            "cambios" to op.cambios.mapValues { (_, cambio) ->
                "${codigo(cambio.anterior)}${codigo(cambio.nuevo)}"
            },
            "aplicados" to op.aplicados.sorted(),
            "estado" to when {
                op.cancelada -> "CANCELADA"
                op.completada -> "COMPLETADA"
                else -> "EN_PROGRESO"
            },
            "actualizado_en" to FieldValue.serverTimestamp()
        )
    }

    private fun validarOperacion(op: OperacionHistorial, ids: Set<String>) {
        require(esUuid(op.id)) { "ID de operación inválido" }
        require(listOf(op.huellaBase, op.huellaObjetivo).all { it.matches(Regex("[a-f0-9]{64}")) }) {
            "Huella inválida"
        }
        op.revisionPreparacion?.let { revision ->
            require(revision.matches(Regex("-?\\d+:\\d{1,9}"))) { "Revisión inválida" }
            val partes = revision.split(':')
            require(partes[0].toLongOrNull() != null && partes[1].toInt() in 0..999_999_999) {
                "Revisión fuera de rango"
            }
        }
        require(op.cambios.keys.all { it in ids } && op.aplicados.all { it in op.cambios }) {
            "Materias de operación inválidas"
        }
        require(op.cambios.values.all { it.anterior != it.nuevo }) { "Operación sin cambio académico" }
        require(!(op.completada && op.cancelada)) { "Operación con estados incompatibles" }
        require(!op.completada || op.aplicados == op.cambios.keys) { "Operación completada incompleta" }
    }

    private fun esUuid(id: String): Boolean =
        runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)

    private fun codigo(estado: EstadoAcademico): Char = when (estado) {
        EstadoAcademico.PENDIENTE -> 'P'
        EstadoAcademico.EN_CURSO -> 'C'
        EstadoAcademico.APROBADA -> 'A'
    }

    private fun decodificar(codigo: String): CambioEstadoHistorial {
        require(codigo in setOf("PA", "PC", "AP", "AC", "CP", "CA")) { "Código de cambio inválido" }
        fun estado(letra: Char) = when (letra) {
            'P' -> EstadoAcademico.PENDIENTE
            'C' -> EstadoAcademico.EN_CURSO
            else -> EstadoAcademico.APROBADA
        }
        return CambioEstadoHistorial(estado(codigo[0]), estado(codigo[1]))
    }
}

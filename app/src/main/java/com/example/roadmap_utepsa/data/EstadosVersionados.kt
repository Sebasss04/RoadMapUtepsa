package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Todos los escritores incrementan control_academico/revision en el mismo commit.
 * Las reglas lo exigen también a clientes antiguos. Así una consulta completa
 * más una revisión sustituye las 50 lecturas seriales dentro de cada transacción.
 * No almacena estados en disco ni acepta caché pendiente como prueba remota.
 */
internal class EstadosVersionados(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    private data class Snapshot(val uid: String, val session: Any, val ids: Set<String>,
                                val revision: Long, val estados: Map<String, EstadoAcademico>)
    private class RevisionCambiada : IllegalStateException()
    private val mutex = Mutex()
    private var snapshot: Snapshot? = null
    private fun referencia(uid: String) = db.document("estudiantes/$uid/control_academico/revision")
    private fun sesion(uid: String): Any {
        require(uid.isNotBlank() && '/' !in uid)
        return checkNotNull(auth.currentUser).also { check(auth.currentUser?.uid == uid) }
    }
    private fun comprobar(uid: String, session: Any) {
        check(auth.currentUser === session && auth.currentUser?.uid == uid) { "La sesión cambió" }
    }
    private fun revision(doc: DocumentSnapshot): Long {
        if (!doc.exists()) return 0
        val data = checkNotNull(doc.data)
        require(data.keys == setOf("version_formato", "valor") && data["version_formato"] == 1L)
        return (data["valor"] as? Long ?: error("Revisión inválida")).also {
            require(it in 1 until Long.MAX_VALUE) { "Revisión fuera de rango" }
        }
    }
    private suspend fun revisionServidor(uid: String): Long {
        val doc = referencia(uid).get(Source.SERVER).await()
        check(!doc.metadata.isFromCache && !doc.metadata.hasPendingWrites())
        return revision(doc)
    }
    private suspend fun <T> leerEstable(uid: String, ids: Set<String>, session: Any,
                                       extra: suspend () -> T): Pair<Snapshot, T> {
        require(ids.size == 50)
        repeat(3) {
            comprobar(uid, session)
            val antes = revisionServidor(uid)
            val (estados, adicionales) = coroutineScope {
                val estados = async {
                    val docs = db.collection("estudiantes").document(uid).collection("estados")
                        .get(Source.SERVER).await()
                    check(!docs.metadata.isFromCache && !docs.metadata.hasPendingWrites())
                    AcademicDataMapper.estados(docs.documents.map {
                        AcademicDocument(it.id, checkNotNull(it.data))
                    }, ids)
                }
                val adicionales = async { extra() }
                estados.await() to adicionales.await()
            }
            val despues = revisionServidor(uid)
            comprobar(uid, session)
            if (antes == despues) return Snapshot(uid, session, ids.toSet(), antes, estados) to adicionales
        }
        error("El historial sigue cambiando; vuelve a comprobar el resultado")
    }
    suspend fun <T> leer(uid: String, ids: Set<String>, extra: suspend () -> T): Pair<Map<String, EstadoAcademico>, T> =
        mutex.withLock {
            val (actual, adicionales) = leerEstable(uid, ids, sesion(uid), extra)
            snapshot = actual
            actual.estados to adicionales
        }

    /** El bloque retorna resultado y deltas. Los efectos externos quedan fuera del callback SDK. */
    suspend fun <T> transaccion(uid: String, ids: Set<String>,
        bloque: (Transaction, Map<String, EstadoAcademico>) -> Pair<T, Map<String, EstadoAcademico>>): T = mutex.withLock {
        val session = sesion(uid)
        repeat(3) {
            var base = snapshot
            if (base == null || base.uid != uid || base.session !== session || base.ids != ids) {
                base = leerEstable(uid, ids, session) { Unit }.first
                snapshot = base
            }
            val esperada = base
            try {
                val commit = db.runTransaction { tx ->
                    comprobar(uid, session)
                    val actual = revision(tx.get(referencia(uid)))
                    if (actual != esperada.revision) throw RevisionCambiada()
                    val resultado = bloque(tx, esperada.estados)
                    comprobar(uid, session)
                    tx.set(referencia(uid), mapOf("version_formato" to 1L, "valor" to actual + 1))
                    resultado
                }.await()
                comprobar(uid, session)
                snapshot = esperada.copy(revision = esperada.revision + 1,
                    estados = (esperada.estados + commit.second).filterValues { it != EstadoAcademico.PENDIENTE })
                return@withLock commit.first
            } catch (e: Exception) {
                snapshot = null // Incluye respuesta perdida: nunca predecir un commit.
                if (generateSequence<Throwable>(e) { it.cause }.none { it is RevisionCambiada }) throw e
            }
        }
        error("El historial sigue cambiando; vuelve a comprobar el resultado")
    }
}

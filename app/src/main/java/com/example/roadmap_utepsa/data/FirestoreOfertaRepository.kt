package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class FirestoreOfertaRepository(
    private val academic: AcademicRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : OfertaRepository {
    private fun identity(uid: String): () -> Unit {
        val user = checkNotNull(auth.currentUser)
        check(user.uid == uid)
        return { check(auth.currentUser === user && auth.currentUser?.uid == uid) { "La sesión cambió" } }
    }
    private fun ref(uid: String) = db.document("estudiantes/$uid/ofertas/actual")
    private fun decode(doc: DocumentSnapshot, plan: String, ids: Set<String>): Oferta? {
        if (!doc.exists()) return null
        val data = checkNotNull(doc.data)
        check(data.keys == setOf("version_formato", "id_plan", "materias", "revision", "actualizado_en"))
        check(data["version_formato"] == 1L && data["id_plan"] == plan)
        val list = data["materias"] as? List<*> ?: error("Selección inválida")
        check(list.size <= 50 && list.all { it is String && it in ids } && list.toSet().size == list.size)
        check(data["actualizado_en"] is com.google.firebase.Timestamp)
        return Oferta(plan, list.map { it as String }.toSet(), data["revision"] as? Long ?: error("Revisión inválida"))
    }
    private suspend fun read(uid: String, plan: String, ids: Set<String>): Oferta? {
        val doc = ref(uid).get(Source.SERVER).await()
        check(!doc.metadata.isFromCache && !doc.metadata.hasPendingWrites())
        return decode(doc, plan, ids)
    }
    override suspend fun load(uid: String): DatosOferta {
        val checkSession = identity(uid)
        checkSession()
        val data = academic.load(uid)
        checkSession()
        check(data.uid == uid && data.materias.size == 50)
        val offer = read(uid, data.idPlan, data.materias.map { it.id }.toSet())
        checkSession()
        return DatosOferta(uid, data.idPlan, data.materias, offer, data.estados)
    }
    override suspend fun save(uid: String, materias: Set<String>, expectedRevision: Long?): Oferta {
        val checkSession = identity(uid)
        checkSession()
        val data = academic.load(uid)
        checkSession()
        val ids = data.materias.map { it.id }.toSet()
        require(data.uid == uid && ids.size == 50 && materias.size <= 50 && ids.containsAll(materias))
        require(expectedRevision == null || expectedRevision in 1 until Long.MAX_VALUE - 1)
        val selected = materias.toSet()
        val next = (expectedRevision ?: 0) + 1
        try {
            db.runTransaction { tx ->
                checkSession()
                val current = decode(tx.get(ref(uid)), data.idPlan, ids)
                if (current?.revision != expectedRevision) throw ConflictoOferta(current)
                tx.set(ref(uid), mapOf("version_formato" to 1L, "id_plan" to data.idPlan,
                    "materias" to selected.sorted(), "revision" to next,
                    "actualizado_en" to FieldValue.serverTimestamp()))
            }.await()
            checkSession()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            checkSession()
            val conflict = generateSequence<Throwable>(e) { it.cause }.filterIsInstance<ConflictoOferta>().firstOrNull()
            if (conflict != null) throw conflict
            // A transport failure may follow a successful commit. Never blindly resend.
            val remote = try { read(uid, data.idPlan, ids) } catch (e: CancellationException) { throw e } catch (readError: Exception) {
                checkSession(); throw ResultadoOfertaIncierto(readError)
            }
            checkSession()
            if (remote?.revision == next && remote.materias == selected) return remote
            if (remote?.revision != expectedRevision) throw ConflictoOferta(remote)
            throw e
        }
        val remote = try { read(uid, data.idPlan, ids) } catch (e: CancellationException) { throw e } catch (e: Exception) {
            checkSession(); throw ResultadoOfertaIncierto(e)
        }
        checkSession()
        if (remote?.revision != next || remote.materias != selected) throw ConflictoOferta(remote)
        return remote
    }
}

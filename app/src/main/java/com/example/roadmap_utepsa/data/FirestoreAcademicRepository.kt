package com.example.roadmap_utepsa.data

import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.model.AcademicData
import com.example.roadmap_utepsa.domain.model.ProfileRegistration
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await

/** Límite concreto de lectura para probar la coordinación sin simular clases del SDK. */
internal interface AcademicReadSource {
    val currentUid: String?
    val session: Any? get() = currentUid
    suspend fun materias(plan: String): AcademicDocuments
    suspend fun requisitos(idMateria: String): AcademicDocuments
    suspend fun estados(uid: String): AcademicDocuments
    suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico)
    suspend fun saveStateComparado(uid: String, idMateria: String, estado: EstadoAcademico,
                                  ids: Set<String>, expectedStates: Map<String, EstadoAcademico>) {
        throw UnsupportedOperationException("La fuente no implementa comparación transaccional")
    }
}

private class FirebaseAcademicReadSource(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : AcademicReadSource {
    private val versionados = EstadosVersionados(db, auth)
    override val currentUid get() = auth.currentUser?.uid
    override val session: Any? get() = auth.currentUser
    private suspend fun read(query: Query): AcademicDocuments {
        val snapshot = query.get(Source.SERVER).await()
        check(snapshot.documents.none { it.metadata.isFromCache || it.metadata.hasPendingWrites() })
        return AcademicDocuments(snapshot.documents.map { AcademicDocument(it.id, it.data.orEmpty()) },
            snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
    }
    override suspend fun materias(plan: String) = read(db.collection("materias").whereEqualTo("id_plan", plan))
    override suspend fun requisitos(idMateria: String) = read(db.collection("materias").document(idMateria).collection("prerrequisitos"))
    override suspend fun estados(uid: String) = read(db.collection("estudiantes").document(uid).collection("estados"))
    override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico) {
        val documents = materias(ProfileStudyPlan.PLAN_ID).confirmed()
        val ids = AcademicDataMapper.materias(documents, ProfileStudyPlan.PLAN_ID).map { it.id }.toSet()
        require(idMateria in ids)
        versionados.transaccion(uid, ids) { tx, _ ->
            val doc = db.document("estudiantes/$uid/estados/$idMateria")
            if (estado == EstadoAcademico.PENDIENTE) tx.delete(doc)
            else tx.set(doc, mapOf("estado" to estado.name))
            Unit to mapOf(idMateria to estado)
        }
    }

    override suspend fun saveStateComparado(uid: String, idMateria: String, estado: EstadoAcademico,
                                           ids: Set<String>, expectedStates: Map<String, EstadoAcademico>) {
        require(ids.size == 50 && idMateria in ids && expectedStates.keys.all { it in ids })
        val identidad = checkNotNull(session) { "La sesión cambió" }
        fun comprobarSesion() = check(currentUid == uid && session === identidad) { "La sesión cambió" }
        val esperados = expectedStates.filterValues { it != EstadoAcademico.PENDIENTE }.toMap()
        comprobarSesion()
        val collection = db.collection("estudiantes").document(uid).collection("estados")
        versionados.transaccion(uid, ids) { tx, actuales ->
            comprobarSesion()
            if (actuales != esperados) throw ConflictoEstadoAcademico(actuales)
            val destino = collection.document(idMateria)
            if (estado == EstadoAcademico.PENDIENTE) tx.delete(destino)
            else tx.set(destino, mapOf("estado" to estado.name))
            Unit to mapOf(idMateria to estado)
        }
        comprobarSesion()
    }
}

class FirestoreAcademicRepository internal constructor(
    private val source: AcademicReadSource,
    private val profileRepository: ProfileRepository,
    private val relojNanos: () -> Long = System::nanoTime
) : AcademicRepository {
    constructor(profileRepository: ProfileRepository,
                db: FirebaseFirestore = FirebaseFirestore.getInstance(),
                auth: FirebaseAuth = FirebaseAuth.getInstance()) :
        this(FirebaseAcademicReadSource(db, auth), profileRepository)

    private data class Catalogo(val session: Any, val plan: String, val creado: Long,
                                val data: AcademicData)
    private val catalogoMutex = Mutex()
    private var catalogo: Catalogo? = null
    private val catalogoTtlNanos = 5L * 60 * 1_000_000_000

    private fun checkIdentity(uid: String) {
        check(uid.isNotBlank() && source.currentUid == uid) { "La sesión cambió" }
    }

    override suspend fun load(uid: String): AcademicData = coroutineScope {
        checkIdentity(uid)
        val profile = profileRepository.load(uid)
        checkIdentity(uid)
        check(profile.registration == ProfileRegistration.COMPLETE) { "Completa el perfil antes de consultar la malla" }
        val plan = profile.plan.id
        check(plan == ProfileStudyPlan.PLAN_ID) { "Plan académico no admitido" }
        val identidad = checkNotNull(source.session)
        val statesRead = async { source.estados(uid).confirmed() }
        val catalog = catalogoMutex.withLock {
            val guardado = catalogo
            val ahora = relojNanos()
            if (guardado != null && guardado.session === identidad && guardado.plan == plan &&
                ahora - guardado.creado in 0 until catalogoTtlNanos) {
                guardado.data
            } else {
                catalogo = null
                val documents = source.materias(plan).confirmed()
                val subjects = AcademicDataMapper.materias(documents, plan)
                val permits = Semaphore(5)
                val requirements = subjects.map { materia -> async {
                    permits.withPermit {
                        checkIdentity(uid)
                        materia.id to source.requisitos(materia.id).confirmed()
                    }
                } }.awaitAll().toMap()
                val validado = AcademicDataMapper.assemble(uid, plan, subjects, requirements, emptyList())
                checkIdentity(uid)
                check(source.session === identidad) { "La sesión cambió" }
                catalogo = Catalogo(identidad, plan, relojNanos(), validado)
                validado
            }
        }
        val result = catalog.copy(uid = uid, estados = AcademicDataMapper.estados(statesRead.await(),
            catalog.materias.map { it.id }.toSet()))
        checkIdentity(uid)
        check(source.session === identidad) { "La sesión cambió" }
        result
    }

    override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico): Map<String, EstadoAcademico> {
        checkIdentity(uid)
        val profile = profileRepository.load(uid)
        check(profile.registration == ProfileRegistration.COMPLETE && profile.plan.id == ProfileStudyPlan.PLAN_ID)
        val materias = AcademicDataMapper.materias(source.materias(profile.plan.id).confirmed(), profile.plan.id)
        val ids = materias.map { it.id }.toSet()
        require(idMateria in ids) { "Materia desconocida" }
        checkIdentity(uid)
        source.saveState(uid, idMateria, estado)
        checkIdentity(uid)
        val states = AcademicDataMapper.estados(source.estados(uid).confirmed(), ids)
        check((states[idMateria] ?: EstadoAcademico.PENDIENTE) == estado) { "El estado guardado no pudo confirmarse" }
        checkIdentity(uid)
        return states
    }

    override suspend fun saveState(uid: String, idMateria: String, estado: EstadoAcademico,
                                   expectedStates: Map<String, EstadoAcademico>): Map<String, EstadoAcademico> {
        val identidad = checkNotNull(source.session) { "La sesión cambió" }
        fun comprobarSesion() {
            checkIdentity(uid)
            check(source.session === identidad) { "La sesión cambió" }
        }
        val esperados = expectedStates.filterValues { it != EstadoAcademico.PENDIENTE }.toMap()
        comprobarSesion()
        val academic = load(uid)
        comprobarSesion()
        val ids = academic.materias.map { it.id }.toSet()
        require(idMateria in ids && expectedStates.keys.all { it in ids }) { "Materia desconocida" }
        if (academic.estados != esperados) throw ConflictoEstadoAcademico(academic.estados)
        val transicion = ReglasHistorial(academic.materias, academic.prerrequisitos)
            .evaluarTransicion(idMateria, estado, academic.estados)
        require(transicion.permitida) { "La transición introduce o agrava contradicciones" }
        comprobarSesion()
        source.saveStateComparado(uid, idMateria, estado, ids, esperados)
        comprobarSesion()
        val confirmados = AcademicDataMapper.estados(source.estados(uid).confirmed(), ids)
        comprobarSesion()
        check((confirmados[idMateria] ?: EstadoAcademico.PENDIENTE) == estado) {
            "El estado guardado no pudo confirmarse"
        }
        return confirmados
    }
}

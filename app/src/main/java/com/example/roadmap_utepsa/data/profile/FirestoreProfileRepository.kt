package com.example.roadmap_utepsa.data.profile

import com.example.roadmap_utepsa.domain.profile.ProfileRepository

import com.example.roadmap_utepsa.domain.profile.AssignedStudyPlan
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.domain.profile.ProfileRegistration
import com.example.roadmap_utepsa.domain.profile.validProfileName
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/** The two reference IDs are fixed for v1 and are also enforced in firestore.rules. */
object ProfileStudyPlan {
    const val PLAN_ID = "sistemas-rm-615-2016"
    const val CAREER_ID = "ingenieria-sistemas"
}

class FirestoreProfileRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ProfileRepository {
    private data class Registration(
        val user: DocumentSnapshot,
        val person: DocumentSnapshot,
        val student: DocumentSnapshot
    )

    private fun checkIdentity(uid: String) {
        check(auth.currentUser?.uid == uid) { "La sesión cambió. Vuelve a iniciar sesión." }
    }

    private suspend fun serverDocument(path: String): DocumentSnapshot {
        val snapshot = db.document(path).get(Source.SERVER).await()
        // SERVER may still overlay a local queued write. It must not count as remote proof.
        check(!snapshot.metadata.hasPendingWrites()) { "Hay cambios pendientes de confirmación." }
        check(!snapshot.metadata.isFromCache) { "No se recibió confirmación del servidor." }
        return snapshot
    }

    private suspend fun registration(uid: String): Registration = coroutineScope {
        val user = async { serverDocument("usuarios/$uid") }
        val person = async { serverDocument("personas/$uid") }
        val student = async { serverDocument("estudiantes/$uid") }
        Registration(user.await(), person.await(), student.await())
    }

    private suspend fun assignedPlan(): AssignedStudyPlan {
        val plan = serverDocument("planes_estudio/${ProfileStudyPlan.PLAN_ID}")
        val career = serverDocument("carreras/${ProfileStudyPlan.CAREER_ID}")
        check(plan.exists() && career.exists()) { "No está configurado el plan asignado." }
        check(plan.getString("id_carrera") == ProfileStudyPlan.CAREER_ID)
        check(plan.getString("resolucion") == "615/2016")
        val name = checkNotNull(career.getString("nombre"))
        check(name.isNotBlank())
        return AssignedStudyPlan(ProfileStudyPlan.PLAN_ID, name, "615/2016")
    }

    private fun profile(data: Registration, plan: AssignedStudyPlan): ProfileData {
        val names = data.person.getString("nombres").orEmpty()
        val surnames = data.person.getString("apellidos").orEmpty()
        val studentPlan = data.student.getString("id_plan")
        check(studentPlan == null || studentPlan == plan.id) { "El perfil tiene otro plan asignado." }
        val status = when {
            !data.user.exists() && !data.person.exists() && !data.student.exists() -> ProfileRegistration.ABSENT
            data.user.getTimestamp("fecha_creacion") != null &&
                validProfileName(names) && validProfileName(surnames) && studentPlan == plan.id -> ProfileRegistration.COMPLETE
            else -> ProfileRegistration.INCOMPLETE
        }
        return ProfileData(names, surnames, plan, status)
    }

    override suspend fun load(uid: String): ProfileData {
        checkIdentity(uid)
        val plan = assignedPlan()
        val data = registration(uid)
        checkIdentity(uid)
        return profile(data, plan)
    }

    override suspend fun save(uid: String, names: String, surnames: String): ProfileData {
        checkIdentity(uid)
        require(validProfileName(names) && validProfileName(surnames))
        val plan = assignedPlan()
        val data = registration(uid)
        profile(data, plan) // Refuse a silent migration from a different plan.
        checkIdentity(uid)
        val batch = db.batch()
        if (!data.user.exists()) {
            batch.set(db.document("usuarios/$uid"), mapOf("fecha_creacion" to FieldValue.serverTimestamp()))
        } else {
            check(data.user.getTimestamp("fecha_creacion") != null) { "El registro de cuenta requiere revisión." }
        }
        batch.set(db.document("personas/$uid"), mapOf("nombres" to names.trim(), "apellidos" to surnames.trim()))
        if (data.student.getString("id_plan") == null) {
            batch.set(db.document("estudiantes/$uid"), mapOf("id_plan" to plan.id))
        }
        // Initial registration creates all three documents together. Editing keeps creation date and plan.
        batch.commit().await()
        checkIdentity(uid)
        return ProfileData(names.trim(), surnames.trim(), plan, ProfileRegistration.COMPLETE)
    }
}

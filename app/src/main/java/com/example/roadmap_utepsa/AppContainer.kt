package com.example.roadmap_utepsa

import com.example.roadmap_utepsa.data.OfertaRepository
import com.example.roadmap_utepsa.data.FirestoreOfertaRepository
import com.example.roadmap_utepsa.data.AuthRepository
import com.example.roadmap_utepsa.data.FirebaseAuthRepository
import com.example.roadmap_utepsa.data.FirestoreProfileRepository
import com.example.roadmap_utepsa.data.ProfileRepository
import com.example.roadmap_utepsa.data.AcademicRepository
import com.example.roadmap_utepsa.data.FirestoreAcademicRepository
import com.example.roadmap_utepsa.data.PreparacionRepository
import com.example.roadmap_utepsa.data.FirestorePreparacionRepository

/** Process-wide repositories. Firebase must be initialized before constructing this container. */
class AppContainer {
    val authRepository: AuthRepository = FirebaseAuthRepository()
    val profileRepository: ProfileRepository = FirestoreProfileRepository()
    val academicRepository: AcademicRepository = FirestoreAcademicRepository(profileRepository)
    val ofertaRepository: OfertaRepository = FirestoreOfertaRepository(academicRepository)
    val preparacionRepository: PreparacionRepository = FirestorePreparacionRepository(academicRepository)
    private val recommendationRevision = com.example.roadmap_utepsa.data.EstadosVersionados(
        com.google.firebase.firestore.FirebaseFirestore.getInstance(), com.google.firebase.auth.FirebaseAuth.getInstance())
    val recomendacionesRepository: com.example.roadmap_utepsa.data.RecomendacionesRepository =
        com.example.roadmap_utepsa.data.ConsultaRecomendacionesRepository(preparacionRepository, ofertaRepository,
            recommendationRevision::revisionConsulta) { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
}

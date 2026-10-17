package com.example.roadmap_utepsa

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
    val preparacionRepository: PreparacionRepository = FirestorePreparacionRepository(academicRepository)
}

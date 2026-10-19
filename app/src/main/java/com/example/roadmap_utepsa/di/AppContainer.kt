package com.example.roadmap_utepsa.di

import androidx.lifecycle.viewmodel.initializer
import com.example.roadmap_utepsa.domain.offer.OfertaRepository
import com.example.roadmap_utepsa.data.offer.FirestoreOfertaRepository
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.data.auth.FirebaseAuthRepository
import com.example.roadmap_utepsa.data.profile.FirestoreProfileRepository
import com.example.roadmap_utepsa.domain.profile.ProfileRepository
import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.data.academic.FirestoreAcademicRepository
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.data.history.FirestorePreparacionRepository

/** Process-wide repositories. Firebase must be initialized before constructing this container. */
class AppContainer(private val context: android.content.Context) {
    fun googleAuth(activityContext: android.content.Context) =
        com.example.roadmap_utepsa.platform.auth.GoogleAuthHelper(activityContext)

    val authRepository: AuthRepository = FirebaseAuthRepository()
    val profileRepository: ProfileRepository = FirestoreProfileRepository()
    val academicRepository: AcademicRepository = FirestoreAcademicRepository(profileRepository)
    val ofertaRepository: OfertaRepository = FirestoreOfertaRepository(academicRepository)
    val preparacionRepository: PreparacionRepository = FirestorePreparacionRepository(academicRepository)
    private val recommendationRevision = com.example.roadmap_utepsa.data.academic.EstadosVersionados(
        com.google.firebase.firestore.FirebaseFirestore.getInstance(), com.google.firebase.auth.FirebaseAuth.getInstance())
    val recomendacionesRepository: com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository =
        com.example.roadmap_utepsa.data.recommendation.ConsultaRecomendacionesRepository(preparacionRepository, ofertaRepository,
            recommendationRevision::revisionConsulta) { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }

    fun sessionLockFactory() = androidx.lifecycle.viewmodel.viewModelFactory {
        initializer {
            com.example.roadmap_utepsa.presentation.session.SessionLock(
                com.example.roadmap_utepsa.data.session.DeviceLockPreferences(context))
        }
    }
    fun remindersFactory(session: () -> Pair<String?, Long>) = androidx.lifecycle.viewmodel.viewModelFactory {
        initializer {
            com.example.roadmap_utepsa.presentation.notification.PendingReminders(recomendacionesRepository,
                com.example.roadmap_utepsa.data.notification.DeviceReminderPreferences(context),
                com.example.roadmap_utepsa.platform.notification.AndroidReminderDelivery(context), session)
        }
    }
}

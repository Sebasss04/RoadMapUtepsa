package com.example.roadmap_utepsa.data.history

import com.example.roadmap_utepsa.domain.academic.AcademicRepository
import com.example.roadmap_utepsa.domain.history.PreparacionRepository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Adaptador productivo compartido por AppContainer y el gate de historial. */
class FirestorePreparacionRepository(
    academico: AcademicRepository,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) : PreparacionRepository by CoordinadorPreparacionRepository(
    academico, FirebasePreparacionSource(db, auth), 5
)

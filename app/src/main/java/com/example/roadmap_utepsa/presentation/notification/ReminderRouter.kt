package com.example.roadmap_utepsa.presentation.notification

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.roadmap_utepsa.domain.notification.ReminderKind
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.platform.notification.ReminderIntents

/** A consumed launch intent must not return after rotation/process recreation. */
class ReminderRouter(private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(saved.get<String>(ReminderIntents.UID)?.let { uid ->
        ReminderKind.entries.find { it.name == saved.get<String>(ReminderIntents.KIND) }?.let { ReminderRoute(uid, it) }
    })
    val route = mutable.asStateFlow()
    fun receive(intent: Intent?) { set(ReminderIntents.parse(intent)) }
    fun consume() { set(null) }
    private fun set(route: ReminderRoute?) {
        saved[ReminderIntents.UID] = route?.uid; saved[ReminderIntents.KIND] = route?.kind?.name
        mutable.value = route
    }
}

package com.example.roadmap_utepsa.data.notification

import com.example.roadmap_utepsa.domain.notification.ReminderMemory
import com.example.roadmap_utepsa.domain.notification.ReminderPreferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.reminderStore by preferencesDataStore(name = "pending_reminders")
class DeviceReminderPreferences(context: Context) : ReminderPreferences {
    private val store = context.applicationContext.reminderStore
    private fun Preferences.memory(uid: String) = ReminderMemory(
        this[booleanPreferencesKey("$uid:enabled")] ?: false,
        this[booleanPreferencesKey("$uid:history")] ?: false,
        this[booleanPreferencesKey("$uid:offer")] ?: false)
    override suspend fun read(uid: String) = store.data.first().memory(uid)
    override suspend fun update(uid: String, change: (ReminderMemory) -> ReminderMemory): ReminderMemory {
        val result = store.edit { prefs ->
            val next = change(prefs.memory(uid))
            prefs[booleanPreferencesKey("$uid:enabled")] = next.enabled
            prefs[booleanPreferencesKey("$uid:history")] = next.historySeen
            prefs[booleanPreferencesKey("$uid:offer")] = next.offerSeen
        }
        return result.memory(uid)
    }
}

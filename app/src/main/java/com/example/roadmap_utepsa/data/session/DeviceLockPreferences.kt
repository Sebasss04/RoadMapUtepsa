package com.example.roadmap_utepsa.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*

import com.example.roadmap_utepsa.domain.session.LockPreferences

private val Context.sessionLockStore by preferencesDataStore(name = "session_lock")
class DeviceLockPreferences(context: Context) : LockPreferences {
    private val store = context.applicationContext.sessionLockStore
    override suspend fun enabled(uid: String) = store.data.first()[booleanPreferencesKey(uid)] ?: false
    override suspend fun setEnabled(uid: String, enabled: Boolean) {
        store.edit { it[booleanPreferencesKey(uid)] = enabled }
    }
}

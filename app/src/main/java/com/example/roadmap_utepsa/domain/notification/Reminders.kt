package com.example.roadmap_utepsa.domain.notification

enum class ReminderKind { HISTORY, OFFER }
data class ReminderRoute(val uid: String, val kind: ReminderKind)
data class ReminderMemory(val enabled: Boolean = false, val historySeen: Boolean = false, val offerSeen: Boolean = false) {
    fun seen(kind: ReminderKind) = if (kind == ReminderKind.HISTORY) historySeen else offerSeen
    fun mark(kind: ReminderKind, value: Boolean) = if (kind == ReminderKind.HISTORY) copy(historySeen = value) else copy(offerSeen = value)
}
interface ReminderPreferences {
    suspend fun read(uid: String): ReminderMemory
    suspend fun update(uid: String, change: (ReminderMemory) -> ReminderMemory): ReminderMemory
}
interface ReminderDelivery {
    fun available(): Boolean
    fun show(route: ReminderRoute): Boolean
    fun cancel()
    fun keepOnly(route: ReminderRoute?)
}

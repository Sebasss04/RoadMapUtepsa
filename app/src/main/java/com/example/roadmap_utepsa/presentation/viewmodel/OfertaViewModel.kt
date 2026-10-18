package com.example.roadmap_utepsa.presentation.viewmodel

import androidx.lifecycle.*
import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.example.roadmap_utepsa.presentation.academic.filterCurriculum

data class OfertaUiState(
    val loading: Boolean = true, val data: DatosOferta? = null,
    val editing: Boolean = false, val draft: Set<String> = emptySet(), val query: String = "",
    val saving: Boolean = false, val saved: Boolean = false, val error: Boolean = false,
    val conflict: Boolean = false, val remote: Oferta? = null, val uncertain: Boolean = false,
    val expired: Boolean = false, val approvedRemoved: Boolean = false, val refreshFailed: Boolean = false
) {
    val dirty get() = editing && draft != data?.oferta?.materias.orEmpty()
    val visible get() = data?.let {
        filterCurriculum(AcademicData(it.uid, it.idPlan, it.catalogo.filter { subject -> subject.id !in it.aprobadas },
            emptyList(), it.estados), query, null, null)
    }.orEmpty()
}
class OfertaViewModel(private val uid: String, private val sessionVersion: Long,
    private val repository: OfertaRepository, private val auth: AuthRepository,
    private val currentSession: () -> Long) : ViewModel() {
    private val mutable = MutableStateFlow(OfertaUiState())
    val state = mutable.asStateFlow()
    private var expired = false
    private var readJob: Job? = null
    init {
        viewModelScope.launch { auth.uidChanges.collect { if (it != uid) expire() } }
        reload()
    }
    private fun expire() { expired = true; mutable.value = OfertaUiState(loading = false, expired = true) }
    private fun valid(): Boolean {
        if (expired || auth.currentUid != uid || currentSession() != sessionVersion) { expire(); return false }
        return true
    }
    fun reload() {
        if (!valid() || state.value.saving || state.value.uncertain || readJob?.isActive == true) return
        mutable.value = if (state.value.editing) state.value.copy(loading = true, refreshFailed = false) else OfertaUiState()
        readJob = viewModelScope.launch {
            try {
                val data = repository.load(uid)
                ensureActive()
                check(data.uid == uid)
                if (valid()) {
                    val s = state.value
                    if (s.editing) {
                        val draft = data.sinAprobadas(s.draft)
                        // Keep the offer revision being edited: a remote offer change must still conflict on save.
                        mutable.value = s.copy(loading = false, data = data.copy(oferta = s.data?.oferta), draft = draft,
                            approvedRemoved = s.approvedRemoved || draft != s.draft, refreshFailed = false)
                    } else mutable.value = OfertaUiState(loading = false, data = data)
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { if (valid()) mutable.value = if (state.value.editing)
                state.value.copy(loading = false, refreshFailed = true) else OfertaUiState(loading = false, error = true) }
        }
    }
    fun edit() {
        val s = state.value
        val data = s.data ?: return
        if (valid() && !s.saving && !s.loading) {
            val original = data.oferta?.materias.orEmpty()
            val draft = data.sinAprobadas(original)
            mutable.value = s.copy(editing = true, draft = draft, saved = false, error = false,
                approvedRemoved = draft != original, refreshFailed = false)
        }
    }
    fun query(value: String) { if (valid()) mutable.value = state.value.copy(query = value) }
    fun toggle(id: String) {
        val s = state.value
        if (!valid() || !s.editing || s.loading || s.refreshFailed || s.saving || s.conflict || s.uncertain ||
            s.data?.catalogo?.none { it.id == id } != false || id in s.data?.aprobadas.orEmpty()) return
        mutable.value = s.copy(draft = if (id in s.draft) s.draft - id else s.draft + id, error = false)
    }
    fun cancel() { if (valid() && !state.value.saving) mutable.value = state.value.copy(
        editing = false, draft = emptySet(), query = "", error = false, conflict = false, remote = null, uncertain = false,
        approvedRemoved = false, refreshFailed = false) }
    /** Remote adoption and explicit draft retry are separate user decisions. */
    fun resolveConflict(keepDraft: Boolean) {
        val s = state.value
        if (!valid() || !s.conflict || s.saving) return
        val original = if (keepDraft) s.draft else s.remote?.materias.orEmpty()
        val draft = s.data?.sinAprobadas(original) ?: original
        mutable.value = s.copy(data = s.data?.copy(oferta = s.remote),
            draft = draft, approvedRemoved = s.approvedRemoved || draft != original,
            conflict = false, remote = null, error = false, uncertain = false)
        if (keepDraft) save()
    }
    fun save() {
        val s = state.value
        if (!valid() || !s.editing || s.loading || s.refreshFailed || s.saving || s.conflict || s.data == null) return
        mutable.value = s.copy(saving = true, error = false, saved = false)
        viewModelScope.launch {
            try {
                if (s.uncertain) {
                    val remote = repository.load(uid).oferta
                    ensureActive()
                    if (!valid()) return@launch
                    if (remote?.revision == (s.data.oferta?.revision ?: 0) + 1 && remote.materias == s.draft) {
                        mutable.value = s.copy(data = s.data.copy(oferta = remote), editing = false,
                            saving = false, saved = true, uncertain = false); return@launch
                    }
                    if (remote?.revision != s.data.oferta?.revision) throw ConflictoOferta(remote)
                }
                // Refresh academic states before sending a normal save. Never mutate an uncertain selection before reconciliation.
                val latest = repository.load(uid)
                ensureActive()
                if (!valid()) return@launch
                check(latest.uid == uid)
                val draft = latest.sinAprobadas(s.draft)
                if (draft != s.draft) {
                    mutable.value = s.copy(data = latest.copy(oferta = s.data.oferta), draft = draft,
                        saving = false, approvedRemoved = true, uncertain = false)
                    return@launch
                }
                val result = repository.save(uid, s.draft, s.data.oferta?.revision)
                ensureActive()
                if (valid()) mutable.value = s.copy(data = latest.copy(oferta = result), editing = false,
                    saving = false, saved = true, uncertain = false, query = "")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                if (valid()) {
                    val conflict = generateSequence<Throwable>(e) { it.cause }.filterIsInstance<ConflictoOferta>().firstOrNull()
                    mutable.value = s.copy(saving = false, error = conflict == null,
                        conflict = conflict != null, remote = conflict?.remota,
                        uncertain = s.uncertain || e is ResultadoOfertaIncierto)
                }
            }
        }
    }
    class Factory(private val uid: String, private val version: Long, private val repo: OfertaRepository,
        private val auth: AuthRepository, private val session: () -> Long) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OfertaViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return OfertaViewModel(uid, version, repo, auth, session) as T
        }
    }
    companion object { fun key(uid: String, version: Long) = "oferta:$uid:$version" }
}

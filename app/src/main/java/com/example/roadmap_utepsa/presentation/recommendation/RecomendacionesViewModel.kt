package com.example.roadmap_utepsa.presentation.recommendation

import androidx.lifecycle.*
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.recommendation.RecomendacionesRepository
import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.recommendation.ResultadoRecomendaciones
import com.example.roadmap_utepsa.domain.recommendation.MotorRecomendaciones
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class EstadoRecomendaciones { CARGANDO, ERROR, HISTORIAL_PENDIENTE, HISTORIAL_INCOHERENTE,
    RECUPERACION_PENDIENTE, OFERTA_AUSENTE, OFERTA_VACIA, SIN_CANDIDATAS, LISTO, SESION_CADUCADA }
data class RecomendacionesUiState(val estado: EstadoRecomendaciones = EstadoRecomendaciones.CARGANDO,
    val resultado: ResultadoRecomendaciones? = null, val ofertaRevision: Long? = null,
    val revisionAcademica: Long? = null)

class RecomendacionesViewModel(private val uid: String, private val version: Long,
    private val repository: RecomendacionesRepository, private val auth: AuthRepository,
    private val currentVersion: () -> Long) : ViewModel() {
    private val mutable = MutableStateFlow(RecomendacionesUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var generation = 0L
    private var expired = false
    init { viewModelScope.launch { auth.uidChanges.collect { if (it != uid) expire() } } }
    private fun expire() {
        expired = true; generation++; job?.cancel()
        mutable.value = RecomendacionesUiState(EstadoRecomendaciones.SESION_CADUCADA)
    }
    private fun valid(): Boolean {
        if (expired || auth.currentUid != uid || currentVersion() != version) { expire(); return false }
        return true
    }
    fun reload() {
        if (!valid()) return
        job?.cancel()
        val request = ++generation
        mutable.value = RecomendacionesUiState()
        job = viewModelScope.launch {
            try {
                val data = repository.load(uid)
                ensureActive()
                check(data.preparacion.academic.uid == uid)
                if (!valid() || request != generation) return@launch
                val h = data.preparacion
                val academic = h.academic
                val operation = h.operacion
                val estado = when {
                    operation != null && !operation.completada && !operation.cancelada -> EstadoRecomendaciones.RECUPERACION_PENDIENTE
                    h.registro?.decision != DecisionPreparacion.CONFIRMADO -> EstadoRecomendaciones.HISTORIAL_PENDIENTE
                    ReglasHistorial(academic.materias, academic.prerrequisitos).validar(academic.estados).contradicciones.isNotEmpty() -> EstadoRecomendaciones.HISTORIAL_INCOHERENTE
                    data.oferta == null -> EstadoRecomendaciones.OFERTA_AUSENTE
                    data.oferta.materias.isEmpty() -> EstadoRecomendaciones.OFERTA_VACIA
                    else -> EstadoRecomendaciones.LISTO
                }
                val result = if (estado == EstadoRecomendaciones.LISTO) MotorRecomendaciones().calcular(academic, checkNotNull(data.oferta)) else null
                mutable.value = RecomendacionesUiState(if (result?.candidatas?.isEmpty() == true) EstadoRecomendaciones.SIN_CANDIDATAS else estado,
                    result, data.oferta?.revision, data.revisionAcademica)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { if (valid() && request == generation) mutable.value = RecomendacionesUiState(EstadoRecomendaciones.ERROR) }
        }
    }
    fun leave() { generation++; job?.cancel(); mutable.value = RecomendacionesUiState() }
    class Factory(private val uid: String, private val version: Long, private val repo: RecomendacionesRepository,
        private val auth: AuthRepository, private val session: () -> Long) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RecomendacionesViewModel::class.java))
            @Suppress("UNCHECKED_CAST") return RecomendacionesViewModel(uid, version, repo, auth, session) as T
        }
    }
    companion object { fun key(uid: String, version: Long) = "recomendaciones:$uid:$version" }
}

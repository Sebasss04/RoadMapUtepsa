package com.example.roadmap_utepsa.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roadmap_utepsa.data.*
import com.example.roadmap_utepsa.domain.academic.DiagnosticoMateriaHistorial
import com.example.roadmap_utepsa.domain.academic.ReglasHistorial
import com.example.roadmap_utepsa.domain.history.BorradorHistorial
import com.example.roadmap_utepsa.domain.history.DecisionPreparacion
import com.example.roadmap_utepsa.domain.history.huellaHistorial
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.domain.model.EstadoAcademico.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PasoHistorial { BIENVENIDA, APROBADAS, EN_CURSO, REVISION }
enum class ModoHistorial { PREPARACION, CONSULTA, EDICION }
enum class EstadoPreparacionUi { CARGANDO, ERROR, CONFIGURACION_PENDIENTE, CONFIRMADO, REQUIERE_REVISION, GUARDADO_PENDIENTE }

sealed interface HistorialUiState {
    data object Loading : HistorialUiState
    data object Error : HistorialUiState
    data object SessionExpired : HistorialUiState
    data class Ready(
        val lectura: LecturaPreparacion,
        val borrador: BorradorHistorial,
        val paso: PasoHistorial = PasoHistorial.BIENVENIDA,
        val guardando: Boolean = false,
        val esperando: Boolean = false,
        val errorGuardado: Boolean = false,
        val progresoAplicado: Int = 0,
        val progresoTotal: Int = 0,
        val conflicto: LecturaPreparacion? = null,
        val confirmado: Boolean = false,
        val pospuesto: Boolean = false,
        val diagnosticos: List<DiagnosticoMateriaHistorial> = emptyList(),
        val recuperacionPendiente: Boolean = false,
        val modo: ModoHistorial = ModoHistorial.PREPARACION
    ) : HistorialUiState
}

/** Conserva borradores durante recreación de pantalla; no los persiste tras cerrar el proceso. */
class HistorialViewModel(
    private val uid: String,
    private val repository: PreparacionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow<HistorialUiState>(HistorialUiState.Loading)
    val state = mutableState.asStateFlow()
    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var expired = false
    private var posponerPendiente = false
    private var consultarAlCargar = false

    init {
        require(uid.isNotBlank())
        viewModelScope.launch { authRepository.uidChanges.collect { if (it != uid) expirar() } }
        reload()
    }

    private fun sesionValida(): Boolean {
        if (expired || authRepository.currentUid != uid) { expirar(); return false }
        return true
    }

    private fun expirar() {
        expired = true
        loadJob?.cancel()
        saveJob?.cancel()
        mutableState.value = HistorialUiState.SessionExpired
    }

    fun reload() {
        if (!sesionValida() || loadJob?.isActive == true || saveJob?.isActive == true) return
        // Nunca usar recarga como una acción implícita de descarte del borrador.
        if (state.value is HistorialUiState.Ready) return
        mutableState.value = HistorialUiState.Loading
        loadJob = viewModelScope.launch {
            try {
                val lectura = repository.load(uid)
                verificar(lectura)
                if (sesionValida()) mutableState.value = nuevo(lectura)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { if (sesionValida()) mutableState.value = HistorialUiState.Error }
        }
    }

    /** Entrada explícita desde Malla o descarte ya confirmado por el estudiante. */
    fun iniciarRevision() {
        if (!sesionValida() || loadJob?.isActive == true || saveJob?.isActive == true) return
        consultarAlCargar = true
        mutableState.value = HistorialUiState.Loading
        reload()
    }

    fun editar() {
        val actual = editable() ?: return
        mutableState.value = actual.copy(modo = ModoHistorial.EDICION, paso = PasoHistorial.APROBADAS)
    }

    fun cancelarEdicion() {
        val actual = editable() ?: return
        mutableState.value = nuevo(actual.lectura).copy(modo = ModoHistorial.CONSULTA)
    }

    private fun verificar(lectura: LecturaPreparacion) { check(lectura.academic.uid == uid) }
    private fun reglas(lectura: LecturaPreparacion) =
        ReglasHistorial(lectura.academic.materias, lectura.academic.prerrequisitos)
    private fun activa(lectura: LecturaPreparacion) =
        lectura.operacion?.let { !it.completada && !it.cancelada } == true
    private fun nuevo(lectura: LecturaPreparacion) = HistorialUiState.Ready(
        lectura, BorradorHistorial(uid, lectura.academic.estados),
        diagnosticos = reglas(lectura).validar(lectura.academic.estados).contradicciones,
        recuperacionPendiente = activa(lectura),
        modo = if (consultarAlCargar || lectura.registro != null) ModoHistorial.CONSULTA else ModoHistorial.PREPARACION
    )
    private fun editable(): HistorialUiState.Ready? {
        if (!sesionValida()) return null
        val actual = state.value as? HistorialUiState.Ready ?: return null
        return actual.takeUnless { it.guardando || it.conflicto != null || it.recuperacionPendiente || it.confirmado || it.pospuesto }
    }

    fun abrirPaso(paso: PasoHistorial) {
        val actual = editable() ?: return
        mutableState.value = actual.copy(paso = if (paso == PasoHistorial.BIENVENIDA && actual.modo != ModoHistorial.PREPARACION)
            PasoHistorial.APROBADAS else paso,
            modo = if (actual.modo == ModoHistorial.CONSULTA) ModoHistorial.EDICION else actual.modo)
    }

    fun seleccionarAprobada(id: String, checked: Boolean) {
        val actual = editable() ?: return
        if (actual.lectura.academic.materias.none { it.id == id }) return
        val destino = if (checked) APROBADA else if (actual.borrador.base[id] == EN_CURSO) EN_CURSO else PENDIENTE
        mutableState.value = actual.copy(borrador = actual.borrador.seleccionar(id, destino), diagnosticos = emptyList())
    }

    fun seleccionarEnCurso(id: String, checked: Boolean) {
        val actual = editable() ?: return
        if (actual.lectura.academic.materias.none { it.id == id }) return
        if (actual.borrador.seleccion[id] == APROBADA) return
        val destino = if (checked) EN_CURSO else PENDIENTE
        val resultado = reglas(actual.lectura).evaluarTransicion(id, destino, actual.borrador.seleccion)
        mutableState.value = if (!resultado.permitida) actual.copy(diagnosticos = resultado.bloqueos)
        else actual.copy(borrador = actual.borrador.seleccionar(id, destino), diagnosticos = emptyList())
    }

    fun revisar() {
        val actual = editable() ?: return
        mutableState.value = actual.copy(paso = PasoHistorial.REVISION,
            diagnosticos = actual.borrador.validar(reglas(actual.lectura)).contradicciones)
    }

    /** Omitir el paso no retira materias en curso que ya estaban guardadas. */
    fun omitirNuevasEnCurso() {
        val actual = editable() ?: return
        var borrador = actual.borrador
        actual.borrador.seleccion.forEach { (id, estado) ->
            if (estado == EN_CURSO && actual.borrador.base[id] != EN_CURSO) {
                borrador = borrador.seleccionar(id, PENDIENTE)
            }
        }
        mutableState.value = actual.copy(borrador = borrador, paso = PasoHistorial.REVISION,
            diagnosticos = borrador.validar(reglas(actual.lectura)).contradicciones)
    }

    fun confirmar() {
        val actual = editable() ?: return
        val validacion = actual.borrador.validar(reglas(actual.lectura))
        if (!validacion.coherente) {
            mutableState.value = actual.copy(paso = PasoHistorial.REVISION, diagnosticos = validacion.contradicciones)
            return
        }
        posponerPendiente = false
        ejecutar(actual) { repository.confirmar(actual.lectura, actual.borrador, ::progreso) }
    }

    fun posponer() {
        val actual = editable() ?: return
        if (actual.modo != ModoHistorial.PREPARACION || actual.lectura.registro?.decision == DecisionPreparacion.CONFIRMADO) return
        posponerPendiente = true
        ejecutar(actual) { repository.posponer(actual.lectura) }
    }

    private fun progreso(aplicado: Int, total: Int) {
        if (!sesionValida()) return
        val actual = state.value as? HistorialUiState.Ready ?: return
        if (actual.guardando) mutableState.value = actual.copy(progresoAplicado = aplicado, progresoTotal = total)
    }

    private fun ejecutar(actual: HistorialUiState.Ready, operacion: suspend () -> LecturaPreparacion) {
        if (saveJob?.isActive == true || !sesionValida()) return
        mutableState.value = actual.copy(guardando = true, esperando = false, errorGuardado = false)
        saveJob = viewModelScope.launch {
            val aviso = launch {
                delay(10_000)
                val vigente = state.value as? HistorialUiState.Ready
                if (vigente?.guardando == true && sesionValida()) mutableState.value = vigente.copy(esperando = true)
            }
            try {
                val resultado = operacion()
                verificar(resultado)
                if (sesionValida()) mutableState.value = nuevo(resultado).copy(
                    paso = PasoHistorial.REVISION,
                    confirmado = resultado.registro?.decision == DecisionPreparacion.CONFIRMADO &&
                        reglas(resultado).validar(resultado.academic.estados).coherente && !activa(resultado),
                    pospuesto = resultado.registro?.decision == DecisionPreparacion.POSPUESTO
                )
            } catch (e: CancellationException) { throw e
            } catch (e: SinCambiosRemotos) {
                if (sesionValida()) mutableState.value = actual.copy(lectura = e.lectura,
                    errorGuardado = false, recuperacionPendiente = false, conflicto = null)
            } catch (e: Exception) {
                registrarError(actual, e)
            } finally { aviso.cancel() }
        }
    }

    private fun registrarError(actual: HistorialUiState.Ready, error: Exception) {
        if (!sesionValida()) return
        // Firestore puede envolver la excepción del bloque de una transacción.
        val conflicto = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<ConflictoHistorial>().firstOrNull()
        if (conflicto != null && conflicto.remoto.academic.uid != uid) {
            expirar()
            return
        }
        mutableState.value = (state.value as? HistorialUiState.Ready ?: actual).copy(
            guardando = false, esperando = false, errorGuardado = true,
            recuperacionPendiente = true, conflicto = conflicto?.remoto ?: actual.conflicto)
    }

    /** Relee antes de reintentar. No convierte un error en permiso de sobrescritura. */
    fun leerConflicto() {
        val actual = state.value as? HistorialUiState.Ready ?: return
        if (actual.guardando || actual.conflicto == null || !sesionValida() || saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            try {
                val remoto = repository.load(uid)
                verificar(remoto)
                if (sesionValida()) mutableState.value = actual.copy(conflicto = remoto)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { if (sesionValida()) mutableState.value = actual.copy(errorGuardado = true) }
        }
    }

    /** Reconciliación explícita; puede continuar la operación que el usuario solicita recuperar. */
    fun recuperarResultado() {
        val actual = state.value as? HistorialUiState.Ready ?: return
        if (actual.guardando || !sesionValida()) return
        ejecutar(actual) {
            val remoto = repository.load(uid)
            verificar(remoto)
            val op = remoto.operacion
            val ids = actual.lectura.academic.materias.map { it.id }.toSet()
            val mismaOperacion = op != null &&
                op.huellaBase == huellaHistorial(actual.borrador.base, ids) &&
                op.huellaObjetivo == huellaHistorial(actual.borrador.seleccion, ids) &&
                op.revisionPreparacion == actual.lectura.registro?.revision
            if (activa(remoto)) {
                if (mismaOperacion || (actual.lectura.operacion?.id == op?.id && actual.borrador.cambios().isEmpty()))
                    repository.reanudar(uid, op!!.id, ::progreso)
                else throw ConflictoHistorial(remoto)
            } else if (mismaOperacion && op?.completada == true) {
                // El repositorio comprueba objetivo, coherencia y marcador al recuperar.
                repository.reanudar(uid, op.id, ::progreso)
            } else {
                if (posponerPendiente && remoto.registro?.decision == DecisionPreparacion.POSPUESTO &&
                    actual.borrador.diferenciasRemotas(remoto.academic.estados).isEmpty() &&
                    remoto.operacion == actual.lectura.operacion) return@ejecutar remoto
                if (actual.borrador.diferenciasRemotas(remoto.academic.estados).isNotEmpty() ||
                    remoto.registro != actual.lectura.registro || remoto.operacion != actual.lectura.operacion)
                    throw ConflictoHistorial(remoto)
                // Sin escritura detectada: conservar selección y pedir reintento explícito.
                throw SinCambiosRemotos(remoto)
            }
        }
    }

    /** Solo se llama después de confirmar en UI el descarte de la selección no enviada. */
    fun descartarBorradorYUsarRemoto() = resolver(null)

    /** Selección revisada explícitamente frente al remoto mostrado, sin mezcla automática. */
    fun resolverConflictoConSeleccion(seleccionRevisada: Map<String, EstadoAcademico>) = resolver(seleccionRevisada.toMap())

    private fun resolver(seleccion: Map<String, EstadoAcademico>?) {
        val actual = state.value as? HistorialUiState.Ready ?: return
        val remoto = actual.conflicto ?: return
        if (actual.guardando || !sesionValida()) return
        if (seleccion != null) require(seleccion.keys.all { id -> remoto.academic.materias.any { it.id == id } })
        ejecutarResolucion(actual, remoto, seleccion)
    }

    private fun ejecutarResolucion(actual: HistorialUiState.Ready, remoto: LecturaPreparacion,
                                   seleccion: Map<String, EstadoAcademico>?) {
        if (saveJob?.isActive == true) return
        mutableState.value = actual.copy(guardando = true)
        saveJob = viewModelScope.launch {
            try {
                val resultado = if (activa(remoto)) repository.descartarOperacion(remoto) else repository.load(uid)
                verificar(resultado)
                if (!activa(remoto) && (resultado.registro != remoto.registro || resultado.operacion != remoto.operacion ||
                        BorradorHistorial(uid, remoto.academic.estados).diferenciasRemotas(resultado.academic.estados).isNotEmpty()))
                    throw ConflictoHistorial(resultado)
                val borrador = BorradorHistorial(uid, resultado.academic.estados, seleccion ?: resultado.academic.estados)
                if (sesionValida()) mutableState.value = nuevo(resultado).copy(borrador = borrador,
                    modo = ModoHistorial.EDICION, paso = PasoHistorial.REVISION, diagnosticos = borrador.validar(reglas(resultado)).contradicciones)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                registrarError(actual, e)
            }
        }
    }

    private class SinCambiosRemotos(val lectura: LecturaPreparacion) : Exception()

    class Factory(private val uid: String, private val repository: PreparacionRepository,
                  private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HistorialViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return HistorialViewModel(uid, repository, authRepository) as T
        }
    }

    companion object { fun key(uid: String, sessionVersion: Long) = "historial:$uid:$sessionVersion" }
}

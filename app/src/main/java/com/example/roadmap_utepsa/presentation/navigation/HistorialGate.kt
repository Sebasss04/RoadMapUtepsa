package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.roadmap_utepsa.domain.auth.AuthRepository
import com.example.roadmap_utepsa.domain.history.PreparacionRepository
import com.example.roadmap_utepsa.presentation.history.HistorialActions
import com.example.roadmap_utepsa.presentation.history.HistorialScreen
import com.example.roadmap_utepsa.presentation.history.HistorialUiState
import com.example.roadmap_utepsa.presentation.history.HistorialViewModel
import com.example.roadmap_utepsa.presentation.history.EstadoPreparacionUi
import com.example.roadmap_utepsa.presentation.history.ModoHistorial
import com.example.roadmap_utepsa.domain.notification.ReminderRoute
import com.example.roadmap_utepsa.domain.notification.ReminderKind

/** Gate productivo de preparación; cargarlo no confirma, pospone ni recupera operaciones. */
@Composable
internal fun HistorialGate(uid: String, sessionVersion: Long, owner: ViewModelStoreOwner,
                           repository: PreparacionRepository, auth: AuthRepository,
                           onProfile: () -> Unit, onSignIn: () -> Unit, onEnterCurriculum: () -> Unit,
                           curriculum: @Composable (onReview: () -> Unit, status: EstadoPreparacionUi) -> Unit,
                           notificationRoute: ReminderRoute? = null,
                           onNotificationDestination: (ReminderKind) -> Unit = {}) {
    val vm = remember(uid, sessionVersion, owner, repository, auth) {
        ViewModelProvider(owner, HistorialViewModel.Factory(uid, repository, auth))
            .get(HistorialViewModel.key(uid, sessionVersion), HistorialViewModel::class.java)
    }
    val state by vm.state.collectAsState()
    var inCurriculum by rememberSaveable(uid, sessionVersion) { mutableStateOf(false) }
    var initialDecisionHandled by rememberSaveable(uid, sessionVersion) { mutableStateOf(false) }
    val ready = state as? HistorialUiState.Ready
    // Malla tiene su propio ViewModel: debe releer también al saltar la bienvenida
    // después de recrear el proceso y al volver de una revisión del historial.
    LaunchedEffect(inCurriculum) {
        if (inCurriculum) onEnterCurriculum()
    }
    LaunchedEffect(ready?.lectura, notificationRoute) {
        if (notificationRoute == null && !initialDecisionHandled && ready != null) {
            initialDecisionHandled = true
            if (ready.lectura.registro != null && !ready.recuperacionPendiente) inCurriculum = true
        }
    }
    fun reviewPreservingDraft() {
        if (ready?.modo != ModoHistorial.EDICION) vm.iniciarRevision()
        inCurriculum = false
    }
    LaunchedEffect(notificationRoute, ready?.guardando, ready?.recuperacionPendiente, ready != null) {
        val route = notificationRoute ?: return@LaunchedEffect
        if (route.uid != uid || ready == null || ready.guardando || ready.recuperacionPendiente) return@LaunchedEffect
        if (route.kind == ReminderKind.OFFER && ready.lectura.registro == null) return@LaunchedEffect
        initialDecisionHandled = true
        if (route.kind == ReminderKind.HISTORY) {
            if (inCurriculum) reviewPreservingDraft()
        } else inCurriculum = true
        onNotificationDestination(route.kind)
    }
    if (inCurriculum) curriculum({
        reviewPreservingDraft()
    }, when {
        state == HistorialUiState.Loading -> EstadoPreparacionUi.CARGANDO
        ready == null -> EstadoPreparacionUi.ERROR
        ready.recuperacionPendiente -> EstadoPreparacionUi.GUARDADO_PENDIENTE
        ready.lectura.registro?.decision == com.example.roadmap_utepsa.domain.history.DecisionPreparacion.CONFIRMADO -> EstadoPreparacionUi.CONFIRMADO
        else -> EstadoPreparacionUi.CONFIGURACION_PENDIENTE
    })
    else HistorialScreen(state, HistorialActions(
        onEdit = vm::editar, onCancel = vm::cancelarEdicion,
        onRetry = vm::reload, onProfile = onProfile, onSignIn = onSignIn,
        onOpenStep = vm::abrirPaso, onApproved = vm::seleccionarAprobada,
        onCourse = vm::seleccionarEnCurso, onReview = vm::revisar,
        onSkipCourse = vm::omitirNuevasEnCurso, onConfirm = vm::confirmar,
        onPostpone = vm::posponer, onRecover = vm::recuperarResultado,
        onDiscard = { if (ready?.modo == ModoHistorial.EDICION) vm.cancelarEdicion() else { vm.iniciarRevision(); inCurriculum = true } },
        onExit = { if (ready?.guardando != true) inCurriculum = true },
        onReadConflict = vm::leerConflicto, onCompareSelection = vm::compareSelection,
                    onResolveConflict = vm::resolverConflictoConSeleccion
    ))
}

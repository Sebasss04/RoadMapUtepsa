package com.example.roadmap_utepsa.presentation.history

import androidx.compose.ui.res.pluralStringResource
import com.example.roadmap_utepsa.presentation.shared.text.quantityText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.academic.DiagnosticoMateriaHistorial
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico
import com.example.roadmap_utepsa.domain.academic.EstadoAcademico.*
import com.example.roadmap_utepsa.domain.academic.Materia
import com.example.roadmap_utepsa.domain.academic.filterCurriculum
import com.example.roadmap_utepsa.presentation.history.HistorialUiState
import com.example.roadmap_utepsa.presentation.history.PasoHistorial
import com.example.roadmap_utepsa.presentation.history.ModoHistorial
import com.example.roadmap_utepsa.presentation.shared.text.durationLabel

data class HistorialActions(
    val onEdit: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onOpenStep: (PasoHistorial) -> Unit = {},
    val onApproved: (String, Boolean) -> Unit = { _, _ -> },
    val onCourse: (String, Boolean) -> Unit = { _, _ -> },
    val onReview: () -> Unit = {},
    val onSkipCourse: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onPostpone: () -> Unit = {},
    val onRecover: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onExit: () -> Unit = {},
    val onReadConflict: () -> Unit = {},
    val onCompareSelection: (Map<String, EstadoAcademico>) -> Unit = {},
    val onResolveConflict: (Map<String, EstadoAcademico>) -> Unit = {}
)

private enum class Vista { NORMAL, CONSULTA, LOADING, ERROR, EXPIRED, SAVING, WAITING, SAVE_ERROR, RECOVER, CONFLICT, DISCARD, SUCCESS, POSTPONED }

/** H01–H07 / H09–H20 / H31–H32: estados de un mismo recorrido, sin destinos extra. */
@Composable
fun HistorialScreen(state: HistorialUiState, actions: HistorialActions) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    val ready = state as? HistorialUiState.Ready
    val uid = ready?.borrador?.uid
    var query by rememberSaveable(uid) { mutableStateOf("") }
    var semester by rememberSaveable(uid) { mutableIntStateOf(1) }
    var discard by rememberSaveable(uid) { mutableStateOf(false) }
    var previewDraft by rememberSaveable(uid) { mutableStateOf(false) }
    var comparing by remember(ready?.conflicto) { mutableStateOf(false) }
    var comparisonStep by remember(ready?.conflicto) { mutableStateOf(PasoHistorial.APROBADAS) }
    val reviewed = ready?.comparisonDraft ?: ready?.borrador?.seleccion.orEmpty()
    val saving = ready?.guardando == true
    val editing = ready?.modo == ModoHistorial.EDICION
    var summaryFilter by rememberSaveable(uid) { mutableStateOf<String?>(null) }
    val selection = if (comparing) reviewed else ready?.borrador?.seleccion.orEmpty()
    val step = if (previewDraft) PasoHistorial.APROBADAS else if (comparing) comparisonStep else ready?.paso ?: PasoHistorial.BIENVENIDA
    val materias = ready?.lectura?.academic?.materias.orEmpty()
    val byId = remember(materias) { materias.associateBy { it.id } }
    val approved = selection.values.count { it == APROBADA }
    val course = selection.values.count { it == EN_CURSO }
    val pending = materias.size - approved - course
    val postponing = ready?.modo == ModoHistorial.PREPARACION && ready.paso == PasoHistorial.BIENVENIDA
    val operation = ready?.lectura?.operacion
    val applied = maxOf(ready?.progresoAplicado ?: 0, operation?.aplicados?.size ?: 0)
    val total = maxOf(ready?.progresoTotal ?: 0, operation?.cambios?.size ?: 0)
    val vista = when {
        state == HistorialUiState.Loading -> Vista.LOADING
        state == HistorialUiState.Error -> Vista.ERROR
        state == HistorialUiState.SessionExpired -> Vista.EXPIRED
        saving && ready.esperando -> Vista.WAITING
        saving -> Vista.SAVING
        discard -> Vista.DISCARD
        ready?.conflicto != null && !comparing -> Vista.CONFLICT
        ready?.confirmado == true -> Vista.SUCCESS
        ready?.pospuesto == true -> Vista.POSTPONED
        !comparing && !previewDraft && ready?.errorGuardado == true -> if (applied > 0 && !postponing) Vista.RECOVER else Vista.SAVE_ERROR
        !comparing && !previewDraft && ready?.recuperacionPendiente == true -> Vista.RECOVER
        ready?.modo == ModoHistorial.CONSULTA -> Vista.CONSULTA
        else -> Vista.NORMAL
    }
    val diagnostics = if (comparing && ready != null) {
        ready.comparisonDiagnostics
    } else ready?.diagnosticos.orEmpty()
    val contradictoryReview = step == PasoHistorial.REVISION && diagnostics.isNotEmpty()
    fun openStep(value: PasoHistorial) {
        if (comparing) comparisonStep = value else actions.onOpenStep(value)
    }
    fun exit() {
        if (saving) return
        if (ready?.recuperacionPendiente == true || ready?.conflicto != null) return
        if (ready?.borrador?.cambios()?.isNotEmpty() == true && ready.confirmado.not()) discard = true
        else if (editing) actions.onCancel() else actions.onExit()
    }
    fun readConflict() { comparing = true; comparisonStep = PasoHistorial.APROBADAS; actions.onReadConflict() }
    BackHandler { if (vista == Vista.DISCARD) discard = false else if (previewDraft) previewDraft = false else exit() }

    Scaffold(Modifier.fillMaxSize().safeDrawingPadding(), contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(texts.getString(R.string.history_historial_screen_1), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(actions.onProfile, enabled = !saving && vista != Vista.EXPIRED && vista != Vista.LOADING) {
                    Icon(painterResource(R.drawable.academic_account), stringResource(R.string.academic_profile),
                        Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }, bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (vista) {
                        Vista.CONSULTA -> {
                            Primary(R.string.history_edit, actions.onEdit)
                            Secondary(R.string.history_flow_back_curriculum, actions.onExit)
                        }
                        Vista.LOADING -> Unit
                        Vista.ERROR -> Primary(R.string.profile_retry, actions.onRetry)
                        Vista.EXPIRED -> Primary(R.string.history_flow_access, actions.onSignIn)
                        Vista.SAVING, Vista.WAITING -> Primary(when {
                            postponing -> R.string.history_flow_saving_decision_action
                            vista == Vista.WAITING -> R.string.history_flow_waiting_action
                            else -> R.string.history_flow_saving_action
                        }, {}, enabled = false)
                        Vista.SUCCESS, Vista.POSTPONED -> Primary(R.string.history_flow_go_curriculum, actions.onExit)
                        Vista.SAVE_ERROR -> {
                            Primary(R.string.history_flow_check_retry, actions.onRecover)
                            if (postponing) Secondary(R.string.history_flow_continue_curriculum, actions.onExit)
                            else Secondary(R.string.history_flow_preview_selection, { previewDraft = true })
                        }
                        Vista.RECOVER -> Primary(R.string.history_flow_check_continue, actions.onRecover)
                        Vista.CONFLICT -> {
                            Primary(R.string.history_flow_read_changes, ::readConflict)
                            Secondary(R.string.history_flow_keep_draft, ::readConflict)
                        }
                        Vista.DISCARD -> {
                            Primary(R.string.history_flow_keep_reviewing, { discard = false })
                            Secondary(if (editing) R.string.history_discard_to_summary else R.string.history_flow_discard_exit, { discard = false; actions.onDiscard() })
                        }
                        Vista.NORMAL -> if (previewDraft) {
                            Primary(R.string.history_flow_check_retry, { previewDraft = false; actions.onRecover() })
                            Secondary(R.string.history_flow_back_message, { previewDraft = false })
                        } else when (step) {
                            PasoHistorial.BIENVENIDA -> Secondary(R.string.history_flow_postpone, actions.onPostpone, tag = "history-postpone")
                            PasoHistorial.APROBADAS -> {
                                Caption(pluralStringResource(R.plurals.history_selected_approved_count, approved, approved))
                                Primary(if (approved == 0) R.string.history_flow_continue_zero else R.string.history_flow_continue,
                                    { openStep(PasoHistorial.EN_CURSO) })
                                Secondary(if (editing) R.string.academic_cancel else R.string.history_flow_back, { if (comparing) comparing = false else if (editing) exit() else openStep(PasoHistorial.BIENVENIDA) })
                            }
                            PasoHistorial.EN_CURSO -> {
                                Caption(historyCounts(approved, course))
                                Primary(if (editing) R.string.history_review_edits else if (course == 0) R.string.history_flow_review_zero_course else R.string.history_flow_review,
                                    { if (comparing) comparisonStep = PasoHistorial.REVISION else actions.onReview() })
                                Secondary(if (course == 0) R.string.history_flow_back_approved else R.string.history_flow_skip_course,
                                    { if (course == 0) openStep(PasoHistorial.APROBADAS)
                                      else if (comparing) {
                                          actions.onCompareSelection(reviewed.mapValues { (id, value) ->
                                              if (value == EN_CURSO && ready?.borrador?.base?.get(id) != EN_CURSO) PENDIENTE else value
                                          })
                                          comparisonStep = PasoHistorial.REVISION
                                      } else actions.onSkipCourse() })
                            }
                            PasoHistorial.REVISION -> {
                                Caption(stringResource(if (contradictoryReview) R.string.history_flow_not_confirmed else R.string.history_flow_review_before_confirm))
                                Primary(when {
                                    contradictoryReview -> R.string.history_flow_correct
                                    editing && !comparing && ready.borrador.cambios().isEmpty() && ready.lectura.registro?.decision == com.example.roadmap_utepsa.domain.history.DecisionPreparacion.CONFIRMADO -> R.string.history_view
                                    editing && !comparing && ready.borrador.cambios().isEmpty() -> if (approved == 0) R.string.history_flow_confirm_zero else R.string.history_flow_confirm
                                    editing && !comparing -> R.string.history_save_edits
                                    comparing -> R.string.history_flow_review
                                    approved == 0 -> R.string.history_flow_confirm_zero
                                    else -> R.string.history_flow_confirm
                                }, {
                                    if (contradictoryReview) openStep(PasoHistorial.APROBADAS)
                                    else if (comparing) { actions.onResolveConflict(reviewed.toMap()); comparing = false }
                                    else if (editing && ready.borrador.cambios().isEmpty() && ready.lectura.registro?.decision == com.example.roadmap_utepsa.domain.history.DecisionPreparacion.CONFIRMADO) actions.onCancel()
                                    else actions.onConfirm()
                                })
                                Secondary(if (contradictoryReview) R.string.history_flow_back_curriculum else R.string.history_flow_edit_selection,
                                    { if (contradictoryReview && !comparing) exit() else openStep(PasoHistorial.APROBADAS) })
                            }
                        }
                    }
                    if (vista == Vista.NORMAL && editing && step != PasoHistorial.APROBADAS) Secondary(R.string.academic_cancel, ::exit)
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("history-content"),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (vista) {
                Vista.CONSULTA -> {
                    item { Heading(R.string.history_view, R.string.history_summary_title, R.string.history_summary_detail) }
                    item { Summary(approved, course, pending) }
                    if (diagnostics.isNotEmpty()) {
                        item { Info(R.string.history_flow_review_data, R.string.history_flow_review_data_detail, true) }
                        items(diagnostics, key = { "summary-diagnostic-${it.idMateria}" }) { Diagnostic(it, byId) }
                    }
                    if (ready?.lectura?.registro?.decision != com.example.roadmap_utepsa.domain.history.DecisionPreparacion.CONFIRMADO) {
                        item { Info(R.string.history_pending_title, R.string.history_summary_unconfirmed) }
                    }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(null, APROBADA, EN_CURSO, PENDIENTE).forEach { value ->
                                FilterChip(summaryFilter == value?.name, { summaryFilter = value?.name },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.primary),
                                    label = { Text(stringResource(value?.let { stateLabel(it) } ?: R.string.history_registered)) })
                            }
                        }
                    }
                    val savedRows = materias.filter {
                        val value = selection[it.id] ?: PENDIENTE
                        if (summaryFilter == null) value != PENDIENTE else value.name == summaryFilter
                    }
                    if (savedRows.isEmpty()) item { Body(R.string.history_summary_empty) }
                    savedRows.groupBy { it.semestre }.forEach { (number, rows) ->
                        item(key = "summary-semester-$number") { Strong(stringResource(R.string.academic_semester, number), 18) }
                        items(rows, key = { "saved-${it.id}" }) { materia -> HistoryCard {
                            Strong(materia.nombre)
                            Caption("${materia.codigo} · ${stringResource(durationLabel(materia))}")
                            Body(stringResource(stateLabel(selection[materia.id] ?: PENDIENTE)))
                        } }
                    }
                }
                Vista.LOADING -> {
                    item { Heading(R.string.history_flow_preparation, R.string.history_flow_loading, R.string.history_flow_loading_detail) }
                    item { Info(R.string.history_flow_reading, R.string.history_flow_reading_detail) }
                    item { Body(R.string.history_flow_loading_footer) }
                }
                Vista.ERROR -> {
                    item { Heading(R.string.history_flow_preparation, R.string.history_flow_read_error, R.string.history_flow_read_error_detail) }
                    item { Info(R.string.history_flow_connection, R.string.history_flow_connection_detail, accent = true) }
                    item { Body(R.string.history_flow_read_error_footer) }
                }
                Vista.EXPIRED -> {
                    item { Heading(R.string.history_flow_invalid_session, R.string.history_flow_expired, R.string.history_flow_expired_detail) }
                    item { Info(R.string.history_flow_account_isolation, R.string.history_flow_account_isolation_detail, accent = true) }
                    item { Body(R.string.history_flow_expired_footer) }
                }
                Vista.SAVING, Vista.WAITING -> {
                    item { Heading(if (postponing) R.string.history_flow_preparation else R.string.history_flow_confirmation_progress,
                        when { postponing -> R.string.history_flow_saving_decision; vista == Vista.WAITING -> R.string.history_flow_waiting; else -> R.string.history_flow_saving },
                        when { postponing -> R.string.history_flow_saving_decision_detail; vista == Vista.WAITING -> R.string.history_flow_waiting_detail; else -> R.string.history_flow_saving_detail }) }
                    if (vista == Vista.WAITING) item { Info(R.string.history_flow_do_not_resend, R.string.history_flow_do_not_resend_detail, true) }
                    else if (postponing) item { Info(R.string.history_flow_preserved, R.string.history_flow_postpone_preserved, true) }
                    else {
                        item { Summary(approved, course, pending) }
                        item { Info(R.string.history_flow_not_yet_confirmed, R.string.history_flow_wait_result, true) }
                    }
                    if (!postponing && total > 0) item { Progress(applied, total) }
                    item { Body(if (postponing) R.string.history_flow_saving_decision_footer else if (vista == Vista.WAITING) R.string.history_flow_delay_not_failure else R.string.history_flow_saving_footer) }
                }
                Vista.SAVE_ERROR, Vista.RECOVER -> {
                    item { Heading(if (vista == Vista.RECOVER) R.string.history_flow_recover_title else R.string.history_flow_unconfirmed,
                        if (vista == Vista.RECOVER) R.string.history_flow_partial else R.string.history_flow_save_error,
                        if (postponing) R.string.history_flow_postpone_error_detail else if (vista == Vista.RECOVER) R.string.history_flow_partial_detail else R.string.history_flow_save_error_detail) }
                    if (postponing) item { Info(R.string.history_flow_no_state_changes, R.string.history_flow_postpone_error_preserved) }
                    else if (vista == Vista.RECOVER) item { Progress(applied, total) }
                    else item { Summary(approved, course, pending) }
                    item { Info(if (vista == Vista.RECOVER) R.string.history_flow_confirmed_preserved else R.string.history_flow_check_first,
                        if (vista == Vista.RECOVER) R.string.history_flow_confirmed_preserved_detail else R.string.history_flow_check_first_detail, true) }
                    item { Body(if (postponing) R.string.history_flow_postpone_error_footer else R.string.history_flow_draft_footer) }
                }
                Vista.CONFLICT -> {
                    item { Heading(R.string.history_flow_review_needed, R.string.history_flow_changed, R.string.history_flow_changed_detail) }
                    val differences = ready?.conflicto?.let { ready.borrador.diferenciasRemotas(it.academic.estados) }.orEmpty()
                    items(differences, key = { "difference-${it.idMateria}" }) { difference ->
                        HistoryCard(modifier = Modifier.testTag("history-conflict")) {
                            Strong(byId[difference.idMateria]?.let { "${it.nombre} · ${it.codigo}" } ?: difference.idMateria)
                            Body(stringResource(R.string.history_flow_your_draft, stringResource(stateLabel(difference.seleccionado))))
                            Body(stringResource(R.string.history_flow_now_saved, stringResource(stateLabel(difference.remoto))))
                        }
                    }
                    item { Info(R.string.history_flow_review_continue, R.string.history_flow_review_continue_detail, true) }
                    item { Body(R.string.history_flow_conflict_footer) }
                }
                Vista.DISCARD -> {
                    item { Heading(R.string.history_flow_draft_title, R.string.history_flow_exit_without_saving, R.string.history_flow_exit_detail) }
                    item { Info(R.string.history_flow_discard_title, R.string.history_flow_discard_detail) }
                    item { Body(R.string.history_flow_discard_footer) }
                }
                Vista.SUCCESS, Vista.POSTPONED -> {
                    item { Heading(R.string.history_flow_completed, if (vista == Vista.SUCCESS) R.string.history_flow_ready else R.string.history_flow_postponed,
                        if (vista == Vista.SUCCESS) R.string.history_flow_ready_detail else R.string.history_flow_postponed_detail) }
                    item { Summary(approved, course, pending, true) }
                    item { Info(R.string.history_flow_correct_later, R.string.history_flow_correct_later_detail) }
                    item { Body(R.string.history_flow_self_declared) }
                }
                Vista.NORMAL -> when (step) {
                    PasoHistorial.BIENVENIDA -> {
                        item { Heading(R.string.history_flow_before_starting, R.string.history_flow_welcome, R.string.history_flow_welcome_detail) }
                        item { HistoryCard(onClick = { openStep(PasoHistorial.APROBADAS) }, modifier = Modifier.testTag("history-new")) {
                            Strong(stringResource(R.string.history_flow_new)); Body(R.string.history_flow_new_detail)
                        } }
                        item { HistoryCard(accent = true, onClick = { openStep(PasoHistorial.APROBADAS) }, modifier = Modifier.testTag("history-approved-start")) {
                            Strong(stringResource(R.string.history_flow_existing)); Body(R.string.history_flow_existing_detail)
                        } }
                        item { Info(R.string.history_flow_preserved, R.string.history_flow_preserved_detail) }
                        item { Body(R.string.history_flow_revisit) }
                    }
                    PasoHistorial.APROBADAS, PasoHistorial.EN_CURSO -> {
                        val isCourse = step == PasoHistorial.EN_CURSO
                        item { Heading(if (isCourse) R.string.history_flow_step_course else R.string.history_flow_step_approved,
                            if (editing) { if (isCourse) R.string.history_edit_course else R.string.history_edit_approved } else if (isCourse) R.string.history_flow_course_title else R.string.history_flow_approved_title,
                            if (isCourse) R.string.history_flow_course_detail else R.string.history_flow_approved_detail) }
                        if (comparing) item { Info(R.string.history_flow_review_needed, R.string.history_flow_conflict_footer, true) }
                        if (previewDraft) item { Info(R.string.history_flow_check_first, R.string.history_flow_check_first_detail, true) }
                        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("history-search"),
                            label = { Text(stringResource(R.string.history_flow_search)) }, singleLine = true) }
                        if (query.isBlank()) item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..9).forEach { n ->
                                    FilterChip(semester == n, { semester = n }, modifier = Modifier.weight(1f).testTag("history-semester-$n"),
                                        border = null, shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.primary),
                                        label = { Text("$n", fontSize = 11.sp) })
                                }
                            }
                        }
                        val visible = ready?.lectura?.academic?.let { academic ->
                            filterCurriculum(academic, query, null, semester.takeIf { query.isBlank() })
                                .filter { !isCourse || selection[it.id] != APROBADA }
                        }.orEmpty()
                        if (visible.isEmpty()) {
                            item { Info(R.string.history_flow_no_results, R.string.history_flow_no_results_detail, true) }
                            item { Secondary(R.string.history_flow_clear_search, { query = "" }) }
                        }
                        visible.groupBy { it.semestre }.forEach { (number, rows) ->
                            item(key = "semester-$number") { Strong(stringResource(R.string.academic_semester, number), 18) }
                            items(rows, key = { it.id }) { materia ->
                                val checked = selection[materia.id] == if (isCourse) EN_CURSO else APROBADA
                                val eligibility = if (isCourse && ready != null)
                                    (if (comparing) ready.comparisonEligibility else ready.eligibility)[materia.id] else null
                                val allowed = !previewDraft && (!isCourse || checked || eligibility?.habilitada == true)
                                val requirements = eligibility?.requisitosSinAprobar.orEmpty().mapNotNull { byId[it]?.nombre }.sorted()
                                SubjectChoice(materia, checked, allowed,
                                    if (requirements.isNotEmpty()) stringResource(R.string.history_flow_missing_requirements, requirements.joinToString(" · "))
                                    else if ((eligibility?.aprobadasFaltantes ?: 0) > 0) quantityText(R.plurals.history_flow_missing_approved, eligibility!!.aprobadasFaltantes)
                                    else if (isCourse) stringResource(R.string.history_flow_can_select) else null) { value ->
                                    if (previewDraft) return@SubjectChoice
                                    if (comparing) actions.onCompareSelection(reviewed + (materia.id to if (value) { if (isCourse) EN_CURSO else APROBADA } else PENDIENTE))
                                    else if (isCourse) actions.onCourse(materia.id, value) else actions.onApproved(materia.id, value)
                                }
                            }
                        }
                        if (isCourse) {
                            if (course == 0) item { Info(R.string.history_flow_zero_course, R.string.history_flow_zero_course_detail) }
                            item { Body(R.string.history_flow_course_not_prerequisite) }
                            items(diagnostics, key = { "diagnostic-${it.idMateria}" }) { Diagnostic(it, byId) }
                        }
                    }
                    PasoHistorial.REVISION -> {
                        item { Heading(if (contradictoryReview) R.string.history_flow_set_review else R.string.history_flow_step_review,
                            if (contradictoryReview) R.string.history_flow_review_data else if (editing) R.string.history_review_edits else R.string.history_flow_review_title,
                            if (contradictoryReview) R.string.history_flow_review_data_detail else if (approved == 0) R.string.history_flow_zero_review_detail else R.string.history_flow_review_detail) }
                        item { Summary(approved, course, pending) }
                        if (editing) {
                            val changes = ready.borrador.cambios()
                            if (changes.isEmpty()) item { Body(R.string.history_no_changes) }
                            items(changes.entries.toList(), key = { "change-${it.key}" }) { (id, change) -> HistoryCard {
                                Strong(byId.getValue(id).nombre)
                                Body("${stringResource(stateLabel(change.anterior))} → ${stringResource(stateLabel(change.nuevo))}")
                            } }
                        }
                        if (contradictoryReview) {
                            items(diagnostics, key = { "diagnostic-${it.idMateria}" }) { Diagnostic(it, byId) }
                            item { Info(R.string.history_flow_correct_missing, R.string.history_flow_correct_missing_detail) }
                        } else {
                            listOf(APROBADA, EN_CURSO).forEach { estado ->
                                materias.filter { selection[it.id] == estado }.groupBy { it.semestre }.forEach { (semestre, rows) ->
                                    item(key = "review-${estado.name}-$semestre") { HistoryCard {
                                        Strong(stringResource(R.string.history_flow_state_semester, stringResource(stateLabel(estado)), semestre))
                                        rows.forEach { Strong(it.nombre, 15); Caption(it.codigo) }
                                    } }
                                }
                            }
                            if (approved == 0) item { Info(R.string.history_flow_no_approved, R.string.history_flow_no_approved_detail, true) }
                            else item { Info(R.string.history_flow_coherent, R.string.history_flow_coherent_detail, true) }
                            item { Body(if (comparing) R.string.history_flow_conflict_footer else R.string.history_flow_finish_after_reading) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(step: Int, title: Int, detail: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(step), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(stringResource(title), fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 37.8.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        Body(detail)
    }
}

@Composable
private fun HistoryCard(modifier: Modifier = Modifier, accent: Boolean = false, onClick: (() -> Unit)? = null,
                        content: @Composable ColumnScope.() -> Unit) {
    val color = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    if (onClick != null) Surface(onClick, modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = color) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    } else Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = color) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable private fun Strong(value: String, size: Int = 16) { Text(value, fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size * 1.35).sp) }
@Composable private fun Body(value: String) { Text(value, fontSize = 14.sp, lineHeight = 18.9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable private fun Body(value: Int) = Body(stringResource(value))
@Composable private fun Caption(value: String) { Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable private fun Info(title: Int, detail: Int, accent: Boolean = false) = HistoryCard(accent = accent) { Strong(stringResource(title)); Body(detail) }
@Composable private fun Summary(approved: Int, course: Int, pending: Int, accent: Boolean = false) = HistoryCard(accent = accent) {
    Strong(historyCounts(approved, course)); Body(pluralStringResource(R.plurals.history_pending_subject_count, pending, pending))
}
@Composable private fun Progress(applied: Int, total: Int) = HistoryCard {
    Strong(pluralStringResource(R.plurals.history_flow_progress, total, applied, total)); Body(R.string.history_flow_progress_detail)
}
@Composable private fun Primary(label: Int, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("history-primary"), enabled = enabled) { Text(stringResource(label)) }
}
@Composable private fun Secondary(label: Int, onClick: () -> Unit, tag: String = "history-secondary") {
    TextButton(onClick, Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag(tag)) { Text(stringResource(label)) }
}

@Composable
private fun SubjectChoice(materia: Materia, checked: Boolean, enabled: Boolean, detail: String?, onChecked: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChecked)
            .testTag("history-subject-${materia.id}").padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Checkbox(checked, null, enabled = enabled)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Strong(materia.nombre, 15)
                Caption("${materia.codigo} · ${stringResource(durationLabel(materia))}")
                if (detail != null) Caption(detail)
            }
        }
    }
}

@Composable
private fun Diagnostic(diagnostic: DiagnosticoMateriaHistorial, materias: Map<String, Materia>) {
    HistoryCard(accent = true, modifier = Modifier.testTag("history-diagnostic-${diagnostic.idMateria}")) {
        Strong(materias[diagnostic.idMateria]?.let { "${it.nombre} · ${it.codigo}" } ?: diagnostic.idMateria)
        Caption(stringResource(stateLabel(diagnostic.estado)))
        if (diagnostic.requisitosSinAprobar.isNotEmpty()) {
            Strong(stringResource(R.string.history_flow_missing_title))
            diagnostic.requisitosSinAprobar.mapNotNull { materias[it] }.sortedBy { it.codigo }.forEach { Body("${it.nombre} · ${it.codigo}") }
        }
        if (diagnostic.aprobadasFaltantes > 0) {
            Body(stringResource(R.string.history_flow_threshold, quantityText(R.plurals.history_needed, diagnostic.minAprobadas), quantityText(R.plurals.history_computable, diagnostic.aprobadasComputables), quantityText(R.plurals.history_missing, diagnostic.aprobadasFaltantes)))
        }
    }
}

private fun stateLabel(state: EstadoAcademico) = when (state) {
    PENDIENTE -> R.string.academic_pending
    EN_CURSO -> R.string.academic_in_progress
    APROBADA -> R.string.academic_approved
}

@Composable private fun historyCounts(approved: Int, course: Int): String =
    stringResource(R.string.history_counts_summary,
        pluralStringResource(R.plurals.history_approved_subject_count, approved, approved), course)

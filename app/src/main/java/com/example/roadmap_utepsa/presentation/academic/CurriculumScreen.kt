package com.example.roadmap_utepsa.presentation.academic

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.model.*
import com.example.roadmap_utepsa.presentation.viewmodel.*

@Composable
fun CurriculumScreen(state: AcademicUiState, onRetry: () -> Unit, onProfile: () -> Unit,
                     onSignOut: () -> Unit, onOpen: (String) -> Unit,
                     onSelect: (EstadoAcademico) -> Unit, onSave: () -> Unit,
                     onClose: () -> Unit, onDismissSaved: () -> Unit,
                     onReviewHistory: (() -> Unit)? = null, historyStatus: EstadoPreparacionUi = EstadoPreparacionUi.CONFIRMADO,
                     onReviewConflict: () -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var semester by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val ready = state as? AcademicUiState.Ready
    val saving = ready?.editor?.saving == true
    val status = if (ready != null && historyStatus !in listOf(EstadoPreparacionUi.CARGANDO, EstadoPreparacionUi.ERROR, EstadoPreparacionUi.GUARDADO_PENDIENTE) &&
        com.example.roadmap_utepsa.domain.academic.ReglasHistorial(ready.data.materias, ready.data.prerrequisitos).validar(ready.data.estados).contradicciones.isNotEmpty())
        EstadoPreparacionUi.REQUIERE_REVISION else historyStatus
    val statusTitle = when (status) {
        EstadoPreparacionUi.CONFIGURACION_PENDIENTE -> R.string.history_pending_title
        EstadoPreparacionUi.REQUIERE_REVISION -> R.string.history_flow_review_data
        EstadoPreparacionUi.GUARDADO_PENDIENTE -> R.string.history_flow_partial
        EstadoPreparacionUi.CARGANDO -> R.string.history_flow_loading
        EstadoPreparacionUi.ERROR -> R.string.history_flow_read_error
        EstadoPreparacionUi.CONFIRMADO -> R.string.history_view
    }
    val snackbar = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.academic_saved)
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.primary)
    LaunchedEffect(ready?.saved) {
        if (ready?.saved == true) {
            snackbar.showSnackbar(savedMessage)
            onDismissSaved()
        }
    }
    Scaffold(modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("RoadMap", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                TextButton(onRetry, enabled = !saving && state != AcademicUiState.Loading) { Text(stringResource(R.string.academic_refresh)) }
                IconButton(onProfile, enabled = !saving) {
                    Icon(painterResource(R.drawable.academic_account), stringResource(R.string.academic_profile),
                        Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }, bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val labels = listOf(R.string.academic_summary, R.string.academic_curriculum,
                    R.string.academic_offer, R.string.academic_recommendations)
                val icons = listOf(R.drawable.academic_summary, R.drawable.academic_curriculum,
                    R.drawable.academic_offer, R.drawable.academic_recommendations)
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(selected = index == 1, onClick = {}, enabled = index == 1,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
                        icon = { Icon(painterResource(icons[index]), null, Modifier.size(21.dp)) },
                        label = { Text(stringResource(label), fontSize = 11.sp) })
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("curriculum-list"),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text(stringResource(R.string.academic_title), fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold) }
            when (state) {
                AcademicUiState.Loading -> item {
                    Text(stringResource(R.string.academic_loading), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                AcademicUiState.Error -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.academic_error), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.academic_error_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onRetry, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.profile_retry)) }
                    }
                }
                AcademicUiState.SessionExpired -> item { Text(stringResource(R.string.academic_expired)) }
                is AcademicUiState.Ready -> {
                    item { Text(stringResource(R.string.academic_subtitle, state.data.materias.size), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp) }
                    if (onReviewHistory != null) item {
                        Surface(color = when (status) {
                            EstadoPreparacionUi.ERROR -> MaterialTheme.colorScheme.errorContainer
                            EstadoPreparacionUi.REQUIERE_REVISION, EstadoPreparacionUi.GUARDADO_PENDIENTE -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                            shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (status != EstadoPreparacionUi.CONFIRMADO) {
                                    Text(stringResource(statusTitle), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(when (status) {
                                        EstadoPreparacionUi.REQUIERE_REVISION -> R.string.history_flow_review_data_detail
                                        EstadoPreparacionUi.GUARDADO_PENDIENTE -> R.string.history_flow_partial_detail
                                        EstadoPreparacionUi.ERROR -> R.string.history_flow_read_error_detail
                                        EstadoPreparacionUi.CARGANDO -> R.string.history_flow_loading_detail
                                        else -> R.string.history_pending_detail
                                    }), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onReviewHistory, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(when (status) {
                                        EstadoPreparacionUi.CONFIGURACION_PENDIENTE -> R.string.history_resume
                                        EstadoPreparacionUi.REQUIERE_REVISION -> R.string.history_flow_correct
                                        EstadoPreparacionUi.GUARDADO_PENDIENTE -> R.string.history_flow_check_continue
                                        EstadoPreparacionUi.ERROR -> R.string.profile_retry
                                        else -> R.string.history_view
                                    }))
                                }
                            }
                        }
                    }
                    item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.academic_search)) }, singleLine = true) }
                    item {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val values = listOf(null, EstadoAcademico.PENDIENTE, EstadoAcademico.EN_CURSO, EstadoAcademico.APROBADA)
                            val labels = listOf(R.string.academic_all, R.string.academic_pending_plural, R.string.academic_in_progress, R.string.academic_approved_plural)
                            values.forEachIndexed { i, value -> FilterChip(filter == value?.name,
                                { filter = value?.name }, shape = RoundedCornerShape(16.dp), border = null, colors = chipColors,
                                label = { Text(stringResource(labels[i]), fontSize = 11.sp) }) }
                        }
                    }
                    item { Text(stringResource(R.string.academic_hint), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..9).forEach { number -> FilterChip(semester == number, { semester = if (semester == number) null else number },
                                shape = RoundedCornerShape(16.dp), border = null, colors = chipColors,
                                modifier = Modifier.weight(1f).semantics { contentDescription = "Semestre $number" },
                                label = { Text("$number", fontSize = 11.sp) }) }
                        }
                    }
                    val visible = filterCurriculum(state.data, query, filter?.let(EstadoAcademico::valueOf), semester)
                    if (visible.isEmpty()) item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.academic_no_results), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.academic_no_results_detail))
                            TextButton({ focus.clearFocus(); query = ""; filter = null; semester = null }) { Text(stringResource(R.string.academic_clear_filters)) }
                        }
                    }
                    visible.groupBy { it.semestre }.forEach { (number, materias) ->
                        item(key = "semester-$number") { Text(stringResource(R.string.academic_semester, number), fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        items(materias, key = { it.id }) { materia ->
                            Surface(onClick = { onOpen(materia.id) }, enabled = !saving,
                                shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().testTag("subject-${materia.id}")) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(materia.nombre, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("${materia.codigo} · ${stringResource(areaLabel(materia.area))} · ${stringResource(durationLabel(materia))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    StateLabel(state.data.estados[materia.id] ?: EstadoAcademico.PENDIENTE)
                                    Text(requirementSummary(materia, state.data), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item { TextButton({ confirmSignOut = true }, enabled = !saving) { Text(stringResource(R.string.profile_sign_out)) } }
        }
    }
    if (ready?.editor != null) SubjectSheet(ready, onSelect, onSave, onClose, onReviewHistory, onReviewConflict)
    if (confirmSignOut) AlertDialog(onDismissRequest = { confirmSignOut = false },
        title = { Text(stringResource(R.string.academic_sign_out_title)) },
        text = { Text(stringResource(R.string.academic_sign_out_detail)) },
        confirmButton = { TextButton({ confirmSignOut = false; onSignOut() }) { Text(stringResource(R.string.profile_sign_out)) } },
        dismissButton = { TextButton({ confirmSignOut = false }) { Text(stringResource(R.string.academic_cancel)) } })
}

internal fun stateLabel(state: EstadoAcademico) = when (state) {
    EstadoAcademico.PENDIENTE -> R.string.academic_pending
    EstadoAcademico.EN_CURSO -> R.string.academic_in_progress
    EstadoAcademico.APROBADA -> R.string.academic_approved
}
private fun areaLabel(area: AreaMateria) = when (area) {
    AreaMateria.BASICA -> R.string.academic_area_basic
    AreaMateria.COMPLEMENTARIA -> R.string.academic_area_complementary
    AreaMateria.ESPECIALIDAD -> R.string.academic_area_specialty
    AreaMateria.ELECTIVA -> R.string.academic_area_elective
}
@Composable private fun StateLabel(state: EstadoAcademico) {
    val color = when (state) { EstadoAcademico.APROBADA -> Color(0xFF21664A); EstadoAcademico.EN_CURSO -> Color(0xFF275A92); else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val mark = when (state) { EstadoAcademico.APROBADA -> "✓"; EstadoAcademico.EN_CURSO -> "◷"; else -> "○" }
    Text("$mark ${stringResource(stateLabel(state))}", color = color, fontSize = 12.sp)
}
@Composable private fun requirementSummary(materia: Materia, data: AcademicData): String {
    val byId = data.materias.associateBy { it.id }
    val codes = data.prerrequisitos.filter { it.idObjetivo == materia.id }.map { byId.getValue(it.idRequisito).nombre }.sorted()
    return when {
        materia.minAprobadas > 0 -> stringResource(R.string.academic_minimum, materia.minAprobadas)
        codes.isEmpty() -> stringResource(R.string.academic_no_requirements)
        else -> stringResource(R.string.academic_requirements, codes.joinToString(" y "))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SubjectSheet(ready: AcademicUiState.Ready, onSelect: (EstadoAcademico) -> Unit,
                                     onSave: () -> Unit, onClose: () -> Unit, onReviewHistory: (() -> Unit)?,
                                     onReviewConflict: () -> Unit) {
    val editor = ready.editor ?: return
    val transition = editor.transition
    val materia = ready.data.materias.single { it.id == editor.idMateria }
    val original = ready.data.estados[materia.id] ?: EstadoAcademico.PENDIENTE
    val returningToPending = original == EstadoAcademico.EN_CURSO && editor.selected == EstadoAcademico.PENDIENTE
    val thresholdCourse = original == EstadoAcademico.PENDIENTE && editor.selected == EstadoAcademico.EN_CURSO &&
        materia.minAprobadas > 0 && transition?.permitida == true
    if (transition != null && (returningToPending || thresholdCourse || !transition.permitida ||
            (editor.selected == EstadoAcademico.APROBADA && transition.propuesto.contradicciones.any { it.idMateria == editor.idMateria }))) {
        HistoryTransitionSheet(ready, onSave, onClose, onReviewHistory, onReviewConflict)
        return
    }
    var confirmDiscard by remember(editor.idMateria) { mutableStateOf(false) }
    val requestClose = {
        if (!editor.saving) { if (editor.selected != original) confirmDiscard = true else onClose() }
    }
    val latestEditor by rememberUpdatedState(editor)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (latestEditor.saving) false
        else if (value == SheetValue.Hidden && latestEditor.selected != original) {
            confirmDiscard = true
            false
        } else true
    })
    ModalBottomSheet(onDismissRequest = requestClose, sheetState = sheet,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().testTag("academic-editor").verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.academic_detail_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(materia.nombre, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
            Text("${materia.codigo} · ${stringResource(R.string.academic_semester, materia.semestre)} · ${stringResource(durationLabel(materia))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.history_saved_state, stringResource(stateLabel(original))),
                fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.history_selected_state, stringResource(stateLabel(editor.selected))), fontSize = 14.sp)
            val result = ready.habilitaciones.getValue(materia.id)
            Surface(color = if (result.habilitada) Color(0xFFEAF5EF) else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(if (result.habilitada) R.string.academic_requirements_met else R.string.academic_requirements_missing), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(requirementSummary(materia, ready.data), fontSize = 13.sp)
                    val byId = ready.data.materias.associateBy { it.id }
                    ready.data.prerrequisitos.filter { it.idObjetivo == materia.id }.forEach {
                        val required = byId.getValue(it.idRequisito)
                        Text("${required.nombre} · ${required.codigo}", fontSize = 13.sp)
                        StateLabel(ready.data.estados[required.id] ?: EstadoAcademico.PENDIENTE)
                    }
                    if (result.aprobadasFaltantes > 0) Text(stringResource(R.string.academic_missing_count, result.aprobadasFaltantes), fontSize = 13.sp)
                }
            }
            if (!result.habilitada) Text(stringResource(R.string.academic_history_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (editor.selected == EstadoAcademico.PENDIENTE && original == EstadoAcademico.EN_CURSO) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.history_repeat_course_title), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.history_repeat_course_detail), fontSize = 14.sp)
                        Text(stringResource(R.string.history_three_states), fontSize = 13.sp)
                    }
                }
            }
            if (materia.minAprobadas > 0 && result.habilitada && editor.selected == EstadoAcademico.EN_CURSO) {
                Text(stringResource(R.string.history_threshold_met, ready.data.estados.count { (id, state) ->
                    id != materia.id && state == EstadoAcademico.APROBADA
                }), fontSize = 14.sp)
            }
            Text(stringResource(R.string.academic_choose_state), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            EstadoAcademico.entries.forEach { value ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(editor.selected == value,
                    enabled = !editor.saving, role = Role.RadioButton, onClick = { onSelect(value) })
                    .testTag("state-${value.name}"), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(editor.selected == value, onClick = null, enabled = !editor.saving)
                    Spacer(Modifier.width(10.dp)); Text(stringResource(stateLabel(value)), fontSize = 15.sp)
                }
            }
            if (editor.conflict) {
                Text(stringResource(R.string.history_state_conflict_title), fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                Text(stringResource(R.string.history_state_conflict_detail), fontSize = 14.sp)
                Button(onReviewConflict, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.history_review_changes))
                }
            }
            if (editor.failed) Text(stringResource(R.string.academic_save_error), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (editor.awaitingConfirmation) Text(stringResource(R.string.academic_save_pending),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Button(onSave, enabled = !editor.saving && !editor.conflict, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(when {
                    editor.saving -> R.string.profile_saving
                    editor.selected == EstadoAcademico.PENDIENTE && original == EstadoAcademico.EN_CURSO -> R.string.history_save_pending
                    editor.selected == EstadoAcademico.EN_CURSO && materia.minAprobadas > 0 -> R.string.history_save_course
                    else -> R.string.academic_save
                }))
            }
            TextButton(requestClose, enabled = !editor.saving, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.academic_cancel)) }
        }
    }
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.academic_discard_title)) }, text = { Text(stringResource(R.string.academic_discard_detail)) },
        confirmButton = { TextButton({ confirmDiscard = false; onClose() }) { Text(stringResource(R.string.profile_discard_confirm)) } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.profile_keep_editing)) } })
}

internal fun durationLabel(materia: Materia): Int = when (materia.duracion) {
    DuracionMateria.MODULAR -> R.string.subject_modular
    DuracionMateria.BIMODULAR -> R.string.subject_bimodular
    DuracionMateria.ESPECIAL -> R.string.subject_special
    null -> R.string.subject_duration_unavailable
}

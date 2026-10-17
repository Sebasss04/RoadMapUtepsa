package com.example.roadmap_utepsa.presentation.academic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.model.EstadoAcademico
import com.example.roadmap_utepsa.presentation.viewmodel.AcademicUiState

/** Paneles H21–H24/H26–H27: diagnóstico calculado, sin modificar dependientes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryTransitionSheet(ready: AcademicUiState.Ready, onSave: () -> Unit,
                                     onClose: () -> Unit, onReview: (() -> Unit)?, onReviewConflict: () -> Unit) {
    val editor = ready.editor ?: return
    val result = editor.transition ?: return
    val materia = ready.data.materias.single { it.id == editor.idMateria }
    val byId = ready.data.materias.associateBy { it.id }
    val etiquetas = mapOf(
        EstadoAcademico.PENDIENTE to stringResource(R.string.academic_pending),
        EstadoAcademico.EN_CURSO to stringResource(R.string.academic_in_progress),
        EstadoAcademico.APROBADA to stringResource(R.string.academic_approved))
    val original = ready.data.estados[materia.id] ?: EstadoAcademico.PENDIENTE
    val returningToPending = original == EstadoAcademico.EN_CURSO && editor.selected == EstadoAcademico.PENDIENTE
    val thresholdCourse = original == EstadoAcademico.PENDIENTE && editor.selected == EstadoAcademico.EN_CURSO &&
        materia.minAprobadas > 0 && result.permitida
    val simpleUpdate = returningToPending || thresholdCourse
    val dependencias = result.dependientesAfectados.isNotEmpty()
    val revisar: () -> Unit = { onReview?.invoke(); Unit }
    val requisitos = ready.data.prerrequisitos.filter { it.idObjetivo == materia.id }
        .map { byId.getValue(it.idRequisito) }.sortedBy { it.codigo }
    val diagnosticos = if (result.permitida) result.propuesto.contradicciones.filter { it.idMateria == materia.id }
        else result.bloqueos
    ModalBottomSheet(onDismissRequest = { if (!editor.saving) onClose() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { !editor.saving }), dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().testTag("academic-editor").heightIn(max = 700.dp).padding(horizontal = 24.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(when {
                    dependencias -> R.string.history_dependencies_title
                    simpleUpdate -> R.string.academic_detail_title
                    result.permitida -> R.string.history_register_approval
                    else -> R.string.academic_detail_title
                }), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(materia.nombre, fontSize = 21.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold)
                Text("${materia.codigo} · ${stringResource(durationLabel(materia))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TransitionCard {
                    Text(stringResource(R.string.history_saved_state,
                        stringResource(stateLabel(ready.data.estados[materia.id] ?: EstadoAcademico.PENDIENTE))),
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.history_selected_state, stringResource(stateLabel(editor.selected))),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (editor.conflict) TransitionCard(highlight = true) {
                    Text(stringResource(R.string.history_state_conflict_title), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.history_state_conflict_detail))
                }
                if (returningToPending) TransitionCard(highlight = true) {
                    Text(stringResource(R.string.history_repeat_course_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.history_repeat_course_detail), fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (thresholdCourse) TransitionCard(highlight = true) {
                    Text(stringResource(R.string.history_threshold_met_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(stringResource(R.string.history_threshold_met_count,
                        ready.data.estados.count { (id, state) -> id != materia.id && state == EstadoAcademico.APROBADA }),
                        fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!dependencias && !simpleUpdate && requisitos.isNotEmpty()) TransitionCard {
                    Text(stringResource(R.string.history_all_requirements), fontWeight = FontWeight.Bold)
                    requisitos.forEach { required ->
                        Text("${required.nombre}: ${etiquetas.getValue(ready.data.estados[required.id] ?: EstadoAcademico.PENDIENTE)}", fontSize = 14.sp)
                    }
                    Text(stringResource(R.string.history_all_requirements_detail), fontSize = 13.sp)
                }
                diagnosticos.forEach { d ->
                    TransitionCard(highlight = true) {
                        Text(if (dependencias) stringResource(R.string.history_dependent, byId.getValue(d.idMateria).nombre)
                            else if (d.aprobadasFaltantes > 0) stringResource(R.string.history_threshold, d.minAprobadas)
                            else stringResource(R.string.history_missing_approved), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (d.requisitosSinAprobar.isNotEmpty()) Text(d.requisitosSinAprobar.sorted().joinToString("\n") {
                            val required = byId.getValue(it)
                            "${required.nombre}: ${etiquetas.getValue(ready.data.estados[it] ?: EstadoAcademico.PENDIENTE)}"
                        }, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (d.aprobadasFaltantes > 0) Text(stringResource(R.string.history_threshold_detail,
                            d.aprobadasComputables, d.aprobadasFaltantes), fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(stringResource(when {
                    dependencias -> R.string.history_no_cascade
                    returningToPending -> R.string.history_three_states
                    thresholdCourse -> R.string.history_threshold_explicit_save
                    result.permitida -> R.string.history_approval_warning
                    else -> R.string.history_course_warning
                }), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (editor.failed) Text(stringResource(R.string.academic_save_error), color = MaterialTheme.colorScheme.error)
                if (editor.awaitingConfirmation) Text(stringResource(R.string.academic_save_pending))
            }
            Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = if (editor.conflict) onReviewConflict else if (dependencias) revisar else onSave,
                    enabled = !editor.saving && (editor.conflict || if (dependencias) onReview != null else result.permitida),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(when {
                        editor.saving -> R.string.profile_saving
                        editor.conflict -> R.string.history_review_changes
                        dependencias -> R.string.history_review_related
                        returningToPending -> R.string.history_save_pending
                        thresholdCourse -> R.string.history_save_course
                        result.permitida -> R.string.history_save_approval
                        else -> R.string.history_cannot_save_course
                    }))
                }
                TextButton(if (dependencias || simpleUpdate) onClose else revisar, enabled = !editor.saving && (dependencias || simpleUpdate || onReview != null),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                    Text(stringResource(when {
                        simpleUpdate -> R.string.academic_cancel
                        dependencias -> R.string.history_keep_approval
                        result.permitida -> R.string.history_review
                        else -> R.string.history_review_requirements
                    }))
                }
            }
        }
    }
}

@Composable private fun TransitionCard(highlight: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

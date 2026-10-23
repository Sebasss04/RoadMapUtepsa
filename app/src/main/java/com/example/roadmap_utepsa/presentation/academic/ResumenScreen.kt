package com.example.roadmap_utepsa.presentation.academic

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMapInProgressContainer
import com.example.roadmap_utepsa.presentation.shared.theme.RoadMapInProgressInk
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.presentation.shared.components.AcademicBottomBar
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.presentation.history.EstadoPreparacionUi

/** Figma 8:253/318/387/434; los datos y avisos proceden de la sesión académica. */
@Composable
fun ResumenScreen(state: AcademicUiState, historyStatus: EstadoPreparacionUi,
                  onRetry: () -> Unit, onProfile: () -> Unit, onCurriculum: () -> Unit,
                  onHistory: (() -> Unit)?,
                  onOffer: (() -> Unit)? = null, onRecommendations: (() -> Unit)? = null) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    BackHandler(onBack = onCurriculum)
    val summary = resumenState(state, historyStatus)
    ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.sp)) {
        Scaffold(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding(), contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 24.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(texts.getString(R.string.academic_curriculum_screen_1), fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 28.sp, modifier = Modifier.weight(1f))
                    IconButton(onProfile) {
                        Icon(painterResource(R.drawable.academic_account), stringResource(R.string.academic_profile),
                            Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }, bottomBar = { AcademicBottomBar(0, onSummary = {}, onCurriculum = onCurriculum,
                onOffer = onOffer, onRecommendations = onRecommendations) }) { padding ->
            val gap = if (summary is ResumenUiState.Ready) 16.dp else 20.dp
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp).testTag("summary-screen"),
                verticalArrangement = Arrangement.spacedBy(gap)) {
                Text(stringResource(R.string.summary_title), fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.summary_degree), fontSize = 15.sp, lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (summary) {
                    ResumenUiState.Loading -> {
                        SummaryCard(padding = 14.dp, gap = 4.dp, modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                        }) {
                            Text(stringResource(R.string.summary_loading), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.summary_loading_detail), fontSize = 13.sp, lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        repeat(3) {
                            Surface(Modifier.fillMaxWidth().height(96.dp), shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant) {}
                        }
                    }
                    ResumenUiState.Error -> {
                        Spacer(Modifier.height(74.dp))
                        Box(Modifier.fillMaxWidth().height(76.dp), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.summary_warning), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Text(stringResource(R.string.summary_error), Modifier.fillMaxWidth(), fontSize = 24.sp,
                            lineHeight = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.summary_error_detail), Modifier.fillMaxWidth(), fontSize = 15.sp,
                            lineHeight = 21.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Button(onRetry, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                            Text(stringResource(R.string.profile_retry), fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
                        }
                        if (historyStatus == EstadoPreparacionUi.ERROR && onHistory != null) {
                            TextButton(onHistory) { Text(stringResource(R.string.history_view)) }
                        }
                    }
                    ResumenUiState.SessionExpired -> {
                        Text(stringResource(R.string.academic_expired))
                    }
                    is ResumenUiState.Ready -> {
                        val value = summary.resumen
                        Text(stringResource(R.string.summary_intro), fontSize = 12.sp, lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SummaryCard(color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(stringResource(R.string.summary_progress), fontSize = 11.sp, lineHeight = 15.sp,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(Modifier.heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("${value.porcentaje} %", fontSize = 42.sp, lineHeight = 59.sp,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(stringResource(R.string.summary_approved_count, value.aprobadas), fontSize = 14.sp,
                                    lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                            }
                            val fraction = value.porcentaje / 100f
                            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surface).semantics {
                                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                                }) {
                                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary))
                            }
                            Text(stringResource(R.string.summary_only_approved), fontSize = 12.sp, lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(Triple(value.aprobadas, R.string.academic_approved_plural, Color(0xFF21664A)),
                                Triple(value.enCurso, R.string.academic_in_progress, RoadMapInProgressInk),
                                Triple(value.pendientes, R.string.academic_pending_plural, MaterialTheme.colorScheme.onSurfaceVariant)).forEach { (count, label, color) ->
                                Surface(Modifier.weight(1f).heightIn(min = 80.dp), shape = RoundedCornerShape(18.dp), color = if (label == R.string.academic_in_progress) RoadMapInProgressContainer else MaterialTheme.colorScheme.surfaceVariant) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(count.toString(), fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = color)
                                        Text(stringResource(label), fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        if (summary.historial != EstadoPreparacionUi.CONFIRMADO) SummaryCard {
                            val title = when (summary.historial) {
                                EstadoPreparacionUi.REQUIERE_REVISION -> R.string.history_flow_review_data
                                EstadoPreparacionUi.GUARDADO_PENDIENTE -> R.string.history_flow_partial
                                else -> R.string.history_pending_title
                            }
                            Text(stringResource(title), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.summary_history_notice), fontSize = 13.sp, lineHeight = 18.sp)
                            if (onHistory != null) TextButton(onHistory) { Text(stringResource(R.string.history_view)) }
                        }
                        if (value.aprobadas == 0 && value.enCurso == 0) {
                            SummaryCard(padding = 14.dp, gap = 4.dp) {
                                Text(stringResource(R.string.summary_start), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.summary_start_detail), fontSize = 13.sp, lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onCurriculum, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                Text(stringResource(R.string.summary_update), fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
                            }
                        } else SummaryCard {
                            Text(stringResource(R.string.summary_next), fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.summary_next_detail), fontSize = 14.sp, lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val unavailable = stringResource(R.string.summary_recommendations_unavailable)
                            TextButton({ onRecommendations?.invoke() }, enabled = onRecommendations != null, contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.heightIn(min = 44.dp).semantics {
                                    if (onRecommendations == null) stateDescription = unavailable
                                }) {
                                Text(stringResource(R.string.summary_recommendations), fontSize = 14.sp, lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
                            }
                        }
                        OutlinedButton(onCurriculum, Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Text(stringResource(R.string.summary_view_curriculum), color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, lineHeight = 21.sp,
                                fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
                        }
                        if (summary.historial == EstadoPreparacionUi.CONFIRMADO && onHistory != null) {
                            TextButton(onHistory) { Text(stringResource(R.string.history_view)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SummaryCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceVariant,
    padding: Dp = 18.dp, gap: Dp = 10.dp, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = color, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(gap), content = content)
    }
}

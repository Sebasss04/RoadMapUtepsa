package com.example.roadmap_utepsa.presentation.recommendation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.recommendation.*
import com.example.roadmap_utepsa.presentation.academic.durationLabel
import com.example.roadmap_utepsa.presentation.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecomendacionesScreen(state: RecomendacionesUiState, onRetry: () -> Unit,
    onMalla: () -> Unit, onOffer: () -> Unit, onHistory: () -> Unit,
    onProfile: () -> Unit, onSignOut: () -> Unit,
    signOutWarning: Int = R.string.academic_sign_out_detail, onSummary: (() -> Unit)? = null) {
    var detail by remember(state) { mutableStateOf<Recomendacion?>(null) }
    var showPrevious by remember(state) { mutableStateOf(false) }
    var signOut by remember { mutableStateOf(false) }
    BackHandler { onMalla() }
    Scaffold(Modifier.fillMaxSize().safeDrawingPadding(), contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("RoadMap", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onProfile) { Icon(painterResource(R.drawable.academic_account), stringResource(R.string.academic_profile), Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) }
            }
        }, bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                val labels = listOf(R.string.academic_summary, R.string.academic_curriculum, R.string.academic_offer, R.string.academic_recommendations)
                val icons = listOf(R.drawable.academic_summary, R.drawable.academic_curriculum, R.drawable.academic_offer, R.drawable.academic_recommendations)
                labels.forEachIndexed { index, label -> NavigationBarItem(selected = index == 3,
                    onClick = { if (index == 0) onSummary?.invoke() else if (index == 1) onMalla() else if (index == 2) onOffer() }, enabled = index != 0 || onSummary != null,
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
                    icon = { Icon(painterResource(icons[index]), null, Modifier.size(21.dp)) }, label = { Text(stringResource(label), fontSize = 11.sp) }) }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("recommendations-list"), contentPadding = PaddingValues(24.dp, 16.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item { Text(stringResource(R.string.rec_title), fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold) }
            item { Text(stringResource(R.string.rec_subtitle), fontSize = 15.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (state.estado) {
                EstadoRecomendaciones.CARGANDO -> {
                    item { RecSurface { Text(stringResource(R.string.rec_loading), fontWeight = FontWeight.Bold); Text(stringResource(R.string.rec_loading_detail)) } }
                    items(3) { Surface(Modifier.fillMaxWidth().height(125.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {} }
                }
                EstadoRecomendaciones.SESION_CADUCADA -> item { Text(stringResource(R.string.academic_expired)) }
                EstadoRecomendaciones.LISTO -> {
                    val result = checkNotNull(state.resultado)
                    if (result.pendientesAnteriores.isNotEmpty()) item {
                        Column {
                            Text(stringResource(if (result.alternativaDisponible) R.string.rec_alternative_detail else R.string.rec_previous_detail),
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton({ showPrevious = !showPrevious }) {
                                Text(stringResource(if (showPrevious) R.string.rec_hide_previous else R.string.rec_show_previous))
                            }
                        }
                    }
                    if (showPrevious) items(result.pendientesAnteriores, key = { "previous:${it.id}" }) {
                        Text("${it.nombre} · ${it.codigo}")
                    }
                    result.principal?.let { main -> item {
                        RecommendationCard(main, RecommendationRole.MAIN) { detail = main }
                    } }
                    if (result.alternativas.isNotEmpty()) item {
                        Text(stringResource(R.string.rec_two), fontWeight = FontWeight.Bold)
                    }
                    result.secundaria?.let { second -> item {
                        RecommendationCard(second, RecommendationRole.SECONDARY) { detail = second }
                    } }
                    if (result.secundaria != null && result.opcionales.isNotEmpty()) item {
                        Text(stringResource(R.string.rec_other), fontWeight = FontWeight.Bold)
                    }
                    items(result.opcionales, key = { it.materia.id }) { r -> RecommendationCard(r, RecommendationRole.OPTIONAL) { detail = r } }
                }
                else -> item {
                    val content = when (state.estado) {
                        EstadoRecomendaciones.HISTORIAL_PENDIENTE -> Triple(R.string.rec_history_pending, R.string.rec_history_pending_detail, R.string.rec_review_history)
                        EstadoRecomendaciones.HISTORIAL_INCOHERENTE -> Triple(R.string.rec_history_invalid, R.string.rec_history_invalid_detail, R.string.rec_review_history)
                        EstadoRecomendaciones.RECUPERACION_PENDIENTE -> Triple(R.string.rec_recovery, R.string.rec_recovery_detail, R.string.rec_resume_history)
                        EstadoRecomendaciones.OFERTA_AUSENTE -> Triple(R.string.rec_absent, R.string.rec_absent_detail, R.string.rec_register_offer)
                        EstadoRecomendaciones.OFERTA_VACIA -> Triple(R.string.rec_empty, R.string.rec_empty_detail, R.string.rec_edit_offer)
                        EstadoRecomendaciones.SIN_CANDIDATAS -> Triple(R.string.rec_none, R.string.rec_none_detail, R.string.rec_review_malla)
                        else -> Triple(R.string.rec_error, R.string.rec_error_detail, R.string.profile_retry)
                    }
                    Column(Modifier.fillMaxWidth().heightIn(min = 440.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)) {
                        Icon(painterResource(when (state.estado) {
                            EstadoRecomendaciones.OFERTA_AUSENTE, EstadoRecomendaciones.OFERTA_VACIA -> R.drawable.recommendation_offer_empty
                            EstadoRecomendaciones.SIN_CANDIDATAS -> R.drawable.recommendation_curriculum_empty
                            else -> R.drawable.academic_warning
                        }), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(content.first), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(stringResource(content.second), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(when (state.estado) {
                            EstadoRecomendaciones.HISTORIAL_PENDIENTE, EstadoRecomendaciones.HISTORIAL_INCOHERENTE, EstadoRecomendaciones.RECUPERACION_PENDIENTE -> onHistory
                            EstadoRecomendaciones.OFERTA_AUSENTE, EstadoRecomendaciones.OFERTA_VACIA -> onOffer
                            EstadoRecomendaciones.SIN_CANDIDATAS -> onMalla
                            else -> onRetry
                        }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(content.third)) }
                        if (state.estado == EstadoRecomendaciones.SIN_CANDIDATAS) OutlinedButton(onOffer, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.rec_edit_offer)) }
                    }
                }
            }
            item { TextButton({ signOut = true }) { Text(stringResource(R.string.offer_sign_out)) } }
        }
    }
    detail?.let { r -> ModalBottomSheet(onDismissRequest = { detail = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.rec_why), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.rec_meta, r.materia.codigo, stringResource(durationLabel(r.materia)), r.materia.semestre), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(r.materia.nombre, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            RecSurface(color = Color(0xFFEAF5EF)) {
                Text(stringResource(R.string.rec_can), color = Color(0xFF21664A), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.rec_eligible))
                if (r.requisitosSatisfechos.isEmpty() && r.umbralSatisfecho == 0) Text(stringResource(R.string.rec_no_requirements))
                if (r.requisitosSatisfechos.isNotEmpty()) {
                    Text(stringResource(R.string.rec_requirements))
                    r.requisitosSatisfechos.forEach { Text("${it.nombre} · ${it.codigo}") }
                }
                if (r.umbralSatisfecho > 0) Text(pluralStringResource(R.plurals.rec_threshold, r.umbralSatisfecho, r.umbralSatisfecho))
            }
            if (r.desbloqueadas.isNotEmpty()) {
                Text(stringResource(R.string.rec_unlocks), fontWeight = FontWeight.Bold)
                RecSurface { r.desbloqueadas.forEach { Text("${it.nombre} · ${it.codigo}") } }
            }
            if (r.avancesParciales.isNotEmpty()) {
                Text(stringResource(R.string.rec_partial_title), fontWeight = FontWeight.Bold)
                r.avancesParciales.forEach { avance -> RecSurface(color = Color(0xFFEAF2FF)) {
                    Text(avance.materia.nombre, fontWeight = FontWeight.Bold)
                    Text(pluralStringResource(R.plurals.rec_partial_progress,
                        avance.requisitosAprobados.size + avance.requisitosPendientes.size, avance.requisitosAprobados.size,
                        avance.requisitosAprobados.size + avance.requisitosPendientes.size))
                    if (avance.requisitosAprobados.isNotEmpty()) {
                        Text(stringResource(R.string.rec_after_approved))
                        avance.requisitosAprobados.forEach { Text("${it.nombre} · ${it.codigo}") }
                    }
                    if (avance.requisitosPendientes.isNotEmpty()) {
                        Text(stringResource(R.string.rec_still_needed))
                        avance.requisitosPendientes.forEach { Text("${it.nombre} · ${it.codigo}") }
                    }
                    if (avance.aprobadasFaltantes > 0) Text(pluralStringResource(R.plurals.rec_remaining_threshold,
                        avance.aprobadasFaltantes, avance.aprobadasFaltantes))
                } }
            }
            if (!r.tieneBeneficio) Text(stringResource(R.string.rec_optional_reason), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.rec_order), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.rec_read_only), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button({ detail = null }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.rec_understood)) }
        }
    } }
    if (signOut) AlertDialog(onDismissRequest = { signOut = false }, title = { Text(stringResource(R.string.offer_sign_out)) },
        text = { Text(stringResource(signOutWarning)) }, confirmButton = { TextButton(onSignOut) { Text(stringResource(R.string.offer_confirm)) } },
        dismissButton = { TextButton({ signOut = false }) { Text(stringResource(R.string.offer_stay)) } })
}

@Composable
private fun RecSurface(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceVariant, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = color) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun RecommendationCard(r: Recomendacion, role: RecommendationRole, onExplain: () -> Unit) {
    RecSurface(Modifier.testTag("recommendation-${r.materia.id}"), role.background) {
        Text(stringResource(role.label), fontSize = 12.sp, lineHeight = 17.sp, color = role.accent, fontWeight = FontWeight.Bold)
        Text(r.materia.nombre, fontSize = 18.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20232B))
        Text(stringResource(R.string.rec_meta, r.materia.codigo, stringResource(durationLabel(r.materia)), r.materia.semestre), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(recommendationReason(r), fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, color = role.accent)
        TextButton(onExplain, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.textButtonColors(contentColor = role.accent)) {
            Text(stringResource(R.string.rec_explain), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp)); Icon(painterResource(R.drawable.academic_arrow), null, Modifier.size(18.dp))
        }
    }
}

private enum class RecommendationRole(val label: Int, val background: Color, val accent: Color) {
    MAIN(R.string.rec_main, Color(0xFFEAF5EF), Color(0xFF21664A)),
    SECONDARY(R.string.rec_secondary, Color(0xFFEAF2FF), Color(0xFF244B87)),
    OPTIONAL(R.string.rec_optional, Color(0xFFF7F8FA), Color(0xFF20232B))
}

@Composable
private fun recommendationReason(r: Recomendacion): String {
    val complete = when (r.desbloqueadas.size) {
        0 -> null
        1 -> if (r.avancesParciales.isEmpty()) stringResource(R.string.rec_unlock_one, r.desbloqueadas.single().nombre)
            else pluralStringResource(R.plurals.rec_count, 1, 1)
        else -> pluralStringResource(R.plurals.rec_count, r.desbloqueadas.size, r.desbloqueadas.size)
    }
    val partial = when (r.avancesParciales.size) {
        0 -> null
        1 -> if (r.desbloqueadas.isEmpty()) stringResource(R.string.rec_partial_one, r.avancesParciales.single().materia.nombre)
            else pluralStringResource(R.plurals.rec_partial_count, 1, 1)
        else -> pluralStringResource(R.plurals.rec_partial_count, r.avancesParciales.size, r.avancesParciales.size)
    }
    return listOfNotNull(complete, partial).joinToString("\n").ifEmpty { stringResource(R.string.rec_optional_reason) }
}

package com.example.roadmap_utepsa.presentation.offer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.presentation.viewmodel.OfertaUiState
import com.example.roadmap_utepsa.presentation.academic.durationLabel

@Composable
fun OfertaScreen(state: OfertaUiState, onRetry: () -> Unit, onEdit: () -> Unit,
    onQuery: (String) -> Unit, onToggle: (String) -> Unit, onSave: () -> Unit,
    onCancel: () -> Unit, onConflict: (Boolean) -> Unit, onMalla: () -> Unit,
    onProfile: () -> Unit, onSignOut: () -> Unit, onRecommendations: (() -> Unit)? = null) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var discard by remember { mutableStateOf(false) }
    var signOut by remember { mutableStateOf(false) }
    val cancel = { if (state.dirty) discard = true else onCancel() }
    BackHandler(enabled = true) { if (!state.saving) { if (state.editing) cancel() else onMalla() } }
    Scaffold(Modifier.fillMaxSize().safeDrawingPadding(), contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("RoadMap", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onProfile, enabled = !state.saving) {
                    Icon(painterResource(R.drawable.academic_account), stringResource(R.string.academic_profile),
                        Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }, bottomBar = {
            Column {
                if (state.editing) Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text(pluralStringResource(R.plurals.offer_count, state.draft.size, state.draft.size), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button({ focus.clearFocus(); onSave() }, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("offer-save"),
                        enabled = !state.loading && !state.refreshFailed && !state.saving && !state.conflict) {
                        Text(stringResource(if (state.saving) R.string.offer_saving else if (state.error)
                            R.string.offer_retry_save else R.string.offer_save))
                    }
                    TextButton(cancel, Modifier.align(Alignment.CenterHorizontally), enabled = !state.saving) {
                        Text(stringResource(R.string.offer_cancel))
                    }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val labels = listOf(R.string.academic_summary, R.string.academic_curriculum,
                        R.string.academic_offer, R.string.academic_recommendations)
                    val icons = listOf(R.drawable.academic_summary, R.drawable.academic_curriculum,
                        R.drawable.academic_offer, R.drawable.academic_recommendations)
                    labels.forEachIndexed { index, label ->
                        NavigationBarItem(selected = index == 2, onClick = { if (index == 1) onMalla() else if (index == 3) onRecommendations?.invoke() },
                            enabled = !state.saving && (index in 1..2 || index == 3 && onRecommendations != null),
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary),
                            icon = { Icon(painterResource(icons[index]), null, Modifier.size(21.dp)) },
                            label = { Text(stringResource(label), fontSize = 11.sp) })
                    }
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("offer-list"),
            contentPadding = PaddingValues(24.dp, 16.dp, 24.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.offer_title), fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold) }
            item { Text(stringResource(R.string.offer_subtitle), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when {
                state.expired -> item { Text(stringResource(R.string.academic_expired)) }
                state.loading -> item { CircularProgressIndicator(); Text(stringResource(R.string.offer_loading)) }
                state.data == null -> item {
                    Text(stringResource(R.string.offer_read_error))
                    Button(onRetry) { Text(stringResource(R.string.profile_retry)) }
                }
                state.editing -> {
                    if (state.refreshFailed) item {
                        Text(stringResource(R.string.offer_history_read_error))
                        Button(onRetry) { Text(stringResource(R.string.profile_retry)) }
                    }
                    if (state.approvedRemoved) item {
                        OfferNotice(stringResource(R.string.offer_history_updated), stringResource(R.string.offer_approved_removed))
                    }
                    if (state.error) item {
                        OfferNotice(stringResource(R.string.offer_save_error), stringResource(
                            if (state.uncertain) R.string.offer_uncertain else R.string.offer_save_error_detail), true)
                    }
                    if (state.conflict) item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OfferNotice(stringResource(R.string.offer_conflict), stringResource(R.string.offer_conflict_detail), true)
                            Text(if (state.remote == null) stringResource(R.string.offer_absent) else
                                pluralStringResource(R.plurals.offer_remote_count, state.remote.materias.size, state.remote.materias.size), fontWeight = FontWeight.Bold)
                            state.data.catalogo.filter { it.id in state.remote?.materias.orEmpty() }.forEach {
                                Text("${it.nombre} · ${it.codigo}")
                            }
                            OutlinedButton({ onConflict(false) }) { Text(stringResource(R.string.offer_load_remote)) }
                            Button({ onConflict(true) }) { Text(stringResource(R.string.offer_keep_draft)) }
                        }
                    }
                    item {
                        OutlinedTextField(state.query, onQuery, Modifier.fillMaxWidth().testTag("offer-search"),
                            label = { Text(stringResource(R.string.offer_search)) }, singleLine = true, enabled = !state.saving,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }))
                    }
                    item { Text(stringResource(R.string.offer_select_help), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (state.visible.isEmpty()) item { Text(stringResource(if (state.data.catalogo.all { it.id in state.data.aprobadas })
                        R.string.offer_all_approved else R.string.offer_no_results)) }
                    state.visible.groupBy { it.semestre }.forEach { (semester, subjects) ->
                        item(key = "semester-$semester") { Text(stringResource(R.string.academic_semester, semester), fontWeight = FontWeight.Bold) }
                        items(subjects, key = { it.id }) { subject ->
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(Modifier.fillMaxWidth().toggleable(subject.id in state.draft,
                                    enabled = !state.refreshFailed && !state.saving && !state.conflict && !state.uncertain, role = Role.Checkbox,
                                    onValueChange = { focus.clearFocus(); onToggle(subject.id) }).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(subject.id in state.draft, onCheckedChange = null, enabled = !state.saving && !state.conflict && !state.uncertain)
                                    Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(subject.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${subject.codigo} · ${stringResource(durationLabel(subject))}", fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    val offer = state.data.oferta
                    if (offer == null || offer.materias.isEmpty()) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                Icon(painterResource(R.drawable.academic_offer), null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(if (offer == null) R.string.offer_absent else R.string.offer_empty), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(stringResource(if (offer == null) R.string.offer_absent_detail else R.string.offer_empty_detail), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (state.saved) Text(stringResource(R.string.offer_saved))
                                Button(onEdit, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Text(stringResource(if (offer == null) R.string.offer_select else R.string.offer_add))
                                }
                            }
                        }
                    } else {
                        item { Text(pluralStringResource(R.plurals.offer_count, offer.materias.size, offer.materias.size), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { OfferNotice(stringResource(R.string.offer_saved), stringResource(R.string.offer_saved_detail)) }
                        items(state.data.catalogo.filter { it.id in offer.materias }, key = { it.id }) { subject ->
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(subject.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${subject.codigo} · ${stringResource(durationLabel(subject))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        item { Button(onEdit, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.offer_edit)) } }
                        item { OutlinedButton({ onRecommendations?.invoke() }, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = onRecommendations != null) { Text(stringResource(R.string.offer_recommendations)) } }
                    }
                    item { TextButton(onRetry) { Text(stringResource(R.string.academic_refresh)) } }
                }
            }
            item { TextButton({ signOut = true }, enabled = !state.saving) { Text(stringResource(R.string.offer_sign_out)) } }
        }
    }
    if (discard || signOut) AlertDialog(onDismissRequest = { discard = false; signOut = false },
        title = { Text(stringResource(if (signOut) R.string.offer_sign_out else R.string.offer_discard)) },
        text = { Text(stringResource(if (state.dirty) R.string.offer_discard_detail else R.string.offer_sign_out_detail)) },
        confirmButton = { TextButton({ if (signOut) onSignOut() else onCancel(); discard = false; signOut = false }) {
            Text(stringResource(R.string.offer_confirm)) } },
        dismissButton = { TextButton({ discard = false; signOut = false }) { Text(stringResource(R.string.offer_stay)) } })
}
@Composable
private fun OfferNotice(title: String, detail: String, error: Boolean = false) {
    Surface(shape = RoundedCornerShape(18.dp), color = if (error) MaterialTheme.colorScheme.errorContainer else Color(0xFFEAF5EF)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = if (error) MaterialTheme.colorScheme.error else Color(0xFF21664A), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

package com.example.roadmap_utepsa.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.data.ProfileRegistration
import com.example.roadmap_utepsa.presentation.viewmodel.ProfileLoad
import com.example.roadmap_utepsa.presentation.viewmodel.ProfileUiState

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onNamesChange: (String) -> Unit,
    onSurnamesChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryLoad: () -> Unit,
    onDiscard: () -> Unit,
    onSignOut: () -> Unit
) {
    val focus = LocalFocusManager.current
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val existing = state.profile?.registration == ProfileRegistration.COMPLETE
    val completing = state.load == ProfileLoad.READY && !existing
    val requestSignOut = { if (state.hasChanges) confirmSignOut = true else onSignOut() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Provisional integration: Account and Summary are deliberately not destinations yet.
            TextButton(
                onClick = requestSignOut,
                enabled = !state.saving, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Icon(painterResource(R.drawable.profile_back), contentDescription = null, Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.profile_sign_out), color = MaterialTheme.colorScheme.onSurface)
            }
            Text(stringResource(if (completing) R.string.profile_title_create else R.string.profile_title_edit),
                fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(if (completing) R.string.profile_description_create else R.string.profile_description_edit),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, lineHeight = 21.sp)
            when (state.load) {
                ProfileLoad.LOADING -> {
                    Text(stringResource(R.string.profile_loading),
                        Modifier.align(Alignment.CenterHorizontally).semantics { liveRegion = LiveRegionMode.Polite })
                }
                ProfileLoad.ERROR -> {
                    Spacer(Modifier.height(56.dp))
                    Text(stringResource(R.string.profile_load_error), fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.profile_load_error_detail), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRetryLoad, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.profile_retry))
                    }
                }
                ProfileLoad.READY -> {
                    if (state.saved) ProfileNotice(
                        stringResource(R.string.profile_saved), stringResource(R.string.profile_saved_detail), success = true)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = state.names, onValueChange = onNamesChange,
                            label = { Text(stringResource(R.string.profile_names)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.saving,
                            isError = state.invalidNames,
                            supportingText = if (state.invalidNames) ({ Text(stringResource(R.string.profile_name_validation)) }) else null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                        )
                        OutlinedTextField(
                            value = state.surnames, onValueChange = onSurnamesChange,
                            label = { Text(stringResource(R.string.profile_surnames)) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !state.saving,
                            isError = state.invalidSurnames,
                            supportingText = if (state.invalidSurnames) ({ Text(stringResource(R.string.profile_name_validation)) }) else null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { if (!state.saving) { focus.clearFocus(); onSave() } })
                        )
                    }
                    state.profile?.plan?.let { plan ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(stringResource(R.string.profile_plan_label), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(plan.careerName, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(R.string.profile_plan_resolution, plan.resolution), fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(R.string.profile_plan_assigned), fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (state.profile?.registration == ProfileRegistration.INCOMPLETE) {
                        Text(stringResource(R.string.profile_incomplete), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (state.saveFailed) ProfileNotice(stringResource(R.string.profile_save_error),
                        stringResource(R.string.profile_save_error_detail))
                    if (state.awaitingConfirmation) ProfileNotice(stringResource(R.string.profile_pending),
                        stringResource(R.string.profile_pending_detail))
                    Button(
                        onClick = { focus.clearFocus(); onSave() }, enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) {
                        Text(stringResource(when {
                            state.saving -> R.string.profile_saving
                            state.saveFailed -> R.string.profile_retry_save
                            existing -> R.string.profile_save_changes
                            else -> R.string.profile_save
                        }))
                    }
                    if (existing) TextButton(
                        onClick = { if (state.hasChanges) confirmDiscard = true else onDiscard() },
                        enabled = !state.saving, modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text(stringResource(R.string.profile_discard)) }
                    else TextButton(onClick = requestSignOut, enabled = !state.saving,
                        modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.profile_sign_out))
                    }
                }
            }
        }
    }
    if (confirmDiscard || confirmSignOut) AlertDialog(
        onDismissRequest = { confirmDiscard = false; confirmSignOut = false },
        title = { Text(stringResource(R.string.profile_discard_title)) },
        text = { Text(stringResource(R.string.profile_discard_detail)) },
        confirmButton = {
            TextButton(onClick = {
                if (confirmSignOut) onSignOut() else onDiscard()
                confirmDiscard = false
                confirmSignOut = false
            }) { Text(stringResource(R.string.profile_discard_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { confirmDiscard = false; confirmSignOut = false }) {
                Text(stringResource(R.string.profile_keep_editing))
            }
        }
    )
}

@Composable
private fun ProfileNotice(title: String, detail: String, success: Boolean = false) {
    Surface(
        color = if (success) androidx.compose.ui.graphics.Color(0xFFE8F4EE) else MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = if (success) androidx.compose.ui.graphics.Color(0xFF116544) else MaterialTheme.colorScheme.error)
            Text(detail, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

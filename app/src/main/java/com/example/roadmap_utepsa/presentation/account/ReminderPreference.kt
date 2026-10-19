package com.example.roadmap_utepsa.presentation.account

import com.example.roadmap_utepsa.presentation.shared.components.AccessFeedback

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.presentation.notification.PendingReminders

@Composable
internal fun ReminderPreferenceContent(controller: PendingReminders) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    val state by controller.state.collectAsState()
    val context = LocalContext.current
    var explain by rememberSaveable(state.uid) { mutableStateOf(false) }
    var denied by rememberSaveable(state.uid) { mutableStateOf(false) }
    var requestedUid by rememberSaveable { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (requestedUid == controller.state.value.uid) denied = !granted
        requestedUid = null
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(if (state.enabled) texts.getString(R.string.account_enabled) else texts.getString(R.string.account_account_screen_11), color = MaterialTheme.colorScheme.primary)
        val label = stringResource(R.string.pending_setting)
        Switch(state.enabled, { enabled -> if (enabled) explain = true else controller.setEnabled(false) },
            Modifier.testTag("pending-toggle").semantics { contentDescription = label }, enabled = state.ready && !state.loading,
            colors = SwitchDefaults.colors(uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFFCCCFD6),
                uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                uncheckedThumbColor = androidx.compose.ui.graphics.Color.White))
    }
    Text(stringResource(R.string.pending_setting_detail), fontSize = 13.sp)
    AccessFeedback(state.message, true)
    if (!state.ready && !state.loading) TextButton(controller::retry) { Text(texts.getString(R.string.account_reminder_preference_21)) }
    if (denied || (state.enabled && !controller.deliveryAvailable())) {
        Text(stringResource(R.string.notification_permission_denied), fontSize = 13.sp)
        TextButton({ context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text(texts.getString(R.string.account_reminder_preference_22)) }
    }
    if (explain) AlertDialog(onDismissRequest = { explain = false },
        title = { Text(stringResource(R.string.pending_setting)) },
        text = { Text(stringResource(R.string.pending_permission_context)) },
        confirmButton = { TextButton({
            explain = false; controller.setEnabled(true)
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestedUid = state.uid; permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }) { Text(texts.getString(R.string.account_reminder_preference_23)) } },
        dismissButton = { TextButton({ explain = false }) { Text(texts.getString(R.string.account_reminder_preference_24)) } })
}

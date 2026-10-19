package com.example.roadmap_utepsa.presentation.account

import com.example.roadmap_utepsa.presentation.auth.messageRes
import com.example.roadmap_utepsa.R

import com.example.roadmap_utepsa.presentation.shared.components.AccountPage
import com.example.roadmap_utepsa.presentation.shared.components.AccessFeedback
import com.example.roadmap_utepsa.presentation.shared.components.SecretField

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.roadmap_utepsa.domain.auth.AuthIdentity
import com.example.roadmap_utepsa.domain.auth.AccessValidation
import com.example.roadmap_utepsa.domain.profile.ProfileData
import com.example.roadmap_utepsa.presentation.account.AccountState
import com.example.roadmap_utepsa.presentation.session.LockState

@Composable
private fun AccountCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp); content()
        }
    }
}

@Composable
fun AccountScreen(identity: AuthIdentity, profile: ProfileData?, state: AccountState, lock: LockState?, origin: String,
    onBack: () -> Unit, onEdit: () -> Unit, onSignOut: () -> Unit, onRecover: () -> Unit,
    onLink: (String?, String?) -> Unit, onBiometric: (Boolean) -> Unit, onRefresh: () -> Unit,
    reminders: com.example.roadmap_utepsa.presentation.notification.PendingReminders? = null) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    var linking by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<Int?>(null) }
    val hasPassword = "password" in identity.providers
    val hasGoogle = "google.com" in identity.providers
    AccountPage(texts.getString(R.string.account_account_screen_1), origin, onBack) {
        AccountCard(listOfNotNull(profile?.names, profile?.surnames).joinToString(" ")) { Text(identity.email.orEmpty()) }
        TextButton(onEdit, enabled = !state.busy) { Text(texts.getString(R.string.account_account_screen_2)) }
        AccessFeedback(state.message, state.error)
        AccountCard(texts.getString(R.string.account_account_screen_3)) {
            Text(texts.getString(if (hasPassword) R.string.account_password_active else R.string.account_password_inactive))
            Text(texts.getString(if (hasGoogle) R.string.account_google_active else R.string.account_google_inactive))
            if (!hasGoogle || !hasPassword) TextButton({ linking = true }, enabled = !state.busy && state.reconciled) {
                Text(if (!hasGoogle) texts.getString(R.string.account_account_screen_5) else texts.getString(R.string.account_account_screen_6))
            }
            if (hasPassword) TextButton(onRecover, enabled = !state.busy) { Text(texts.getString(R.string.account_account_screen_7)) }
            if (state.error || !state.reconciled) TextButton(onRefresh, enabled = !state.busy) { Text(texts.getString(R.string.account_account_screen_8)) }
            if (!state.reconciled) Text(texts.getString(R.string.account_account_screen_9))
        }
        AccountCard(texts.getString(R.string.account_account_screen_10)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (lock?.enabled == true) texts.getString(R.string.account_enabled) else texts.getString(R.string.account_account_screen_11), color = MaterialTheme.colorScheme.primary)
                Switch(lock?.enabled == true, onCheckedChange = onBiometric,
                    enabled = lock != null && !lock.loading && !lock.prompting && !state.busy,
                    colors = SwitchDefaults.colors(uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFFCCCFD6),
                        uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        uncheckedThumbColor = androidx.compose.ui.graphics.Color.White))
            }
            Text(texts.getString(R.string.account_account_screen_12), fontSize = 13.sp)
            AccessFeedback(lock?.message, true)
        }
        if (reminders != null) AccountCard(androidx.compose.ui.res.stringResource(com.example.roadmap_utepsa.R.string.pending_setting)) {
            ReminderPreferenceContent(reminders)
        }
        OutlinedButton(onSignOut, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(texts.getString(R.string.account_account_screen_13)) }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (linking) AlertDialog(onDismissRequest = { linking = false; password = ""; confirm = ""; currentPassword = "" },
        title = { Text(if (hasPassword) texts.getString(R.string.account_account_screen_5) else texts.getString(R.string.account_account_screen_6)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(texts.getString(R.string.account_confirm_current_access, identity.email.orEmpty()))
            if (hasPassword) SecretField(texts.getString(R.string.account_account_screen_14), currentPassword, { currentPassword = it })
            else {
                Text(texts.getString(R.string.account_account_screen_15))
                SecretField(texts.getString(R.string.account_account_screen_16), password, { password = it })
                SecretField(texts.getString(R.string.account_account_screen_17), confirm, { confirm = it })
                Text(texts.getString(R.string.account_account_screen_18), fontSize = 12.sp)
            }
            AccessFeedback(validation, true)
        } },
        confirmButton = { TextButton({
            validation = if (!hasPassword) AccessValidation.password(identity.email.orEmpty(), password, confirm)?.messageRes()
                else if (currentPassword.isEmpty()) R.string.access_current_password else null
            if (validation == null) {
                onLink(if (hasPassword) currentPassword else null, if (hasPassword) null else password)
                password = ""; confirm = ""; currentPassword = ""; linking = false
            }
        }) { Text(texts.getString(R.string.account_account_screen_19)) } },
        dismissButton = { TextButton({ linking = false; password = ""; confirm = ""; currentPassword = "" }) { Text(texts.getString(R.string.account_account_screen_20)) } })
}

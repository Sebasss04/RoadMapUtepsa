package com.example.roadmap_utepsa.presentation.auth

import com.example.roadmap_utepsa.presentation.shared.components.SecretField

import com.example.roadmap_utepsa.presentation.shared.components.AccessFeedback

import com.example.roadmap_utepsa.presentation.shared.components.AccountPage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.domain.auth.AccessValidation
import com.example.roadmap_utepsa.presentation.auth.AuthUiState
import kotlinx.coroutines.delay

@Composable
fun EmailFormScreen(registration: Boolean, state: AuthUiState, onBack: () -> Unit,
    onSubmit: (String, String) -> Unit) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<Int?>(null) }
    AccountPage(if (registration) texts.getString(R.string.auth_access_screens_4) else texts.getString(R.string.auth_access_screens_5), texts.getString(R.string.auth_access_screens_6), onBack) {
        if (!registration) Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.account_mail), null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(if (registration) texts.getString(R.string.auth_access_screens_7) else texts.getString(R.string.auth_access_screens_8),
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AccessFeedback(validation ?: state.errorMessage, error = true)
        AccessFeedback(state.notice)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), enabled = !state.isLoading,
            label = { Text(texts.getString(R.string.auth_access_screens_9)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        if (registration) {
            SecretField(texts.getString(R.string.auth_access_screens_10), password, { password = it }, !state.isLoading)
            SecretField(texts.getString(R.string.auth_access_screens_11), confirm, { confirm = it }, !state.isLoading)
            Text(texts.getString(R.string.auth_access_screens_12), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button({
            validation = if (registration) AccessValidation.password(email, password, confirm)?.messageRes()
                else if (!AccessValidation.email(email)) R.string.access_invalid_email else null
            if (validation == null) onSubmit(email.trim(), password)
        }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(if (state.isLoading) texts.getString(R.string.auth_access_screens_13) else if (registration) texts.getString(R.string.auth_access_screens_14) else texts.getString(R.string.auth_access_screens_15))
        }
        if (!registration) Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(texts.getString(R.string.auth_access_screens_16), fontWeight = FontWeight.Bold)
                Text(texts.getString(R.string.auth_access_screens_17))
            }
        }
        TextButton(onBack, enabled = !state.isLoading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (registration) texts.getString(R.string.auth_access_screens_18) else texts.getString(R.string.auth_access_screens_19))
        }
        if (registration) Text(texts.getString(R.string.auth_access_screens_20), fontSize = 12.sp)
    }
}

@Composable
fun VerificationScreen(state: AuthUiState, onResend: () -> Unit, onCheck: () -> Unit, onSignOut: () -> Unit) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    var confirmSignOut by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.nanoTime() / 1_000_000) }
    LaunchedEffect(state.resendAvailableAt) {
        while (true) { now = System.nanoTime() / 1_000_000; delay(1000) }
    }
    val seconds = ((state.resendAvailableAt - now + 999) / 1000).coerceAtLeast(0)
    AccountPage(texts.getString(R.string.auth_access_screens_21)) {
        Text(state.identity?.email.orEmpty())
        Text(texts.getString(R.string.auth_access_screens_22))
        AccessFeedback(state.errorMessage, true); AccessFeedback(state.notice)
        Button(onCheck, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(texts.getString(R.string.auth_access_screens_23)) }
        OutlinedButton(onResend, enabled = !state.isLoading && seconds == 0L, modifier = Modifier.fillMaxWidth()) {
            Text(if (seconds > 0) texts.getString(R.string.auth_resend_seconds, seconds) else texts.getString(R.string.auth_access_screens_24))
        }
        TextButton({ confirmSignOut = true }) { Text(texts.getString(R.string.auth_access_screens_25)) }
    }
    if (confirmSignOut) AlertDialog({ confirmSignOut = false }, title = { Text(texts.getString(R.string.auth_access_screens_25)) },
        text = { Text(texts.getString(R.string.auth_access_screens_26)) },
        confirmButton = { TextButton(onSignOut) { Text(texts.getString(R.string.auth_access_screens_25)) } },
        dismissButton = { TextButton({ confirmSignOut = false }) { Text(texts.getString(R.string.auth_access_screens_1)) } })
}

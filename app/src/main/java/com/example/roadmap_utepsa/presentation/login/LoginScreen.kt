package com.example.roadmap_utepsa.presentation.login

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.presentation.viewmodel.AuthUiState
import com.example.roadmap_utepsa.ui.theme.RoadMap_UtepsaTheme

@Composable
fun LoginScreen(state: AuthUiState, onSignIn: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    // La contraseña permanece solo en memoria, fuera del estado guardado de Android.
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var pendingMessage by remember { mutableStateOf<Int?>(null) }
    val errorMessage = state.errorMessageRes?.let { stringResource(it) } ?: state.errorMessage
    val focusManager = LocalFocusManager.current
    val invalidEmail = submitted && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val invalidPassword = submitted && password.isBlank()
    val submitEmail: () -> Unit = {
        submitted = true
        focusManager.clearFocus()
        if (Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && password.isNotBlank()) {
            pendingMessage = R.string.login_email_pending
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.roadmap_brand), contentDescription = null, modifier = Modifier.size(44.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.login_brand), fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.login_degree), fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.login_tagline), fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.login_welcome), fontSize = 34.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
                Text(stringResource(R.string.login_description), fontSize = 15.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                errorMessage?.let { message ->
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.login_error_title), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(message, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading,
                    label = { Text(stringResource(R.string.login_email)) }, singleLine = true,
                    isError = invalidEmail,
                    supportingText = if (invalidEmail) ({ Text(stringResource(R.string.login_invalid_email)) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading,
                    label = { Text(stringResource(R.string.login_password)) }, singleLine = true,
                    isError = invalidPassword,
                    supportingText = if (invalidPassword) ({ Text(stringResource(R.string.login_required_password)) }) else null,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !state.isLoading) {
                            Icon(painterResource(R.drawable.password_visibility),
                                contentDescription = stringResource(if (passwordVisible) R.string.login_hide_password else R.string.login_show_password),
                                modifier = Modifier.size(24.dp))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!state.isLoading) submitEmail() })
                )
                TextButton(
                    onClick = { focusManager.clearFocus(); pendingMessage = R.string.login_recovery_pending },
                    modifier = Modifier.align(Alignment.End), enabled = !state.isLoading,
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) { Text(stringResource(R.string.login_forgot_password), fontSize = 14.sp) }
                Button(onClick = submitEmail, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.login_sign_in), fontSize = 16.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HorizontalDivider(Modifier.weight(1f))
                Text(stringResource(R.string.login_or), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.weight(1f))
            }
            OutlinedButton(
                onClick = { focusManager.clearFocus(); onSignIn() }, enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.login_google_mark), fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(if (state.isLoading) R.string.login_loading else R.string.login_google), fontSize = 16.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.login_first_time), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { focusManager.clearFocus(); pendingMessage = R.string.login_register_pending }, enabled = !state.isLoading) {
                    Text(stringResource(R.string.login_create_account), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(stringResource(R.string.login_footer), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    pendingMessage?.let { message ->
        PendingAccessDialog(message = message, onDismiss = { pendingMessage = null })
    }
}

@Composable
private fun PendingAccessDialog(@StringRes message: Int, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.login_pending_title)) },
        text = { Text(stringResource(message)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.login_understood)) } })
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginPreview() {
    RoadMap_UtepsaTheme { LoginScreen(AuthUiState(), onSignIn = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginLoadingPreview() {
    RoadMap_UtepsaTheme { LoginScreen(AuthUiState(isLoading = true), onSignIn = {}) }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun LoginErrorPreview() {
    RoadMap_UtepsaTheme { LoginScreen(AuthUiState(errorMessage = "No se pudo iniciar sesión con Google. Inténtalo de nuevo."), onSignIn = {}) }
}

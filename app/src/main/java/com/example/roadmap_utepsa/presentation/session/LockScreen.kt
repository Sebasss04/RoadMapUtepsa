package com.example.roadmap_utepsa.presentation.session

import com.example.roadmap_utepsa.presentation.shared.components.AccountPage
import com.example.roadmap_utepsa.presentation.shared.components.AccessFeedback

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.roadmap_utepsa.R
import com.example.roadmap_utepsa.presentation.session.LockState

@Composable
fun LockScreen(state: LockState, onUnlock: () -> Unit, onSignOut: () -> Unit, dirty: Boolean = false) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    var confirm by remember { mutableStateOf(false) }
    Dialog({}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false,
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        AccountPage(texts.getString(R.string.session_lock_screen_1)) {
            Spacer(Modifier.height(32.dp))
            Icon(painterResource(R.drawable.account_fingerprint), null, Modifier.size(64.dp).align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.primary)
            Text(texts.getString(R.string.session_lock_screen_2))
            AccessFeedback(state.message, true)
            if (state.loading) CircularProgressIndicator()
            Button(onUnlock, enabled = !state.loading && !state.prompting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(texts.getString(R.string.session_unlock)) }
            OutlinedButton({ confirm = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(texts.getString(R.string.session_lock_screen_3)) }
            Text(texts.getString(R.string.session_lock_screen_4))
        }
        if (confirm) AlertDialog({ confirm = false }, title = { Text(texts.getString(R.string.session_lock_screen_5)) },
            text = { Text(if (dirty) texts.getString(R.string.session_lock_screen_6) else texts.getString(R.string.session_lock_screen_7)) },
            confirmButton = { TextButton(onSignOut) { Text(texts.getString(R.string.session_lock_screen_5)) } },
            dismissButton = { TextButton({ confirm = false }) { Text(texts.getString(R.string.session_lock_screen_8)) } })
    }
}

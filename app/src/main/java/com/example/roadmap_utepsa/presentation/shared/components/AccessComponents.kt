package com.example.roadmap_utepsa.presentation.shared.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadmap_utepsa.R

@Composable
fun AccountPage(title: String, backLabel: String? = null, onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    BackHandler(onBack != null) { onBack?.invoke() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (onBack != null) TextButton(onBack, contentPadding = PaddingValues(0.dp), modifier = Modifier.heightIn(min = 56.dp).testTag("account-back")) {
                Icon(painterResource(R.drawable.account_back), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(10.dp)); Text(backLabel ?: texts.getString(R.string.auth_access_screens_1), color = MaterialTheme.colorScheme.onSurface)
            } else Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 30.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
fun AccessFeedback(message: Int?, error: Boolean = false) {
    message?.let {
        Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp)) {
            Text(stringResource(it), Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SecretField(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean = true) {
    val texts = androidx.compose.ui.platform.LocalResources.current

    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), enabled = enabled, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton({ visible = !visible }) {
            Icon(painterResource(R.drawable.password_visibility), if (visible) texts.getString(R.string.auth_access_screens_2) else texts.getString(R.string.auth_access_screens_3), Modifier.size(24.dp))
        } })
}

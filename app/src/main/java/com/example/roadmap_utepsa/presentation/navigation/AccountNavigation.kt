package com.example.roadmap_utepsa.presentation.navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun AccountNavigation(onBack: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onBack, properties = DialogProperties(usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false), content = content)
}

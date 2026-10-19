package com.example.roadmap_utepsa.presentation.navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy

@Composable
internal fun AccountNavigation(protected: Boolean, onBack: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onBack, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
        securePolicy = if (protected) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit), content = content)
}

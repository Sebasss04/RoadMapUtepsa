package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import com.example.roadmap_utepsa.presentation.auth.LoginScreen
import com.example.roadmap_utepsa.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch
import com.example.roadmap_utepsa.presentation.auth.EmailFormScreen

@Composable
internal fun EntryNavigation(authViewModel: AuthViewModel, state: com.example.roadmap_utepsa.presentation.auth.AuthUiState,
    accessForm: String?, onForm: (String?) -> Unit, google: suspend () -> Result<String>,
    scope: kotlinx.coroutines.CoroutineScope) {
    if (accessForm != null) EmailFormScreen(accessForm == "register", state,
            { onForm(null); authViewModel.clearFeedback() }) { email, password ->
            scope.launch {
                if (accessForm == "register") authViewModel.emailAccess(email, password, true)
                else authViewModel.recover(email)
            }
        } else LoginScreen(state = state,
            onEmail = { email, password -> scope.launch { authViewModel.emailAccess(email, password, false) } },
            onRegister = { authViewModel.clearFeedback(); onForm("register") },
            onRecover = { authViewModel.clearFeedback(); onForm("recover") },
            onSignIn = { scope.launch { authViewModel.signInWithGoogle(google) } })
}

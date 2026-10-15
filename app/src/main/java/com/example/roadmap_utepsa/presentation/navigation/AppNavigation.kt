package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.roadmap_utepsa.data.FirestoreProfileRepository
import com.example.roadmap_utepsa.presentation.login.LoginScreen
import com.example.roadmap_utepsa.presentation.profile.ProfileScreen
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import com.example.roadmap_utepsa.presentation.viewmodel.ProfileViewModel
import com.example.roadmap_utepsa.services.GoogleAuthHelper
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(authViewModel: AuthViewModel, viewModelOwner: ViewModelStoreOwner) {
    val state by authViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val helper = remember(context) { GoogleAuthHelper(context) }

    val uid = state.uid
    if (state.isAuthenticated && uid != null) {
        // Rotation retains the form; a new authentication session gets a fresh server read.
        val profileViewModel = remember(uid, state.sessionVersion, viewModelOwner) {
            ViewModelProvider(viewModelOwner, ProfileViewModel.Factory(uid, FirestoreProfileRepository()))
                .get("profile:$uid:${state.sessionVersion}", ProfileViewModel::class.java)
        }
        val profileState by profileViewModel.state.collectAsState()
        ProfileScreen(
            state = profileState,
            onNamesChange = profileViewModel::changeNames,
            onSurnamesChange = profileViewModel::changeSurnames,
            onSave = profileViewModel::save,
            onRetryLoad = profileViewModel::retryLoad,
            onDiscard = profileViewModel::discardChanges,
            onSignOut = authViewModel::signOut
        )
    } else {
        LoginScreen(state = state, onSignIn = {
            scope.launch { authViewModel.signInWithGoogle(helper::getGoogleIdToken) }
        })
    }
}

package com.example.roadmap_utepsa.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
//import com.example.roadmap_utepsa.presentation.home.HomeScreen
import com.example.roadmap_utepsa.presentation.login.LoginScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {

    val auth = FirebaseAuth.getInstance()

    var usuario by remember {
        mutableStateOf(auth.currentUser)
    }

    if (usuario == null) {

        LoginScreen(
            onLoginSuccess = {
                usuario = auth.currentUser
            }
        )

    } else {

        /*HomeScreen(
            usuario = usuario!!,
            onLogout = {
                auth.signOut()
                usuario = null
            }
        )

         */
    }
}
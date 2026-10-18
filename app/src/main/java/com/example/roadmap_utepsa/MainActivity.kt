package com.example.roadmap_utepsa

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.roadmap_utepsa.services.SessionNotifications
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.ui.theme.RoadMap_UtepsaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
        val container = (application as RoadMapApplication).container
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModel.Factory(container.authRepository)
        )[AuthViewModel::class.java]
        setContent {
            RoadMap_UtepsaTheme {
                val authState by authViewModel.state.collectAsState()
                SessionNotifications(authState.uid, authState.sessionVersion)
                AppNavigation(authViewModel, this@MainActivity, container.profileRepository,
                    container.academicRepository, container.authRepository, container.preparacionRepository, container.ofertaRepository,
                    container.recomendacionesRepository)
            }
        }
    }
}

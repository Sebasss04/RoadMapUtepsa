package com.example.roadmap_utepsa

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.example.roadmap_utepsa.data.FirebaseAuthRepository
import com.example.roadmap_utepsa.presentation.viewmodel.AuthViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.roadmap_utepsa.presentation.navigation.AppNavigation
import com.example.roadmap_utepsa.ui.theme.RoadMap_UtepsaTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        val authViewModel = ViewModelProvider(
            this,
            AuthViewModel.Factory(FirebaseAuthRepository())
        )[AuthViewModel::class.java]
        setContent {
            RoadMap_UtepsaTheme {
                AppNavigation(authViewModel, this@MainActivity)
            }
        }
    }
}

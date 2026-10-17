package com.example.roadmap_utepsa.services

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.roadmap_utepsa.R

/** Un saludo por sesión; recomponer, rotar o abrir Perfil no repite el aviso. */
@Composable
fun SessionNotifications(uid: String?, sessionVersion: Long) {
    val context = LocalContext.current
    var handled by rememberSaveable(uid, sessionVersion) { mutableStateOf(false) }
    val currentUid by rememberUpdatedState(uid)
    var requestedUid by rememberSaveable { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && currentUid != null && currentUid == requestedUid) {
            NotificationHelper.showWelcome(context)
        } else if (!granted) {
            Toast.makeText(context, R.string.notification_permission_denied, Toast.LENGTH_LONG).show()
        }
        requestedUid = null
    }
    LaunchedEffect(uid, sessionVersion) {
        if (uid == null || handled) return@LaunchedEffect
        handled = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            NotificationHelper.showWelcome(context)
        } else {
            requestedUid = uid
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

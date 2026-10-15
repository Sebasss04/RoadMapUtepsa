package com.example.roadmap_utepsa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RoadMapRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF3F4),
    onPrimaryContainer = RoadMapInk,
    secondary = RoadMapMuted,
    background = Color.White,
    onBackground = RoadMapInk,
    surface = Color.White,
    onSurface = RoadMapInk,
    surfaceVariant = RoadMapSurface,
    onSurfaceVariant = RoadMapMuted,
    outline = Color(0xFF79747E),
    outlineVariant = RoadMapBorder,
    error = Color(0xFFAA2637),
    errorContainer = Color(0xFFFFF3F4),
    onErrorContainer = Color(0xFFAA2637)
)

// La entrega aprobada es clara; los colores del dispositivo no sustituyen la marca.
@Composable
fun RoadMap_UtepsaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.hostelattendance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Professional Dark Theme Colors
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueDark = Color(0xFF2563EB)
val SecondaryGreen = Color(0xFF10B981)
val AccentOrange = Color(0xFFEF4444)
val BackgroundDark = Color(0xFF0F172A)
val SurfaceDark = Color(0xFF1E293B)
val SurfaceLight = Color(0xFF334155)
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val BorderColor = Color(0xFF475569)
val SuccessGreen = Color(0xFF22C55E)
val WarningYellow = Color(0xFFFBBF24)
val ErrorRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryGreen,
    tertiary = AccentOrange,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

@Composable
fun HostelAttendanceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
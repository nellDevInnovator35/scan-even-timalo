package com.timalo.mobileevent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Indigo = Color(0xFF6366F1)
private val IndigoDark = Color(0xFF4F46E5)
private val Orange = Color(0xFFEA580C)
private val Red = Color(0xFFDC2626)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = IndigoDark,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Color(0xFF818CF8),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    error = Color(0xFFF87171)
)

object AppColors {
    val Preprod = Orange
    val Prod = Red
    val Accent = Indigo
}

@Composable
fun MobileEventTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}

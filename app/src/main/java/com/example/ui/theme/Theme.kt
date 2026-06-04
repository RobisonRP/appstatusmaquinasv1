package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate100,
    onSurface = Slate100,
    outline = Slate700,
    secondary = StatusDisponivel,
    tertiary = StatusPreventiva,
    surfaceVariant = Slate800,
    error = StatusManutencao
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
         colorScheme = DarkColorScheme,
         typography = Typography,
         content = content
    )
}

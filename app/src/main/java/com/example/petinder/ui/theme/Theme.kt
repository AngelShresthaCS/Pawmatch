package com.example.petinder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PetinderColors = lightColorScheme(
    primary = Color(0xFFE8755D),
    onPrimary = Color.White,
    secondary = Color(0xFF4F7560),
    background = Color(0xFFFFFCF7),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF28362E),
    onBackground = Color(0xFF28362E)
)

@Composable
fun PetinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PetinderColors, typography = Typography, content = content)
}
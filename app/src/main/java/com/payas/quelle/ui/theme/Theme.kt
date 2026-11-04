package com.payas.quelle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val QuelleColors = lightColorScheme(
    primary = WaterDeep,
    onPrimary = Surface,
    primaryContainer = BabyBlueLight,
    onPrimaryContainer = Ink,
    secondary = GreenDeep,
    onSecondary = Surface,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenDeep,
    tertiary = Water,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
)

private val QuelleShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun QuelleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuelleColors,
        shapes = QuelleShapes,
        content = content,
    )
}

package com.payas.quelle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val QuelleColors = lightColorScheme(
    primary = Midnight,
    onPrimary = Surface,
    primaryContainer = Champagne,
    onPrimaryContainer = Ink,
    secondary = Emerald,
    onSecondary = Surface,
    secondaryContainer = Sage,
    onSecondaryContainer = Emerald,
    tertiary = Gold,
    onTertiary = Surface,
    outline = LineStrong,
    outlineVariant = Line,
    surfaceVariant = Mist,
    error = Danger,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Highlight,
    surfaceContainer = Background,
    surfaceContainerHigh = Surface,
    surfaceContainerHighest = Mist,
    background = Background,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    onSurfaceVariant = InkMuted,
)

private val QuelleShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun QuelleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QuelleColors,
        shapes = QuelleShapes,
        content = content,
    )
}

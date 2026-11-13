package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LostEarthColorScheme = darkColorScheme(
    primary = AmberTactical,
    onPrimary = TacticalObsidian,
    secondary = SteelCyan,
    onSecondary = TacticalObsidian,
    tertiary = EmeraldVital,
    onTertiary = TacticalObsidian,
    background = TacticalObsidian,
    onBackground = TextPrimary,
    surface = GunmetalDark,
    onSurface = TextPrimary,
    surfaceVariant = GunmetalSurface,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = CrimsonThreat,
    onError = TextPrimary
)

private val TacticalShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

@Composable
fun LostEarthTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LostEarthColorScheme,
        typography = LostEarthTypography,
        shapes = TacticalShapes,
        content = content
    )
}

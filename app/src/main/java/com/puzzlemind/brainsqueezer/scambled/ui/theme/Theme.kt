package com.puzzlemind.brainsqueezer.scambled.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.puzzlemind.brainsqueezer.ui.theme.*

private val DarkColorPalette = darkColors(
    primary = BlueSapphire,
    primaryVariant = MidnightGreenEagleGreen,
    secondary = SkyBlueCrayola,
    secondaryVariant = MaximumBlue,
    background = LightBluee,
    surface = Color.LightGray,
    onPrimary = Color.White,
    onSecondary = Color.Gray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun BrainSqueezerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
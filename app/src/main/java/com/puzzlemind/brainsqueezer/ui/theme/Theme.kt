package com.puzzlemind.brainsqueezer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkColorPalette = darkColors(
    primary = CyanPrimaryColor,
    primaryVariant = CyanPrimaryDarkColor,
    secondary = CyanSecondaryColor,
    secondaryVariant = CyanSecondaryDarkColor,
    surface = CyanSecondaryLightColor,
    background = Color.Gray,
    onPrimary = Color.White,
    onSecondary = Color.Gray,
    onBackground = Color.White,
    onSurface = Color.Black,
)

private val BlueSappirePalette = darkColors(
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

val DarkCornflowerBlue: Color = Color(0xff023E8A)
val StarCommandBlue: Color = Color(0xff0077B6)
val CeruleanCrayola: Color = Color(0xff00B4D8)
val SkyBlue: Color = Color(0xff48CAE4)
val PowderBlue: Color = Color(0xffCAF0F8)
val ChampagnePink: Color = Color(0xffFFE5D9)

private val MorningBluePalette = darkColors(
    primary = StarCommandBlue,
    primaryVariant = DarkCornflowerBlue,
    secondary = SkyBlue,
    secondaryVariant = CeruleanCrayola,
    background = PowderBlue,
    surface = ChampagnePink,
    onPrimary = Color.White,
    onSecondary = Color.DarkGray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val BluePalette = darkColors(
    primary = BluePrimaryColor,
    primaryVariant = BluePrimaryDarkColor,
    secondary = BlueSecondaryColor,
    secondaryVariant = BlueSecondaryDarkColor,
    surface = BlueSecondaryLightColor,
    background = Color.LightGray,
    onPrimary = Color.White,
    onSecondary = Color.Gray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)


private val AmberPalette = darkColors(
    primary = AmberPrimaryColor,
    primaryVariant = AmberPrimaryDarkColor,
    secondary = AmberSecondaryColor,
    secondaryVariant = AmberSecondaryDarkColor,
    surface = AmberSecondaryLightColor,
    background = Color.Gray,
    onPrimary = Color.Black,
    onSecondary = Color.Gray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val CyanPalette = darkColors(
    primary = CyanPrimaryColor,
    primaryVariant = CyanPrimaryDarkColor,
    secondary = CyanSecondaryColor,
    secondaryVariant = CyanSecondaryDarkColor,
    surface = CyanSecondaryLightColor,
    background = Color.Gray,
    onPrimary = Color.White,
    onSecondary = Color.Gray,
    onBackground = Color.White,
    onSurface = Color.Black,
)

private val ParadisePinkPalette = darkColors(
    primary = FlickerPink,
    primaryVariant = TwilightLavender,
    secondary = WildOrchid,
    secondaryVariant = JazzberryJam,
    background = QueenPink,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Gray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val MiddleRedPalette = darkColors(
    primary = CopperRed,
    primaryVariant = MediumCarmine,
    secondary = LiverOrgan,
    secondaryVariant = PortlandOrange,
    background = Color.Gray,
    surface = MiddleRedd,
    onPrimary = Color.White,
    onSecondary = Color.DarkGray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val OrangePalette = darkColors(
    primary = OrangePrimaryColor,
    primaryVariant = OrangePrimaryDarkColor,
    secondary = OrangeSecondaryColor,
    secondaryVariant = OrangeSecondaryDarkColor,
    surface = OrangeSecondaryLightColor,
    background = Color.LightGray,
    onPrimary = Color.White,
    onSecondary = Color.DarkGray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

private val CaribbeanOceanPalette = darkColors(
    primary = caribbean,
    primaryVariant = BlueNcS,
    secondary = MiddleBlueGreen,
    secondaryVariant = MiddleBlue,
    background = CadetBlue,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Gray,
    onBackground = Color.Black,
    onSurface = Color.Black,
)


private val AmaranthRedPalette = darkColors(
    primary = amaranth_red,
    primaryVariant = space_cadet,
    secondary = imperial_red,
    secondaryVariant = manatee,
    background = alice_blue,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,

    )

@Composable
fun BrainSqueezerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = MorningBluePalette


    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
val colors = mutableListOf<Colors>(
    BlueSappirePalette,
    BluePalette,
    CaribbeanOceanPalette,
    AmberPalette,
    MiddleRedPalette,
    OrangePalette,
    AmaranthRedPalette,

    DarkColorPalette

)

@Composable
fun MCQLevelsTheme(
    colorsThemeIndex: Int = 0,
    content: @Composable() () -> Unit
) {

    val themeIndex:Int = if(colorsThemeIndex < 1){
        0
    }
    else{
        colorsThemeIndex -1
    }
    MaterialTheme(
        colors = colors[themeIndex],
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun MCQTheme(
    colorsThemeIndex: Int = 0,
    content: @Composable() () -> Unit
) {

    val themeIndex:Int = if(colorsThemeIndex < 1){
         colorsThemeIndex
    }
    else{
        colorsThemeIndex -1
    }

    MaterialTheme(
        colors = colors[themeIndex],
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
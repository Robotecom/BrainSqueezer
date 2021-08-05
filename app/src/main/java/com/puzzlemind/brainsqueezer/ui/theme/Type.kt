package com.puzzlemind.brainsqueezer.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.puzzlemind.brainsqueezer.R

val NawarFont = FontFamily(
    Font(R.font.nawar_regular, FontWeight.Light),
    Font(R.font.nawar_regular, FontWeight.Normal),
    Font(R.font.nawar_regular, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.nawar_regular, FontWeight.Medium),
    Font(R.font.nawar_regular, FontWeight.Bold)
)
// Set of Material typography styles to start with
val Typography = Typography(defaultFontFamily = NawarFont
//    body1 = TextStyle(
//        fontFamily = NawarFont,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp
//    )
    /* Other default text styles to override
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    */
)


val jozoor_font = FontFamily(
    Font(R.font.jozoor_font, FontWeight.Light),
    Font(R.font.jozoor_font, FontWeight.Normal),
    Font(R.font.jozoor_font, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.jozoor_font, FontWeight.Medium),
    Font(R.font.jozoor_font, FontWeight.Bold)
)

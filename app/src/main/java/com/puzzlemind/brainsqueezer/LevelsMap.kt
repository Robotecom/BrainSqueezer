package com.puzzlemind.brainsqueezer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.roundToInt


@Composable
fun LevelMap(modifier: Modifier,divider: Int = 3,content: @Composable () -> Unit) {

    Layout(content =  content ,modifier = modifier){ measurables, constraints ->

        val itemWidth = constraints.maxWidth / divider

        val placeables = measurables.map {

            val placeable = it.measure(constraints = Constraints.fixed(itemWidth, itemWidth))
            placeable
        }


        val width = itemWidth * divider
        val height = itemWidth * ceil(measurables.size / divider.toFloat()).roundToInt()

        layout(width = width, height = height) {

            placeables.forEachIndexed { index: Int, placeable: Placeable ->

                val yHop = index / divider
                val x = (index % divider) * placeable.width
                val y = yHop * placeable.height
                placeable.placeRelative(x, y)

            }
        }
    }
}



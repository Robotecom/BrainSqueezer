package com.puzzlemind.brainsqueezer.scambled

import androidx.annotation.Keep
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.R
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import com.puzzlemind.brainsqueezer.mcq.FinalResultClickable
import com.puzzlemind.brainsqueezer.mcq.finalClicked
import com.puzzlemind.brainsqueezer.ui.theme.TransparentBlackLight
import com.puzzlemind.brainsqueezer.utils.painterFile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt





@Composable
fun PuzzleBoard(dimension: Int,
                content: @Composable () -> Unit){

    Column(
        modifier = Modifier
            .defaultMinSize()
            .aspectRatio(1f, false)
            .border(width = 2.dp,color = MaterialTheme.colors.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        PuzzleLayout(
            modifier = Modifier,
            puzzleSize = dimension,
            content = content
        )
    }
}


@Composable
fun PuzzleLayout(modifier: Modifier = Modifier,
                 puzzleSize:Int,
                 content:@Composable () -> Unit
){
    var width = 0
    var tileSpace = 0
    Layout(content =  content , measurePolicy = { measurables, constraints ->


        //sum of spaces between tiles
        val spaces = constraints.maxWidth* ( 4)/100

        tileSpace = spaces/(puzzleSize + 1)
        val tileDimension = (constraints.maxWidth - spaces)/puzzleSize
        val placeables = measurables.map { measurable ->

            val placeable = measurable.measure(constraints = Constraints.fixed(tileDimension,tileDimension))
            placeable
        }

        width = constraints.maxWidth
        val height = constraints.maxHeight

        layout(width = width,height = height){

            placeables.forEachIndexed {index , placeable ->

                val x = index%puzzleSize * ( placeable.width + tileSpace ) + tileSpace
                val y = (index/puzzleSize) * ( placeable.width + tileSpace) + tileSpace
                placeable.place(x,y)

            }
        }
    },modifier = modifier)

}

//enu class for allowed directions a tile can have
//tile can have only one direction at any moment
enum class TileDirection{
    CENTER,UP,DOWN,LEFT,RIGHT
}




fun Modifier.swipeToMove(direction:TileDirection,
                         tileWidth: Dp,
                         onMoved: () -> Unit
): Modifier = composed {
    // This `Animatable` stores the horizontal offset for the element.
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    pointerInput(Unit) {
        // Used to calculate a settling position of a fling animation.
        val decay = splineBasedDecay<Float>(this)
        // Wrap in a coroutine scope to use suspend functions for touch events and animation.
        coroutineScope {
            while (true) {
                // Wait for a touch down event.
                val pointerId = awaitPointerEventScope { awaitFirstDown().id }
                // Interrupt any ongoing animation.
                offsetX.stop()
                offsetY.stop()
                // Prepare for drag events and record velocity of a fling.
                val velocityTrackerX = VelocityTracker()
                val velocityTrackerY = VelocityTracker()
                // Wait for drag events.
                if (direction == TileDirection.CENTER)
                    break
                awaitPointerEventScope {
                    drag(pointerId = pointerId){pointerInputChange ->
                        launch {
                            println("drag value:${pointerInputChange.positionChange().x}")
                            val changeX = pointerInputChange.positionChange().x
                            val changeY = pointerInputChange.positionChange().y
                            when(direction){
                                TileDirection.RIGHT ->
                                    if (changeX > 0) {
                                        var xCoordinates = offsetX.value + changeX
                                        if (xCoordinates > tileWidth.toPx()){
                                            xCoordinates = tileWidth.toPx()
                                        }
                                        offsetX.snapTo(xCoordinates)
                                    }else{
                                        return@launch
                                    }
                                TileDirection.LEFT ->
                                    if (changeX < 0){
                                        var xCoordinates = offsetX.value + changeX
                                        if (xCoordinates < -tileWidth.toPx()){
                                            xCoordinates = -tileWidth.toPx()
                                        }
                                        offsetX.snapTo(xCoordinates)
                                    }else{
                                        return@launch
                                    }

                                TileDirection.UP ->
                                    if (changeY < 0){
                                        var yCoordinates = offsetY.value + changeY
                                        if (yCoordinates < -tileWidth.toPx()){
                                            yCoordinates = -tileWidth.toPx()
                                        }
                                        offsetY.snapTo(yCoordinates)
                                    }else{
                                        return@launch
                                    }
                                else  ->
                                    if (changeY > 0) {
                                        var yCoordinates = offsetY.value + changeY
                                        if (yCoordinates > tileWidth.toPx()){
                                            yCoordinates = tileWidth.toPx()
                                        }
                                        offsetY.snapTo(yCoordinates)
                                    }else{
                                        return@launch
                                    }
                            }

                            when(direction){
                                TileDirection.RIGHT,TileDirection.LEFT ->
                                    velocityTrackerX.addPosition(pointerInputChange.uptimeMillis, pointerInputChange.position)
                                TileDirection.UP,TileDirection.DOWN ->
                                    velocityTrackerY.addPosition(pointerInputChange.uptimeMillis, pointerInputChange.position)
                                else ->
                                    println()
                            }
                        }
                    }

                }
                // Dragging finished. Calculate the velocity of the fling.
                val velocityX = velocityTrackerX.calculateVelocity().x
                val velocityY = velocityTrackerY.calculateVelocity().y
                // Calculate where the element eventually settles after the fling animation.
                val targetOffsetX = decay.calculateTargetValue(offsetX.value, velocityX)
                val targetOffsetY = decay.calculateTargetValue(offsetY.value, velocityY)
                // The animation should end as soon as it reaches these bounds.
                offsetX.updateBounds(
                    lowerBound = -size.width.toFloat(),
                    upperBound = size.width.toFloat()
                )
                offsetY.updateBounds(
                    lowerBound = -size.width.toFloat(),
                    upperBound = size.width.toFloat()
                )
                launch {
                    when(direction){
                        TileDirection.RIGHT ,
                        TileDirection.LEFT ->
                            if (targetOffsetX.absoluteValue <= size.width) {
                                // Not enough velocity; Slide back to the default position.
                                offsetX.animateTo(targetValue = 0f, initialVelocity = velocityX)
                            } else {
                                // Enough velocity to slide away the element to the edge.
                                offsetX.animateDecay(velocityX, decay)
                                // The element was swiped away.
                                onMoved()
//                                offsetX.snapTo(0f)
                            }
                        else ->
                            if (targetOffsetY.absoluteValue <= size.width) {
                                // Not enough velocity; Slide back to the default position.
                                offsetY.animateTo(targetValue = 0f, initialVelocity = velocityY)
                            } else {
                                // Enough velocity to slide away the element to the edge.
                                offsetY.animateDecay(velocityY, decay)
                                // The element was swiped away.
                                onMoved()
//                                offsetY.snapTo(0f)
                            }
                    }

                }

            }
        }
    }
        // Apply the horizontal offset to the element.
        .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
}


@ExperimentalAnimationApi
@Composable
fun PuzzleTile1(
    direction: TileDirection,
    resourceId: Int,
    uri:String,
    order: Int,
    currentPos: Int,
    modifier: Modifier,
    hintShown: Boolean,
    isSpace: Boolean,
    onMoved: (tileIndex: Int) -> Unit
) {


    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

        BoxWithConstraints {

            Box(
                modifier = if (true) {
                    modifier
                        .fillMaxSize()
                        .swipeToMove(direction = direction, onMoved = {
                            onMoved(currentPos)

                        }, tileWidth = this.maxWidth)
                } else {
                    modifier
                        .fillMaxSize()
                        .clickable {
                            if (direction != TileDirection.CENTER)
                                onMoved(currentPos)
                        }
                }

            ) {

                if (!isSpace)
                    Image(
                        modifier = modifier.fillMaxSize().background(color = Color.White),
                        painter = painterFile(uri = uri,id = resourceId),
                        contentScale = ContentScale.Crop,
                        contentDescription = ""
                    )


                if (hintShown && !isSpace) {
                    Text(
                        text = "${order + 1}",
                        modifier = Modifier
                            .wrapContentSize()
                            .background(TransparentBlackLight)
                            .padding(4.dp),
                        fontSize = 12.sp,
                        color = Color.White
                    )

                }
            }
        }
    }
}

@Keep
data class EventCallBack(
    val onMoved: (tileIndex: Int) -> Unit,
    val onNumberHintShow: () -> Unit,
    val onShowPreviewHint: () -> Unit,
    val onRestartPuzzle: () -> Unit,
    val onStartPuzzle: () -> Unit,
    val onShowAd:() -> Unit,
    val finalResultClickable: FinalResultClickable,
    val onDismissError:() -> Unit,
    val onBackPressed:() -> Unit,
    val onSoundOnClick: () -> Unit
) {

}


fun clickEventCombiner(
    onMoved: (tileIndex: Int) -> Unit = {},
    onNumberHintShow: () -> Unit = {},
    onShowPreview: () -> Unit = {},
    onRestartPuzzle: () -> Unit = {},
    onStartPuzzle: () -> Unit = {},
    onShowAd: () -> Unit = {},
    finalResultClickable: FinalResultClickable = finalClicked(),
    onDismissError:() -> Unit = {},
    onBackPressed: () -> Unit = {},
    onSoundOnClick: () -> Unit = {}

) = EventCallBack(
    onMoved = onMoved,
    onNumberHintShow = onNumberHintShow,
    onShowPreviewHint = onShowPreview,
    onRestartPuzzle = onRestartPuzzle,
    onStartPuzzle = onStartPuzzle,
    onShowAd = onShowAd,
    finalResultClickable = finalResultClickable,
    onDismissError = onDismissError,
    onBackPressed = onBackPressed,
    onSoundOnClick = onSoundOnClick
)

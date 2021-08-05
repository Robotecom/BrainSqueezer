package com.puzzlemind.brainsqueezer

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzlemind.brainsqueezer.ui.theme.StrongRed

enum class TimerEnums {
    Empty, Full
}

@Composable
fun Timer(
    modifier: Modifier,
    diameter: Int,
    time: Int = 0,
    backgroundColor: Color?,
    start: Boolean,
    withText: Boolean
) {


    var timerState by remember { mutableStateOf(TimerEnums.Empty) }
    timerState = if (time == 0) {
        TimerEnums.Empty
    } else {
        if (start) {
            TimerEnums.Empty
        } else {
            TimerEnums.Full
        }
    }

    val timerTransitionData = updateTransitionState(
        targetState = timerState,
        time = time * 1000
    )


    Box(modifier = modifier.wrapContentSize()) {


        Canvas(modifier = Modifier
            .size(diameter.dp)
            .clip(CircleShape)
            .background(backgroundColor ?: timerTransitionData.backgroundColor)
            .padding(8.dp),
            onDraw = {


                val centerOffset = Offset(size.width / 2, (size.height / 2))
                val halfSize = Size(size.width / 2, size.height / 2)
                val startAngle = -90f
                val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round)


                drawCircle(
                    color = Color.DarkGray,
                    radius = halfSize.width,
                    center = centerOffset,
                    style = stroke
                )

                drawArc(
                    color = Color.Green,
                    startAngle = startAngle,
                    sweepAngle = timerTransitionData.sweepAngle,
                    useCenter = false,
                    style = stroke,
                    topLeft = Offset(0f, 0f),
                )
            })

        if (withText) {

            val timeText = kotlin.math.ceil(timerTransitionData.timeRemain).toInt().toString()

            Text(text = timeText,
                fontSize = 32.sp,
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier.align(Alignment.Center)
                )

        }
    }
}

class TimerTransitionData(
    sweepAngle: State<Float>,
    timeRemain: State<Float>,
    backgroundColor: State<Color>
) {
    val sweepAngle by sweepAngle
    val timeRemain by timeRemain
    val backgroundColor by backgroundColor
}

@Composable
fun updateTransitionState(
    targetState: TimerEnums,
    time: Int,
): TimerTransitionData {

    val transition = updateTransition(targetState, label = "")
    val ani = animateIntAsState(targetValue = 0,
        animationSpec = tween(durationMillis = time),
        finishedListener = { println("Finished Listener") })
    ani.value

    val sweepAngle = transition.animateFloat(
        transitionSpec = {
            when {
                TimerEnums.Empty isTransitioningTo TimerEnums.Full ->
                    tween(
                        durationMillis = 300,
                        delayMillis = 0,
                        easing = FastOutSlowInEasing
                    )

                else -> tween(durationMillis = time, easing = LinearEasing)
            }
        }, label = ""
    ) { state ->
        if (state == TimerEnums.Empty) {
            0f
        } else {
            360f
        }

    }
    val timeRemain = transition.animateFloat(
        transitionSpec = {
            when {
                TimerEnums.Empty isTransitioningTo TimerEnums.Full ->
                    tween(durationMillis = 300, delayMillis = 0, easing = FastOutSlowInEasing)
                else ->
                    tween(durationMillis = time, delayMillis = 0, easing = LinearEasing)
            }
        }, label = ""
    ) { state ->
        if (state == TimerEnums.Empty) 0f else (time / 1000).toFloat()

    }
    val color = transition.animateColor(
        transitionSpec = {
            when {
                TimerEnums.Empty isTransitioningTo TimerEnums.Full ->
                    tween(durationMillis = 300, delayMillis = 0, easing = FastOutSlowInEasing)
                else ->

                    tween(durationMillis = time, delayMillis = 0, easing = FastOutSlowInEasing)
            }
        }, label = ""
    ) { state ->
        if (state == TimerEnums.Empty) StrongRed else MaterialTheme.colors.primary
    }


    return remember(transition) {
        TimerTransitionData(
            sweepAngle = sweepAngle,
            timeRemain = timeRemain,
            backgroundColor = color
        )
    }
}

@SuppressLint("StaticFieldLeak")
object SoundUtil {

    private var mContext: Context? = null
    private var isPlaying: Boolean = false
    private val soundList = mutableMapOf<SoundType, Int>()

    fun init(context: Context) {
        mContext = context

        soundList[SoundType.Alert] = R.raw.clean

        soundList[SoundType.Correct] = R.raw.correct_answer
        soundList[SoundType.Incorrect] = R.raw.incorrect_answer
        soundList[SoundType.CompleteLevel] = R.raw.leve_complete
        soundList[SoundType.LostLevel] = R.raw.loss_game
        soundList[SoundType.ClockTicking] = R.raw.clock_ticking
        soundList[SoundType.RemoveTwoChoices] = R.raw.remove_two_choices
        soundList[SoundType.ChangeQuestion] = R.raw.change_question
        soundList[SoundType.Unsuccessful] = R.raw.unsuccessful_process

    }

    fun getResourceId(sound: SoundType): Int {
        return soundList[sound] ?:R.raw.unsuccessful_process
    }


    fun play(sound: SoundType) {
        isPlaying = true

    }



}

sealed class SoundType(resId: Int) {
    object Alert : SoundType(R.raw.clean)
    object Correct : SoundType(R.raw.correct_answer)
    object Incorrect : SoundType(R.raw.correct_answer)
    object CompleteLevel : SoundType(R.raw.leve_complete)
    object LostLevel : SoundType(R.raw.loss_game)
    object ClockTicking : SoundType(R.raw.clock_ticking)
    object RemoveTwoChoices : SoundType(R.raw.remove_two_choices)
    object ChangeQuestion : SoundType(R.raw.change_question)
    object Unsuccessful : SoundType(R.raw.unsuccessful_process)
}
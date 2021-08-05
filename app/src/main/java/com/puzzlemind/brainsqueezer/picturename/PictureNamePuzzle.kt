package com.puzzlemind.brainsqueezer

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.puzzlemind.brainsqueezer.ui.theme.ScrimBlack
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.imageloading.ImageLoadState
import com.google.accompanist.imageloading.isFinalState
import com.google.android.gms.ads.rewarded.RewardedAd
import com.puzzlemind.brainsqueezer.mcq.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlin.random.Random


@ExperimentalAnimationApi
@Composable
fun WhoIsInPicScaffold(
    activity: Activity,
    whoIsClickable: WhoIsClickable = whoIsClicked(),
    whosInPicState: WhoIsInPicState
) {

    BackHandler(enabled = whosInPicState.backPressedEnabled, onBack = whoIsClickable.onBackPress) {


        val mRewardedAd: RewardedAd? = null
        Box {
            Column {

                if (whosInPicState.showReady) {
                    Dialog(onDismissRequest = {
                        whoIsClickable.startTest()

                    }, properties = DialogProperties()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(
                                    RoundedCornerShape(12.dp)
                                )
                                .background(Color.LightGray)
                                .padding(24.dp), verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.pic_name_first_note),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                Header(
                    state = whosInPicState,
                    whoIsClickable = whoIsClickable
                )
                Spacer(modifier = Modifier.size(16.dp))

                AnimatedVisibility(
                    visible = whosInPicState.showRequest,
                    enter = fadeIn() + slideInVertically()
                ) {
                    QuestionCompose(modifier = Modifier.padding(8.dp), uiState = whosInPicState)

                }

                Spacer(modifier = Modifier.size(4.dp))

                if (whosInPicState.showChoices)
                    MCQ_AnswersLayout() {

                        for (i in 0 until whosInPicState.choices.second.size) {
                            AnimatedVisibility(
                                visible = true,
                                enter = expandIn(
                                    Alignment.Center,
                                    clip = true
                                )
                            ) {

                                key(Random.nextInt(100)) {

                                    SingleAnswer(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        number = (i + 1).toString(),
                                        correctAnswer = whosInPicState.choices.first,
                                        answer = whosInPicState.choices.second[i],
                                        correctAnsBg = whosInPicState.choices.first == whosInPicState.choices.second[i] && whosInPicState.checkCorrectAnw
                                    ) { s ->

                                        whoIsClickable.onAnswerSelected(s)
                                    }

                                }


                            }
                        }
                    }

                if (whosInPicState.showResult) {
                    FinalResult(
                        testResult = whosInPicState.testResult,
                        onClickButton = whoIsClickable.finalResultClickable
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                HelpChoices(
                    buttonState = whosInPicState.buttonState,
                    onWatchAdClick = whoIsClickable.onShowVideoAd,
                    onRemoveTwoChoices = whoIsClickable.onRemoveChoices,
                    {},
                    showChangeButt = false
                )


                AdBanner(0)

            }

            if(whosInPicState.showVideoAd) {

                Box{
                    RewardedVideoAd(
                        "ca-app-pub-3940256099942544/5224354917",
                        activity,
                        whoIsClickable.onRewardedVideoAdCallBacks
                    )
                }

            }

        }
    }

}

@Composable
fun WhoIsImage(
    modifier: Modifier,
    whoIsInPicState: WhoIsInPicState,
    resumeCountDown: (Any?) -> Unit
) {

    val stateUi = viewModel<WhoIsInPicViewModel>().ui_state.collectAsState()

    val circleDiameter = 208


    val painter = rememberGlidePainter(request = stateUi.value.currentImage.second, fadeIn = true)
    LaunchedEffect(painter) {
        snapshotFlow { painter.loadState }
            .filter { it.isFinalState() }
            .collect { result ->
                when (result) {
                    is ImageLoadState.Success -> {
                        println("result:$result")
                        resumeCountDown(painter.request)
                    }
                    is ImageLoadState.Error -> println("result:$result")
                    is ImageLoadState.Loading -> println("result:$result")
                    else -> println("result:$result")
                }
            }
    }

    if (whoIsInPicState.internetConnectError) {
        imageLoadingError()
    }
    Column(
        modifier = modifier
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .size((circleDiameter).dp)
                .background(MaterialTheme.colors.surface),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painter,
                modifier = Modifier
                    .size((circleDiameter).dp),
                contentDescription = "",
                contentScale = ContentScale.Crop
            )

            if (!stateUi.value.hideTimer)
                Box(contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize()) {

                    Timer(
                        modifier = Modifier.padding(8.dp),
                        diameter = 32,
                        time = whoIsInPicState.timerState.time,
                        backgroundColor = Color.Transparent,
                        start = whoIsInPicState.timerState.start,
                        withText = false
                    )
                }

            if (stateUi.value.currentImage.first != "") {
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    Text(
                        stateUi.value.currentImage.first,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(ScrimBlack)
                            .padding(8.dp),
                        fontSize = 16.sp,
                        color = MaterialTheme.colors.onPrimary
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }


        }

    }

}

@Composable
fun imageLoadingError() {
    AlertDialog(onDismissRequest = {
    }, title = {
        Text(text = "Error")
    },
        dismissButton = {
            Button(onClick = { }) {
                Text(text = "Close")

            }
        },
        confirmButton = {
            Button(onClick = { }) {
                Text(text = "Reload")

            }
        }
    )
}


@Composable
fun Header(
    whoIsClickable: WhoIsClickable,
    state: WhoIsInPicState,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        HeaderTop()
        Spacer(modifier = Modifier.size(8.dp))
        WhoIsImage(
            modifier = Modifier,
            whoIsInPicState = state,
            resumeCountDown = { any -> whoIsClickable.resumeCountDown(any) }
        )
    }

}

@Composable
fun HeaderTop() {

    Row(
        modifier = Modifier
            .height(80.dp)
            .background(MaterialTheme.colors.primaryVariant),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .weight(1f)
                .wrapContentSize()
        ) {
            Text(
                text = "Score:320",
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.primary)
                    .padding(8.dp),
                color = MaterialTheme.colors.onPrimary


            )
        }
        Box(
            modifier = Modifier
                .padding(8.dp)
                .weight(1f)
                .align(CenterVertically)
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.who),
                    style = MaterialTheme.typography.h4,
                    color = Color.White
                )
//                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Box(
            modifier = Modifier
                .padding(8.dp)
                .weight(1f)
                .wrapContentSize()
        ) {
            Text(
                text = "Score:320",
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.primary)
                    .padding(8.dp),
                color = MaterialTheme.colors.onPrimary
            )

        }

    }
}


@Composable
fun QuestionCompose(modifier: Modifier, uiState: WhoIsInPicState) {

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .wrapContentHeight()
            .animateContentSize()
            .background(MaterialTheme.colors.surface)
            .padding(8.dp)

    ) {

        Column(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart)
                .padding(0.dp, 0.dp, 4.dp, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(id = R.string.question_who_label),
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text =
                stringResource(
                    id = R.string.q_left,
                    uiState.currentIndex,
                    uiState.totalImages
                ),

                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(MaterialTheme.colors.primary)
                .padding(8.dp)
        ) {

            Text(
                text = uiState.request,
                style = MaterialTheme.typography.h6,
                color = Color.White,
                lineHeight = 20.sp
            )
        }
    }


}

@Composable
fun MCQ_AnswersLayout(content: @Composable () -> Unit) {

    Layout(content = content) { measurables, constraints ->

        val placeables = measurables.map { measurable ->
            val placeable =
                measurable.measure(constraints = Constraints.fixedWidth(constraints.maxWidth / 2))
            placeable
        }
        val width = constraints.maxWidth
        val height = placeables[0].height * 2
        layout(width = width, height = height) {

            placeables.forEachIndexed { index, placeable ->
                val x = index % 2 * placeable.width
                val y = index / 2 * placeable.height
                placeable.placeRelative(x, y)
            }
        }
    }
}

data class WhoIsClickable(
    val imgLoadingError: () -> Unit,
    val resumeCountDown: (Any?) -> Unit,
    val startTest: () -> Unit,
    val onAnswerSelected: (String) -> Unit,
    val finalResultClickable: FinalResultClickable,
    val onRewardedVideoAdCallBacks: RewardedVideoAdCallBacks,
    val onShowVideoAd: () -> Unit,
    val onRemoveChoices: () -> Unit,
    val onBackPress: () -> Unit
)

fun whoIsClicked(
    imgLoadingError: () -> Unit = {},
    resumeCountDown: (Any?) -> Unit = {},
    startTest: () -> Unit = {},
    onAnswerSelected: (String) -> Unit = {},
    finalResultClickable: FinalResultClickable = finalClicked(),
    rewardedVideoAdCallBacks: RewardedVideoAdCallBacks = rewardCallback(),
    onShowVideoAd: () -> Unit = {},
    onRemoveChoices: () -> Unit = {},
    onBackPress: () -> Unit = {}
) =
    WhoIsClickable(
        imgLoadingError = imgLoadingError,
        resumeCountDown = { any -> resumeCountDown(any) },
        startTest = startTest,
        onAnswerSelected = { answer -> onAnswerSelected(answer) },
        finalResultClickable = finalResultClickable,
        onRewardedVideoAdCallBacks = rewardedVideoAdCallBacks,
        onShowVideoAd = onShowVideoAd,
        onRemoveChoices = onRemoveChoices,
        onBackPress = onBackPress
    )
package com.puzzlemind.brainsqueezer.mcq

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.Keep
import androidx.annotation.LayoutRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import kotlin.math.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.puzzlemind.brainsqueezer.data.Question
import com.puzzlemind.brainsqueezer.ui.theme.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.puzzlemind.brainsqueezer.BuildConfig
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.Timer
import com.puzzlemind.brainsqueezer.utils.isOnline
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@SuppressLint("InflateParams")
@ExperimentalAnimationApi
@Composable
fun MCQ_Scaffold(
    onClickButton: Clickable = clickCombined(),
    mcqState: MCQState
) {


    BackHandler(mcqState.backPressedEnabled, onClickButton.onBackPressed) {
        Box {


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalGradientScrim(
                        color = MaterialTheme.colors.secondaryVariant,
                        startYPercentage = 1f,
                        endYPercentage = 0f
                    )
                    .verticalScroll(rememberScrollState())
            ) {

                val deep = 0
                val headerHeight = 96 + deep
                val headerRadius = 40
                val timerPos = headerHeight - deep - headerRadius + 4
                val timerDiameter = (headerRadius - 4) * 2
                Box(modifier = Modifier.wrapContentHeight()) {
                    Column(modifier = Modifier.wrapContentHeight()) {
                        HeaderTop(
                            deepness = deep,
                            height = headerHeight,
                            radius = headerRadius,
                            headerState = mcqState.headerState,
                            onClickButton = onClickButton
                        )

                        QuestionCompose(
                            modifier = Modifier.padding(8.dp),
                            mcqState.currentQuestion,
                            mcqState = mcqState,
                            radiusDp = headerRadius
                        )
                    }

                    Timer(
                        modifier = Modifier
                            .align(alignment = TopCenter)
                            .padding(0.dp, timerPos.dp, 0.dp, 0.dp),
                        diameter = timerDiameter,
                        time = mcqState.timerTime,
                        start = mcqState.startTimer,
                        withText = true,
                        backgroundColor = null

                    )
                }


                AnswerChoices(answers = mcqState.currentQuestion.choices,
                    correctAnswer = mcqState.currentQuestion.answer,
                    answerChosen = { answer -> onClickButton.onChoice(answer) },
                    checkCorrectAns = mcqState.checkCorrectAnswer)

                Spacer(modifier = Modifier.weight(1f))


                HelpChoices(
                    buttonState = mcqState.helpButtonState,
                    onWatchAdClick = onClickButton.onWatchAd,
                    onRemoveTwoChoices = onClickButton.onRemoveTwo,
                    onChangeQuestion = onClickButton.onChangeQuestion
                )

                if (mcqState.firstOpen) {
                    StartDialog( mcqState.loadingLevel,onClickButton)
                }

                if (mcqState.showFinalResult) {

                    FinalResult(mcqState.testResult, onClickButton.finalResultClickable)
                }

                //AdBanner Shown at the bottom of screen
                AdBanner(R.layout.mcq_ad_banner)

            }

            if (mcqState.showVideoAd) {
                //show Video rewarded Ad
                AndroidView(modifier = Modifier.fillMaxSize(),
                    update = {

                        //this function will be invoked whenever AndroidView recompose

                    },
                    factory = { context ->

                        LayoutInflater.from(context)
                            .inflate(R.layout.videw_ad_layout, null, false)
                    })

            }


            if (mcqState.showError) {
                //show error dialog whenever there is
                ErrorDialog(mcqState, onClickButton)
            }
        }
    }

}

@Composable
fun StartDialog( loadingLevel:Boolean,onClickButton: Clickable) {

    Dialog(onDismissRequest = {
        if (!loadingLevel) {
            onClickButton.onStartTest()
        }
    }) {

        Column(modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colors.surface)
            .animateContentSize()
            .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(id = R.string.alert_first_open),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface
                )

            Spacer(modifier = Modifier.size(16.dp))

            if (loadingLevel)
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.size(16.dp))
            Button(onClick = {
                    onClickButton.onStartTest()

            },enabled = !loadingLevel
                ,modifier = Modifier
                    .width(120.dp)
                    .clip(RoundedCornerShape(16.dp))) {
                Text(text = stringResource(id = R.string.start_label),
                    fontSize = 20.sp)
            }
        }

    }

}

@Composable
fun FinalResult(testResult: TestResult, onClickButton: FinalResultClickable) {
    Dialog(onDismissRequest = { }) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            FinalResultStarBar(testResult = testResult)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colors.surface)
                    .wrapContentSize(align = Center)
                    .padding(24.dp)

            ) {

                val isInfoShown = remember{mutableStateOf(false)}

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    if (testResult.numberOfStars == 3 )
                    Image(painterResource(
                        id = R.drawable.trophy_filled
                    ),
                        modifier = Modifier.size(56.dp),
                        contentDescription = null)

                    Text(
                        text = stringResource(id = if (testResult.passed) R.string.success else R.string.failure),
                        fontFamily = jozoor_font,
                        color = MaterialTheme.colors.primaryVariant,
                        style = MaterialTheme.typography.h4
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Row(modifier = Modifier.padding(4.dp)) {

                        Text(
                            text = stringResource(id = R.string.correct_answers),
                            fontFamily = jozoor_font,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )

                        Text(
                            text = stringResource(
                                id = R.string.number_convert,
                                testResult.correctAnswers
                            ),
                            modifier = Modifier.padding(12.dp, 0.dp),
                            fontFamily = jozoor_font,
                            color = MaterialTheme.colors.onSurface
                        )

                    }
                    Row(modifier = Modifier.padding(4.dp)) {

                        Text(
                            text = stringResource(id = R.string.time_remaining),
                            fontFamily = jozoor_font,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )

                        Text(
                            text = stringResource(
                                id = R.string.number_convert,
                                testResult.timeRemaining
                            ),
                            modifier = Modifier.padding(12.dp, 0.dp),
                            fontFamily = jozoor_font,
                            color = MaterialTheme.colors.onSurface
                        )

                    }

                    Spacer(modifier = Modifier.size(4.dp))

                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(end = 8.dp)
                        .background(Color.Gray))

                    Row(modifier = Modifier.padding(4.dp)) {
                        Text(
                            text = stringResource(id = R.string.total_score),
                            fontFamily = jozoor_font,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )
                        Text(
                            text = stringResource(
                                id = R.string.number_convert,
                                testResult.score
                            ),
                            modifier = Modifier.padding(12.dp, 0.dp),
                            fontFamily = jozoor_font,
                            color = MaterialTheme.colors.onSurface
                        )

                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    if (!isOnline(context = LocalContext.current))
                    Row(horizontalArrangement = Arrangement.Center,verticalAlignment = Alignment.CenterVertically) {

                        Text(text = stringResource(id = R.string.connect_to_internet_to_share_result),
                            style = MaterialTheme.typography.caption
                            )
                        
                        
                        IconButton(onClick = { 
                                             isInfoShown.value = true
                        },modifier = Modifier.size(24.dp)) {

                            Icon(Icons.Filled.Info,contentDescription = null)
                        }

                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Row(modifier = Modifier.wrapContentSize(Center)) {
                        Button(onClick = {

                            onClickButton.onRedoTest()
                        },shape = CircleShape,modifier = Modifier.size(56.dp)
                        ) {

                            Image(painter = painterResource(id = R.drawable.ic_go_back_arrow),
                                contentDescription = null

                            )
                        }

                        Spacer(modifier = Modifier.size(8.dp))
                        Button(onClick = {
                            onClickButton.onNextTest()

                        },shape = CircleShape,modifier = Modifier.size(56.dp)) {

                            Image(painter = painterResource(id = R.drawable.ic_right_arrow),
                                contentDescription = null,
                                modifier = Modifier.clip(CircleShape)
                            )
                        }

                        if (testResult.passed) {
                            Spacer(modifier = Modifier.size(8.dp))
                            Button(onClick = {
                                onClickButton.onShareTestResult()

                            }, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_baseline_share_24),
                                    contentDescription = null,
                                    modifier = Modifier.clip(CircleShape)
                                )
                            }
                        }
                    }

                }

                if (isInfoShown.value)
                Dialog(onDismissRequest = { isInfoShown.value = false }) {

                    Text(text = stringResource(id = R.string.climbup_leaderboard_info),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colors.surface)
                            .wrapContentSize()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                        )
                }
            }
        }
    }
}


@Composable
fun FinalResultStarBar(testResult: TestResult) {
    StarBar(testResult.numberOfStars,modifier = Modifier
        .graphicsLayer {
            shape = RoundedCornerShape(topStart = 64.dp, topEnd = 64.dp)
            clip = true
        }
        .height(72.dp)
        .background(MaterialTheme.colors.secondaryVariant)
        .padding(start = 40.dp, end = 40.dp, top = 16.dp))
}

@Composable
fun RewardedVideoAd(
    rewardId: String,
    mcqActivity: Activity,
    onClickButton: RewardedVideoAdCallBacks
) {
    Box {


        RequestVideoAd(
            rewardId = rewardId,
            activity = mcqActivity,
            onRewardCallback = onClickButton
        )
    }
}

@Composable
fun ErrorDialog(mcqState: MCQState, onClickButton: Clickable) {
    AlertDialog(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.LightGray)
            .wrapContentSize(Center)
            .padding(12.dp),
        text = {
            Text(
                text = mcqState.error,
                color = MaterialTheme.colors.onSurface,
                fontFamily = jozoor_font
            )

        },
        title = {},
        onDismissRequest = { onClickButton.onRewardCallback.onDismissError() },
        contentColor = Color.Black,
        backgroundColor = Color.LightGray,
        confirmButton = {
            Button(onClick = { onClickButton.onRewardCallback.onDismissError() }) {
                Text(text = stringResource(id = R.string.okay))
            }
        }
    )
}

@SuppressLint("InflateParams")
@Composable
fun AdBanner(@LayoutRes adBannerLayout:Int) {
    AndroidView(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
        factory = { context ->

            val adLayout = if (BuildConfig.DEBUG){
                R.layout.ad_banner
            }else{
                adBannerLayout
            }
            val adView = LayoutInflater.from(context)
                .inflate(adLayout, null, false)
                .findViewById<AdView>(R.id.adView)



            adView.adListener = object : AdListener() {

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                }
            }
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            adView
        }, update = {

        })
}


@Composable
fun HeaderTop(
    deepness: Int,
    height: Int = 80,
    radius: Int,
    headerState: HeaderState,
    onClickButton: Clickable
) {

    val headerBgColor = MaterialTheme.colors.primaryVariant
    Box {

        Row(modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .drawBehind {
                headerDeepTopBackground(
                    deepness = deepness.dp.toPx(),
                    radiusDp = radius,
                    headerBgColor
                )
            }
            .padding(12.dp)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(16.dp))
                        .width(104.dp)
                        .background(MaterialTheme.colors.primary)
                    ,horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {


                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = stringResource(id = R.string.level_number, headerState.currentLevel),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onPrimary
                    )

                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = TopCenter
            ) {

                Spacer(modifier = Modifier.height(8.dp))
              Image(modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onClickButton.onMuteSound()
                    },
                    contentDescription = null,
                    painter = painterResource(id = if (headerState.soundOn){R.drawable.ic_baseline_volume_up_24}else{R.drawable.ic_baseline_volume_off_24}))

            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .width(104.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colors.primary),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Text(
                        modifier = Modifier.padding(8.dp),
                        text = stringResource(id = R.string.number_convert, headerState.balance.toInt()),
                        fontSize = 18.sp,
                        color = MaterialTheme.colors.onPrimary,
                        textAlign = TextAlign.Center

                    )
                    Image(painterResource(id = R.drawable.dollar_filled_strocked),
                        contentDescription = "",
                        modifier = Modifier
                            .size(24.dp)
                            .padding(2.dp)
                    )

                }
            }


        }

    }
}


@Composable
fun QuestionCompose(modifier: Modifier, question: Question, mcqState: MCQState, radiusDp: Int) {

    val headerBgColor = MaterialTheme.colors.surface
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .wrapContentHeight()
            .animateContentSize()
            .drawBehind {
                headerBottomBackground(radiusDp = radiusDp, backgroundColor = headerBgColor)
            }
            .padding(8.dp, 0.dp + radiusDp.dp, end = 8.dp, bottom = 8.dp)

    ) {

        Column(
            modifier = Modifier
                .width(48.dp)
                .wrapContentHeight()
                .padding(0.dp, 0.dp, 4.dp, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(id = R.string.question_label),
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.onSurface,
                fontFamily = NawarFont
            )



            Text(
                text = stringResource(
                    id = R.string.question_left,
                    mcqState.currentProgress,
                    mcqState.totalQueCount
                ),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface
            )


        }

        Column(Modifier.weight(1f)) {

            Box(modifier = Modifier
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(MaterialTheme.colors.primaryVariant)
                .padding(4.dp)){

                Text(text = question.category,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onPrimary)
            }


            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .background(MaterialTheme.colors.primary)
                    .padding(8.dp)
            ) {

                Text(
                    text = question.question,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.onPrimary,
                    fontFamily = NawarFont,
                    lineHeight = 24.sp
                )
            }


        }
    }


}

@ExperimentalAnimationApi
@Composable
fun AnswerChoices(answers: List<String>, correctAnswer:String, answerChosen: (String) -> Unit,checkCorrectAns:Boolean) {

    var visible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .animateContentSize()
    ) {

        for (i in answers.indices) {

            scope.launch {
                delay(400)
                visible = true

            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(0.1f) + slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(200, i * 60)
                ),
                exit = fadeOut(0.3f, animationSpec = tween(200)) +
                        slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(200, i * 60)
                        )
            ) {

                SingleAnswer(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(4.dp),
                    number = stringResource(id = R.string.number_convert, i + 1),
                    answer = answers[i],
                    correctAnswer = correctAnswer,
                    onAnswerClick = { answer ->

                        visible = false
                        answerChosen(answer)
                    },
                    correctAnsBg = answers[i].trim() == correctAnswer.trim() && checkCorrectAns
                )


            }


        }
    }

}

@Composable
fun StarBar(numberOfStars: Int,modifier: Modifier) {

    Row(modifier = modifier,
        verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier
                .rotate(-15f)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (numberOfStars >= 1) VividOrangeLight else Color.Gray),
            painter = painterResource(id = R.drawable.ic_baseline_stars_24), contentDescription = ""
        )
        Spacer(modifier = Modifier.size(4.dp))
        Column(Modifier.defaultMinSize()) {
            Image(
                modifier = Modifier
                    .rotate(0f)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (numberOfStars >= 2) VividOrangeLight else Color.Gray),
                painter = painterResource(id = R.drawable.ic_baseline_stars_24),
                contentDescription = ""
            )
            Spacer(modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.size(4.dp))

        Image(
            modifier = Modifier
                .rotate(15f)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (numberOfStars > 2) VividOrangeLight else Color.Gray),
            painter = painterResource(id = R.drawable.ic_baseline_stars_24),
            contentDescription = ""
        )
    }
}


@Composable
fun SingleAnswer(
    modifier: Modifier,
    number: String,
    correctAnswer:String,
    answer: String,
    correctAnsBg:Boolean ,
    onAnswerClick: (String) -> Unit,
) {


    val color = MaterialTheme.colors.primary
    val animColor = remember { Animatable(color) }
    val scope = rememberCoroutineScope()

    DisposableEffect(key1 = answer) {
        onDispose {
            scope.launch {
                animColor.animateTo(color)

            }
        }
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (correctAnsBg) {
                    WarmGreen
                } else {
                    animColor.value
                }
            )
            .border(1.dp, color = MaterialTheme.colors.surface, shape = CircleShape)
            .wrapContentHeight(Alignment.CenterVertically)
            .clickable {

                scope.launch {
                    //                    animColor.animateTo(StrongBlueDark)
                    //                    delay(100)
                    animColor.animateTo(if (correctAnswer.trim() == answer.trim()) WarmGreen else DeepOrangeDark)
                    delay(300)
                    onAnswerClick(answer)

                }
            }
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        Text(
            text = number,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.surface)
                .padding(4.dp)
                .wrapContentSize(Center),
            color = MaterialTheme.colors.onSurface,

            style = MaterialTheme.typography.h5
        )
        Text(
            text = answer, modifier = Modifier
                .weight(1f)
                .padding(8.dp, 0.dp),
            lineHeight = 18.sp,
            color = MaterialTheme.colors.onPrimary,
            style = MaterialTheme.typography.subtitle1,
            fontFamily = NawarFont
        )
    }
}

@Composable
fun HelpChoices(
    buttonState: ButtonState,
    onWatchAdClick: () -> Unit,
    onRemoveTwoChoices: () -> Unit,
    onChangeQuestion: () -> Unit,
    showVidButt:Boolean = true,
    showChangeButt:Boolean = true,
    showRemoveButt:Boolean = true
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {

        if (showVidButt)
        Button(
            onClick = { onWatchAdClick() },
            modifier = Modifier
                .size(56.dp)
                .border(BorderStroke(1.dp, Color.White), CircleShape)
                .clip(CircleShape),
            enabled = buttonState.watchAdEnabled
        ) {

            Icon(painter = painterResource(id = R.drawable.ic__40_quiz), contentDescription = "")
        }

        Spacer(modifier = Modifier.size(16.dp))
        if (showRemoveButt)
        Button(
            onClick = onRemoveTwoChoices,
            modifier = Modifier
                .size(56.dp)
                .border(BorderStroke(1.dp, Color.White), CircleShape)
                .clip(CircleShape),
            enabled = buttonState.removeTwoEnabled
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_remove_two),
                contentDescription = "")
        }

        Spacer(modifier = Modifier.size(16.dp))
        if (showChangeButt)
        Button(
            onClick = onChangeQuestion,
            modifier = Modifier
                .size(56.dp)
                .border(BorderStroke(1.dp, Color.White), CircleShape)
                .clip(CircleShape),
            enabled = buttonState.changeQuestionEnabled
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic__28_light_bulb_1),
                contentDescription = ""
            )
        }
    }
}

fun DrawScope.headerBottomBackground(deepness: Float = 32.dp.toPx(), radiusDp: Int, backgroundColor: Color) {

    val headerHeight = size.height
    val radius = radiusDp.dp.toPx()
    val path = Path()
    path.moveTo(0f, 0f)
    val h = radius - deepness
    val b = sqrt(
        radius
            .toDouble()
            .pow(2) - h.pow(2)
    )
    val theta = acos(b / radius) * 180 / PI

    path.lineTo((size.width / 2 - b).toFloat(), 0f)

    path.arcTo(
        Rect(
            Offset((size.width / 2 - radius), -h - radius),
            Offset((size.width / 2 + radius), deepness)
        ),
        startAngleDegrees = -180f,
        sweepAngleDegrees = -(180 - theta * 2).toFloat() - 180, false
    )

    path.lineTo(size.width, 0f)
    path.lineTo(size.width, headerHeight)


    path.lineTo(0f, headerHeight)
    path.close()

    drawPath(path, backgroundColor, style = Fill)
}


fun DrawScope.headerDeepTopBackground(deepness: Float = 32.dp.toPx(), radiusDp: Int, backgroundColor:Color) {

    val headerHeight = size.height
    val radius = radiusDp.dp.toPx()
    val path = Path()
    path.moveTo(0f, 0f)
    path.lineTo(size.width, 0f)
    path.lineTo(size.width, headerHeight)

    val b = sqrt(
        radius
            .toDouble()
            .pow(2) - deepness.pow(2)
    )
    val theta = acos(b / radius) * 180 / PI

    path.lineTo((size.width / 2 + b).toFloat(), headerHeight)
    path.arcTo(
        Rect(
            Offset((size.width / 2 - radius), headerHeight - deepness - radius),
            Offset((size.width / 2 + radius), (headerHeight - deepness + radius))
        ),
        startAngleDegrees = theta.toFloat(),
        sweepAngleDegrees = -(360 - (180 - theta * 2)).toFloat(), false
    )

    path.lineTo(0f, headerHeight)
    path.close()

    drawPath(path, backgroundColor, style = Fill)
}

@Composable
fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    // Safely update the current `onBack` lambda when a new one is provided
    val currentOnBack by rememberUpdatedState(onBack)
    // Remember in Composition a back callback that calls the `onBack` lambda
    val backCallback = remember {
        object : OnBackPressedCallback(enabled) {
            override fun handleOnBackPressed() {
                currentOnBack()
            }
        }
    }
    // On every successful composition, update the callback with the `enabled` value
    SideEffect {
        backCallback.isEnabled = enabled
    }
    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
    }.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, backDispatcher) {
        // Add callback to the backDispatcher
        backDispatcher.addCallback(lifecycleOwner, backCallback)
        // When the effect leaves the Composition, remove the callback
        onDispose {
            backCallback.remove()
        }
    }

    content()
}

@Keep
data class Clickable constructor(
    val onChoice: (String) -> Unit,
    val onRemoveTwo: () -> Unit,
    val onWatchAd: () -> Unit,
    val onChangeQuestion: () -> Unit,
    val onStartTest: () -> Unit,
    val onBackPressed: () -> Unit,
    val onRewardCallback: RewardedVideoAdCallBacks,
    val finalResultClickable: FinalResultClickable,
    val onMuteSound: () -> Unit

    )
@Keep
data class RewardedVideoAdCallBacks(
    val onError: (String) -> Unit,
    val onRewardEarned: () -> Unit,
    val onDismiss: () -> Unit,
    val onDismissError: () -> Unit,
    val onAdShowing: () -> Unit
)

fun rewardCallback(
    onRewardEarned: () -> Unit = {},
    onError: (String) -> Unit = {},
    onDismiss: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onAdShowing: () -> Unit = {}
) = RewardedVideoAdCallBacks(
    onDismiss = onDismiss,
    onError = { errMsg -> onError(errMsg) },
    onRewardEarned = onRewardEarned,
    onDismissError = onDismissError,
    onAdShowing = onAdShowing
)


fun clickCombined(
    onChoice: (String) -> Unit = {},
    onRemoveTwo: () -> Unit = {},
    onWatchAd: () -> Unit = {},
    onChangeQuestion: () -> Unit = {},
    onStartTest: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    onRewardCallback: RewardedVideoAdCallBacks = rewardCallback(),
    finalResultClickable: FinalResultClickable = finalClicked(),
    onMuteSound:() -> Unit = {}

) = Clickable(
    onChoice = { answer -> onChoice(answer) },
    onRemoveTwo = onRemoveTwo,
    onWatchAd = onWatchAd,
    onChangeQuestion = onChangeQuestion,
    onStartTest = onStartTest,
    onBackPressed = onBackPressed,
    onRewardCallback = onRewardCallback,
    finalResultClickable = finalResultClickable,
    onMuteSound = onMuteSound
)


//class that hold data of the header part of MCQ
@Keep
data class MCQState(
    var headerState: HeaderState = HeaderState(),
    var questions: MutableList<Question> = mutableListOf(),
    var currentQuestion: Question = Question(),
    var currentProgress: Int = 1,
    var totalQueCount: Int = 1,
    var correctAnswers: Int = 0,
    var wrongAnswers: Int = 0,
    var showVideoAd: Boolean = false,
    var timerTime: Int = 0,
    var startTimer: Boolean = false,
    var helpButtonState: ButtonState = ButtonState(),
    var showFinalResult: Boolean = false,
    var testResult: TestResult = TestResult(),
    var backPressedEnabled: Boolean = true,
    var error: String = "",
    var showError: Boolean = false,
    var checkCorrectAnswer: Boolean = false,
    var loadingLevel: Boolean = false,
    var firstOpen:Boolean = true,
)

@Keep
data class ButtonState(
    var changeQuestionEnabled: Boolean = true,
    var removeTwoEnabled: Boolean = true,
    var watchAdEnabled: Boolean = true
)
@Keep
data class TestResult(
    var numberOfStars: Int = 0,
    var correctAnswers: Int = 0,
    var incorrectAnswers: Int = 0,
    var timeRemaining:Int = 0,
    var passed: Boolean = false,
    var score:Int = 0
) {

    fun zeroVariables() {
        correctAnswers = 0
        incorrectAnswers = 0
        passed = false
    }
}
@Keep
data class FinalResultClickable(
    val onRedoTest:() -> Unit,
    val onNextTest:() -> Unit,
    val onShareTestResult:() -> Unit,
    val onDoubleScore:() -> Unit
)

fun finalClicked(
    onRedoTest: () -> Unit ={},
    onNextTest: () -> Unit = {},
    onShareTestResult: () -> Unit = {},
    onDoubleScore: () -> Unit = {}
                 ) = FinalResultClickable(
    onRedoTest = onRedoTest,
    onNextTest = onNextTest,
    onShareTestResult = onShareTestResult,
    onDoubleScore = onDoubleScore
                 )

@Keep
data class HeaderState(
    var balance: Float = 0f,
    var questionNumber: Int = 0,
    var currentLevel: Int = 0,
    var stars: Int = 0,
    var soundOn:Boolean = true

)
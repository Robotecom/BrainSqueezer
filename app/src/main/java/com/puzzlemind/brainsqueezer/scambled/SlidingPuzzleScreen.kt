package com.puzzlemind.brainsqueezer.scambled

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.mcq.*
import com.puzzlemind.brainsqueezer.ui.theme.TROPHY_COLOR
import com.puzzlemind.brainsqueezer.ui.theme.TransparentBlackLight
import com.puzzlemind.brainsqueezer.ui.theme.jozoor_font
import com.puzzlemind.brainsqueezer.utils.isOnline
import com.puzzlemind.brainsqueezer.utils.painterFile
import java.text.DecimalFormat


@ExperimentalAnimationApi
@Composable
fun PuzzleScreen(uiState: PuzzleUIState, eventCallBack: EventCallBack) {

    BackHandler(
        enabled = uiState.backHandlerEnabled,
        onBack = eventCallBack.onBackPressed
    ) {

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val context = LocalContext.current

            if (uiState.toast != 0)
            LaunchedEffect(key1 = uiState.toast){
                Toast.makeText(context,uiState.toast,Toast.LENGTH_LONG).show()
            }

            val isLess600 = remember { mutableStateOf(this.maxWidth < 600.dp) }

            Column(Modifier.fillMaxSize()) {

                Header(uiState, onVolumeChanged = eventCallBack.onSoundOnClick)

                Row(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {

                    PuzzleWidget(
                        uiState = uiState,
                        onMoved = eventCallBack.onMoved
                    )

                    if (!isLess600.value)
                        Footer(
                            uiState = uiState,
                            showNumberHint = eventCallBack.onNumberHintShow,
                            showPreviewHint = eventCallBack.onShowPreviewHint,
                            showAd = eventCallBack.onShowAd
                        )

                }

                if (isLess600.value)
                    Footer(
                        uiState = uiState,
                        showNumberHint = eventCallBack.onNumberHintShow,
                        showPreviewHint = eventCallBack.onShowPreviewHint,
                        showAd = eventCallBack.onShowAd

                    )

            }

            if (uiState.startDialog) {
                Dialog(onDismissRequest = {

                    if (!uiState.loadingLevel)
                        eventCallBack.onStartPuzzle()

                }) {

                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colors.surface)
                            .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (uiState.loadingLevel)
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = stringResource(
                                id =
                            uiState.startDialogText
                            ),
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                eventCallBack.onStartPuzzle()
                            }, enabled = !uiState.loadingLevel,
                            shape = RoundedCornerShape(16.dp)
                        ) {

                            Text(text = stringResource(id = R.string.start_label))
                        }

                    }

                }
            }

            if (uiState.blockUI){
                Box(modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        println("UI Blocker***********************")
                    }){

                }
            }

            if (uiState.testFinished)
                SlidingPuzzleFinalResult(
                    uiState,
                    onClickButton = eventCallBack.finalResultClickable,
                )

            if (uiState.showAd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = TransparentBlackLight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.wrapContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = stringResource(id = R.string.wait_for_ad),
                            fontSize = 20.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp)
                        )

                    }
                }
            }

            if (uiState.showError) {
                Dialog(onDismissRequest = {
                    eventCallBack.onDismissError()
                }) {

                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .wrapContentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(color = MaterialTheme.colors.surface)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = uiState.errorMsg,
                            color = MaterialTheme.colors.onSurface,
                            fontSize = 18.sp
                        )
                    }

                }
            }
        }
    }
}

@Composable
fun SlidingPuzzleFinalResult(
    uiState: PuzzleUIState,
    onClickButton: FinalResultClickable
) {
    Dialog(onDismissRequest = {
        onClickButton.onNextTest()
    }) {

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            FinalResultStarBar(
                testResult = TestResult(
                    numberOfStars = uiState.finalResult.stars,
                )
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colors.surface)
                    .wrapContentSize(align = Alignment.Center)
                    .padding(24.dp)

            ) {

                val isInfoShown = remember { mutableStateOf(false) }
                val context = LocalContext.current

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Spacer(modifier = Modifier.height(8.dp))


                    if (uiState.finalResult.wonTrophy) {
                        Image(
                            painterResource(
                                id = R.drawable.trophy_filled
                            ),
                            modifier = Modifier.size(56.dp),
                            contentDescription = null
                        )
                    }

                    Text(
                        text = stringResource(id = if (uiState.finalResult.passed) R.string.success else R.string.scrambled_failure),
                        fontFamily = jozoor_font,
                        color = MaterialTheme.colors.primaryVariant,
                        style = MaterialTheme.typography.h4
                    )

                    if (!uiState.finalResult.passed)
                    Text(textAlign = TextAlign.Center,
                        text = stringResource(id = uiState.failureMsg),
                        fontFamily = jozoor_font,
                        color = MaterialTheme.colors.primaryVariant,
                        style = MaterialTheme.typography.caption,
                        lineHeight = 12.sp
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Row(modifier = Modifier.padding(4.dp)) {

                        Text(
                            text = stringResource(id = R.string.playtime),
                            fontFamily = jozoor_font,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )

                        Text(
                            text = stringResource(
                                id = R.string.time_spent,
                                uiState.finalResult.timeSpent
                            ),
                            modifier = Modifier.padding(12.dp, 0.dp),
                            fontFamily = jozoor_font,
                            color = MaterialTheme.colors.onSurface
                        )

                    }

                    Row(modifier = Modifier.padding(4.dp)) {

                        Text(
                            text = stringResource(id = R.string.moves_have_been),
                            fontFamily = jozoor_font,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )

                        Text(
                            text = stringResource(
                                id = R.string.number_convert,
                                uiState.finalResult.moves
                            ),
                            modifier = Modifier.padding(12.dp, 0.dp),
                            fontFamily = jozoor_font,
                            color = MaterialTheme.colors.onSurface
                        )

                    }

                    Spacer(modifier = Modifier.size(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(end = 8.dp)
                            .background(Color.Gray)
                    )

                    Row(modifier = Modifier.padding(4.dp)) {
                        Text(
                            text = stringResource(id = R.string.scrambled_score),
                            fontFamily = jozoor_font,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colors.onSurface
                        )
                        Text(
                            text = stringResource(
                                id = R.string.number_convert,
                                uiState.finalResult.score
                            ),
                            modifier = Modifier.padding(12.dp, 0.dp),
                            fontFamily = jozoor_font,
                            color = MaterialTheme.colors.onSurface
                        )

                    }
                    Spacer(modifier = Modifier.size(8.dp))


                    if (uiState.doubleScoreShown) {

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {


                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {


                                Row(modifier = Modifier.wrapContentSize()) {

                                    Image(
                                        painter = painterResource(id = R.drawable.ic__13_earning),
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp)
                                    )


                                    Text(
                                        text = stringResource(
                                            id = R.string.number_convert,
                                            +uiState.finalResult.bonusMoney
                                        ),
                                        fontFamily = jozoor_font,
                                        color = TROPHY_COLOR,
                                        fontSize = 48.sp
                                    )

                                }
                            }

                        }

                    }

                    if (!isOnline(context = context))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            IconButton(onClick = {
                                isInfoShown.value = true
                            }, modifier = Modifier.size(16.dp)) {

                                Icon(Icons.Filled.Info, contentDescription = null)
                            }

                            Text(
                                text = stringResource(id = R.string.connect_to_internet_to_share_result),
                                style = MaterialTheme.typography.caption
                            )


                        }


                    Spacer(modifier = Modifier.size(16.dp))

                    Row {

                        Button(
                            onClick = {

                                onClickButton.onRedoTest()
                            }, shape = CircleShape, modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.ic_go_back_arrow),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(4.dp)

                            )
                        }

                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = {
                                onClickButton.onNextTest()

                            }, shape = CircleShape, modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.ic_baseline_play_arrow_24),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.clip(CircleShape)
                            )
                        }

                    }


                    Spacer(modifier = Modifier.height(8.dp))

                    if (isOnline(context = context) && !uiState.doubleScoreShown && uiState.finalResult.passed)
                        Button(
                            onClick = {

                                onClickButton.onDoubleScore()

                            }, shape = CircleShape, modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Text(
                                    text = "+${uiState.finalResult.bonusMoney}",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontFamily = jozoor_font
                                )

                                Image(
                                    painter = painterResource(id = R.drawable.dollar_filled_strocked),
                                    contentDescription = null
                                )
                            }

                        }


                }



                if (uiState.finalResult.passed) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .wrapContentSize()
                    ) {

                        IconButton(
                            onClick = {
                                onClickButton.onShareTestResult()
                            }, modifier = Modifier
                                .padding(top = 8.dp)
                                .size(40.dp)
                        ) {

                            Icon(Icons.Filled.Share, contentDescription = null)
                        }
                        Spacer(
                            modifier = Modifier
                                .size(8.dp)
                        )
                    }

                }
            }

        }
    }
}

@ExperimentalAnimationApi
@Composable
fun Footer(
    uiState: PuzzleUIState,
    showNumberHint: () -> Unit,
    showPreviewHint: () -> Unit,
    showAd: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
    ) {



        AnimatedVisibility(
            visible = uiState.previewHintShown,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .defaultMinSize()

            ) {


                Box(
                    modifier = Modifier
                        .size(160.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = MaterialTheme.colors.surface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {


                        Image(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colors.surface),
                            painter = painterFile(
                                uri = uiState.previewHintImageFromFile,
                                id = uiState.previewHintImageResourceId
                            ),
                            contentDescription = null
                        )

                        Text(
                            text = "${uiState.previewTimer}",
                            modifier = Modifier
                                .wrapContentSize()
                                .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
                                .background(TransparentBlackLight)
                                .padding(8.dp),
                            fontSize = 12.sp,
                            color = Color.White
                        )

                    }


                }
            }

            Spacer(modifier = Modifier.height(56.dp))

        }
        Column(Modifier.wrapContentSize()) {

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {


                Button(
                    onClick = showNumberHint, modifier = Modifier
                        .width(104.dp)
                        .height(40.dp)
                        .padding(horizontal = 8.dp)

                        .clip(RoundedCornerShape(24.dp)),
                    enabled = uiState.helperButtonState.numberHintButt
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic__03_numbers),
                            contentDescription = null
                        )
                        Text(
                            text = " ${uiState.helperButtonState.numberHintCount}",
                            fontSize = 24.sp
                        )
                    }


                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        showPreviewHint()
                    }, modifier = Modifier
                        .width(104.dp)
                        .height(40.dp)
                        .padding(horizontal = 8.dp)

                        .clip(RoundedCornerShape(24.dp)),
                    enabled = uiState.helperButtonState.imageHintButt
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            painterResource(id = R.drawable.ic_baseline_api_24),
                            contentDescription = null
                        )

                        Text(
                            text = " ${uiState.helperButtonState.imageHintCount}",
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        showAd()
                    }, modifier = Modifier
                        .width(104.dp)
                        .height(40.dp)
                        .padding(horizontal = 8.dp)

                        .clip(RoundedCornerShape(24.dp)),
                    enabled = uiState.helperButtonState.imageHintButt
                ) {

                    Icon(
                        painterResource(id = R.drawable.ic__10_video_player_1),
                        contentDescription = null
                    )


                }


            }

            Spacer(modifier = Modifier.weight(1f))


            SmartBannerAd()

        }
    }

}

@Composable
fun SmartBannerAd() {


    AdBanner(adBannerLayout = R.layout.scrambled_banner_ad)
}

@Composable
fun Header(uiState: PuzzleUIState, onVolumeChanged: () -> Unit) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colors.primaryVariant)
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .height(IntrinsicSize.Max)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {

                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .wrapContentHeight()
                        .width(104.dp)
                        .background(MaterialTheme.colors.primary)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    Text(
                        text = stringResource(id = R.string.time_label),
                        color = MaterialTheme.colors.onPrimary
                    )

                    Text(
                        text = "${uiState.timer}",
                        color = MaterialTheme.colors.onPrimary,
                        fontSize = 24.sp
                    )

                    Text(
                        text = stringResource(id = R.string.min_time, uiState.timeToSolve),
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.caption
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {

                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Image(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {

                                onVolumeChanged()

                            },
                        painter = painterResource(
                            id = if (uiState.soundOn) {
                                R.drawable.ic_baseline_volume_up_24
                            } else {
                                R.drawable.ic_baseline_volume_off_24
                            }
                        ),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colors.primary)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "${uiState.level}",
                            color = MaterialTheme.colors.onPrimary,
                            fontSize = 24.sp
                        )

                        Text(
                            text = stringResource(id = R.string.scrambled_level_label),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onPrimary
                        )

                    }

                }

            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {

                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .wrapContentHeight()
                        .width(104.dp)
                        .background(MaterialTheme.colors.primary)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally

                ) {
                    Text(
                        text = stringResource(id = R.string.moves_label),
                        color = MaterialTheme.colors.onPrimary
                    )

                    Text(
                        text = "${uiState.moves}",
                        color = MaterialTheme.colors.onPrimary,
                        fontSize = 24.sp
                    )


                    Text(
                        text = stringResource(id = R.string.minimum_moves, uiState.minimumMoves),
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }

    }

}


@ExperimentalAnimationApi
@Composable
fun PuzzleWidget(uiState: PuzzleUIState, onMoved: (tileIndex: Int) -> Unit) {

    Box(
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
    ) {


        PuzzleBoard(uiState.puzzleDimen) {

            uiState.tileList.forEachIndexed { _, puzzleTile ->

                key(puzzleTile.hashCode()) {

                    println("puzzle:${puzzleTile}\n")
                    PuzzleTile1(
                        direction = puzzleTile.direction,
                        resourceId = puzzleTile.resourceId,
                        uri = puzzleTile.uri,
                        currentPos = puzzleTile.currentPos,
                        modifier = Modifier,
                        hintShown = puzzleTile.hintShown,
                        order = puzzleTile.order,
                        isSpace = puzzleTile.isBlank,
                        onMoved = { tileIndex ->

                            onMoved(tileIndex)

                        }
                    )
                }

            }

        }

    }
}
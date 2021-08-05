package com.puzzlemind.brainsqueezer.scambled

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.mcq.LevelStarBar
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import com.puzzlemind.brainsqueezer.ui.theme.*
import com.puzzlemind.brainsqueezer.utils.Result
import com.puzzlemind.brainsqueezer.utils.painterFile
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim

class ScrambledLevelActivity : ComponentActivity() {

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel by viewModels<ScrambledLevelViewModel> {
            ScrambledLevelViewModelFactory(
                (application as PuzzleApp).scrambledRepository,
                application = application
            )
        }

        viewModel.setDifficulty(intent.getIntExtra(Constants.DIFFICULTY_KEY, 1))

        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    LevelScreen(
                        viewModel = viewModel,
                        onLevelClick = { level ->
                            val intent = Intent(this, SlidingPuzzleActivity::class.java)
                            intent.putExtra(Constants.LEVEL_KEY, level)
                            intent.putExtra(Constants.DIFFICULTY_KEY, viewModel.difficulty.index)
                            startActivity(intent)

                        },
                        onLoadMoreClick = { level -> viewModel.onLoadMore(level) },
                        onDeleteLevel = { level -> viewModel.onDeleteLevel(level) }
                    )

                }
            }
        }
    }
}


@ExperimentalMaterialApi
@Composable
fun LevelScreen(
    viewModel: ScrambledLevelViewModel,
    onLevelClick: (Int) -> Unit,
    onLoadMoreClick: (Int) -> Unit,
    onDeleteLevel: (ScrambledLevel) -> Unit
) {

    val uiState = viewModel.scrambledLevelUiState.collectAsState().value

    Box(modifier = Modifier.fillMaxSize()
        ) {


        LazyColumn(modifier = Modifier.fillMaxSize()
            , state = rememberLazyListState()) {
            item("topSpacing") { Spacer(modifier = Modifier.height(32.dp)) }

            items(uiState.levelsList.size) { level ->

                key(uiState.levelsList[level]) {

                    LevelCard(
                        uiState.levelsList[level],
                        onClick = onLevelClick,
                        onDeleteLevel = onDeleteLevel
                    )
                }
            }

            item("bottomSpacing") { Spacer(modifier = Modifier.height(16.dp)) }

            item(uiState.levelsList) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {


                    when (uiState.resultFeedback) {
                         is Result.Success -> {

                             if (!(uiState.resultFeedback as Result.Success).isEmpty) {
                                 LoadMoreButton(
                                     uiState = uiState,
                                     onLoadMoreClick = { levelId -> onLoadMoreClick(levelId) })
                             }else{
                                 ShowText((uiState.resultFeedback as Result.Success).resId)
                             }
                         }

                        Result.Idle -> {
                                LoadMoreButton(uiState = uiState,onLoadMoreClick = {levelId ->  onLoadMoreClick(levelId) })

                        }
                        Result.Loading -> CircularProgressIndicator()

                        else -> ShowText(resId = (uiState.resultFeedback as Result.Failure).resId)
                    }

                    }


            }

            item("bottomPadding") {
                Spacer(modifier = Modifier.height(56.dp))
            }

        }

    }

}

@Composable
fun ShowText(resId: Int) {

    Text(text = stringResource(id = resId))
}

@Composable
fun LoadMoreButton(uiState: ScrambledLevelUiState,onLoadMoreClick:(Int) -> Unit) {
    Button(
        onClick = { onLoadMoreClick(uiState.levelsList.last().level) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.load_more_butt_label),
            style = MaterialTheme.typography.h6
        )
    }
}


@ExperimentalMaterialApi
@Composable
fun LevelCard(
    scrambledLevel: ScrambledLevel,
    onClick: (Int) -> Unit,
    onDeleteLevel: (ScrambledLevel) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {

        val context = LocalContext.current

        val painter = painterFile(
            uri = scrambledLevel.preview,
            id = if (scrambledLevel.isResource) {
                context.resources.getIdentifier(
                    scrambledLevel.preview.split(".")[0],
                    "drawable", context.packageName
                )

            } else {

                0

            }
        )

        Surface(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
            elevation = 16.dp,
            onClick = {

                onClick(scrambledLevel.level)

            }
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = Color.Gray)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colors.secondary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {


                        Image(
                            painter = if (scrambledLevel.isPassed) {
                                painter
                            } else {
                                painterResource(id = R.drawable.ic__07_gallery)
                            },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }


                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .wrapContentHeight()
                    ) {

                        Row {
                            Text(
                                text = stringResource(
                                    id = R.string.scrambled_level,
                                    scrambledLevel.level
                                ),
                                color = MaterialTheme.colors.primary,
                                fontSize = 20.sp,
                                modifier = Modifier
                            )

                            val isOpen = remember { mutableStateOf(false) } // initial value

                            Spacer(modifier = Modifier.width(4.dp))


                            if (!scrambledLevel.isResource)
                                Icon(
                                    modifier = Modifier.clickable {
                                        isOpen.value = true
                                    },
                                    painter = painterResource(id = R.drawable.ic_baseline_more_vert_24),
                                    contentDescription = null
                                )



                            DropdownMenu(modifier = Modifier
                                .wrapContentWidth()
                                .background(MaterialTheme.colors.surface),
                                properties = PopupProperties(),
                                expanded = isOpen.value,
                                onDismissRequest = {
                                    isOpen.value = false
                                }
                            ) {

                                DropdownMenuItem(onClick = {
                                    onDeleteLevel(scrambledLevel)
                                    isOpen.value = false
                                }) {

                                    Text(
                                        modifier = Modifier,
                                        text = stringResource(id = R.string.delete_label)
                                    )
                                }
                            }

                        }


                        Text(text = scrambledLevel.name)


                        Row(
                            modifier = Modifier.wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {


                            Image(
                                painter = painterResource(id = R.drawable.ic_baseline_photo_size_select_small_24),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "${scrambledLevel.puzzleSize} * ${scrambledLevel.puzzleSize}",
                                color = Color.Gray,
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Image(
                                painter = painterResource(id = R.drawable.ic_baseline_timer_24),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )

                            Text(
                                text = "${scrambledLevel.timeToSolve}s",
                                color = Color.Gray,
                            )

                        }


                    }

                    Column(
                        modifier = Modifier.wrapContentSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {


                        Image(
                            painter = painterResource(
                                id = if (scrambledLevel.trophy) {
                                    R.drawable.trophy_filled
                                } else {
                                    R.drawable.trophy_empty
                                }
                            ),
                            contentDescription = "",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp, color = if (scrambledLevel.trophy) {
                                        TROPHY_COLOR
                                    } else {
                                        Color.Gray
                                    }, shape = CircleShape
                                )
                                .padding(8.dp)
                        )


                        Spacer(modifier = Modifier.height(8.dp))
                        LevelStarBar(
                            numberOfStars = scrambledLevel.stars,
                            modifier = Modifier
                                .width(64.dp)
                        )
                    }


                }


            }
        }

    }
}

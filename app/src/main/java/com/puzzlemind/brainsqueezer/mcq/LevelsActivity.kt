package com.puzzlemind.brainsqueezer.mcq

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.puzzlemind.brainsqueezer.*
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.DifficultyConverter
import com.puzzlemind.brainsqueezer.data.Level
import com.puzzlemind.brainsqueezer.ui.theme.*
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim

class LevelsActivity : ComponentActivity() {

    val TAG = "LevelsActivity"
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diffLevel = intent.getIntExtra(Constants.DIFFICULTY_KEY, 1)
        Log.d(TAG, "onCreate: theme Index: ${diffLevel}")
        setContent {
            MCQLevelsTheme(colorsThemeIndex = diffLevel) {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val levelsViewModel =
                        viewModel<LevelViewModel>(
                            factory = LevelViewModelFactory(
                                (application as PuzzleApp).repository,
                                diffLevel = diffLevel
                            )
                        )

                    val stateUi = levelsViewModel.uiState.collectAsState()

                    LevelScaffold(stateUi)


                }
            }
        }
    }
}


@ExperimentalMaterialApi
@Composable
fun LevelScaffold(stateUi: State<LevelTreeMapState>) {


    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalGradientScrim(
                color = MaterialTheme.colors.secondary,
                startYPercentage = 1f,
                endYPercentage = 0f
            )
            .wrapContentSize()
            .verticalScroll(scrollState)
    ) {


        BoxWithConstraints(modifier = Modifier.wrapContentSize()) {

            val divider = if(this.maxWidth> 600.dp){5}else{3}
            LevelMap(
                modifier = Modifier
                    .fillMaxSize(), divider = divider
            ) {
                for (level in stateUi.value.levels) {

                    LevelItem(level)


                }
            }

        }


    }


}

@ExperimentalMaterialApi
@Composable
fun LevelItem(level: Level) {

    val context = LocalContext.current
    Card(
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, Color.White, shape = RoundedCornerShape(4.dp))
          ,
        onClick = {

            if (level.isOpen) {
                val intent = Intent(context, McqActivity::class.java)
                intent.putExtra(Constants.LEVEL_KEY, level.level)
                intent.putExtra(
                    Constants.THEME_COLOR_INDEX,
                    DifficultyConverter.fromEnumToInt(level.diffIndex)
                )

                context.startActivity(intent)
            }
        },
        backgroundColor = if (level.isPassed) {
            MaterialTheme.colors.primary
        } else {

            MaterialTheme.colors.primaryVariant
        }
    ) {

        Box {


            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(id = R.string.number_convert, level.level ),
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Default,
                    color = if (level.isPassed) {
                        MaterialTheme.colors.onPrimary
                    } else {
                        MaterialTheme.colors.onSecondary
                    },
                    modifier = Modifier.padding(top = 16.dp),
                    fontWeight = FontWeight.Bold
                )

                LevelStarBar(level.stars, modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            if (level.isPassed) {
                                MaterialTheme.colors.secondary
                            } else {
                                MaterialTheme.colors.secondaryVariant
                            }
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Image(
                        painter = painterResource(
                            id = if (level.score.toInt() == 0) {
                                R.drawable.ic__empty_dollar
                            } else {
                                R.drawable.dollar_filled_strocked
                            }
                        ),
                        contentDescription = "",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(vertical = 6.dp)
                    )

                    if (level.score.toInt() != 0) {

                        Text(
                            text = stringResource(id = R.string.number_convert, level.score.toInt()),
                            color = MaterialTheme.colors.onPrimary,
                            fontFamily = FontFamily.Default,
                            fontSize = 16.sp,
                            modifier = Modifier)

                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Image(
                        painter = painterResource(
                            id = if (level.trophy) {
                                R.drawable.trophy_filled_strocked
                            } else {
                                R.drawable.trophy_empty
                            }
                        ),
                        contentDescription = "",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(vertical = 6.dp)
                    )


                }

            }
            if (!level.isPassed && level.isOpen)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TransparentBlackLight)
                ) {

                    Image(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp), contentScale = ContentScale.Crop,
                        painter = painterResource(
                            id = R.drawable.ic_baseline_play_arrow_24
                        ),
                        contentDescription = null
                    )

                }
        }


    }

}


@Composable
fun LevelStarBar(numberOfStars: Int, modifier: Modifier) {

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape),
            painter = painterResource(
                id = if (numberOfStars > 0) {
                    R.drawable.ic_baseline_star_rate_24_filled
                } else {
                    R.drawable.ic_baseline_star_rate_24
                }
            ), contentDescription = ""
        )
        Spacer(modifier = Modifier.size(4.dp))
        Image(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape),
            painter = painterResource(
                id = if (numberOfStars > 1) {
                    R.drawable.ic_baseline_star_rate_24_filled
                } else {
                    R.drawable.ic_baseline_star_rate_24
                }
            ),
            contentDescription = ""
        )

        Spacer(modifier = Modifier.size(4.dp))

        Image(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape),
            painter = painterResource(
                id = if (numberOfStars > 2) {
                    R.drawable.ic_baseline_star_rate_24_filled
                } else {
                    R.drawable.ic_baseline_star_rate_24
                }
            ),
            contentDescription = ""
        )
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview5() {
    BrainSqueezerTheme {
//        LevelScaffold(mutableStateOf(LevelTreeMapState()))
    }
}
package com.puzzlemind.brainsqueezer.scambled

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.Difficulty
import com.puzzlemind.brainsqueezer.mcq.FirstProgress
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledDashboard
import com.puzzlemind.brainsqueezer.ui.theme.*
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim

class ScrambledActivity : ComponentActivity() {

    val TAG = "ScrambledActivity"
    val viewModel: ScrambledViewModel by viewModels<ScrambledViewModel> {
        ScrambledViewModelFactory((application as PuzzleApp).scrambledRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {


                    val uiState = viewModel.uiState.collectAsState()

                    Log.d(TAG, "onCreate: ${uiState.value.dashboard.points}")
                    ScrambledScreen(uiState.value)


                }
            }
        }
    }
}

@Composable
fun ScrambledScreen(uiState: ScrambledDashboardUiState) {


    Box(modifier = Modifier.fillMaxSize()
    ) {

        val navController = rememberNavController()

        Column(modifier = Modifier.fillMaxSize()) {

            ScrambledHeader(onHeaderItemClick = { destination ->

                navController.navigate(destination.route) {
                    popUpTo(ScrambledDestination.Home.route)
                }

            })

            NavHost(
                navController = navController,
                startDestination = ScrambledDestination.Home.route
            ) {

                composable(ScrambledDestination.Home.route) {

                    ScrambledHome()
                }

                composable(ScrambledDestination.Live.route) {

                    ScrambledLive()
                }

                composable(ScrambledDestination.Tournament.route) {

                    ScrambledTournament()
                }
                composable(ScrambledDestination.Search.route) {

                    ScrambledMarket()
                }
            }


        }
    }
}

@Composable
fun ScrambledMarket() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(text = "Market")

    }

}

@Composable
fun ScrambledTournament() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(text = "Tournament")

    }
}

@Composable
fun ScrambledLive() {


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Text(text = "Live")

    }
}

@Composable
fun ScrambledHome() {
    val uiState = viewModel<ScrambledViewModel>(
        factory = ScrambledViewModelFactory((LocalContext.current.applicationContext as PuzzleApp).scrambledRepository)
    ).uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        ScrambledDashboard(uiState.value.dashboard)

        ScrambledDifficulties(uiState.value)

    }

}

@Composable
fun ScrambledHeader(onHeaderItemClick: (ScrambledDestination) -> Unit) {


    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.primary)
                .height(56.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = stringResource(id = R.string.scramble_game_label),
                fontSize = 24.sp,
                color = MaterialTheme.colors.onPrimary
            )

        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()

                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(40.dp)
                    .clickable {

                        onHeaderItemClick(ScrambledDestination.Live)
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = stringResource(id = R.string.scrambled_live_butt_label),
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(40.dp)
                    .clickable {

                        onHeaderItemClick(ScrambledDestination.Tournament)
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colors.primary)
                    .padding(horizontal = 8.dp), contentAlignment = Alignment.Center
            ) {

                Text(
                    text = stringResource(id = R.string.scrambled_tournament_butt_label),
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(40.dp)
                    .clickable {

                        onHeaderItemClick(ScrambledDestination.Search)
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = stringResource(id = R.string.scrambled_search_butt_label),
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

}

@Composable
fun ScrambledDashboard(dashboard: ScrambledDashboard) {


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = 24.dp
    ) {


        Box {

            Column(
                Modifier
                    .wrapContentHeight()
                    .background(color = MaterialTheme.colors.secondary)
            ) {


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.White)
                        .padding(16.dp)
                ) {

                    FirstProgress(
                        modifier = Modifier.weight(1f),
                        drawableRes = R.drawable.ic_value_chain,
                        labelResId = R.string.mcq_progress_label,
                        progress = (dashboard.easySkillful + dashboard.mediumSkillful + dashboard.hardSkillful) / 300f,
                        count = dashboard.matchWon
                    )

                    FirstProgress(
                        modifier = Modifier.weight(1f),
                        drawableRes = R.drawable.ic__26_star,
                        labelResId = R.string.stars_collected_label,
                        progress = dashboard.stars / (300 * 3).toFloat(),
                        count = dashboard.stars
                    )

                    FirstProgress(
                        modifier = Modifier.weight(1f),
                        drawableRes = R.drawable.ic__25_trophy,
                        labelResId = R.string.trophies_collected_label,
                        progress = dashboard.trophies/300f,
                        count = dashboard.trophies
                    )

                }

                Divider()

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.padding(horizontal = 24.dp)) {


                    Text(text = stringResource(id = R.string.scrambled_points_label),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(text = stringResource(id = R.string.number_convert, dashboard.points),
                        color = Color.White
                    )

                }

                Spacer(modifier = Modifier.height(8.dp))

            }
        }

    }

}

sealed class ScrambledDifficulty(
    @StringRes val resId: Int,
    val difficulty: Difficulty,
    val index: Int
) {
    object Easy : ScrambledDifficulty(
        index = 1,
        resId = R.string.easy_difficuty,
        difficulty = Difficulty.EASY
    )

    object Medium : ScrambledDifficulty(
        index = 2,
        resId = R.string.normal_difficuty,
        difficulty = Difficulty.NORMAL
    )

    object Hard : ScrambledDifficulty(
        index = 3,
        resId = R.string.hard_difficulty,
        difficulty = Difficulty.HARD
    )

    companion object {
        fun getDiffFromIndex(index: Int): ScrambledDifficulty {
            return when (index) {
                1 -> Easy
                2 -> Medium
                else -> Hard
            }
        }

        fun getDiffStringFrom(difficulty: ScrambledDifficulty): String {
            return when (difficulty) {
                is Easy -> "easy"
                is Medium -> "medium"
                is Hard -> "hard"
            }
        }
    }
}

@Composable
fun ScrambledDifficulties(uiState: ScrambledDashboardUiState) {

    val difficulties = mutableListOf<ScrambledDifficulty>(
        ScrambledDifficulty.Easy,
        ScrambledDifficulty.Medium,
        ScrambledDifficulty.Hard
    )
    for (difficulty in difficulties) {

        DifficultyCard(
            difficulty = difficulty, progress = when (difficulty) {
                ScrambledDifficulty.Easy -> uiState.dashboard.easySkillful / 100f
                ScrambledDifficulty.Medium -> uiState.dashboard.mediumSkillful / 100f
                else -> uiState.dashboard.hardSkillful / 100f

            }
        )
    }
}

@Composable
fun DifficultyCard(difficulty: ScrambledDifficulty, progress: Float) {

    val context = LocalContext.current
    Box(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Surface(
            elevation = 16.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .wrapContentHeight()

        ) {

            Box(modifier = Modifier
                .fillMaxSize()
                .clickable {

                    println("$difficulty clicked")

                    val intent = Intent(context, ScrambledLevelActivity::class.java)
                    intent.putExtra(Constants.DIFFICULTY_KEY, difficulty.index)
                    context.startActivity(intent)

                }

                .padding(16.dp),
                contentAlignment = Alignment.Center) {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    Text(
                        text = stringResource(id = difficulty.resId),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(184.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colors.primary,
                        backgroundColor = Color.Gray,
                        progress = progress
                    )
                }

            }

        }
    }

}

sealed class ScrambledDestination(
    val route: String,
) {

    object Home : ScrambledDestination(Constants.SCRAMBLED_HOME)
    object Live :
        ScrambledDestination(Constants.SCRAMBLED_LIVE)              //where user can find live match

    object Tournament : ScrambledDestination(Constants.SCRAMBLED_TOURNAMENT)
    object Search : ScrambledDestination(Constants.SCRAMBLED_SEARCH)
}
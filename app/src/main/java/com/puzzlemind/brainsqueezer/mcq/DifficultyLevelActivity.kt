package com.puzzlemind.brainsqueezer.mcq

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.DifficultyLevel
import com.puzzlemind.brainsqueezer.data.Game
import com.puzzlemind.brainsqueezer.ui.theme.*
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim

class DifficultyLevelActivity : ComponentActivity() {
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val viewModel = viewModel<DifficultyViewModel>(
                        factory = DifficultyViewModelFactory(
                            (application as PuzzleApp).repository,
                            game = Game.MCQ
                        )
                    )

                    val uiState = viewModel.uiState.collectAsState()
                    DifficultyScaffold(uiState.value)

                }
            }
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun DifficultyItem(difficultyLevel: DifficultyLevel) {

    val bgColorList = remember {
        mutableListOf(
            MidnightGreenEagleGree, BluePrimaryColor,
            CaribbeanGreen, AmberPrimaryColor, MiddleRed,
            OrangePrimaryColor, amaranth_red
        )
    }
    val context = LocalContext.current



    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp)
        .height(120.dp),
        backgroundColor = bgColorList[difficultyLevel.index - 1],
        onClick = {
            if (difficultyLevel.isOpen) {
                val intent = Intent(context, LevelsActivity::class.java)
                intent.putExtra(Constants.DIFFICULTY_KEY, difficultyLevel.index)
                context.startActivity(intent)
            }
        }

    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Text(
                text = stringResource(id = R.string.levels_num, difficultyLevel.levels),
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colors.onPrimary,
                fontSize = 18.sp
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = difficultyLevel.name,
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.onPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .width(120.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    backgroundColor = TransparentBlack,
                    color = MaterialTheme.colors.onPrimary, progress = difficultyLevel.progress
                )

            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .wrapContentSize(), verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = stringResource(
                        id = R.string.diff_trophies,
                        difficultyLevel.trophies,
                        difficultyLevel.levels
                    ), color = MaterialTheme.colors.onPrimary,
                    fontSize = 18.sp
                )
                Image(
                    painter = painterResource(id = R.drawable.trophy_filled),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                )
            }


            if (!difficultyLevel.isOpen)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(TransparentBlack)

                ) {
                    Image(
                        painterResource(id = R.drawable.ic_lock_3),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
        }

    }


}

@ExperimentalMaterialApi
@Composable
fun DifficultyScaffold(uiState: DifficultyScreenState) {

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()

    ) {


        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .verticalGradientScrim(
                    color = MaterialTheme.colors.primary,
                    startYPercentage = 1f,
                    endYPercentage = 0f
                )
                .verticalScroll(scrollState)) {


            MCQDashboardUI(uiState)

            Spacer(modifier = Modifier.size(24.dp))

            for (item in uiState.diffs) {

                DifficultyItem(difficultyLevel = item)
                Spacer(modifier = Modifier.height(16.dp))

            }
            Spacer(modifier = Modifier.size(32.dp))

        }
    }

}

@Composable
fun MCQDashboardUI(uiState: DifficultyScreenState) {

    Surface(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(24.dp)
        .clip(RoundedCornerShape(16.dp)),
        elevation = 24.dp
        ) {

        Column(
            Modifier
                .wrapContentHeight()
                .background(color = MaterialTheme.colors.secondary)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth().background(color = Color.White)
                    .padding(16.dp)
            ) {

                FirstProgress(
                    modifier = Modifier.weight(1f),
                    drawableRes = R.drawable.ic_value_chain,
                    labelResId = R.string.mcq_progress_label,
                    progress = uiState.dashboard.progress,
                    count = uiState.dashboard.maxLevel
                )

                FirstProgress(
                    modifier = Modifier.weight(1f),
                    drawableRes = R.drawable.ic__26_star,
                    labelResId = R.string.stars_collected_label,
                    progress = uiState.dashboard.stars / (uiState.dashboard.levels * 3).toFloat(),
                    count = uiState.dashboard.stars
                )

                FirstProgress(
                    modifier = Modifier.weight(1f),
                    drawableRes = R.drawable.ic__25_trophy,
                    labelResId = R.string.trophies_collected_label,
                    progress = uiState.dashboard.trophies / uiState.dashboard.levels.toFloat(),
                    count = uiState.dashboard.trophies
                )

            }


            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.padding(horizontal = 24.dp)) {


                Text(text = stringResource(id = R.string.scrambled_points_label),
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(text = stringResource(id = R.string.number_convert, uiState.dashboard.points),
                    color = Color.White
                )

            }

            Spacer(modifier = Modifier.height(8.dp))

        }

    }
}

@Composable
fun FirstProgress(modifier: Modifier, @DrawableRes drawableRes: Int, @StringRes labelResId:Int,progress:Float,count:Int) {
    Column(modifier = modifier,horizontalAlignment = Alignment.CenterHorizontally) {

        Box (modifier = Modifier
            .defaultMinSize()
            .aspectRatio(1f)
            .padding(4.dp),
            contentAlignment = Alignment.Center
        ){
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)){


                val stroke = Stroke(width = 4.dp.toPx(),cap = StrokeCap.Round)

                drawArc(
                    color = Color.Gray,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = Color.Blue,
                    startAngle = -90f,
                    style = stroke,
                    sweepAngle = progress*360f,
                    useCenter = false,
                )


            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(modifier = Modifier.size(32.dp),painter = painterResource(id = drawableRes),contentDescription = null,
                    contentScale = ContentScale.Inside)

                Text(text = stringResource(id = R.string.number_convert, count))
            }

        }

        Text(text = stringResource(id = labelResId))



    }
}

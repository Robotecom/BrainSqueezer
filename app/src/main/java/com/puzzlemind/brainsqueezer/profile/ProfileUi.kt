package com.puzzlemind.brainsqueezer.profile

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import com.google.accompanist.glide.rememberGlidePainter
import com.puzzlemind.brainsqueezer.ui.theme.TransparentBlack
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.Game
import com.puzzlemind.brainsqueezer.leaderboard.ErrorState
import com.puzzlemind.brainsqueezer.login.LoginActivity
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim

@Composable
fun ProfileScreen(profileState: ProfileState) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalGradientScrim(
                color = MaterialTheme.colors.primary,
                startYPercentage = 1f,
                endYPercentage = 0f
            )
    ) {

        if (!profileState.loading)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.height(56.dp))

                Image(
                    painter = rememberGlidePainter(
                        request = profileState.user.profile,
                        requestBuilder = {
                                         this.error(R.drawable.ic_user)
                            this.fallback(R.drawable.ic_user)
                        },
                        previewPlaceholder = R.drawable.ic_user
                    ),

                    modifier = Modifier
                        .size(116.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colors.onPrimary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profileState.user.name,
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Divider(modifier = Modifier.fillMaxWidth(), startIndent = 24.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(TransparentBlack)
                ) {

                    Column(
                        modifier = Modifier
                            .wrapContentHeight()
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.trophies_label),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Image(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(4.dp),
                            painter = painterResource(id = R.drawable.ic__03_trophy_2),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(text = "${profileState.user.trophies}", color = Color.White)

                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 16.dp)
                            .width(1.dp)
                            .background(Color.Gray)
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = stringResource(id = R.string.stars_label),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Image(
                            modifier = Modifier.size(40.dp),
                            painter = painterResource(id = R.drawable.ic__09_star),
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(text = "${profileState.user.stars}", color = Color.White)

                    }

                }

                GameScoreDetails(profileState = profileState)

            }


        val localModifier = Modifier.align(Alignment.Center)
        if (profileState.loading) {
            CircularProgressIndicator(modifier = localModifier)
        }

        if (profileState.error.isError) {

            ErrorUi(profileState.error, localModifier)

        }

        if (profileState.isOwner){

            val context = LocalContext.current
            ConstraintLayout(modifier = Modifier.fillMaxSize()) {

                val image = createRef()
                Image(painter = painterResource(id = R.drawable.ic_baseline_edit_24),contentDescription = null,
                    modifier = Modifier.constrainAs(image){
                        end.linkTo(parent.end , 56.dp)
                        top.linkTo(parent.top,56.dp)
                    }
                        .clip(CircleShape)
                        .clickable {
                            context.startActivity(Intent(context,LoginActivity::class.java))
                        }
                        .background(MaterialTheme.colors.secondaryVariant)
                        .padding(8.dp)

                )
            }

        }

    }
}

@Composable
fun GameScoreDetails(profileState: ProfileState) {


    Spacer(modifier = Modifier.height(24.dp))
    Row(modifier = Modifier.fillMaxWidth()) {

        Spacer(modifier = Modifier.width(32.dp))
        Text(
            text = stringResource(id = R.string.games_profile_label),
            color = MaterialTheme.colors.onPrimary,
            fontSize = 20.sp
        )

    }

    Spacer(modifier = Modifier.height(16.dp))

    LabelBar()

    Spacer(modifier = Modifier.height(8.dp))


    for (item in profileState.user.maxLevel) {

        GameDetailsCard(profileState, gameLabel = GameType.getTypeFromKey(item.key), gameKey = item.key)

        Spacer(modifier = Modifier.height(16.dp))

    }

}

@Composable
fun GameDetailsCard(profileState: ProfileState, gameLabel: GameType, gameKey: String) {
    Box(modifier = Modifier.padding(horizontal = 32.dp)) {

        Card(modifier = Modifier.fillMaxWidth()) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Image(
                    painter = painterResource(id =gameLabel.resId),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).background(Color.LightGray).padding(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    modifier = Modifier.width(120.dp),
                    text = stringResource(
                        id = gameLabel.nameResId
                    )
                )

                ConstraintLayout(Modifier.weight(1f)) {

                    val (pointsText, maxLevel) = createRefs()
                    Text(
                        modifier = Modifier
                            .constrainAs(pointsText) {
                                start.linkTo(parent.start)
                                end.linkTo(maxLevel.start)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                            }
                            ,
                        textAlign = TextAlign.Center,
                        text = stringResource(
                            id = R.string.number_convert,
                            profileState.user.points
                        )
                    )


                    Text(modifier = Modifier
                        .constrainAs(maxLevel) {
                            end.linkTo(parent.end, margin = 8.dp)
                            top.linkTo(parent.top)
                            start.linkTo(pointsText.end)
                            bottom.linkTo(parent.bottom)

                        }
                        .width(32.dp),
                        textAlign = TextAlign.Center, text = stringResource(
                            id = R.string.number_convert,
                            profileState.user.maxLevel[gameKey] ?: 0
                        ))


                }


            }
        }

    }
}

@Composable
fun LabelBar() {

    Box(modifier = Modifier.padding(horizontal = 32.dp)) {

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colors.primary)
                .padding(vertical = 4.dp)
        ) {


            Spacer(modifier = Modifier.width(72.dp))
            Text(
                modifier = Modifier.width(120.dp),
                text = stringResource(id = R.string.game_label),
                color = Color.White,
                fontSize = 12.sp,

                )

            ConstraintLayout(Modifier.fillMaxWidth()) {

                val (pointsText, maxLevel) = createRefs()


                Text(
                    modifier = Modifier
                        .constrainAs(pointsText) {
                            start.linkTo(parent.start)
                            end.linkTo(maxLevel.start)
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                        }
                        .width(40.dp),
                    text = stringResource(id = R.string.points_label),
                    color = Color.White,
                    fontSize = 12.sp
                )


                Text(modifier = Modifier
                    .constrainAs(maxLevel) {
                        end.linkTo(parent.end, margin = 8.dp)
                        top.linkTo(parent.top)
                        start.linkTo(pointsText.end)
                        bottom.linkTo(parent.bottom)

                    }
                    .width(48.dp),
                    text = stringResource(id = R.string.max_level_label),
                    color = Color.White,
                    fontSize = 12.sp
                )


            }
        }
    }

}

@Composable
fun ErrorUi(error: ErrorState, modifier: Modifier) {

    val hasError = remember{mutableStateOf(error.isError)}
    if (hasError.value)
    Dialog(onDismissRequest = {
        hasError.value = false
    }) {

        Box(
            modifier = modifier

                .background(MaterialTheme.colors.surface, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {


            Column(modifier = modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {

                if (error.hasImage) {
                    Image(
                        painterResource(id = R.drawable.ic_baseline_error_outline_24),
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = stringResource(
                        id = R.string.error_msg,
                        error.errorMsg
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface
                )
            }

        }

    }

}

sealed class GameType(val nameResId:Int,val type: Game,@DrawableRes val resId: Int){
    object QMIND :GameType(R.string.game_1_name,Game.MCQ,R.drawable.ic_mcq_game_icon)
    object Puzzle15 :GameType(R.string.game_2_name,Game.SCRAMBLED,R.drawable.ic_mcq_game_icon)

    companion object{
        fun getTypeFromKey(key:String):GameType{
            return when(key){
                "MCQ" -> QMIND
                "Puzzle15" -> Puzzle15
                else -> Puzzle15
            }
        }
    }
}

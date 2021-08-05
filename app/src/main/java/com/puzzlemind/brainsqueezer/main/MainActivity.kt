package com.puzzlemind.brainsqueezer.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.leaderboard.LeaderBoardScreen
import com.puzzlemind.brainsqueezer.leaderboard.isScrollingUp
import com.puzzlemind.brainsqueezer.login.SplashScreen
import com.puzzlemind.brainsqueezer.login.LoginActivity
import com.puzzlemind.brainsqueezer.login.SplashState
import com.puzzlemind.brainsqueezer.mcq.DifficultyLevelActivity
import com.puzzlemind.brainsqueezer.profile.ProfileActivity
import com.puzzlemind.brainsqueezer.scambled.ScrambledActivity
import com.puzzlemind.brainsqueezer.settings.SettingsActivity
import com.puzzlemind.brainsqueezer.ui.theme.*
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim
import kotlinx.coroutines.InternalCoroutinesApi


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"
    private var currentUser: FirebaseUser? = Firebase.auth.currentUser

    @ExperimentalPagerApi
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    ScreenRoot(onLeaderboardClick = {
                        Log.d(TAG, "onCreate: leader board is clicked")

                    })


                }
            }
        }

        Firebase.auth.addAuthStateListener {
            if (it.currentUser != null) {

                currentUser = it.currentUser
                updateUserMetrics(currentUser)

            }
        }

    }


    override fun onStart() {
        super.onStart()

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    //the purpose of this function is to trigger cloud functions  "onUpdateUserMetrics"
    //and "onCreateUserMetrics" which is used to calculate and update User score on leaderboard
    private fun updateUserMetrics(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            Firebase.firestore.document(
                Constants.USERS + "/" +
                        currentUser.uid + "/" +
                        Constants.CALCULATE_COL + "/" +
                        currentUser.uid
            ).set(
                mutableMapOf(
                    "name" to currentUser.displayName,
                    "uid" to currentUser.uid,
                    "profile" to currentUser.photoUrl.toString(),
                    "timestamp" to FieldValue.serverTimestamp()
                )
            )
        } else {
            Log.d(TAG, "updateUserMetrics: current user is null")
        }
    }

}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun ScreenRoot(onLeaderboardClick: () -> Unit) {

    val transitionState = remember { MutableTransitionState(SplashState.Shown) }
    val transition = updateTransition(transitionState, label = "splashTransition")
    val splashAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 100) }, label = "splashAlpha"
    ) {
        if (it == SplashState.Shown) 1f else 0f
    }
    val contentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 300) }, label = "contentAlpha"
    ) {
        if (it == SplashState.Shown) 0f else 1f
    }
    val contentTopPadding by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }, label = "contentTopPadding"
    ) {
        if (it == SplashState.Shown) 100.dp else 0.dp
    }

    Box {
        SplashScreen(
            modifier = Modifier.alpha(splashAlpha),
            onTimeout = { transitionState.targetState = SplashState.Completed }
        )
        MainContent(
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(top = contentTopPadding), onLeaderboardClick = onLeaderboardClick
        )


    }

}

@ExperimentalMaterialApi
@Composable
fun GameBoard() {

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {

        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .background(Color.White)
                .verticalGradientScrim(
                    color = MaterialTheme.colors.secondaryVariant,
                    startYPercentage = 1f,
                    endYPercentage = 0f
                )
                .padding(32.dp)
                .verticalScroll(scrollState),

            ) {

            val context = LocalContext.current

            Spacer(modifier = Modifier.height(64.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(160.dp)
                    .wrapContentSize()
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        context.startActivity(Intent(context, DifficultyLevelActivity::class.java))
                    }, backgroundColor = StrongBlueDark,
                onClick = {
                    context.startActivity(Intent(context, DifficultyLevelActivity::class.java))

                }

            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primaryVariant)

                        .verticalGradientScrim(
                            color = MaterialTheme.colors.primary,
                            startYPercentage = 1f,
                            endYPercentage = 0f

                        )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Image(
                            painter = painterResource(id = R.drawable.ic_mcq_game_icon),
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Inside,
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(id = R.string.how_savvy_game_label),
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.onPrimary
                        )


                    }
                }

            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(160.dp)
                    .wrapContentSize()
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        context.startActivity(Intent(context, DifficultyLevelActivity::class.java))
                    },
                backgroundColor = StrongBlueDark,
                onClick = {

                    context.startActivity(Intent(context,ScrambledActivity::class.java))

                }
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.secondaryVariant)

                        .verticalGradientScrim(
                            color = MaterialTheme.colors.secondary,
                            startYPercentage = 1f,
                            endYPercentage = 0f

                        )
                        .wrapContentSize(Alignment.Center)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {


                        Image(modifier = Modifier.size(64.dp),
                            painter = painterResource(id = R.drawable.ic_slidingpuzzle3),
                            contentDescription = null)
                        Text(
                            text = stringResource(id = R.string.scramble_game_label),
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.onPrimary
                        )


                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(160.dp)
                    .wrapContentSize()
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
//                        context.startActivity(Intent(context, DifficultyLevelActivity::class.java))
                    },
                backgroundColor = StrongBlueDark
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LimePrimaryDarkColor)
                        .verticalGradientScrim(
                            color = LimePrimaryColor,
                            startYPercentage = 1f,
                            endYPercentage = 0f

                        )
                        .wrapContentSize(Alignment.Center)
                ) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {


                        Text(
                            text = stringResource(id = R.string.hanoi_tower_game_label),
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.onPrimary
                        )

                        Text(
                            text = stringResource(id = R.string.coming_soon),
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onPrimary
                        )

                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

        }

        HomeTopBar(modifier = Modifier.align(Alignment.TopStart))

    }
}

@OptIn(InternalCoroutinesApi::class)
@Composable
fun HomeTopBar(modifier: Modifier) {

    val applicationContext = LocalContext.current.applicationContext
    val homeViewModel = viewModel<MainViewModel>(
        factory = MainViewModelFactory((applicationContext as PuzzleApp).repository)
    )

    val uiState = homeViewModel.uiState.collectAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(TransparentBlack)

    ) {

        Row(
            modifier = Modifier
                .wrapContentHeight()
                .padding(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
            ) {


                val painter = rememberGlidePainter(
                    uiState.value.user.profile,
                    requestBuilder = {
                        this.fallback(R.drawable.ic_user)
                        this.error(R.drawable.ic_user)
                    },
                    previewPlaceholder = R.drawable.ic_user
                )

                val context = LocalContext.current
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {

                            val intent = Intent(context, ProfileActivity::class.java)
                            intent.data = Uri.parse(homeViewModel.firebaseUser?.uid!!)
                            context.startActivity(intent)
                        }
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colors.onPrimary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colors.secondaryVariant),
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

            }


            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {


                Column(Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = stringResource(
                                id = R.string.balance_home_label,
                                uiState.value.user.balance.toInt()
                            ),

                            color = MaterialTheme.colors.onPrimary
                        )


                    }


                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Text(
                            text = stringResource(id = R.string.user_progress_label),
                            color = MaterialTheme.colors.onPrimary
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        LinearProgressIndicator(
                            modifier = Modifier
                                .width(80.dp)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colors.primary,
                            backgroundColor = Color.DarkGray,
                            progress = uiState.value.user.skills
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = stringResource(
                                id = R.string.percentage,
                                uiState.value.user.skills * 100
                            ),
                            color = MaterialTheme.colors.onPrimary
                        )

                    }

                }


                val context = LocalContext.current
                Box(modifier = Modifier.clickable {
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                }) {
                    Image(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(id = R.drawable.ic_baseline_settings_24),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )

                }
            }
        }
    }
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@ExperimentalMaterialApi
@Composable
fun MainContent(modifier: Modifier, onLeaderboardClick: () -> Unit) {

    val navController = rememberNavController()
    val destinations = mutableListOf(
        MainDestination.GamesFragment,
        MainDestination.LeaderBoardFragment
    )
    val lazyListState = rememberLazyListState()
    val bottomState = lazyListState.isScrollingUp()


    Scaffold(modifier = modifier,
        topBar = {

        },
        bottomBar = {

            AnimatedVisibility(
                visible = bottomState,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {

                BottomNavigationCustom(navController, destinations)
            }


        }) {


        NavHost(
            navController = navController,
            startDestination = MainDestination.GamesFragment.route
        ) {

            composable(MainDestination.GamesFragment.route) {
                FirstFragment()
            }
            composable(MainDestination.LeaderBoardFragment.route) {
                SecondFragment(lazyListState)
                onLeaderboardClick()
            }
        }


    }

}

@Composable
fun BottomNavigationCustom(
    navController: NavHostController,
    mainDestinations: MutableList<MainDestination>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    BottomNavigation(elevation = 24.dp, backgroundColor = MaterialTheme.colors.primary) {

        mainDestinations.forEachIndexed { _, destination ->

            BottomNavigationItem(
                selected = currentRoute == destination.route,
                label = { Text(text = stringResource(id = destination.resourceId)) },
                icon = {
                    Icon(
                        painter = painterResource(id = destination.drawableRes),
                        contentDescription = null
                    )
                },
                onClick = {

                    navController.navigate(destination.route) {

                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationRoute!!) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true

                    }

                })

        }
    }
}

@ExperimentalMaterialApi
@Composable
fun FirstFragment() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Cyan)
    ) {

        GameBoard()
    }
}

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun SecondFragment(lazyListState: LazyListState) {


    BrainSqueezerTheme {

        LeaderBoardScreen(Modifier.fillMaxSize(), lazyListState)

    }


}

sealed class MainDestination(
    val route: String,
    @StringRes val resourceId: Int,
    @DrawableRes val drawableRes: Int
) {
    object GamesFragment : MainDestination(
        Constants.GAMES,
        R.string.games,
        R.drawable.ic_baseline_games_24
    )

    object LeaderBoardFragment : MainDestination(
        Constants.LEADERBOARD,
        R.string.leaderboard, R.drawable.ic_baseline_leaderboard_24
    )


}

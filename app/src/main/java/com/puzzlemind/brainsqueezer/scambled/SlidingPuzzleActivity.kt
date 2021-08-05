package com.puzzlemind.brainsqueezer.scambled

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.core.view.*
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.BuildConfig
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.scambled.ui.theme.BrainSqueezerTheme
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.mcq.*
import com.puzzlemind.brainsqueezer.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

class SlidingPuzzleActivity : ComponentActivity() {
    private var firebaseUser: FirebaseUser? = null
    val puzzleViewModel by viewModels<SlidingPuzzleViewModel>
    {
        PuzzleViewModelFactory(
            repository = (application as PuzzleApp).scrambledRepository,
            application = application
        )
    }
    var level = 1
    var difficulty = 1

    lateinit var adManager: AdManager

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseUser = Firebase.auth.currentUser
//        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
//
//            Log.d(TAG, "onCreate: windowInset Listener")
////            v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
//            lifecycleScope.launch {
//                delay(1500)
//                hideSystemUI()
//            }
//            insets
//        }

        if (intent.data == null) {

            level = intent.getIntExtra(Constants.LEVEL_KEY, 1)
            difficulty = intent.getIntExtra(Constants.DIFFICULTY_KEY, 1)
            if (savedInstanceState == null)
                puzzleViewModel.setLevel(level, difficulty)

        }else{
            puzzleViewModel.loadingLevel()
            extractLink()

        }



        setContent {

            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {


                    val uiState = puzzleViewModel.uiState.collectAsState()

                    PuzzleScreen(
                        uiState = uiState.value, eventCallBack = clickEventCombiner(
                            onMoved = { tileIndex -> puzzleViewModel.onMoved(tileIndex) },
                            onRestartPuzzle = puzzleViewModel::reStart,
                            onShowPreview = puzzleViewModel::showPreviewHint,
                            onNumberHintShow = puzzleViewModel::showNumberHint,
                            onStartPuzzle = puzzleViewModel::onStartPuzzle,
                            onShowAd = puzzleViewModel::onShowAd,
                            finalResultClickable = finalClicked(
                                onRedoTest = puzzleViewModel::reStart,
                                onNextTest = { finish() },
                                onShareTestResult = {
                                    shareResult(puzzleViewModel.uiState.value.finalResult.previewUrl,level,difficulty)
                                },
                                onDoubleScore = puzzleViewModel::onDoubleScore
                            ),
                            onDismissError = puzzleViewModel::onDismissRewardError,
                            onBackPressed = puzzleViewModel::onBackPressed,
                            onSoundOnClick = puzzleViewModel::onSoundOn
                        )
                    )
                }
            }
        }



        Handler(Looper.getMainLooper()).post {

           prepareVideoAds()

        }
    }

    private fun extractLink() {

        if(isOnline(this)) {
            Firebase.dynamicLinks.getDynamicLink(intent)
                .addOnSuccessListener { pendingDynamicLinkData ->

                    val deepLink: Uri?
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.link

                        Log.d(TAG, "onCreate: deepLink:${deepLink}")
                        level = (deepLink?.getQueryParameter("level") ?: "1").toInt()
                        difficulty = (deepLink?.getQueryParameter(LinkSchema.SCRAMBLED_DIFFICULTY)?:"1").toInt()

                        val query = Firebase.firestore.collectionGroup("puzzles").whereEqualTo("level",level)
                            .whereEqualTo("difficulty",difficulty)

                        lifecycleScope.launch {

                            withContext(Dispatchers.IO) {
                                val scrambledLevel =
                                    (application as PuzzleApp).scrambledRepository.getLevel(
                                        level = level,
                                        difficulty = difficulty
                                    )

                                if (scrambledLevel != null) {
                                    Log.d(TAG, "extractLink: level already exists, so just go back:${scrambledLevel}")
                                    puzzleViewModel.setLevel(level = level, difficulty = difficulty)
                                    puzzleViewModel.levelReady()
                                    return@withContext
                                }
                                SlidingPuzzleFetcher(repository = (application as PuzzleApp).scrambledRepository,
                                    context = applicationContext,
                                    scope = lifecycleScope,
                                    callback = object :
                                        SlidingPuzzleFetcher.SlidingPuzzleFetcherCallback {
                                        override fun finishLoading(resultFeedback: Result) {

                                            puzzleViewModel.setLevel(
                                                level = level,
                                                difficulty = difficulty
                                            )
                                            puzzleViewModel.levelReady()

                                        }

                                    },
                                ).fetchMoreAndSave(query)
                            }
                        }

                        Log.d(TAG, "onCreate: level in url:${level}")
                        sendRewardToReferrer(deepLink)

                    }else{
                        puzzleViewModel.setLevel(level = level,difficulty = difficulty)
                        puzzleViewModel.levelReady()

                    }

                }.addOnFailureListener{
                    Log.d(TAG, "onCreate: failed to get link from dynamic link:${it.message}")

                    puzzleViewModel.setLevel(level = level,difficulty = difficulty)
                }
        }else{
            Toast.makeText(this,R.string.this_operation_need_internet,Toast.LENGTH_LONG).show()

        }

    }

    private fun sendRewardToReferrer(deepLink: Uri?) {


    }

    private fun prepareVideoAds() {
        val rewardUnitId = if (BuildConfig.DEBUG) {
            getString(R.string.reward_vid_testing_id)
        } else {
            getString(R.string.scrambled_reward_ad_unit_id)
        }
        adManager = AdManager(
            context = this,
            adUnitId = rewardUnitId,
            rewardedAdCallback = rewardCallback(
                onRewardEarned = puzzleViewModel::onRewardEarned,
                onError = { error -> puzzleViewModel.onRewardAdError(error) },
                onDismiss = puzzleViewModel::onDismissRewardAd,
                onAdShowing = puzzleViewModel::onAdShowing
            )
        )

        adManager.loadRewardedAd()

        lifecycleScope.launch {
            puzzleViewModel.adState.collect { value: SlidingPuzzleViewModel.ActionDelegator ->

                when (value) {
                    SlidingPuzzleViewModel.ActionDelegator.SHOW ->

                        adManager.showAd(this@SlidingPuzzleActivity)

                    SlidingPuzzleViewModel.ActionDelegator.LOAD -> adManager.loadRewardedAd()

                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            if (this::adManager.isInitialized) {
                adManager.clear()
            }

        }
    }

    fun shareResult(previewUrl:String,level:Int,difficulty:Int) {
            lifecycleScope.launch {

                val dynamicLink = DynamicLinksCreator.createLinkForSlidingPuzzle(
                    socialTagTitle = getString(R.string.title_for_sharing_scrambled),
                    socialTagDesc = getString(R.string.can_you_solve_this_sliding_puzzle),
                    socialImageLink = previewUrl,
                    scrambledDifficulty = difficulty,
                    userId = firebaseUser?.uid.toString(),
                    level = level

                ).await()

                Log.d(
                    TAG,
                    "onShareTestResult: previewLink:${dynamicLink.previewLink} :\n short link ${dynamicLink.shortLink}"
                )
                val share = Intent.createChooser(Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${dynamicLink.shortLink}")

                    // (Optional) Here we're setting the title of the content
                    putExtra(Intent.EXTRA_TITLE, getString(R.string.can_you_solve_this_sliding_puzzle))


                }, "this is title")
                startActivity(share)
            }


    }

    val TAG = "SlidingPuzzleAct"

    public fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.statusBars())

    }
}


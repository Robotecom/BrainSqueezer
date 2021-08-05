package com.puzzlemind.brainsqueezer.mcq

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.*
import com.puzzlemind.brainsqueezer.R
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.ui.theme.MCQTheme
import com.puzzlemind.brainsqueezer.utils.DynamicLinksCreator
import com.puzzlemind.brainsqueezer.utils.LinkSchema
import com.puzzlemind.brainsqueezer.utils.isOnline
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class McqActivity : ComponentActivity() {


    private var elapsedTimeInSeconds: Long = 0
    private var firebaseUser: FirebaseUser? = null
    val TAG = "MCQ_Activity"
    var level = 1
    private var themeIndex = 0
    private var mRewardedAd: RewardedAd? = null
    private val mcqViewModel  by viewModels<McqViewModel> {
        McqViewModelFactory(
            (application as PuzzleApp).repository,
            level = level,
            applicationContxt = application
        )

    }

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        firebaseUser = Firebase.auth.currentUser

        if (intent.data == null) {
            level = intent.getIntExtra(Constants.LEVEL_KEY, 1)
            themeIndex = intent.getIntExtra(Constants.THEME_COLOR_INDEX, 0)
        }else{
            Log.d(TAG, "onCreate:dynamicLink ${intent.data}")
            mcqViewModel.loadingLevel()

            if(isOnline(this)) {
                Firebase.dynamicLinks.getDynamicLink(intent)
                    .addOnSuccessListener { pendingDynamicLinkData ->
                        // Get deep link from result (may be null if no link is found)

                        val deepLink: Uri?
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.link

                            Log.d(TAG, "onCreate: deepLink:${deepLink}")
                            level = (deepLink?.getQueryParameter("level") ?: "1").toInt()
                            mcqViewModel.level = level

                            mcqViewModel.levelReady()
                            Log.d(TAG, "onCreate: level in url:${level}")
                            sendRewardToReferrer(deepLink)

                        }else{
                            mcqViewModel.level = level
                        }
                    }.addOnFailureListener{
                        Log.d(TAG, "onCreate: failed to get link from dynamic link:${it.message}")
                        mcqViewModel.level = level
                        mcqViewModel.levelReady()
                    }
            }else{
                Toast.makeText(this,getString(R.string.this_operation_need_internet),Toast.LENGTH_LONG).show()
                mcqViewModel.levelReady()

            }
        }


        setContent {
            MCQTheme(colorsThemeIndex = themeIndex) {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val mcqState = mcqViewModel.mcqState.collectAsState()

                    MCQ_Scaffold(
                        clickCombined(
                            onChoice = { answer -> mcqViewModel.onChoice(answer) },
                            onRemoveTwo = { mcqViewModel.onRemoveTwo() },
                            onWatchAd = {
                                if (isOnline(this)) {
                                    mcqViewModel.onWatchAd()
                                }else{
                                    Toast.makeText(this,R.string.this_feature_needs_internet,Toast.LENGTH_LONG).show()
                                }
                                        },
                            onChangeQuestion = { mcqViewModel.onChangeQuestion() },
                            onStartTest = { mcqViewModel.onStartTest() },
                            onBackPressed = { mcqViewModel.onBackPressed() },
                            onMuteSound = {mcqViewModel.onMuteSound()},
                            onRewardCallback = rewardCallback(
                                onRewardEarned = { mcqViewModel.onVideoRewardEarned() },
                                onError = { errMsg -> mcqViewModel.onRewardVidError(errMsg) },
                                onDismiss = { mcqViewModel.onRewardVideoDismissed() },
                                onDismissError = { mcqViewModel.onDismissError() },
                                onAdShowing = { mcqViewModel.onAdShowing() }
                            ),
                            finalResultClickable = finalClicked(
                                onNextTest = { finish() },
                                onRedoTest = { mcqViewModel.onStartTest() },
                                onShareTestResult = {

                                    issueIntent(level = level)

                                }
                            )
                        ),
                        mcqState = mcqState.value,
                    )
                }
            }
        }
        val rewardUnitId = if (BuildConfig.DEBUG){
            getString(R.string.reward_vid_testing_id)
        }else{
            getString(R.string.view_vid_to_pass_question_unit_id)
        }

        lifecycleScope.launch {
            mcqViewModel.adState.collect {
                if (it.show){

                    showAd(mcqViewModel)
                }

                if (it.load){
                    if (!mcqViewModel.adState.value.show)
                    if (isOnline(this@McqActivity)) {
                        loadRewardedAd(rewardUnitId, mcqViewModel)
                        mcqViewModel.adState.value = mcqViewModel.adState.value.copy(load = false)
                    }
                }

            }
        }


        loadRewardedAd(rewardUnitId,mcqViewModel)

    }

    private fun showAd(mcqViewModel: McqViewModel) {
        mRewardedAd?.show(this@McqActivity
        ) {
            mcqViewModel.onVideoRewardEarned()
            mcqViewModel.adState.value = mcqViewModel.adState.value.copy(show = false)

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mRewardedAd = null

    }


    private fun loadRewardedAd(adId: String, mcqViewModel: McqViewModel){
        Log.d(TAG, "loadRewardedAd: loading..........")
        if (SystemClock.elapsedRealtime()/1000 - elapsedTimeInSeconds < 10)return

        elapsedTimeInSeconds = SystemClock.elapsedRealtime()/1000
        val request = AdRequest.Builder().build()
        RewardedAd.load(this,adId,request,object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError?.message)
                mRewardedAd = null
                mcqViewModel.onRewardVidError(adError.message)
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                Log.d(TAG, "loadRewardedAd: Ad was loaded.")
                mRewardedAd = rewardedAd

                mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "onAdDismissedFullScreenContent: ")
                        mcqViewModel.onRewardVideoDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                        Log.d(TAG, "onAdFailedToShowFullScreenContent: ${adError?.message}")
                        mcqViewModel.onRewardVidError(adError?.message!!)
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "onAdShowedFullScreenContent: ")
                        // Called when ad is dismissed.
                        // Don't set the ad reference to null to avoid showing the ad a second time.

                        mRewardedAd = null
                    }
                }

                if (mcqViewModel.adState.value.show){
                    showAd(mcqViewModel = mcqViewModel)
                }

            }
        })
    }

    private fun sendRewardToReferrer(deepLink: Uri?) {

        val referrerId = deepLink?.getQueryParameter(LinkSchema.USER_ID)
        Firebase.firestore.document("${Constants.USERS}/${referrerId}")
            .collection(Constants.REWARDS).document(firebaseUser?.uid!!)
            .set(mutableMapOf(
                "MCQ_Sharing" to true,
                "userId" to firebaseUser?.uid))
    }

    private fun issueIntent(level:Int) {
        lifecycleScope.launch {

            val dynamicLink = DynamicLinksCreator.getDynamicLongShortLink(
                userId = firebaseUser?.uid.toString(),
                level = level,
                socialTagTitle = getString(R.string.app_name),
                socialTagDesc = getString(R.string.share_game_title),
                socialImageLink = getString(R.string.app_icon_url)
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
                putExtra(Intent.EXTRA_TITLE, getString(R.string.share_game_title))


            }, "this is title")
            startActivity(share)
        }
    }


    fun Activity.makeStatusBarTransparent() {

        WindowCompat.getInsetsController(window, window.decorView)
            ?.hide(WindowInsetsCompat.Type.statusBars())

    }

}

@Composable
fun RequestVideoAd(
    rewardId: String, activity: Activity,
    onRewardCallback: RewardedVideoAdCallBacks = rewardCallback()
) {
    val adRequest = AdRequest.Builder().build()
    var mRewardedAd: RewardedAd?

    val context = LocalContext.current

    println("load rewardAd for me*******")
    RewardedAd.load(context, rewardId, adRequest, object : RewardedAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {

            onRewardCallback.onError(
                context.resources.getString(
                    R.string.check_network,
                    adError.message
                )
            )
            println("RewardAd:${adError.message}")
            mRewardedAd = null
        }

        override fun onAdLoaded(rewardedAd: RewardedAd) {
            println("RewardAd was loaded.")
            mRewardedAd = rewardedAd


            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    println("RewardAd was dismissed.")
                    onRewardCallback.onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError?) {
                    println("RewardAd failed to show.${adError?.message}")
                    onRewardCallback.onError("Ad Can't be Shown")
                }

                override fun onAdShowedFullScreenContent() {
                    println("RewardAd showed fullscreen content.")
                    // Called when ad is dismissed.
                    // Don't set the ad reference to null to avoid showing the ad a second time.

                    mRewardedAd = null
                }
            }

            if (mRewardedAd != null) {

                mRewardedAd?.show(activity) {
                    println("RewardAd User earned the reward.")
                    onRewardCallback.onRewardEarned()
                }
            } else {
                println("RewardAd The rewarded ad wasn't ready yet.")
            }
        }
    })

}

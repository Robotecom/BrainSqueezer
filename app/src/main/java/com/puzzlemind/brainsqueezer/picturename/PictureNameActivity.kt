package com.puzzlemind.brainsqueezer.picturename

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.puzzlemind.brainsqueezer.*
import com.puzzlemind.brainsqueezer.mcq.finalClicked
import com.puzzlemind.brainsqueezer.mcq.rewardCallback
import com.puzzlemind.brainsqueezer.ui.theme.BrainSqueezerTheme

class PictureNameActivity : ComponentActivity() {

    val TAG = "PictureNameActiv"
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    val whosInPicViewModel = viewModel<WhoIsInPicViewModel>()

                    val state by whosInPicViewModel.ui_state.collectAsState()

                    WhoIsInPicScaffold(this, whoIsClicked(
                        imgLoadingError = {whosInPicViewModel.imgLoadingError()},
                        resumeCountDown = {any -> whosInPicViewModel.resumeCountDown(any)},
                        startTest = {whosInPicViewModel.startTest()},
                        onAnswerSelected = {answer -> whosInPicViewModel.onAnswerSelected(answer)},
                        onBackPress = {whosInPicViewModel.onBackPressed()},
                        finalResultClickable = finalClicked(
                            onRedoTest = {whosInPicViewModel.onRedoTest()},
                            onNextTest = {finish()}
                        ),
                        rewardedVideoAdCallBacks = rewardCallback (
                            onRewardEarned = {whosInPicViewModel.onVideoRewardEarned()},
                            onError = {errMsg -> whosInPicViewModel.onRewardVidError(errMsg)},
                            onDismiss = {whosInPicViewModel.onRewardVideoDismissed()},
                            onDismissError = {whosInPicViewModel.onDismissError()},
                            onAdShowing = {whosInPicViewModel.onAdShowing()}
                        ),

                        onShowVideoAd = {whosInPicViewModel.showVideoAd()},
                        onRemoveChoices = {whosInPicViewModel.onRemoveChoices()}
                    ),whosInPicState = state)
                }
            }
        }

        FirebaseFirestore.getInstance().collection("some").addSnapshotListener(object :
            EventListener<QuerySnapshot> {
            override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {

                Log.d(TAG, "onEvent: ${value?.documents}")
            }
        })

    }



}

@Composable
fun Greeting3(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview3() {
    BrainSqueezerTheme {
        Greeting3("Android")
    }
}
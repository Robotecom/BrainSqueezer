package com.puzzlemind.brainsqueezer

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.puzzlemind.brainsqueezer.data.PicName
import com.puzzlemind.brainsqueezer.mcq.ButtonState
import com.puzzlemind.brainsqueezer.mcq.TestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random


class WhoIsInPicViewModel: ViewModel() {

    private val imageTime: Int = 1      //seconds
    private val gap = 300               //in milliseconds
    private var numberOfImages = 4      //this value is used in countDownTimer

    private var rewardVideoEarned: Boolean = false
    private lateinit var testResult: TestResult
    private var currentIndex: Int = 1
    private var incorrectAnsCount: Int = 0
    private var correctAnsCount: Int = 0
    private var picName: PicName = PicName()
    private var remainingTime = 0
    val TAG:String = "WhoIsInPicViewModel"

    val _ui_state = MutableStateFlow(WhoIsInPicState())
    val ui_state = _ui_state.asStateFlow()


    val testImageKeyList = mutableListOf<String>()
    var timerTime = 0
    var imageIndex = 0
    val countDownTimer = object :CountDownTimer(1000 * imageTime * numberOfImages + gap * numberOfImages.toLong(),gap + imageTime*1000L){
        override fun onTick(millisUntilFinished: Long) {
            remainingTime = (millisUntilFinished/1000).toInt()
            changePic()
            Log.d(TAG, "onTick: ")
        }

        override fun onFinish() {

            Log.d(TAG, "onFinishTime: ")

            finishShowing()

        }

    }

    private fun finishShowing() {
        val state = _ui_state.value.copy(
            currentImage = Pair("",0),
            request = picName.hint,
            showRequest = true,
            hideTimer = true
        )
        emitState(state)

        viewModelScope.launch {
            delay(2000)
            selectRandomImage()
            emitState(_ui_state.value.copy(buttonState = ButtonState()))
        }
    }

    private fun selectRandomImage() {
        val randomIndex = Random.nextInt(testImageKeyList.size)

        val choicesList = getChoices(testImageKeyList[randomIndex])
        choicesList.shuffle()
        emitState(_ui_state.value.copy(
            showChoices = true,
            currentImage = Pair("",picName.picNameMap[testImageKeyList[randomIndex]]!!),
            choices = Pair(testImageKeyList[randomIndex],choicesList),
            currentIndex = currentIndex++
        ))
    }

    private fun getChoices(correctAnswer: String): MutableList<String> {
        val choices = mutableListOf(correctAnswer)
        for (item in picName.picNameMap.entries){
            if (choices.size == 4)break
            if (item.key != correctAnswer) {
                choices.add(item.key)
            }
        }
        return choices
    }

    private fun changePic() {
        Log.d(TAG, "changePic: ")
        if (imageIndex < testImageKeyList.size) {
            viewModelScope.launch {
                resetTimer()
                val state = _ui_state.value.copy(
                    timerValue = 200f,
                    timerState = TimerState(imageTime, true),
                    currentImage = Pair(
                        testImageKeyList[imageIndex],
                        picName.picNameMap[testImageKeyList[imageIndex]]!!
                    ),
                    totalImages = testImageKeyList.size
                )

                emitState(state)
                imageIndex++
            }
        }


    }

    private suspend fun resetTimer() {
        val state = _ui_state.value.copy(timerState = TimerState(5,false))
        emitState(state)
        delay(300)
    }

    init {

        prapareTest()

    }

    private fun prapareTest() {
        correctAnsCount = 0
        incorrectAnsCount = 0
        currentIndex = 1
        imageIndex = 0
        picName = getTest()
        val state = _ui_state.value.copy(
            picName = picName,
            showChoices = false,
            showRequest = false,
            showResult = false,
            showReady = true,
            hideTimer = false,
            buttonState = ButtonState(false,false,false)
            )

        for (item in picName.picNameMap.entries){
            testImageKeyList.add(item.key)
        }
        emitState(state)
    }


    fun emitState(state:WhoIsInPicState){
        viewModelScope.launch {
            _ui_state.emit(state)

        }
    }

    fun imgLoadingError() {

        val state = ui_state.value.copy(internetConnectError = true,timerState = TimerState(timerTime+1,start = false))

        emitState(state = state)
    }

    fun resumeCountDown(any: Any?) {

        Log.d(TAG, "resumeCountDown:$any")


    }



    fun startTest() {

        emitState(_ui_state.value.copy(showReady = false))
        countDownTimer.start()
    }

    fun getTest():PicName{

        val picsNameMap:MutableMap<String,Int> =
            mutableMapOf<String,Int>()
//        picsNameMap["جغبوص"] = R.drawable.cat
//        picsNameMap["ميمي"] = R.drawable.cat1
//        picsNameMap["كيكي"] = R.drawable.cat2
//        picsNameMap["أصفراني"] = R.drawable.cat4

        return PicName(picNameMap = picsNameMap,hint = "Now, try to give the correct name of each cat you see")
    }

    fun onAnswerSelected(answer: String) {

        Log.d(TAG, "onAnswerSelected: $answer")
        //count correct answers
        //select another image
        val correctAnswer = _ui_state.value.choices.first
        if (correctAnswer == answer){
            correctAnsCount++
            SoundUtil.play(SoundType.Correct)
        }else{
            incorrectAnsCount++
            SoundUtil.play(SoundType.Incorrect)
        }
        testImageKeyList.remove(correctAnswer)
        if (testImageKeyList.size != 0)
        selectRandomImage()
        else showResult()
    }

    private fun showResult() {
        testResult = TestResult(correctAnswers = correctAnsCount,incorrectAnswers = incorrectAnsCount)
        testResult.passed = checkSuccess()
        if (checkSuccess()) {
            playSound(SoundType.CompleteLevel)
        } else {
            playSound(SoundType.LostLevel)

        }
        testResult.numberOfStars = ((testResult.correctAnswers.toFloat()/picName.picNameMap.size)*3).toInt()
        val state = _ui_state.value.copy(showResult = true,testResult = testResult)
        emitState(state)
    }

    private fun playSound(soundType: SoundType) {
        SoundUtil.play(soundType)
    }

    private fun checkSuccess(): Boolean {
        return testResult.correctAnswers > testResult.incorrectAnswers && testResult.correctAnswers > picName.picNameMap.size / 2
    }

    fun onRedoTest() {

        prapareTest()
    }

    fun onBackPressed() {
        Log.d(TAG, "onBackPressed: ")
        if (ui_state.value.showVideoAd){
            emitState(_ui_state.value.copy(showVideoAd = false))
//            resumeTimer()
        }else {
            emitState(_ui_state.value.copy(backPressedEnabled = false))
        }
    }

    val videoWaitTimer = object :CountDownTimer(Constants.VIDEO_RESTRICTION_TIME_LONG_MILLI_SEC,1000){
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            val watchButtonState = _ui_state.value.buttonState.copy(watchAdEnabled = true)
            emitState(_ui_state.value.copy(buttonState = watchButtonState))

        }
    }

    fun onVideoRewardEarned() {

        Log.d(TAG, "onVideoRewardEarned: ")
        emitState(_ui_state.value.copy(showVideoAd = false))
        disableHelper(_ui_state.value.buttonState.copy(watchAdEnabled = false))

        rewardVideoEarned = true
    }

    private fun disableHelper(buttonState: ButtonState) {
        emitState(_ui_state.value.copy(buttonState = buttonState))
    }

    fun onRewardVidError(errMsg: String) {
        Log.d(TAG, "onRewardVidError: $errMsg")
        if (ui_state.value.showVideoAd) {
            emitState(
                _ui_state.value.copy(
                    error = errMsg,
                    showError = true,
                    showVideoAd = false
                )
            )
        }
    }

    fun onRewardVideoDismissed() {
        Log.d(TAG, "onRewardVideoDismissed: ")
        if (rewardVideoEarned){
            viewModelScope.launch {
                chooseAnswerForUser()
                videoWaitTimer.start()
            }
        }else{

//            resumeTimer()
        }
        emitState(_ui_state.value.copy(showVideoAd = false))
        rewardVideoEarned = false
    }

    private suspend fun chooseAnswerForUser() {

        emitState(_ui_state.value.copy(checkCorrectAnw = true))
        delay(1000)
        onAnswerSelected(ui_state.value.choices.first)
        emitState(_ui_state.value.copy(checkCorrectAnw = false))
        Log.d(TAG, "chooseAnswerForUser: ")
    }

    fun onDismissError() {
        Log.d(TAG, "onDismissError: ")
        emitState(_ui_state.value.copy(showError = false,showVideoAd = false))
        rewardVideoEarned = false
//        resumeTimer()
    }

    fun onAdShowing() {
        Log.d(TAG, "onAdShowing: ")
        emitState(_ui_state.value.copy(showVideoAd = false))

//        stopTimer()

    }

    fun showVideoAd() {
        emitState(_ui_state.value.copy(showVideoAd = true))
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: ")
        if (countDownTimer != null) {
            countDownTimer.cancel()
        }
    }

    fun onRemoveChoices() {
        Log.d(TAG, "onRemoveChoices: ")
        val correctAnswer = _ui_state.value.choices.first
        val choices = mutableListOf<String>()
        choices.addAll(_ui_state.value.choices.second)
        choices.remove(correctAnswer)

        choices.removeAt(Random.nextInt(3))
        choices.removeAt(Random.nextInt(2))
        choices.add(correctAnswer)

        choices.shuffle()

        val buttonState = _ui_state.value.buttonState.copy(removeTwoEnabled = false)
        emitState(_ui_state.value.copy(choices = Pair(correctAnswer,choices),buttonState = buttonState))

    }

}

data class WhoIsInPicState(
    var timerValue: Float = 360f, val timerTime: Int = 0,
    var picName: PicName = PicName(),
    var currentImage: Pair<String, Int> = Pair("", 0),
    var internetConnectError: Boolean = false,
    var timerState: TimerState = TimerState(0, true),
    var request: String = "",
    var showRequest: Boolean = false,
    var showChoices: Boolean = false,
    var choices: Pair<String, MutableList<String>> = Pair("", mutableListOf<String>()),
    var currentIndex: Int = 1,
    val totalImages: Int = 0,
    val showResult: Boolean = false,
    val testResult: TestResult = TestResult(),
    var showReady: Boolean = true,
    var hideTimer: Boolean = false,
    val showVideoAd: Boolean = false,
    val buttonState: ButtonState = ButtonState(),
    var error:String = "",
    var showError:Boolean = false,
    var backPressedEnabled:Boolean = true,
    var checkCorrectAnw: Boolean = false
)

data class TimerState(var time:Int = 0,var start:Boolean = false)
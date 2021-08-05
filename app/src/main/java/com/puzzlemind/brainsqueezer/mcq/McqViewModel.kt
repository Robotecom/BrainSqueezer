package com.puzzlemind.brainsqueezer.mcq

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.puzzlemind.brainsqueezer.*
import com.puzzlemind.brainsqueezer.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlin.random.Random

class McqViewModel(
    val repository: AppRepository, val leve: Int,
    application: Application
) : AndroidViewModel(
    application

) {

    private lateinit var remoteConfig: FirebaseRemoteConfig
    private val applicationContext = application
    private var rewardVideoEarned: Boolean = false
    private var testGenerator: TestGenerator = TestGenerator(repository)
    private var flagComplete: Boolean = false
    private val TEST_QUESTIONS_COUNT: Int = 6
    private var testTime = TEST_QUESTIONS_COUNT * 10           //in seconds
    val VIDEO_RESTRICTION_TIME: Long = testTime * 1000 / 3L
    private var remove_choices_cost = 100
    private var change_question_cost = 50
    private val SHOW_VIDEO_AD_EVERY_4_LEVELS = 4

    private lateinit var timer: Timer
    private var remainingTime: Int = 0
    var level = leve
    val TAG = "MCQ_ViewModel"
    private var questionIndex: Int = 0

    val _mcq_state = MutableStateFlow(MCQState())
    val mcqState = _mcq_state.asStateFlow()
    val adState = MutableStateFlow(RewardAdState())
    val sharedPre:SharedPreferences

    private val testResult: TestResult = TestResult()

    fun emit(mcqState: MCQState) {
        Log.d(TAG, "emit: new state")
        viewModelScope.launch {
            _mcq_state.emit(mcqState)

        }
    }

    init {
        viewModelScope.launch {
            repository.getLiveUser(0).cancellable().collect { user ->

                Log.d(TAG, "mcq_viewmodel: collect user")
                val header = _mcq_state.value.headerState.copy(balance = user.balance)
                emit(_mcq_state.value.copy(headerState = header))
            }
        }

        sharedPre = applicationContext.getSharedPreferences(
            applicationContext.getString(
                R.string.preference_file_key
            ), Context.MODE_PRIVATE
        )

        setupRemoteConfig()

    }

    fun setupRemoteConfig(){

        remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 24*3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        remoteConfig.fetchAndActivate().addOnCompleteListener {

            if (it.isSuccessful) {
                remove_choices_cost = remoteConfig.getLong("remove_two_choices_cost").toInt()
                change_question_cost = remoteConfig.getLong("change_question_cost").toInt()
            }
        }
    }

    fun onChoice(answer: String) {

        if (answer.trim() == _mcq_state.value.currentQuestion.answer) {
            testResult.correctAnswers++
            playSound(SoundType.Correct)
            saveAsAnswered(_mcq_state.value.currentQuestion.id, Answered.CORRECT)
        } else {
            testResult.incorrectAnswers++
            playSound(SoundType.Incorrect)
            saveAsAnswered(_mcq_state.value.currentQuestion.id, Answered.INCORRECT)

        }


        ++questionIndex
        if (questionIndex > TEST_QUESTIONS_COUNT - 1) {
            onTimeFinished()
            return
        }

        viewModelScope.launch {
            emitAnswer()

        }

    }

    private fun emitAnswer() {

        val state = _mcq_state.value.copy(
            currentProgress = questionIndex + 1,
            currentQuestion = _mcq_state.value.questions[questionIndex],
            correctAnswers = testResult.correctAnswers,
            wrongAnswers = testResult.incorrectAnswers
        )

        emit(state)

    }

    private fun saveAsAnswered(id: Int, correct: Answered) {
        viewModelScope.launch {

            repository.updateQuestionAnswered(id, correct)

        }
    }

    fun onRemoveTwo() {

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val user = repository.getUser(0)

                if (user.balance > remove_choices_cost) {

                    repository.deductFromBalance(user, deduction = remove_choices_cost)

                    Log.d(TAG, "onRemoveTwo: ")
                    val answer = _mcq_state.value.currentQuestion.answer
                    val currentChoice = _mcq_state.value.currentQuestion.choices
                    currentChoice.remove(answer.trim())

                    val shortList = mutableListOf<String>()
                    shortList.add(answer)

                    shortList.add(currentChoice[Random.nextInt(currentChoice.size)])

                    shortList.shuffle()

                    val currentQuestion = _mcq_state.value.currentQuestion.copy(choices = shortList)
                    val buttonState =
                        _mcq_state.value.helpButtonState.copy(removeTwoEnabled = false)
                    val state =
                        _mcq_state.value.copy(
                            currentQuestion = currentQuestion,
                            helpButtonState = buttonState
                        )
                    emit(state)
                    playSound(SoundType.RemoveTwoChoices)

                } else {

                    playSound(SoundType.Unsuccessful)

                }
            }
        }


    }

    fun onWatchAd() {
        //shows a video add
        Log.d(TAG, "onWatchAd: ")
        //stop time or reset it
        //pass question and add it as solved

        stopTimer()
        adState.value = adState.value.copy(show = true,load = true)

        val state = _mcq_state.value.copy(showVideoAd = true)

        emit(state)
    }


    private fun stopTimer() {
        viewModelScope.launch {
            remainingTime++
            timer.cancel()
            val state = _mcq_state.value.copy(startTimer = false, timerTime = remainingTime)
            emit(state)
            delay(400)
        }

    }

    private fun resumeTimer() {
        viewModelScope.launch {
            timer?.cancel()
            timer = Timer(remainingTime * 1000L, 1000)
            timer.start()
            val state = _mcq_state.value.copy(startTimer = true, timerTime = remainingTime)
            emit(state)
            delay(400)
        }
    }

    private val INTERVAL = 10
    fun onChangeQuestion() {

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val user = repository.userDao.getUser(0)

                if (user.balance > change_question_cost) {

                    withContext(Dispatchers.Main) {
                        resetTimer(remainingTime + INTERVAL)

                    }

                    val question =
                        getQuestion(testGenerator.getQuestionNotInList(count = repository.getQuestionCount())).also {
                            it.choices.add(it.answer)
                            it.choices.addAll(it.incorrectChoices)
                            it.choices.shuffle()

                        }
                    val buttonState =
                        _mcq_state.value.helpButtonState.copy(changeQuestionEnabled = false)
                    val state = _mcq_state.value.copy(
                        helpButtonState = buttonState,
                        currentQuestion = question,
                    )
                    emit(state)
                    repository.deductFromBalance(user, deduction = change_question_cost)

                    playSound(SoundType.ChangeQuestion)


                } else {
                    playSound(SoundType.Unsuccessful)
                }
            }
        }


    }

    fun onStartTest() {
        viewModelScope.launch {
            adState.value = adState.value.copy(show = false, load = false)

            withContext(Dispatchers.IO) {
                _mcq_state.value = generateUIState()
            }
            flagComplete = false
            startTimer()
        }

    }

    inner class Timer(millisInFuture: Long, countDownInterval: Long) :
        CountDownTimer(millisInFuture, countDownInterval) {

        override fun onTick(millisUntilFinished: Long) {
            remainingTime = (millisUntilFinished / 1000).toInt()

            if (remainingTime == 5) {
                if (!flagComplete)
                    playSound(SoundType.ClockTicking)
                disableHelpers()
            }
        }

        override fun onFinish() {

            onTimeFinished()
        }
    }

    private fun disableHelpers() {

        emit(
            _mcq_state.value.copy(
                helpButtonState = ButtonState(
                    changeQuestionEnabled = false,
                    removeTwoEnabled = false,
                    watchAdEnabled = false
                )
            )
        )

    }

    private var mediaPlayer: MediaPlayer? = null
    private fun playSound(soundType: SoundType) {
        if (_mcq_state.value.headerState.soundOn) {
            mediaPlayer =
                MediaPlayer.create(applicationContext, SoundUtil.getResourceId(sound = soundType))
            mediaPlayer?.setOnCompletionListener {
                it?.reset()
                it?.release()
            }
            mediaPlayer?.start()
        }
    }


    fun onTimeFinished() {

        timer?.cancel()

        if (!flagComplete) {

            Log.d(TAG, "onTimeFinished: remainingTime:$remainingTime")
            testResult.passed = checkSuccess()
            testResult.timeRemaining = remainingTime
            testResult.numberOfStars =
                ((testResult.correctAnswers.toFloat() / TEST_QUESTIONS_COUNT) * 3).toInt()

            if (testResult.numberOfStars > 0) {
                testResult.score =
                    testResult.correctAnswers * Constants.CORRECT_ANS_SCORE + remainingTime / 4
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        saveUserProgress(level, testResult)

                    }

                }
                if (leve % SHOW_VIDEO_AD_EVERY_4_LEVELS == 0) {
                    Log.d(TAG, "onTimeFinished: show ad every three levels is showing/////*****")
                    showRewardAdForFailure()
                }
            } else {
                showRewardAdForFailure()
                testResult.score = 0
            }


            val state = _mcq_state.value.copy(
                showFinalResult = true, testResult = testResult
            )
            if (checkSuccess()) {
                playSound(SoundType.CompleteLevel)
            } else {
                playSound(SoundType.LostLevel)

            }
            emit(state)
        }
        viewModelScope.launch {
            zeroTimer()
        }

        flagComplete = true

    }

    private suspend fun saveUserProgress(level: Int, testResult: TestResult) {

        val levelProgress: Level = repository.getLevelData(level = level, game = Game.MCQ)

        println("saveUserProgress:$testResult")
        if (levelProgress != null) {
            Log.d(TAG, "saveUserProgress: updating")
            if (testResult.score > levelProgress.score) {

                repository.updateUserBalance(testResult.score - levelProgress.score, id = 0)
                repository.updateProgress(
                    levelProgress.copy(
                        stars = testResult.numberOfStars,
                        trophy = testResult.numberOfStars == 3,
                        score = testResult.score.toFloat(),
                        isOpen = true,
                        isPassed = testResult.passed,
                        scheduled = true
                    )
                )

                if (testResult.passed) {

                    repository.openNextLevel(level + 1)

                    if (levelProgress.isFinal) {
                        openNextDifficulty(level = level + 1)
                    }
                } else {

                    showRewardAdForFailure()

                }


            } else {
                if (testResult.passed) {

                    repository.openNextLevel(level + 1)

                    if (levelProgress.isFinal) {
                        openNextDifficulty(level = level + 1)
                    }
                } else {

                    showRewardAdForFailure()

                }
                Log.d(TAG, "saveUserProgress: score is lower than previous value")
            }
        } else {

            repository.updateLevelProgress(
                Level(
                    id = level,
                    game = Game.MCQ,
                    level = level,
                    stars = testResult.numberOfStars,
                    trophy = testResult.numberOfStars == 3,
                    score = testResult.score.toFloat()
                )
            )

            Log.d(TAG, "saveUserProgress: first time to save progress:level:$level")
        }
    }

    private fun showRewardAdForFailure() {

        viewModelScope.launch {

            delay(1200)
            adState.value = adState.value.copy(show = true)

        }
    }

    private fun openNextDifficulty(level: Int) {

        repository.openNextDifficulty(level, Game.MCQ)
    }

    private fun checkSuccess(): Boolean {
        return testResult.correctAnswers > testResult.incorrectAnswers && testResult.correctAnswers > TEST_QUESTIONS_COUNT / 2
    }

    private suspend fun resetTimer(seconds: Int) {

        var state = _mcq_state.value.copy(startTimer = false, timerTime = seconds)
        emit(state)
        delay(400)
        state = _mcq_state.value.copy(startTimer = true)
        emit(state)

        timer?.cancel()
        timer = Timer(seconds * 1000L, 1000)
        timer.start()

    }

    private fun zeroTimer() {

        val state = _mcq_state.value.copy(startTimer = false, timerTime = testTime)
        emit(state)

    }


    private suspend fun startTimer() {

        delay(400)
        val state = _mcq_state.value.copy(startTimer = true)
        emit(state)

        timer = Timer(testTime * 1000L, 1000)
        timer.start()
    }


    private fun getQuestion(id: Int): Question {

        return repository.fetchQuestion(id)
    }


    private fun generateUIState(): MCQState {

        Log.d(TAG, "generateUIState: level is:${level}")
        val currentLevel = repository.getLevelData(level = level, game = Game.MCQ)

        testTime = getTimeAccordingToDifficulty(currentLevel.diffIndex, currentLevel.isPassed)
        val mcqState = MCQState()

        val headerState = HeaderState()
        headerState.currentLevel = level
        headerState.questionNumber = 1
        headerState.balance = getUserBalance()
        headerState.stars = 4
        headerState.soundOn = sharedPre.getBoolean(Constants.SOUND_ON_KEY, true)
        mcqState.firstOpen = false
        mcqState.headerState = headerState
        mcqState.timerTime = testTime
        questionIndex = 0
        testResult.zeroVariables()


        val ques = testGenerator.createTest(level - 1, TEST_QUESTIONS_COUNT)

        ques.forEach {
            val choicesList = mutableListOf<String>()
            choicesList.add(it.answer)
            choicesList.addAll(it.incorrectChoices)
            choicesList.shuffle()
            it.choices = choicesList
        }

        ques.shuffle()
        mcqState.currentQuestion = ques.first()

        mcqState.questions = ques
        mcqState.currentProgress = 1
        mcqState.totalQueCount = TEST_QUESTIONS_COUNT
        return mcqState
    }

    private fun getTimeAccordingToDifficulty(diffIndex: Difficulty, passed: Boolean): Int {

        val testT = (16 - DifficultyConverter.fromEnumToInt(diffIndex)) * TEST_QUESTIONS_COUNT

        return if (passed) {
            testT * 60 / 100
        } else {
            testT
        }

    }

    private fun getUserBalance(): Float {
        return repository.getUser(0).balance
    }

    override fun onCleared() {
        super.onCleared()
        flagComplete = true
        timer.cancel()
        mediaPlayer?.release()
        mediaPlayer = null

    }

    fun onBackPressed() {
        Log.d(TAG, "onBackPressed: ")
        if (mcqState.value.showVideoAd) {
            emit(_mcq_state.value.copy(showVideoAd = false))
            resumeTimer()
        } else {
            emit(_mcq_state.value.copy(backPressedEnabled = false))
        }
    }

    fun onVideoRewardEarned() {
        //user can pass the question, mark question as solved
        Log.d(TAG, "onVideoRewardEarned: ")
        rewardVideoEarned = true

        emit(_mcq_state.value.copy(showVideoAd = false))
        disableHelper(_mcq_state.value.helpButtonState.copy(watchAdEnabled = false))


    }

    private suspend fun chooseAnswerForUser() {
        emit(_mcq_state.value.copy(checkCorrectAnswer = true))
        delay(1500)

        onChoice(mcqState.value.currentQuestion.answer)
        emit(_mcq_state.value.copy(checkCorrectAnswer = false))
        if (!flagComplete) {
            resumeTimer()
        }

    }

    fun onRewardVidError(errMsg: String) {
        Log.d(TAG, "onRewardVidError:$errMsg ")
        if (mcqState.value.showVideoAd) {
            emit(
                _mcq_state.value.copy(
                    error = errMsg,
                    showError = true,
                    showVideoAd = false
                )
            )
        }
        adState.value = adState.value.copy(load = true,show = false)
    }

    private val videoWaitTimer = object : CountDownTimer(VIDEO_RESTRICTION_TIME, 1000) {
        override fun onTick(millisUntilFinished: Long) {
        }

        override fun onFinish() {
            if (remainingTime > 5) {
                val watchButtonState = _mcq_state.value.helpButtonState.copy(watchAdEnabled = true)
                emit(_mcq_state.value.copy(helpButtonState = watchButtonState))
            }
        }
    }

    fun onRewardVideoDismissed() {
        Log.d(TAG, "onRewardVideoDismissed: ")

        if (!flagComplete) {
            if (rewardVideoEarned) {
                viewModelScope.launch {

                    chooseAnswerForUser()
                    videoWaitTimer.start()
                }

            } else {

                resumeTimer()

            }
        }
        emit(_mcq_state.value.copy(showVideoAd = false))

        rewardVideoEarned = false

        adState.value = adState.value.copy(load = true,show = false)

    }

    private fun disableHelper(buttonToChangeState: ButtonState) {

        emit(_mcq_state.value.copy(helpButtonState = buttonToChangeState))
    }

    fun onDismissError() {
        emit(_mcq_state.value.copy(showError = false, showVideoAd = false))
        rewardVideoEarned = false
        resumeTimer()
    }

    fun onAdShowing() {
        stopTimer()
    }

    fun levelReady() {
        emit(_mcq_state.value.copy(loadingLevel = false))

    }

    fun loadingLevel() {
        emit(_mcq_state.value.copy(loadingLevel = true))
    }

    fun onMuteSound() {

        val headerState = _mcq_state.value.headerState.copy(soundOn = !_mcq_state.value.headerState.soundOn)

        sharedPre.edit().putBoolean(Constants.SOUND_ON_KEY,headerState.soundOn).apply()
        emit(_mcq_state.value.copy(headerState = headerState))
    }

}

@Keep
data class RewardAdState(
    val show: Boolean = false,
    val load: Boolean = false,
)

class McqViewModelFactory(
    private val repository: AppRepository,
    private val level: Int,
    val applicationContxt: Application
) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(McqViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return McqViewModel(
                repository = repository,
                leve = level,
                application = applicationContxt
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


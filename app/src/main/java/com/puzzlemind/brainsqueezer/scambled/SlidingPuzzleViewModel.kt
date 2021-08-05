package com.puzzlemind.brainsqueezer.scambled

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
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.SoundType
import com.puzzlemind.brainsqueezer.SoundUtil
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledRepository
import com.puzzlemind.brainsqueezer.utils.CountUpTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.random.Random

class SlidingPuzzleViewModel(val repository: ScrambledRepository, application: Application) :
    AndroidViewModel(
        application
    ) {

    private lateinit var showingAdPurpose: ShowAdFor
    private var rewardEarned: Boolean = false
    val TAG = "PuzzleViewMo"
    val applicationContext: Application = application
    val _uiState = MutableStateFlow(PuzzleUIState())
    private lateinit var puzzleEngine: PuzzleEngine


    private val tileListState = MutableStateFlow(emptyList<PuzzleTile>())
    private val timerState = MutableStateFlow(0)
    private val previewTimerState = MutableStateFlow(0)
    private var puzzleSize = MutableStateFlow(1)
    internal val adState = MutableStateFlow(ActionDelegator.LOAD)

    private lateinit var stopWatchTimer: CountUpTimer
    private val SHOW_NUMBER_HINT_TIME_INTERVAL = 7000L
    private val PREVIEW_HINT_SHOW_TIME_INTERVAL = 10000L

    private val STAR_WORTH_POINTS = 10
    private val TIME_WORTH_POINTS = 1
    private val HINT_WORTH_POINTS = 3
    private val MOVE_WORTH_POINT = 1

    private val SHOW_IMAGE_HINT_COST = 50
    private val SHOW_NUMBER_HINT_COST = 100

    private lateinit var scrambledLevel: ScrambledLevel

    val uiState: StateFlow<PuzzleUIState>
        get() = _uiState.asStateFlow()

    private val scope = viewModelScope
    val sharedPre: SharedPreferences


    fun setLevel(level: Int, difficulty: Int) {


        scope.launch {
            withContext(Dispatchers.IO) {
                scrambledLevel = repository.getLevel(level, difficulty)


                puzzleEngine = PuzzleEngine(scrambledLevel.isPassed)
                _uiState.value = _uiState.value.copy(
                    helperButtonState = HelperButtonState(
                        numberHintCount = scrambledLevel.hintNumber,
                        imageHintCount = scrambledLevel.hintImage
                    ),
                    level = scrambledLevel.level,
                    timeToSolve = scrambledLevel.timeToSolve,
                    minimumMoves = scrambledLevel.minimumMovesCount,
                    startDialogText = when (difficulty) {
                        ScrambledDifficulty.Easy.index -> R.string.easy_difficulty_start_dialog_text
                        ScrambledDifficulty.Medium.index -> R.string.medium_difficulty_start_dialog_text
                        else -> R.string.hard_difficulty_start_dialog_text
                    }

                )
                if (scrambledLevel.isResource) {

                    puzzleSize.value = scrambledLevel.puzzleSize
                    puzzleEngine.makePuzzle(
                        tileListRes = fetchLevelResources(
                            difficulty = difficulty,
                            level = level,
                            puzzleSize.value
                        ),
                        puzzleDimen = puzzleSize.value,
                        defaultConfi = scrambledLevel.defaultConfig
                    )
                    tileListState.value = puzzleEngine.getTileList()

                } else {

                    puzzleSize.value = scrambledLevel.puzzleSize

                    puzzleEngine.makePuzzleFromFiles(
                        tileUris = fetchLevelResourcesFromFile(scrambledLevel = scrambledLevel),
                        puzzleDimen = puzzleSize.value,
                        defaultConfi = scrambledLevel.defaultConfig
                    )

                    tileListState.value = puzzleEngine.getTileList()
                }

            }
        }

    }

    private fun fetchLevelResourcesFromFile(scrambledLevel: ScrambledLevel): MutableList<String> {

        val folderName = scrambledLevel.fid

        //name of hintPreview.webp is fixed
        _uiState.value =
            _uiState.value.copy(previewHintImageFromFile = "${folderName}/hintPreview.webp")

        val listFiles = mutableListOf<String>()
        //downloaded tiles are zero-based
        for (file in 0 until scrambledLevel.puzzleSize * scrambledLevel.puzzleSize) {
            listFiles.add("${folderName}/layer_${file}.webp")
        }

        return listFiles
    }

    private fun fetchLevelResources(
        difficulty: Int,
        level: Int,
        puzzleSize: Int
    ): MutableList<Int> {

        //preview in resources are the zero layer
        _uiState.value = _uiState.value.copy(
            previewHintImageResourceId =
            applicationContext
                .resources
                .getIdentifier(
                    "d${difficulty}_l${level}_preview",
                    "drawable", applicationContext.packageName
                )
        )
        val imageList = mutableListOf<Int>()

        for (item in 0 until puzzleSize * puzzleSize) {

            imageList.add(
                applicationContext
                    .resources
                    .getIdentifier(
                        "d${difficulty}_l${level}_layer_${item}",

                        "drawable", applicationContext.packageName
                    )
            )
        }




        return imageList.toMutableList()
    }

    init {


        scope.launch {

            combine(
                timerState,
                tileListState,
                previewTimerState,
                puzzleSize
            ) { timer, tiles, previewTimer, puzzlieDim ->

                Log.d(TAG, "flowCombiner: called${puzzlieDim}")

                _uiState.value.copy(
                    tileList = tiles,
                    timer = timer,
                    puzzleDimen = puzzlieDim,
                    previewTimer = previewTimer
                )

            }.catch { throwable ->
                Log.d(TAG, "flowCollector: error caught")
                throw  throwable

            }.collect {
                _uiState.value = it
            }

        }

        sharedPre = applicationContext.getSharedPreferences(
            applicationContext.getString(
                R.string.preference_file_key
            ), Context.MODE_PRIVATE
        )

        _uiState.value =
            _uiState.value.copy(soundOn = sharedPre.getBoolean(Constants.SOUND_ON_KEY, true))

    }


    fun onMoved(tilePosFrom: Int) {

        puzzleEngine.moveTile(tilePosFrom)

        Log.d(TAG, "onMoved: item ")

        tileListState.value = puzzleEngine.getTileList()

        _uiState.value = _uiState.value.copy(moves = _uiState.value.moves + 1)

        if (puzzleEngine.isSolved()) {

            viewModelScope.launch {

                _uiState.value = _uiState.value.copy(tileList = puzzleEngine.showBlankTile(true))
                onTestFinish()

            }

            stopTimers()


        }

        if (scrambledLevel.difficulty == ScrambledDifficulty.Hard.index) {
            if (uiState.value.moves >= scrambledLevel.minimumMovesCount) {
                _uiState.value =
                    _uiState.value.copy(failureMsg = R.string.scrambled_failure_for_overpassing_steps)

                viewModelScope.launch {
                    onTestFinish()

                }
            }
        }


    }

    private fun stopTimers() {
        stopWatchTimer.stop()
        if (this::hintTimer.isInitialized)
            hintTimer.cancel()
        if (this::previewHintCountDownTimer.isInitialized)
            previewHintCountDownTimer.cancel()
    }

    private suspend fun onTestFinish() {

        if (!_uiState.value.testFinished) {
            resetTimers()
            _uiState.value = _uiState.value.copy(blockUI = true)

            val passed = isPassed()
            val stars = calculateStars(passed)

            val result = SlidingPuzzleResult(
                wonTrophy = stars == 3,
                timeSpent = uiState.value.timer,
                moves = uiState.value.moves,
                stars = stars,
                passed = passed,
                score = calculateScore(stars),
                levelId = scrambledLevel.id,
                newTimeToSolve = getTimeToSolve(
                    timeSpent = uiState.value.timer,
                    timeToSolve = scrambledLevel.timeToSolve
                ),
                bonusMoney = when (scrambledLevel.difficulty) {
                    ScrambledDifficulty.Easy.index -> 25
                    ScrambledDifficulty.Medium.index -> 50
                    else -> 100
                },
                previewUrl = scrambledLevel.previewUrl

            )

            if (result.score > scrambledLevel.highScore) {

                withContext(Dispatchers.IO) {

                    repository.updateUserBalance(
                        (result.score - scrambledLevel.highScore).toFloat(),
                        id = Constants.USER_RECORD_ID
                    )

                    repository.saveProgress(result)

                    if (result.passed) {
                        playSound(SoundType.CompleteLevel)

                        repository.openNextLevel(
                            nextLevel = scrambledLevel.level + 1,
                            difficulty = scrambledLevel.difficulty
                        )
                    } else {
                        playSound(SoundType.LostLevel)

                    }

                }
                Log.d(TAG, "onTestFinish: passed:${result.passed}")

            } else {
                if (!result.passed) {
                    playSound(SoundType.LostLevel)

                    delay(1000)
                    //show Ad if user failed and every 4th level
                    if (scrambledLevel.level % 4 == 0) {
                        showingAdPurpose = ShowAdFor.FAILURE
                        showAd()
                    }
                } else {
                    playSound(SoundType.CompleteLevel)
                }
            }
            delay(1000)

            _uiState.value = _uiState.value.copy(finalResult = result, testFinished = true)


        }

    }

    private fun getTimeToSolve(timeSpent: Int, timeToSolve: Int): Int {
        return if (timeSpent > timeToSolve) {
            timeToSolve
        } else {
            timeSpent
        }
    }

    private fun calculateScore(stars: Int): Int {
        if (stars == 0) return 0
        return calculateScoreFromMetrics(stars)

    }

    private fun calculateScoreFromMetrics(stars: Int): Int {
        return stars * STAR_WORTH_POINTS +
                calculateTimeDifference() * TIME_WORTH_POINTS +
                calculateRemainingHints() * HINT_WORTH_POINTS +
                calculateMoveRemaining() * MOVE_WORTH_POINT
    }

    private fun calculateMoveRemaining(): Int {

        return if (scrambledLevel.minimumMovesCount > uiState.value.moves) {
            scrambledLevel.minimumMovesCount - uiState.value.moves
        } else {
            0
        }
    }

    private fun calculateRemainingHints(): Int {

        return uiState.value.helperButtonState.imageHintCount +
                uiState.value.helperButtonState.numberHintCount
    }

    private fun calculateTimeDifference(): Int {
        if (uiState.value.timer < scrambledLevel.timeToSolve) {
            return scrambledLevel.timeToSolve - uiState.value.timer
        } else {
            return 0
        }
    }

    private fun isPassed(): Boolean {

        return puzzleEngine.isSolved()
    }

    private fun calculateStars(isPassed: Boolean): Int {

        var stars = 0
        if (isPassed) {

            stars++
        } else {
            return 0
        }

        if (uiState.value.timer < scrambledLevel.timeToSolve
        ) {
            if (scrambledLevel.timeToSolve - uiState.value.timer >= scrambledLevel.timeToSolve / 4)
                stars++
        }

        if (uiState.value.moves < scrambledLevel.minimumMovesCount) {
            if (scrambledLevel.minimumMovesCount - uiState.value.moves >= scrambledLevel.minimumMovesCount / 4)
                stars++
        }

        return stars
    }

    private fun resetTimers() {

        if (this::previewHintCountDownTimer.isInitialized) {
            previewHintCountDownTimer.cancel()
        }
        if (this::stopWatchTimer.isInitialized) {
            stopWatchTimer.reset()
            stopWatchTimer.stop()
        }
        if (this::hintTimer.isInitialized) {
            hintTimer.cancel()
        }
    }

    fun reStart() {

        puzzleEngine.restart()
        tileListState.value = puzzleEngine.getTileList()
        _uiState.value = _uiState.value.copy(
            isSolved = false,
            moves = 0,
            showBlankTile = false,
            testFinished = false,
            doubleScoreShown = false,
            blockUI = false

            )
        stopWatchTimer.reset()
        stopWatchTimer.start()


    }


    fun onStartPuzzle() {
        _uiState.value = _uiState.value.copy(startDialog = false)

        if (!this::stopWatchTimer.isInitialized)
            stopWatchTimer = object : CountUpTimer(1000) {
                override fun onTick(elapsedTime: Int) {
                    Log.d(TAG, "onTick: $elapsedTime")
                    timerState.value = elapsedTime
                    if (scrambledLevel.difficulty >= ScrambledDifficulty.Medium.index) {
                        if (timerState.value >= scrambledLevel.timeToSolve) {
                            viewModelScope.launch {
                                _uiState.value =
                                    _uiState.value.copy(failureMsg = R.string.scrambled_failure_for_timeout)
                                onTestFinish()
                                stopTimers()

                            }
                        }
                    }
                }
            }
        stopWatchTimer.start()

    }


    private lateinit var previewHintCountDownTimer: CountDownTimer

    fun showPreviewHint() {
        Log.d(TAG, "showPreview: ")

        if (_uiState.value.helperButtonState.imageHintCount == 0) {
            //play can't do it sound
            playSound(SoundType.Unsuccessful)
            return
        }
        if (_uiState.value.previewHintShown) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val user = repository.getUser()
                if (user.balance < SHOW_IMAGE_HINT_COST) {
                    _uiState.value = _uiState.value.copy(toast = R.string.not_enough_balance)
                    playSound(SoundType.Unsuccessful)

                    return@withContext
                } else {

                    repository.deductFromBalance(user = user, amount = SHOW_IMAGE_HINT_COST)

                    showImageHint(true)

                }
            }
        }


    }

    private suspend fun showImageHint(decrease: Boolean) {
        _uiState.value = _uiState.value.copy(previewHintShown = true)
        val buttonState =
            _uiState.value.helperButtonState.copy(
                imageHintButt = false,
                numberHintButt = false
            )
        _uiState.value = _uiState.value.copy(helperButtonState = buttonState)

        withContext(Dispatchers.Main) {


            previewHintCountDownTimer =
                object : CountDownTimer(PREVIEW_HINT_SHOW_TIME_INTERVAL, 1000) {
                    override fun onTick(millisUntilFinished: Long) {

                        previewTimerState.value = (millisUntilFinished / 1000).toInt()

                    }

                    override fun onFinish() {

                        _uiState.value = _uiState.value.copy(previewHintShown = false)

                        val hintCount = if (decrease) {
                            _uiState.value.helperButtonState.imageHintCount - 1
                        } else {
                            _uiState.value.helperButtonState.imageHintCount
                        }


                        val buttonState1 = _uiState.value.helperButtonState.copy(
                            imageHintButt = true,
                            imageHintCount = hintCount,
                            numberHintButt = true
                        )
                        _uiState.value =
                            _uiState.value.copy(helperButtonState = buttonState1)

                    }

                }


            previewHintCountDownTimer.start()

        }

        playSound(SoundType.ClockTicking)
    }

    private lateinit var hintTimer: CountDownTimer
    fun showNumberHint() {
        Log.d(TAG, "showNumberHint: ")

        if (_uiState.value.helperButtonState.numberHintCount == 0) {
            //play can't do it sound
            playSound(SoundType.Unsuccessful)
            return
        }
        if (puzzleEngine.numberHintShown) return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val user = repository.getUser()
                if (user.balance < SHOW_NUMBER_HINT_COST) {
                    _uiState.value = _uiState.value.copy(toast = R.string.not_enough_balance_)
                    playSound(SoundType.Unsuccessful)

                    return@withContext
                } else {

                    repository.deductFromBalance(user = user, amount = SHOW_NUMBER_HINT_COST)

                    showNumbers(true)

                }
            }
        }
    }

    private suspend fun showNumbers(decrease: Boolean) {
        puzzleEngine.showNumberHint(true)
        tileListState.value = puzzleEngine.getTileList()
        val buttonState =
            _uiState.value.helperButtonState.copy(
                imageHintButt = false,
                numberHintButt = false
            )
        _uiState.value = _uiState.value.copy(helperButtonState = buttonState)

        withContext(Dispatchers.Main) {
            hintTimer = object :
                CountDownTimer(
                    SHOW_NUMBER_HINT_TIME_INTERVAL,
                    SHOW_NUMBER_HINT_TIME_INTERVAL
                ) {
                override fun onTick(millisUntilFinished: Long) {

                }

                override fun onFinish() {
                    puzzleEngine.showNumberHint(false)
                    tileListState.value = puzzleEngine.getTileList()

                    val hintCount = if (decrease) {
                        _uiState.value.helperButtonState.numberHintCount - 1
                    } else {
                        _uiState.value.helperButtonState.numberHintCount
                    }
                    val buttonState1 = _uiState.value.helperButtonState.copy(
                        imageHintButt = true,
                        numberHintButt = true,
                        numberHintCount = hintCount
                    )
                    _uiState.value = _uiState.value.copy(helperButtonState = buttonState1)

                }
            }

            hintTimer.start()
        }
        playSound(SoundType.ClockTicking)
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun playSound(soundType: SoundType) {

        if (_uiState.value.soundOn) {
            mediaPlayer =
                MediaPlayer.create(applicationContext, SoundUtil.getResourceId(sound = soundType))
            mediaPlayer?.setOnCompletionListener {
                it?.reset()
                it?.release()
            }
            mediaPlayer?.start()
        }
    }

    override fun onCleared() {
        super.onCleared()

        if (this::hintTimer.isInitialized) {
            hintTimer.cancel()
        }
        if (this::stopWatchTimer.isInitialized) {
            stopWatchTimer.stop()
        }
    }

    fun onShowAd() {

        showingAdPurpose = ShowAdFor.HELP
        stopTimer()
        showAd()

    }

    fun showAd() {

        _uiState.value = _uiState.value.copy(showAd = true)
        adState.value = ActionDelegator.SHOW

    }

    //enum class just to send actions to activity through flowState
    enum class ActionDelegator {
        LOAD, SHOW
    }

    fun onRewardEarned() {

        rewardEarned = true
    }

    fun onRewardAdError(error: String) {

        if (uiState.value.showAd) {
            _uiState.value = _uiState.value.copy(showError = true, errorMsg = error)
        }

        //wait little before loading another add
        viewModelScope.launch {
            delay(3000)
            adState.value = ActionDelegator.LOAD

        }

    }

    fun onDismissRewardError() {

        if (uiState.value.showAd) {
            _uiState.value = _uiState.value.copy(showAd = false)
        }

        _uiState.value = _uiState.value.copy(showError = false, errorMsg = "")
        resumeTimer()
        rewardEarned = false

    }

    private fun resumeTimer() {
        stopWatchTimer.resume(uiState.value.timer)

    }

    private fun stopTimer() {
        stopWatchTimer.stop()
    }

    fun onDismissRewardAd() {


        _uiState.value = _uiState.value.copy(showAd = false)
        if (rewardEarned) {

            when (showingAdPurpose) {
                ShowAdFor.HELP -> {
                    if (!_uiState.value.testFinished) {
                        showHint()
                        resumeTimer()

                        playSound(SoundType.ChangeQuestion)
                    }
                }

                ShowAdFor.DOUBLING_SCORE -> {
                    doubleScore()
                    playSound(SoundType.ChangeQuestion)

                }

                ShowAdFor.FAILURE -> doNothing()
            }


        }
        adState.value = ActionDelegator.LOAD
        rewardEarned = false
    }

    private fun doNothing() {

    }

    private fun doubleScore() {

        viewModelScope.launch {
            withContext(Dispatchers.IO) {

                Log.d(TAG, "doubleScore: ${uiState.value.finalResult.bonusMoney}")
                repository.updateUserBalance(
                    (uiState.value.finalResult.bonusMoney).toFloat(),
                    Constants.USER_RECORD_ID
                )

            }
        }
        _uiState.value = _uiState.value.copy(doubleScoreShown = true)

    }

    private fun showHint() {
        viewModelScope.launch {

            if (Random.nextBoolean()) {

                showNumbers(false)
            } else {

                showImageHint(false)

            }
        }
    }

    fun onAdShowing() {

        Log.d(TAG, "onAdShowing: ")
    }

    fun onBackPressed() {

        Log.d(TAG, "onBackPressed: ")
        if (uiState.value.showAd) {
            _uiState.value = _uiState.value.copy(showAd = false)
            return
        }
        _uiState.value = _uiState.value.copy(backHandlerEnabled = false)

    }

    fun onSoundOn() {

        _uiState.value = _uiState.value.copy(soundOn = !_uiState.value.soundOn)

        sharedPre.edit().putBoolean(Constants.SOUND_ON_KEY, _uiState.value.soundOn).apply()
    }

    fun onDoubleScore() {

        showingAdPurpose = ShowAdFor.DOUBLING_SCORE
        showAd()
    }

    fun loadingLevel() {
        _uiState.value = _uiState.value.copy(loadingLevel = true)
    }

    fun levelReady() {

        _uiState.value = _uiState.value.copy(loadingLevel = false)

    }

    enum class ShowAdFor {
        DOUBLING_SCORE, FAILURE, HELP
    }
}


class PuzzleViewModelFactory(val repository: ScrambledRepository, val application: Application) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(SlidingPuzzleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SlidingPuzzleViewModel(repository = repository, application = application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}

@Keep
data class PuzzleUIState(
    val tileList: List<PuzzleTile> = mutableListOf(),
    val timer: Int = 0,
    val puzzleDimen: Int = 0,
    val isSolved: Boolean = false,
    val testFinished: Boolean = false,
    val startDialog: Boolean = true,
    var startDialogText: Int = R.string.easy_difficulty_start_dialog_text,            //resId of string that displayed in start dialog
    var moves: Int = 0,
    var failureMsg: Int = 0,
    var previewHintShown: Boolean = false,
    var previewHintImageResourceId: Int = 0,
    var previewHintImageFromFile: String = "",
    var previewTimer: Int = 0,
    var showBlankTile: Boolean = false,
    val helperButtonState: HelperButtonState = HelperButtonState(),
    val level: Int = 1,
    val finalResult: SlidingPuzzleResult = SlidingPuzzleResult(),
    var showResult: Boolean = false,
    var showAd: Boolean = false,
    var showError: Boolean = false,
    var errorMsg: String = "",
    var backHandlerEnabled: Boolean = true,
    var soundOn: Boolean = true,
    var doubleScoreShown: Boolean = false,
    var loadingLevel: Boolean = false,
    var timeToSolve: Int = 0,
    var minimumMoves: Int = 0,
    var toast: Int = 0,
    var blockUI:Boolean = false

)

@Keep
data class SlidingPuzzleResult(
    var wonTrophy: Boolean = false,
    var timeSpent: Int = 0,
    var moves: Int = 0,
    var stars: Int = 0,
    var passed: Boolean = false,
    var score: Int = 0,
    var newTimeToSolve: Int = 0,
    var levelId: Int = 0,
    var bonusMoney: Int = 0,
    var previewUrl: String = ""      //This is used for sharing purposes

)

@Keep
data class HelperButtonState(
    var numberHintButt: Boolean = true,
    var imageHintButt: Boolean = true,
    var numberHintCount: Int = 0,
    var imageHintCount: Int = 0
)
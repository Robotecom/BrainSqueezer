package com.puzzlemind.brainsqueezer

import android.util.Log
import androidx.work.ListenableWorker
import com.puzzlemind.brainsqueezer.data.*
import kotlinx.coroutines.flow.Flow

class AppRepository(val roomDb: PuzzleRoomDatabase) {


    private val puzzleDao: PuzzleDao = roomDb.puzzleDao()
    private val levelDao: LevelDao = roomDb.levelDao()
    val userDao: UserDao = roomDb.UserDao()
    private val diffDao: DifficultyDao = roomDb.difficultyDao()
    private val leaderboardItemDao: LeaderboardItemDao = roomDb.leaderboardDao()


    fun fetchQuestion(id: Int): Question {
        return puzzleDao.fetchQuestion(id)
    }

    suspend fun updateQuestionAnswered(id: Int, correct: Answered) {
        puzzleDao.updateQuestionAnswered(id, correct)
    }

    fun fetchQuestionList(from: Int, range: Int): MutableList<Question> {
        return puzzleDao.fetchQuestionList(from, range)
    }

    suspend fun updateLevelProgress(levelProgress: Level) {
        println("saveUserProgress: repository")
        this.levelDao.insertLevel(level = levelProgress)
    }

    fun getQuestionCount(): Int {
        return puzzleDao.fetchRecordCount()
    }

    fun getLevelData(level: Int, game: Game): Level {

        return levelDao.fetchLevel(level = level, game = game)
    }

    val TAG = "AppRepository"
    fun getDifficultyLevels(diff: Difficulty): Flow<MutableList<Level>> {

        Log.d(TAG, "getDifficultyLevels: $diff")
        return levelDao.fetchLevelForDifficulty(diff)

    }

    suspend fun updateProgress(level: Level) {
        this.levelDao.updateProgress(level = level)
    }

    fun openNextLevel(nextLevel: Int) {

        this.levelDao.openNextLevel(nextLevel, true)
    }

    fun getUser(id: Int): User {
        return this.userDao.getUser(id)
    }

    fun updateUserBalance(balance: Float, id: Int) {

        val user = getUser(id)
        this.userDao.updateBalance(balance = user.balance + balance, id = id)
    }

    fun deductFromBalance(user: User, deduction: Int) {

        val newBalance = user.balance - deduction
        this.userDao.updateBalance(balance = newBalance, user.id)
    }

    fun getLiveUser(id: Int): Flow<User> {
        return this.userDao.getLiveUser(id)
    }

    fun getDifficultyLive(game: Game): Flow<MutableList<DifficultyLevel>> {
        return this.diffDao.getDifficultyLive(game = game)
    }

    fun openNextDifficulty(level: Int, game: Game) {
        val nextLevel = this.levelDao.fetchLevel(level = level, game = game)

        nextLevel?.let {

            this.diffDao.openDifficulty(nextLevel.diffIndex, true)
        }
    }

    fun updateDifficultyTrophyProgress(
        diff: Difficulty,
        game: Game,
        trophies: Int,
        progress: Float
    ) {
        this.diffDao.updateTrophyProgress(diff, game, trophies, progress)
    }

    fun getScheduledScores(): MutableList<Level> {

        return this.levelDao.fetchScheduledLevels(true)
    }

    private fun updateScheduledLevel(level: Int) {
        Log.d(TAG, "updateScheduledLevel: $level")
        this.levelDao.updateScheduledLevel(level, scheduled = false)
    }

    fun updateScheduledScores(scheduledScoresList: MutableList<Level>): ListenableWorker.Result {
        for (level in scheduledScoresList) {
            updateScheduledLevel(level = level.level)
        }

        return ListenableWorker.Result.success()
    }


    fun getLeaderboardListLive(): Flow<MutableList<LeaderboardItem>> {
        return leaderboardItemDao.getLeaderboardListLive()
    }

    fun updateUserSkills(skills: Float) {
        val scrambled = roomDb.ScrambledDashboardDao().getDashboard(1)

        val skillfullness = (scrambled.matchWon/300f + skills)/2f
        Log.d(TAG, "updateUserSkills: ProgressFromMCQ:${skills}, ProgressFromScrambled:${scrambled.matchWon/300f} skillfulness:${skillfullness}")

        this.userDao.updateProgress(skillfullness,0)
    }

    fun updateUserUidnProfile(name: String, profile: String) {

        this.userDao.updateNameProfile(name,profile,0)
    }

    fun getSumOfHighscores():Int {
        return this.levelDao.getSumOfLevelHighscore()
    }

    fun getSumOfStars(): Int {
        return this.levelDao.getSumOfStars()
    }

    fun getSumOfTrophies(): Int {
        return this.levelDao.getSumOfTrophies()
    }

    fun updateMCQDashboard(
        points: Int,
        stars: Int,
        trophies: Int,
        maxLevel: Int,
        progress: Float,
        levels: Int
    ) {
        this.diffDao.updateMCQDashboard(points = points,
            stars = stars,
            trophies = trophies,
            id = 1,
            maxLevel = maxLevel,
            progress = progress,
            levels = levels
            )
    }

    fun getMCQDashboardLive():Flow<MCQDashboard>{
        return this.diffDao.getMCQDashboardLive(id = 1)
    }

    fun getMCQMaxLevelReached(): Int {

        return this.levelDao.getLastLevelReached(true)
    }

    fun getlevelsCount(): Int {
        return this.levelDao.getlevelCount()
    }

}
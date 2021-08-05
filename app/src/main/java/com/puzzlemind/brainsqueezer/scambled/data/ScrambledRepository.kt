package com.puzzlemind.brainsqueezer.scambled.data

import android.content.Context
import android.util.Log
import com.puzzlemind.brainsqueezer.data.*
import com.puzzlemind.brainsqueezer.scambled.ScrambledDifficulty
import com.puzzlemind.brainsqueezer.scambled.SlidingPuzzleResult
import com.puzzlemind.brainsqueezer.utils.deleteDirectory
import kotlinx.coroutines.flow.Flow
import java.io.File


class ScrambledRepository(val database: PuzzleRoomDatabase, val context: Context) {


    val TAG = "ScrambledRepository"
    fun getScrambledDifficulties(): Flow<MutableList<DifficultyLevel>> {

        return database.difficultyDao().getDifficultyLive(Game.SCRAMBLED)
    }

    fun getLevel(level: Int, difficulty: Int): ScrambledLevel {
        return database.scrambledDifficultyLevelDao().getLevel(level, difficulty)
    }

    fun getLevels(difficulty: ScrambledDifficulty): Flow<MutableList<ScrambledLevel>> {

        return database.scrambledDifficultyLevelDao().getLiveLevels(difficulty.index)
    }


    fun updateDashboard(points:Int,skillfulness:Float){
        database.ScrambledDashboardDao().updateDashboard(points = points,skillfulness = skillfulness,id = 1)
    }



    fun saveProgress(result: SlidingPuzzleResult) {

        database.scrambledDifficultyLevelDao().updateLevel(stars= result.stars,
            trophy = result.wonTrophy,
            highScore = result.score,
            id = result.levelId,
            isPassed = result.passed,
            scheduled = true,
            )
    }

    private fun getLevelById(levelId: Int): ScrambledLevel {
        return database.scrambledDifficultyLevelDao().getLevelById(levelId)
    }


    fun openNextLevel(nextLevel: Int, difficulty: Int) {
        database.scrambledDifficultyLevelDao().openNextLevel(
            nextLevel = nextLevel,
            difficulty = difficulty,
            isOpen = true
            )
    }

    fun getUser(): User {
        return database.UserDao().getUser(id = 0)
    }

    fun updateUserBalance(balance: Float, id: Int) {

        val user = getUser()
        database.UserDao().updateBalance(balance = user.balance + balance, id = id)
    }

    fun getScrambledDashboardLive(): Flow<ScrambledDashboard> {
        return database.ScrambledDashboardDao().getDashboardLive(1)
    }

    fun getScrambledDashboard(): ScrambledDashboard {
        return database.ScrambledDashboardDao().getDashboard(1)
    }

    suspend fun insertLevel(scrambledLeve: ScrambledLevel) {

        database.scrambledDifficultyLevelDao().insertLevel( scrambledLeve)
    }

    fun addMoneyToUserBalance(moneyToAdd: Int) {

        database.UserDao().updateBalance(moneyToAdd.toFloat(),id = 1)
    }

    fun getPointSumForPassedLevels():Flow<Int> {
        return database.scrambledDifficultyLevelDao().getHighscoreSumForPassed(isPassed = true)
    }

    fun updateScrambledDashboardPoints() {
        database.ScrambledDashboardDao().updateDashboardSum(id = 1)
    }

    fun updateDashboardProgressForDiff(difficulty: Int){
        when(difficulty){
            ScrambledDifficulty.Easy.index -> database.ScrambledDashboardDao().updateDashboardProgressForEasy(id = 1)

            ScrambledDifficulty.Medium.index -> database.ScrambledDashboardDao().updateDashboardProgressForMedium(id = 1)

            else -> database.ScrambledDashboardDao().updateDashboardProgressForHard(id = 1)
        }

    }

    fun getScheduledScores(): MutableList<ScrambledLevel> {
        return database.scrambledDifficultyLevelDao().getScheduledLevels(true)
    }

    fun updateScheduledScores() {

        database.scrambledDifficultyLevelDao().updateScheduledScores(scheduled = false,isScheduled = true)
    }

    fun updateUserSkillfulness(slidingPuzzleProgress: Float) {
        val mcqDashboard = database.difficultyDao().getMCQDashboard(1)

        Log.d(TAG, "updateUserSkillfulness: MCQ:${mcqDashboard.progress} from Sliding:${slidingPuzzleProgress} average:${(mcqDashboard.progress + slidingPuzzleProgress)/2f}")
        database.UserDao().updateProgress((mcqDashboard.progress + slidingPuzzleProgress)/2f,0)
    }

    fun deductFromBalance(user: User,amount: Int) {

        database.UserDao().updateBalance(user.balance - amount,user.id)
    }

    suspend fun deleteScrambledLevel(level: ScrambledLevel) {

        database.scrambledDifficultyLevelDao().deleteLevel(level.id)

        val fileDir = File(context.filesDir,level.fid)

        fileDir.deleteDirectory()


    }


}
package com.puzzlemind.brainsqueezer.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledDashboard
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.parcelize.Parcelize
import java.lang.StringBuilder

// TODO: 13-Jun-21 annotate data classes with @Keep to prevent proguard from deleting them

@Keep
@Entity(tableName = "user_table")
data class User(
    @PrimaryKey var id: Int = 0,
    var name: String = "",
    var balance:Float = 0f,
    var uid: String = "",
    var trophies:Int = 0,
    var profile: String = "",
    var stars:Int = 0,
    var points:Int = 0,
    @Ignore val timestamp: Timestamp = Timestamp.now(),
    @Ignore var maxLevel: MutableMap<String,Int> = mutableMapOf(),
    var skills: Float = 0f,
                )
@Keep
@Entity(tableName = "difficulty_table")
data class DifficultyLevel(
    @PrimaryKey val id: Int = 0,
    val name: String = "",
    var index:Int = 1,
    var levels:Int = 0,     //how many level are their in a single difficultyLevel
    var trophies:Int = 0,
    var progress:Float = 0f,
    var isOpen:Boolean = false,
    var game: Game = Game.NO_GAME,
    var difficulty: String = "",
    var diffIndex:Difficulty = Difficulty.BEGINNER
)

@Keep
@Entity(tableName = "question_table")
data class Question(@PrimaryKey(autoGenerate = true) var id:Int = 0,
                    var question: String = "",
                    var answer: String = "",
                    var hint: String = "",
                    var incorrectChoices:MutableList<String> = mutableListOf(),
                    var difficulty: String = "",
                    var diffIndex: Difficulty = Difficulty.BEGINNER,
                    var category: String = "",
                    var answered:Answered = Answered.NOT_ANSWERED,
                    @Ignore var choices:MutableList<String> = mutableListOf()
)

@Keep
@Entity(tableName = "pic_name_table")
data class PicName(@PrimaryKey(autoGenerate = true) var id:Int =0,
                   var picNameMap:MutableMap<String,Int> = mutableMapOf(),
                   var hint: String = "",

                   )
@Keep
@Entity(tableName = "level_table")
data class Level(@PrimaryKey(autoGenerate = true) var id:Int = 0,
                 var level:Int = 0,
                 var game:Game = Game.NO_GAME,
                 var score:Float = 0f,
                 var stars:Int = 0,
                 var trophy:Boolean = false,
                 var isOpen:Boolean = false,
                 var isPassed:Boolean = false,
                 var difficulty: String = "",
                 var diffIndex:Difficulty = Difficulty.BEGINNER,
                 var isFinal:Boolean = false,        //this is used to check if level is the final one in its difficulty
                 var scheduled:Boolean = false      //scheduled levels are pending writes that need to be sent to firestore leaderboard
){
    fun getMap(game: Game): MutableMap<String, Any> {
        val map = mutableMapOf<String,Any>()

        map["game"] = GameConverter.getIntForGame(game)
        map["score"] = score
        map["level"] = level
        map["stars"] = stars
        map["trophy"] = trophy
        map["timestamp"] = FieldValue.serverTimestamp()

        return map
    }
}

@Keep
@Entity(tableName = "mcq_dashboard_table")
data class MCQDashboard(
    @PrimaryKey(autoGenerate = true) val id:Int = 0,
    var progress:Float = 0f,
    var points:Int = 0,
    var maxLevel:Int = 0,
    var trophies:Int = 0,
    var stars:Int = 0,
    var levels: Int = 0         //count of levels there
)

@Keep
@Parcelize
@Entity(tableName = "leaderboard_table")
data class LeaderboardItem(
    @PrimaryKey
    val id: Int = 0,
    val uid: String = "",
    var rank: Int = 0,
    val gender: String = "Male",
    val name: String = "",
    val profile: String = "",
    val points: Int = 0,
    val trophies: Int = 0,
    val maxLevel:MutableMap<String,Int> = mutableMapOf()
) : Parcelable {

    companion object{
        const val MAX_LEVEL_MCQ = "maxLevelMCQ"
        const val TROPHIES = "trophies"
        const val POINTS = "points"
    }


}





class PicNameConverter{

    @TypeConverter
    fun mapToString(map:MutableMap<String,Int>): String {
        return map.toString()
    }

    @TypeConverter
    fun stringToMap(str: String):MutableMap<String,Int>{
        return mutableMapOf()
    }
}

class GameConverter{

    companion object{
        fun getIntForGame(game: Game): Int {
            return when(game){
                Game.MCQ -> 1
                Game.SCRAMBLED -> 2
                Game.HANOI -> 3
                Game.MEMORY_CARD -> 4
                else -> 0
            }
        }
    }

    @TypeConverter
    fun gameToInt(game:Game):Int{
        return when(game){
            Game.MCQ -> 1
            Game.SCRAMBLED -> 2
            Game.HANOI -> 3
            Game.MEMORY_CARD -> 4
            else -> 0
        }
    }

    @TypeConverter
    fun intToGame(gameInt:Int):Game{
        return when(gameInt){
            1  -> Game.MCQ
            2  -> Game.SCRAMBLED
            3  -> Game.HANOI
            4  -> Game.MEMORY_CARD
            else -> Game.NO_GAME
        }
    }
}


enum class Game{
    NO_GAME, MCQ, SCRAMBLED, HANOI, MEMORY_CARD
}


class ChoicesConverter {

    @TypeConverter
    fun fromStringToList(from: String): MutableList<String> {

        val list = mutableListOf<String>()
        for (item in from.removePrefix("[").removeSuffix("]").split("--")){
            list.add(item.removePrefix("\"").removeSuffix("\"").trim())
        }
        return list
    }

    @TypeConverter
    fun fromListToString(incorrectChoices: MutableList<String>): String {

        val stringBuilder = StringBuilder().append("[")
        incorrectChoices.forEachIndexed{index: Int, s: String ->
            stringBuilder.append(s)
            if (incorrectChoices.lastIndex != index)
                stringBuilder.append(" -- ")
        }
        stringBuilder.append("]")

        return stringBuilder.toString()
    }
}

enum class Answered {
    CORRECT,INCORRECT,NOT_ANSWERED
}

class QuestionAnswered{
    @TypeConverter
    fun fromIntToEnum(integer:Int):Answered{
        return when(integer){
            0 -> Answered.NOT_ANSWERED
            1 -> Answered.CORRECT
            else -> Answered.INCORRECT
        }
    }
    @TypeConverter
    fun fromEnumToInt(enum:Answered):Int{
        return when(enum){
            Answered.NOT_ANSWERED -> 0
            Answered.CORRECT -> 1
            else -> 2
        }
    }
}

class DifficultyConverter{

    companion object {
        fun fromIntToEnum(diffIndex:Int?): Difficulty {

            return when(diffIndex){
                1 -> Difficulty.BEGINNER
                2 -> Difficulty.BASIC
                3 -> Difficulty.EASY
                4 -> Difficulty.NORMAL
                5 -> Difficulty.HARD
                6 -> Difficulty.VERY_HARD
                7 -> Difficulty.IMPOSSIBLE
                8 -> Difficulty.UNLIKELY
                else -> Difficulty.UNLIKELY
            }

        }

        fun fromEnumToInt(diffIndex: Difficulty): Int {
            return when(diffIndex){
                Difficulty.BEGINNER -> 1
                Difficulty.BASIC -> 2
                Difficulty.EASY -> 3
                Difficulty.NORMAL -> 4
                Difficulty.HARD -> 5
                Difficulty.VERY_HARD -> 6
                Difficulty.IMPOSSIBLE -> 7
                Difficulty.UNLIKELY -> 8
                else -> 8
            }
        }
    }
    @TypeConverter
    fun fromIntToEnum(difficulty:Int?): Difficulty {

        return when(difficulty){
            1 -> Difficulty.BEGINNER
            2 -> Difficulty.BASIC
            3 -> Difficulty.EASY
            4 -> Difficulty.NORMAL
            5 -> Difficulty.HARD
            6 -> Difficulty.VERY_HARD
            7 -> Difficulty.IMPOSSIBLE
            8 -> Difficulty.UNLIKELY
            else -> Difficulty.UNLIKELY

        }

    }

    @TypeConverter
    fun fromEnumDifficultyToInt(enumString: Difficulty):Int{

        return when(enumString){
            Difficulty.BEGINNER -> 1
            Difficulty.BASIC -> 2
            Difficulty.EASY -> 3
            Difficulty.NORMAL -> 4
            Difficulty.HARD -> 5
            Difficulty.VERY_HARD -> 6
            Difficulty.IMPOSSIBLE -> 7
            Difficulty.UNLIKELY -> 8
            else -> 8
        }
    }
}



@Dao
interface PuzzleDao {
    @Insert
    suspend fun insertQuestion(question: Question)

    @Insert
    suspend fun insertQuestions(questions: List<Question>)

    @Transaction
    suspend fun insertAllQuestions(questionList: List<Question>){

        insertQuestions(questionList)

    }
    @Query("DELETE FROM question_table")
    fun deleteAllQuestions()

    @Query("UPDATE question_table SET answered = :correct WHERE id = :id")
    suspend fun updateQuestionAnswered(id: Int,correct: Answered)


    @Query("SELECT * FROM question_table")
    fun fetchAllQuestions(): List<Question>

    @Query("SELECT * FROM question_table WHERE id = :id")
    fun fetchQuestion(id:Int):Question

    @Query("SELECT * FROM question_table WHERE diffIndex = :diffIndex")
    fun fetchQuestionWithDifficulty(diffIndex: Difficulty):Flow<Question>

    @Query("SELECT * FROM question_table LIMIT :from,:range")
    fun fetchQuestionList(from: Int, range: Int):MutableList<Question>

    @Query("SELECT COUNT(id) FROM question_table")
    fun fetchRecordCount():Int

    @Query("SELECT * FROM question_table WHERE   id = (SELECT MAX(id)  FROM question_table);")
    fun getLastRecord():Question

    @Query("SELECT * FROM question_table WHERE id > :lastIndex ORDER BY id")
    fun getRecordAfter(lastIndex: Int): List<Question>

    @Query("SELECT COUNT(id) FROM question_table")
    fun getCount(): Int

    @Query("SELECT DISTINCT difficulty FROM question_table ORDER BY diffIndex")
    fun getDiffLevel(): List<String>

    @Query("SELECT COUNT(id) FROM question_table WHERE difficulty = :difficultyName")
    fun getRecordCountWith(difficultyName: String):Int

    @Query("SELECT DISTINCT difficulty FROM question_table WHERE id > :lastIndex ORDER BY diffIndex ")
    fun getDiffLevelAfter(lastIndex: Int):List<String>

    @Query("SELECT diffIndex FROM question_table WHERE difficulty = :difficulty LIMIT 1")
    fun getDiffIndexOf(difficulty: String): Int

    @Query("SELECT COUNT(id) FROM question_table WHERE difficulty = :difficultyName AND id > :lastIndex")
    fun getRecordCountWithAfter(difficultyName: String, lastIndex: Int): Int
}

@Dao
interface PicNamePuzzleDao{

    @Insert
    suspend fun insertPicName(vararg picName: PicName)

    @Query("SELECT * FROM pic_name_table")
    fun fetchAll(): List<PicName>

    @Query("SELECT * FROM pic_name_table WHERE id = :id")
    fun fetchOne(id: Int): PicName
}

@Dao
interface LevelDao{

    @Insert
    suspend fun insertLevel(level: Level)


    @Transaction
    @Insert
    suspend fun insertLevels(levels: MutableList<Level>)

    @Update
    suspend fun updateProgress(level: Level)

    @Query("SELECT * FROM level_table WHERE game = :game ORDER BY level")
    fun fetchLevels(game: Game):Flow<Level>

    @Query("SELECT * FROM level_table WHERE diffIndex = :diffIndex")
    fun fetchLevelForDifficulty(diffIndex: Difficulty):Flow<MutableList<Level>>

    @Query("SELECT * FROM level_table WHERE level = :level AND game = :game")
    fun fetchLevel(level: Int, game: Game):Level

    @Query("UPDATE level_table SET isOpen = :isOpen WHERE level = :nextLevel")
    fun openNextLevel(nextLevel: Int,isOpen: Boolean)

    @Query("SELECT * FROM level_table WHERE scheduled = :scheduled")
    fun fetchScheduledLevels(scheduled: Boolean) :MutableList<Level>

    @Query("UPDATE level_table SET scheduled = :scheduled WHERE level = :level")
    fun updateScheduledLevel(level: Int,scheduled: Boolean)

    @Query("SELECT * FROM level_table WHERE   level = (SELECT MAX(level)  FROM level_table);")
    fun getLastLevel(): Level

    @Query("UPDATE level_table SET isFinal = :isFinal WHERE  level = (SELECT MAX(level)  FROM level_table);")
    fun updateLastLevelisFinal(isFinal: Boolean)

    @Query("SELECT SUM(score) FROM level_table")
    fun getSumOfLevelHighscore(): Int

    @Query("SELECT SUM(stars) FROM level_table")
    fun getSumOfStars():Int

    @Query("SELECT SUM(trophy) FROM level_table")
    fun getSumOfTrophies():Int

    @Query("SELECT MAX(level)  FROM level_table WHERE isPassed = :isPassed")
    fun getLastLevelReached(isPassed: Boolean): Int

    @Query("SELECT COUNT(id) FROM level_table")
    fun getlevelCount(): Int

}



@Dao
interface UserDao{

    @Insert
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE user_table SET balance = :balance WHERE id = :id")
    fun updateBalance(balance:Float,id: Int)

    @Query("SELECT * FROM user_table WHERE id = :id")
    fun getUser(id: Int):User

    @Query("SELECT * FROM user_table WHERE id = :id")
     fun getLiveUser(id: Int): Flow<User>

     @Query("UPDATE user_table SET skills = :skills WHERE id = :id")
    fun updateProgress(skills: Float,id: Int)

    @Query("UPDATE user_table SET name = :name,profile = :profile WHERE id = :id")
    fun updateNameProfile(name: String, profile: String,id: Int)


}

@Dao
interface DifficultyDao{
    @Insert
    suspend fun insertDifficulty(difficulty: DifficultyLevel)

    @Insert
    suspend fun insertDifficulties(difficulty: MutableList<DifficultyLevel>)

    @Update
    suspend fun updateDifficulty(difficultyLevel: DifficultyLevel)

    @Query("SELECT * FROM difficulty_table WHERE game = :game")
    fun getDifficulty(game: Game):DifficultyLevel

    @Query("SELECT * FROM difficulty_table WHERE game = :game")
    fun getDifficultyLive(game: Game):Flow<MutableList<DifficultyLevel>>

    @Query("UPDATE difficulty_table SET isOpen = :b WHERE diffIndex = :diffIndex")
    fun openDifficulty(diffIndex: Difficulty, b: Boolean)

    @Query("UPDATE difficulty_table SET trophies = :trophies,progress = :progress WHERE diffIndex = :diff AND game = :game")
    fun updateTrophyProgress(diff: Difficulty, game: Game, trophies: Int, progress: Float)

    @Query("SELECT * FROM difficulty_table WHERE diffIndex = :newDiffIndex")
    fun getDifficultyWithDiff(newDiffIndex: Int): DifficultyLevel

    @Query("UPDATE difficulty_table SET levels = :difficultyLevel WHERE diffIndex = :diffIndex")
    fun updateDifficultyLevelCount(difficultyLevel: Int,diffIndex: Difficulty)

    @Query("SELECT * FROM difficulty_table WHERE difficulty = :diffName")
    fun getDifficultyWithName(diffName: String): DifficultyLevel

    //interfaces of mcqDashboard//////////////
    @Query("UPDATE mcq_dashboard_table SET points =:points, stars =:stars,trophies =:trophies,maxLevel =:maxLevel, progress =:progress, levels =:levels WHERE id =:id")
    fun updateMCQDashboard(
        points: Int,
        stars: Int,
        trophies: Int,
        id: Int,
        maxLevel: Int,
        progress: Float,
        levels: Int
    )

    @Insert
    fun insertMCQDashboard(mcqDashboard: MCQDashboard)

    @Query("SELECT * FROM mcq_dashboard_table WHERE id =:id")
    fun getMCQDashboardLive(id: Int):Flow<MCQDashboard>

    @Query("SELECT * FROM mcq_dashboard_table WHERE id =:id")
    fun getMCQDashboard(id: Int):MCQDashboard
}

@Dao
interface ScrambledDifficultyLevelDao{

    @Insert
    suspend fun insertLevels(difficulty: MutableList<ScrambledLevel>)

    @Query("SELECT * FROM scrambled_level_table WHERE level = :level AND difficulty = :difficulty" )
     fun getLevel(level: Int,difficulty: Int):ScrambledLevel

     @Query("SELECT * FROM scrambled_level_table WHERE difficulty = :difficulty ORDER BY level")
     fun getLiveLevels(difficulty: Int): Flow<MutableList<ScrambledLevel>>

     @Insert
     suspend fun insertLevel(scrambledLeve: ScrambledLevel)

     @Query("UPDATE scrambled_level_table SET stars = :stars, trophy = :trophy, highScore = :highScore,isPassed = :isPassed, scheduled = :scheduled WHERE id = :id")
    fun updateLevel(
         stars: Int,
         trophy: Boolean,
         highScore: Int,
         isPassed: Boolean,
         id: Int,
         scheduled: Boolean
     )

    @Query("UPDATE scrambled_level_table SET isOpen = :isOpen WHERE level = :nextLevel AND difficulty = :difficulty")
    fun openNextLevel(nextLevel: Int, difficulty: Int,isOpen: Boolean)

    @Query("SELECT * FROM scrambled_level_table WHERE id = :levelId")
    fun getLevelById(levelId: Int): ScrambledLevel

    @Query("SELECT SUM(highScore) FROM scrambled_level_table WHERE isPassed = :isPassed")
    fun getHighscoreSumForPassed(isPassed: Boolean): Flow<Int>

    @Query("SELECT * FROM scrambled_level_table WHERE scheduled = :b")
    fun getScheduledLevels(b: Boolean): MutableList<ScrambledLevel>

    @Query("UPDATE scrambled_level_table SET scheduled = :scheduled WHERE scheduled = :isScheduled")
    fun updateScheduledScores(scheduled: Boolean,isScheduled:Boolean)

    @Query("DELETE FROM scrambled_level_table WHERE id = :levelId")
    fun deleteLevel(levelId: Int)


}

@Dao
interface ScrambledDashboardDao{

    @Query("SELECT * FROM scrambled_dashboard_table WHERE id = :id")
    fun getDashboardLive(id: Int):Flow<ScrambledDashboard>

    @Insert
    fun insert(scrambledDashboard: ScrambledDashboard)

    @Query("UPDATE scrambled_dashboard_table SET skillfulness = :skillfulness WHERE id = :id")
    fun updateSkillfulness(skillfulness:Float,id: Int)

    @Query("UPDATE scrambled_dashboard_table SET points = :points,skillfulness = :skillfulness WHERE id = :id")
    fun updateDashboard(points: Int,skillfulness: Float,id: Int)

    @Query("SELECT * FROM scrambled_dashboard_table WHERE id = :id")
    fun getDashboard(id: Int): ScrambledDashboard

    @Query("UPDATE scrambled_dashboard_table SET points = (SELECT SUM(highScore) FROM scrambled_level_table),trophies = (SELECT SUM(trophy) FROM scrambled_level_table), stars = (SELECT SUM(stars) FROM scrambled_level_table), matchWon = (SELECT SUM(isPassed) FROM SCRAMBLED_LEVEL_TABLE) WHERE id =:id")
    fun updateDashboardSum(id: Int)

    @Query("UPDATE scrambled_dashboard_table SET easySkillful = (SELECT SUM(isPassed) FROM scrambled_level_table WHERE difficulty = 1) WHERE id =:id")
    fun updateDashboardProgressForEasy( id: Int)

    @Query("UPDATE scrambled_dashboard_table SET mediumSkillful = (SELECT SUM(isPassed) FROM scrambled_level_table WHERE difficulty = 2) WHERE id =:id")
    fun updateDashboardProgressForMedium( id: Int)

    @Query("UPDATE scrambled_dashboard_table SET hardSkillful = (SELECT SUM(isPassed) FROM scrambled_level_table WHERE difficulty = 3) WHERE id =:id")
    fun updateDashboardProgressForHard( id: Int)
}

@Dao
interface LeaderboardItemDao {
    @Query("SELECT * FROM leaderboard_table")
    fun getAll(): MutableList<LeaderboardItem>

    @Query("SELECT * FROM leaderboard_table WHERE uid IN (:leaderboardItemIds)")
    fun loadAllByIds(leaderboardItemIds: IntArray): List<LeaderboardItem>

    @Query("SELECT * FROM leaderboard_table")
    fun getLeaderboardListLive():Flow<MutableList<LeaderboardItem>>

    @Insert
    fun insertAll( leaderboardItems: MutableList<LeaderboardItem>)

    @Delete
    fun delete(leaderboardItem: LeaderboardItem)

}


enum class Difficulty{
    BEGINNER, BASIC, EASY, NORMAL, HARD, VERY_HARD, IMPOSSIBLE, UNLIKELY
}

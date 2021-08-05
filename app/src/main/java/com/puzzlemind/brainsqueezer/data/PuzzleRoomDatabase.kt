package com.puzzlemind.brainsqueezer.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.puzzlemind.brainsqueezer.Constants.LANGUAGE_AR
import com.puzzlemind.brainsqueezer.scambled.ScrambledDifficulty
import com.puzzlemind.brainsqueezer.scambled.data.ListConverter
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledDashboard
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


@TypeConverters(DifficultyConverter::class,ChoicesConverter::class,
    GameConverter::class,PicNameConverter::class,
    ListConverter::class,
    QuestionAnswered::class)
@Database(entities = [User::class,DifficultyLevel::class,
    ScrambledLevel::class, ScrambledDashboard::class,MCQDashboard::class,
    Question::class,PicName::class, Level::class,LeaderboardItem::class], version = 1, exportSchema = true)
abstract class PuzzleRoomDatabase : RoomDatabase() {

    abstract fun puzzleDao(): PuzzleDao
    abstract fun picNamePuzzleDao(): PicNamePuzzleDao
    abstract fun levelDao(): LevelDao
    abstract fun UserDao(): UserDao
    abstract fun difficultyDao(): DifficultyDao
    abstract fun leaderboardDao(): LeaderboardItemDao
    abstract fun scrambledDifficultyLevelDao(): ScrambledDifficultyLevelDao
    abstract fun ScrambledDashboardDao():ScrambledDashboardDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: PuzzleRoomDatabase? = null

        fun getDatabase(context: Context,scope: CoroutineScope): PuzzleRoomDatabase {

            val dbName: String = if (Locale.getDefault().isO3Language == LANGUAGE_AR){
                "db_ar"
            }else{
                "db_en"
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PuzzleRoomDatabase::class.java,
                    "puzzle_database"
                )
                    .createFromAsset("${dbName}.db")
                    .addCallback(RoomCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }


        fun getDatabase(context: Context): PuzzleRoomDatabase {

            val dbName: String = if (Locale.getDefault().isO3Language == LANGUAGE_AR){
                "db_ar"
            }else{
                "db_en"
            }
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PuzzleRoomDatabase::class.java,
                    "puzzle_database"
                )
                    .createFromAsset("${dbName}.db")
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    class RoomCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let {database ->

                scope.launch {

                    val levelList = mutableListOf<Level>()
                    val difficultyLevel = mutableListOf<DifficultyLevel>()

                    val diffLevelStr = database.puzzleDao().getDiffLevel()


                    diffLevelStr.forEachIndexed {index,difficultyName->
                        val count = database.puzzleDao().getRecordCountWith(difficultyName)
                        println("onCreateDB: diffs:${diffLevelStr}")

                        val diffLevel = DifficultyLevel(
                            id = index,
                            name = difficultyName,
                            index = index + 1,
                            levels = count/6,
                            isOpen = index == 0,
                            game = Game.MCQ,
                            diffIndex = DifficultyConverter.fromIntToEnum(index + 1),
                            difficulty = difficultyName,
                        )

                        difficultyLevel.add(diffLevel)
                    }

                    var startingLevel = 0
                    var endingLevel = 0
                    difficultyLevel.forEachIndexed { _, diffLvl->

                        endingLevel += diffLvl.levels
                        for (item in startingLevel until endingLevel){

                            //do not insert values for id just let auto increment do its job for you
                            levelList.add(Level(
                                level = item + 1,
                                game = Game.MCQ,
                                isOpen = item == 0,
                                diffIndex = diffLvl.diffIndex,
                                difficulty = diffLvl.difficulty,
                                isFinal = item == endingLevel - 1
                            ))

                        }
                        startingLevel = endingLevel

                    }

                    database.difficultyDao().insertMCQDashboard(
                        MCQDashboard()
                    )
                    database.difficultyDao().insertDifficulties(difficultyLevel)
                    database.levelDao().insertLevels(levelList)
                    database.UserDao().insertUser(User(
                        id = 0,
                        name = "User${Random().nextInt(1000)}"
                    ))

                    val scrambledLevels = mutableListOf<ScrambledLevel>()

                    //the zer layer i.e layer{level}_0 is the preview layer in resources
                    scrambledLevels.add(
                        ScrambledLevel(
                            level = 1,
                            preview = "d1_l1_preview.webp",
                            difficulty = ScrambledDifficulty.Easy.index,
                            puzzleSize = 3,
                            fileName = "d1_l1_layer1_0.webp",
                            downloadUrl = "",
                            defaultConfig = listOf(2,4,7,6,1,0,5,3,8),
                            tags = listOf("Scrambled", "game", "15puzzle"),
                            name = "Scrambled",
                            isResource = true,
                            timeToSolve = 50,
                            hintNumber = 0,
                            hintImage = 4,
                            minimumMovesCount = 50,
                            isOpen = true

                        ))


                    scrambledLevels.add(
                        ScrambledLevel(
                            level = 2,
                            preview = "d1_l2_preview.webp",
                            difficulty = ScrambledDifficulty.Easy.index,
                            puzzleSize = 3,
                            fileName = "d1_l2_layer1_0.webp",
                            downloadUrl = "",
                            defaultConfig = listOf(2,4,7,6,1,0,5,3,8),
                            tags = listOf("Eiffel", "Paris", "France", "Tower"),
                            name = "Eiffel Tower",
                            isResource = true,
                            timeToSolve = 50,
                            hintNumber = 4,
                            hintImage = 4,

                            minimumMovesCount = 50,
                            isOpen = true

                        ))

                    scrambledLevels.add(
                        ScrambledLevel(
                        level = 1,
                            preview = "d2_l1_preview.webp",
                            difficulty = ScrambledDifficulty.Medium.index,
                            puzzleSize = 4,
                            fileName = "d2_l1_layer2_0.webp",
                            downloadUrl = "",
                            defaultConfig = listOf(
                                2,4,7,6,1,
                                0,5,3,8,10,
                                13,9,14,12,15,11),
                            tags = listOf("Cat", "Meme", "Animal", "Pet"),
                            name = "Meme Cat",
                            isResource = true,
                            timeToSolve = 120,
                            hintNumber = 6,
                            hintImage = 6,
                            minimumMovesCount = 180,
                            isOpen = true


                        ))

                    scrambledLevels.add(
                        ScrambledLevel(
                            level = 1,
                            preview = "d3_l1_preview.webp",
                            difficulty = ScrambledDifficulty.Hard.index,
                            puzzleSize = 5,
                            fileName = "d3_l1_layer3_0.webp",
                            downloadUrl = "",
                            defaultConfig = listOf(
                                2, 4, 7, 6, 1,
                                0, 5, 3, 8, 10,
                                13,9,14,12,15,
                                11,24,16,23,17,
                                22,18,21,19,20),
                            tags = listOf("Parrot", "Bird", "Animal", "Pet"),
                            name = "Parrot",
                            isResource = true,
                            timeToSolve = 300,
                            hintNumber = 8,
                            hintImage = 8,
                            minimumMovesCount = 400,
                            isOpen = true
                        )
                    )



                    database.scrambledDifficultyLevelDao().insertLevels(scrambledLevels)

                    database.ScrambledDashboardDao().insert(ScrambledDashboard(
                        points = 0,
                        skillfulness = 0.0f,
                        matchLost = 0,
                        matchWon = 0
                    ))

                }

            }


        }

    }


}


@TypeConverters(DifficultyConverter::class,ChoicesConverter::class,
    GameConverter::class,PicNameConverter::class,
    QuestionAnswered::class)
@Database(entities = [Question::class,PicName::class], version = 1, exportSchema = true)
abstract class UpdatingRoomDatabase : RoomDatabase() {

    abstract fun puzzleDao(): PuzzleDao
    abstract fun picNamePuzzleDao(): PicNamePuzzleDao


    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: UpdatingRoomDatabase? = null

        fun getDatabase(context: Context,localFile: File): UpdatingRoomDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UpdatingRoomDatabase::class.java,
                    "updating_database${Random().nextInt(1000)}"
                )
                    .createFromFile(localFile)
                    .build()
                INSTANCE = instance

                instance
            }
        }
    }
}
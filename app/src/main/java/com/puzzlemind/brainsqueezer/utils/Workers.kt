package com.puzzlemind.brainsqueezer.utils

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.*
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import kotlinx.coroutines.tasks.await
import java.io.File

class SendScheduledScores(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    val TAG = "SendScheduledWorker"
    override suspend fun doWork(): Result {

        val scheduledScoresList =
            (applicationContext as PuzzleApp).repository.getScheduledScores()

        return if (!scheduledScoresList.isNullOrEmpty()) {

            sendData(scheduledScoresList)

            (applicationContext as PuzzleApp).repository.updateScheduledScores(scheduledScoresList)
            Result.success()

        } else {

            Result.success()
        }


    }

    private suspend fun sendData(scoresList: MutableList<Level>) {

        val firebaseUser = Firebase.auth.currentUser
        val db = Firebase.firestore
        val batch = db.batch()
        for (level in scoresList) {

            val docRef = db.document("${Constants.USERS}/${firebaseUser?.uid}/${Constants.MCQ_GAME}/level${level.id}")
            batch.set(docRef, level.getMap(Game.MCQ))
        }

        batch.commit().await()
    }


}

class SendScheduledSlidingPuzzleScores(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    val TAG = "SendScheduledSpWorker"
    override suspend fun doWork(): Result {

        val scheduledScoresList =
            (applicationContext as PuzzleApp).scrambledRepository.getScheduledScores()

        return if (!scheduledScoresList.isNullOrEmpty()) {

            Log.d(TAG, "doWork: sending sliding puzzle scores...")
            sendData(scheduledScoresList)

            (applicationContext as PuzzleApp).scrambledRepository.updateScheduledScores()
            Result.success()

        } else {

            Result.success()
        }


    }

    private suspend fun sendData(scoresList: MutableList<ScrambledLevel>) {

        val firebaseUser = Firebase.auth.currentUser
        val db = Firebase.firestore
        val batch = db.batch()

        for (level in scoresList) {

            val docRef = db.document("${Constants.USERS}/${firebaseUser?.uid}/${Constants.SLIDING_PUZZLE_GAME}/level${level.id}")
            batch.set(docRef, level.getMap())
        }
        Log.d(TAG, "sendData: scoreSize:${scoresList.size}")

        batch.commit().await()
    }


}



class DBFetcherWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext,
    params
){
    override suspend fun doWork(): Result {
        println("DBFetcher: started")

        val sharedPreferences = applicationContext.getSharedPreferences(applicationContext.getString(
            R.string.preference_file_key),Context.MODE_PRIVATE)

        val language = sharedPreferences.getString(Constants.LANGUAGE_CHOSEN,Constants.LANGUAGE_EN)

        val shouldUpdate = Firebase.firestore.document(Constants.GAME_UPDATES + "/"+ "update_${language}").get().await()

        val databaseUrl = shouldUpdate.data?.get("databaseUrl") as String
        if (databaseUrl?.isEmpty()){

            return Result.success()
        }


        val oldDBUrl = sharedPreferences.getString(Constants.NEW_DATABASE_URL,"")
        if (databaseUrl == oldDBUrl){

            return Result.success()
        }

        val dbRef = Firebase.storage.getReferenceFromUrl(databaseUrl)

        val localDbFile = File.createTempFile("mcq","db")

        val task = dbRef.getFile(localDbFile).await()

        if (task.task.isSuccessful) {
            //continue to create database from local file and populate the other database
            val database = UpdatingRoomDatabase.getDatabase(applicationContext, localDbFile)

            transferTables(database)
            localDbFile.delete()
            cleanUp(database)
            val shareEditor = sharedPreferences.edit()
            shareEditor.putString(Constants.NEW_DATABASE_URL,databaseUrl)
            shareEditor.apply()
            println("DBFetcher: successfully downloaded database to:${localDbFile.absolutePath}")

        }else{
            //nothing just shut down the worker
            println( "DBFetcher: doWork: failed to fetch the db file:${task.task.exception}")
        }

        return Result.success()
    }

    private fun cleanUp(database: UpdatingRoomDatabase) {
        database.clearAllTables()
        database.close()

        val dbPath = applicationContext.getDatabasePath(database.openHelper.databaseName).absolutePath
        println("DBFetcher: path $dbPath")
        if (!dbPath.isNullOrEmpty())return
        if (File(dbPath).delete()) {
            println("DBFetcher: updating Database deleted")
        }else{
            println("DBFetcher: could not delete updating Database")

        }
    }

    private suspend fun transferTables(updatingDB: UpdatingRoomDatabase) {

        val originalDB = PuzzleRoomDatabase.getDatabase(applicationContext)
        val lastRecord = originalDB.puzzleDao().getLastRecord()
        var lastIndex = 0

        lastRecord?.let {
            lastIndex = it.id

        }

        println("transferTable: lastIndex${lastIndex}")
        val listQuestion = updatingDB.puzzleDao().getRecordAfter(lastIndex)

        if (listQuestion.isNotEmpty()) {
            originalDB.puzzleDao().insertAllQuestions(listQuestion)
            val listOfDiffNames = originalDB.puzzleDao().getDiffLevelAfter(lastIndex = lastIndex)

            listOfDiffNames.forEach { difficultyName ->
                val count = originalDB.puzzleDao().getRecordCountWith(difficultyName = difficultyName)

                val newDiffIndex = originalDB.puzzleDao().getDiffIndexOf(difficultyName)
                println("count")

                val oldDiffIndex = originalDB.difficultyDao().getDifficultyWithDiff(newDiffIndex)
                val diffLevel: DifficultyLevel?
                if (oldDiffIndex?.index == newDiffIndex){

                    originalDB.difficultyDao().updateDifficultyLevelCount(difficultyLevel = count/6,diffIndex = DifficultyConverter.fromIntToEnum(newDiffIndex))
                    originalDB.levelDao().updateLastLevelisFinal(false)

                }else{
                    diffLevel = DifficultyLevel(
                        id = newDiffIndex,
                        name = difficultyName,
                        index = newDiffIndex ,
                        levels = count/6,
                        isOpen = newDiffIndex == 0,
                        game = Game.MCQ,
                        diffIndex = DifficultyConverter.fromIntToEnum(newDiffIndex),
                        difficulty = difficultyName
                    )
                    originalDB.difficultyDao().insertDifficulty(diffLevel)
                }

            }

            var startingLevel = originalDB.levelDao().getLastLevel().level + 1
            println("DBFetcher: starting Level${startingLevel}")
            var endingLevel = startingLevel
            val levelList = mutableListOf<Level>()
            listOfDiffNames.forEach { diffName->

                val diffLvl = originalDB.difficultyDao().getDifficultyWithName(diffName)
                val addedLevels = originalDB.puzzleDao().getRecordCountWithAfter(diffName,((startingLevel -1) * 6)-1)/6


                endingLevel +=  addedLevels
                for (item in startingLevel until endingLevel){

                    //do not insert values for id just let auto increment do its job for you
                    levelList.add(Level(
                        level = item ,
                        game = Game.MCQ,
                        isOpen = item == 0,
                        diffIndex = diffLvl.diffIndex,
                        difficulty = diffName,
                        isFinal = item == endingLevel - 1
                    ))

                }
                startingLevel = endingLevel

            }

            originalDB.levelDao().insertLevels(levelList)


        }else{
            cleanUp(database = updatingDB)
        }

    }


}

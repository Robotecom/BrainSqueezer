package com.puzzlemind.brainsqueezer

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.*
import com.puzzlemind.brainsqueezer.data.PuzzleRoomDatabase
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.FirebaseApp
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledRepository
import com.puzzlemind.brainsqueezer.utils.DBFetcherWorker
import com.puzzlemind.brainsqueezer.utils.SendScheduledScores
import com.puzzlemind.brainsqueezer.utils.SendScheduledSlidingPuzzleScores
import io.grpc.android.BuildConfig
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class PuzzleApp : Application() {

    private val scope = CoroutineScope(SupervisorJob())
    val database by lazy { PuzzleRoomDatabase.getDatabase(this,scope) }
    val repository by lazy { AppRepository(database) }
    val scrambledRepository by lazy { ScrambledRepository(database,applicationContext) }


    val TAG = "PuzzleApp"


    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) {
            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf("12448C062C2EE5B9417B746467ECF12B")).build()
            MobileAds.setRequestConfiguration(config)
        }
        MobileAds.initialize(this) {}

        FirebaseApp.initializeApp(this)

        Handler(Looper.getMainLooper()).post {


            SoundUtil.init(this)
            scheduleWorks()

            updateQuestionsTable()
        }

    }

    private fun updateQuestionsTable() {
        Log.d(TAG, "updateQuestionsTable: ")
        val updateDBWorker = OneTimeWorkRequestBuilder<DBFetcherWorker>()
            .addTag("DatabaseFetcherWorker")
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(Constants.UPDATE_DB_UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
            updateDBWorker
        )
    }

    private fun scheduleWorks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED).build()

        val workRequest = PeriodicWorkRequestBuilder<SendScheduledScores>(
            repeatInterval = 1,
            TimeUnit.HOURS
        ).setInitialDelay(0,TimeUnit.SECONDS)
            .setConstraints(constraints)
            .addTag(Constants.SEND_SCHEDULED_SCORE_WORKER)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            Constants.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        val slidingPuzzleWorkRequest = PeriodicWorkRequestBuilder<SendScheduledSlidingPuzzleScores>(
            repeatInterval = 1,
            TimeUnit.HOURS
        ).setInitialDelay(0,TimeUnit.SECONDS)
            .setConstraints(constraints)
            .addTag(Constants.SEND_SLIDING_PUZZLE_SCHEDULED_SCORE_WORKER)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            Constants.UNIQUE_WORK_NAME_FOR_SLIDING_PUZZLE,
            ExistingPeriodicWorkPolicy.REPLACE,
            slidingPuzzleWorkRequest
        )

    }


}
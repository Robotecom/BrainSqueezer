package com.puzzlemind.brainsqueezer.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.Executor

class SlidingPuzzleFetcher(
    val repository: ScrambledRepository,
    val context: Context,
    val scope: CoroutineScope,
    val callback: SlidingPuzzleFetcherCallback,
) {


    val TAG = "SlidingPuzzleFet"
    suspend fun fetchMoreAndSave(query: Query) {

        val newLevelQuerySnapshot = query.get().await()

        val newLevels =
            newLevelQuerySnapshot.documents.map { it?.toObject(ScrambledLevel::class.java)!! }


        println("fetch: Document received... :${newLevels}")

        if (newLevels.isEmpty()){

            callback.finishLoading(Result.Success(R.string.empty_result,true))
            return
        }
        newLevels.forEach { scrambledLeve ->
            Log.d(TAG, "fetchNewSlidingPuzzles: $scrambledLeve")

            val dir = File(context.filesDir, scrambledLeve.fid)
            dir.mkdir()
            val zipFile = File(context.filesDir, "${dir.name}/${scrambledLeve.fileName}")

            val zipFileRef = Firebase.storage.getReferenceFromUrl(scrambledLeve.downloadUrl)

            zipFileRef.getFile(zipFile).addOnCompleteListener {
                if (it.isSuccessful) {

                    Log.d(TAG, "fetchMoreAndSave: success")
                    scope.launch(Dispatchers.IO) {

                        unZipFile(dir.path, zipFile.path)

                        //this is the image appearing with level information before entering level
                        scrambledLeve.preview = "${dir.name}/preview.webp"
                        repository.insertLevel(scrambledLeve)

                        callback.finishLoading(Result.Success(0,false))

                    }



                } else {
                    callback.finishLoading(Result.Failure(R.string.general_failure))

                    Log.d(
                        TAG,
                        "fetchNewSlidingPuzzles: could not download file${it.exception.toString()}"
                    )
                }

            }


        }
    }

    private fun unZipFile(dirPath: String, zipPath: String) {

        ZipManager.unzip(zipPath, dirPath)

        if (File(zipPath).delete()) {
            Log.d(TAG, "unZipFile: zip file deleted")
        } else {
            Log.d(TAG, "unZipFile: could not deleted zip file")
        }

    }


    interface SlidingPuzzleFetcherCallback {

        fun finishLoading(resultFeedback: Result)
    }




}
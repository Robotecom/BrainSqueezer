package com.puzzlemind.brainsqueezer.scambled

import android.app.Application
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledLevel
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledRepository
import com.puzzlemind.brainsqueezer.utils.SlidingPuzzleFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.puzzlemind.brainsqueezer.utils.*

class ScrambledLevelViewModel(repository: ScrambledRepository, application: Application) :AndroidViewModel(
    application
){


    val TAG = "ScrambledLevelVM"
    val applicationContext = application
    val scrambledRepository = repository
    var difficulty:ScrambledDifficulty = ScrambledDifficulty.Easy

    val scrambledLevelUiState = MutableStateFlow(ScrambledLevelUiState())


    fun setDifficulty(diffIndex:Int){
        viewModelScope.launch {

            difficulty = ScrambledDifficulty.getDiffFromIndex(diffIndex)
            scrambledRepository.getLevels(ScrambledDifficulty.getDiffFromIndex(diffIndex))
                .collect { levels->
                    withContext(Dispatchers.IO){
                        scrambledRepository.updateScrambledDashboardPoints()
                        scrambledRepository.updateDashboardProgressForDiff(diffIndex)

                    }

                    scrambledLevelUiState.value = scrambledLevelUiState.value.copy(levelsList = levels)
                }
        }

    }

    fun onLoadMore(lastLevel:Int){

        scrambledLevelUiState.value = scrambledLevelUiState.value.copy(resultFeedback = Result.Loading)

        viewModelScope.launch {
            withContext(Dispatchers.IO){

                val newLevelQuerySnapshot = Firebase.firestore.collection(
                    Constants.SLIDING_PUZZLES_COL + "/" +
                            ScrambledDifficulty.getDiffStringFrom(difficulty = difficulty) + "/" +
                            Constants.PUZZLES_COL
                ).orderBy("level")
                    .startAfter(lastLevel).limit(1)

                SlidingPuzzleFetcher(
                    repository = scrambledRepository,
                    context = applicationContext,
                    scope = viewModelScope,
                    callback = object :SlidingPuzzleFetcher.SlidingPuzzleFetcherCallback{
                        override fun finishLoading(resultFeedback: Result) {

                            scrambledLevelUiState.value =
                                scrambledLevelUiState.value.copy(resultFeedback = resultFeedback)

                        }
                    },
                ).fetchMoreAndSave(
                    query = newLevelQuerySnapshot
                )


            }
        }
    }

    fun onDeleteLevel(level: ScrambledLevel) {

        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "onDeleteLevel: ************")
            scrambledRepository.deleteScrambledLevel(level = level)

        }

    }

}

@Keep
data class ScrambledLevelUiState(var levelsList:MutableList<ScrambledLevel> = mutableListOf(),
                                 var loadingMore:Boolean = false,
                                 var progress:Int = 0,
                                 var resultFeedback:Result = Result.Idle
                                 )

class ScrambledLevelViewModelFactory(val repository: ScrambledRepository,val application: Application):ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScrambledLevelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScrambledLevelViewModel(repository = repository,application = application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
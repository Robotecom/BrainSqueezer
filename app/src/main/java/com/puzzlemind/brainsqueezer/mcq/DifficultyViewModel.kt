package com.puzzlemind.brainsqueezer.mcq

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.puzzlemind.brainsqueezer.AppRepository
import com.puzzlemind.brainsqueezer.data.DifficultyLevel
import com.puzzlemind.brainsqueezer.data.Game
import com.puzzlemind.brainsqueezer.data.MCQDashboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DifficultyViewModel(val repository: AppRepository,val game: Game):ViewModel() {

    val TAG = "DifficultyViewModel"
    private val NUMBER_OF_GAMES: Float = 3F
    private val _uiState = MutableStateFlow(DifficultyScreenState())
    val uiState = _uiState.asStateFlow()

    private suspend fun emitState(difficultyScreenState: DifficultyScreenState){
        _uiState.emit(difficultyScreenState)
    }

    init {
        Log.d(TAG, "dashboard: before: ")

        viewModelScope.launch {

            repository.getDifficultyLive(game = game)
                .collect { diffs: MutableList<DifficultyLevel> ->


                    emitState(_uiState.value.copy(diffs = diffs))
                    var progress = 0f
                    diffs.forEach { difficultyLevel ->

                        progress +=difficultyLevel.progress
                    }

                    withContext(Dispatchers.IO){
                        val NUMBER_OF_Diff_LEVELS = diffs.size
                        repository.updateUserSkills(progress/(NUMBER_OF_Diff_LEVELS))

                        repository.updateMCQDashboard(
                            points = repository.getSumOfHighscores(),
                            stars = repository.getSumOfStars(),
                            trophies = repository.getSumOfTrophies(),
                            maxLevel = repository.getMCQMaxLevelReached(),
                            progress = progress/NUMBER_OF_Diff_LEVELS.toFloat(),
                            levels = repository.getlevelsCount()
                        )


                    }

            }


        }

        viewModelScope.launch {
            Log.d(TAG, "dashboard: before: ")
            repository.getMCQDashboardLive().collect{ value: MCQDashboard ->

                Log.d(TAG, ": dashboard:${value}")
                _uiState.value = _uiState.value.copy(dashboard = value)
            }
        }

    }
}

@Keep
data class DifficultyScreenState(
    val diffs:MutableList<DifficultyLevel> = mutableListOf(),
    val dashboard:MCQDashboard = MCQDashboard()
)

class DifficultyViewModelFactory(private val repository: AppRepository, private val game: Game) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DifficultyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DifficultyViewModel(repository = repository,game = game) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
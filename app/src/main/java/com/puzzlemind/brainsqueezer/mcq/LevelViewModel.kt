package com.puzzlemind.brainsqueezer.mcq

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.puzzlemind.brainsqueezer.AppRepository
import com.puzzlemind.brainsqueezer.data.Level
import com.puzzlemind.brainsqueezer.data.DifficultyConverter
import com.puzzlemind.brainsqueezer.data.DifficultyLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LevelViewModel(val repository: AppRepository,val diffLevel: Int):ViewModel() {

    val TAG = "LevelViewModel"

    private val _uiState = MutableStateFlow(LevelTreeMapState())
    val uiState = _uiState.asStateFlow()
    private var difficultyLevel:Int = diffLevel
    private lateinit var levelMap: Flow<MutableList<Level>>


    init {
        viewModelScope.launch{
            levelMap = repository.getDifficultyLevels(diff =
             DifficultyConverter.fromIntToEnum(difficultyLevel))
            levelMap.collect { value: MutableList<Level> ->

                emitState(_uiState.value.copy(levels = value))

                val difficulty = getDifficultyLevel(value)

                Log.d(TAG, "Difficulty : $difficulty")
                withContext(Dispatchers.IO) {
                    repository.updateDifficultyTrophyProgress(
                        diff = difficulty.diffIndex,
                        game = difficulty.game,
                        trophies = difficulty.trophies,
                        progress = difficulty.progress
                    )
                }
            }
        }

    }

    private fun getDifficultyLevel(value: MutableList<Level>): DifficultyLevel {

        var trophies = 0
        var progress = 0f

        for (level in value){
            if (level.trophy) {
                trophies++
            }
            if (level.isPassed){
                progress += 1
            }
        }
        progress /= value.size

        return DifficultyLevel(levels = value.size,
            trophies = trophies,
            progress = progress,
            game = value.first().game,
            diffIndex = value.first().diffIndex
            )
    }

    private suspend fun emitState(levelTreeMap:LevelTreeMapState){

        _uiState.emit(levelTreeMap)
    }


    override fun onCleared() {
        super.onCleared()
        levelMap?.cancellable()
    }


}

@Keep
data class LevelTreeMapState(
    val diffLevel: Int = 1,
    val levels:MutableList<Level> = mutableListOf()
)

class LevelViewModelFactory(private val repository: AppRepository,private val diffLevel: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LevelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LevelViewModel(repository = repository,diffLevel = diffLevel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
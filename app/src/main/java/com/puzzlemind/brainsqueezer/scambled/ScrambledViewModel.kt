package com.puzzlemind.brainsqueezer.scambled

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledDashboard
import com.puzzlemind.brainsqueezer.scambled.data.ScrambledRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScrambledViewModel(val repository: ScrambledRepository) :ViewModel(){

    val TAG = "ScrambledViewModel"

    val _uiState = MutableStateFlow(ScrambledDashboardUiState())
    val uiState = _uiState.asStateFlow()



    init {

        viewModelScope.launch {
            repository.getScrambledDashboardLive().collect { dashboard:ScrambledDashboard? ->


                withContext(Dispatchers.IO){
                    repository.updateUserSkillfulness(dashboard?.matchWon!!/300f)

                }
                Log.d(TAG, "scrambledViewModel: dashboard:${dashboard}")
                emitState(_uiState.value.copy(dashboard = dashboard?: ScrambledDashboard(points = 333,skillfulness = 0.4f)))
            }


        }

    }

    private suspend fun emitState(state: ScrambledDashboardUiState) {
        _uiState.emit(state)
    }
}

@Keep
data class ScrambledDashboardUiState(
    val dashboard: ScrambledDashboard = ScrambledDashboard()
)


class ScrambledViewModelFactory(private val repository: ScrambledRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScrambledViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScrambledViewModel(repository = repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
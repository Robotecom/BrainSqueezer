package com.puzzlemind.brainsqueezer.main

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.AppRepository
import com.puzzlemind.brainsqueezer.data.Game
import com.puzzlemind.brainsqueezer.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(repository:AppRepository):ViewModel(){


    var firebaseUser:FirebaseUser? = Firebase.auth.currentUser

    val TAG = "MainViewModel"
    val uiState = MutableStateFlow(MainActivityState())
    init {

        Log.d(TAG, "mainviewmodel initiated: ")
        viewModelScope.launch {
            repository.getLiveUser(0).collect {

                Log.d(TAG, "userLive:${it}")
                it?.let {
                    uiState.value = uiState.value.copy(user = it)

                }

            }

        }

        Firebase.auth.addAuthStateListener {
            firebaseUser = it.currentUser
        }

    }
}

@Keep
data class MainActivityState(val  user: User = User())

class MainViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository = repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
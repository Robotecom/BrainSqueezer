package com.puzzlemind.brainsqueezer.profile

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.AppRepository
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.data.User
import com.puzzlemind.brainsqueezer.leaderboard.ErrorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(userId:String,val repository:AppRepository):ViewModel(){

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState = _uiState.asStateFlow()

    private suspend fun emitState(profileState: ProfileState){
        _uiState.emit(profileState)
    }

    init {

        getProfileOfUser(userId)
    }


    private fun getProfileOfUser(userId:String){
        viewModelScope.launch {
            val currentUserId = Firebase.auth.uid
            emitState(_uiState.value.copy(loading = true,isOwner = currentUserId == userId))

        }


            Firebase.firestore.document(Constants.USERS + "/" + userId)
                .get().addOnCompleteListener {
                    viewModelScope.launch {

                        if (it.isSuccessful) {

                            emitState(
                                _uiState.value.copy(
                                    user = it.result.toObject(User::class.java) ?: User(),
                                    loading = false
                                )
                            )

                        } else {
                            emitState(
                                _uiState.value.copy(
                                    error = ErrorState(
                                        true,
                                        it.exception?.message!!
                                    ), loading = false
                                )
                            )
                        }
                    }
                }

    }

}

class ProfileViewModelFactory(private val userId: String,val repository: AppRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(userId = userId, repository = repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@Keep
data class ProfileState(
    val user:User = User(),
    var loading:Boolean = false,
    var error:ErrorState = ErrorState(false,""),
    val isOwner: Boolean = false


) {
}
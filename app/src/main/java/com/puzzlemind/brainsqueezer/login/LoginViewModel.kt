package com.puzzlemind.brainsqueezer.login

import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.puzzlemind.brainsqueezer.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.AuthCredential

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import java.util.*


class LoginViewModel:ViewModel(){

    val TAG = "LoginViewModel"
    val _uiState = MutableStateFlow(LoginScreenState())
    val uiState = _uiState.asStateFlow()
    var name:String = ""

    var currentUser:FirebaseUser? = null

     fun setLoading(state:Boolean){
         viewModelScope.launch {
             _uiState.emit(_uiState.value.copy(signInLoading = state))

         }
    }


    fun directToDataEntryThenUpdate() {
        viewModelScope.launch {

            val user = currentUser?.let { User(uid = it.uid,
                name = it.displayName!!,
                profile = it.photoUrl.toString(),
                downloadUri = it.photoUrl.toString()
                ) }
            _uiState.emit(_uiState.value.copy(direct = true,user = user?:User()))

        }
    }

    fun signInAnonymouslyOrSkip(){
        viewModelScope.launch {
            if (currentUser == null ) {
                Log.d(TAG, "signAnonymously: ")
                Firebase.auth.signInAnonymously().addOnCompleteListener {
                    Log.d(TAG, "directToDataEntry userSignedInAnonymously: ")
                }
            }
            _uiState.emit(_uiState.value.copy(direct = true))
            if (currentUser != null){
                _uiState.emit(_uiState.value.copy(user = User(name = currentUser?.displayName!!,profile = currentUser?.photoUrl.toString())))
            }
        }
    }

    fun setProfile(data: Uri) {

        val user = _uiState.value.user.copy(profile = data.toString(),downloadUri = "")
        viewModelScope.launch {
            _uiState.emit(_uiState.value.copy(user = user))

        }

    }

    fun setFirebaseUser(currentUser: FirebaseUser?) {
        this.currentUser = currentUser
    }

    fun submitUserData(name:String){

        this.name = name

        viewModelScope.launch {
            if (name.trim().isNotEmpty()){
                _uiState.value = _uiState.value.copy(progressbar = true)

                if (_uiState.value.user.profile.isNotEmpty()){

                    if (_uiState.value.user.downloadUri.isEmpty()) {
                        sendUserProfile(name)
                    }else{
                        sendUserData(name)
                    }
                }else{
                    Log.d(TAG, "submitUserData: has not profile Pic")
                    sendUserData(name)
                }
            }else{

                _uiState.value = _uiState.value.copy(error = "Too Short Name")
            }
        }

    }

    private suspend fun sendUserProfile(name: String) {

        val file = Uri.parse(_uiState.value.user.profile)
        _uiState.emit(_uiState.value.copy(profileLoading = true))
        val imageRef = Firebase.storage.reference
            .child(Constants.USERS)
            .child(currentUser?.uid!!)
            .child(Constants.PROFILE)
            .child(file.lastPathSegment.toString())

        val uploadTask = imageRef.putFile(file)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            var state = _uiState.value.copy(profileLoading = false)

            state = if (task.isSuccessful) {

                val downloadUri = task.result
                val user = state.user.copy(
                    downloadUri = downloadUri.toString())
                state.copy(user = user,progressbar = true)

            } else {
                // Handle failures
                // ...
                state.copy(error = task.exception.toString())
            }
            viewModelScope.launch {
                _uiState.emit(state)
                if (task.isSuccessful){
                    sendUserData(name = name)
                }

            }

        }

        Log.d(TAG, "sendUserProfile: storageRef:${imageRef.path}")



    }

    private suspend fun sendUserData(name: String) {

        _uiState.emit(_uiState.value.copy(progressbar = true))

        if (currentUser != null){
            val user = _uiState.value.user.copy(uid = currentUser?.uid.toString(),
                name = name
            )

            Firebase.firestore.document("${Constants.USERS}/${user.uid}")
                .set(user.getUser(), SetOptions.merge()).await()

            val request = UserProfileChangeRequest.Builder()
            request.displayName = user.name
            request.photoUri = Uri.parse(user.downloadUri)
            currentUser!!.updateProfile(request.build()).await()

            _uiState.emit(_uiState.value.copy(finishUI = true))
        }
    }


}

class SignInHelper{

    companion object {


        fun linkWithCredentials(
            firebaseUser: FirebaseUser,
            providerCredentials: AuthCredential?
        ): Task<AuthResult?>? {
            return firebaseUser.linkWithCredential(providerCredentials!!)
        }

        fun getGoogleCredentials(signInAccount: GoogleSignInAccount): AuthCredential? {
            return GoogleAuthProvider.getCredential(signInAccount.idToken, null)
        }

        fun signInWithCredentials(
            firebaseAuth: FirebaseAuth,
            authCredential: AuthCredential?
        ): Task<AuthResult?>? {
            return firebaseAuth.signInWithCredential(authCredential!!)
        }
    }
}

@Keep
data class LoginScreenState(
    val user: User = User(),
    val signInLoading: Boolean = false,
    val direct: Boolean = false,
    val profileLoading:Boolean = false,
    val error:String = "",
    val progressbar:Boolean = false,
    val finishUI:Boolean = false
)

@Keep
data class User(
    var uid: String = "",
    var name: String = "",
    var profile: String = "",
    var downloadUri:String = "",
    var timestamp:Timestamp = Timestamp.now()

){
    fun getUser(): MutableMap<String, Any> {
        val map = mutableMapOf<String,Any>()
        map["uid"] = uid
        map["name"] = name
        map["profile"] = profile
        map["downloadUri"] = downloadUri
        map["timestamp"] = FieldValue.serverTimestamp()
        return map
    }
}
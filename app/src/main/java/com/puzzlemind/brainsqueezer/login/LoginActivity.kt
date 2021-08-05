package com.puzzlemind.brainsqueezer.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.ui.theme.BrainSqueezerTheme
import com.puzzlemind.brainsqueezer.utils.isOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LoginActivity : ComponentActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var auth: FirebaseAuth
    val TAG = "LoginActivity"
    private var googleSignInClient: GoogleSignInClient? = null
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data

                viewModel.setLoading(true)
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                    if (viewModel.currentUser == null) {
                        firebaseAuthWithGoogle(account.idToken!!)
                    } else {
                        linkWithCreditials(SignInHelper.getGoogleCredentials(account))
                    }
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e)
                }
            } else {

                Log.d(
                    TAG,
                    "onActivity for result:${result.resultCode} Canceled or something went wrong${result.data?.extras}"
                )
            }


        }

    private fun linkWithCreditials(authCredential: AuthCredential?) {

        SignInHelper.linkWithCredentials(viewModel.currentUser!!, authCredential)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    signInWithCredential(it.result?.credential)
                } else {
                    Toast.makeText(applicationContext, it.exception?.message, Toast.LENGTH_LONG)
                        .show()
                    viewModel.setLoading(false)

                }
            }
    }

    private fun signInWithCredential(credential: AuthCredential?) {

        SignInHelper.signInWithCredentials(Firebase.auth, authCredential = credential)
            ?.addOnCompleteListener {
                viewModel.setLoading(false)

                if (it.isSuccessful) {
                    viewModel.directToDataEntryThenUpdate()

                } else {
                    Toast.makeText(applicationContext, it.exception?.message, Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private var imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                Log.d(TAG, "successful result: ${result.data?.data}")
                if (result.data?.data != null) {
                    viewModel.setProfile(result.data?.data!!)
                }
            } else {

                Log.d(
                    TAG,
                    "onActivity for result:${result.resultCode} Canceled or something went wrong${result.data?.extras}"
                )
            }
        }


    @ExperimentalComposeUiApi
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        // Configure Google Sign In


        viewModel = ViewModelProvider.NewInstanceFactory().create(LoginViewModel::class.java)

        lifecycleScope.launch {
            viewModel.uiState.collect { loginState ->

                if (loginState.finishUI) {

                    withContext(Dispatchers.IO) {
                        (application as PuzzleApp).repository.updateUserUidnProfile(
                            viewModel.name,
                            loginState.user.downloadUri
                        )
                    }
                    delay(1000)

                    finish()
                }

                if (loginState.error.isNotEmpty()) {
                    Toast.makeText(applicationContext, loginState.error, Toast.LENGTH_LONG).show()

                    delay(2000)
                    viewModel._uiState.value = viewModel._uiState.value.copy(error = "")
                }

            }

        }

        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val loginState = viewModel.uiState.collectAsState()
                    LoginScaffold(onSignInWithGoogle = {
                        if (isOnline(applicationContext)) {
                            signIn()
                        } else {
                            Toast.makeText(
                                applicationContext,
                                R.string.this_operation_need_internet,
                                Toast.LENGTH_LONG
                            ).show()

                        }
                    },
                        onSkipButton =
                        {
                            if (isOnline(context = applicationContext)) {
                                viewModel.signInAnonymouslyOrSkip()
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    R.string.this_operation_need_internet,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        loginScreenState = loginState.value,
                        onImagePickerClick = {

                            val intent = Intent()
                            intent.type = "image/*"
                            intent.action = Intent.ACTION_GET_CONTENT

                            imagePickerLauncher.launch(
                                Intent.createChooser(
                                    intent,
                                    getString(R.string.pick_image)
                                )
                            )

                        },
                        onSubmitClick = { input ->
                            if (isOnline(context = applicationContext)) {

                                if (input.length < 4) {
                                    Toast.makeText(
                                        applicationContext,
                                        R.string.name_too_short,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else
                                    viewModel.submitUserData(input)

                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    R.string.this_operation_need_internet,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }
        }

        Handler(Looper.getMainLooper())
            .post {
                setupSignInIntegration()
                saveNecessarySettings()
            }
    }

    private fun saveNecessarySettings() {

        val shared = getSharedPreferences(getString(R.string.preference_file_key), MODE_PRIVATE)
        if (shared.getString(Constants.LANGUAGE_CHOSEN, "") == "") {
            val sharedEditor = shared.edit()
            val phoneLang = Locale.getDefault().isO3Language

            if (phoneLang.equals("ara")) {
                sharedEditor.putString(Constants.LANGUAGE_CHOSEN, "ara")

            } else {
                sharedEditor.putString(Constants.LANGUAGE_CHOSEN, "eng")
            }
            sharedEditor.apply()
        }

    }

    private fun signIn() {

        val signInIntent = googleSignInClient?.signInIntent
        if (signInIntent == null) Log.d(TAG, "signIn: googleSignInClients intent is null")
        resultLauncher.launch(signInIntent)

    }

    private fun setupSignInIntegration() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.request_id_token))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)


        auth.addAuthStateListener {
            viewModel.setFirebaseUser(it.currentUser)

            if (viewModel.currentUser != null) {
                if (!viewModel.currentUser?.isAnonymous!!) run {
                    viewModel.signInAnonymouslyOrSkip()
                }
            }
        }

    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->

                viewModel.setLoading(false)
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    viewModel.directToDataEntryThenUpdate()

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    Toast.makeText(applicationContext, "Error:${task.exception}", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }
}

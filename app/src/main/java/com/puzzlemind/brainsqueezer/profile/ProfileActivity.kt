package com.puzzlemind.brainsqueezer.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.puzzlemind.brainsqueezer.AppRepository
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.ui.theme.BrainSqueezerTheme

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //from anywhere in the app if you want to open this
        //Activity you must provide userId in the intent
        val userId = intent.data


        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val viewModel = viewModel<ProfileViewModel>(factory = ProfileViewModelFactory(userId = userId.toString(),(application as PuzzleApp).repository))

                    val profileState = viewModel.uiState.collectAsState()
                    ProfileScreen(profileState = profileState.value)
                }
            }
        }
    }
}

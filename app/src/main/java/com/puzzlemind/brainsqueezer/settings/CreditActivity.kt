package com.puzzlemind.brainsqueezer.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.ui.theme.BrainSqueezerTheme

class CreditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Credit()
                }
            }
        }
    }
}

@Composable
fun Credit() {

    Box(modifier = Modifier.padding(24.dp)){
        Column {
            Text(text = stringResource(id = R.string.credit_text),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground
            )
            Divider()
            Text(text = stringResource(id = R.string.credit_open_trivia),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground
            )
        }



    }

}

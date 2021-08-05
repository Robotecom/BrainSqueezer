package com.puzzlemind.brainsqueezer.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.ktx.Firebase
import com.puzzlemind.brainsqueezer.Constants
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.ui.theme.BrainSqueezerTheme
import com.puzzlemind.brainsqueezer.utils.DynamicLinksCreator
import java.net.URLEncoder
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.ContextCompat.startActivity
import java.lang.Exception


class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainSqueezerTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Menu()
                }
            }
        }
    }
}

@Composable
fun Menu() {

    Column {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = stringResource(id = R.string.sound_label),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.weight(1f))

            val context = LocalContext.current
            val sharedPreferences = context.getSharedPreferences(context.getString(
                R.string.preference_file_key), Context.MODE_PRIVATE)

            val toggleState = remember { mutableStateOf(sharedPreferences.getBoolean(Constants.SOUND_ON_KEY, true)) }

            Switch(checked = toggleState.value,onCheckedChange = {
                toggleState.value = !toggleState.value
                sharedPreferences.edit().putBoolean(Constants.SOUND_ON_KEY,toggleState.value).apply()
            },modifier = Modifier.padding(vertical = 16.dp))

        }
        Divider()
        val context = LocalContext.current
        Text(text = "Credit",
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(Intent(context, CreditActivity::class.java))
                }
                .padding(24.dp),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colors.onBackground
        )
        Divider()

        Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable {
            getSharableLink(context)

        }) {
            Text(text = stringResource(id = R.string.share_app),
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colors.onBackground
            )

            Icon(painter = painterResource(id = R.drawable.ic_baseline_share_24),
                contentDescription = null)

            Spacer(modifier = Modifier.width( 24.dp))
        }

        Divider()

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.puzzlemind.brainsqueezer")))
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.puzzlemind.brainsqueezer")))
                }
            }) {
            Text(text = stringResource(id = R.string.rate_game),
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colors.onBackground
            )

            Icon(painter = painterResource(id = R.drawable.ic_baseline_star_rate_24),
                contentDescription = null)

            Spacer(modifier = Modifier.width( 24.dp))
        }

        Divider()

        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
            ){

            Text(text = stringResource(id = R.string.contact_us))

            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier
                .size(48.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("mailto:brainsqueezerc@gmail.com");

                    intent.putExtra(Intent.EXTRA_CC, arrayOf("xyz@gmail.com"))
                    intent.putExtra(Intent.EXTRA_BCC, arrayOf("pqr@gmail.com"))
                    intent.putExtra(Intent.EXTRA_SUBJECT, "your subject goes here...")
                    intent.putExtra(Intent.EXTRA_TEXT, "Your message content goes here...")
                    context.startActivity(intent)
                }
                .background(color = Color.White, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {


                Image(modifier = Modifier.padding(8.dp),
                    painter = painterResource(id = R.drawable.ic_gmail_icon), contentDescription = null )


            }

            Spacer(modifier = Modifier.width(8.dp))

            val Twitter_Blue = Color(0xff1DA1F2)

            Box(modifier = Modifier
                .size(48.dp)
                .clickable {
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("twitter://user?screen_name=TechnologyPara")
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://twitter.com/#!/TechnologyPara")
                            )
                        )
                    }
                }
                .background(color = Twitter_Blue, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center) {

                Image(modifier = Modifier.padding(8.dp),
                    painter = painterResource(id = R.drawable.ic_twitter_icon), contentDescription = null )



            }
        }

    }
}

fun getSharableLink(context: Context) {

    val currentUser = Firebase.auth.currentUser
    DynamicLinksCreator.getShortDynamicLinkForSharingGame(
        socialTagTitle = context.getString( R.string.app_name),
        socialTagDesc = context.getString(R.string.train_your_brain),
        socialImageLink = context.getString(R.string.app_icon_url),
        userId = currentUser?.uid?:"",
    ).addOnCompleteListener {
        startIntentChooser(context, it.result.shortLink.toString())
    }
}

fun startIntentChooser(context: Context,shortLink: String) {
    val share = Intent.createChooser(Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shortLink)

        // (Optional) Here we're setting the title of the content
        putExtra(Intent.EXTRA_TITLE, context.getString(R.string.train_your_brain))


    }, context.getString( R.string.app_name))
    context.startActivity(share)
}



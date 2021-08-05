package com.puzzlemind.brainsqueezer.login

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.glide.rememberGlidePainter
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.ui.theme.NawarFont
import com.puzzlemind.brainsqueezer.ui.theme.StrongBlueDark
import com.puzzlemind.brainsqueezer.ui.theme.TransparentBlackLight
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim
import kotlinx.coroutines.delay

enum class SplashState { Shown, Completed }

private const val SplashWaitTime: Long = 1000

@Composable
fun SplashScreen(modifier: Modifier = Modifier, onTimeout: () -> Unit) {
    Box(modifier = modifier
        .fillMaxSize()
        .verticalGradientScrim(color = MaterialTheme.colors.primaryVariant)
        .background(MaterialTheme.colors.primary), contentAlignment = Alignment.Center) {
        // Adds composition consistency. Use the value when LaunchedEffect is first called
        val currentOnTimeout by rememberUpdatedState(onTimeout)

        LaunchedEffect(Unit) {
            delay(SplashWaitTime)
            currentOnTimeout()
        }
        Image(painterResource(id = R.drawable.ic_innovation), contentDescription = null)
    }
}

@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun LoginScaffold(loginScreenState: LoginScreenState,
                  onSignInWithGoogle: () -> Unit,
                  onSkipButton: () -> Unit,
                  onImagePickerClick: () -> Unit,
                  onSubmitClick: (String) -> Unit
) {

    Box(modifier = Modifier.fillMaxWidth()) {


        AnimatedVisibility(visible = !loginScreenState.direct,
            exit = slideOutHorizontally(targetOffsetX = {it}),
            enter = slideInHorizontally(initialOffsetX = {-it/2})
            ) {
            LoginScreen(
                onSignInWithGoogle = onSignInWithGoogle,
                onSkipButton = onSkipButton,
                loginScreenState = loginScreenState
            )
        }

        AnimatedVisibility(visible = loginScreenState.direct,
            exit = slideOutHorizontally(targetOffsetX = {it}),
            enter = slideInHorizontally()+ fadeIn()) {
            DataEntryScreen(loginScreenState.user,
                onImagePickerClick = onImagePickerClick,
                onSubmitClick =  {input -> onSubmitClick(input)},
                profileLoading = loginScreenState.profileLoading,
                progressbar = loginScreenState.progressbar,
                isError = loginScreenState.error.isNotEmpty()
                )

        }


    }

}

@ExperimentalMaterialApi
@Composable
fun LoginScreen(
    loginScreenState: LoginScreenState,
    onSignInWithGoogle: () -> Unit,
    onSkipButton:() -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalGradientScrim(
                color = MaterialTheme.colors.primaryVariant,
                startYPercentage = 1f,
                endYPercentage = 0f
            )
            .background(MaterialTheme.colors.primary)

    ) {

        Column(
            modifier = Modifier
                .wrapContentSize()
                .animateContentSize()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(painter = painterResource(id = R.drawable.ic_innovation),
                contentDescription = null)

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = stringResource(id = R.string.app_login_name),
                color = Color.White,
                fontFamily = NawarFont,
                fontSize = 48.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(240.dp)
            )

            Spacer(modifier = Modifier.height(96.dp))

            Column(modifier = Modifier
                .animateContentSize()
                .width(IntrinsicSize.Max)) {


                Card(elevation = 8.dp,
                    backgroundColor = Color.White,
                    modifier = Modifier.clickable {
                        onSignInWithGoogle()
                    },onClick = onSignInWithGoogle
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                            .height(40.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.google),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(24.dp))


                        Text(
                            text = stringResource(id = R.string.signin_with_google),
                            color = Color.DarkGray,
                            fontFamily = FontFamily.Default,
                            modifier = Modifier.padding(end = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .clickable {

                            onSkipButton()
                        },
                    backgroundColor = Color.White,
                    onClick = onSkipButton

                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {


                        Text(
                            text = stringResource(id = R.string.skip_label),
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                }

                Spacer(modifier = Modifier.height(24.dp))

                if (loginScreenState.signInLoading)
                    Column(modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    }

            }
        }


    }

}


@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun DataEntryScreen(user: User,
                    onImagePickerClick:()-> Unit,
                    onSubmitClick:(String)-> Unit,
                    profileLoading: Boolean,
                    progressbar: Boolean,
                    isError:Boolean
                    ) {

    val painter = rememberGlidePainter(request = if (user.profile == ""){R.drawable.ic_user}else{user.profile},
        fadeIn = true,
        requestBuilder = {
                         this.error(R.drawable.ic_user)
            this.fallback(R.drawable.ic_user)
        },
        previewPlaceholder =  R.drawable.ic_user
    )
    val nameText = remember { mutableStateOf(user.name) }

    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier
        .fillMaxSize()
        .verticalGradientScrim(color = MaterialTheme.colors.secondaryVariant)
        .background(MaterialTheme.colors.primary)
    ) {

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            if (progressbar) {
                LinearProgressIndicator(color = Color.White)

                Spacer(modifier = Modifier.height(16.dp))
            }
            Box (modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(IntrinsicSize.Max)){

                Image(
                    modifier = Modifier
                        .size(116.dp)
                        .border(
                            2.dp,
                            color = MaterialTheme.colors.onPrimary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (!progressbar)
                                if (!profileLoading)
                                    onImagePickerClick()
                        },
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                if (profileLoading) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(TransparentBlackLight))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center), color = Color.White
                    )
                }

                val iconVisible = remember{ mutableStateOf(true)}
                val alphaAnimation = animateFloatAsState(targetValue = if (iconVisible.value){1f}else{0f})
                LaunchedEffect(iconVisible){

                    delay(2500)
                    iconVisible.value = false
                }


                    Image(
                        painterResource(id = R.drawable.ic_baseline_edit_24),
                        contentDescription = null,
                        modifier = Modifier
                            .alpha(alphaAnimation.value)
                            .clip(RoundedCornerShape(topEnd = 16.dp))
                            .background(TransparentBlackLight)
                            .align(Alignment.TopEnd)

                    )


            }


            Spacer(modifier = Modifier.height(64.dp))


            OutlinedTextField(value = nameText.value,
                onValueChange = {
                    if (it.length < 34)
                    nameText.value = (it)
                                },
                label = { Text(text = stringResource(id = R.string.user_name),color = MaterialTheme.colors.onPrimary) },
                singleLine = true,
                keyboardActions = KeyboardActions (onDone = {
                    onSubmitClick(nameText.value)
                    focusManager.clearFocus()
                }),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                textStyle = TextStyle.Default.copy(color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                    ),
                isError = isError,
                enabled = !progressbar
            )

            Spacer(modifier = Modifier.height(24.dp))


            Button(
                onClick = {
                          onSubmitClick(nameText.value)
                          },
                modifier = Modifier.fillMaxWidth(),
                enabled = !progressbar,
                border = BorderStroke(2.dp,MaterialTheme.colors.onPrimary)
            ) {

                Text(
                    text = stringResource(id = R.string.submit_label),
                    color = MaterialTheme.colors.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(120.dp))


        }

    }

}



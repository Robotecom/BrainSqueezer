package com.puzzlemind.brainsqueezer.leaderboard


import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.glide.rememberGlidePainter
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.puzzlemind.brainsqueezer.PuzzleApp
import com.puzzlemind.brainsqueezer.R
import com.puzzlemind.brainsqueezer.data.LeaderboardItem
import com.puzzlemind.brainsqueezer.profile.ErrorUi
import com.puzzlemind.brainsqueezer.profile.ProfileActivity
import com.puzzlemind.brainsqueezer.ui.theme.BlueSapphire
import com.puzzlemind.brainsqueezer.utils.verticalGradientScrim

@ExperimentalAnimationApi
@ExperimentalPagerApi
@Composable
fun LeaderBoardScreen(
    modifier: Modifier,
    lazyListState: LazyListState = rememberLazyListState()
) {

    val application = LocalContext.current.applicationContext

    val leaderboardViewModel = viewModel<LeaderboardViewModel>(

        factory = LeaderboardModelFactory((application as PuzzleApp).repository)
    )
    val uiState = leaderboardViewModel.uistate.collectAsState()
    val pagerState = rememberPagerState(pageCount = 3, initialOffscreenLimit = 2, initialPage = 1)
    val context = LocalContext.current

    val onItemClick: (String) -> Unit = {
        val intent = Intent(context, ProfileActivity::class.java)
        intent.data = Uri.parse(it)
        context.startActivity(intent)
    }

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {


        if (!uiState.value.empty && !uiState.value.refreshing)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalGradientScrim(
                    color = MaterialTheme.colors.secondaryVariant,
                    startYPercentage = 1f,
                    endYPercentage = 0f
                )
            ) {


                LazyColumn(
                    modifier = modifier,
                    state = lazyListState,
                ) {

                    item { Spacer(modifier = Modifier.height(80.dp)) }


                    item {

                        if (uiState.value.firstThreeList.size == 3)
                            HorizontalPager(
                                state = pagerState, itemSpacing = 24.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->


                                FirstPosition(
                                    leaderboardItem = uiState.value.firstThreeList[page],
                                    page = page,
                                    modifier = Modifier.wrapContentSize(),
                                    onItemClick = { uid: String ->
                                        onItemClick(uid)
                                    }
                                )

                            }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }

                    item {

                        Row(
                            Modifier.padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                modifier = Modifier.height(24.dp),
                                text = stringResource(id = R.string.top_50),
                                color = MaterialTheme.colors.onBackground,
                                fontSize = 20.sp
                            )

                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.last_week),
                                color = MaterialTheme.colors.onBackground,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.weight(1f))
                            OrderSpinner(
                                uiState.value.orderBy,
                                leaderboardViewModel::onOrderSelected
                            )
                        }

                    }
                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    item {

                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(MaterialTheme.colors.primary)

                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                ConstraintLayout(Modifier.fillMaxSize()) {

                                    val (rankL, imageS,nameL, pointL, trophyL) = createRefs()
                                    Text(
                                        text = stringResource(id = R.string.rank_label),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .constrainAs(rankL) {
                                                start.linkTo(parent.start)

                                            }
                                            .padding(horizontal = 12.dp)

                                    )
                                    Spacer(modifier = Modifier
                                        .constrainAs(imageS) {
                                            start.linkTo(rankL.end)
                                        }
                                        .width(40.dp))

                                    BoxWithConstraints(modifier = Modifier.constrainAs(nameL) {
                                        start.linkTo(imageS.end)
                                    }) {

                                        Text(modifier = Modifier.width(if (this.maxWidth > 400.dp ){200.dp}else{120.dp}),
                                            text = stringResource(id = R.string.name_label),
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Start
                                        )
                                    }

                                    Text(
                                        modifier = Modifier.constrainAs(pointL) {
                                            end.linkTo(trophyL.start, margin = 8.dp)
                                            start.linkTo(nameL.end)

                                        },
                                        text = stringResource(id = R.string.points_label),
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )


                                    Text(
                                        modifier = Modifier
                                            .constrainAs(trophyL) {
                                                end.linkTo(parent.end, margin = 8.dp)
                                            }
                                           ,
                                        text = stringResource(id = R.string.trophy_label),
                                        color = Color.White,
                                        fontSize = 12.sp,

                                        )

                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(4.dp)) }

                    items(uiState.value.leaderboardList.size) { index ->
                        LeaderboardItem(
                            leaderboardItem = uiState.value.leaderboardList[index],
                            onItemClick = {
                                onItemClick(it)

                            }
                        )

                    }


                    item { Spacer(modifier = Modifier.height(64.dp)) }


                }

            }

        val localModifier = Modifier.align(Alignment.Center)
        if (uiState.value.empty && !uiState.value.refreshing) {
            Text(modifier = localModifier, text = stringResource(id = R.string.no_data_available))
        }

        if (uiState.value.refreshing) {

            CircularProgressIndicator(modifier = localModifier)
        }

        if (uiState.value.error.isError) {

            ErrorUi(error = uiState.value.error, modifier = localModifier)

        }

        AnimatedVisibility(
            visible = lazyListState.isScrollingUp(),
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {

            Surface(
                elevation = 24.dp,

            ) {

                Box(modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.leaderboard_label),
                        color = MaterialTheme.colors.onPrimary,
                        fontSize = 24.sp
                    )
                }


            }

        }

    }

}


@Composable
fun LeaderboardItem(leaderboardItem: LeaderboardItem, onItemClick: (String) -> Unit) {

    Box(
        modifier = Modifier
            .wrapContentHeight()
            .clickable {
                onItemClick(leaderboardItem.uid)
            }
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colors.surface)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            ConstraintLayout(Modifier.fillMaxSize()) {

                val (rankBox, imageBox, nameBox, pointBox, trophyBox) = createRefs()

                Box(
                    modifier = Modifier
                        .constrainAs(rankBox) {
                            start.linkTo(parent.start)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(parent.top)
                        }
                        .padding(horizontal = 8.dp)
                        .width(28.dp)
                ) {


                    if (leaderboardItem.rank <= 10)
                        Image(
                            painter = painterResource(id = R.drawable.ic__42_badge_rank),
                            contentDescription = null
                        )

                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "${leaderboardItem.rank}",
                        color = if (leaderboardItem.rank <= 10) {
                            MaterialTheme.colors.onPrimary
                        } else {
                            MaterialTheme.colors.onSurface
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center

                    )

                }

                Image(
                    modifier = Modifier
                        .constrainAs(imageBox) {
                            start.linkTo(rankBox.end)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(parent.top)
                        }
                        .border(1.dp, color = MaterialTheme.colors.onSurface, shape = CircleShape)
                        .clip(CircleShape)
                        .size(32.dp),
                    painter = rememberGlidePainter(leaderboardItem.profile),
                    contentDescription = "photo",
                    contentScale = ContentScale.Crop

                )

                BoxWithConstraints(modifier = Modifier
                    .constrainAs(nameBox) {
                        start.linkTo(imageBox.end)
                        bottom.linkTo(parent.bottom)
                        top.linkTo(parent.top)
                    }) {


                    Text(
                        text = leaderboardItem.name,
                        modifier = Modifier
                            .padding(8.dp)
                            .width(
                                if (this.maxWidth > 400.dp) {
                                    200.dp
                                } else {
                                    120.dp
                                }
                            ),
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }

                Text(
                    text = "${leaderboardItem.points}",
                    modifier = Modifier
                        .constrainAs(pointBox) {
                            start.linkTo(nameBox.end)
                            end.linkTo(trophyBox.start, margin = 16.dp)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(parent.top)
                        }
                        .padding(8.dp),
                    color = MaterialTheme.colors.onSecondary,
                    maxLines = 1,
                    fontSize = 16.sp

                )

                Text(
                    text = "${leaderboardItem.trophies}",
                    modifier = Modifier
                        .constrainAs(trophyBox) {
                            end.linkTo(parent.end, margin = 16.dp)
                            bottom.linkTo(parent.bottom)
                            top.linkTo(parent.top)
                        }
                        .width(32.dp),
                    color = MaterialTheme.colors.primaryVariant,
                    fontSize = 16.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }

        }

    }

}


@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}


@ExperimentalPagerApi
@Composable
fun FirstPosition(
    leaderboardItem: LeaderboardItem,
    page: Int,
    modifier: Modifier,
    onItemClick: (String) -> Unit
) {

    val animateSize = when (page) {
        1 -> Size(width = 140f, height = 160f)
        else -> Size(width = 120f, height = 140f)
    }
    val painter = rememberGlidePainter(
        request = leaderboardItem.profile,
        requestBuilder = {this.error(R.drawable.ic_user)
                         this.fallback(R.drawable.ic_user)},
        previewPlaceholder = R.drawable.ic_baseline_person_24
    )

    Box {

        Box(
            modifier = modifier
                .wrapContentSize()
        ) {


            Column(
                modifier = Modifier
                    .size(width = animateSize.width.dp, height = animateSize.height.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.surface)
                    .clickable {
                        onItemClick(leaderboardItem.uid)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    modifier = Modifier
                        .border(border = BorderStroke(1.dp, Color.Black), shape = CircleShape)
                        .clip(
                            CircleShape
                        )
                        .size(
                            if (page != 1) {
                                40.dp
                            } else {
                                48.dp
                            }
                        ),
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = leaderboardItem.name,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.width(
                        if (page != 1) {
                            96.dp
                        } else {
                            112.dp
                        }
                    ),
                    fontSize = if (page != 1) {
                        14.sp
                    } else {
                        16.sp
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(id = R.string.points, leaderboardItem.points),
                    color = Color.Gray,
                    fontSize = if (page != 1) {
                        12.sp
                    } else {
                        14.sp
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.clip(RoundedCornerShape(16.dp)).defaultMinSize(minWidth = if (page != 1){48.dp}else{56.dp}).background(MaterialTheme.colors.primary),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {

                    Image(painter = painterResource(id = R.drawable.trophy_filled_strocked),
                        contentDescription = null,
                        modifier = Modifier.size(if (page != 1) {
                            10.dp
                        } else {
                            12.dp
                        })
                        )
                    Text(
                        text = " = ${leaderboardItem.trophies}",
                        color = MaterialTheme.colors.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (page != 1) {
                            12.sp
                        } else {
                            14.sp
                        },
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

            }

        }

        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .clip(CircleShape)
                .width(
                    if (page != 1) {
                        40.dp
                    } else {
                        48.dp
                    }
                )
                .height(
                    if (page != 1) {
                        40.dp
                    } else {
                        48.dp
                    }
                )
                .align(Alignment.TopEnd)
        ) {

            val medalist = remember{ mutableStateListOf(R.drawable.ic_39_silver_medal,R.drawable.ic__39_medal_15,R.drawable.ic_39_bronze_medal)}
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .align(Alignment.TopCenter),
                contentScale = ContentScale.Inside,
                painter = painterResource(id = medalist[page]),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(
                        top = if (page != 1) {
                            16.dp
                        } else {
                            20.dp
                        }
                    ),
                text = "${leaderboardItem.rank}",
                color = BlueSapphire,
                fontSize = if (page != 1) {
                    16.sp
                } else {
                    18.sp
                }
            )
        }
    }
}

@Composable
fun DropDownList(
    requestToOpen: Boolean = false,
    list: List<Order>,
    request: (Boolean) -> Unit,
    selectedString: (Order) -> Unit
) {
    DropdownMenu(
        modifier = Modifier
            .wrapContentWidth()
            .background(MaterialTheme.colors.surface),
        properties = PopupProperties(),
        expanded = requestToOpen,
        onDismissRequest = { request(false) },
    ) {
        list.forEach {
            DropdownMenuItem(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    request(false)
                    selectedString(it)
                }
            ) {
                Text(
                    stringResource(id = it.stringRes), modifier = Modifier
                        .wrapContentWidth(),
                    color = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

@Composable
fun OrderSpinner(orderBy: OrderBy, orderBySelected: (Order) -> Unit) {

    val orderListText = mutableListOf(
        Order.BY_LEVEL,
        Order.BY_POINT,
        Order.BY_TROPHY
    )


    val orderLabelText =
        remember { mutableStateOf(Order.getOrderLabelFromOrder(orderBy = orderBy)) } // initial value
    val isOpen = remember { mutableStateOf(false) } // initial value
    val openCloseOfDropDownList: (Boolean) -> Unit = {
        isOpen.value = it
    }
    val userSelectedString: (Order) -> Unit = {
        orderLabelText.value = it.stringRes
        orderBySelected(it)
    }
    Box {
        Column {

            Row {

                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(id = orderLabelText.value),
                    modifier = Modifier.wrapContentWidth()
                )


            }

            DropDownList(
                requestToOpen = isOpen.value,
                list = orderListText,
                openCloseOfDropDownList,
                userSelectedString
            )
        }
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Transparent)
                .padding(horizontal = 10.dp)
                .clickable(
                    onClick = { isOpen.value = true }
                )
                .focusable()
        )
    }
}
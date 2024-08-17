package com.pnd.android.loop.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.pnd.android.loop.R
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.isSkip
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.common.findLastFullyVisibleItemIndex
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyAchievementPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val minDate by achievementViewModel.flowMinCreatedDate
        .collectAsState(initial = LocalDate.now())

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val lazyListState = rememberLazyListState()
    val pagerState = rememberPagerState {
        ChronoUnit.MONTHS.between(
            minDate.withDayOfMonth(1),
            LocalDate.now().withDayOfMonth(1),
        ).toInt() + 1
    }

    val coroutineScope = rememberCoroutineScope()
    val onDateSelected = remember {
        func@{ date: LocalDate ->
            if (date.isBefore(minDate) || date.isAfter(LocalDate.now())) {
                return@func
            }
            coroutineScope.launch {
                lazyListState.scrollToItem(
                    calculateListItemPosition(
                        itemCount = lazyListState.layoutInfo.totalItemsCount,
                        selectedDate = selectedDate,
                    )
                )
                pagerState.scrollToPage(calculateTargetCalendarPage(selectedDate))
            }
            selectedDate = date
        }
    }

    val viewMode by achievementViewModel.flowViewMode.collectAsState(
        initial = DailyAchievementPageViewMode.COLOR_DOT
    )
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            SimpleAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = selectedDate.formatYearMonth(),
                onNavigateUp = onNavigateUp,
                actions = {
                    AppBarIcon(
                        imageVector = Icons.Outlined.Checklist,
                        color = if (viewMode == DailyAchievementPageViewMode.DESCRIPTION_TEXT) {
                            AppColor.primary.copy(alpha = 0.8f)
                        } else {
                            AppColor.onSurface.copy(alpha = 0.8f)
                        },
                        descriptionResId = R.string.daily_record,
                        onClick = {
                            achievementViewModel.toggleViewMode()
                        }
                    )

                    AppBarDateIcon(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        onMoveToToday = { onDateSelected(LocalDate.now()) }
                    )
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DailyAchievementPageContent(
                pagerState = pagerState,
                lazyListState = lazyListState,
                achievementViewModel = achievementViewModel,
                viewMode = viewMode,
                minDate = minDate,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onUpdateSelectedDate = { localDate -> selectedDate = localDate }
            )
        }
    }
}

@Preview
@Composable
private fun AppBarDateIcon(
    modifier: Modifier = Modifier,
    currDate: LocalDate = LocalDate.now(),
    onMoveToToday: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clickable(onClick = onMoveToToday)
    ) {
        val borderColor = AppColor.onSurface
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(22.dp)
        )
        {
            drawRoundRect(
                color = borderColor,
                style = Stroke(
                    width = 1.dp.toPx()
                ),
                cornerRadius = CornerRadius(x = 4.dp.toPx(), y = 4.dp.toPx())
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 1.dp),
            text = "${currDate.dayOfMonth}",
            textAlign = TextAlign.Center,
            style = AppTypography.titleSmall.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold
            ),
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyAchievementPageContent(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    lazyListState: LazyListState,
    achievementViewModel: DailyAchievementViewModel,
    viewMode: DailyAchievementPageViewMode,
    minDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    Column(modifier = modifier) {

        LaunchedEffect(key1 = Unit) {
            lazyListState.scrollToItem(Int.MAX_VALUE)
        }

        UpdateSelectedDate(
            pagerState = pagerState,
            lazyListState = lazyListState,
            minDate = minDate,
            onUpdateSelectedDate = onUpdateSelectedDate
        )

        DailyAchievementCalendar(
            modifier = Modifier.weight(1f),
            viewMode = viewMode,
            pagerState = pagerState,
            achievementViewModel = achievementViewModel,
            minDate = minDate,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )

        DailyAchievementsRecords(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp),
            lazyListState = lazyListState,
            achievementViewModel = achievementViewModel,
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpdateSelectedDate(
    pagerState: PagerState,
    lazyListState: LazyListState,
    minDate: LocalDate,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    if (lazyListState.isScrollInProgress) {
        OnListScrolled(
            pagerState = pagerState,
            lazyListState = lazyListState,
            onUpdateSelectedDate = onUpdateSelectedDate
        )
    } else if (pagerState.isScrollInProgress) {
        OnPagerScrolled(
            pagerState = pagerState,
            lazyListState = lazyListState,
            minDate = minDate,
            onUpdateSelectedDate = onUpdateSelectedDate
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnListScrolled(
    pagerState: PagerState,
    lazyListState: LazyListState,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    val total by remember {
        derivedStateOf { lazyListState.layoutInfo.totalItemsCount }
    }
    val index by remember {
        derivedStateOf { lazyListState.findLastFullyVisibleItemIndex() }
    }

    val selectedDate = LocalDate.now().minusDays((total - 1).toLong()).plusDays(index.toLong())
    onUpdateSelectedDate(selectedDate)

    LaunchedEffect(key1 = selectedDate.withDayOfMonth(1)) {
        pagerState.animateScrollToPage(calculateTargetCalendarPage(selectedDate))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnPagerScrolled(
    pagerState: PagerState,
    lazyListState: LazyListState,
    minDate: LocalDate,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    var selectedDate = LocalDate.now()
        .minusMonths(pagerState.targetPage.toLong())
        .withDayOfMonth(1)
    selectedDate = if (selectedDate.isBefore(minDate)) {
        minDate
    } else if (selectedDate.isAfter(LocalDate.now())) {
        LocalDate.now()
    } else {
        selectedDate
    }

    onUpdateSelectedDate(selectedDate)
    LaunchedEffect(key1 = selectedDate) {
        lazyListState.scrollToItem(
            calculateListItemPosition(
                itemCount = lazyListState.layoutInfo.totalItemsCount,
                selectedDate = selectedDate,
            )
        )
    }
}

fun calculateTargetCalendarPage(selectedDate: LocalDate) = ChronoUnit.MONTHS.between(
    selectedDate.withDayOfMonth(1),
    LocalDate.now().withDayOfMonth(1),
).toInt()

fun calculateListItemPosition(
    itemCount: Int,
    selectedDate: LocalDate,
) =
    (selectedDate.toEpochDay() -
            LocalDate.now().minusDays((itemCount - 1).toLong()).toEpochDay()
            ).toInt()


@Composable
private fun DailyAchievementsRecords(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    achievementViewModel: DailyAchievementViewModel,
) {
    val items = achievementViewModel.achievementPager.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        reverseLayout = true,
    ) {
        items(
            count = items.itemCount,
            key = { index -> items[index]!![0].date }
        ) { index ->
            val item = items[index]!!

            AchievementItem(
                modifier = Modifier.padding(vertical = 18.dp),
                item = item
            )
        }
    }
}

@Composable
private fun AchievementItem(
    modifier: Modifier = Modifier,
    item: List<FullLoopVo>
) {
    Column(modifier = modifier) {
        val itemDate = item[0].date.toLocalDate()
        AchievementItemDateHeader(
            modifier = Modifier
                .padding(top = 22.dp)
                .padding(bottom = 18.dp),
            itemDate = itemDate,
        )

        val doneList = item.filter { it.done.isDone() }
        if (doneList.isNotEmpty()) {
            AchievementItemSection(
                modifier = Modifier.fillMaxWidth(),
                loops = doneList,
                itemDate = itemDate,
                title = stringResource(id = R.string.done),
                backgroundColor = AppColor.primary.compositeOverSurface(
                    alpha = if (isSystemInDarkTheme()) 0.15f else 0.1f
                ),
                icon = Icons.Filled.Done,
                iconColor = AppColor.primary,
            )
        }

        val skipList = item.filter { it.done.isSkip() }
        if (skipList.isNotEmpty()) {
            AchievementItemSection(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth(),
                loops = skipList,
                itemDate = itemDate,
                title = stringResource(id = R.string.skip),
                backgroundColor = compositeOverSurface(),
                icon = Icons.Filled.Clear,
                iconColor = AppColor.onSurface,
            )
        }

        if (doneList.isEmpty() && skipList.isEmpty()) {
            NoAchievementItemSection()
        }
    }
}

@Composable
private fun AchievementItemDateHeader(
    modifier: Modifier = Modifier,
    itemDate: LocalDate
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = itemDate.formatMonthDateDay(),
        style = AppTypography.headlineSmall.copy(
            color = AppColor.onSurface,
            textAlign = TextAlign.Center,
        )
    )
}

@Composable
private fun NoAchievementItemSection(
    modifier: Modifier = Modifier
) {
    val color = AppColor.onSurface.copy(alpha = 0.6f)
    Text(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = Stroke(
                    width = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
                drawRoundRect(
                    color = color,
                    style = stroke
                )
            }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        text = stringResource(id = R.string.no_achievements),
        style = AppTypography.bodyLarge.copy(
            color = color
        )
    )
}

@Composable
private fun AchievementItemSection(
    modifier: Modifier = Modifier,
    loops: List<FullLoopVo>,
    itemDate: LocalDate,
    title: String,
    backgroundColor: Color,
    icon: ImageVector,
    iconColor: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundShapes.small)
            .background(backgroundColor)
            .padding(vertical = 12.dp)
    ) {
        AchievementItemSectionHeader(
            modifier = Modifier.padding(
                start = 8.dp,
                bottom = 4.dp
            ),
            title = title,
            itemCount = loops.size,
            icon = icon,
            iconColor = iconColor
        )
        loops.forEach { loop ->
            key(loop.id) {
                AchievementItemSectionBody(
                    loop = loop,
                    itemDate = itemDate,
                )
            }
        }
    }
}

@Composable
private fun AchievementItemSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    itemCount: Int,
    icon: ImageVector,
    iconColor: Color,
) {
    Row(
        modifier = modifier
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "($itemCount) $title",
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(12.dp),
            imageVector = icon,
            colorFilter = ColorFilter.tint(color = iconColor),
            contentDescription = title
        )
    }
}

@Composable
private fun AchievementItemSectionBody(
    modifier: Modifier = Modifier,
    loop: FullLoopVo,
    itemDate: LocalDate,
) {

    Column(
        modifier = modifier.padding(
            start = 16.dp,
            top = 8.dp
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = loop.color.compositeOverOnSurface(),
                        shape = CircleShape
                    )
            )

            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = loop.title,
                style = AppTypography.bodyLarge.copy(
                    color = AppColor.onSurface
                )
            )

            val createdDate = remember(loop.created) { loop.created.toLocalDate() }
            if (itemDate == createdDate) {
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = "[${stringResource(id = R.string.created)}]",
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.error.copy(alpha = 0.7f)
                    )
                )
            }
        }

        if (loop.retrospect.isNotEmpty()) {
            Retrospect(
                modifier = Modifier.padding(
                    top = 12.dp,
                    start = 24.dp,
                    bottom = 12.dp
                ),
                retrospect = loop.retrospect
            )
        }
    }
}

@Composable
private fun Retrospect(
    modifier: Modifier = Modifier,
    retrospect: String
) {
    Text(
        modifier = modifier,
        text = retrospect,
        style = AppTypography.bodyMedium.copy(color = AppColor.onSurface.copy(alpha = 0.7f))
    )
}
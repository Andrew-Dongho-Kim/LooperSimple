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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.common.findLastFullyVisibleItemIndex
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun DailyAchievementPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
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
                    AppBarDateIcon(
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DailyAchievementPageContent(
                achievementViewModel = achievementViewModel,
                selectedDate = selectedDate,
                onSelectedDate = { date -> selectedDate = date }
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
                .size(21.dp)
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
            style = AppTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyAchievementPageContent(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel,
    selectedDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit
) {
    Column(modifier = modifier) {
        val allLoopsWithDoneStates by achievementViewModel
            .flowAllLoopsWithDoneStates
            .collectAsState(initial = null)

        val minCreatedDate = remember(allLoopsWithDoneStates) {
            allLoopsWithDoneStates?.minOfOrNull {
                it.loop.created
            }?.toLocalDate() ?: LocalDate.now()
        }

        val pagerState = rememberPagerState {
            ChronoUnit.MONTHS.between(
                minCreatedDate.withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(1),
            ).toInt() + 1
        }
        val lazyListState = rememberLazyListState()

        UpdateSelectedDate(
            pagerState = pagerState,
            lazyListState = lazyListState,
            minDate = minCreatedDate,
            onSelectDate = onSelectedDate
        )

        Calendar(
            modifier = Modifier.weight(1f),
            pagerState = pagerState,
            selectedDate = selectedDate,
            onSelectDate = onSelectedDate
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
    onSelectDate: (LocalDate) -> Unit,
) {
    var savedCalendarPage by remember { mutableStateOf(pagerState.targetPage) }
    if (lazyListState.isScrollInProgress) {
        val index by remember {
            derivedStateOf { lazyListState.findLastFullyVisibleItemIndex() }
        }
        val selectedDate = minDate.plusDays(index.toLong())

        onSelectDate(selectedDate)
        LaunchedEffect(key1 = selectedDate.withDayOfMonth(1)) {
            val page = ChronoUnit.MONTHS.between(
                selectedDate.withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(1),
            ).toInt()

            savedCalendarPage = page
            pagerState.animateScrollToPage(page)
        }
    } else {
        if (savedCalendarPage == pagerState.targetPage) return
        savedCalendarPage = pagerState.targetPage

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

        onSelectDate(selectedDate)
        LaunchedEffect(key1 = selectedDate) {
            val index = (selectedDate.toEpochDay() - minDate.toEpochDay()).toInt()
            lazyListState.scrollToItem(index)
        }

    }
}

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
                modifier = Modifier.padding(top = 12.dp),
                item = item
            )
        }
    }
}

@Composable
private fun AchievementItem(
    modifier: Modifier = Modifier,
    item: List<LoopWithDone>
) {
    Column(modifier = modifier) {
        val itemDate = item[0].date.toLocalDate()
        AchievementItemDateHeader(
            modifier = Modifier
                .padding(top = 22.dp)
                .padding(bottom = 12.dp),
            itemDate = itemDate,
        )

        val doneList = item.filter { it.done == LoopDoneVo.DoneState.DONE }
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

        val skipList = item.filter { it.done == LoopDoneVo.DoneState.SKIP }
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
private fun AchievementItemSection(
    modifier: Modifier = Modifier,
    loops: List<LoopWithDone>,
    itemDate: LocalDate,
    title: String,
    backgroundColor: Color,
    icon: ImageVector,
    iconColor: Color,
) {
    Column(
        modifier = modifier
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
    loop: LoopBase,
    itemDate: LocalDate,
) {
    Row(
        modifier = modifier.padding(
            start = 16.dp,
            top = 8.dp
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = loop.title,
            style = AppTypography.bodyMedium.copy(
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

}
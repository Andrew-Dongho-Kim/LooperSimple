package com.pnd.android.loop.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.sp
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
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
            selectedDate = date
            coroutineScope.launch {
                // Scroll to the tapped date, not the previously selected one.
                val pos = calculateListItemPosition(
                    itemCount = lazyListState.layoutInfo.totalItemsCount,
                    selectedDate = date,
                )
                if (pos < 0) return@launch
                lazyListState.scrollToItem(pos)
                pagerState.scrollToPage(calculateTargetCalendarPage(date))
            }
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
                .padding(horizontal = Dimens.screenHorizontalPadding),
            lazyListState = lazyListState,
            achievementViewModel = achievementViewModel,
        )
    }
}

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
        val pos = calculateListItemPosition(
            itemCount = lazyListState.layoutInfo.totalItemsCount,
            selectedDate = selectedDate,
        )
        if (pos < 0) return@LaunchedEffect
        lazyListState.scrollToItem(pos)
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
                modifier = Modifier.padding(vertical = Dimens.contentPadding),
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
            modifier = Modifier.padding(
                top = Dimens.sectionSpacing,
                bottom = Dimens.contentPadding
            ),
            itemDate = itemDate,
        )

        val doneList = item.filter { it.done.isDone() }
        if (doneList.isNotEmpty()) {
            AchievementItemSection(
                modifier = Modifier.fillMaxWidth(),
                loops = doneList,
                itemDate = itemDate,
                title = stringResource(id = R.string.done),
                icon = Icons.Filled.Done,
                accentColor = AppColor.primary,
            )
        }

        val skipList = item.filter { it.done.isSkip() }
        if (skipList.isNotEmpty()) {
            AchievementItemSection(
                modifier = Modifier
                    .padding(top = Dimens.cardSpacing)
                    .fillMaxWidth(),
                loops = skipList,
                itemDate = itemDate,
                title = stringResource(id = R.string.skip),
                icon = Icons.Filled.Clear,
                accentColor = AppColor.onSurface.copy(alpha = 0.6f),
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
    val isToday = itemDate == LocalDate.now()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateHeaderRule(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.padding(horizontal = Dimens.contentPadding),
            text = itemDate.formatMonthDateDay(),
            style = AppTypography.titleMedium.copy(
                color = if (isToday) AppColor.primary else AppColor.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        )
        DateHeaderRule(modifier = Modifier.weight(1f))
    }
}

/** 날짜 헤더 좌우로 뻗는 머리카락 굵기의 구분선. */
@Composable
private fun DateHeaderRule(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = AppColor.onSurface.copy(alpha = 0.12f),
    )
}

@Composable
private fun NoAchievementItemSection(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.large)
            .background(AppColor.surfaceContainer)
            .padding(vertical = Dimens.contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.no_achievements),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun AchievementItemSection(
    modifier: Modifier = Modifier,
    loops: List<FullLoopVo>,
    itemDate: LocalDate,
    title: String,
    icon: ImageVector,
    accentColor: Color,
) {
    Column(
        modifier = modifier
            .clip(RoundShapes.large)
            .background(AppColor.surfaceContainer)
            .padding(
                horizontal = Dimens.contentPadding,
                vertical = Dimens.contentPadding - 4.dp
            )
    ) {
        AchievementItemSectionHeader(
            title = title,
            itemCount = loops.size,
            icon = icon,
            accentColor = accentColor,
        )
        loops.forEach { loop ->
            key(loop.loopId) {
                AchievementItemSectionBody(
                    modifier = Modifier.padding(top = Dimens.cardSpacing),
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
    accentColor: Color,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.itemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionIconChip(icon = icon, accentColor = accentColor, contentDescription = title)
        Text(
            modifier = Modifier.padding(start = Dimens.itemSpacing + 2.dp),
            text = title,
            style = AppTypography.titleSmall.copy(color = AppColor.onSurface)
        )
        CountBadge(
            modifier = Modifier.padding(start = Dimens.itemSpacing),
            count = itemCount,
            accentColor = accentColor,
        )
    }
}

/** 섹션 제목 앞에 놓이는, 액센트 색이 옅게 깔린 원형 아이콘 칩. */
@Composable
private fun SectionIconChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accentColor: Color,
    contentDescription: String,
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(color = accentColor.copy(alpha = 0.15f), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier.size(12.dp),
            imageVector = icon,
            colorFilter = ColorFilter.tint(color = accentColor),
            contentDescription = contentDescription,
        )
    }
}

/** 항목 개수를 보여주는 알약형 배지. */
@Composable
private fun CountBadge(
    modifier: Modifier = Modifier,
    count: Int,
    accentColor: Color,
) {
    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        text = "$count",
        style = AppTypography.labelMedium.copy(color = accentColor),
    )
}

@Composable
private fun AchievementItemSectionBody(
    modifier: Modifier = Modifier,
    loop: FullLoopVo,
    itemDate: LocalDate,
) {
    val loopColor = loop.color.compositeOverOnSurface()
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = loopColor, shape = CircleShape)
            )
            Text(
                modifier = Modifier.padding(start = Dimens.contentPadding),
                text = loop.title,
                style = AppTypography.bodyLarge.copy(color = AppColor.onSurface)
            )

            val createdDate = remember(loop.created) { loop.created.toLocalDate() }
            if (itemDate == createdDate) {
                CreatedBadge(modifier = Modifier.padding(start = Dimens.itemSpacing))
            }
        }

        if (loop.retrospect.isNotEmpty()) {
            Retrospect(
                // 좌측 점/제목 들여쓰기(점 8dp + 간격)에 맞춰 회고 블록을 정렬한다.
                modifier = Modifier.padding(
                    start = Dimens.contentPadding + 8.dp,
                    top = Dimens.cardSpacing,
                    end = Dimens.itemSpacing,
                ),
                accentColor = loopColor,
                retrospect = loop.retrospect,
            )
        }
    }
}

/** 해당 날짜에 처음 만들어진 루프임을 알리는 알약형 배지. */
@Composable
private fun CreatedBadge(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(AppColor.error.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        text = stringResource(id = R.string.created),
        style = AppTypography.labelSmall.copy(
            color = AppColor.error.copy(alpha = 0.9f)
        )
    )
}

/**
 * 회고 메모. 루프 색의 얇은 좌측 액센트 바와 같은 색의 옅은 틴트 배경으로
 * 어떤 루프의 회고인지 시각적으로 연결한다. 액센트는 모두 반투명이라
 * 라이트/다크 카드 위 어디서나 자연스럽게 얹힌다.
 */
@Composable
private fun Retrospect(
    modifier: Modifier = Modifier,
    accentColor: Color,
    retrospect: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(accentColor.copy(alpha = 0.06f))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accentColor.copy(alpha = 0.5f))
        )
        Text(
            modifier = Modifier.padding(
                start = Dimens.contentPadding - 4.dp,
                end = Dimens.contentPadding,
                top = Dimens.cardSpacing,
                bottom = Dimens.cardSpacing,
            ),
            text = retrospect,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )
        )
    }
}
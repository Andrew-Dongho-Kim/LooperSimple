package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.input.selector.TimeStepperCard
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.h2m2
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalDateTime

/** anytime 시트에 평소 소요시간과 함께 늘 내놓는 길이(분). */
private val ANYTIME_PRESET_MINUTES = listOf(10, 30, 60)

/** 직접 입력을 펼쳤을 때 anytime 의 시작 칸을 채울 길이. 평소 소요시간이 없을 때만 쓴다. */
private const val DEFAULT_MANUAL_MINUTES = 30

/** 완료 기록 시트의 칩 하나. 칩을 누르는 순간 [times] 로 저장한다. null 이면 시각 없이 완료. */
internal sealed interface RecordDoneOption {
    val times: Pair<Long, Long>?

    data class Usual(val minutes: Int, override val times: Pair<Long, Long>) : RecordDoneOption
    data class Minutes(val minutes: Int, override val times: Pair<Long, Long>) : RecordDoneOption
    data class AsPlanned(override val times: Pair<Long, Long>) : RecordDoneOption
    data class FinishedNow(override val times: Pair<Long, Long>) : RecordDoneOption
    data object NoTime : RecordDoneOption {
        override val times: Pair<Long, Long>? get() = null
    }
}

/**
 * 시트에 내놓을 칩. 맨 앞 칩이 가장 그럴듯한 답이라 강조된다.
 *
 * - anytime: 지금 끝낸 것으로 보고 길이만 고르게 한다. 평소 → 10분/30분/1시간 → 시간 없이.
 * - 시간제: 계획대로 → (시간창 안이면) 지금 끝남 → 평소 길이(계획 시작부터).
 */
internal fun recordDoneOptions(
    loop: LoopBase,
    nowInDay: Long,
    isInWindow: Boolean,
    usualMinutes: Int?,
): List<RecordDoneOption> {
    fun backFromNow(minutes: Int) = Math.floorMod(nowInDay - minutes * MS_1MIN, MS_1DAY) to nowInDay

    if (loop.isAnyTime) return buildList {
        usualMinutes?.let { add(RecordDoneOption.Usual(it, backFromNow(it))) }
        ANYTIME_PRESET_MINUTES.filter { it != usualMinutes }
            .forEach { add(RecordDoneOption.Minutes(it, backFromNow(it))) }
        add(RecordDoneOption.NoTime)
    }

    val start = loop.startInDay
    val end = loop.endInDay
    if (start !in 0 until MS_1DAY || end !in 0 until MS_1DAY) return emptyList()
    return buildList {
        add(RecordDoneOption.AsPlanned(start to end))
        if (isInWindow && nowInDay != start) add(RecordDoneOption.FinishedNow(start to nowInDay))
        usualMinutes?.let { add(RecordDoneOption.Usual(it, start to Math.floorMod(start + it * MS_1MIN, MS_1DAY))) }
    }
}

/** "25분", "1시간", "1시간 30분". */
internal fun Context.durationText(minutes: Int): String = when {
    minutes < 60 -> getString(R.string.duration_minutes, minutes)
    minutes % 60 == 0 -> getString(R.string.duration_hours, minutes / 60)
    else -> getString(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}

/** "07:00–07:40". 스낵바 문구도 만들므로 Compose 밖에서 쓸 수 있는 포맷을 쓴다. */
internal fun Pair<Long, Long>.rangeText(): String = "${h2m2(first)}–${h2m2(second)}"

/**
 * 완료 기록 시트. 카드 메뉴의 '완료로 기록'과 ✓ 뒤 스낵바의 '시간 수정'이 함께 쓴다.
 *
 * 칩 하나를 누르면 곧바로 저장하고 닫힌다. 칩으로 안 되는 경우만 '직접 입력'을 펼쳐 시작·종료를 고른다.
 *
 * @param onRecord 저장할 시각. null 이면 시각 없이 완료(anytime 의 '시간 없이').
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordDoneSheet(
    loop: LoopBase,
    usualMinutes: Int?,
    onRecord: (times: Pair<Long, Long>?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now() }
    val nowInDay = now.toLocalTime().toMs()
    val options = remember(loop, usualMinutes) {
        recordDoneOptions(loop, nowInDay, isInWindow = loop.isActive(now), usualMinutes = usualMinutes)
    }
    val record: (Pair<Long, Long>?) -> Unit = { times ->
        onRecord(times)
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColor.surfaceElevated,
        contentColor = AppColor.onSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenHorizontalPadding)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(
                        id = if (loop.isAnyTime) R.string.record_sheet_title_anytime else R.string.record_sheet_title,
                        loop.title,
                    ),
                    style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.cancel), tint = AppColor.onSurface)
                }
            }
            if (loop.isAnyTime) {
                Text(
                    text = stringResource(R.string.record_sheet_hint_anytime, h2m2(nowInDay)),
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
                )
            }

            if (options.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEachIndexed { index, option ->
                        RecordDoneChip(
                            text = option.label(context),
                            emphasized = index == 0,
                            onClick = { record(option.times) },
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 20.dp),
                    color = AppColor.onSurface.copy(alpha = 0.08f),
                )
            }
            ManualTimes(
                loop = loop,
                nowInDay = nowInDay,
                usualMinutes = usualMinutes,
                // 칩이 없으면(계획 시각을 알 수 없는 경우) 입력이 이 시트의 전부이므로 접지 않는다.
                collapsible = options.isNotEmpty(),
                onRecord = record,
            )
        }
    }
}

private fun RecordDoneOption.label(context: Context): String = when (this) {
    is RecordDoneOption.Usual -> context.getString(R.string.record_option_usual, context.durationText(minutes))
    is RecordDoneOption.Minutes -> context.durationText(minutes)
    is RecordDoneOption.AsPlanned -> context.getString(R.string.record_option_as_planned, times.rangeText())
    is RecordDoneOption.FinishedNow -> context.getString(R.string.record_option_finished_now, times.rangeText())
    RecordDoneOption.NoTime -> context.getString(R.string.record_option_no_time)
}

/** 맨 앞(가장 그럴듯한) 칩만 primary 틴트로 세우고 나머지는 헤어라인 칩으로 둔다. */
@Composable
private fun RecordDoneChip(
    text: String,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    val shape = CircleShape
    Text(
        modifier = Modifier
            .clip(shape)
            .then(
                if (emphasized) Modifier.background(AppColor.primary.copy(alpha = 0.12f))
                else Modifier.border(1.dp, AppColor.outlineVariant, shape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        text = text,
        style = AppTypography.bodyMedium.copy(
            color = if (emphasized) AppColor.primary else AppColor.onSurface.copy(alpha = 0.85f),
        ),
    )
}

/**
 * 칩으로 안 되는 경우를 위한 시작·종료 직접 입력. 칩이 있으면 접어 두어([collapsible]) 시트를 칩 중심으로 가볍게 둔다.
 * 기존 '완료로 기록' 다이얼로그와 같은 시각 입력(TimeStepperCard)과 같은 검증(종료 ≥ 시작)을 쓴다.
 *
 * anytime 의 기본값은 지금 끝난 것으로 두고, 시작은 평소 소요시간(없으면 30분)만큼 앞으로 채운다.
 */
@Composable
private fun ManualTimes(
    loop: LoopBase,
    nowInDay: Long,
    usualMinutes: Int?,
    collapsible: Boolean,
    onRecord: (Pair<Long, Long>) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(!collapsible) }
    val initialStart = if (loop.startInDay >= 0) {
        loop.startInDay
    } else {
        (nowInDay - (usualMinutes ?: DEFAULT_MANUAL_MINUTES) * MS_1MIN).coerceAtLeast(0)
    }
    var startMs by rememberSaveable { mutableLongStateOf(initialStart) }
    var endMs by rememberSaveable { mutableLongStateOf(if (loop.endInDay >= 0) loop.endInDay else nowInDay) }
    val isValidRange = endMs >= startMs

    if (collapsible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.record_sheet_manual),
                style = AppTypography.bodyMedium.copy(color = AppColor.onSurface.copy(alpha = 0.7f)),
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = AppColor.onSurface.copy(alpha = 0.5f),
            )
        }
    }
    if (!expanded) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (collapsible) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TimeStepperCard(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.start),
            localTime = startMs.toLocalTime(),
            enabled = true,
            onTimeChanged = { startMs = it.toMs() },
        )
        TimeStepperCard(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.end),
            localTime = endMs.toLocalTime(),
            enabled = true,
            onTimeChanged = { endMs = it.toMs() },
        )
    }
    if (!isValidRange) {
        Text(
            modifier = Modifier.padding(top = 10.dp),
            text = stringResource(id = R.string.loop_record_done_invalid_time),
            style = AppTypography.labelMedium.copy(color = AppColor.error),
        )
    }
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        enabled = isValidRange,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColor.primary,
            contentColor = AppColor.onPrimary,
        ),
        onClick = { onRecord(startMs to endMs) },
    ) {
        Text(text = stringResource(id = R.string.save))
    }
}

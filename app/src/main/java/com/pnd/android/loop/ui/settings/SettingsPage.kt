package com.pnd.android.loop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.HabitualStartShift
import com.pnd.android.loop.alarm.notification.IN_PROGRESS_REMIND_CHOICES
import com.pnd.android.loop.alarm.notification.IN_PROGRESS_REMIND_OFF
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.divider
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import java.util.Locale

/**
 * 알림 설정 화면.
 *
 * 안드로이드 알림 채널 설정으로 되는 것(종류별 켜기·끄기, 소리, 진동, 중요도)은 여기 두지 않고
 * 맨 아래에서 시스템 설정으로 보낸다. 여기 있는 항목은 시스템이 대신할 수 없는, 이 앱만 아는
 * 값들뿐이다([NotificationSettings] 참고).
 */
@Composable
fun SettingsPage(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val preferences by viewModel.preferences.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColor.background),
    ) {
        SimpleAppBar(
            title = stringResource(id = R.string.settings),
            onNavigateUp = onNavigateUp,
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenHorizontalPadding),
        ) {
            SettingsSectionTitle(text = stringResource(id = R.string.settings_section_habit))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_anytime_due_title),
                description = stringResource(id = R.string.settings_anytime_due_desc),
                checked = preferences.anyTimeDueEnabled,
                onCheckedChange = viewModel::setAnyTimeDueEnabled,
            )

            // 습관 알림을 끈 상태에서 시각 보정을 보여주면 무엇에 영향을 주는지 알 수 없다.
            SettingsChoiceRow(
                title = stringResource(id = R.string.settings_habitual_shift_title),
                description = stringResource(id = R.string.settings_habitual_shift_desc),
                enabled = preferences.anyTimeDueEnabled,
                choices = HabitualStartShift.entries,
                selected = preferences.habitualStartShift,
                labelOf = { shift -> stringResource(id = shift.labelResId()) },
                onSelected = viewModel::setHabitualStartShift,
            )

            SettingsDivider()

            SettingsSectionTitle(text = stringResource(id = R.string.settings_section_quiet))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_quiet_hours_title),
                description = stringResource(id = R.string.settings_quiet_hours_desc),
                checked = preferences.quietHoursEnabled,
                onCheckedChange = viewModel::setQuietHoursEnabled,
            )

            QuietHoursRow(
                enabled = preferences.quietHoursEnabled,
                startHour = preferences.quietHoursStartHour,
                endHour = preferences.quietHoursEndHour,
                onChanged = viewModel::setQuietHours,
            )

            SettingsDivider()

            SettingsSectionTitle(text = stringResource(id = R.string.settings_section_in_progress))

            SettingsChoiceRow(
                title = stringResource(id = R.string.settings_in_progress_remind_title),
                description = stringResource(id = R.string.settings_in_progress_remind_desc),
                choices = IN_PROGRESS_REMIND_CHOICES,
                selected = preferences.inProgressRemindIntervalHours,
                labelOf = { hours -> remindIntervalLabel(hours) },
                onSelected = viewModel::setInProgressRemindIntervalHours,
            )

            SettingsDivider()

            SettingsSectionTitle(text = stringResource(id = R.string.settings_section_system))

            SettingsNavigationRow(
                title = stringResource(id = R.string.settings_open_system_notification),
                description = stringResource(id = R.string.settings_open_system_notification_desc),
                onClick = { context.openAppNotificationSettings() },
            )

            Spacer(modifier = Modifier.height(Dimens.sectionSpacing))
        }
    }
}

/** 진행 중 리마인드 간격 라벨. 0은 "끔". */
@Composable
private fun remindIntervalLabel(hours: Int): String =
    if (hours == IN_PROGRESS_REMIND_OFF) {
        stringResource(id = R.string.settings_in_progress_remind_off)
    } else {
        stringResource(id = R.string.settings_in_progress_remind_hours, hours)
    }

private fun HabitualStartShift.labelResId() = when (this) {
    HabitualStartShift.EARLIER -> R.string.settings_habitual_shift_earlier
    HabitualStartShift.USUAL -> R.string.settings_habitual_shift_usual
    HabitualStartShift.LATER -> R.string.settings_habitual_shift_later
}

@Composable
private fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(top = Dimens.sectionSpacing, bottom = Dimens.itemSpacing),
        text = text,
        style = AppTypography.labelLarge.copy(
            color = AppColor.primary,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Dimens.sectionSpacing)
            .height(1.dp)
            .background(color = AppColor.divider),
    )
}

/** 제목 + 설명 한 쌍. 모든 설정 행이 같은 위계로 읽히도록 공통으로 쓴다. */
@Composable
private fun SettingsRowLabel(
    title: String,
    description: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    // 비활성 행은 텍스트를 흐리게 해서 "지금은 영향이 없다"를 색으로 알린다.
    val titleAlpha = if (enabled) 1f else 0.4f
    val descriptionAlpha = if (enabled) 0.6f else 0.3f

    Column(modifier = modifier) {
        Text(
            text = title,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = titleAlpha),
                fontWeight = FontWeight.Medium,
            ),
        )
        if (description != null) {
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = description,
                style = AppTypography.labelMedium.copy(
                    color = AppColor.onSurface.copy(alpha = descriptionAlpha),
                ),
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowLabel(
            modifier = Modifier.weight(1f),
            title = title,
            description = description,
            enabled = true,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColor.onPrimary,
                checkedTrackColor = AppColor.primary,
            ),
        )
    }
}

/**
 * 값 몇 개 중 하나를 고르는 행. 칩을 한 줄로 늘어놓아 현재 값과 선택지를 한눈에 보여준다.
 * (드롭다운이면 지금 값 말고 무엇을 고를 수 있는지 열어봐야 알 수 있다)
 */
@Composable
private fun <T> SettingsChoiceRow(
    title: String,
    description: String?,
    choices: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier = modifier.padding(vertical = 12.dp)) {
        SettingsRowLabel(title = title, description = description, enabled = enabled)
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                SettingsChoiceChip(
                    label = labelOf(choice),
                    isSelected = choice == selected,
                    enabled = enabled,
                    onClick = { onSelected(choice) },
                )
            }
        }
    }
}

@Composable
private fun SettingsChoiceChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.4f
    val backgroundColor = if (isSelected) {
        AppColor.primary.copy(alpha = 0.14f * alpha)
    } else {
        AppColor.surfaceContainer.copy(alpha = alpha)
    }
    val textColor = if (isSelected) {
        AppColor.primary.copy(alpha = alpha)
    } else {
        AppColor.onSurface.copy(alpha = 0.6f * alpha)
    }

    Text(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(color = backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        text = label,
        style = AppTypography.labelLarge.copy(
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        ),
    )
}

/**
 * 방해 금지 구간(시 단위)을 고르는 행. 분 단위까지 필요한 설정이 아니라 시간만 오르내리게 한다.
 * 시작이 종료보다 커도 되며, 그때는 자정을 넘는 구간으로 해석된다(예: 23시 ~ 6시).
 */
@Composable
private fun QuietHoursRow(
    enabled: Boolean,
    startHour: Int,
    endHour: Int,
    onChanged: (startHour: Int, endHour: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HourStepper(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.settings_quiet_hours_start),
            hour = startHour,
            enabled = enabled,
            onHourChanged = { hour -> onChanged(hour, endHour) },
        )
        HourStepper(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.settings_quiet_hours_end),
            hour = endHour,
            enabled = enabled,
            onHourChanged = { hour -> onChanged(startHour, hour) },
        )
    }
}

@Composable
private fun HourStepper(
    label: String,
    hour: Int,
    enabled: Boolean,
    onHourChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.4f

    Column(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(color = AppColor.surfaceContainer.copy(alpha = alpha))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f * alpha),
            ),
        )
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 0시에서 한 칸 내리면 23시로, 23시에서 올리면 0시로 감긴다(시각은 원형이다).
            HourStepperButton(
                imageVector = Icons.Rounded.Remove,
                descriptionResId = R.string.settings_quiet_hours_decrease,
                enabled = enabled,
                onClick = { onHourChanged((hour + 23) % 24) },
            )
            Text(
                modifier = Modifier.weight(1f),
                text = String.format(Locale.getDefault(), "%02d:00", hour),
                style = AppTypography.titleMedium.copy(
                    color = AppColor.onSurface.copy(alpha = alpha),
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            HourStepperButton(
                imageVector = Icons.Rounded.Add,
                descriptionResId = R.string.settings_quiet_hours_increase,
                enabled = enabled,
                onClick = { onHourChanged((hour + 1) % 24) },
            )
        }
    }
}

@Composable
private fun HourStepperButton(
    imageVector: ImageVector,
    descriptionResId: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color = AppColor.primary.copy(alpha = 0.12f * alpha))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = imageVector,
            tint = AppColor.primary.copy(alpha = alpha),
            contentDescription = stringResource(id = descriptionResId),
        )
    }
}

/** 다른 화면(시스템 설정)으로 나가는 행. */
@Composable
private fun SettingsNavigationRow(
    title: String,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsRowLabel(
            modifier = Modifier.weight(1f),
            title = title,
            description = description,
            enabled = true,
        )
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            tint = AppColor.onSurface.copy(alpha = 0.4f),
            contentDescription = null,
        )
    }
}

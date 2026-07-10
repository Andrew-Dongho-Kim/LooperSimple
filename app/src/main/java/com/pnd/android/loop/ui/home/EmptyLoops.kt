package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

/**
 * 루프가 하나도 없을 때 보여주는 OOBE(첫 실행) 빈 화면.
 *
 * 위쪽은 앱의 다른 빈 상태("오늘 완료" 등)와 똑같은 공용 [HomeEmptyState]를 그대로 써서
 * 이질감 없이 이어지고, 아래쪽 "빠른 시작"에서는 추천 루프를 **실제 루프 카드와 같은 문법**
 * (색 도트 + 제목 + 요일 + 시간 알약)으로 보여준다. 카드를 누르면 [onSelectTemplate]로
 * 미리 채워진 루프가 전달되어 그대로 추가된다 — 첫 루프를 한 번에 시작할 수 있다.
 *
 * 색·모서리·여백은 모두 앱 토큰([AppColor], [LoopCardShape], [Dimens])에서 가져와
 * 라이트/다크 모드와 기존 화면에 함께 대응한다.
 */
@Composable
fun EmptyLoops(
    modifier: Modifier = Modifier,
    onSelectTemplate: (LoopBase) -> Unit,
    onCreateManually: () -> Unit,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 상단: 공용 빈 상태(틴트 원 안 아이콘 + 제목 + 힌트)를 재사용한다.
        HomeEmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenHorizontalPadding),
            icon = Icons.Outlined.AllInclusive,
            title = stringResource(R.string.oobe_title),
            hint = stringResource(R.string.oobe_subtitle),
        )

        Spacer(modifier = Modifier.height(36.dp))

        // "빠른 시작" 섹션 헤더 — 다른 섹션 헤더와 동일한 titleSmall 스타일.
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenHorizontalPadding)
                .padding(bottom = 12.dp),
            text = stringResource(R.string.oobe_quickstart_label),
            style = AppTypography.titleSmall.copy(color = AppColor.onSurface),
        )

        // 추천 루프 카드 목록 — 실제 루프 카드와 같은 간격/여백으로 쌓는다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
        ) {
            LoopTemplates.forEach { template ->
                // 제목은 리소스 문자열이라 Composable 컨텍스트에서 읽어 루프에 채운다.
                val title = stringResource(template.titleRes)
                val loop = remember(template, title) { template.toLoop(title = title) }
                TemplateLoopCard(
                    loop = loop,
                    onClick = { onSelectTemplate(loop) },
                )
            }

            // 추천 대신 원하는 루프를 직접 만들 수 있는 진입점. 입력 편집기를 연다.
            ManualAddRow(onClick = onCreateManually)
        }

        // 화면 하단에 떠 있는 입력 바에 콘텐츠가 가리지 않도록 여백을 둔다.
        Spacer(modifier = Modifier.height(140.dp))
    }
}

/**
 * 추천 루프 한 장. 실제 [Loop] 카드와 동일한 표면/모서리/테두리와 "색 도트 + 제목 + 요일 +
 * 시간 알약" 구성을 그대로 따르되, 아직 추가 전이므로 관리 메뉴 대신 추가(+) 아이콘을 둔다.
 * 행 전체가 클릭 영역이라 한 번 눌러 루프를 바로 추가할 수 있다.
 */
@Composable
private fun TemplateLoopCard(
    loop: LoopBase,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LoopCardShape)
            .background(AppColor.surfaceElevated)
            .border(
                width = 1.dp,
                color = AppColor.onSurface.copy(alpha = 0.08f),
                shape = LoopCardShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 좌측 식별 색 도트 — 실제 카드와 동일하게 20dp 자리 안에 10dp 점을 둔다.
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(loop.color.compositeOverOnSurface()),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = loop.title,
                style = AppTypography.bodyMedium.copy(
                    // 제목 톤(0.9)은 실제 카드와 맞춘다.
                    color = AppColor.onSurface.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // 요일 표기는 실제 카드의 공용 컴포넌트를 그대로 재사용한다("매일/주중" 또는 요일 글자).
            LoopCardActiveDays(
                modifier = Modifier.padding(top = 3.dp),
                loop = loop,
            )
        }

        TemplateTimeChip(
            modifier = Modifier.padding(start = 8.dp),
            loop = loop,
        )

        Icon(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(20.dp),
            imageVector = Icons.Outlined.Add,
            tint = AppColor.primary,
            contentDescription = stringResource(R.string.add),
        )
    }
}

/**
 * 추천이 아닌 원하는 루프를 직접 만드는 진입점. 추천 카드와 구분되도록 배경 없이 테두리만 둔
 * '고스트' 행으로, 가운데에 primary 색의 추가 아이콘 + 라벨을 둔다. 누르면 입력 편집기를 연다.
 */
@Composable
private fun ManualAddRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LoopCardShape)
            .border(
                width = 1.dp,
                color = AppColor.primary.copy(alpha = 0.4f),
                shape = LoopCardShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Outlined.Add,
            tint = AppColor.primary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.oobe_create_own),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.primary,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

/**
 * 시간 창을 보여주는 알약 — 실제 카드의 시간 칩과 같은 표면/톤을 쓴다.
 * '언제든지' 루프는 시간 대신 [R.string.anytime]을 표시한다.
 */
@Composable
private fun TemplateTimeChip(
    loop: LoopBase,
    modifier: Modifier = Modifier,
) {
    // 시간이 없는(ANY_TIME, -1) 루프는 시간을 포맷하면 크래시하므로, isAnyTime 뿐 아니라
    // 음수 시작/종료도 '언제든지'로 취급해 방어한다.
    val isAnyTime = loop.isAnyTime || loop.startInDay < 0 || loop.endInDay < 0
    val text = if (isAnyTime) {
        stringResource(id = R.string.anytime)
    } else {
        "${loop.startInDay.formatHourMinute(withAmPm = false)} – " +
            loop.endInDay.formatHourMinute(withAmPm = false)
    }

    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = AppTypography.labelMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.6f),
        ),
    )
}

/**
 * 빠른 시작 템플릿 정의. 화면 표시용 제목/식별 색과, 추가 시 만들 루프의 스케줄 정보를
 * 함께 담는다. [isAnyTime]이 true면 시간 없이 '언제든지' 루프로 생성한다.
 */
private data class LoopTemplate(
    val titleRes: Int,
    val color: Int,
    val activeDays: Int,
    val isAnyTime: Boolean = false,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0,
) {
    /**
     * 템플릿을 실제 추가 가능한 루프로 변환한다. 제목은 리소스에서 읽은 [title]을 받고,
     * created(생성 시각)는 추가 시점에 호출부에서 채운다.
     */
    fun toLoop(title: String): LoopBase {
        return if (isAnyTime) {
            LoopVo.anytime(
                title = title,
                color = color,
                activeDays = activeDays,
            )
        } else {
            LoopVo.create(
                title = title,
                color = color,
                startInDay = LocalTime.of(startHour, startMinute).toMs(),
                endInDay = LocalTime.of(endHour, endMinute).toMs(),
                activeDays = activeDays,
            )
        }
    }
}

// 빠른 시작 템플릿 목록. 색은 앱의 파스텔 팔레트(SUPPORTED_COLORS)에서 골랐다.
private val LoopTemplates = listOf(
    LoopTemplate(
        titleRes = R.string.oobe_tpl_water_title,
        color = 0xFF95C8EC.toInt(),
        activeDays = LoopDay.EVERYDAY,
        isAnyTime = true,
    ),
    LoopTemplate(
        titleRes = R.string.oobe_tpl_reading_title,
        color = 0xFFC7A5E2.toInt(),
        activeDays = LoopDay.EVERYDAY,
        startHour = 22, startMinute = 0,
        endHour = 22, endMinute = 30,
    ),
    LoopTemplate(
        titleRes = R.string.oobe_tpl_workout_title,
        color = 0xFFF2A98F.toInt(),
        activeDays = LoopDay.MONDAY or LoopDay.WEDNESDAY or LoopDay.FRIDAY,
        startHour = 18, startMinute = 0,
        endHour = 19, endMinute = 0,
    ),
    LoopTemplate(
        titleRes = R.string.oobe_tpl_walk_title,
        color = 0xFFA6D99C.toInt(),
        activeDays = LoopDay.EVERYDAY,
        startHour = 12, startMinute = 30,
        endHour = 13, endMinute = 0,
    ),
)

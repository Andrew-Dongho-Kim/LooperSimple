package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.appwidget.ui.LoopWidgetMedium
import com.pnd.android.loop.appwidget.ui.LoopWidgetSmall
import com.pnd.android.loop.appwidget.ui.WidgetEmptyReason
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.ui.theme.AppWidgetColorProviders

private val logger = Logger("AppWidget")

// 위젯 하나를 갱신할 때마다 크기별로 여러 번 그려지므로(SizeMode.Responsive) 한 번만 만들어 쓴다.
private val objectMapper = ObjectMapper()

class AppWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = AppWidgetColorProviders) {
                ResponsiveLoopWidget()
            }
        }
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            SIZE_SMALL,
            SIZE_MEDIUM,
            SIZE_LARGE
        )
    )

    @Composable
    private fun ResponsiveLoopWidget() {
        val content = currentState(KEY_LOOPS_JSON).toWidgetContent()

        val revision = currentState(KEY_REVISION)
        val size = LocalSize.current
        logger.i { "AppWidget updated revision:$revision, widgetSize:$size" }

        // 어떤 경우에도 화면은 반드시 그린다. 아무것도 그리지 않고 빠져나가면 위젯이 빈 채로
        // 남고, 다음 갱신이 올 때까지 그 상태로 멈춘다.
        when (size) {
            SIZE_SMALL -> LoopWidgetSmall(
                loops = content.loops,
                emptyReason = content.emptyReason,
            )

            else -> LoopWidgetMedium(
                loops = content.loops,
                emptyReason = content.emptyReason,
            )
        }
    }

    /**
     * 위젯 상태에 저장해 둔 JSON([KEY_LOOPS_JSON])을 그대로 옮겨 담는 그릇.
     * 값을 채우는 쪽은 [AppWidgetUpdateWorker] 다.
     */
    class AppWidgetData {
        /** 아직 답하지 않은 몫들. 이미 완료/건너뛴 몫은 워커에서 걸러져 여기 오지 않는다. */
        lateinit var loops: List<Map<String, Any>>

        /** 오늘 예정된 몫 전체 수(이미 답한 것 포함). */
        var total: Int = 0

        /** 등록된 루프 수(꺼져 있는 루프도 포함). */
        var registered: Int = 0
    }

    companion object {
        private val SIZE_SMALL = DpSize(54.dp, 73.dp)
        private val SIZE_MEDIUM = DpSize(110.dp, 148.dp)
        private val SIZE_LARGE = DpSize(500.dp, 600.dp)

        val KEY_LOOPS_JSON = stringPreferencesKey("key_loops_json")
        val KEY_REVISION = longPreferencesKey("key_revision")
    }
}

/** 화면이 그대로 쓸 수 있게 옮겨 둔 위젯 내용. */
private class WidgetContent(
    /**
     * 한 줄의 단위는 루프가 아니라 occurrence 다. 자정을 넘기는 루프는 어젯밤 몫과
     * 오늘 밤 몫이 나란히 올 수 있다([WidgetLoop] 참고).
     */
    val loops: List<WidgetLoop>,
    /** [loops] 가 비었을 때 띄울 안내. */
    val emptyReason: WidgetEmptyReason,
) {
    companion object {
        /**
         * 아직 그릴 데이터가 없는 상태(위젯을 막 놓았거나 저장된 JSON 이 깨진 경우).
         *
         * 데이터가 없으면 어떤 안내를 띄울지 고를 근거도 없어 "루프가 아직 없다"로 둔다.
         * 위젯을 놓는 즉시 워커가 데이터를 채우므로([AppWidgetReceiver]) 잠깐만 보인다.
         */
        val NOT_READY = WidgetContent(
            loops = emptyList(),
            emptyReason = WidgetEmptyReason.NO_LOOPS,
        )
    }
}

/**
 * 위젯 상태에 저장해 둔 JSON([AppWidget.KEY_LOOPS_JSON])을 화면이 쓰는 형태로 옮긴다.
 *
 * 옮기다 무슨 일이 있어도 예외를 밖으로 내보내지 않는다. 컴포지션이 예외로 끊기면 위젯이 통째로
 * 비고 다음 갱신까지 그대로 멈추기 때문이다. 앱을 업데이트해 예전 버전이 남긴 JSON 을 읽게 되는
 * 경우가 대표적이다(키가 없거나 값의 타입이 다르다).
 */
private fun String?.toWidgetContent(): WidgetContent {
    if (isNullOrEmpty()) return WidgetContent.NOT_READY

    return try {
        val data = objectMapper.readValue(this, AppWidget.AppWidgetData::class.java)
        WidgetContent(
            loops = data.loops.map { it.asWidgetLoop() },
            emptyReason = data.emptyReason(),
        )
    } catch (e: Exception) {
        logger.e { "Parse failed:$this, exception:$e" }
        WidgetContent.NOT_READY
    }
}

/**
 * 띄울 몫이 없을 때 어떤 안내를 보여줄지. 넓은 쪽에서 좁은 쪽으로 좁혀 가며 판정한다.
 *  - 오늘 몫이 있었는데 [AppWidget.AppWidgetData.loops] 가 비었다 → 오늘 것을 다 답했다
 *  - 오늘 몫 자체가 없었지만 루프는 있다 → 오늘이 그 루프들의 날이 아닐 뿐이다
 *  - 루프도 없다 → 아직 시작 전이다
 */
private fun AppWidget.AppWidgetData.emptyReason(): WidgetEmptyReason = when {
    total > 0 -> WidgetEmptyReason.ALL_DONE
    registered > 0 -> WidgetEmptyReason.NOTHING_SCHEDULED
    else -> WidgetEmptyReason.NO_LOOPS
}
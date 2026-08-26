package com.pnd.android.loop.appwidget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.pnd.android.loop.appwidget.AppWidget.Companion.KEY_LOOPS_JSON
import com.pnd.android.loop.appwidget.AppWidget.Companion.KEY_REVISION
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.TodayLoopOrder
import com.pnd.android.loop.data.TodayOccurrence
import com.pnd.android.loop.data.actualEndInDay
import com.pnd.android.loop.data.actualStartInDay
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.buildTodayOccurrences
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val logger = Logger("AppWidgetRefresher")

/**
 * 지금의 DB 상태를 위젯 화면으로 옮긴다.
 *
 * 부르는 곳은 둘이다.
 *  - [AppWidgetUpdateWorker]: 루프가 바뀐 뒤(위젯·알림 버튼, 시작/종료 알람, 앱에서의 편집)와
 *    정기 갱신.
 *  - [com.pnd.android.loop.alarm.notification.LoopForegroundService]: 진행 중인 루프가 있는 동안
 *    1분마다. 위젯 문구의 상당수가 지금 시각으로 계산되므로("32분 남음"), 데이터가 그대로여도
 *    다시 그려야 낡지 않는다. 서비스는 이미 1분마다 깨어 있으니 워커를 거치지 않고 바로 부른다.
 */
@Singleton
class AppWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    appDb: AppDatabase,
) {
    private val fullLoopDao = appDb.fullLoopDao()
    private val loopDao = appDb.loopDao()

    /**
     * 갱신은 "DB 읽기 → 위젯 상태 쓰기" 두 걸음이라, 두 갱신이 겹쳐 돌면 늦게 끝난 쪽이 먼저
     * 읽어 둔 오래된 목록으로 덮어쓸 수 있다(방금 완료한 루프가 위젯에 그대로 남는 증상).
     * 겹침만 막으면 마지막으로 쓰이는 목록은 언제나 그 시점의 DB 상태다.
     */
    suspend fun refresh() = lock.withLock {
        // 위젯이 하나도 없으면 읽어 봐야 쓸 곳이 없다. 1분마다 오는 갱신이 헛돌지 않게 먼저 끊는다.
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(AppWidget::class.java)
        if (glanceIds.isEmpty()) {
            logger.i { "no app widget is placed - skip refresh" }
            return@withLock
        }

        val today = LocalDate.now()
        // 자정을 넘기는 루프의 done 기록은 시작한 날인 어제 행에 있다. 오늘 행만 보면 이미 완료한
        // 루프가 계속 미응답으로 남고, 오늘 아침에 끝난 몫도 놓친다.
        val yesterdayLoops = fullLoopDao.getAllEnabledLoops(date = today.minusDays(1).toMs())
        val todayLoops = fullLoopDao.getAllEnabledLoops(date = today.toMs())

        // 홈 오늘 탭과 같은 규칙으로 occurrence 를 만든다. 자정을 넘기는 루프는 어젯밤 몫과
        // 오늘 밤 몫이 각각 한 줄씩 올라온다.
        val occurrences = buildTodayOccurrences(
            todayLoops = todayLoops,
            yesterdayLoops = yesterdayLoops.associateBy { it.loopId },
        )
        // 비활성 처리된 몫은 오늘 할 일이 아니므로 목록에서도, 아래 총 개수에서도 뺀다.
        val todayOccurrences = occurrences.filterNot { occurrence -> occurrence.loop.isDisabled }

        updateWidget(
            glanceIds = glanceIds,
            // 이미 답한 몫은 위젯에 남기지 않는다.
            loops = todayOccurrences
                .filterNot { occurrence -> occurrence.loop.isRespond }
                .sortedWith(compareBy(TodayLoopOrder()) { occurrence -> occurrence.loop })
                .map { occurrence -> occurrence.toWidgetLoop() },
            todayTotal = todayOccurrences.size,
            registeredTotal = loopDao.countAllLoops(),
        )
    }

    /**
     * 위젯으로 전달되는 루프는 putTo/asLoop 를 거치며 done 상태·실제 시작/종료 시각을 잃고
     * 순수 LoopVo 로 재구성된다. anytime 루프는 base start/end 가 항상 ANY_TIME(-1)이라,
     * 이대로면 위젯이 진행 여부를 알 수 없어 늘 "시작" 버튼만 노출된다.
     * 그래서 anytime 루프에 한해 실제 시작/종료 시각(done 기록)을 start/end 로 옮겨 실어,
     * 위젯이 시작/정지 버튼과 "started at" 라벨을 올바로 표시하게 한다.
     *
     * 어느 날 몫인지도 함께 실어야 위젯에서 누른 응답이 올바른 날짜 행에 기록된다([WidgetLoop]).
     */
    private fun TodayOccurrence.toWidgetLoop(): WidgetLoop = WidgetLoop(
        loop = if (loop.isAnyTime) {
            loop.asLoopVo(startInDay = loop.actualStartInDay, endInDay = loop.actualEndInDay)
        } else {
            loop
        },
        dateMs = date.toMs(),
        isCarriedOver = isCarriedOver,
    )

    /**
     * [loops] 가 비었을 때 위젯이 어떤 안내를 띄울지는 아래 두 수로 갈린다
     * ([com.pnd.android.loop.appwidget.ui.WidgetEmptyReason]). 남은 몫 수를 보내면 빈 화면일 때
     * 늘 0 이라 세 상태를 구분할 수 없으므로, 응답 여부와 무관한 전체 수를 보낸다.
     *
     * @param glanceIds       지금 놓여 있는 위젯들([refresh] 에서 읽어 온 것)
     * @param loops           위젯에 띄울, 아직 답하지 않은 몫들
     * @param todayTotal      오늘 예정된 몫 전체 수(이미 답한 것 포함)
     * @param registeredTotal 등록된 루프 수(꺼져 있는 루프도 포함)
     */
    private suspend fun updateWidget(
        glanceIds: List<GlanceId>,
        loops: List<WidgetLoop>,
        todayTotal: Int,
        registeredTotal: Int,
    ) {
        val jsonArray = JSONArray()
        loops.forEach { widgetLoop ->
            val map = mutableMapOf<String, Any?>()
            widgetLoop.putTo(map)

            jsonArray.put(JSONObject(map))
        }
        logger.i { "updateWidget:$jsonArray" }

        glanceIds.forEach { glanceId ->
            updateAppWidgetState(
                context = context,
                glanceId = glanceId
            ) { prefs ->
                // This is hack to force update widget
                val revision = prefs[KEY_REVISION]?.let { it + 1 } ?: 0
                prefs[KEY_REVISION] = revision
                prefs[KEY_LOOPS_JSON] =
                    "{\"loops\": $jsonArray, \"total\":$todayTotal, \"registered\":$registeredTotal}"

                logger.i { "updateWidget[$glanceId] revision:$revision" }
            }
        }
        AppWidget().updateAll(context)
    }

    companion object {
        /** 인스턴스가 여럿 만들어져도 겹침을 막을 수 있도록 클래스에 하나만 둔다([refresh] 참고). */
        private val lock = Mutex()
    }
}

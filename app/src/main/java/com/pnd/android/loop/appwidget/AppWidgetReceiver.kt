package com.pnd.android.loop.appwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class AppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = AppWidget()

    /**
     * 위젯을 새로 놓았을 때, 재부팅·앱 업데이트 뒤에 온다.
     *
     * 그릴 데이터(JSON)는 위젯 인스턴스마다 따로 저장되므로, 두 번째 위젯을 놓으면 그 인스턴스는
     * 빈 데이터로 시작한다. onEnabled 는 첫 인스턴스에서 한 번만 오기 때문에, 여기서 채우지 않으면
     * 다른 갱신 트리거(알람·앱 조작)가 올 때까지 빈 화면이 남는다.
     *
     * 정기 갱신도 여기서 다시 걸어 둔다. 이미 걸려 있으면 그대로 두므로 여러 번 불려도 무방하고,
     * 재부팅이나 앱 업데이트로 예약이 사라진 경우가 여기서 함께 복구된다.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        AppWidgetUpdateWorker.updateWidget(context)
        AppWidgetUpdateWorker.schedulePeriodicUpdate(context)
    }

    /** 마지막 위젯이 제거됐다. 더 갱신할 곳이 없으니 정기 갱신을 거둔다. */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        AppWidgetUpdateWorker.cancelPeriodicUpdate(context)
    }
}

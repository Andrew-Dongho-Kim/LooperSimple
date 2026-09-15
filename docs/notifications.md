# Notification design

## Information hierarchy

- Collapsed: full-width loop title, remaining/elapsed time, then secondary context. For multiple loops, show the next loop to finish beside its remaining time.
- Expanded: running count, then loops ordered by nearest end time. Each row has a loop-color marker, title, time, a thin progress track, and a labeled Done/Stop button.
- Standard text colors follow the notification host theme. Loop colors identify rows and progress; action labels use the system text color so pale loop colors remain usable.
- Start, in-progress, completion, and anytime prompts use a short state in the notification header and reserve the title for the loop name. The status-bar icon is a monochrome loop.
- Show up to three rows normally, two above 1.15 font scale, and one above 1.5. Remaining loops have an Open app link. Hide the collapsed progress track above 1.3 font scale. Above 1.5, cap the compact title/metadata at 20/16dp so two lines fit the 64dp system budget; expanded text continues to use the full font scale.
- Channel IDs, notification IDs, intent routing, and completion dates are preserved.

## Device rendering verification

`NotificationRenderingTest` inflates the actual RemoteViews with synthetic data. It checks collapsed/expanded height, text height, action target size and descriptions, and single-loop notification actions across Korean/English, light/dark, 1.0/1.3/2.0 font scales, 260/340dp widths, and single/anytime/mixed/overflow lists.

Run with a locally installed Gradle 8.5 and Java 17 (the repository wrapper JAR is absent):

```powershell
gradle -I tools/notification-validation.groovy :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pnd.android.loop.alarm.notification.NotificationRenderingTest --offline
```

The opt-in init script builds an unminified, isolated `com.pnd.android.loop.notificationpreview` package and uses AndroidJUnitRunner. It does not replace the installed production app. The test never posts notifications or reads existing loop records. Representative PNGs are saved in the preview app's external files directory under `notification-renders`.

These checks cover the custom content; notification shade framing and native prompt buttons are supplied by Android and can vary between devices.
## Verified result (2026-09-15)

- Kotlin/resource compilation and isolated debug APK assembly passed.
- The final AndroidJUnitRunner invocation passed all 192 rendering cases on the connected SM-S948N (API 37). Light/dark PNGs were visually reviewed.
- The 200% compact-height failure was fixed by capping compact text size; action labels use a content-sized button with a 48dp minimum touch target.
- Local evidence: `build/notification-validation-results.txt` and `build/notification-renders/`. These are generated outputs, excluded from source control.
- To retain PNGs, assemble and install the preview and test APKs, run the test via `adb shell am instrument -w -e class com.pnd.android.loop.alarm.notification.NotificationRenderingTest com.pnd.android.loop.notificationpreview.test/androidx.test.runner.AndroidJUnitRunner`, and pull the external files directory before uninstalling. Gradle connected tests may remove the preview package after the run.
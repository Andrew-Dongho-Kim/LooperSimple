# Statistics widgets

Three independent Android home-screen providers are available: **오늘의 페이스** (Today’s pace),
**주간 리듬** (Weekly rhythm), and **월간 성취** (Monthly achievements).
Tap any widget to open the statistics screen. Resize to reveal charts and supporting metrics.
The default sizes show each design; narrow/short sizes show the primary number.

The minimum requested resize bounds are **56 × 56dp** (previously 160 × 160dp).
The launcher determines the actual cell dimensions and may impose a larger minimum.
Below the expanded layout threshold (250dp wide and 260dp tall, or 280dp tall for monthly, scaled for system font size),
the compact layout measures text against both available axes. Single-row, wide widgets put the
period and value side by side; narrow widgets stack them. Supporting metrics appear only when
their whole group fits. The smallest sizes prioritize the primary value, shortening period labels
and abbreviating hours/minutes as h/m or large values with SI suffixes. Accessibility descriptions
retain the full period and unabridged value. Large system fonts may hide the heading at tiny sizes.
Resizing uses Glance SizeMode.Exact, so no data refresh or widget re-add is needed.

## Calculation rules

- Today: completed / all occurrences scheduled for that calendar date, including pending and skipped
  occurrences. Remaining excludes completed and skipped. No scheduled occurrences displays a dash,
  never a false 100%.
- Week: Monday through today; completion rate uses settled occurrences, matching the app statistics.
  The seven bars show daily completion counts. Future days display a dash. The leading loop is ranked
  by completion rate, then completion count, then stable loop ID; its numerator and denominator are shown.
- Month: current-month invested time and completion rate, plus six consecutive monthly time totals,
  including zero months. The current month is explicitly marked as in progress.
- Current and longest streak count consecutive dates with at least one completed loop, using the
  app’s shared streak calculator. Today’s pending activity preserves a streak anchored yesterday.
- All periods use the revision-aware history resolver. A midnight-crossing occurrence belongs to its
  start date. Recorded elapsed instants take precedence over clock times. Missing clock times do not
  become a 24-hour duration. Invested time can include planned time, so it is not labeled measured focus.

## Updates and appearance

All four widget providers (including the original loop widget) share the existing serialized refresh
path and periodic worker. Adding another instance loads its own state. Removing one provider does not
cancel periodic updates while another provider remains. Each refresh reads one database snapshot.
Streak computation scans saved completions rather than expanding all historical missed dates.

Colors use the app’s day/night palette. The ring uses monochrome bitmap masks with theme-aware tints.
Labels, values, bar baselines and chart captions remain readable without relying on color alone.
Korean and English strings and picker previews are included. Empty and loading states are distinct.

## Verification

`StatisticsWidgetModelsTest` covers pending versus settled counts, historical schedule changes,
year boundaries, zero-filled months, overnight/measured durations, disabled plans, and JSON restoration.

`StatisticsWidgetRenderingTest` renders synthetic state through Glance into real RemoteViews at tiny,
narrow, square, horizontal and default sizes, in both themes, Korean/English, and 1×/2× font scales.
It also covers empty/loading states and large values, checking text bounds, compact ellipsis,
text height and presence of primary values. It writes representative PNGs
to the test application's external-files `widget-renders` directory. It does not access loop records.
Use AndroidJUnitRunner for this instrumentation test; the repository's default HiltJUnitRunner is absent.

The repository root Gradle wrapper JAR is absent on this checkout. Use an installed Gradle 8.5 with
JDK 17/21 when running `:app:testDebugUnitTest :app:assembleDebug`.

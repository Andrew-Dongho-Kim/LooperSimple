# 루프 변경 이력 및 통계 영향 검토

검토일: 2026-09-14. 대상: 현재 작업 트리, Room schema v9. 이 문서는 구현 전 검토 결과이며 앱 코드·DB는 변경하지 않았다.

## 1. 결론

변경 로그 테이블을 추가하는 것만으로는 과거 통계를 보존할 수 없다. **설정 저장, 날짜별 일정 판정, 실행 기록 저장, 통계 분모, 알림·위젯의 실행 대상 식별**을 함께 정리해야 한다.

권장 구조는 현재 설정을 담는 `loop`를 유지하고, 적용 시점을 가진 설정 이력과 실행별 계획 정보를 추가하는 것이다. 실제 수행 결과는 기존 `loop_done`을 중심으로 보존한다. 모든 통계가 동일한 날짜별 판정 결과를 사용하되, 오늘 포함 여부·집계 기간·주간 목표 등 지표별 의미는 명시적으로 구분한다.

우선 해결할 사항은 다음과 같다.

1. 이름·색만 바꿔도 오늘 실행 기록이 초기화되는 저장 경로.
2. 현재 설정으로 과거 일정·계획 시각·실행 유형을 재해석하는 조회.
3. 서로 다른 완료율 분모와 저장되지 않은 예정일 처리.
4. 알람 취소가 비동기로 루프를 다시 저장하는 우회 경로.
5. 과거 기록 수정, 위젯·알림 응답, 삭제 취소에서 이력과 실행 시각을 보존하는 처리.

## 2. 조사 범위와 검증 수준

앱의 Kotlin 소스 145개를 대상으로 모델·DAO 사용, 설정 필드 참조, 저장 호출, 계산 함수와 화면 연결을 전역 검색하고, 직접 관련된 구현을 읽어 호출 경로를 추적했다. 테스트 소스 9개, benchmark 소스 3개, Gradle, Manifest, v1~v9 스키마 파일의 존재와 현재 v9 구조, 한국어·영어 통계 리소스도 조사 범위에 포함했다. 생성물·라이브러리·이미지·음원 자체는 검토 대상 코드에서 제외했다.

검증은 정적 분석 및 **실제 v9 스키마와 DAO SQL을 사용한 메모리 SQLite 재현**이다. 기기 UI, Room Flow 발행 순서, Android 알람 전달, 동시 실행 경합은 실행 검증하지 않았다. 따라서 아래 경합 가능성을 기기에서 재현된 사실로 보아서는 안 된다. 전체 앱 빌드와 JUnit/계측 테스트는 이번 검토에서 실행하지 않았다.

## 3. 현재 구조에서 확인한 문제

### A. 설정 저장이 오늘 수행 결과를 덮어쓴다 — 최우선

[LoopRepository.kt:209](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/viewmodel/LoopRepository.kt:209)의 `addOrUpdateLoop()`는 `loop` 저장 후 오늘 행을 항상 다음 중 하나로 처리한다.

- 활성 요일이면 `NO_RESPONSE`와 현재 계획 시간을 저장한다.
- 비활성 설정이면 `DISABLED`로 저장한다.
- 활성 설정이나 오늘 요일을 제거했으면 오늘 행을 삭제한다.

이 처리는 이름만 변경해도 동일하게 실행된다. 기존 `DONE`, `SKIP`, `IN_PROGRESS`와 실측 시간이 사라질 수 있다. 이력만 추가하면 잘못 초기화된 결과를 더 정확하게 설명할 뿐 데이터 손실은 막지 못한다.

**변경안:** 설정 저장과 실행 상태 저장을 분리한다. 기존 값을 트랜잭션 안에서 읽고 실제 바뀐 필드만 비교해 이력을 추가한다. 이름·색 변경은 실행 행을 건드리지 않는다. 일정 변경도 확정·진행 중인 실행을 보존하고, 아직 시작하지 않은 예정분에 대한 처리만 명시적 정책으로 수행한다. 변경 없는 저장, 초안 편집·취소는 이력을 만들지 않는다.

### B. 요일·활성 여부를 바꾸면 과거 분모가 바뀐다

[MonthInsightModels.kt:129](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/history/MonthInsightModels.kt:129)의 `resolveInsightDays()`와 [FullLoopDao.kt:22](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/FullLoopDao.kt:22)의 `getAchievementDayFlow()`는 저장 행이 없는 과거 날짜를 현재 `enabled/activeDays`로 복원한다. 같은 과거 기간이 설정 변경 후 다른 완료율·완벽한 날·요일별 패턴을 보인다.

[RecentLoopCompletion.kt:35](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/RecentLoopCompletion.kt:35)와 [LoopViewModel.kt:522](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/viewmodel/LoopViewModel.kt:522)은 현재 요일을 먼저 검사하므로, 바뀐 요일에서 제외된 과거 완료 기록까지 집계하지 않는다. 반면 월간 기록과 전체 그리드는 저장 행을 우선한다.

**변경안:** 해당 날짜의 일정 이력으로 예정 여부를 판단한다. 실행 기록과 예정일의 관계도 공통 규칙으로 정의한다. 비예정일에 사용자가 남긴 완료·건너뜀을 무조건 소거하지 말고 수동 기록 여부를 구분해 일관되게 집계한다.

### C. 계획 시각과 이름·실행 유형이 최신 설정에 연결된다

[FullLoopDao.kt:158](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/FullLoopDao.kt:158)의 `getResponsesFlow()`는 `plannedStartInDay`, `isAnyTime`, 이름·색을 현재 `loop`에서 가져온다. `getAchievementDayFlow()`, 완료/미완료 날짜별 조회도 과거 이름·색을 보존하지 않는다.

시간 변경은 [StatisticsModels.kt:547](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/statisctics/StatisticsModels.kt:547)의 계획 대비 실제 지연을 바꾸며, 시간제 ↔ 언제든지 전환은 과거 기록이 분석 표본에 포함되는지까지 바꾼다.

**변경안:** 계획은 실행에 적용된 설정 버전 또는 계획 스냅샷에서 읽는다. 실측은 실행 행에서 읽는다. 과거 일별 기록의 이름·색과 루프 목록·기간 순위의 대표 이름은 별도 표시 정책으로 정한다. 집계 키는 항상 `loopId`를 유지한다.

### D. 완료율 정의가 화면마다 다르다

| 경로 | 현재 분모·상태 처리 | 이력 도입 시 필요한 조치 |
|---|---|---|
| 홈 전체/오늘 헤더 | `loop_done`의 `DISABLED` 이외 저장 행 | 저장되지 않은 예정일과 오늘 진행분 포함 정책 연결 |
| 홈 최근 30일 칩 | 현재 요일로 계산한 예정일, 생성일·오늘 제외 | 날짜별 이력으로 변경, 기록 우선순위 통일 |
| 홈 잘 지키는/놓치는 루프 | 최근 활동일 표본, 현재 요일 기준 | 이력별 활동일·비활성 구간 반영 |
| 상세 요약·최근 28일 | 확정된 저장 행, 오늘 미응답·진행 중 제외, 누락 행 보완 안 함 | 기록 기반 의미 유지 여부 결정 후 공통 입력 연결 |
| 상세 스트릭 | 전체 과거에 현재 요일 적용, 비활성 구간 정보 없음 | 날짜별 예정일과 비활성 구간 사용 |
| 상세 이번 주 목표 | 현재 요일 수 또는 현재 `weeklyGoal` | 주중 설정 변경의 목표 적용 규칙 필요 |
| 일별 기록·월간 인사이트 | 저장 행 우선 + 현재 설정으로 빈 날 보완, 오늘 진행분 포함 | 동일한 날짜 판정기로 일·주·월 조회 통일 |
| 통계 기간 요약·월별 추세·습관 건강·정착률 | `DISABLED`를 제외한 저장 행 | 동일 범위의 예정분 보완 여부와 상태 정책 연결 |
| 통계 루프 순위 | **`DISABLED`까지 포함한 모든 저장 행** | 다른 완료율과 분모 불일치 우선 수정 |

순위의 근거는 [FullLoopDao.kt:95](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/FullLoopDao.kt:95). 상세 기록 기준의 근거는 [DetailActivityStats.kt:56](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/detail/DetailActivityStats.kt:56).

모든 화면의 숫자를 무조건 같게 만드는 것이 목적은 아니다. 28일과 30일, 오늘 포함/제외, 달력 연속과 예정일 연속은 의미가 다르다. **같은 기간·같은 정책에서는 같은 결과가 나오도록** 기반 판정을 공유해야 한다. 분모 자체를 바꾸면 사용자에게 보이는 기존 수치가 변하므로 이력 도입과 구분해 변경 내용을 설명해야 한다.

단순 완료 횟수, 완료한 날짜 수, 실제 기록 기준 시간 합계, 완료 기록의 시간대 분포, 회고율, 앱 전체 달력 연속 기록, 이 값들의 마일스톤은 기존 실행 데이터가 보존되면 설정 이력을 매번 JOIN할 필요가 없다. 이력 도입을 이유로 과거 시간을 새 계획 시간으로 다시 계산해서는 안 된다. 시간 품질 검증은 별도 개선이다. 월말 예측의 `computeMonthlyProjection()`은 경과 일수로 단순 환산하므로 현재 정의를 유지하면 직접 수정할 필요가 없고, 남은 예정일을 반영한 예측으로 확장하려는 경우에만 향후 일정 입력을 추가한다.

### E. 알람 취소가 DB 설정을 비동기로 수정한다

[LoopScheduler.kt:237](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/LoopScheduler.kt:237)의 `cancelAlarm()`은 활성 루프를 전달받으면 별도 코루틴에서 `loopDao.addOrUpdate(enabled=false)`를 실행한다. 저장소를 우회하므로 저장소에만 이력을 넣으면 활성 상태 변경이 누락된다.

[LoopRepository.kt:251](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/viewmodel/LoopRepository.kt:251)은 알람 취소 후 루프를 삭제한다. 비동기 저장이 삭제보다 늦게 실행되면 삽입으로 삭제된 루프가 다시 생길 수 있는 경합 구조다. 이번에 Android 동시 실행으로 재현한 것은 아니지만 코드상 가능한 순서다.

**변경안:** 알람 취소는 알람만 취소하도록 한다. 활성 상태 변경은 단일 저장 경로에서 트랜잭션으로 수행한다. 삭제·복원과 이력 저장을 원자적으로 처리하고, 알림·위젯 갱신은 커밋 후 수행한다. 실패 시 재동기화가 가능해야 한다.

### F. 과거 수정·위젯·알림은 다른 저장 규칙을 사용한다

[LoopDetailViewModel.kt:155](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/detail/LoopDetailViewModel.kt:155)의 과거 상태 변경은 **현재 루프가 언제든지일 때만** 기존 행의 시간을 보존한다. 시간제 루프의 과거 완료 상태를 바꾸면 현재 계획 시간이 다시 기록될 수 있다.

[AppWidgetUpdateWorker.kt:92](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/appwidget/AppWidgetUpdateWorker.kt:92)의 `done()/skip()`도 현재 루프의 시작·종료를 저장한다. `start()/stop()`는 직접 DAO에 쓰고, 설정 버전 연결이 없다. 알림 액션도 이 워커로 들어온다.

**변경안:** 화면·위젯·알림에서 공통 실행 기록 명령을 호출한다. `loopId + occurrenceDate`로 기존 행과 당시 설정을 확인하고, 상태 변경과 시간 변경을 구분한다. 과거 수정은 설정 변경 이력을 새로 만들지 않는다. 늦게 누른 알림은 눌린 시점의 최신 일정으로 날짜·시간을 다시 계산하지 않는다. 삭제된 루프의 오래된 액션은 무시한다.

### G. 미응답 보완과 습관 시작 추정도 영향받는다

[LoopScheduler.kt:190](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/LoopScheduler.kt:190)의 `fillNoResponse()`는 어제·오늘만 현재 설정으로 채운다. 오랫동안 실행하지 않은 기간의 예정일 전체를 물리적으로 저장하지 않으므로, `loop_done`만으로 모든 분모를 계산할 수 없다. 수정 후 어제 빈 행을 채울 때도 새 설정이 과거에 적용된다.

[LoopDoneDao.kt:64](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/LoopDoneDao.kt:64)의 `getRecentStarts()`는 `startInDay >= 0`만으로 표본을 읽는다. 시간제 루프를 언제든지로 바꾸면 과거 시간제의 계획 시간·미응답 행이 실제 시작 표본에 섞일 수 있다. [HabitualStart.kt:207](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/HabitualStart.kt:207)까지 함께 수정해야 한다.

**변경안:** 이력으로 어제·오늘의 예정분을 판정하고 완료·진행 행을 덮어쓰지 않는다. 통계용 누락 예정일은 조회 시 계산할 수 있다. 습관 추정에는 실제 시작 사실과 당시 실행 유형을 확인한 표본만 넣는다.

## 4. 권장 데이터 설계

아래 이름과 필드는 구현안이다. 현재 코드에 존재하는 타입으로 오해하지 않아야 한다.

| 구성 | 역할 | 권장 정보 |
|---|---|---|
| 기존 `loop` | 현재 편집 설정·식별자 | 기존 필드 유지, 필요하면 최신 revision 식별자 |
| 신규 `loop_revision` | 변경 시점과 적용 구간이 있는 설정 스냅샷 | revisionId, loopId, recordedAt, 적용 날짜/시각, 순번, title, color, activeDays, start/end, isAnyTime, enabled, weeklyGoal, 이력 출처/신뢰도 |
| 기존 `loop_done` 확장 또는 별도 실행 계획 테이블 | 그 실행에 적용된 계획 고정 | nullable revisionId 또는 계획 시작·종료·실행 유형 스냅샷, 필요하면 실제 시각의 출처 |
| 신규 순수 판정기 | 날짜별 예정 여부와 실행 결과 결합 | loopId, 날짜, 적용 설정, 저장 기록, 예정/비예정/비활성/정보 없음, 집계 가능 여부 |

완료 기록에 revisionId만 추가하면 **기록이 없는 예정일**의 분모를 복원하지 못하므로 설정 이력 테이블이 필요하다. 반대로 설정 이력만 추가하면 같은 날 여러 번 수정하거나 실행 중 일정이 변경될 때 어느 계획으로 시작했는지 모호하므로 실행별 연결도 필요하다.

### 저장 불변 조건

- 최초 생성 시 첫 스냅샷을 실제 생성 시점부터 기록한다. 수정 시 직전 DB 값과 최종 저장값을 비교한다.
- `title`, `color`, `activeDays`, `startInDay`, `endInDay`, `isAnyTime`, `enabled`, `weeklyGoal`을 추적한다. `loopId/created`는 일반 편집으로 변경하지 않는다. `isMock`, 임시 표시 상태, 수행 상태·회고는 설정 이력에서 분리한다.
- 요일 변경으로 `weeklyGoal`이 자동 보정된 경우도 최종 스냅샷에 함께 기록한다.
- 한 번의 저장에서 여러 필드가 바뀌어도 하나의 원자적 revision을 기록한다. 동일 밀리초의 연속 저장을 구분할 안정적인 순번/ID를 둔다.
- 기존 설정 갱신, 이력 추가, 필요한 실행 계획 변경은 동일한 DB 트랜잭션으로 처리한다. 최신값을 읽기 전에 UI의 낡은 전체 객체로 덮어쓰지 않는다. patch 또는 기대 revision 검증을 고려한다.
- `loop`에 `INSERT OR REPLACE`를 적용하지 않는다. 부모 삭제로 처리되어 기존 `loop_done/loop_memo`의 CASCADE 삭제를 유발할 수 있다. 삽입·수정을 명시적으로 분리한다.
- 시각·적용 날짜는 저장 작업에서 한 번 캡처한다. 자정 사이에 호출한 `now()`들이 서로 다른 날짜를 가리키지 않게 한다.

### 적용 시점 정책: 구현 전에 정해야 할 사항

| 항목 | 권장 기준과 주의점 |
|---|---|
| 이름·색 | 현재 목록에는 즉시 표시. 과거 일별 기록은 당시 값을 표시할지 명시. 이름 변경으로 수치·실행 상태는 변경하지 않음 |
| 요일·시간·실행 유형 | 시작/확정된 실행의 계획은 고정. 아직 시작 전 실행만 새 설정 적용 가능. 하루 단위 다음날 적용을 택하면 단순하지만 현재 화면·알람도 같은 적용 규칙을 따라야 함 |
| 당일 여러 변경 | 감사용 변경 시각과 통계용 적용 시점을 구분. 자정 기준으로 revision을 고르면 당일 낮 수정이 무시될 수 있고, 마지막 revision을 고르면 당일 완료가 재해석됨 |
| 활성/비활성 | 과거 확정 결과 유지. 비활성 기간의 빈 날짜는 미응답에 넣지 않음. 진행 중 종료/중단 처리는 별도 명령으로 정의 |
| 주간 목표 | 현재 코드는 상세 주간 진행에 적용하고 일반 완료율에는 적용하지 않음. 목표 변경은 다음 주 적용을 고려하되, 주중 즉시 적용을 택하면 혼합 주의 목표 계산 규칙 필요. 매일 weeklyGoal을 더하면 안 됨 |
| 일정 없는 날 수동 완료 | 실제 완료를 보존하고 비예정 수동 실행을 표시. 예정 완료율과 전체 수행 횟수에 어떻게 포함할지 명시 |
| 기간 대표 이름 | 현재 이름 또는 기간 마지막 적용 이름 등을 선택. `values.first().title`로 우연히 가장 오래된 이름을 선택하지 않음 |

기간 대표 이름 관련 실제 변경 지점: `buildMonthInsightReport()`의 `values.first().title`, `computeHabitHealth()`의 `recs.first()`, 순위/정착률 DTO. 집계 키를 이름으로 바꾸면 개명 전후가 별도 루프로 분리되므로 `loopId`를 유지해야 한다.

## 5. 파일별 변경 지도

### 필수: DB·저장·통계 계산

| 파일 | 변경 내용 |
|---|---|
| [AppDatabase.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/AppDatabase.kt:18) | v9 이후 버전, 신규 엔티티·DAO·마이그레이션, 기존 데이터 초기 스냅샷 정책 |
| [AppModule.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/di/AppModule.kt:24) | 수동 migration 등록이 필요한 설계면 등록. DB·저장 서비스의 공유 범위 점검 |
| [LoopDao.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/LoopDao.kt:45) | insert/update 명령 분리, 현재 설정 조회와 날짜별 조회의 용도 구분 |
| [LoopDoneVo.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/LoopDoneVo.kt:17) | 실행 계획 revision/스냅샷, 실제 시간 출처 확장 검토. 기존 복합 PK와 상태 값 유지 |
| [LoopDoneDao.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/LoopDoneDao.kt:124) | 상태 변경 시 계획·실측 보존, 구간 조회, 기존 count 쿼리의 의미/소비자 정리, 실제 시작 표본 |
| [LoopRepository.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/viewmodel/LoopRepository.kt:209) | 설정 이력 원자 저장, 오늘 기록 초기화 제거, 실행 명령 통합, 삭제·복원 전용 경로 |
| [LoopDetailViewModel.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/detail/LoopDetailViewModel.kt:101) | 이력 Flow 연결, 과거 상태 수정, 삭제 스냅샷·복원 확장, CSV 정책 |
| [FullLoopDao.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/dao/FullLoopDao.kt:21) | 일별 과거 조회·계획/실행 유형·이름·색, 순위 분모, 기간 조회·월별 집계·정착률 입력 정리 |
| [StatisticsRecords.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/StatisticsRecords.kt:28) | 당시 계획·타입·신뢰도 등 명확한 DTO. respondedCount가 실제 응답인지 예정 표본인지 구분 |
| [FullLoopVo.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/FullLoopVo.kt:56) | 당시 설정과 실제 시각 매핑. 값이 있다는 이유로 anytime의 원래 유형이 사라지지 않게 분리 |
| [AchievementDayRecord.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/AchievementDayRecord.kt:21) | 신규 일별 조회 모델 매핑, 계획 유형과 시간 표시 여부 분리 |
| [LoopWithDone.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/LoopWithDone.kt:1) | 현재 설정과 occurrence 계획의 혼용 해소, 조회 projection 변경 전파 |
| [RecentLoopCompletion.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/RecentLoopCompletion.kt:35) | 최근 30일의 날짜별 이력 적용 |
| [LoopViewModel.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/viewmodel/LoopViewModel.kt:261) | 추세·칩·전체/오늘 비율에 공통 판정 데이터 공급, 이력 변경 시 갱신 |
| [AllDoneHistoryGrid.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/home/AllDoneHistoryGrid.kt:473) | 빈 셀의 예정/비예정/비활성/정보 없음 판정을 이력 기반으로 변경 |
| [DetailStats.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/detail/DetailStats.kt:87) | 상세 스트릭·이번 주 목표의 날짜별 설정 적용 |
| [DetailActivityStats.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/detail/DetailActivityStats.kt:62) | 기존 기록 기반 지표의 의미를 명시하고 공통 판정 결과 연결 |
| [DailyAchievementViewModel.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/history/DailyAchievementViewModel.kt:40) | 일·달력·월간 보고서에 동일 이력 입력과 구간 조회 제공 |
| [MonthInsightModels.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/history/MonthInsightModels.kt:129) | 날짜 복원·전월 비교·분모·대표 이름. 이력이 있는 무응답 기간의 비교 가능 여부 재정의 |
| [StatisticsModels.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/statisctics/StatisticsModels.kt:380) | 예정일 스트릭, 기간 완료율·완벽한 날·건강·정착률·계획 대비 실제의 공통 입력 |
| [StatisticsViewModel.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/statisctics/StatisticsViewModel.kt:38) | 집계 조회 교체, 이력과 오늘 변경 관찰, 무거운 집계의 백그라운드 실행 |

신규 구성 후보: `LoopRevisionVo`, `LoopRevisionDao`, `LoopOccurrenceResolver`, `LoopMutationStore`, migration 및 통합 테스트. 경로·이름은 구현 시 기존 패키지 규칙에 맞춰 정한다.

### 필수: 실행·알림·위젯의 일관성

| 파일 | 변경 또는 회귀 확인 |
|---|---|
| [LoopScheduler.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/LoopScheduler.kt:190) | 과거 미응답 보완, 취소의 DB 부작용 제거, 변경 후 START/END/ANYTIME 예약 정리 |
| [AppWidgetUpdateWorker.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/appwidget/AppWidgetUpdateWorker.kt:92) | 공통 실행 기록 명령 사용, 오래된 요청·타입 변경·날짜 경계 처리 |
| [Time.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/util/Time.kt:298) | occurrence를 오늘 최신 설정으로만 선택하지 않게 변경. timezone·자정 정책 일치 |
| [TimeStat.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/TimeStat.kt:126) | 홈 카드의 진행·남은 시간 계산에 적용 occurrence 사용. LaunchedEffect 키가 현재 loopId/start/end/isAnyTime뿐이므로 요일·상태·실측·revision만 바뀔 때 낡은 객체를 계속 사용하지 않게 검토 |
| [TodayOccurrence.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/data/TodayOccurrence.kt:48) | 어제/오늘 설정이 달라도 이전 실행 유지, 중복 표시·중복 기록 방지 |
| [AppWidgetRefresher.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/appwidget/AppWidgetRefresher.kt:63) | 이력/실행 계획이 결합된 오늘·어제 조회 사용 |
| [LoopForegroundService.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/notification/LoopForegroundService.kt:172) | 변경 전 시작한 진행분·상시 알림 표시 일치 |
| [LoopEndPrompter.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/notification/LoopEndPrompter.kt:76) | 변경된 종료 시간 대신 실제 대상 실행으로 응답 요청 |
| [LoopNotificationActionReceiver.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/notification/LoopNotificationActionReceiver.kt:25) | date 및 필요 시 revision 전달. 워커 공통 경로에 따른 회귀 확인 |
| [NotificationHelper.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/notification/NotificationHelper.kt:622) | 액션 대상 실행 식별. 알림의 낡은 extras로 실행 대상을 오인하지 않도록 검증 |
| [WidgetLoop.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/appwidget/WidgetLoop.kt:43) | 실행 식별·계획/현재 유형 분리 시 직렬화와 상태 판정 갱신 |
| [HabitualStart.kt](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/alarm/HabitualStart.kt:207) | 당시 유형 및 실제 시작 출처가 확인된 표본만 사용 |

### 변경 전파·조건부 수정

- 편집 진입점: `Home`, `LoopCardWithOption`, `Section`, `DetailEditor`, `DetailScheduleSheet`, `DetailPage`, `LoopEditorDraft`, `ScheduleDraft`. 저장 완료 시점에만 이력 생성. 요일 변경에 따른 주간 목표 자동 보정 포함. UI마다 별도 이력 INSERT를 넣지 않는다.
- 상태 입력 진입점: `LoopYesterdayCard`, `LoopDoneSkipCard`, `Section`, `RecordDoneDialog`, 상세 `DetailJournal/DetailJournalSheet`, 다이얼, `AppWidgetActions`, 위젯 UI. 변경 전 시작한 실행의 날짜·시간·상태가 공통 저장 경로까지 전달되는지 확인한다.
- 표시 소비자: `RecentCompletionChip`, 홈 헤더, `DetailSummary`, `DetailStatsSection`, `DetailActivityCharts`, `RecordFirstAchievementPage`, `AchievementDateNavigation`, `MonthInsightAnalysis`, `MonthlyInsightExperience`, `StatisticsPage`. 계산을 화면에 중복 구현하지 않고 모델을 전달한다. 정보 부족을 0%와 구분할 경우 UI 모델·문구 수정이 필요하다.
- `LoopOrder`의 진행/종료/종료시간 정렬과 `LoopCard`·위젯의 남은/경과 시간은 같은 occurrence 계획을 사용해야 한다. 수정 직후 정렬만 최신 시간으로 바뀌고 표시·상태는 이전 시간에 머무는 불일치를 검증한다.
- `LoopBase/LoopVo`의 현재 설정 역할을 유지하면 모든 복사·Intent·Map에 전체 이력을 넣을 필요는 없다. revision 식별자를 전달하는 설계라면 `copyAs/asLoopVo/putTo/asLoop`, SavedState 복원과 위젯 JSON을 빠짐없이 맞춘다.
- `LoopRetrospectVo/LoopRetrospectDao`는 계속 loopId+date에 연결할 수 있다. 삭제·복원 트랜잭션과 날짜 정책 변경 영향만 점검한다.
- 리소스: `values[-ko]/recent_completion_strings.xml`, `monthly_insight_strings.xml`, `history_strings.xml`, `strings.xml`. 현재 일정으로 과거를 추정한다는 안내, 완료율 분모·오늘 포함 규칙을 새 정책에 맞춘다.
- 순수 테마·색·폰트·도형·광고·일반 공통 레이아웃, 음원·아이콘에는 이력 저장/집계 로직의 직접 변경 필요성을 찾지 못했다. 날짜 표시·포맷은 입력 타입 변경 시 회귀 확인 대상이다.

### 레거시 경로 구분

[DailyAchievementPage.kt:111](D:/project/LooperSimple-main/LooperSimple/app/src/main/java/com/pnd/android/loop/ui/history/DailyAchievementPage.kt:111)은 현재 `RecordFirstAchievementPage`로 진입한다. 기존 `LegacyDailyAchievementPage`, `DailyAchievementPagingSource`, `DailyAchievementCalendar`에는 최신 설정으로 과거를 재구성하는 구현이 남아 있다.

`flowMonthSummary()`, 과거 완료/미완료 날짜 조회, `MonthSummaryDialog`도 잔존 참조를 구분해야 한다. 현재 주 경로의 월간 화면은 `flowMonthReport()`이다. 미사용 경로까지 무조건 확장하기보다 제거 또는 동일 판정기로 통합할 대상을 정한다. 레거시 페이징을 유지하면 현재 요일별 캐시와 N×일수 DB 조회, 이력 변경 시 invalidation도 함께 수정한다.

`LoopDoneDao.deleteNoResponseAll()`은 선언은 있으나 현재 앱 소스에서 호출을 찾지 못했다. 현재 코드가 모든 미응답 행을 항상 삭제한다고 가정하면 안 된다. 반면 홈 `getAllHistoryFlow()`는 조회 결과에서 미응답·진행 중 행을 제외하므로 이를 다시 미응답으로 추정하는 부분은 실제로 존재한다.

## 6. 마이그레이션·삭제·시간 데이터 주의점

### 기존 사용자 데이터

v9에는 이전 이름·요일·활성 구간·실행 유형이 없다. 과거 `loop_done.start/end`도 시간제에서는 계획 시각 복사이고 언제든지에서는 실측일 수 있다. **마이그레이션으로 소실된 변경 이력을 복구할 수는 없다.**

권장안은 이력 기록을 시작한 시점과 정보 출처를 남기는 것이다. 이력이 확실한 기간부터 날짜별 일정을 정확하게 계산하고, 이전 기간은 실제 저장 기록과 명시적인 추정 정책으로 처리한다. 현재 설정을 생성일부터의 사실로 소급 저장해서는 안 된다. 이전 값으로 fallback하더라도 추정임을 모델에 남긴다. 마이그레이션 후의 새 이름·요일 변경이 이 이전 기간의 fallback을 계속 바꾸지 않도록 초기 기준값을 보존한다.

새 설치, v9→신규, v1~v8→v9→신규 연속 migration, 행이 없는 DB, 비활성 루프, 기존 DONE/SKIP/IN_PROGRESS/DISABLED, memo-only 날짜를 검증한다. Room schema JSON을 생성·검증하고 파괴적 migration으로 우회하지 않는다. 새로운 필드의 기본값·nullable·FK·인덱스·projection·`@JvmOverloads` 생성자 선택을 함께 확인한다.

### 삭제와 실행 취소

기존 `loop_done`과 `loop_memo`는 루프 삭제 시 CASCADE 삭제된다. 변경 이력만 남겨도 이 실행 데이터가 없어지면 삭제된 루프의 통계는 유지되지 않는다. 삭제 후 통계 보존까지 원한다면 별도 보관/논리 삭제 정책이 필요하며 이번 요구의 필수 범위로 자동 확대하지 않는다.

현재 상세 삭제 취소 스냅샷은 루프·실행·메모만 담는다. 이력을 추가하고 원래 revision ID·적용 시점을 유지해 복원해야 한다. 일반 생성 함수를 호출해 ‘복원 시점 신규 이력’을 덧붙이면 과거가 바뀐다. 삭제 스냅샷 조회+삭제, 복원 전체를 각각 트랜잭션으로 처리하며, 복원 중 오늘 빈 행 추가와 중간 알람 갱신을 피한다.

### 시간과 날짜

- 기존 date는 시스템 timezone 기준 자정 epoch ms이고 조회도 현재 timezone으로 LocalDate를 복원한다. revision의 날짜를 새로 정의하면서 기존 date와 단순 비교하지 않는다. 여행·timezone 변경, DST, 자정·월말·연말을 검증한다.
- 23:00~01:00 실행의 통계 날짜는 시작한 날을 유지한다. 다음날 일정 변경이 어젯밤 실행을 바꾸면 안 된다. 오늘·어제 revision이 서로 다를 때 `currentOccurrenceDate()`의 현재 유형 기준 분기를 고쳐야 한다.
- 시간제의 저장 시각은 보통 계획값을 복사한다. 이를 모두 실제 측정 시각으로 간주하면 이력을 추가해도 정시율은 정확한 실측 통계가 되지 않는다. 출처를 분리하거나 지표 설명/표본 조건을 조정한다.
- 전체 통계의 `investedTimeMs()`와 SQL 합계는 시작·종료의 유효 범위 검증이 없지만 월간 인사이트는 범위를 검사한다. -1/부분 시각을 실제 시간처럼 합산하지 않도록 일치시킨다. 0은 실제 자정일 수 있으므로 일괄 결측값 취급하지 않는다.
- 기존 하루 안의 시각만으로는 24시간을 넘는 실행 기간이나 자정 근처 시작 지연을 완전히 복원할 수 없다. 이를 정확히 지원할 범위라면 실측 시작/종료 instant와 날짜 오프셋을 별도로 저장한다. 이번 변경에서 기존 값만으로 이를 해결했다고 주장하면 안 된다.

## 7. 조회·성능·갱신 설계

월/기간 조회는 루프, 실행 기록, 메모, 이력을 구간 단위로 가져온다. **조회 시작일 이전의 마지막 유효 revision도 포함**해야 한다. 기간 내에 생성된 revision만 읽으면 오래 유지된 일정이 없는 것으로 나온다.

이력은 `(loopId, effectiveDate/At, sequence)` 탐색 인덱스와 안정적인 정렬을 둔다. 날짜별로 1개의 적용 revision만 선택한다. 이력 JOIN으로 한 실행이 여러 행이 되면 완료 횟수·시간이 배수로 증가하므로 단일 매칭을 검증한다. 같은 날 전체 이력이 여러 개일 수 있으므로 `(loopId, date)`를 이력 PK로 사용하면 안 된다.

공통 판정기는 날짜 순회와 정렬된 revision 순회 또는 인덱싱으로 계산한다. 날짜×루프×전체 이력을 매번 검색하거나 DAO를 개별 호출하지 않는다. 월간은 제한된 범위로 읽고, 전체 누적 지표는 불필요한 무기한 날짜 확장을 피한다. 기존 기록 기반 완료 횟수·투자시간은 이력 JOIN 없이 계산 가능한 부분을 유지한다.

이력·실행·메모의 여러 Flow를 단순 combine하면 커밋은 원자적이어도 서로 다른 방출 시점이 일시적으로 섞일 수 있다. 화면에 숫자 점프나 잘못된 알림이 발생하지 않게 일관된 읽기 스냅샷/조회 모델을 검토한다. 이력 테이블만 바뀌어도 관련 조회가 갱신돼야 한다. 기존 unscoped DB 제공과 `enableMultiInstanceInvalidation()` 구조에서는 프로세스 안의 임의 Mutex만으로 모든 DB 쓰기를 보호했다고 가정하지 않는다.

## 8. 메모리 SQLite 확인 결과

실제 저장 DB를 열지 않고 저장소 v9 JSON으로 메모리 테이블을 만들었다. 아래 과거 조회·순위·응답 조회는 `FullLoopDao.kt`에서 읽은 SQL을 사용했다. 마지막 행 초기화는 Repository가 수행하는 INSERT/REPLACE를 재현한 것으로, Kotlin 저장 함수를 직접 실행한 테스트는 아니다.

| 입력/변경 | 현재 관측 결과 |
|---|---|
| 월요일 예정, 해당 날짜 기록 없음 → 요일을 화요일로 변경 | 같은 날짜 일별 조회 행 수 `1 → 0` |
| 과거 완료 기록 존재 → 이름 변경 | 과거 조회의 이름도 `Old name → New name` |
| 과거 09시 시작 기록 유지 → 계획 시작 11시로 변경 | 과거 계획 대비 차이가 `-120분` |
| 과거 시간제 완료 유지 → 현재 언제든지 전환 | 과거 응답 DTO의 `isAnyTime = true` |
| DONE 1개 + DISABLED 1개 | 순위 완료율 `0.5`, 기간 응답 조회는 DONE 1개만 반환 |
| 오늘 DONE 행 → 현재 Repository의 활성 요일 설정 저장 방식 적용 | `done = 1 → 0` |

## 9. 회귀 테스트 목록

| 범위 | 필수 시나리오/기대 |
|---|---|
| 설정 저장 | 이름·색만 수정한 후 DONE/SKIP/IN_PROGRESS·실측 시각 보존 |
| 변경 이력 | 최초 생성, 여러 필드 한 번 저장, 동일값 저장, 취소, 같은 ms 연속 저장, A→B→A 각각 검증 |
| 요일 변경 | 월수금→화목으로 변경해도 변경 전 예정일·완료·미응답·분모 유지 |
| 시간 변경 | 09~10시→11~12시 변경 후 과거 시간 합계·정시율 불변 |
| 유형 전환 | 시간제↔언제든지 이후 과거 표본·진행 중 start/stop·위젯 상태 보존 |
| 활성 전환 | 꺼진 기간에 행이 없어도 미응답 미생성, 과거 완료 유지, 재활성화 후 예정일 복원 |
| 적용 경계 | 당일 실행 전/중/후 변경, 동일 날짜 여러 변경, 날짜별 적용 정책 확인 |
| 주간 목표 | 주중 목표 변경, 요일 축소의 목표 자동 보정, 명시 목표 0, 생성 주·비활성 주 |
| 과거 기록 수정 | 당시 설정 사용, 상태만 변경 시 시각·revision 유지, 기존 행 없는 날짜·메모만 있는 날짜 |
| 자정 | 전날 23시 시작 후 오늘 시간/요일/유형 변경, 완료를 전날 행에 한 번 기록 |
| 지연 액션 | 설정 변경 전 알림·위젯 버튼, 삭제 후 버튼, 두 번 누름, stale revision 처리 |
| 삭제·복원 | 삭제 취소 후 실행·메모·모든 revision·통계 원상복구, 취소 알람 경합으로 루프 재생성 없음 |
| 누락 기간 | 앱 장기 미실행, query 시작 이전 revision, 실행 행 없는 예정일, 신규/기존 데이터 경계 |
| 통계 일관성 | 동일 기간·정책의 홈/상세/일별/월간/순위 분자·분모 일치, 빈 표본과 0% 구분 |
| 집계 특성 | 이름·색만 바꾸면 모든 수치 불변, 이후 날짜 설정 변경이 이전 확정 구간 수치를 바꾸지 않음 |
| 시간 품질 | -1/-1, 부분 결측, 실제 00:00, 밤샘, 실측/계획 혼합, DST·timezone·윤일 |
| 원자성·성능 | 저장 중 실패 rollback, UI+Worker+Scheduler 동시 쓰기, JOIN 중복 없음, 많은 루프·오래된 이력 조회 |
| migration | 기존 모든 버전 업그레이드, FK·schema 검증, 기존 기록 유실 없음, 이력 불명 기간을 사실로 오인하지 않음 |

기존 테스트 중 `RecentLoopCompletionTest`는 비예정 요일의 저장 기록을 제외하는 동작을 고정하고 있다. `MonthInsightModelsTest`는 반대로 변경된 일정에서도 저장 기록을 보존하도록 검증한다. 이는 지금의 서로 다른 정책이므로 기대값을 무조건 유지하거나 일괄 삭제하지 말고 공통 정책 확정 후 수정한다. `DetailStatsTest`, `DetailActivityStatsTest`, `ScheduleDraftTest`, `LoopEditorDraftTest`, `WidgetStateTest`를 함께 확장한다.

현재 테스트 목록에는 이력·Repository 저장 원자성·Room migration 검증이 없다. 순수 계산 단위 테스트 외에 실제 Room DAO/Repository 통합 테스트가 필요하다. `app/build.gradle`에는 room-testing 의존성이 없고 `HiltJUnitRunner` 설정에 대응하는 소스도 이번 소스 목록에서 찾지 못했으므로, 계측 테스트 실행 구성을 먼저 확인한다.

## 10. 권장 구현 순서

1. 날짜별 적용 시점, 분모, 실제 시각 출처, 기존 이력 불명 기간, 주간 목표와 이름 표시 정책을 문서로 고정한다.
2. 저장소의 오늘 기록 초기화와 알람 취소의 DB 쓰기를 먼저 분리하고 회귀 테스트를 만든다.
3. 이력 엔티티·DAO·migration·생성/수정/삭제/복원 트랜잭션을 구현한다.
4. 실행별 계획 연결과 공통 날짜 판정기를 구현한다. 기존 데이터 fallback과 구간 선행 revision 조회를 포함한다.
5. 일별·월간·상세·홈·통계 순으로 입력을 전환하고 같은 데이터의 분자·분모를 비교한다. 실행 횟수·시간의 중복 합산을 확인한다.
6. 위젯·알림·Scheduler·밤샘 실행을 공통 실행 명령과 날짜 판정에 연결한다.
7. 레거시 경로·안내 문구·CSV 확장 여부를 정리하고 migration·동시성·기기 회귀를 통과시킨다.

CSV는 현재 날짜·상태·회고와 상단 현재 이름만 내보낸다. 이력까지 반출하려면 호환성을 고려한 추가 열 또는 별도 이력 파일이 필요하지만, 통계 정확성을 위한 필수 조건은 아니다. Manifest의 `allowBackup=false`와 소스 검색 결과상 별도 클라우드 동기화/가져오기 구현은 확인되지 않았다. `syncLoops()`는 원격 동기화가 아닌 로컬 알람·실행 행 동기화이므로 존재하지 않는 동기화 계층을 전제로 범위를 늘릴 필요는 없다.

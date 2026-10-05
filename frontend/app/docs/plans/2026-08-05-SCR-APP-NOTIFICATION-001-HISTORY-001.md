# PLAN — SCR-APP-NOTIFICATION-001(알림) + SCR-APP-HISTORY-001(이용 내역)

> 대상 Phase: PHASE.md "Phase 6" (2026-08-05 재정렬로 두 화면이 하나의 Phase로 묶임)
> 근거 문서: docs/PROJECT_GUIDE.md §5.6(RENTAL-002)·§5.11(ALERT-001)·§9.2(상태 Chip 표시 규칙) /
> docs/app-screen-flow.md §2.6(SCR-APP-HISTORY-001)·§2.7(SCR-APP-NOTIFICATION-001)

---

## 1. 구현 목표

- **이용 내역**: 하단 탭 "내역조회"에서 대여 이력을 조회한다(`ALL`/`ACTIVE`/`COMPLETED`/`EMPTY`/`CARD_EXPANDED`).
- **알림**: 홈 화면 알림 벨(현재 no-op)을 눌러 알림 목록을 확인한다(`LIST_WITH_UNREAD`/`EMPTY`/`SWIPE_ACTION`).
- 둘 다 Mock 우선, 서버 영속 저장 없음(알림 읽음 상태는 로컬 세션에서만).
- 챗봇의 `OPEN_HISTORY` CTA(현재 자리만 있고 no-op)를 이 화면으로 실제 연결.

## 2. 만들 파일 목록

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/types/history.ts` | `RENTAL-002` 응답 타입(`RentalHistoryItem`, `RentalHistoryResponse`, 상태 필터 유니언) |
| 신규 | `src/api/historyApi.ts` | `RENTAL-002` Mock — status 필터 지원, `nextCursor`는 항상 `null`(페이지네이션 자체는 §2.6 명시대로 이번 Phase 범위 아님, API 모양만 cursor 계약 그대로) |
| 수정 | `src/screens/SCR-APP-HISTORY-001/HistoryScreen.tsx` | 자리표시자 → 실제 목록 UI로 전체 작성 |
| 신규 | `src/types/alert.ts` | `ALERT-001` 응답 타입(`AlertItem`, `AlertType`/`AlertActionType` 상수 — §5.11 원문 값만 사용) |
| 신규 | `src/api/alertApi.ts` | `ALERT-001` Mock |
| 신규 | `src/screens/SCR-APP-NOTIFICATION-001/NotificationScreen.tsx` | 알림 목록 화면 신규 작성 |
| 수정 | `src/navigation/RootStackNavigator.tsx` | `Notification` 라우트 추가(`Chatbot`과 동일하게 네이티브 기본 헤더 + minimal 뒤로가기) |
| 수정 | `src/components/layout/AppHeader.tsx` | 알림 벨에 `onPressAlert?: () => void` prop 추가(기존엔 눌러도 아무 반응 없었음) |
| 수정 | `src/screens/SCR-APP-HOME-001/HomeScreen.tsx` | 알림 벨 `onPressAlert`를 `Notification` 화면 이동으로 연결 |
| 수정 | `src/screens/SCR-APP-CHATBOT-001/ChatbotScreen.tsx` | `handleAction`의 `OPEN_HISTORY` 케이스를 `Tabs → History` 실제 이동으로 연결 |
| 수정 | `App.tsx` | `GestureHandlerRootView`로 최상단 감싸기(`react-native-gesture-handler` 사용을 위한 RN 공식 요구사항) |

- 신규 의존성: `react-native-gesture-handler`(Expo SDK 54 호환 `~2.28.0`) — 알림 스와이프 읽음 처리용,
  사용자 승인 완료(§7).

- 마이페이지(`MyPageScreen.tsx`)의 알림 벨은 이번 Phase 범위 아님(PHASE.md에 "홈 화면의 알림 벨"만 명시) —
  건드리지 않는다. 현재도 배지만 표시, 탭해도 무동작인 채로 유지.

## 3. `historyApi.ts` / `types/history.ts` 설계 (§5.6 원문 그대로)

```ts
// types/history.ts
export type HistoryStatusFilter = 'ALL' | 'ACTIVE' | 'COMPLETED' | 'LOST' | 'FAILED'

export interface RentalHistoryItem {
  rentalId: string
  status: RentalStatus            // 기존 types/rental.ts RENTAL_STATUS 재사용
  checkoutStationName: string
  returnStationName: string | null
  rentedAt: string
  endedAt: string | null
  dueAt: string
  settlementAmount: number
  settlementStatus: SettlementStatus | null   // 기존 types/settlement.ts 재사용
  failureCode: string | null
  failedAt: string | null
}

export interface RentalHistoryResponse {
  items: RentalHistoryItem[]
  nextCursor: string | null
}
```

- `historyApi.ts`는 `MOCK_ITEMS: RentalHistoryItem[]` 고정 배열(대여중 1건, 반납완료+정산완료 1건,
  반납완료+미정산 1건, 분실 1건, 실패 1건 — 필터·Chip·정산 뱃지 조합을 한 번에 확인 가능하게 구성)을
  두고 `getRentalHistory({ status })`가 `status !== 'ALL'`이면 클라이언트에서 필터링해 반환.
  `nextCursor`는 항상 `null`(페이지네이션 UI 자체는 이번 Phase 범위 아님 — app-screen-flow.md §2.6 명시).

## 4. `HistoryScreen` 화면 구성

- 상단: 필터 탭 5개 — **전체 / 대여중 / 완료 / 분실 / 실패**(`ALL`/`ACTIVE`/`COMPLETED`/`LOST`/`FAILED`,
  `RENTAL-002` status 쿼리 enum 그대로)
- 헤더: Home/MyPage와 동일한 공용 `AppHeader`(로고+학교명+알림 벨) 재사용. `schoolName`/`alertCount`는
  MyPageScreen과 같은 방식으로 `HOME-001`(`getHomeSummary`) 재사용(전용 API 새로 안 만듦, 기존 §5.4
  근거 그대로)
- 각 카드: 대여소(`checkoutStationName` → `returnStationName`), 대여일시~반납일시(`rentedAt`~`endedAt`,
  진행 중이면 "대여중"), 상태 Chip(§9.2 규칙 재사용 — 대여중/반납완료/연체중(`ACTIVE`+`dueAt` 초과를
  클라이언트에서 서버 시각 비교, 새 Enum 안 만듦)/분실·파손/그 외 원문 status 그대로), 정산 금액+뱃지
  (`settlementStatus`가 `PENDING`이면 "미정산" 강조, `PAID`면 "정산완료")
- **`CARD_EXPANDED`**: 카드 탭하면 로컬 state(`expandedRentalId`)로 펼쳐서 반납 대여소·정확한 일시·
  실패 사유(`failureCode`/`failedAt`, 있을 때만) 추가 표시. 네비게이션 없이 인라인 아코디언.
- **`EMPTY`**: 현재 필터 기준 0건이면 "○○ 내역이 없어요" 안내
- Loading/Error: 기존 화면들과 동일하게 `LoadingScreen`/`ErrorBanner`+"다시 시도"
- Pull-to-Refresh: 현재 필터로 재조회(기존 홈 화면과 동일 패턴)

## 5. `alertApi.ts` / `types/alert.ts` 설계 (§5.11 원문 그대로)

```ts
// types/alert.ts
export const ALERT_TYPE = {
  RENTAL_DUE_SOON: 'RENTAL_DUE_SOON',
  UNSETTLED: 'UNSETTLED',
  RETURN_COMPLETED: 'RETURN_COMPLETED',
  ADMIN_REVIEW: 'ADMIN_REVIEW',
  RECOVERY_REQUIRED: 'RECOVERY_REQUIRED',
} as const

export const ALERT_ACTION_TYPE = {
  OPEN_MY_PAGE: 'OPEN_MY_PAGE',
  OPEN_HISTORY: 'OPEN_HISTORY',
  NONE: 'NONE',
} as const

export interface AlertItem {
  alertKey: string
  type: AlertType
  title: string
  message: string
  occurredAt: string
  action: { type: AlertActionType; targetId: string | null }
}
```

- 응답 자체엔 `read` 필드가 있지만(§5.11) "P0에서는 App 로컬 상태"라 Mock 응답엔 포함하지 않고, 화면이
  자체 `Set<string>`(읽은 `alertKey` 모음)으로 관리한다. 새로고침하면 초기화되는 게 정상 동작
  (PHASE.md 원문 그대로).
- `alertApi.ts`는 `MOCK_ITEMS: AlertItem[]` 고정 배열(5개 타입 각 1건씩, 그중 1~2개는 `action.type`이
  `OPEN_MY_PAGE`/`OPEN_HISTORY`가 되도록 구성)을 반환.

## 6. `NotificationScreen` 화면 구성

- `RootStackNavigator`에 `Chatbot`과 동일한 방식으로 등록(네이티브 기본 헤더, `title: '알림'`,
  `headerBackButtonDisplayMode: 'minimal'`)
- `LIST_WITH_UNREAD`: 목록 렌더, 안 읽은 항목은 왼쪽 점 표시 등으로 구분
- `EMPTY`: 목록이 0건이면 안내 문구
- **`SWIPE_ACTION`(읽음 처리)**: `react-native-gesture-handler`의 `Swipeable`로 구현 — 항목을 옆으로
  스와이프하면 "읽음" 액션 노출, 확정 시 로컬 `읽은 alertKey` Set에 추가
- 항목 탭(스와이프와는 별개) → 읽음 처리 + `action.type`이 `NONE`이 아니면 해당 화면으로 이동:
  - `OPEN_MY_PAGE` → `Tabs → MyPage → MyPageHome`(챗봇과 동일 패턴)
  - `OPEN_HISTORY` → `Tabs → History`(이번 Phase에서 새로 생기는 화면이라 처음으로 실제 연결됨)
- Loading/Error도 기존 화면들과 동일 패턴

## 7. 확인된 결정 사항 (2026-08-05 사용자 확인)

- **읽음 처리 UX**: 진짜 스와이프 제스처로 구현 — `react-native-gesture-handler`(Expo SDK 54 호환
  버전 `~2.28.0`) 신규 설치 후 `Swipeable`로 구현. `babel-preset-expo`가 설치만 되면 자동으로
  `react-native-gesture-handler`를 인식하므로 babel 설정 별도 수정은 불필요(챗봇 Phase에서
  `react-native-worklets` 자동 인식됐던 것과 같은 방식). 단, RN 공식 가이드대로 앱 최상단
  (`App.tsx`)에 `GestureHandlerRootView`로 감싸는 작업이 필요 — 이번 Phase에서 `App.tsx`도
  같이 수정한다(§2 파일 목록에 추가).
- **이용 내역 필터 탭**: `LOST`·`FAILED`도 별도 탭으로 추가 — 총 5개 탭(전체/대여중/완료/분실/실패,
  `RENTAL-002`의 status 쿼리 enum 그대로 노출).
- **이용 내역 헤더**: Home/MyPage와 동일한 공용 `AppHeader` 재사용.

## 8. 하지 않는 일

- `ALERT-002`(서버 영속 읽음 저장) — 여전히 `DEFERRED_NOT_CONTRACTED`
- 이용 내역 실제 무한 스크롤/커서 페이지네이션 UI — API 계약은 맞추되 동작은 다음 기회
- 마이페이지 알림 벨 연결(이번 Phase 범위 아님)
- 챗봇의 `OPEN_MAP` CTA 연결(Phase 8 지도 완료 후)

## 9. 테스트 방법

1. 하단 탭 "내역조회" → 전체/대여중/완료 필터 전환, 각 상태 Chip·정산 뱃지 확인
2. 카드 탭 → 펼침(`CARD_EXPANDED`) 확인, 필터별 `EMPTY` 상태 확인
3. 홈 화면 알림 벨 탭 → 알림 목록 진입, 항목 탭 → 읽음 표시 변화 + `OPEN_MY_PAGE`/`OPEN_HISTORY` 이동 확인
4. 챗봇에서 분실/파손 등 질문 후 나오는 `OPEN_HISTORY` CTA(있다면) → 실제 내역조회 탭 이동 확인
5. `npx tsc --noEmit` 통과 확인

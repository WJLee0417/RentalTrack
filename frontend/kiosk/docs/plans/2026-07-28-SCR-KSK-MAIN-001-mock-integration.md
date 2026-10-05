# PLAN: SCR-KSK-MAIN-001 — Mock 데이터·상태 연동

> Phase: **Phase 1 (SCR-KSK-COMMON, PHASE.md 기준)** 잔여 작업
> 이전 PLAN(`2026-07-28-SCR-KSK-MAIN-001.md`)의 1~5단계는 완료, 이 문서는 남은 6~8단계(PROGRESS.md 기준)를 다룬다.
> 이 PLAN은 SCR-KSK-MAIN-001의 `DEFAULT`/`RENT_DISABLED_NO_STOCK` variant만 다룬다. `RETURN_DISABLED_WIREFRAME_ONLY`는 09번 문서(§2 표, WF-CHANGE-009)에서 제거 권장 상태라 구현하지 않는다.
> 아직 승인 전 — **이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## 1. 구현 목표

`MainScreen.tsx`에 하드코딩된 `MOCK_USABLE_SLOT_COUNT` 상수를 제거하고, Mock API(`src/api/stationApi.ts`) + Zustand 스토어(`src/store/kioskMainStore.ts`)를 통해 재고 수량을 비동기로 조회하여 화면에 반영한다.

- 목표는 **구조 완성**이지 실제 서버 연동이 아니다 — CLAUDE.md 코드 스타일 "API 호출은 src/api에 작성, 실제 호출 없이 Mock 데이터로 우선 개발" 원칙을 따른다.
- 대응 API 후보: `KSK-STATION-001`(`GET /api/v1/kiosk/station-summary`) — PROJECT_GUIDE.md §5.4. 09번 문서(§3.1)의 구 명칭 `CAND-KSK-LOCAL-AVAILABILITY-001`(Pi 대상)은 v2.0 아키텍처 교체(`DEC-024`)로 대체된 것으로 간주한다.
- 화면 자체의 마크업/스타일은 이미 완료(1~5단계) — 이번 범위는 **상태 관리 계층 추가**로 한정한다.

---

## 2. 만들거나 수정할 파일 목록

| 구분 | 파일 |
|---|---|
| 신규 | `src/types/slot.ts` |
| 신규 | `src/api/stationApi.ts` |
| 신규 | `src/store/kioskMainStore.ts` |
| 수정 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` |
| 수정 | `src/App.tsx` |

---

## 3. 각 파일의 역할

### `src/types/slot.ts` (신규)
- `StationSummary` 응답 형태(인터페이스)를 정의: `usableSlotCount`, `observedAt`, `stale`.
- `MainScreenVariant`를 `MainScreen.tsx` 내부 로컬 타입에서 이곳으로 이동 — CLAUDE.md 코드 스타일 "상태 코드/enum은 src/types에 문자열 상수로 중앙 관리"를 따르기 위함. `screen.ts`(SCREEN_ID)와 동일한 패턴(문자열 상수 객체 + `typeof` 파생 타입)을 사용한다.
- 새 상태값을 임의로 만들지 않는다 — variant는 기존 2종(`DEFAULT`, `RENT_DISABLED_NO_STOCK`)만 유지.

### `src/api/stationApi.ts` (신규)
- `fetchStationSummary(): Promise<StationSummary>` Mock 함수 1개.
- 실제 네트워크 호출 없음. `setTimeout` 기반 인위적 지연으로 로딩 상태를 재현.
- 파일 상단에 `KSK-STATION-001` 대응이라는 것과 실제 스키마 미확인 상태임을 주석으로 남긴다(문서 근거: PROJECT_GUIDE.md §5.4 표, §13.4).

### `src/store/kioskMainStore.ts` (신규)
- Zustand 스토어. `MainScreen`이 구독하는 유일한 상태 출처.
- `fetchStationSummary` 액션이 `stationApi`를 호출해 상태를 채운다.
- variant 계산 로직도 이 스토어에 둔다(3절 상태 관리 방식 참고).

### `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (수정)
- `MOCK_USABLE_SLOT_COUNT`, `variant` prop 제거.
- 마운트 시 스토어의 `fetchStationSummary` 호출.
- 스토어의 `isLoading` / `usableSlotCount` / `variant`를 구독해 렌더링.
- 로딩 중에는 기존 Phase 1 산출물인 `LoadingScreen`(`src/screens/common/LoadingScreen.tsx`)을 재사용.

### `src/App.tsx` (수정)
- `MainScreen`에 하드코딩해 넘기던 `variant="DEFAULT"` prop 제거(스토어가 내부적으로 결정하므로).
- `onRent` 콜백으로 `CameraCaptureScreen`을 띄우는 기존 배선은 **건드리지 않는다** — 이는 Phase 2 성격 기존 커밋(PROGRESS.md "Phase 범위 초과" 절 참고)이며 이 PLAN의 범위 밖이다.

---

## 4. 상태 관리 방식

Zustand 스토어(`useKioskMainStore`) 1개로 관리한다.

```
state:
  usableSlotCount: number | null   // 조회 전 null
  isLoading: boolean
  isStale: boolean                 // 서버가 stale=true를 반환한 경우
  error: string | null

action:
  fetchStationSummary(): Promise<void>
    - isLoading = true, error = null 로 시작
    - stationApi.fetchStationSummary() 호출
    - 성공: usableSlotCount / isStale 갱신, isLoading = false
    - 실패: error 기록, isLoading = false

derived (컴포넌트에서 계산, 스토어에 별도 저장하지 않음):
  variant =
    (usableSlotCount === 0 || isStale || error) ? RENT_DISABLED_NO_STOCK
    : DEFAULT
```

- **fetch 시점**: `MainScreen` 마운트 시 1회. 폴링/주기 갱신은 이번 범위에 포함하지 않는다(8절 참고).
- **variant를 스토어에 별도 저장하지 않는 이유**: `usableSlotCount`가 SSOT이고 variant는 그로부터 파생되는 화면 표시값이므로, 두 곳에 상태를 나눠 저장하면 불일치 위험이 생긴다. PROJECT_GUIDE.md가 `RENTAL-001.dueAt` 관련해 강조하는 "서버 산정값만 표시, 클라이언트 재계산 금지" 원칙과는 다른 사안(단순 0/비0 분기이며 서버가 별도 정책 계산을 하는 값이 아님)이라 클라이언트 파생을 허용한다.
- **stale/error 처리**: 09번 문서 §3.1 "상태·복구 — Partial Success"의 "재고 수량을 알 수 없으면 대여만 보수 차단하고 반납은 가능한 경로를 유지한다" 원칙에 따라, `stale`이거나 조회 실패 시 **새 variant를 만들지 않고 기존 `RENT_DISABLED_NO_STOCK`으로 보수적으로 처리**한다(대여 버튼 비활성, 반납 버튼은 계속 활성 유지).

---

## 5. Mock 데이터 구조

```ts
interface StationSummary {
  usableSlotCount: number
  observedAt: string   // ISO 8601
  stale: boolean
}
```

- `stationApi.ts` 내부에 시나리오 전환용 상수(예: 재고 있음 / 재고 0 / stale)를 두고, 수동 테스트 시 이 상수 값만 바꿔서 각 분기를 확인한다.
- 응답 지연은 300~800ms 사이 임의값으로 시뮬레이션해 로딩 화면이 실제로 보이는지 확인 가능하게 한다.
- 얼굴·이미지 관련 필드는 전혀 포함하지 않는다(이 화면은 인증 이전 단계).

---

## 6. 테스트 방법 (수동 확인 기준)

1. `npm run build`, `npm run lint` 통과 확인.
2. `stationApi.ts`의 Mock 시나리오를 재고 2개로 설정 후 브라우저 확인:
   - 로딩 스피너가 잠깐 보였다가 "현재 대여 가능한 우산 2개" + 대여/반납 버튼 모두 활성으로 전환되는지.
3. Mock 시나리오를 재고 0개로 설정 후 확인:
   - 대여 버튼 비활성 + "우산 재고 없음" 배지 표시, 반납 버튼은 계속 활성.
4. Mock 시나리오를 `stale: true`로 설정 후 확인:
   - 대여 버튼이 보수적으로 비활성 처리되는지(정확한 문구는 7절 리스크 참고).
5. Mock 함수가 reject하도록 임시로 바꿔 확인:
   - 화면이 깨지지 않고 대여 버튼이 비활성 상태로 안전하게 유지되는지.
6. 콘솔에 에러·경고가 없는지, `console.log` 등으로 민감정보(해당 없음, 이 화면은 얼굴/개인정보 미포함)가 남지 않는지 확인.

---

## 7. 예상 리스크

- **`KSK-STATION-001` 실제 응답 스키마 미확인**: PROJECT_GUIDE.md §5.4는 API ID·경로·역할만 명시하고 필드 단위 계약은 없음(§13.4에 명시). Mock 구조가 실제 Backend 응답과 다를 수 있어, 추후 실 연동 시 필드명 조정이 필요할 수 있다.
- **stale 상태의 정확한 UI 문구·동작 미정**: 09번 문서는 "확인 중/이용 불가"라고만 서술하고 구체적인 variant나 문구를 정의하지 않았다. 이번 PLAN은 기존 `RENT_DISABLED_NO_STOCK`(문구: "우산 재고 없음")으로 보수 처리하지만, 실제로는 "재고가 0인 것"과 "재고를 알 수 없는 것"은 사용자에게 다른 의미라 문구가 부정확할 수 있다 — 추후 재확인 필요(신규 DECISION_REQUIRED 성격).
- **폴링 정책 미정(`DEC-SF-015`류)**: 09번 문서 §6은 Kiosk 진행 상태 갱신 방식(Polling/SSE/WebSocket)을 RENT·RETURN·AUTH 화면 기준으로 미정 처리했다. MAIN 화면의 유휴 상태 재고 갱신 주기는 그 결정에 명시적으로 포함되지 않아, 이번엔 1회성 조회만 구현하고 주기 갱신은 보류한다.
- **`App.tsx` 회귀 가능성**: `MainScreen`의 prop 시그니처가 바뀌므로(`variant` prop 제거), 기존 카메라 스트림 전환 배선과의 연결부를 건드리지 않도록 최소 변경만 적용해야 한다 — 손대는 범위를 넘으면 Phase 2 성격 코드까지 수정하게 되어 CLAUDE.md "한 번에 하나의 Phase만 구현" 원칙을 벗어난다.

---

## 8. 이번 Phase에서 하지 않을 일

- 실제 Spring Kiosk API(`KSK-STATION-001`) 연동 — Mock만 사용.
- 재고 실시간 폴링/자동 갱신(주기 갱신 정책은 미정 상태로 남겨둠).
- `RETURN_DISABLED_WIREFRAME_ONLY` variant 구현(09번 문서 기준 제거 권장).
- stale 상태의 최종 확정 문구/UX 결정 — 보수적 처리만 하고 정확한 워딩은 결정하지 않음.
- `CameraCaptureScreen`, `RecognitionGuideScreen`, `SlotGuideScreen`, `kioskFlowStore`, `types/screen.ts` 등 이미 존재하는 Phase 2/3 성격 코드의 수정·정리·배선 — PROGRESS.md의 "Phase 범위 초과 커밋" 절에서 별도 이슈로 다룬다.
- PHASE.md의 `CURRENT PHASE`를 Phase 2로 전환하는 작업 — 이 PLAN 완료 후 별도로 논의.

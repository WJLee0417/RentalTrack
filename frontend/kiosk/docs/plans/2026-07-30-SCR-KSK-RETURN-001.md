# PLAN: SCR-KSK-RETURN-001 — 우산 반납 화면 (Phase 3)

> Phase: **Phase 3 (SCR-KSK-RETURN, PHASE.md 기준)**
> 대응 문서: `docs/09-screen-flow.md` §3.4 SCR-KSK-RETURN-001, PROJECT_GUIDE.md §5.4 "반납 흐름", §6 "반납 결과 표현 규칙"
> 조사 결과는 대화 중 별도로 보고함(CLAUDE.md/PROJECT_GUIDE.md/PHASE.md 확인)
> 아직 승인 전 — **이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## 1. 구현 목표

메인 화면의 "반납" 버튼(현재 `onClick` 미연결 상태)부터, 얼굴 인증(기존 SCR-KSK-AUTH-001 재사용) → 활성 대여 확인 → 우산 촬영 → AI 검수 처리 중 → 반납 슬롯 안내 → 반납 완료(AI 결과에 따라 문구 분기)까지, PHASE.md Phase 3 구현 범위 8개 항목(반납 시작 화면 / 얼굴 인증 화면 / 얼굴 인증 API(Mock) / 반납 진행 화면 / 슬롯 안내 / AI 검수 결과 화면 / 완료 화면 / 예외 처리)을 구현한다.

**핵심 설계 결정 — 얼굴 인증 화면 재사용**: 조사 결과 `AuthScreen.tsx`/`faceAuthStore.ts`는 RENT 전용 로직을 담고 있지 않다(variant 5종은 모드 무관, "인증 성공 이후 무엇을 할지"는 전부 `App.tsx`의 `onAuthenticated` 콜백에 위임돼 있음). 따라서 **`AuthScreen`/`faceAuthStore`/`faceAuthApi`는 코드 변경 없이 그대로 재사용**하고, `App.tsx`가 반납 모드일 때 다른 `onAuthenticated` 분기를 실행하도록만 확장한다.

**대응 API 후보**: `KSK-RETURN-001`(PROJECT_GUIDE.md §5.4) — 이번 PLAN에서는 Mock 3개 함수(활성 대여 확인 / AI 검수 / 반납 시작)로 단순화한다(실제 폴링·세션 상태 조회는 이번 범위 밖, 8절 참고).

---

## 2. 만들거나 수정할 파일 목록

| 구분 | 파일 |
|---|---|
| 신규 | `src/types/inspection.ts` |
| 신규 | `src/api/returnEligibilityApi.ts` |
| 신규 | `src/api/inspectionApi.ts` |
| 신규 | `src/api/returnApi.ts` |
| 신규 | `src/screens/SCR-KSK-RETURN-001/ReturnCaptureScreen.tsx` |
| 신규 | `src/screens/SCR-KSK-RETURN-001/ReturnSlotScreen.tsx` |
| 신규 | `src/screens/SCR-KSK-RETURN-001/ReturnCompleteScreen.tsx` |
| 수정 | `src/types/screen.ts` |
| 수정 | `src/types/eligibility.ts` |
| 수정 | `src/components/common/RentalBlockModal.tsx` |
| 수정 | `src/screens/common/LoadingScreen.tsx` |
| 수정 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` |
| 수정 | `src/App.tsx` |
| 변경 없음(재사용 확인) | `src/screens/SCR-KSK-AUTH-001/AuthScreen.tsx`, `src/store/faceAuthStore.ts`, `src/api/faceAuthApi.ts`, `src/screens/common/CameraCaptureScreen.tsx`, `src/components/common/UmbrellaGuideOverlay.tsx`, `src/components/common/SlotActionCard.tsx`, `src/components/icons/UmbrellaSlotAnimationIcon.tsx`(이미 `direction="return"` 지원) |

---

## 3. 각 파일의 역할

### 신규

| 파일 | 역할 |
|---|---|
| `types/inspection.ts` | `INSPECTION_RESULT` 상수(`NORMAL`/`DAMAGED`/`UNCERTAIN`/`FAILED`, PROJECT_GUIDE.md §6·PHASE.md AI 결과 정책 기준) |
| `api/returnEligibilityApi.ts` | `checkActiveRentalForReturn()` — 반납 선택+얼굴 인증 후 "활성 대여 확인"(09번 문서 §4.1) 대응 Mock. `'FOUND'` 또는 `RENTAL_BLOCK_REASON.ACTIVE_RENTAL_NOT_FOUND` 반환 |
| `api/inspectionApi.ts` | `inspectUmbrella()` — 우산 촬영 후 AI 검수(YOLOv8n+PatchCore, PROJECT_GUIDE.md §3) 대응 Mock. `InspectionResult` 하나를 지연 후 반환 |
| `api/returnApi.ts` | `startReturn()` — 반납 슬롯 배정(09번 문서: "AI 결과 기록 후에만 원자 선정") 대응 Mock. `{ slotNumber, returnedAt }` 반환 |
| `screens/SCR-KSK-RETURN-001/ReturnCaptureScreen.tsx` | `UMBRELLA_CAPTURE` 대응. `CameraCaptureScreen` + `UmbrellaGuideOverlay` 조합, 내부에 자동 촬영 타이머(AuthScreen의 `AUTO_CAPTURE_DELAY_MS` 패턴과 동일하게 자체 관리) 후 `onCaptured` 콜백 호출 |
| `screens/SCR-KSK-RETURN-001/ReturnSlotScreen.tsx` | `RETURN_SLOT_GUIDE` 대응. `SlotActionCard`(`type="return"`)로 슬롯 번호·삽입 안내·"넣었어요" 버튼 렌더링 |
| `screens/SCR-KSK-RETURN-001/ReturnCompleteScreen.tsx` | `RETURN_COMPLETED` 대응. `inspectionResult`에 따라 제목/부제 문구 분기(PROJECT_GUIDE.md §6 표 그대로: `NORMAL`="반납이 완료되었습니다.", 나머지="반납은 완료되었으며 우산 상태는 관리자가 확인합니다."), `RentalCompleteScreen`과 동일하게 5초 후 자동 홈 복귀 |

### 수정

| 파일 | 변경 내용 |
|---|---|
| `types/screen.ts` | `SCREEN_ID.RETURN = 'SCR-KSK-RETURN-001'` 추가 |
| `types/eligibility.ts` | `RENTAL_BLOCK_REASON`에 `ACTIVE_RENTAL_NOT_FOUND` 추가(기존 주석 "반납 흐름 전용이라 이번 범위에 포함하지 않는다" 해제) |
| `components/common/RentalBlockModal.tsx` | `MESSAGES` 맵에 `ACTIVE_RENTAL_NOT_FOUND` 문구 추가("반납할 대여 내역이 없어요" 등). 컴포넌트/스토어 이름은 그대로 두고 재사용(7절 리스크 참고) |
| `screens/common/LoadingScreen.tsx` | 하드코딩된 `ai-scan-user.svg` 대신 선택적 `icon` prop 추가(기본값 `ai-scan-user.svg` 유지, 기존 `FACE_PROCESSING` 호출부는 변경 없음). `INSPECTION_PROCESSING`에서 미사용 상태였던 `ai-scan-umbrella.svg`를 전달 |
| `screens/SCR-KSK-MAIN-001/MainScreen.tsx` | 반납 `ActionCard`에 `onClick`이 없는 상태 — `onReturn` prop을 받아 연결(현재 `onRent`만 있음) |
| `App.tsx` | `mode`(`'RENT' \| 'RETURN'`) 로컬 상태 추가(대여/반납 버튼 클릭 시 세팅) → `handleAuthenticated`를 모드별로 분기하는 `handleReturnAuthenticated` 추가. `returnStep`(`'CAPTURE'\|'PROCESSING'\|'SLOT_GUIDE'\|'COMPLETED'`)·`inspectionResult`·`returnSlot` 로컬 상태 추가, `SCREEN_ID.RETURN` 렌더링 분기 추가 |

---

## 4. 상태 관리 방식

- **화면 전환**: 기존 `kioskFlowStore`(`currentScreen`)에 `SCREEN_ID.RETURN` 한 값만 추가. AUTH 화면은 RENT/RETURN 공용이라 여전히 `SCREEN_ID.AUTH` 하나로 유지.
- **RENT/RETURN 모드 구분**: 전역 스토어에 넣지 않고 `App.tsx`의 로컬 `useState<'RENT' | 'RETURN'>`로 관리한다. 기존 `rental`/`isPickedUp`도 `kioskFlowStore`(전역)가 아니라 `App.tsx` 로컬 상태로 관리되고 있는 패턴을 그대로 따른 것 — RENT 흐름과의 일관성을 우선했다.
- **반납 진행 단계(`returnStep`)**: `faceAuthStore`처럼 전용 Zustand 스토어를 새로 만드는 대신, RENT 흐름(`rental`/`isPickedUp`)과 동일하게 **`App.tsx` 로컬 상태**로 관리한다.
  - 검토한 대안: `faceAuthStore`와 동일하게 `returnFlowStore.ts`를 새로 만드는 방법도 있었으나, RENT-001이 이미 로컬 상태만으로 구현되어 있고 반납 진행 단계도 "촬영→처리 중→슬롯 안내→완료" 4단계의 선형 흐름이라 별도 스토어 없이 로컬 상태로 충분하다고 판단했다. AUTH-001에 전용 스토어가 있는 이유는 `retry`(재촬영)처럼 화면 자체에서 여러 진입점으로 되돌아가는 로직이 있어서였는데, 이번 반납 진행 단계에는 그런 분기가 없다.
  - 자동 촬영 타이머는 `ReturnCaptureScreen.tsx` 내부(컴포넌트 로컬)에서 관리(AuthScreen의 `AUTO_CAPTURE_DELAY_MS` 패턴과 동일).
- **활성 대여 없음(`ACTIVE_RENTAL_NOT_FOUND`) 표시**: 새 모달을 만들지 않고 기존 `kioskMainStore.rentalBlockReason` + `RentalBlockModal`을 그대로 재사용(문구만 추가). 필드/컴포넌트 이름이 "Rental"에 국한돼 있는 채로 반납 흐름에도 쓰이게 되는 점은 리스크로 기록(7절).

---

## 5. Mock 데이터 구조

```ts
// types/inspection.ts
export const INSPECTION_RESULT = {
  NORMAL: 'NORMAL',
  DAMAGED: 'DAMAGED',
  UNCERTAIN: 'UNCERTAIN',
  FAILED: 'FAILED',
} as const
export type InspectionResult = (typeof INSPECTION_RESULT)[keyof typeof INSPECTION_RESULT]
```

```ts
// api/returnEligibilityApi.ts
// MOCK_SCENARIO를 바꿔서 화면 분기 확인 (기존 eligibilityApi.ts와 동일 패턴)
const MOCK_SCENARIO: 'FOUND' | typeof RENTAL_BLOCK_REASON.ACTIVE_RENTAL_NOT_FOUND = 'FOUND'
// 300~800ms 지연 후 반환 (checkRentalEligibility와 동일 지연폭)
export function checkActiveRentalForReturn(): Promise<'FOUND' | typeof RENTAL_BLOCK_REASON.ACTIVE_RENTAL_NOT_FOUND>
```

```ts
// api/inspectionApi.ts
const MOCK_SCENARIO: InspectionResult = INSPECTION_RESULT.NORMAL
// 1000~2000ms 지연 — 실제 AI 추론 체감을 위해 다른 Mock보다 약간 길게 잡음(근거 없는 임의값, 확정 아님)
export function inspectUmbrella(): Promise<InspectionResult>
```

```ts
// api/returnApi.ts
export interface ReturnStart {
  slotNumber: number
  returnedAt: string // ISO
}
const MOCK_SLOT_NUMBER = 2
// 300~800ms 지연 (rentalApi.ts의 startRental()과 동일 패턴)
export function startReturn(): Promise<ReturnStart>
```

수동 테스트 시 `MOCK_SCENARIO` 상수 3곳(활성 대여 유무 / AI 검수 결과)을 바꿔가며 분기 확인 — 기존 Mock API들과 동일한 방식.

---

## 6. 테스트 방법 (수동 확인 기준)

1. **정상 경로**: 메인 "반납" 클릭 → 얼굴 인증(기존 AUTH-001 그대로: GUIDE→FACE_CAPTURE→FACE_PROCESSING) → 인증 성공 → (활성 대여 있음, `MOCK_SCENARIO='FOUND'`) → `ReturnCaptureScreen`(카메라+사각 가이드) → 자동 촬영 후 "우산 상태를 확인하고 있어요"(로딩, 우산 아이콘) → `MOCK_SCENARIO=NORMAL`이면 `ReturnSlotScreen`(2번 슬롯, "넣어주세요") → "넣었어요" 클릭 → `ReturnCompleteScreen`("반납이 완료되었습니다.") → 5초 후 자동 홈 복귀 확인
2. **AI 결과 분기**: `inspectionApi.ts`의 `MOCK_SCENARIO`를 `DAMAGED`/`UNCERTAIN`/`FAILED`로 각각 바꿔서 반납 완료 화면 문구가 "반납은 완료되었으며 우산 상태는 관리자가 확인합니다."로 바뀌는지 확인(확정 파손 문구가 아닌지 육안 확인)
3. **활성 대여 없음**: `returnEligibilityApi.ts`의 `MOCK_SCENARIO`를 `ACTIVE_RENTAL_NOT_FOUND`로 바꿔서, 인증 성공 후 촬영 화면으로 안 가고 메인 화면 + 모달("반납할 대여 내역이 없어요") 노출 확인
4. **얼굴 인증 실패 재사용 확인**: 반납 진입 상태에서 `faceAuthApi.ts`의 `MOCK_SCENARIO`를 `NOT_DETECTED`/`NOT_MATCHED`로 바꿔서, RENT 흐름과 동일한 오류 화면이 반납 흐름에서도 정상 표시되는지 확인(재사용 검증 목적)
5. **중간 이탈**: 촬영 화면/슬롯 안내 화면에서 "이전"·"홈으로" 클릭 시 `mode`·`returnStep`·`inspectionResult`·`returnSlot`이 초기화되고 메인 화면으로 정상 복귀하는지 확인
6. 매 파일 변경 후 `npm run build` 통과 확인. 위 시나리오는 `npm run dev`로 브라우저에서 직접 확인(카메라 권한 필요 — `AuthScreen`과 동일하게 실제 Orin 스트림 URL을 쓰므로 로컬에서는 이미지 로드 실패가 예상되나, 타이머 기반 진행에는 지장 없음 — 이전 대화에서 확인된 동작과 동일)

---

## 7. 예상 리스크

- **`RentalBlockModal`/`rentalBlockReason` 이름이 더 이상 "Rental" 전용이 아님**: 반납 흐름의 `ACTIVE_RENTAL_NOT_FOUND`도 같은 컴포넌트·상태를 쓰게 되면서 이름과 실제 역할이 어긋난다. 이번엔 리네이밍하지 않고 문구만 추가 — 나중에 `BlockModal`/`blockReason` 같은 이름으로 정리할 여지를 리스크로만 남긴다.
- **`App.tsx` 복잡도 증가**: RENT/RETURN 두 모드의 로컬 상태(`rental`/`isPickedUp` + `returnStep`/`inspectionResult`/`returnSlot`)가 한 컴포넌트에 누적된다. Phase 3 완료 후 필요하면 커스텀 훅(`useRentalFlow`/`useReturnFlow`)으로 분리하는 리팩터를 별도로 검토할 수 있다(이번 PLAN 범위 밖).
- **`FACE_NOT_MATCHED` 문구 재사용**: 현재 문구("일치하는 학생 정보를 찾을 수 없습니다")가 대여 흐름 기준으로 쓰여 있는데, 반납 흐름에서도 그대로 노출된다. 09번 문서상 두 흐름의 얼굴 인증 오류 메시지가 다르다는 근거는 없어 이번엔 그대로 재사용하지만, 어색하면 재확인 필요.
- **09-screen-flow.md §3.4.1의 신규 7개 Variant 미구현**(`UMBRELLA_NOT_DETECTED`, `RETURN_SLOT_GUIDE` 이후의 `WAITING_FOR_ITEM_INSERTION`/`LOCKING_RETURN_SLOT`/`PHYSICAL_RETURN_DONE`, `RECOVERY_REQUIRED`, `INSERTION_INCOMPLETE`, `LOCK_FAILED`) — 8절 참고, 실제 장치 연동 전까지는 의미 있게 Mock하기 어려운 상태들.
- **잠금 해제/잠금 실패 오류 코드 분리 여부 미확정**(PROJECT_GUIDE.md §13.1 리스크 3): 이번 Phase는 장치 실패 시뮬레이션 자체를 하지 않아 당장 영향은 없으나, 실기기 연동 시 재확인 필요.
- **`LoadingScreen`에 `icon` prop 추가는 기존 화면(FACE_PROCESSING) 회귀 가능성**: 기본값을 유지하도록 설계하지만, 실제 구현 시 회귀 여부를 반드시 스크린샷으로 재확인해야 한다.

---

## 8. 이번 Phase에서 하지 않을 일

- 09-screen-flow.md §3.4.1의 장치 실패류 세분화 Variant(`UMBRELLA_NOT_DETECTED`, `WAITING_FOR_ITEM_INSERTION`, `LOCKING_RETURN_SLOT`, `PHYSICAL_RETURN_DONE`, `RECOVERY_REQUIRED`, `INSERTION_INCOMPLETE`, `LOCK_FAILED`) — PHASE.md Phase 3 구현 범위 목록에 없고, 실제 장치 없이 의미 있게 Mock하기 어려워 이번 범위에서 제외한다. Rules("DECISION_REQUIRED 항목은 구현하지 않는다")에 따라 이 상태들의 정확한 트리거 조건·오류 코드가 확정되기 전까지는 보류.
- 실제 Spring Kiosk API(`KSK-RETURN-001`) 연동 — Mock만 사용.
- 관리자 최종 판정, 파손 정산(`SETTLEMENT`) 생성 로직 — PROJECT_GUIDE.md §5.5(담당하지 않는 범위)에 명시된 대로 Frontend 1 책임 밖.
- 재시도 횟수 제한, 세션 만료 타이머(`DEC-SF-010`) — DECISION_REQUIRED, Phase 2에서도 구현 안 함.
- 얼굴 정렬 실시간 안내(`FACE_ALIGNMENT_REQUIRED`) — Phase 2에서 후순위 보류 결정된 항목 그대로 유지, 이번에도 손대지 않음.
- 반납 가능 슬롯 0일 때 반납 버튼 비활성화(`RETURN_DISABLED_NO_CAPACITY`) — Phase 2에서 후순위 보류 결정된 항목, 이번 Phase 범위가 아니라 그대로 유지(메인 화면 반납 버튼에 `onClick`만 연결하고 `disabled` 로직은 추가하지 않음).
- `KSK-DIAG-001`(로컬 슬롯 진단), 팬 제어 — `OUT_OF_SCOPE`/Phase 4 영역.

---

## 변경 이력

- 2026-07-30: 최초 작성, 승인 대기 중.

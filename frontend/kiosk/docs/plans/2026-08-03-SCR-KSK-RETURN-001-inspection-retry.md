# PLAN: SCR-KSK-RETURN-001 — 검수 결과 반복(UNCERTAIN/DAMAGED) 처리 화면 추가

> Phase: **Phase 3 (SCR-KSK-RETURN, PHASE.md 기준)** 내 추가 기능
> 대응 문서: 없음 — 이번 기능은 문서(09-screen-flow.md §3.4, PROJECT_GUIDE.md §6)에 명시되지 않은 **팀 신규 결정**(4절 참고)
> 사용자 요청(2026-08-03 대화) + 클arify 질문 3개 답변 반영
> 아직 승인 전 — **이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## 1. 구현 목표

`ReturnFlow.tsx`의 AI 검수(INSPECTION_PROCESSING) 결과가 **같은 값으로 연속 3회** 나올 때 두 가지 신규 화면으로 흐름을 분기한다.

1. **UNCERTAIN 연속 3회**: 기존 `ErrorScreen`을 재사용해 "확인 불가" 안내 + "우산 재인식하기" 버튼 1개만 표시. 다른 진행 경로 없음(재인식만 가능).
2. **DAMAGED 연속 3회**: 기존 `ErrorScreen`을 재사용해 "관리자 최종 검증 후 파손 처리됩니다. 파손 처리 확정하시겠습니까?" + subtitle("진행 상황은 앱으로 알림이 전송됩니다") + 버튼 2개("예, 반납 진행 계속하기" / "아니요, 우산 재인식하기"). "예"는 `RETURN_SLOT_GUIDE`(`ReturnSlotScreen`)로, "아니요"는 재인식으로.

재인식은 **촬영 화면부터만 재시작**(안내 화면 `UmbrellaGuideScreen`은 다시 보여주지 않음), 카운트는 **연속 기준**(다른 결과가 나오면 초기화), 이번 범위에서는 **카운트·화면 로직만 구현**하고 Mock에 3회 반복 시나리오는 추가하지 않는다(사용자 확인 3건).

---

## 2. 문서와의 관계 — 먼저 알리는 충돌

- PROJECT_GUIDE.md §6·09-screen-flow.md §3.4는 "`DAMAGED|UNCERTAIN|FAILED`는 결과와 무관하게 물리 반납을 계속 진행"이 원칙이라고 명시한다. 이번 기능은 **반복 횟수 조건으로 흐름을 막는** 새 동작이라 이 원칙과 다르다.
- `KSK-RETURN-001` 프로토콜(12-I 확인 기준)은 한 번의 Request가 촬영→검수→슬롯배정→완료까지 EVENT로 쭉 이어지는 구조이고, "검수만 다시 트리거"하는 재시도 규약이 문서에 없다. 이번 구현은 Mock/클라이언트 레벨에서 `beginReturn()`을 다시 호출하는 방식으로 재인식을 흉내 낸다 — **실제 Pi 연동 시 이 재시도가 프로토콜상 유효한지는 별도 확인이 필요**하다(8절에 리스크로 기록).
- 두 사실 모두 팀 결정(이번 대화)으로 진행하되, PROGRESS.md에 "문서 대비 의도적 변경"으로 기록한다.

---

## 3. 만들거나 수정할 파일 목록

| 구분 | 파일 |
|---|---|
| 수정 | `src/screens/SCR-KSK-RETURN-001/ReturnFlow.tsx` |
| 수정 | `src/i18n/translations.ts` |
| 신규 파일 없음 | 기존 `ErrorScreen` 재사용, 새 컴포넌트 안 만듦 |

---

## 4. 상태·로직 설계 (`ReturnFlow.tsx`)

```ts
type UiStep =
  | 'GUIDE' | 'CAPTURE' | 'PROCESSING' | 'SLOT_GUIDE' | 'COMPLETED'
  | 'UNCERTAIN_BLOCKED'   // 신규
  | 'DAMAGED_CONFIRM'     // 신규

// 기존 상태 아래에 추가
const lastStreakResult = useRef<'UNCERTAIN' | 'DAMAGED' | null>(null)
const streakCount = useRef(0)
const countedThisAttempt = useRef(false) // 한 번의 시도(startReturn 1회 호출)당 1번만 스트릭 반영
```

`beginReturn()` 시작 부분에 `countedThisAttempt.current = false` 추가.

`handleEvent`의 `RETURN_SLOT_ASSIGNED | WAITING_INSERTION | SPRING_COMMITTING` case를 아래처럼 확장:

```ts
case 'RETURN_SLOT_ASSIGNED':
case 'WAITING_INSERTION':
case 'SPRING_COMMITTING': {
  if (!countedThisAttempt.current && event.inspectionResult) {
    countedThisAttempt.current = true
    const r = event.inspectionResult

    if (r === 'UNCERTAIN' || r === 'DAMAGED') {
      streakCount.current =
        lastStreakResult.current === r ? streakCount.current + 1 : 1
      lastStreakResult.current = r
    } else {
      streakCount.current = 0
      lastStreakResult.current = null
    }

    if (r === 'UNCERTAIN' && streakCount.current >= 3) {
      setStep('UNCERTAIN_BLOCKED')
      return
    }
    if (r === 'DAMAGED' && streakCount.current >= 3) {
      setStep('DAMAGED_CONFIRM')
      return
    }
  }
  setStep('SLOT_GUIDE')
  return
}
```

재인식 트리거 함수(두 신규 화면이 공유):

```ts
const retryInspection = () => {
  started.current = false
  setStep('CAPTURE') // 안내 화면 건너뛰고 즉시 촬영 화면으로 (사용자 선택)
  beginReturn()
}
```

- `streakCount`/`lastStreakResult`는 **재인식 시에도 초기화하지 않는다** — 연속 횟수를 이어서 세야 하므로.
- `countedThisAttempt`만 매 시도(`beginReturn`)마다 초기화해 같은 시도 안에서 중복 카운트되지 않게 한다.
- `NORMAL`/`FAILED`는 이번 기능 대상이 아니므로 기존 동작(그대로 `SLOT_GUIDE` 진행) 유지, 스트릭만 초기화.

렌더링 분기 추가(기존 `if (step === 'PROCESSING')` 블록 근처):

```tsx
if (step === 'UNCERTAIN_BLOCKED') {
  return (
    <ErrorScreen
      title={t.return.uncertainBlockedTitle}
      tips={t.return.uncertainBlockedTips}
      actionLabel={t.return.retryInspection}
      onAction={retryInspection}
      currentStep={3}
      flow="RETURN"
    />
  )
}

if (step === 'DAMAGED_CONFIRM') {
  return (
    <ErrorScreen
      title={t.return.damagedConfirmTitle}
      subtitle={t.return.damagedConfirmSubtitle}
      actionLabel={t.return.damagedConfirmYes}
      onAction={() => setStep('SLOT_GUIDE')}
      secondaryActionLabel={t.return.retryInspection}
      onSecondaryAction={retryInspection}
      currentStep={3}
      flow="RETURN"
    />
  )
}
```

`DAMAGED_CONFIRM`의 "예" 클릭 시 `slotNumber`는 직전 이벤트에서 이미 세팅돼 있으므로(핸들러 상단 `if (event.slotNumber !== null) setSlotNumber(...)`) 별도 처리 없이 `SLOT_GUIDE`로 전환하면 `ReturnSlotScreen`이 정상 렌더된다.

---

## 5. 문구(i18n 신규 키, `translations.ts` `return` 섹션에 추가)

| 키 | 한국어 | English(draft) |
|---|---|---|
| `uncertainBlockedTitle` | "우산이 정확히 인식되지 않았습니다" (사용자 확정) | "The umbrella couldn't be recognized clearly" |
| `uncertainBlockedTips` (string[]) | ["우산을 완전히 펼쳐 카메라 정면에 비춰주세요", "밝은 곳에서 다시 촬영하면 인식이 더 잘 됩니다"] (draft) | ["Fully open the umbrella and hold it facing the camera", "Try again in a well-lit area for better recognition"] |
| `retryInspection` | "우산 재인식하기" | "Re-scan Umbrella" |
| `damagedConfirmTitle` | "관리자 최종 검증 후 파손 처리됩니다. 파손 처리 확정하시겠습니까?" (사용자 확정) | "After final review by an admin, this will be processed as damage. Confirm damage processing?" |
| `damagedConfirmSubtitle` | "진행 상황은 앱으로 알림이 전송됩니다." (사용자 설명 반영) | "You'll be notified of the progress in the app." |
| `damagedConfirmYes` | "예, 반납 진행 계속하기" (사용자 확정) | "Yes, continue return" |

- 한국어 title/subtitle 4개는 모두 사용자가 확정한 문구. `uncertainBlockedTips`(보조 설명 2줄)만 여전히 초안 — 승인 시 그대로 쓸지 확인 필요.
- "우산 재인식하기" 버튼 라벨은 두 화면에서 공용으로 재사용(DAMAGED_CONFIRM의 보조 버튼 라벨과 동일 문구).

---

## 6. 테스트 방법 (수동 확인 기준)

- Mock에 3회 반복 시나리오가 없으므로(4절 결정), 이번 PLAN에서는 **자동으로 이 두 화면을 브라우저에서 재현할 수 없다**. `npm run build`·`npm run lint` 통과와 코드 리뷰로 검증하고, 화면 자체는 `streakCount.current`를 임시로 조작하거나(예: 개발자 도구) 추후 Mock 시나리오가 추가된 뒤 재확인한다.
- 기존 정상 경로(NORMAL/1~2회 UNCERTAIN/DAMAGED 후 NORMAL 등)가 회귀 없이 그대로 `SLOT_GUIDE`로 진행되는지는 기존 Mock(`piSocket.mock.ts`, 고정 시나리오)으로 확인 가능.

---

## 7. 이번 범위에서 하지 않을 일

- `piSocket.mock.ts`에 "N번째 시도까지 UNCERTAIN/DAMAGED 유지" 같은 시나리오 상수 추가 — 사용자 결정으로 다음 작업으로 미룸.
- UNCERTAIN 3회 화면에 "홈으로" 등 재인식 외의 탈출 경로 — 사용자가 명시한 버튼 1개(재인식)만 구현. **리스크로 기록**: 계속 UNCERTAIN이 나오면 재인식 외에는 화면을 벗어날 방법이 없음(8절 참고).
- 실제 Pi 연동 시 "재인식"이 프로토콜상 어떻게 표현돼야 하는지(새 Request인지, 기존 세션 내 재시도인지) 확정 — 2절에 리스크로 기록, 실 연동 시점에 재확인.

---

## 8. 예상 리스크

- **탈출 경로 없음**: `UNCERTAIN_BLOCKED` 화면은 재인식 버튼 1개뿐이라, 카메라/조명 문제로 계속 UNCERTAIN이 나오면 사용자가 화면에 갇힌다. 필요시 "관리자 호출"/"홈으로" 같은 보조 버튼 추가를 고려할 수 있음(이번엔 사용자 명세대로 1개만 구현).
- **재인식의 실제 백엔드 유효성 미확인**: `KSK-CANCEL-001`은 촬영 시작 전까지만 취소 허용이라고 문서화돼 있는데, 이번 재인식은 이미 검수(촬영)가 끝난 뒤 새 시도를 트리거하는 것이라 이 취소 경계와 다른 상황이다. 실 Pi 연동 전까지는 Mock 레벨 시뮬레이션으로만 유효하다.
- **연속 카운트가 화면 전환 없이 세션 내내 유지됨**: `streakCount`/`lastStreakResult`는 `ReturnFlow` 컴포넌트 생명주기 동안 유지되는 `ref`라, 반납 흐름을 벗어나지 않는 한(홈으로 나가지 않는 한) 계속 누적된다. 홈으로 나가면 `ReturnFlow`가 언마운트되며 자연히 초기화됨 — 별도 리셋 로직 불필요.

---

## 변경 이력

- 2026-08-03: 최초 작성, 승인 대기 중.
- 2026-08-03: 사용자 승인 후 구현 완료(`ReturnFlow.tsx`, `i18n/translations.ts`). `npm run build`·`npm run lint` 통과 확인. Mock 시나리오는 계획대로 미포함 — 브라우저 자동 재현은 아직 불가. 상세는 `PROGRESS.md` 참고.

# PLAN — SCR-APP-MYPAGE-001 (조회 전용) + SCR-APP-INQUIRY-001

> 대상 Phase: PHASE.md "Phase 4"
> 근거 문서: docs/PROJECT_GUIDE.md §5.3(USER-001)·§5.5(RENTAL-001)·§5.7(SETTLEMENT-001)·§7.1(연체·미정산 표시 규칙) /
> docs/app-screen-flow.md §2.5(SCR-APP-MYPAGE-001)·§2.8(SCR-APP-INQUIRY-001)
> 사용자 확인 사항(2026-08-04): 문의하기(`DEC-SF-009` 미확정)는 **네이티브 폼 + Mock API**로 구현
> (외부 링크 축소 방식 대신 선택)

---

## 1. 구현 목표

- 마이페이지 화면에서 사용자 정보·현재 대여·미정산 목록을 조회한다(`USER-001`/`RENTAL-001`/`SETTLEMENT-001` Mock).
- 결제 버튼은 UI만 두고 실제 결제 흐름은 Phase 7(Toss 연동)에서 연결한다(지금은 no-op).
- 연장(`EXTENSION_CONFIRM`)·신고(`INCIDENT_REPORT_CONFIRM`)는 이번에도 구현하지 않는다(`DEFERRED_NOT_CONTRACTED`, 변동 없음).
- 문의하기 화면(`SCR-APP-INQUIRY-001`)을 네이티브 폼(유형/제목/내용) + Mock 제출 API로 만든다. **단, `INQUIRY-001`은
  12-R 확정 13개 API에 속하지 않는 `DEFERRED_NOT_CONTRACTED` 항목이라 아래 Request/Response 모양은 문서 계약이
  아니라 이번에 새로 설계하는 잠정(Mock 전용) 구조다** — 실제 API 계약이 나중에 확정되면 다시 맞춘다.
- Phase 2에서 마이페이지 자리표시자에 임시로 붙여뒀던 로그아웃 버튼을 정식 위치(리스트 항목)로 옮긴다.

---

## 2. 만들 파일 목록

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/api/userApi.ts` | `USER-001` Mock — `getMe()` |
| 신규 | `src/api/rentalApi.ts` | `RENTAL-001` Mock — `getCurrentRental()` (null 가능) |
| 신규 | `src/api/settlementApi.ts` | `SETTLEMENT-001` Mock — `getPendingSettlements()` |
| 신규 | `src/api/inquiryApi.ts` | 문의 제출 Mock(잠정 구조) — `submitInquiry(request)` |
| 수정 | `src/types/rental.ts` | `CurrentRentalDetail`(RENTAL-001 응답 타입) 추가 |
| 신규 | `src/types/settlement.ts` | `SETTLEMENT-001` 응답 타입 |
| 신규 | `src/types/inquiry.ts` | 문의 제출 잠정 타입(`InquiryType`, `InquiryRequest` 등) |
| 수정 | `src/screens/SCR-APP-MYPAGE-001/MyPageScreen.tsx` | 전체 재작성 — 아래 §3 구성대로 |
| 신규 | `src/screens/SCR-APP-INQUIRY-001/InquiryScreen.tsx` | 문의 폼 화면(`CREATE_FORM`/`SUBMITTING`/`SUBMITTED`/`VALIDATION_ERROR`/`SERVER_ERROR`) |
| 신규 | `src/navigation/MyPageStackNavigator.tsx` | MyPage 탭 내부에 `MyPageHome → Inquiry` 스택 추가 |
| 수정 | `src/navigation/TabNavigator.tsx` | `MyPage` 탭이 `MyPageScreen` 대신 `MyPageStackNavigator` 렌더링 |
| 수정 | `src/components/common/TextField.tsx` | `multiline`/`numberOfLines` 옵션 추가(문의 "내용" 입력용) |

---

## 3. 각 요소의 역할과 결정 사항

### API 타입 (12-R 원문 그대로, §5.3/§5.5/§5.7)
```ts
// rental.ts에 추가
interface CurrentRentalDetail {
  rentalId: string
  status: RentalStatus
  checkoutSlotId: string
  checkoutSlotNumber: number
  checkoutStation: { stationId: string; name: string }
  rentedAt: string
  dueAt: string
  overdueDays: number
  calculatedOverdueAmount: number
  latestReturnAttempt: { returnAttemptId: string; status: string; returnSlotId: string; returnSlotNumber: number } | null
  failureCode: string | null
  failedAt: string | null
}

// settlement.ts
interface PendingSettlementItem {
  settlementId: string
  rentalId: string
  reason: 'OVERDUE' | 'LOSS' | 'DAMAGE' | 'ADJUSTMENT'
  amount: number
  paidAmount: number
  outstandingAmount: number
  status: 'PENDING' | 'PAID' | 'CANCELLED'
  description: string
  createdAt: string
}
interface PendingSettlementsResponse {
  items: PendingSettlementItem[]
  totalOutstandingAmount: number
  rentalEligibilityBlocked: boolean
}
```
금액·연체일수는 서버 값 그대로 표시하고 클라이언트가 재계산하지 않는다(§7.1 원칙).

### `MyPageScreen`
- `useQuery`로 `USER-001`/`RENTAL-001`/`SETTLEMENT-001` 3개를 각각 독립 조회(부분 실패가 서로를 막지 않게, Home과
  동일 패턴).
- 사용자 정보: `displayName`·`schoolName`
- 현재 대여 카드: `currentRental === null`이면 "대여 중인 우산이 없어요", 있으면 대여소명·기한·상태 Chip
  (§9.2 규칙: `overdueDays > 0`이면 "연체중" + `calculatedOverdueAmount` 표시, 아니면 "대여중")
- 미정산 카드: `items.length > 0`일 때만 노출, 각 항목 `reason`(한글 라벨)·`outstandingAmount` 표시, "결제하기"
  버튼은 있지만 `onPress`는 no-op(주석으로 Phase 7 연결 예정 명시) — 복귀 후 서버 재조회 원칙은 Phase 7에서 구현
- 리스트 항목: "문의하기"(→ `Inquiry` 화면으로 push), "로그아웃"(기존 임시 버튼을 여기로 정식 이동)
- 연장·신고 항목은 이번에도 없음(`DEFERRED_NOT_CONTRACTED`)

### `InquiryScreen`
- 입력 3개: 유형(`TextField`, 자유 입력 — 별도 enum 발명하지 않음), 제목(`TextField`), 내용(`TextField`
  `multiline`)
- 제출 전 클라이언트 검증(3개 모두 필수) → 비어있으면 `VALIDATION_ERROR`처럼 인라인 에러 표시(Zod 등 검증
  라이브러리는 후보 단계라 미도입, 수동 검증)
- 제출 중 버튼 비활성화(`SUBMITTING`), 성공 시 `SUBMITTED`(완료 문구 + 뒤로가기), 실패 시 `SERVER_ERROR`
  (`ErrorBanner` + 재시도)
- 질문·답변 등 개인 문의 내용을 로컬 상태 외 어디에도 영속 저장하지 않는다(Mock이므로 실제로도 저장 안 함)

### `MyPageStackNavigator` / `TabNavigator`
- `MyPage` 탭 내부만 `createNativeStackNavigator`로 감싸서 `MyPageHome ↔ Inquiry` 전환(뒤로가기 포함)
- 이 스택만 `headerShown: true`로 둬서 기본 상단 바(제목 "마이페이지"/"문의하기" + 뒤로가기)를 사용 —
  나머지 화면들은 자체 헤더(Screen 컴포넌트) 방식이라 이 부분만 다르다는 점을 인지하고 진행

### `TextField` 확장
`multiline`/`numberOfLines` optional prop 추가, `multiline`일 때 `textAlignVertical: 'top'` 적용. 기존
사용처(로그인 등)는 옵션을 안 넘기므로 동작 변화 없음.

---

## 4. 테스트 방법 (수동 확인 기준)

1. 마이페이지 탭 진입 → 사용자 정보·대여 상태·미정산 목록(있으면) 표시 확인
2. 각 api Mock의 시나리오 상수를 바꿔가며 "대여 없음" / "대여중" / "연체+미정산" 케이스 확인
3. "문의하기" 탭 → Inquiry 화면 이동, 뒤로가기 동작 확인
4. 빈 값으로 제출 → 인라인 에러, 값 채우고 제출 → `SUBMITTED` 표시 확인, Mock 에러 시나리오로 `SERVER_ERROR` 확인
5. "로그아웃" 클릭 → 로그인 화면으로 정상 복귀 확인(기존 동작 유지)
6. `npx tsc --noEmit` 통과 확인

---

## 5. 예상 리스크

1. **`INQUIRY-001` 잠정 구조** — 실제 API 계약이 나중에 정해지면 `types/inquiry.ts`·`inquiryApi.ts`를 다시 맞춰야
   한다(지금은 Mock 전용이라는 점을 코드 주석에도 남겨둔다).
2. **MyPage만 네이티브 스택 헤더 사용** — 다른 탭과 헤더 스타일이 달라 보일 수 있음. 통일감이 중요하면 이후에
   커스텀 헤더로 바꿀 수 있다.
3. **결제 버튼 no-op** — Phase 7 연결을 잊지 않도록 주석에 남겨둔다(Home 화면 챗봇 버튼과 동일 패턴).

---

## 6. 이번 Phase에서 하지 않을 일

- 실제 Toss 결제 흐름(`PAYMENT-001~003`) — Phase 7
- 대여 연장·분실/파손 신고 화면 — `DEFERRED_NOT_CONTRACTED`
- 문의 목록 조회(`CAND-APP-INQUIRY-LIST-001`) — `OUT_OF_SCOPE`
- 실제 Spring 서버 연동(계속 Mock)

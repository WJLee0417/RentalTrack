# PLAN — SCR-APP-CHATBOT-001 (빌리 챗봇)

> 대상 Phase: PHASE.md "Phase 5"
> 근거 문서: docs/PROJECT_GUIDE.md §5.13(CHATBOT-001 API)·§6(챗봇 상세 규칙) /
> docs/app-screen-flow.md §2.9(SCR-APP-CHATBOT-001)

---

## 1. 구현 목표

- 홈 화면의 챗봇 플로팅 버튼(현재 no-op)을 눌러 챗봇 화면으로 진입, 질문을 보내고 답변·CTA를 받는다.
- Variant 6종 구현: `INITIAL`, `ANSWERING`, `ANSWERED`, `FALLBACK_FAQ`, `TIMEOUT`, `ERROR`.
- `CHATBOT-001` Mock 연동(`src/api/chatbotApi.ts`).
- 허용 CTA 5종 중 `OPEN_MY_PAGE`·`OPEN_INQUIRY`만 실제 라우팅 연결(Phase 4 완료로 대상 화면 있음).
  `OPEN_HISTORY`(Phase 8)·`OPEN_MAP`(Phase 9)은 PHASE.md 명시대로 아직 대상 화면이 없으니 버튼만
  표시하고 라우팅은 no-op으로 자리만 둔다.
- 대화 이력은 화면 세션 메모리(컴포넌트 state)에만 유지, 영속 저장 안 함.

## 2. 만들 파일 목록

| 구분 | 경로 | 역할 |
|---|---|---|
| 신규 | `src/types/chatbot.ts` | `CHATBOT-001` 요청/응답 타입 + Intent/Action/Source 상수(§5.13 원문 값만 사용) |
| 신규 | `src/api/chatbotApi.ts` | `CHATBOT-001` Mock — `MOCK_SCENARIO`로 정상/폴백/타임아웃/에러 전환 |
| 수정 | `src/screens/SCR-APP-CHATBOT-001/ChatbotScreen.tsx` | 자리표시자 → 실제 채팅 UI로 전체 재작성 |
| 신규 | `src/navigation/RootStackNavigator.tsx` | 인증 후 최상위를 Stack으로 감싸 `Tabs ↔ Chatbot`(모달 프레젠테이션) 이동 제공 |
| 수정 | `src/navigation/RootNavigator.tsx` | `TabNavigator` 직접 렌더 → `RootStackNavigator` 렌더로 교체 |
| 수정 | `src/screens/SCR-APP-HOME-001/HomeScreen.tsx` | 챗봇 플로팅 버튼 `onPress`를 실제 네비게이션으로 연결(no-op 해제) |

## 3. 네비게이션 구조 변경 (중요 결정 사항)

현재 `RootNavigator`는 인증되면 `TabNavigator`를 바로 렌더링해서, 탭 바깥의 화면(챗봇)으로 갈 방법이
없다. `RootNavigator`와 `TabNavigator` 사이에 `RootStackNavigator`(native-stack)를 하나 끼워 넣는다.

```
RootNavigator (인증 분기)
  └─ RootStackNavigator
       ├─ "Tabs"     → TabNavigator (홈/지도/내역조회/마이페이지, 기존 그대로)
       └─ "Chatbot"  → ChatbotScreen (기본 push 전환, 네이티브 헤더 표시)
```

- 홈 화면의 챗봇 버튼은 `navigation.navigate('Chatbot')`로 진입(오른쪽에서 밀려 들어오는 기본 전환).
- 상단 헤더는 `Inquiry` 화면과 동일하게 네이티브 스택 기본 헤더 + `headerBackButtonDisplayMode: 'minimal'`
  (화살표만) 사용 — 뒤로가기를 누르면 스택 pop으로 홈(Tabs)으로 복귀(사용자 확인, 2026-08-05).
- 챗봇 화면 안에서 "마이페이지로 이동"(`OPEN_MY_PAGE`)은 모달을 닫고(`navigation.goBack()` 또는
  `popToTop` 후) 탭의 마이페이지로 이동, "문의하기"(`OPEN_INQUIRY`)는 마이페이지 탭의 Inquiry 화면까지
  중첩 이동 — `navigation.navigate('Tabs', { screen: 'MyPage', params: { screen: 'Inquiry' } })` 형태의
  중첩 파라미터로 처리.
- 이 구조 변경은 챗봇만을 위한 게 아니라 향후 "탭 바깥에서 접근하는 화면"이 생길 때도 재사용 가능한
  패턴이라, 새 파일(`RootStackNavigator.tsx`) 하나 추가 + `RootNavigator.tsx` 최소 수정으로 진행한다.

## 4. `chatbotApi.ts` / `types/chatbot.ts` 설계 (12-R §5.13 원문 그대로)

```ts
// types/chatbot.ts
export const CHATBOT_INTENT = {
  RENTAL_GUIDE: 'RENTAL_GUIDE',
  RETURN_GUIDE: 'RETURN_GUIDE',
  EXTENSION_GUIDE: 'EXTENSION_GUIDE',
  LOSS_OR_DAMAGE_GUIDE: 'LOSS_OR_DAMAGE_GUIDE',
  UNSETTLED_GUIDE: 'UNSETTLED_GUIDE',
  STATION_GUIDE: 'STATION_GUIDE',
  GENERAL_INQUIRY: 'GENERAL_INQUIRY',
} as const

export const CHATBOT_ACTION_TYPE = {
  OPEN_MAP: 'OPEN_MAP',
  OPEN_MY_PAGE: 'OPEN_MY_PAGE',
  OPEN_HISTORY: 'OPEN_HISTORY',
  OPEN_INQUIRY: 'OPEN_INQUIRY',
  NONE: 'NONE',
} as const

export const CHATBOT_SOURCE = {
  STATIC_FAQ: 'STATIC_FAQ',
  EXTERNAL_LLM: 'EXTERNAL_LLM',
  STATIC_FAQ_FALLBACK: 'STATIC_FAQ_FALLBACK',
} as const

interface ChatbotMessageRequest {
  requestId: string
  message: string
  screenContext?: string | null
  clientContext?: { hasActiveRental?: boolean; hasPendingSettlement?: boolean }
}

interface ChatbotMessageResponse {
  messageId: string
  answer: string
  intent: ChatbotIntent
  action: { type: ChatbotActionType; label: string }
  source: ChatbotSource
  fallback: boolean
}
```

- `chatbotApi.ts`는 `MOCK_SCENARIO: 'SUCCESS' | 'FALLBACK' | 'TIMEOUT' | 'ERROR'` 상수로 수동 전환
  (다른 Mock API들과 동일 패턴). 질문 키워드(우산/반납/연장/분실/파손/미정산/대여소)에 따라 그럴듯한
  intent·answer·action을 매핑하는 간단한 규칙 기반 매칭으로 구현(`STATIC_FAQ` 처럼 동작).
  `TIMEOUT`/`ERROR` 시나리오는 실제로 reject해서 화면이 `TIMEOUT`/`ERROR` variant로 전환되는지 확인할 수
  있게 한다.

## 5. `ChatbotScreen` 화면 구성

- 상태: `messages`(로컬 배열, `{role: 'user'|'assistant', text, action?, fallback?}`), `input`(텍스트),
  `status`('idle' | 'answering' | 'timeout' | 'error')
- `INITIAL`: 메시지가 없을 때 추천 질문 칩(우산 대여/반납/연장/분실·파손/미정산/대여소 6종, §6 원문 Intent와
  1:1 매핑) + 자유 입력창
- `ANSWERING`: 사용자 메시지 즉시 리스트에 추가 + "답변 작성 중" 로딩 표시, 전송 버튼/입력 비활성화
- `ANSWERED`: 답변 버블 + `action.type`이 `NONE`이 아니면 허용된 라벨로 CTA 버튼 렌더
- `FALLBACK_FAQ`: `fallback === true`일 때 답변 버블에 "정적 안내로 답변드려요" 같은 절제된 보조 문구만
  추가(기술적 노출 없이, §6 "과도한 기술 노출 없이 표시" 원칙)
- `TIMEOUT`/`ERROR`: 입력했던 메시지를 입력창에 그대로 보존, 답변 리스트에 안내 배너 + "다시 시도"·
  "문의하기"(`OPEN_INQUIRY`) 버튼
- 상단: 네이티브 스택 헤더(제목 + `Inquiry`와 동일한 minimal 뒤로가기 화살표) — 공용 `AppHeader`는
  로고+학교명 전용이라 여기선 안 씀
- 전송·입력 금지 항목(§6): 비밀번호·토큰·paymentKey·얼굴 데이터·카드/계좌 등은애초 이 화면에 해당 입력
  경로 자체가 없어 별도 필터링 로직 불필요(자유 텍스트 입력만 존재하므로 방어적으로 별도 마스킹 처리는
  하지 않음 — Mock 로그도 안 남기므로 실질 위험 없음)

## 6. 테스트 방법

1. 홈 화면 챗봇 버튼 → 모달로 챗봇 화면 진입 확인
2. `INITIAL`에서 추천 질문 탭 → `ANSWERING` → `ANSWERED` 흐름 확인
3. `chatbotApi.ts`의 `MOCK_SCENARIO`를 `FALLBACK`/`TIMEOUT`/`ERROR`로 바꿔가며 각 variant 확인
4. `ANSWERED`에서 분실/파손 질문 → `OPEN_MY_PAGE` CTA → 실제 마이페이지 이동 확인
5. 일반 문의 질문 → `OPEN_INQUIRY` CTA → 마이페이지 문의 화면까지 중첩 이동 확인
6. `TIMEOUT`/`ERROR` 상태에서 입력 보존 확인, "다시 시도" 재전송 확인
7. `npx tsc --noEmit` 통과 확인

## 7. 이번에 하지 않는 일

- 실제 외부 LLM 연동 — Mock 규칙 기반 매칭만
- `OPEN_HISTORY`/`OPEN_MAP` 실제 라우팅 — 대상 화면 Phase(8/9) 완료 후 연결
- 대화 이력 서버 영속 저장 — 세션 메모리만

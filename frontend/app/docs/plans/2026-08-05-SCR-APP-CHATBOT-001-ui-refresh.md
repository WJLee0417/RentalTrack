# PLAN — SCR-APP-CHATBOT-001 UI Refresh (목업 반영)

> 대상 Phase: PHASE.md "Phase 5" (이미 DONE — 이번은 기존 기능 수정, 신규 Phase 아님)
> 근거: 사용자가 공유한 챗봇 화면 목업 스크린샷(2026-08-05) + 기존 PLAN
> `docs/plans/2026-08-05-SCR-APP-CHATBOT-001.md`
> 범위: **화면 표현(UI)만 변경**. `CHATBOT-001` 계약(`src/types/chatbot.ts`,
> `src/api/chatbotApi.ts`)·CTA 라우팅 로직·오류 처리 로직은 변경하지 않는다.

---

## 1. 목업과 현재 구현의 차이

| 영역 | 현재 구현 | 목업 |
|---|---|---|
| 헤더 | 네이티브 기본 헤더, 제목 "빌리 챗봇" 한 줄 | 뒤로가기 + "빌리" + "AI 어시스턴트 · 온라인"(초록 점) 2줄 |
| 시간 표시 | 없음 | "오늘, 오전 10:42" 같은 날짜/시간 구분선 |
| INITIAL 안내 | "무엇을 도와드릴까요?" 텍스트만 | 인사 문구("안녕하세요, 빌리예요! ...") 말풍선이 먼저 나옴 |
| 추천 질문 | 알약(pill) 여러 개가 줄바꿈으로 나열 | 흰 카드 하나 안에 세로 리스트 |
| Assistant 말풍선 배경 | `bg-secondary`(크림색) | 흰색/연회색 계열로 보임 |
| CTA 버튼 | 라벨 텍스트만 | 라벨 + 화살표(→) |

## 2. 이번에 바꾸는 파일

| 구분 | 경로 | 내용 |
|---|---|---|
| 수정 | `src/screens/SCR-APP-CHATBOT-001/ChatbotScreen.tsx` | 레이아웃 재구성(아래 §3) |
| 수정 | `src/navigation/RootStackNavigator.tsx` | Chatbot 화면의 `headerTitle`을 커스텀 컴포넌트로 교체(뒤로가기는 기존 `headerBackButtonDisplayMode: 'minimal'` 그대로 유지, 헤더 자체를 없애지 않음) |

- `types/chatbot.ts`, `api/chatbotApi.ts`는 변경 없음(API 계약·Mock 시나리오 그대로).

## 3. `ChatbotScreen` 변경 상세

1. **헤더 타이틀 커스텀** — `RootStackNavigator`에서 `Chatbot` 스크린 옵션에
   `headerTitle: () => <ChatbotHeaderTitle />` 추가. "빌리" + "AI 어시스턴트 · 온라인"(초록 점,
   Tailwind 기본 `green-500` 사용 — 프로젝트 팔레트엔 온라인 상태색이 없어 기본 팔레트값을 그대로
   씀) 2줄 표시. 뒤로가기 화살표·pop 동작은 기존 그대로(코드 변경 없음).
2. **날짜/시간 구분선** — 화면 진입 시각을 `useState(() => new Date())`로 한 번만 고정,
   "오늘, 오전 10:42" 형식(한국어 오전/오후)으로 메시지 리스트 맨 위에 회색 pill로 1회만 표시.
   서버 응답이 아닌 클라이언트 표시용 로컬 값이며 영속 저장하지 않음(§6 원칙과 무관 — 저장 없음).
3. **INITIAL 인사 말풍선 추가** — 메시지가 없을 때 assistant 말풍선 스타일로 고정 문구
   "안녕하세요, 빌리예요! 우산 대여 서비스 이용 중 궁금하신 점을 무엇이든 도와드릴게요." 표시.
   이 문구는 `CHATBOT-001` 응답이 아니라 화면 로컬 상수 — 서버 호출·영속 저장 없음(기존
   "무엇을 도와드릴까요?" 문구를 대체).
4. **추천 질문 리스트 재구성** — 알약 그리드 → 흰 배경 카드(`rounded-2xl bg-white`) 안에 6개
   질문을 세로로, 각 행 사이 얇은 구분선(`border-b border-disabled`)으로 나열. 탭 동작은 기존과
   동일하게 `handleSend(question)`.
5. **Assistant 말풍선 배경** — `bg-secondary`(크림) → 흰 배경 + 얇은 테두리(`bg-white
   border border-disabled`)로 변경, 목업의 연한 회색 톤에 가깝게. User 말풍선은 기존
   `bg-primary`(노랑) 유지(목업과 일치).
6. **CTA 버튼에 화살표 추가** — 버튼 라벨 뒤에 `→`를 붙여 렌더(`action.label` 문자열 자체는
   변경하지 않고 화면에서만 이어붙임: `` `${message.action.label} →` `` 대신 Text 두 개로 분리
   렌더 — 로그·서버 전송 값에는 영향 없음).
7. 입력창·전송 버튼·`TIMEOUT`/`ERROR` 배너·로딩 상태 표시 로직은 기존 그대로 유지(스타일 미세
   조정만, 동작 변경 없음).

## 4. 확인 필요 사항 (승인 시 아래 기본값으로 진행)

- **온라인 상태 초록 점 색상**: 프로젝트 팔레트에 정의된 색이 없어 Tailwind 기본 `green-500`을
  그대로 쓸 예정 — 이견 없으면 이대로 진행.
- **인사 문구 정확한 텍스트**: 목업 문구를 그대로 사용할 예정("안녕하세요, 빌리예요! 우산 대여
  서비스 이용 중 궁금하신 점을 무엇이든 도와드릴게요.") — 문구 수정 원하면 알려달라.
- **시간 표시**: 실제 서버 타임스탬프가 아니라 화면 진입 시각(클라이언트 로컬 시각)을 그대로
  씀 — `CHATBOT-001` 응답에 타임스탬프 필드가 없으므로 이 방식이 유일한 선택지.

## 5. 하지 않는 일

- API 계약·타입·Mock 시나리오 변경
- CTA 라우팅 로직 변경(`OPEN_MY_PAGE`/`OPEN_INQUIRY`/`OPEN_HISTORY`/`OPEN_MAP` 동작은 기존 그대로)
- 메시지 영속 저장(여전히 세션 메모리만)
- Phase 6 이후(알림·결제·내역·지도) 범위 침범

## 6. 테스트 방법

1. 홈 → 챗봇 버튼 → 새 헤더(빌리 / AI 어시스턴트 · 온라인) 확인
2. `INITIAL`: 인사 말풍선 + 시간 구분선 + 세로 리스트형 추천 질문 카드 확인
3. 추천 질문 탭 → `ANSWERING` → `ANSWERED` 흐름 및 CTA 화살표 표시 확인
4. `chatbotApi.ts`의 `MOCK_SCENARIO`를 `FALLBACK`/`TIMEOUT`/`ERROR`로 바꿔가며 각 variant 스타일 확인
5. `npx tsc --noEmit` 통과 확인

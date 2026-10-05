# PROGRESS

## 현재 src 폴더 구조 및 역할 (2026-08-03 실명 확인 화면 + i18n 다국어 지원 반영)

> 이 절이 현재 코드의 기준이다. 임베디드 팀이 공유한 실제 wire 스펙(Envelope: `type/sessionId/requestId/correlationId/expiresAt/payload`, 지속 연결 1개로 7개 Operation 처리)에 맞춰 `piSocket.ts`를 전면 재작성한 상태 위에, (1) 실 Pi 없이 뒤 화면들을 테스트하기 위한 `piSocket.mock.ts` Mock 레이어, (2) 얼굴 인증 성공 후 실명 확인 단계, (3) 전체 화면 다국어(한국어/영어) 지원을 추가했다. 바로 아래 "이전 기록" 절들은 그 이전 단계(flat `{stage}` 메시지, Mock API 분리 구조) 보존용이다.

```
src/
├── App.tsx                          MAIN/AUTH/RENT/RETURN 화면 전환 최상위. 인증 성공 시 받은 sessionId를
│                                     들고 있다가 RentFlow/ReturnFlow에 그대로 전달, 차단 사유는 kioskMainStore로
├── api/
│   ├── piSocket.ts                  Pi WebSocket 지속 연결 1개 관리(전송 계층). openSession/startFaceAuth/
│   │                                startRent/startReturn/getSessionState/cancelSession/getStationSummary
│   │                                7개 함수. requestId 기준으로 ACK→EVENT×N→RESULT 라우팅. 항상 실제 Pi
│   │                                WebSocket(`VITE_PI_WS_URL`)로만 통신(Mock 분기 제거, 2026-08-06)
│   ├── kioskStageStream.ts          openSession(mode) → startFaceAuth(sessionId) 순서를 강제하고,
│   │                                결과를 AUTH 화면용 단순 stage(AUTH_STARTED/GUIDANCE/AUTH_SUCCEEDED/
│   │                                AUTH_FAILED/ELIGIBILITY_RESULT)로 변환. AUTH_SUCCEEDED에 displayName 실어보냄(TEMP)
│   └── stationApi.ts                메인 재고 조회 Mock(아직 KSK-STATION-001 미연동, 후속 작업 대상)
├── i18n/                            (신규) 다국어 지원 — 새 화면 추가 시 여기 키만 추가하면 됨
│   ├── translations.ts              `Translations` 인터페이스 + `ko`/`en` 딕셔너리(화면/도메인 단위 그룹화,
│   │                                슬롯 번호처럼 값이 들어가는 문구는 함수로 처리: `pickupMessage(slotNumber)` 등)
│   └── useTranslation.ts            languageStore의 현재 언어를 구독해 해당 딕셔너리를 반환하는 훅
├── store/
│   ├── faceAuthStore.ts             stage를 AUTH 화면 Variant로 전환, ELIGIBILITY_RESULT의 sessionId를
│   │                                onEligible 콜백으로 화면 밖(App.tsx)까지 전달. AUTH_SUCCESS 이후 자동 타이머로
│   │                                FACE_PROCESSING 전환하지 않고, AuthSuccessScreen의 "맞습니다" 클릭
│   │                                (confirmIdentity()) 또는 "아닙니다"(retry()) 로만 다음 단계로 넘어감
│   ├── languageStore.ts             (신규) `language: 'ko'|'en'` zustand 스토어. `setLanguage` 호출 시
│   │                                dayjs.locale()도 같이 바꿔 날짜/시간 표기(오전·오후 ↔ AM/PM)까지 전환
│   ├── kioskFlowStore.ts            MAIN/AUTH/RENT/RETURN 화면 전환 상태
│   └── kioskMainStore.ts            재고 상태와 대여·반납 차단 모달 사유
├── types/
│   ├── piProtocol.ts                실 wire 스펙 타입(Envelope, 7개 Operation Request/Event/Result).
│   │                                FaceAuthResult.displayName은 TEMP 필드(실 wire 스펙엔 없음, 3.2 이하 참고)
│   ├── kioskStage.ts                KioskMode, KIOSK_STAGE, ELIGIBILITY_RESULT payload(sessionId 포함),
│   │                                AUTH_SUCCEEDED 전용 displayName(TEMP, 3.2 이하 참고)
│   ├── eligibility.ts               UNSETTLED_BLOCKED / ACTIVE_RENTAL_EXISTS / ACTIVE_RENTAL_NOT_FOUND
│   ├── faceAuth.ts                  AUTH 화면 Variant
│   └── inspection.ts, slot.ts, screen.ts
├── screens/
│   ├── SCR-KSK-MAIN-001/            메인·재고·차단 모달
│   ├── SCR-KSK-AUTH-001/            얼굴 안내/촬영/처리/오류 화면. AuthSuccessScreen은 체크 아이콘 →
│   │                                "인증되었습니다" → 실명 확인("OOO님이 맞으신가요?" + 예/아니요 버튼) 3단계 연출
│   ├── SCR-KSK-RENT-001/            RentFlow(startRent EVENT로 슬롯 안내→완료 자동 전환) + 화면 2개
│   ├── SCR-KSK-RETURN-001/          ReturnFlow(startReturn EVENT 하나로 안내→촬영→검수→슬롯→완료 전체 전환)
│   │                                + UmbrellaGuideScreen/ReturnSlotScreen/ReturnCompleteScreen
│   └── common/                      CameraCaptureScreen, LoadingScreen, ErrorScreen
├── components/
│   ├── common/                      재사용 UI(ActionCard, GuideContent, SlotActionCard, RentalBlockModal,
│   │                                LanguageToggle 등). LanguageToggle은 languageStore를 직접 구독·갱신
│   ├── icons/                       재사용 인라인 SVG
│   └── layout/                      KioskLayout, StepIndicator(대여 3단계/반납 4단계 라벨 분리) — 헤더 텍스트·
│                                     "이전" 버튼·스텝 라벨 모두 useTranslation()으로 렌더링
└── assets/                          로고·AI 스캔 연출 SVG
```

### AuthSuccessScreen 실명 확인 흐름

```text
1. 얼굴 인증 성공(AUTH_SUCCEEDED stage) 수신
   → faceAuthStore: variant=AUTH_SUCCESS, displayName 세팅
2. AuthSuccessScreen 마운트, 로컬 phase state machine 진행
   ICON_ONLY(0~1s, 체크 아이콘만) → TEXT_SHOWN(1~2s, "인증되었습니다") → CONFIRM(2s~)
   - 아이콘은 처음부터 같은 자리를 지키고, CONFIRM 진입 시에만 위로 슬라이드
   - "인증되었습니다" 자리를 ICON_ONLY 단계부터 미리 예약해둬서(opacity만 전환) 아이콘이 순간 이동하며
     버벅이는 문제를 없앰
3. CONFIRM 단계: "{displayName}님이 맞으신가요?" + 버튼 2개
   - "예, 우산 대여/반납 계속하기" → confirmIdentity() → FACE_PROCESSING("환영합니다! {displayName}님")
     → 자격 결과(resolveEligibility) 반영
   - "아니요, 안면 인식 다시하기" → retry() → 얼굴 인증 처음부터 재시작(팀 결정, 2026-08-03)
```

- **TEMP/정책 참고**: 09-screen-flow.md §1은 얼굴 인증 중 이름 표시를 명시적으로 금지하지만, 팀 결정(2026-08-03)으로 이 실명 확인 단계를 추가하기로 했다. 명세서는 아직 갱신 전이라 코드에서 이 결정을 우선 반영했다 — `displayName`은 실 wire 스펙에 없는 TEMP 필드라 Mock 제거(2026-08-06) 이후에는 실 Pi가 값을 채워주지 않는 한 항상 `undefined`로 온다. 필드명이 무엇일지·실제로 내려올지는 여전히 미확정.

### i18n(다국어) 흐름

```text
1. 사용자가 LanguageToggle에서 "Eng" 클릭
   → languageStore.setLanguage('en') 호출
   → (a) zustand state.language = 'en'로 갱신, (b) dayjs.locale('en') 설정(날짜/시간 표기용)
2. 화면에 보이는 각 컴포넌트는 useTranslation() 훅으로 이미 languageStore를 구독 중
   → language 변경으로 자동 리렌더, translations['en'] 딕셔너리를 반환
3. 각 컴포넌트는 하드코딩 텍스트 대신 t.main.rentTitle처럼 딕셔너리 키를 참조 → 즉시 화면 전체 텍스트 전환
```

- 새 화면을 추가할 때는 `i18n/translations.ts`의 `Translations` 인터페이스와 `ko`/`en` 객체에 키만 추가하면 됨(라이브러리 없이 zustand + 딕셔너리 객체로 직접 구현 — react-i18next 등 신규 라이브러리 추가는 프로젝트 규칙상 승인 필요해서 보류)
- 적용 범위: 화면에 실제로 렌더링되는 모든 정적 한글 텍스트(스크린리더 전용 `alt` 속성 2곳은 제외). `LanguageToggle` 자체의 "한국어"/"Eng" 라벨은 고유명사라 언어 전환 대상에서 제외.

### 인증 → 자격 판정 → 대여/반납 흐름 (실 프로토콜)

```text
1. openSession(mode: RENT|RETURN)         → KSK-SESSION-001, sessionId 발급
2. startFaceAuth(sessionId)               → KSK-AUTH-001, {authenticated, eligibility} 수신
   - authenticated=false                  → FACE_NOT_MATCHED
   - eligibility.eligible=false           → blockingReasons에서 아는 값 찾아 RentalBlockModal
   - eligibility.eligible=true            → sessionId를 들고 RentFlow/ReturnFlow 진입
3. startRent(sessionId) / startReturn(sessionId) → KSK-RENT-001/KSK-RETURN-001,
   EVENT로 슬롯 번호·검수 결과 스트리밍, RESULT로 최종 완료 판정
```

- `eligibility.blockingReasons` 배열의 정확한 문자열 값은 임베디드 문서에 명시되지 않음 — 기존 `RENTAL_BLOCK_REASON` 값이 들어있다고 가정(사용자 확인, 2026-07-31), 모르는 값은 무시
- `rentedAt`/`dueAt`/반납 시각은 서버가 현재 항상 `null` 반환(Spring 연동 전, 문서 명시) — 화면은 null이면 "확인 중"으로 표시
- 재연결·복구(`KSK-SESSION-002`), 명시적 취소(`KSK-CANCEL-001`)는 함수는 만들어뒀지만 아직 어떤 화면에서도 호출하지 않음
- 이전 flat `{stage}` 프로토콜 기반이었던 `rentalApi.ts`/`returnApi.ts`/`inspectionApi.ts`/`ReturnCaptureScreen.tsx`는 실 프로토콜로 대체되어 삭제

---

## src 폴더 구조 (이전 기록: stage Mock/실제 Pi 전환 전 상태)

```
src/
├── App.tsx                          kioskFlowStore.currentScreen 기반 MainScreen ↔ AuthScreen ↔ (RENT) PickupSlotScreen/RentalCompleteScreen 전환. onAuthenticated: checkRentalEligibility() → 통과 시 startRental() 호출 후 RENT 화면으로, 차단 시 kioskMainStore.rentalBlockReason 세팅 + 메인 복귀. rental(RentalStart)·isPickedUp 로컬 state로 대여 진행 단계 관리
├── main.tsx                         React root 마운트 엔트리
├── index.css                        Tailwind import, 색상/폰트 등 공통 @theme 토큰
├── vite-env.d.ts                    Vite 환경변수(import.meta.env) 타입 선언
│
├── api/
│   ├── stationApi.ts                `fetchStationSummary()` — KSK-STATION-001 대응 Mock. SCENARIOS(AVAILABLE/EMPTY/STALE) 중 MOCK_SCENARIO 하나를 300~800ms 지연 후 반환
│   ├── faceAuthApi.ts               `authenticateFace()` — KSK-AUTH-001+KSK-SESSION-002 대응 Mock. MOCK_SCENARIO(MATCHED/NOT_DETECTED/NOT_MATCHED) 하나를 800~1500ms 지연 후 반환
│   ├── eligibilityApi.ts            `checkRentalEligibility()` — 얼굴 인증 후 "Spring 정책 검증" 대응 Mock. MOCK_SCENARIO(ELIGIBLE/UNSETTLED_BLOCKED/ACTIVE_RENTAL_EXISTS) 하나를 300~800ms 지연 후 반환
│   ├── rentalApi.ts                 `startRental()` — KSK-RENT-001 대응 Mock. 고정 `MOCK_SLOT_NUMBER`, `dueAt`은 `rentedAt` + 24시간(PROJECT_GUIDE.md §4 무료 대여 시간) 계산, 300~800ms 지연 후 반환. 슬롯 원자 선정은 서버 책임이라 클라이언트가 임의로 고르지 않음
│   └── piSocket.ts                  `requestFaceAuthStart()` — DEC-032 대응, Kiosk FE→Pi 직접 WebSocket 연결(얼굴 인증 트리거 1곳 한정 예외). `VITE_PI_WS_URL`로 연결 후 `START_FACE_AUTH` 전송, 첫 메시지 수신 시 resolve하는 가장 기본적인 연결 확인용 구현. Envelope 기반 정식 프로토콜(KSK-SESSION-001/KSK-AUTH-001 왕복)은 주석으로 보존만 해둔 상태
│
├── assets/                          icon/·logo/ 하위 폴더 없이 평탄화된 구조
│   ├── logo.svg                     KioskLayout 헤더에서 실제 사용 중인 로고
│   ├── logo-navy.svg                (미사용) 로고 변형 — 코드에서 import된 곳 없음
│   ├── eye-icon.svg                 (미사용) — 현재 안면 가이드 화면은 components/icons/EyeIcon.tsx(인라인 SVG)를 씀
│   ├── person-icon.svg              (미사용) — 현재 안면 가이드 화면은 components/icons/UserIcon.tsx(인라인 SVG)를 씀
│   ├── ai-scan-user.svg             LoadingScreen(FACE_PROCESSING 등)에서 사용 중인 얼굴 스캔 연출 아이콘
│   ├── ai-scan-fail.svg             ErrorScreen에서 사용 중인 실패 연출 아이콘
│   ├── ai-scan-umbrella.svg         (미사용) — 우산 스캔 연출, Phase 3(반납 AI 검수) 화면에서 쓸 후보로 보임
│   └── check-circle.svg             RentalCompleteScreen "대여 완료!" 체크 아이콘
│
├── components/
│   ├── common/                      화면 전반에서 재사용하는 범용 UI 요소(레이아웃 조각 단위)
│   │   ├── ActionCard.tsx           대여/반납 큰 카드 버튼 (아이콘 원+제목+부제+배경 큰 아이콘+화살표 버튼, primary/secondary variant)
│   │   ├── Badge.tsx                재고 없음 등 상태 배지. MainScreen 리디자인 이후 사용처 없음(파일은 유지)
│   │   ├── Button.tsx               primary/outline variant 공통 버튼. 고정 min-h-87.5(350px)·text-[43.2px]라 큰 단일 CTA 용도 — FaceAuthGuideScreen 등 목업 기반 화면은 이 크기가 안 맞아 화면 전용 버튼을 별도로 씀(재사용 시 주의)
│   │   ├── FaceGuideOverlay.tsx     카메라 프리뷰 위에 겹치는 점선 얼굴(타원) 가이드 오버레이
│   │   ├── GuideContent.tsx         "제목/부제 + 가이드 박스 + 아이콘 안내 목록 + 하단 액션 버튼" 패턴을 재사용 가능하게 뽑아낸 컴포넌트. FaceAuthGuideScreen이 사용 중, Phase 3 반납 가이드 화면에서도 재사용 예정
│   │   ├── IconBadge.tsx            원형 아이콘 배지 (error/success/neutral variant) — Error/Success/RentalBlockModal 등에서 공용
│   │   ├── InfoNoticeBar.tsx        하단 3열 정적 안내 바(아이콘+제목+부제 반복)
│   │   ├── RentalBlockModal.tsx     대여 불가 안내 모달(UNSETTLED_BLOCKED/ACTIVE_RENTAL_EXISTS) — MainScreen이 kioskMainStore.rentalBlockReason 있을 때 렌더링, 5초 후 자동 닫힘
│   │   ├── SlotActionCard.tsx       (신규, PLAN 문서 없이 커밋됨 — 하단 "이번에 안 한 일" 참고) 슬롯 번호 + 대여/반납 방향 우산 슬라이드 애니메이션(`UmbrellaSlotAnimationIcon`) + 액션 버튼. `type: 'rental' | 'return'`로 애니메이션 방향 결정, `PickupSlotScreen`이 사용 중
│   │   ├── Spinner.tsx              로딩 스피너 (role="status")
│   │   ├── StatusMessage.tsx        화면 중앙 안내 문구 공통 텍스트 컴포넌트
│   │   └── UmbrellaGuideOverlay.tsx 우산 촬영용 사각 점선 가이드 오버레이 (반납 촬영 화면용, Phase 3 성격, 아직 미사용)
│   │
│   ├── icons/                       인라인 SVG 아이콘 컴포넌트 (currentColor로 부모 텍스트 색 상속)
│   │   ├── UmbrellaIcon.tsx
│   │   ├── ReturnIcon.tsx
│   │   ├── CheckCircleIcon.tsx
│   │   ├── AlertCircleIcon.tsx
│   │   ├── ArrowRightIcon.tsx       ActionCard 화살표 버튼용
│   │   ├── ClockIcon.tsx            InfoNoticeBar 운영시간 항목용
│   │   ├── UserIcon.tsx             InfoNoticeBar "학생 인증" 항목 + FaceAuthGuideScreen "①" 안내 아이콘 겸용
│   │   ├── EyeIcon.tsx              FaceAuthGuideScreen "②" 안내 아이콘("정면을 응시해주세요")
│   │   ├── ChevronLeftIcon.tsx      '이전' 버튼 화살표 아이콘 (KioskLayout onBack 모드에서 사용)
│   │   └── UmbrellaSlotAnimationIcon.tsx  (신규, PLAN 문서 없이 커밋됨) 우산함 슬롯 SVG + CSS keyframe 애니메이션. `direction`(rental=하강/return=역방향 상승)·`speed`(초 단위, 기본값 3)·`slotColor` prop. `SlotActionCard`가 `speed={3}`으로 명시 호출 중
│   │
│   └── layout/
│       ├── KioskLayout.tsx          로고/시간·날짜 헤더 + 하단 라운드 콘텐츠 박스 공통 레이아웃. `onBack`(이전 버튼 전환)·`fullBleed`(패딩·정렬 제거)·`currentStep`(1|2|3, 있으면 헤더 중앙에 RentalStepIndicator 표시) 옵션. 콘텐츠 박스는 `overflow-hidden` 적용, `fullBleed`가 아니면 `justify-between gap-14 p-10`으로 세로 정렬(자식 위/아래로 자동 분배)
│       └── RentalStepIndicator.tsx  "1 대여 시작 · 2 얼굴 인식 · 3 대여 완료" 헤더 스텝 표시. `currentStep` prop 하나만 받음
│
├── screens/                         Screen ID(SCR-KSK-*) 기준 화면 컴포넌트 폴더
│   ├── SCR-KSK-MAIN-001/
│   │   └── MainScreen.tsx           대여/반납 선택 메인 화면. `useKioskMainStore` 구독, 마운트 시 재고 조회, 로딩 중엔 LoadingScreen 표시. 통계 바 + `ActionCard`×2 + `InfoNoticeBar` + (있으면) `RentalBlockModal`. 반납 카드는 재고와 무관하게 항상 활성(보류 항목, 아래 PHASE.md 참고)
│   ├── SCR-KSK-AUTH-001/            얼굴 인증 화면
│   │   ├── AuthScreen.tsx           `faceAuthStore.variant` 구독해 5개 Variant(GUIDE/FACE_CAPTURE/FACE_PROCESSING/FACE_NOT_DETECTED/FACE_NOT_MATCHED) 분기 렌더링. `GUIDE`→`startCapture()`(Pi WebSocket 연결 성공 후 FACE_CAPTURE 전환), `FACE_CAPTURE` 진입 1.5초 후 자동으로 `authenticateFace()` 호출 + 10초 무응답 시 홈 복귀 타임아웃 동시 운용
│   │   └── FaceAuthGuideScreen.tsx  안면 인식 시작 전 안내 화면(GUIDE). `KioskLayout` + `GuideContent`로 구성, face 전용 title/steps만 주입
│   ├── SCR-KSK-RENT-001/            (신규, PLAN 문서 없이 커밋됨 — 하단 "이번에 안 한 일" 참고) 대여 진행·완료 화면
│   │   ├── PickupSlotScreen.tsx     PICKUP_SLOT_GUIDE 대응. `SlotActionCard`(type="rental")로 슬롯 번호·인출 안내·"꺼냈어요" 버튼 렌더링
│   │   └── RentalCompleteScreen.tsx RENT_COMPLETED 대응. 대여 시각/반납 기한(dueAt) 카드 + 5초 카운트다운 후 자동 홈 복귀(`AUTO_CONFIRM_SECONDS`) + "홈으로" 즉시 확인 버튼
│   └── common/                      여러 Screen ID에서 재사용하는 화면 단위 컴포넌트
│       ├── LoadingScreen.tsx        KioskLayout + 연출용 카운트업 진행률(%, 실제 진행률 신호 없음·DEC-KSK-005 미정) + `ai-scan-user.svg`. 옵션 `currentStep` prop 전달
│       ├── ErrorScreen.tsx          KioskLayout + `ai-scan-fail.svg` + StatusMessage + 액션 버튼(기본/보조 2종). 옵션 `currentStep` prop 전달
│       ├── SuccessScreen.tsx        KioskLayout + IconBadge(success) + StatusMessage + 액션 버튼
│       ├── CameraCaptureScreen.tsx  카메라 스트림 <img src={streamUrl}> + guide 오버레이 슬롯. 옵션 `currentStep` prop 전달, `fullBleed`로 렌더링. `AuthScreen`이 실제 Orin 스트림 URL(`70.12.247.73:8080`)+`VITE_FACE_STREAM_TOKEN`으로 호출
│       ├── RecognitionGuideScreen.tsx  (미사용) 아이콘 + 안내문 1줄 + 시작 액션 — AUTH-001 GUIDE는 구성요소가 더 많아 대신 GuideContent+FaceAuthGuideScreen을 새로 만듦, 이 파일은 판단 보류 상태로 유지
│       └── SlotGuideScreen.tsx      (미사용) 슬롯 번호 원형 배지 + 안내문 + 액션 — SCR-KSK-RENT-001은 결국 이 파일 대신 `SlotActionCard`로 새로 구현됨, 판단 보류 상태로 유지
│
├── store/                           Zustand 전역 상태
│   ├── kioskFlowStore.ts           currentScreen(MAIN/AUTH/RENT) 기반 화면 전환 스토어. App.tsx가 실제로 구독·배선 중
│   ├── kioskMainStore.ts           MAIN-001 재고 상태(usableSlotCount/returnableSlotCount/isLoading/isStale/error) + fetchStationSummary 액션 + `rentalBlockReason`(대여 불가 사유) 상태·세팅/초기화 액션. selectMainScreenVariant 셀렉터 별도 export
│   └── faceAuthStore.ts            AUTH-001 화면 내부 상태(`variant`, 초기값 GUIDE) + startCapture(Pi WebSocket 연결)/authenticateFace/retry/reset 액션. MATCHED는 이 스토어의 variant를 안 바꾸고 호출부(AuthScreen→App.tsx)가 다음 동작을 결정
│
└── types/                           문자열 상수 기반 타입 정의
    ├── screen.ts                    SCREEN_ID 상수(`MAIN`/`AUTH`/`RENT`)·ScreenId 타입
    ├── slot.ts                      StationSummary 인터페이스(usableSlotCount/returnableSlotCount/observedAt/stale), MAIN_SCREEN_VARIANT 상수·타입(`DEFAULT`/`RENT_DISABLED_NO_STOCK` 2종뿐 — 반납 비활성 Variant 없음)
    ├── faceAuth.ts                  AUTH_SCREEN_VARIANT(GUIDE/FACE_CAPTURE/FACE_PROCESSING/FACE_NOT_DETECTED/FACE_NOT_MATCHED), FACE_AUTH_RESULT(MATCHED/NOT_DETECTED/NOT_MATCHED)
    ├── eligibility.ts               RENTAL_BLOCK_REASON(UNSETTLED_BLOCKED/ACTIVE_RENTAL_EXISTS), RENTAL_ELIGIBILITY_RESULT(위 2종 + ELIGIBLE)
    └── piProtocol.ts                `PiEnvelope<T>` 등 DEC-032 WebSocket 정식 프로토콜 타입 — `piSocket.ts`의 주석 처리된 Envelope 구현에서만 참조, 현재 활성 코드에서는 미사용
```

> `RentalBgImg.tsx`/`ReturnBgImg.tsx`(components/icons/)는 사용자가 직접 삭제함(2026-07-29) — ActionCard 배경 일러스트 대안으로 만들어졌으나 최종적으로 채택되지 않음.
> `SCR-KSK-RENT-001`(`PickupSlotScreen.tsx`/`RentalCompleteScreen.tsx`), `SlotActionCard.tsx`, `UmbrellaSlotAnimationIcon.tsx`는 PLAN 문서·완료 작업 표 없이 이미 커밋되어 있던 상태를 그대로 문서화한 것 — "Phase 범위 초과 커밋" 절과 같은 성격의 기록 누락. 코드는 정상 동작하며 되돌리지 않음.

---

## SCR-KSK-MAIN-001 (DEFAULT / RENT_DISABLED_NO_STOCK)

- 참고 PLAN: `docs/plans/2026-07-28-SCR-KSK-MAIN-001.md`(1~5단계), `docs/plans/2026-07-28-SCR-KSK-MAIN-001-mock-integration.md`(6~10단계)
- 상태: **완료** (1~10단계 완료, `develop` 미병합)

### 완료한 작업

| # | 파일 | 내용 |
|---|---|---|
| 1 | `src/index.css` (수정) | `@import 'tailwindcss'` 추가, PROJECT_GUIDE.md §14.1 실측 색상을 `@theme` 토큰(`--color-primary` 등 6개)으로 정의. Vite 템플릿 데모 스타일(다크모드 미디어쿼리, 1126px 고정폭 `#root` 등) 전부 제거 |
| 2 | `src/components/common/Button.tsx` (신규) | `disabled` prop으로 활성(`bg-primary`)/비활성(`bg-disabled`) 스타일 분기하는 공통 버튼 |
| 3 | `src/components/layout/KioskLayout.tsx` (신규) | 로고(`assets/logo.svg`) + 중앙 정렬 컨테이너 공통 레이아웃 |
| 4 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (신규) | `variant` prop(`DEFAULT` \| `RENT_DISABLED_NO_STOCK`)으로 분기하는 화면. 재고 수량은 아직 `MOCK_USABLE_SLOT_COUNT` 하드코딩 상수 |
| 5 | `src/App.tsx` (수정), `src/App.css` (삭제) | `MainScreen`을 렌더링하도록 엔트리 교체 |
| 6 | `src/types/slot.ts` (신규) | `StationSummary` 인터페이스(`usableSlotCount`/`observedAt`/`stale`, KSK-STATION-001 대응), `MAIN_SCREEN_VARIANT` 상수·`MainScreenVariant` 타입(기존 `MainScreen.tsx` 로컬 타입을 이동) |
| 7 | `src/api/stationApi.ts` (신규) | `fetchStationSummary()` Mock 함수. `SCENARIOS`(`AVAILABLE`/`EMPTY`/`STALE`) 중 `MOCK_SCENARIO` 하나를 300~800ms 임의 지연 후 반환 — 수동 테스트 시 이 배정만 바꿔서 분기 확인 |
| 8 | `src/store/kioskMainStore.ts` (신규) | `useKioskMainStore`(재고 상태 + `fetchStationSummary` 액션) + `selectMainScreenVariant` 셀렉터(variant는 상태로 저장하지 않고 `usableSlotCount`/`isStale`/`error`로부터 매번 파생 계산, `usableSlotCount===null`도 보수적으로 `RENT_DISABLED_NO_STOCK` 처리). 초기 `isLoading: true`로 설정해 마운트 직후 깜빡임 방지 |
| 9 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (수정) | `MOCK_USABLE_SLOT_COUNT`·`variant` prop 제거, `useKioskMainStore` 구독으로 교체, 마운트 시 `fetchStationSummary` 호출(`useEffect`), 로딩 중 `LoadingScreen` 재사용 |
| 10 | `src/App.tsx` (수정) | `MainScreen`에 넘기던 하드코딩 `variant="DEFAULT"` prop 제거. `onRent` → `CameraCaptureScreen` 배선은 유지(Phase 2 성격, 이번 범위 밖) |

### 검증

- 매 단계마다 `npm run build`, `npm run lint` 통과 확인
- 헤드리스 Chromium(Playwright, 프로젝트 의존성에는 미추가·확인용 1회성 도구)으로 두 Variant 스크린샷 확인(1~5단계 시점):
  - `DEFAULT`: 로고 + "현재 대여 가능한 우산 3개" + 대여/반납 버튼 모두 활성
  - `RENT_DISABLED_NO_STOCK`: "현재 대여 가능한 우산 0개" + 대여 버튼 비활성 + "우산 재고 없음" 배지 + 반납 버튼은 계속 활성
- 9~10단계 완료 후, 사용자가 `npm run dev`로 직접 브라우저에서 PLAN §6 시나리오를 수동 확인(`stationApi.ts`의 `MOCK_SCENARIO`를 `AVAILABLE`/`EMPTY`/`STALE`로 바꿔가며 로딩 스피너·재고 표시·버튼 활성/비활성·배지·보수적 stale 처리를 확인) — 정상 동작 확인됨
- 테스트 후 `MOCK_SCENARIO`가 `STALE`로 남아있던 것을 기본값 `AVAILABLE`로 되돌림

### 확정 아닌 결정 (추후 재확인 필요)

- `MainScreen`에 쓴 로고가 이후 `KioskLayout` 공통화 과정에서 `logo-umbrella.svg`로 정리됨 — `logo-black.svg`(모노톤) 등 대안과의 최종 브랜드 로고 확정 여부는 문서로 확인된 바 없음.
- API 통신 경계 문서 충돌(09번 문서의 Pi Local API 전제 vs PROJECT_GUIDE.md v2.0의 Spring Kiosk API 7개 전제), `KSK-STATION-001` 응답 스키마 미확인 등은 6~8단계(Mock/스토어 연동) 착수 시에도 그대로 유효.

---

## SCR-KSK-MAIN-001 리디자인 (티켓 71 후속, 브랜치 75에서 작업)

- 참고 PLAN: `docs/plans/2026-07-29-SCR-KSK-MAIN-001-redesign.md`
- 상태: **완료**, `develop` 미병합. 리스크(§8)는 사용자가 별도로 기록하기로 함(PLAN 승인 시 확인)
- 근거: 사용자 제공 목업 이미지(2026-07-29) — 통계 바(대여·반납 가능 수량), 카드형 대여/반납 버튼, 하단 3열 정적 안내 바

### 완료한 작업

| # | 파일 | 내용 |
|---|---|---|
| 1 | `src/types/slot.ts` (수정) | `StationSummary`에 `returnableSlotCount: number` 필드 추가(문서 근거 없음, 목업 반영용) |
| 2 | `src/api/stationApi.ts` (수정) | `SCENARIOS` 3종 모두에 `returnableSlotCount` 값 추가(목업 값 반영: `AVAILABLE`은 대여 3/반납 2) |
| 3 | `src/store/kioskMainStore.ts` (수정) | `returnableSlotCount` 상태 필드 추가, `fetchStationSummary`가 같이 채움. `selectMainScreenVariant`는 변경 없음(반납 버튼은 여전히 재고와 무관하게 항상 활성) |
| 4 | `src/components/icons/UserIcon.tsx`, `ClockIcon.tsx`, `ArrowRightIcon.tsx` (신규) | 기존 아이콘과 동일 패턴의 인라인 SVG |
| 5 | `src/components/common/ActionCard.tsx` (신규) | 대여/반납 카드 버튼. `icon` prop을 컴포넌트로 받아 아이콘 원(작게)과 배경 일러스트(크고 옅게) 두 군데에 다른 크기로 재사용. `primary`/`secondary` variant, `disabled`일 땐 `secondary` 톤으로 전환 |
| 6 | `src/components/common/InfoNoticeBar.tsx` (신규) | 하단 3열 정적 안내 바. `items` 배열(아이콘·제목·부제)을 받아 구분선과 함께 렌더링 |
| 7 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (전면 수정) | `KioskLayout fullBleed`로 변경 후 통계 바 + `ActionCard`×2 + `InfoNoticeBar` 구조로 재구성. 기존 `Button`/`Badge` 기반 레이아웃 제거(파일은 유지, 9절 참고) |

### 검증

- 매 단계마다 `npm run build`, `npm run lint` 통과 확인
- Playwright(1.62.0, 이번에도 프로젝트 의존성엔 미추가·확인용 1회성 설치 후 제거 예정)로 헤드리스 스크린샷 확인:
  - `AVAILABLE`: 통계 바 "대여 가능한 우산 3개"/"반납 가능한 우산함 2개", 대여 카드(노란 그라데이션)·반납 카드(흰색) 모두 목업과 시각적으로 일치
  - `EMPTY`(재고 0): 대여 카드가 반납 카드와 같은 무채색 톤으로 전환 + "0개" 표시, 반납 카드는 그대로 활성 — 의도대로 동작
- 로고(`logo-umbrella.svg`)가 목업의 "빌리안" 워드마크와 육안상 일치함을 확인(1절 리스크 해소)
- 테스트 중 `MOCK_SCENARIO`를 `EMPTY`로 바꿨다가 확인 후 `AVAILABLE`로 되돌림

---

## SCR-KSK-AUTH-001 (GUIDE / FACE_CAPTURE / FACE_PROCESSING / FACE_NOT_DETECTED / FACE_NOT_MATCHED)

- 참고 PLAN: `docs/plans/2026-07-28-SCR-KSK-AUTH-001.md`
- 상태: **완료** (Mock 흐름까지, `develop` 미병합). Phase 2(SCR-KSK-RENTAL) 첫 화면.

### 완료한 작업

| # | 파일 | 내용 |
|---|---|---|
| 1 | `src/types/faceAuth.ts` (신규) | `AUTH_SCREEN_VARIANT`(`GUIDE`/`FACE_CAPTURE`/`FACE_PROCESSING`/`FACE_NOT_DETECTED`/`FACE_NOT_MATCHED`), `FACE_AUTH_RESULT`(`MATCHED`/`NOT_DETECTED`/`NOT_MATCHED`) 상수 |
| 2 | `src/api/faceAuthApi.ts` (신규) | `authenticateFace()` Mock 함수. `MOCK_SCENARIO` 하나를 800~1500ms 지연 후 반환 |
| 3 | `src/store/faceAuthStore.ts` (신규) | `variant` 상태(초기값 `GUIDE`) + `startCapture`/`authenticateFace`/`retry`/`reset` 액션. `MATCHED`는 스토어 내부 variant를 바꾸지 않고 호출부에 결과만 반환(다음 화면 전환은 `AuthScreen`이 처리) |
| 4 | `src/components/layout/RentalStepIndicator.tsx` (신규) | "1 대여 시작 · 2 얼굴 인식 · 3 대여 완료" 헤더 스텝 표시. `currentStep` prop 하나만 받음 |
| 5 | `src/components/layout/KioskLayout.tsx` (수정) | 옵션 `currentStep?: 1\|2\|3` prop 추가(헤더 중앙에 절대 위치로 `RentalStepIndicator` 렌더링, 없으면 기존과 동일). 메인 콘텐츠 박스 세로 정렬을 `justify-center` → `justify-between`으로 변경(사용자 요청, Phase 1 화면 포함 전체 영향 — 6절 참고) |
| 6 | `src/screens/SCR-KSK-AUTH-001/FaceAuthGuideScreen.tsx` (신규) | 목업 기준 안내 화면: 제목/부제, 점선 가이드 박스(정적 placeholder), 번호 안내 2줄(장식용 단색 원 마커), 하단 풀너비 "준비되었습니다" 버튼. 공용 `Button` 컴포넌트는 고정 `min-h-87.5`(350px)·`text-[43.2px]`라 이 화면 톤(목업의 컴팩트한 바 형태)과 안 맞아 재사용하지 않고 화면 전용 버튼(h-20, text-2xl)을 새로 만듦 |
| 7 | `src/screens/SCR-KSK-AUTH-001/AuthScreen.tsx` (신규) | `faceAuthStore.variant`를 구독해 5개 Variant 분기 렌더링. `FACE_CAPTURE` 진입 1.5초 후 자동으로 `authenticateFace()` 호출(트리거 방식은 문서 근거 약함 — PLAN §8 리스크 유지) |
| 8 | `src/screens/common/LoadingScreen.tsx`, `ErrorScreen.tsx` (수정) | 옵션 `currentStep?: 1\|2\|3` prop 추가해 `KioskLayout`으로 그대로 전달만 함(자체 로직 없음) |
| 9 | `src/screens/common/CameraCaptureScreen.tsx` (수정) | 옵션 `currentStep?: 1\|2\|3` prop 추가(PLAN §3 파일 목록엔 없었지만 §4 AuthScreen 설명에 `FACE_CAPTURE: currentStep={2}`가 명시돼 있어 필요한 추가) |
| 10 | `src/types/screen.ts` (수정) | `SCREEN_ID.AUTH = 'SCR-KSK-AUTH-001'` 추가 |
| 11 | `src/App.tsx` (수정) | `useState(showCameraPreview)` 제거, `useKioskFlowStore.currentScreen` 기반으로 `MainScreen` ↔ `AuthScreen` 전환. `onRent` → `goTo(SCREEN_ID.AUTH)`. "이전"/`FACE_NOT_MATCHED`의 "홈으로"는 `faceAuthStore.reset()` 후 `goTo(SCREEN_ID.MAIN)` |

### 검증

- `npm run build`, `npm run lint` 통과 확인
- Playwright(1.62.0, 프로젝트 의존성엔 미추가 · 확인용 1회성 설치 후 제거)로 헤드리스 스크린샷 확인: 메인 "대여" 클릭 → `GUIDE`(스텝 "1" 활성, 목업과 레이아웃 일치) → "준비되었습니다" 클릭 → `FACE_CAPTURE`(스텝 "2" 활성, 실제 카메라 스트림 수신 확인, `FaceGuideOverlay` 정상 표시)
- 확인 중 공용 `Button` 컴포넌트가 이 화면에 안 맞는 고정 크기(350px 높이)를 강제한다는 걸 발견해 화면 전용 버튼으로 교체 — PLAN에 없던 수정이지만 목업 일치를 위해 필요했음
- 확인용으로 설치했던 `playwright` 패키지(`--no-save`)와 임시 스크립트 파일은 제거함(`package.json`/`package-lock.json` 변경 없음 확인)

### 이번에 안 한 일 (PLAN §9 그대로)

- `SCR-KSK-RENT-001`(대여 진행/슬롯 안내/완료), `SCR-KSK-ERROR-001`(`UNSETTLED_BLOCKED`/`ACTIVE_RENTAL_EXISTS`) — 다음 PLAN
- 인증 성공(`MATCHED`) 이후: `App.tsx`의 `onAuthenticated`는 빈 콜백(연결 지점만 마련)
- 재시도 횟수 제한·세션 만료 타이머(`DEC-SF-010`), 얼굴 정렬 실시간 안내(`DEC-KSK-004`), `AUTH_POLICY_REJECTED`/`ORIN_UNAVAILABLE` 처리
- 실제 Spring Kiosk API 연동(Mock만 사용)

### 확정 아닌 결정 / 리스크 (PLAN §8 유지 + 신규)

- `GUIDE` Variant는 09번 문서에 명시적 근거 없음(목업 근거로 추가, 로컬 UI 단계로 가정)
- `FACE_CAPTURE` 자동 시작 트리거(1.5초 지연)는 문서 근거 약함, 재확인 필요
- **`KioskLayout` 세로 정렬 `justify-between` 변경은 AUTH-001 전용이 아니라 전체 영향**: `LoadingScreen`/`ErrorScreen`처럼 자식이 2~3개뿐인 기존 Phase 1 화면도 이 변경의 영향을 받음(아이콘이 위로, 버튼이 아래로 밀리는 배치로 바뀜). 사용자가 명시적으로 전체 적용을 선택함 — 회귀 확인은 이번에 스크린샷으로 하지 않음, 필요 시 별도 확인 권장

---

## SCR-KSK-ERROR-001 (UNSETTLED_BLOCKED / ACTIVE_RENTAL_EXISTS, 대여 흐름)

- 참고 PLAN: 별도 파일 없이 대화 중 승인(2026-07-29). 09번 문서 §3.5의 두 Variant를 **풀스크린이 아니라 메인 화면 모달**로 구현 — 문서와 다른 선택(리스크로 기록)
- 상태: **완료**(Mock 흐름까지). `ACTIVE_RENTAL_NOT_FOUND`(반납 흐름 전용)는 이번 범위 아님

### 완료한 작업

| # | 파일 | 내용 |
|---|---|---|
| 1 | `src/types/eligibility.ts` (신규) | `RENTAL_BLOCK_REASON`(`UNSETTLED_BLOCKED`/`ACTIVE_RENTAL_EXISTS`), `RENTAL_ELIGIBILITY_RESULT`(위 2종 + `ELIGIBLE`) |
| 2 | `src/api/eligibilityApi.ts` (신규) | `checkRentalEligibility()` Mock. `MOCK_SCENARIO` 하나를 300~800ms 지연 후 반환 |
| 3 | `src/store/kioskMainStore.ts` (수정) | `rentalBlockReason` 상태 + `setRentalBlockReason`/`clearRentalBlockReason` 액션 추가(메인 화면이 이미 구독 중인 스토어에 자연스럽게 편입) |
| 4 | `src/components/common/RentalBlockModal.tsx` (신규) | reason별 안내 문구(제목/부제) + "닫기" 버튼 모달. `IconBadge`(error) 재사용 |
| 5 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (수정) | `rentalBlockReason` 구독해서 있으면 `RentalBlockModal` 렌더링 |
| 6 | `src/App.tsx` (수정) | `onAuthenticated`를 async로 바꿔 얼굴 인증 `MATCHED` 이후 `checkRentalEligibility()` 호출. `ELIGIBLE`이면 자리만 비워둠(다음 PLAN `SCR-KSK-RENT-001`), 차단 사유가 있으면 `rentalBlockReason` 세팅 + `faceAuthStore.reset()` + 메인 복귀 |

### 검증

- `npm run build`, `npm run lint` 통과 확인
- Playwright(1.62.0, 프로젝트 의존성엔 미추가 · 확인용 1회성 설치 후 제거)로 스크린샷 확인: 메인 "대여" → 가이드 → 카메라 → (테스트를 위해 `AuthScreen`의 자동 인증 트리거를 일시적으로 300ms로 낮추고 `eligibilityApi.ts`의 `MOCK_SCENARIO`를 `UNSETTLED_BLOCKED`/`ACTIVE_RENTAL_EXISTS`로 각각 바꿔가며) → 메인 화면 + 모달(문구 정상 분기) → "닫기" 클릭 시 모달만 사라지고 메인 화면 정상 유지
- 확인 후 `AuthScreen.tsx`의 자동 트리거는 사용자가 요청한 "잠깐 비활성화" 상태로 되돌림(주석 처리 유지), `eligibilityApi.ts`의 `MOCK_SCENARIO`는 `ELIGIBLE`로 복귀
- 확인용 `playwright` 패키지·임시 스크립트는 제거함(`package.json` 변경 없음)

### 이번에 안 한 일 / 리스크

- `SCR-KSK-RENT-001`(대여 진행/슬롯 안내/완료) — 다음 PLAN, `ELIGIBLE` 이후 자리만 비워둠
- 앱 결제 안내 CTA(09번 문서 `ACT-KSK-ERROR-003`, QR·딥링크 방식 `DECISION_REQUIRED`) — 모달에 "닫기"만 넣고 별도 액션 버튼은 안 만듦
- **모달 vs 풀스크린**: 09번 문서 화면 카탈로그 원문은 `SCR-KSK-ERROR-001`을 별도 화면으로 정의하지만, 이번 구현은 사용자 지시로 메인 화면 위 모달로 대체함 — 문서와 실제 구현이 다르다는 점 기록
- `AuthScreen.tsx`의 자동 인증 트리거(1.5초)는 여전히 주석 처리된 채로 남아있음 — 실제로 이 기능을 쓰려면 주석을 풀어야 함

---

## SCR-KSK-RETURN-001 (UMBRELLA_CAPTURE / INSPECTION_PROCESSING / RETURN_SLOT_GUIDE / RETURN_COMPLETED, Phase 3)

- 참고 PLAN: `docs/plans/2026-07-30-SCR-KSK-RETURN-001.md`
- 상태: **코드 구현 완료, 사용자 수동 클릭 테스트 대기 중**. `npm run build`·`npm run lint` 통과 확인. 로컬 환경에 Pi가 없어 얼굴 인증 화면("준비되었습니다")의 `requestFaceAuthStart()`가 항상 실패하므로, 전체 흐름 클릭 테스트는 이번엔 AI가 하지 않고 사용자가 직접 진행하기로 함(코드 리뷰 + build/lint만 확인)

### 완료한 작업

| # | 파일 | 내용 |
|---|---|---|
| 1 | `src/types/inspection.ts` (신규) | `INSPECTION_RESULT`(`NORMAL`/`DAMAGED`/`UNCERTAIN`/`FAILED`) |
| 2 | `src/types/eligibility.ts` (수정) | `RENTAL_BLOCK_REASON`에 `ACTIVE_RENTAL_NOT_FOUND` 추가(기존 "반납 흐름 전용이라 범위 밖" 주석 해제) |
| 3 | `src/types/screen.ts` (수정) | `SCREEN_ID.RETURN = 'SCR-KSK-RETURN-001'` 추가 |
| 4 | `src/api/returnEligibilityApi.ts` (신규) | `checkActiveRentalForReturn()` Mock. `'FOUND'` 또는 `ACTIVE_RENTAL_NOT_FOUND` 하나를 300~800ms 지연 후 반환 |
| 5 | `src/api/inspectionApi.ts` (신규) | `inspectUmbrella()` Mock. `InspectionResult` 하나를 1000~2000ms 지연 후 반환(다른 Mock보다 지연폭을 길게 잡음, 근거 없는 임의값) |
| 6 | `src/api/returnApi.ts` (신규) | `startReturn()` Mock. 고정 `MOCK_SLOT_NUMBER`(2) + 현재 시각(`returnedAt`)을 300~800ms 지연 후 반환 |
| 7 | `src/components/common/RentalBlockModal.tsx` (수정) | `MESSAGES`에 `ACTIVE_RENTAL_NOT_FOUND` 문구("반납할 대여 내역이 없어요") 추가 |
| 8 | `src/screens/common/LoadingScreen.tsx` (수정) | 하드코딩됐던 `ai-scan-user.svg` 대신 선택적 `icon` prop 추가(기본값 유지). 그동안 미사용이던 `ai-scan-umbrella.svg`를 `INSPECTION_PROCESSING`에서 사용 |
| 9 | `src/screens/SCR-KSK-RETURN-001/ReturnCaptureScreen.tsx` (신규) | `UMBRELLA_CAPTURE`. `CameraCaptureScreen`+`UmbrellaGuideOverlay` 조합, `AuthScreen`의 `AUTO_CAPTURE_DELAY_MS`(1.5초)와 동일 패턴으로 자체 타이머 후 `onCaptured` 콜백 호출 |
| 10 | `src/screens/SCR-KSK-RETURN-001/ReturnSlotScreen.tsx` (신규) | `RETURN_SLOT_GUIDE`. `SlotActionCard`(`type="return"`, 기존에 이미 지원하던 값이라 컴포넌트 수정 없이 재사용) |
| 11 | `src/screens/SCR-KSK-RETURN-001/ReturnCompleteScreen.tsx` (신규) | `RETURN_COMPLETED`. `inspectionResult`에 따라 문구 분기(`NORMAL`="반납이 완료되었습니다.", 나머지="반납은 완료되었으며 우산 상태는 관리자가 확인합니다." — PROJECT_GUIDE.md §6 그대로), 5초 후 자동 홈 복귀(`RentalCompleteScreen`과 동일 패턴) |
| 12 | `src/screens/SCR-KSK-MAIN-001/MainScreen.tsx` (수정) | 반납 `ActionCard`에 `onClick`이 아예 없었던 것을 발견 — `onReturn` prop 추가해 연결 |
| 13 | `src/App.tsx` (수정) | `mode`(`RENT`/`RETURN`)·`returnStep`(`CAPTURE`/`PROCESSING`/`SLOT_GUIDE`/`COMPLETED`)·`inspectionResult`·`returnResult` 로컬 상태 추가. `AuthScreen`은 코드 변경 없이 재사용하고 `onAuthenticated`만 모드별로 분기(`handleRentAuthenticated`/`handleReturnAuthenticated`) |

### 설계 결정 (PLAN에 명시된 내용)

- **얼굴 인증 화면(`AuthScreen`/`faceAuthStore`/`faceAuthApi`)은 코드 변경 없이 그대로 재사용** — 대여 전용 로직이 없어서 모드별 후속 처리만 `App.tsx`에서 분기
- **반납 진행 단계는 전용 Zustand 스토어를 새로 만들지 않고 `App.tsx` 로컬 상태로 관리** — RENT 흐름(`rental`/`isPickedUp`)과 동일한 패턴 유지, `faceAuthStore` 같은 전용 스토어는 과설계로 판단
- **`ACTIVE_RENTAL_NOT_FOUND`는 새 모달 없이 기존 `RentalBlockModal`/`rentalBlockReason` 재사용** — 이름이 더 이상 "Rental" 전용이 아니게 되는 점은 리스크로 남김(PLAN §7)

### 이번 구현 중 발견해 PLAN에 없던 판단 1건 (→ 아래 후속 작업에서 해소)

- **`KioskLayout`의 `currentStep`(헤더 스텝 인디케이터)을 반납 화면들에 전달하지 않기로 함**: `RentalStepIndicator.tsx`의 스텝 라벨("대여 시작"/"안면 인식"/"우산 받기")이 대여 흐름 전용 문구로 하드코딩돼 있어서, 반납 화면에 그대로 쓰면 문구가 안 맞는다. PLAN에는 이 컴포넌트 관련 내용이 없었음 — 이번엔 범위를 넘기지 않으려고 반납 화면들은 스텝 인디케이터 없이(= `currentStep` prop 생략) 렌더링했다. → 사용자가 후속 요청으로 반납 흐름에도 스텝 표시를 넣기로 결정, 아래 "반납 흐름 4단계 스텝 인디케이터 + 우산 파손 가이드 화면 추가" 절에서 해소함.

### 반납 흐름 4단계 스텝 인디케이터 + 우산 파손 가이드 화면 추가 (같은 날 후속 작업)

- 사용자 제공 목업(우산 파손 가이드 화면, 헤더에 "1 반납 시작 - 2 우산 파손 인식 - 3 반납 완료" 3단계 스텝 인디케이터) 기준으로 논의 후, 최종적으로 **"안면 인식"을 별도 스텝으로 포함한 4단계**(반납 시작 → 안면 인식 → 우산 파손 인식 → 반납 완료)로 확정
- PLAN 문서에는 없던 추가 범위 — 대화 중 승인받아 진행

| # | 파일 | 내용 |
|---|---|---|
| 14 | `components/layout/RentalStepIndicator.tsx` → **`StepIndicator.tsx`로 이름 변경** | 라벨 3개 하드코딩 제거, `flow: 'RENT' \| 'RETURN'` prop으로 `STEP_LABELS` 맵(RENT 3개/RETURN 4개)에서 선택하도록 일반화 |
| 15 | `components/layout/KioskLayout.tsx` (수정) | `flow?: StepFlow`(기본값 `'RENT'`) 추가, `currentStep` 타입 `1\|2\|3` → `1\|2\|3\|4`로 확장 |
| 16 | `screens/common/CameraCaptureScreen.tsx`, `LoadingScreen.tsx`, `ErrorScreen.tsx` (수정) | 동일하게 `flow` prop 추가해 `KioskLayout`으로 패스스루, `currentStep` 타입도 `1\|2\|3\|4`로 확장 |
| 17 | `screens/SCR-KSK-AUTH-001/FaceAuthGuideScreen.tsx` (수정) | `flow` prop 추가(기본값 `'RENT'`). `flow==='RETURN'`이면 제목을 "우산 반납을 위한 안면 인식을 시작합니다."로 분기(기존엔 "대여" 고정 문구였음 — 재사용 시 어색해서 같이 수정) |
| 18 | `screens/SCR-KSK-AUTH-001/AuthScreen.tsx` (수정) | `mode: StepFlow`(기본값 `'RENT'`) prop 추가, 내부에서 렌더링하는 5개 화면 모두에 `flow={mode}` 전달. `currentStep` 숫자 자체(GUIDE=1, 나머지=2)는 RENT/RETURN 공통이라 변경 없음 |
| 19 | `screens/SCR-KSK-RETURN-001/UmbrellaGuideScreen.tsx` (신규) | 사용자 제공 목업 그대로: "우산을 펼쳐 카메라에 보여주세요" + 점선 박스 + 3단계 안내(우산 펼치기/화면 안에 맞추기/2~3초 정지) + "준비되었습니다" 버튼. `GuideContent` 재사용, 아이콘은 기존 `UmbrellaIcon`/`EyeIcon`/`ClockIcon` 재사용(신규 아이콘 없음). `currentStep={3} flow="RETURN"` |
| 20 | `screens/SCR-KSK-RETURN-001/ReturnCaptureScreen.tsx`, `ReturnSlotScreen.tsx`, `ReturnCompleteScreen.tsx` (수정) | 각각 `currentStep={3}`, `currentStep={4}`, `currentStep={4}` + `flow="RETURN"` 추가 |
| 21 | `App.tsx` (수정) | `ReturnStep`에 `'GUIDE'` 추가(순서: `GUIDE`(신규 화면) → `CAPTURE` → `PROCESSING` → `SLOT_GUIDE` → `COMPLETED`), 초기값·`goToMain`·`handleReturnAuthenticated`의 리셋 값을 `'CAPTURE'` → `'GUIDE'`로 변경. `AuthScreen`에 `mode={mode}` 전달, `INSPECTION_PROCESSING`의 `LoadingScreen` 호출에 `currentStep={3} flow="RETURN"` 추가 |

**4단계 최종 매핑**: 1 반납 시작(얼굴 인증 GUIDE) → 2 안면 인식(촬영/처리/오류) → 3 우산 파손 인식(신규 가이드 화면 + 우산 촬영 + AI 검수 중) → 4 반납 완료(슬롯 안내 + 완료 화면). RENT 흐름은 `flow` 기본값이 `'RENT'`라 기존 3단계("대여 시작"/"안면 인식"/"우산 받기") 그대로 변경 없음.

### 검증(후속 작업)

- `npm run build`, `npm run lint` 통과 확인. 브라우저 클릭 테스트는 이전과 동일한 이유(로컬 Pi WebSocket 제약)로 사용자가 직접 진행 예정.

### 이번에 안 한 일 (PLAN §8 그대로)

- 09-screen-flow.md §3.4.1의 장치 실패류 세분화 Variant 7종(`UMBRELLA_NOT_DETECTED`, `WAITING_FOR_ITEM_INSERTION`, `LOCKING_RETURN_SLOT`, `PHYSICAL_RETURN_DONE`, `RECOVERY_REQUIRED`, `INSERTION_INCOMPLETE`, `LOCK_FAILED`)
- 실제 Spring Kiosk API(`KSK-RETURN-001`) 연동, 관리자 최종 판정·정산 생성 로직
- 재시도 횟수 제한·세션 만료 타이머, 얼굴 정렬 실시간 안내(`FACE_ALIGNMENT_REQUIRED`), 반납 가능 슬롯 0일 때 반납 버튼 비활성화(`RETURN_DISABLED_NO_CAPACITY`) — Phase 2에서도 보류된 항목 그대로 유지
- **클라이언트 측 Terminal 대기 타임아웃 미구현(TODO, 실 Pi 연동 시 처리 예정, 2026-08-03 논의)**: `ReturnFlow.tsx`의 `beginReturn()`은 `startReturn(...).then/catch`만 있고, 서버가 최종 Result를 끝까지 안 보내는 경우(연결은 유지된 채 응답만 없는 경우)에 대한 자체 타임아웃이 없다 — Promise가 영영 안 풀리면 `INSPECTION_PROCESSING`(`LoadingScreen`) 화면에 무기한 멈춘다. 09-screen-flow.md §3.4·PROJECT_GUIDE.md §5.4.1의 `KSK-RETURN-001` 표는 ACK 2초·AI 검수 Timeout 30초·전체 반납 Terminal 대기 한도 90초를 명시하고, PROJECT_GUIDE.md §8 원칙 5는 "타임아웃 후 임의 완료 처리 금지 → 재조회로 최종 상태 확인"을 요구한다. 지금은 Mock이라 항상 응답이 오므로 실증되지 않는 문제이며, **실제 Pi WebSocket 연동 시** 90초(또는 단계별 30초/90초) 안에 terminal Result가 없으면 `failed`로 전환해 기존 `ErrorScreen`으로 안내하는 가드를 추가해야 한다. `KSK-RENT-001`(`RentFlow.tsx`)도 동일 패턴이라 같은 이슈를 갖고 있을 가능성이 높음 — 실 연동 시 같이 확인.

### 검증

- 매 단계(타입→Mock API→공용 컴포넌트→반납 화면→배선) 후 `npm run build` 통과 확인, 배선 완료 후 `npm run lint` 통과 확인
- 실제 브라우저 클릭 테스트는 사용자가 직접 진행 예정(Pi WebSocket 로컬 제약으로 AI는 코드 검토까지만 수행)

---

## SCR-KSK-COMMON (Loading / Error / Success / Face Guide Overlay / 공통 컴포넌트)

- 참고 PLAN: `docs/plans/2026-07-28-SCR-KSK-COMMON.md`
- 상태: **완료** (develop 병합 완료, 이후 같은 브랜치 계열에서 추가 커밋 발생 — 아래 "Phase 범위 초과" 절 참고)

### 완료한 작업

| # | 파일 | 내용 |
|---|---|---|
| 1 | `src/screens/common/LoadingScreen.tsx` (신규) | `KioskLayout` + `Spinner` + `StatusMessage`로 구성된 범용 로딩 화면. 진행률(`progressMode`)은 `DEC-KSK-005` 미정이라 불확정 스피너만 사용 |
| 2 | `src/screens/common/ErrorScreen.tsx` (신규) | `IconBadge`(error) + `StatusMessage` + 액션 버튼(기본 "홈으로")으로 구성된 범용 오류 화면. 자동 복귀 타이머는 `DEC-SF-011` 미정이라 넣지 않음 |
| 3 | `src/screens/common/SuccessScreen.tsx` (신규) | `IconBadge`(success) + `StatusMessage` + 액션 버튼(기본 "확인")으로 구성된 범용 완료 화면. 동일하게 자동 타이머 없음 |
| 4 | `src/components/common/FaceGuideOverlay.tsx` (신규) | 카메라 프리뷰 위에 겹치는 점선 타원 가이드(순수 시각 오버레이, 카메라·얼굴 검출 로직 없음) |
| 5 | `src/components/icons/CheckCircleIcon.tsx`, `AlertCircleIcon.tsx` (신규) | Success/Error 화면용 인라인 SVG 아이콘 |
| 6 | `src/components/common/Badge.tsx` (신규) | PROJECT_GUIDE.md 코드 스타일에 명시된 공통 배지 컴포넌트. `MainScreen`의 "우산 재고 없음" 인라인 배지를 이걸로 교체 |
| 7 | `src/index.css`, `src/components/common/Button.tsx` (수정) | `@theme`에서 빠져있던 `--color-primary-text` 참조 버그 수정(`text-black`으로 정리) |
| 8 | `src/components/common/IconBadge.tsx`, `Spinner.tsx`, `StatusMessage.tsx` (신규, 리팩터) | Loading/Error/Success 화면 내부에서 반복되던 원형 배지·스피너·안내문 마크업을 공통 조각으로 분리 |
| 9 | `src/components/common/LoadingScreen.tsx` 등 3개 (이동) | `components/common/` → `screens/common/`으로 위치 이동 (완결된 "화면" 단위이므로 `screens/`가 맞다는 판단, `17592bb` 리팩터 커밋) |

### 검증

- 매 변경마다 `npm run build`, `npm run lint` 통과 확인
- `App.tsx`를 임시로 각 컴포넌트로 교체해 육안 확인(스피너 동작, 버튼 클릭 콜백 호출, 아이콘·배지 렌더링) 후 원상 복구

### Phase 1 "완료 후" 체크리스트 (PHASE.md 기준)

- [x] Loading/Error/Success/FaceGuideOverlay 구현
- [x] `Badge` 공통 컴포넌트 분리
- [x] `develop` 브랜치 병합 (`9a7d8f1` — `S15P11A201-71-fe-kiosk-common` → `develop`)
- [x] MainScreen Mock/스토어 연동(6~10단계) 완료
- [ ] `develop` 브랜치 병합 — 아직 `S15P11A201-75-fe-kiosk-rental` 브랜치에 있음
- [ ] PHASE.md `CURRENT PHASE`를 Phase 2로 변경 — 사용자·팀 확인 후 결정 필요 (아직 임의로 변경하지 않음)

---

## ⚠️ Phase 범위 초과 커밋 — SCR-KSK-RENT-001 계열 선행 작업 (문서화로 정리, 코드는 되돌리지 않음)

`develop` 병합(`9a7d8f1`) 이후, 현재 브랜치(`S15P11A201-75-fe-kiosk-rental`)에서 아래 2개 커밋이 이미 이뤄졌다. 내용상 Phase 2(SCR-KSK-RENTAL) 또는 Phase 3(SCR-KSK-RETURN) 범위에 해당하는데, `docs/PHASE.md`는 여전히 Phase 1을 `CURRENT PHASE`로 표시하고 있고("다음 Phase는 절대 구현하지 않는다"), 이 작업에 대응하는 `docs/plans/` PLAN 문서도 존재하지 않는다.

| 커밋 | 파일 | Phase 성격 | 문제 |
|---|---|---|---|
| `549dd35` | `RecognitionGuideScreen.tsx`, `SlotGuideScreen.tsx`, `ChevronLeftIcon.tsx`, `KioskLayout` onBack 옵션 | Phase 2 (얼굴 인증류 안내, 슬롯 안내) | PLAN 문서 없음 |
| `d3c962a` | `CameraCaptureScreen.tsx`, `IconBadge.tsx`(variant 확장), `App.tsx`의 카메라 스트림 배선 | Phase 2 (카메라 촬영) | PLAN 문서 없음, `App.tsx`에 실제 IP(`70.12.247.73:8080`)가 하드코딩됨 |

**2026-07-28 결정**: 코드는 되돌리지 않는다(git revert·브랜치 분리 안 함). 대신 `docs/PHASE.md` Phase 2 절에 이 파일들의 존재와 성격(PLAN 승인 없이 커밋된 재사용 가능한 UI 껍데기, 실제 대여 흐름 로직과 미연결)을 명시적으로 기록해 문서와 코드 상태를 일치시켰다. Phase 2 정식 착수 시 이 파일들을 그대로 재사용할지 새로 만들지는 그때 작성할 PLAN 문서에서 다시 판단한다.

---

## 변경 이력

- 2026-07-28: SCR-KSK-MAIN-001 1~5단계, SCR-KSK-COMMON 전체 완료 기록 최초 작성
- 2026-07-28: 실제 파일 상태(브랜치 `S15P11A201-75-fe-kiosk-rental`, 커밋 `549dd35`·`d3c962a` 포함) 기준으로 폴더 구조·완료 작업 표를 갱신. `develop` 병합 완료 반영. Phase 1 범위를 넘는 기존 커밋을 별도 절로 명시하고 리스크로 기록.
- 2026-07-28: SCR-KSK-MAIN-001 Mock 연동 6~8단계(`types/slot.ts`, `api/stationApi.ts`, `store/kioskMainStore.ts`) 완료 반영. 폴더 구조 트리·완료 작업 표 갱신, 남은 9~10단계(MainScreen/App.tsx 연동)로 항목 정리.
- 2026-07-28: 9~10단계(`MainScreen.tsx`/`App.tsx` 연동) 완료. 사용자가 브라우저에서 재고 있음/0/stale 시나리오 수동 확인 완료. SCR-KSK-MAIN-001 상태를 "완료"로 변경(`develop` 미병합). Phase 전환 여부는 아직 임의로 판단하지 않고 보류.
- 2026-07-28: "Phase 범위 초과 커밋" 이슈를 코드 원복 없이 문서화로 정리하기로 결정(팀 확인). `docs/PHASE.md` Phase 2 절에 선행 커밋(`549dd35`, `d3c962a`)의 존재와 성격을 명시하는 참고 문구 추가.
- 2026-07-29: 사용자 제공 목업 기준 SCR-KSK-MAIN-001 리디자인 완료(티켓 71 성격, 브랜치 `75`에서 작업하기로 결정). `ActionCard`/`InfoNoticeBar` 등 신규 컴포넌트 추가, `returnableSlotCount` Mock 필드 확장. Playwright 스크린샷으로 정상/재고 0 상태 시각 확인 완료. 리스크 항목은 PLAN 승인 시 사용자가 별도 기록하기로 함.
- 2026-07-30: **TEMP TEST-ONLY** — 로컬 환경에 Pi/Orin 실장비가 없어 SCR-KSK-RENT-001(우산 받기 화면) 흐름을 눈으로 확인하기 위해 임시 우회 적용. `store/faceAuthStore.ts`의 `startCapture()`에서 Pi WebSocket 연결(`requestFaceAuthStart`, DEC-032)을 주석 처리하고 바로 `FACE_CAPTURE`로 전환. `screens/common/CameraCaptureScreen.tsx`는 실제 Orin 스트림(`streamUrl` prop) 대신 `getUserMedia` 기반 로컬 웹캠으로 교체, `AuthScreen.tsx`의 `FACE_STREAM_TOKEN`·실제 스트림 URL도 주석 처리. 실제 Pi/Orin 연동 로직은 삭제하지 않고 주석으로 보존 — **실기기 연동 테스트 전에 반드시 원복 필요**.
- 2026-07-30: 위 TEMP TEST-ONLY 우회 3개 파일(`faceAuthStore.ts`/`CameraCaptureScreen.tsx`/`AuthScreen.tsx`) 전부 원복 완료 — `git diff` 확인 결과 마지막 커밋 상태와 동일. 실제 Pi WebSocket 연결(DEC-032)·Orin 스트림 URL 연동으로 복귀됨.
- 2026-07-30: 출처 미확인 신규 화면흐름 표와 `docs/09-screen-flow.md`·현재 코드를 비교해 발견한 2개 미구현 갭(`RETURN_DISABLED_NO_CAPACITY`, `FACE_ALIGNMENT_REQUIRED`)을 구현 여부 미정 상태로 후순위 보류하기로 결정(사용자 확인). `docs/PHASE.md` Phase 2 절에 근거와 함께 기록.
- 2026-07-30: `screens/common/LoadingScreen.tsx` UI 조정 — 제목/부제와 아이콘 사이 간격 `gap-14`→`gap-20`, 아이콘 크기 `h-56 w-56`(224px)→`h-72.75 w-72.75`(291px, 정확히 30% 증가). 이전에 시도했던 "콘텐츠 전체를 카드 내 수직 중앙으로" 변경은 사용자 요청으로 원복.
- 2026-07-30: `components/icons/UmbrellaSlotAnimationIcon.tsx`의 `speed` 기본값 5→3(초), `components/common/SlotActionCard.tsx`의 명시적 호출값도 4→3으로 통일.
- 2026-07-30: **Phase 2(SCR-KSK-RENTAL) 완료 처리**. 위 2개 보류 항목을 제외한 나머지(대여 시작~완료, 예외 처리)는 이번 대화 이전부터 이미 구현돼 있던 상태(`SCR-KSK-RENT-001` 관련 파일은 PLAN 문서 없이 커밋된 상태였음, 위 폴더 구조 섹션에 기록)를 확인만 하고 완료로 확정. `docs/PHASE.md`의 Phase 2 상태를 `DONE`, Phase 3(`SCR-KSK-RETURN`)를 `CURRENT PHASE`로 변경. Phase 3 착수 전 PLAN 문서 작성 필요(아직 미작성).
- 2026-07-30: `docs/plans/2026-07-30-SCR-KSK-RETURN-001.md` PLAN 작성·승인 후 Phase 3(SCR-KSK-RETURN-001) 구현 완료(코드 기준). 신규 7개·수정 6개 파일, 얼굴 인증 화면은 코드 변경 없이 재사용. 매 단계 `npm run build` 통과, 최종 `npm run lint` 통과 확인. 로컬 Pi WebSocket 제약으로 실제 브라우저 클릭 테스트는 사용자가 직접 진행하기로 함(AI는 build/lint+코드 리뷰까지). PLAN에 없던 판단 1건(`RentalStepIndicator`가 대여 전용 문구라 반납 화면엔 `currentStep` 생략) 발생, PROGRESS.md에 기록. `docs/PHASE.md` Phase 3 상태는 사용자 클릭 테스트 완료 전까지 `CURRENT PHASE`로 유지(아직 `DONE` 처리 안 함).
- 2026-07-30: 사용자 제공 목업 기반 후속 요청으로 반납 흐름에 4단계 스텝 인디케이터("반납 시작"-"안면 인식"-"우산 파손 인식"-"반납 완료") 추가 + 우산 파손 가이드 화면(`UmbrellaGuideScreen.tsx`) 신규 구현. `RentalStepIndicator`를 `StepIndicator`로 일반화(이름 변경), `KioskLayout`/`CameraCaptureScreen`/`LoadingScreen`/`ErrorScreen`/`FaceAuthGuideScreen`/`AuthScreen`에 `flow` prop 전달 배선. 이전에 PLAN 없이 보류했던 "반납 화면 스텝 인디케이터 미표시" 판단을 해소함. `npm run build`·`npm run lint` 통과 확인.
- 2026-07-30: 반납 흐름 마무리 조정 5건.
  1. `api/piSocket.ts`의 `startFaceAuthStream()`에 `mode: 'RENT' | 'RETURN'` 파라미터 추가, `START_FACE_AUTH` 트리거 메시지에 실어 보냄(서버가 실제로 이 필드를 쓰는지는 미확인 — 필드만 추가, 기존 응답 처리 로직은 변경 없음). `faceAuthStore.ts`의 `startCapture`/`retry`, `AuthScreen.tsx`의 두 호출부를 모두 이 시그니처에 맞춰 수정.
  2. `UmbrellaGuideScreen.tsx`에 5초 자동 진행 타이머 추가(`RentalCompleteScreen`/`ReturnCompleteScreen`과 동일한 카운트다운 패턴). 이를 위해 `GuideContent.tsx`에 선택적 `footer` prop을 추가해 카운트다운 배지를 버튼 위에 끼워 넣음.
  3. `ReturnCaptureScreen.tsx`의 임시 "다음으로" 수동 버튼을 제거. 반납·우산 인식 성공 stage 이름이 아직 미확인이라 `onCaptured`를 호출할 시점이 없으므로, 버튼도 없이 안내 메시지만 표시하는 상태로 되돌림(추측으로 다음 단계 진입 지점을 만들지 않기 위함) — 이 화면은 stage 이름이 팀 확인되기 전까지 다음 화면으로 자동 진행되지 않는다.
  4. `RentalCompleteScreen.tsx`/`ReturnCompleteScreen.tsx`의 카운트다운 문구를 동적 숫자 삽입(`{secondsLeft}초 후...`)에서 원형 배지 표시로 통일, 문구 자체는 고정 "5초 후..."로 변경(두 화면 모두 `AUTO_CONFIRM_SECONDS`가 5로 고정이라 실질적 동작 차이 없음, `UmbrellaGuideScreen`과 시각적 일관성 확보 목적).
  5. `npm run build` 통과 확인.
- 2026-07-30: 얼굴 인증의 기존 Pi WebSocket 연결과 `START_FACE_AUTH { mode }` 전송은 유지하면서, 수신 메시지를 공통 stage 로직으로 처리하도록 정리. `kioskStageStream.ts`/`kioskStage.ts`를 추가했고, 기본 source는 실제 Pi WebSocket, `VITE_KIOSK_STAGE_SOURCE=mock`일 때만 Mock stage를 사용한다. 이전 자격 조회 Mock API 2개와 미사용 UI/타입 파일 5개를 삭제. 현재 Mock 환경 변수는 대여/반납별 값이 분리되지 않아 후속 개선 필요.
- 2026-08-03: **Pi WebSocket 프로토콜 재작성 + Mock 레이어 추가**(별도 대화, 문서 갱신 없이 진행됐던 부분 포함): `piProtocol.ts`/`piSocket.ts`가 임베디드 문서(2026-07-31) 실 wire 스펙(Envelope: `type/sessionId/requestId/correlationId/expiresAt/payload`, 지속 연결 1개로 7개 Operation 순차 처리) 기준으로 전면 재작성됨. 로컬에 실 Pi가 없어 인증 이후 화면을 테스트할 수 없다는 문제를 해결하기 위해 `api/piSocket.mock.ts`(신규)를 추가하고, `.env.local`(git 미추적)에 `VITE_PI_MOCK=true`를 넣으면 `piSocket.ts`의 각 함수가 이 Mock으로 위임하도록 배선. 실제 Pi 연동 로직은 건드리지 않음.
- 2026-08-03: **AuthSuccessScreen 실명 확인 흐름 추가**(대화 중 승인, PLAN 문서 없음 — 09-screen-flow.md §1의 "얼굴 인증 중 이름 비표시" 규정과 충돌한다는 점을 먼저 알리고, 팀 결정(2026-08-03)이 명세서보다 우선한다는 확인을 받은 뒤 진행). 얼굴 인증 성공 직후 화면을 체크 아이콘(1초) → "인증되었습니다"(1초) → "{이름}님이 맞으신가요?" + "예/아니요" 버튼 2개의 3단계로 확장.
  - `types/kioskStage.ts`(`KioskStageMessage.displayName`), `types/piProtocol.ts`(`FaceAuthResult.displayName`) — 둘 다 TEMP 필드로 추가. 실 wire 스펙엔 없는 필드라 실 Pi 연동 시 값이 오지 않을 수 있음(주석으로 명시).
  - `api/piSocket.mock.ts` — `MOCK_DISPLAY_NAME` 상수로 Mock에서만 이름 채움.
  - `api/kioskStageStream.ts` — `AUTH_SUCCEEDED` stage emit 시 `displayName` 함께 전달.
  - `store/faceAuthStore.ts` — `displayName` 상태 추가(세션 종료 시 초기화). 기존에 `AUTH_SUCCESS_DISPLAY_MS` 타이머로 자동 전환하던 것을 제거하고, `confirmIdentity()`(신규 액션, "예" 클릭 시 FACE_PROCESSING 진입)로 대체. "아니요"는 기존 `retry()`를 재사용(얼굴 인증 처음부터 재시작).
  - `screens/SCR-KSK-AUTH-001/AuthSuccessScreen.tsx` — 로컬 `phase` state machine(`ICON_ONLY`→`TEXT_SHOWN`→`CONFIRM`), 아이콘 위치를 처음부터 고정해두고 CONFIRM 진입 시에만 위로 슬라이드(`transform: translateY`), 다음 텍스트 자리를 미리 예약해 레이아웃 점프로 인한 버벅임 제거. 버튼은 `ErrorScreen`과 동일한 공용 `Button` 컴포넌트로 통일.
  - `screens/SCR-KSK-AUTH-001/AuthScreen.tsx` — `displayName`/`confirmIdentity`를 스토어에서 읽어 `AuthSuccessScreen`/`FACE_PROCESSING` LoadingScreen(제목 "환영합니다! {이름}님")에 전달.
  - `index.css` — `rise-in` keyframe 추가(요소가 아래에서 살짝 떠오르며 나타나는 공통 연출).
  - `RentalCompleteScreen.tsx`/`ReturnCompleteScreen.tsx` — 라벨(`text-lg`→`text-xl`)·값(`text-base`→`text-lg`) 글자 크기 확대.
- 2026-08-03: **전체 화면 다국어(한국어/영어) 지원 추가**(대화 중 승인, PLAN 문서 없음). 처음엔 메인 화면만 요청받았으나 이후 "모든 화면 번역"으로 범위 확장됨. react-i18next 등 신규 라이브러리 대신, 이 프로젝트에 이미 있는 zustand 패턴을 그대로 따르는 직접 구현(라이브러리 추가는 프로젝트 규칙상 사전 승인 필요해서 보류)을 사용자가 선택.
  - `store/languageStore.ts`(신규) — `language: 'ko'|'en'` 상태 + `setLanguage`. 호출 시 `dayjs.locale()`도 같이 전환해 날짜/시간 표기(오전·오후 ↔ AM/PM)까지 언어에 맞게 바뀜. 기존 `KioskLayout.tsx`에 있던 `dayjs.locale('ko')` 초기화 코드는 이 스토어로 이동.
  - `i18n/translations.ts`(신규) — `Translations` 인터페이스 + `ko`/`en` 딕셔너리. 화면을 훑어 하드코딩된 한글 텍스트를 전수 조사(Explore 서브에이전트 활용)한 뒤 도메인별(common/header/stepIndicator/main/auth/rent/return/deviceErrorTips/rentalBlock)로 키를 구성.
  - `i18n/useTranslation.ts`(신규) — languageStore를 구독해 현재 언어 딕셔너리를 반환하는 훅.
  - 적용 파일 14개: `LanguageToggle`(로컬 `useState` 제거, languageStore로 교체), `KioskLayout`(헤더 기관명·"이전" 버튼), `StepIndicator`(대여/반납 스텝 라벨), `RentalBlockModal`(차단 사유별 문구), `MainScreen`, `AuthScreen`, `AuthSuccessScreen`, `ErrorScreen`(기본 액션 라벨), `FaceAuthGuideScreen`, `PickupSlotScreen`/`ReturnSlotScreen`, `RentFlow`, `RentalCompleteScreen`, `ReturnFlow`, `ReturnCompleteScreen`, `UmbrellaGuideScreen`.
  - `ErrorScreen.tsx`의 `tips` prop 타입을 `string[]` → `readonly string[]`로 완화(딕셔너리 배열을 그대로 전달하기 위함, 화면은 읽기만 해서 안전).
  - 적용 범위: 화면에 실제 렌더링되는 정적 한글 텍스트 전부. 스크린리더 전용 `alt` 속성 2곳(`CameraCaptureScreen`, `KioskLayout` 로고)은 제외. `LanguageToggle`의 "한국어"/"Eng" 라벨 자체는 고유명사라 번역 대상에서 제외.
- 2026-08-06: **`piSocket.mock.ts` Mock 레이어 삭제**(사용자 요청, 범위·PHASE.md 충돌 여부 확인 후 진행). PHASE.md Phase 3 규칙 "Mock API만 사용한다"와 충돌 여부를 먼저 알렸고, 사용자가 "실 Pi 서버가 지금 연결 가능한 상태"라고 확인해 진행. `.env`에 `VITE_PI_MOCK`이 애초에 없어(기본값 false) 런타임 동작 변화는 없음 — 이미 죽은 코드 경로 제거.
  - `api/piSocket.mock.ts` 삭제.
  - `api/piSocket.ts`에서 `import * as mock from './piSocket.mock'`, `USE_MOCK` 상수, 6개 함수(`openSession`/`startFaceAuth`/`startRent`/`startReturn`/`getSessionState`/`cancelSession`)의 `if (USE_MOCK) return mock.xxx(...)` 분기 전부 제거. 이제 항상 실 Pi WebSocket(`request()`)만 사용.
  - 범위 밖으로 명시 제외한 것: `stationApi.ts`(KSK-STATION-001, 100% Mock, 실제 호출 코드 자체가 없어 별도 작업 필요)와 `FaceAuthResult.displayName`/`studentId`(TEMP 필드, 별도 팀 결정 사안이라 이번 작업과 무관 — Mock 제거로 이제 항상 `undefined`로 옴, 위 §"AuthSuccessScreen 실명 확인 흐름" 참고).
  - `npm run build` 통과 확인.
- 2026-08-06: **실 Pi `KSK-AUTH-001.RESULT` 실측으로 실명 확인 필드명 버그 수정**(사용자가 실제 수신 payload를 붙여넣어 발견). 실측 payload에 `displayName`이 아니라 `userName` 필드로 이름이 온다는 것을 확인 — 기존 코드는 `result.displayName`을 읽고 있어서 항상 `undefined`였고, 실명 확인 화면("OOO님이 맞으신가요?")에 이름이 절대 채워지지 않는 버그였다. `studentId`는 필드명이 예상과 동일함을 실측으로 확인.
  - `types/piProtocol.ts` — `FaceAuthResult.displayName` → `userName`으로 필드명 수정, TEMP 주석을 "실 Pi 응답 실측(2026-08-06)으로 확인"으로 갱신.
  - `api/kioskStageStream.ts` — `onStage({ ..., displayName: result.displayName })` → `result.userName`으로 읽는 소스만 수정(스토어·화면 쪽 내부 프로퍼티명은 계속 `displayName` 유지, wire 필드명과 앱 내부명을 분리).
  - `npm run build` 통과 확인. 실기기 클릭 테스트는 사용자가 진행.
- 2026-08-06: 실명 확인 화면 학번 글자 크기 조정(사용자 요청). `i18n/translations.ts`의 `confirmQuestion`이 단일 문자열 대신 `{ before, studentId, after }` 조각을 반환하도록 변경(ko/en 둘 다) — `AuthSuccessScreen.tsx`에서 `studentId`만 `<span className="text-xl">`로 감싸 나머지 문구(`text-3xl font-bold`)와 다른 크기로 렌더링. `npm run build` 통과 확인.
  - `npm run build`·`npm run lint` 매 단계 통과 확인.
- 2026-08-03: 반납 진행 애니메이션(`LoadingScreen`의 %) 관련 논의 기록 — 하드코딩된 시간 동안 %를 캡까지만 올리는 연출 자체는 맞는 방향으로 확인(서버가 `progressMode=INDETERMINATE`만 주고 실제 진행률을 안 주기 때문에 다른 선택지가 없음, 화면 전환은 `LoadingScreen`의 타이머가 아니라 `ReturnFlow.tsx`의 실제 Event 수신으로 이뤄져 안전함 재확인). 다만 "서버가 끝까지 결과를 안 주면 어떻게 되나"라는 질문에서 클라이언트 타임아웃 가드 부재를 발견 — 코드 변경 없이 위 "이번에 안 한 일" 절에 TODO로만 기록, 실제 구현은 실 Pi 연동 시점으로 미룸(사용자 결정). `LoadingScreen`의 캡 값(99%)·목표 시간(1.2초)이 문서 스펙(0~90%, 2~3초)과 다르다는 점도 논의했으나 별도 결정 없이 보류.
- 2026-08-03: **검수 결과 반복(UNCERTAIN/DAMAGED) 처리 화면 2개 추가**(사용자 요청, PLAN 승인 후 구현). 참고 PLAN: `docs/plans/2026-08-03-SCR-KSK-RETURN-001-inspection-retry.md`. **문서(PROJECT_GUIDE.md §6·09-screen-flow.md §3.4 "AI 결과와 무관하게 물리 반납 계속 진행")와 다른 팀 신규 결정** — 검수 결과가 같은 값으로 연속 3회 나오면 흐름을 막는 새 동작을 추가함.
  - `ReturnFlow.tsx`(수정): `UiStep`에 `UNCERTAIN_BLOCKED`/`DAMAGED_CONFIRM` 추가. `lastStreakResult`/`streakCount`(연속 횟수, 재인식해도 초기화 안 함)/`countedThisAttempt`(한 시도당 1번만 카운트) 3개 `ref` 추가. `handleEvent`의 `RETURN_SLOT_ASSIGNED|WAITING_INSERTION|SPRING_COMMITTING` 케이스에서 `event.inspectionResult`가 UNCERTAIN/DAMAGED로 연속 3회째면 각각 `UNCERTAIN_BLOCKED`/`DAMAGED_CONFIRM`로 분기(그 외엔 기존과 동일하게 `SLOT_GUIDE`로 진행). `retryInspection()`(신규) — 안내 화면은 건너뛰고 `CAPTURE`로 바로 전환 후 `beginReturn()` 재호출(사용자 결정: 재인식은 촬영 화면부터만).
  - `UNCERTAIN_BLOCKED`: 기존 `ErrorScreen` 재사용, 버튼 1개("우산 재인식하기")만 표시 — 재인식 외 탈출 경로 없음(리스크로 PLAN에 기록).
  - `DAMAGED_CONFIRM`: 기존 `ErrorScreen` 재사용, title/subtitle은 사용자가 준 문구 그대로("관리자 최종 검증 후 파손 처리됩니다. 파손 처리 확정하시겠습니까?" / "진행 상황은 앱으로 알림이 전송됩니다."), 버튼 2개("예, 반납 진행 계속하기"→`SLOT_GUIDE`, "아니요, 우산 재인식하기"→재인식).
  - `i18n/translations.ts`(수정): `return` 섹션에 `uncertainBlockedTitle`/`uncertainBlockedTips`/`retryInspection`/`damagedConfirmTitle`/`damagedConfirmSubtitle`/`damagedConfirmYes` 6개 키 추가(ko/en). `uncertainBlockedTitle`은 사용자가 확정("우산이 정확히 인식되지 않았습니다"), `uncertainBlockedTips`(보조 설명 2줄)만 AI 초안 — 필요시 추후 수정.
  - **Mock 시나리오 미포함(사용자 결정)**: `piSocket.mock.ts`에 3회 반복 시나리오를 추가하지 않아, 이 두 화면은 현재 Mock으로 브라우저에서 자동 재현되지 않는다. `npm run build`·`npm run lint` 통과만 확인, 실제 화면 확인은 Mock 시나리오 추가 또는 실 Pi 연동 후 별도 진행 필요.
  - PLAN §8 리스크: 재인식이 실제 `KSK-RETURN-001` 프로토콜상 유효한 재시도 방식인지 미확인(문서에 검수만 재시도하는 규약 없음) — 실 Pi 연동 시 재확인 필요.
- 2026-08-03: **검수 결과 반복 처리 후속 조정 — 1~2번째도 안내 화면 추가 + 레이스 컨디션 버그 수정**(같은 대화, 사용자 확인 후). 위 항목 구현 직후 "1~2번째 UNCERTAIN/DAMAGED는 화면에 아예 안 보이고 조용히 넘어간다"(재인식 버튼이 3번째에만 있어 스트릭이 2 이상으로 쌓일 방법이 없는 구조적 문제)를 발견해 같이 고침.
  - `ReturnFlow.tsx`: `UiStep`에 `UNCERTAIN_NOTICE`/`DAMAGED_NOTICE` 추가(1~2번째용, 기존 `UNCERTAIN_BLOCKED`/`DAMAGED_CONFIRM`는 3번째부터로 유지). `STREAK_THRESHOLD=3` 상수화. NOTICE 화면은 "우산 다시인식하기"(주 버튼)+"이대로 진행하기"(보조 버튼) 2개 제공.
  - **레이스 컨디션 버그**: 게이트 화면(NOTICE/BLOCKED/CONFIRM)을 띄운 뒤에도 같은 시도의 뒤이은 이벤트(`WAITING_INSERTION`)와 `startReturn()`의 최종 resolve가 그 화면을 곧바로 `SLOT_GUIDE`/`COMPLETED`로 덮어써버리는 문제가 있었음(Mock의 `STEP_DELAY_MS`가 짧아 거의 즉시 발생). `awaitingDecision`/`pendingResult` ref 2개를 추가해 게이트 화면이 떠 있는 동안은 자동 진행을 막고, 사용자가 "이대로 진행하기"/"예"를 누르면 `finalizeIfReady()`로 미뤄뒀던 결과를 그때 반영하도록 수정.
  - `retryInspection` 버튼 라벨을 "우산 재인식하기"→"우산 다시인식하기"로 변경(사용자 요청).
  - `i18n/translations.ts`: `uncertainNoticeTitle`/`uncertainNoticeSubtitle`/`damagedNoticeTitle`/`damagedNoticeSubtitle`/`proceedAnyway` 5개 키 추가(ko/en, UNCERTAIN·DAMAGED 문구를 다르게 작성 — 사용자 확인).
  - 확인 방법(임시): `.env.local`(git 미추적)에 `VITE_PI_MOCK=true` 추가 + `piSocket.mock.ts`의 `MOCK_RETURN_SCENARIO`를 일시적으로 `'UNCERTAIN'`으로, `STEP_DELAY_MS`를 800(카메라 화면이 보이도록)으로 바꿔 로컬 `npm run dev`로 사용자가 직접 확인 — **확인 후 두 값 모두 원래대로(`'NORMAL'`, `30`) 되돌림**, 커밋에는 포함되지 않음.
  - `npm run build`·`npm run lint` 통과 확인.
- 2026-08-03: **반납 완료 화면(`ReturnCompleteScreen.tsx`)의 관리자 확인 안내 문구 강조**(사용자 요청). `t.return.resultNeedsReview` 문구를 "반납은 완료되었으며, 관리자 최종 검증 후 파손 여부가 확정됩니다\n자세한 내용은 앱에서 확인하세요!"(2줄)로 변경(ko/en), `NORMAL`이 아닌 결과(DAMAGED/UNCERTAIN/FAILED 공통)일 때만 `text-error-text`·`text-xl`·`font-semibold`·`whitespace-pre-line` 적용하고 `max-w-100`(400px)→`max-w-160`(640px)로 확장. `npm run build`·`npm run lint` 통과 확인.
- 2026-08-03: **얼굴 인식 가이드 문구가 언어 전환에 반영되지 않던 버그 수정**(사용자 발견·요청). `api/kioskStageStream.ts`가 화면 컴포넌트가 아니라 API/스트림 레이어인데도 `GUIDANCE_MESSAGES` 맵으로 한국어 문구("얼굴을 가이드 안으로 맞춰주세요" 등)를 직접 만들어 내려보내고 있었음 — 지난 2026-08-03 전체 다국어 작업 때 "screens/ 폴더 화면 컴포넌트"만 훑어서 이 API 레이어 파일이 조사 대상에서 빠졌던 것으로 보임.
  - `types/kioskStage.ts`: `KioskStageMessage`에 `guidanceCode?: string` 필드 추가(원본 코드만 전달, 문구로 미리 resolve하지 않음).
  - `api/kioskStageStream.ts`: `GUIDANCE_MESSAGES` 맵 제거, `GUIDANCE` stage에서 `event.guidanceCode`를 그대로 `guidanceCode`로 전달.
  - `store/faceAuthStore.ts`: 상태 필드명을 `guidanceMessage`(문구)에서 `guidanceCode`(원본 코드)로 변경.
  - `screens/SCR-KSK-AUTH-001/AuthScreen.tsx`: `useTranslation()`이 있는 이 화면 레이어에서 `guidanceCode`→문구 매핑(`ReturnFlow.tsx`가 `userMessageCode`를 처리하는 것과 동일 패턴)을 새로 담당.
  - `i18n/translations.ts`: `auth` 섹션에 `guidanceNone`/`guidanceCenterFace`/`guidanceDefault` 3개 키 추가(ko/en).
  - `npm run build`·`npm run lint` 통과 확인.
- 2026-08-07: **대여/반납 완료 RESULT의 `inventory`로 메인 화면 재고 즉시 동기화**(사용자가 대화 중 임베디드팀 스펙 문서를 붙여줌, PLAN 작성 후 승인받고 진행). 참고 PLAN: `docs/plans/2026-08-07-SCR-KSK-MAIN-001-inventory-sync.md`. **주의**: 이 필드는 userName/studentId 때와 달리 아직 실 Pi 응답 실측 전(임베디드팀이 "구현 완료"라고 설명한 스펙 문서 예시일 뿐) — PROVISIONAL로 취급.
  - `types/piProtocol.ts` — `PiInventorySnapshot`(`rentableSlotCount`/`emptyReturnSlotCount`/`totalSlotCount`) 타입 신규 추가. `RentResult`/`ReturnResult`에 `inventory: PiInventorySnapshot | null` 필드 추가(스프링 미연동 시 null로 온다고 문서에 명시돼 있어 nullable).
  - `store/kioskMainStore.ts` — `applyInventorySnapshot(inventory)` 액션 추가. `inventory`가 `null`이면 아무것도 바꾸지 않고(스프링 미연동을 재고 0으로 오인해 `RENT_DISABLED_NO_STOCK`을 잘못 띄우는 것 방지), 값이 있으면 `usableSlotCount`/`returnableSlotCount`를 덮고 `isStale=false`·`error=null`로 전환. `totalSlotCount`는 쓰는 화면이 없어 스토어에 저장하지 않음.
  - `screens/SCR-KSK-RENT-001/RentFlow.tsx`/`screens/SCR-KSK-RETURN-001/ReturnFlow.tsx` — 각각 `terminalStatus === 'SUCCEEDED'` 분기에서 `useKioskMainStore.getState().applyInventorySnapshot(result.inventory)` 호출 추가.
  - `npm run build` 통과 확인. 실 Pi 응답으로 `inventory` 수신 여부·필드명 실측은 사용자가 이어서 진행.

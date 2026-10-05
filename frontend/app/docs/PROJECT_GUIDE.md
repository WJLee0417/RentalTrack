# 빌리산(Billisan) App 프로젝트 지침서

> 기준: 노션 06번 "빌리산 프론트엔드 역할 명세서 v3.0", 09번 "Billisan Kiosk·App 와이어프레임 기반 화면 흐름 명세 v3.0", 12-R "Billisan REST API 계약 DB v3.0"
> 이 문서는 위 세 문서 중 **App(모바일 앱) 관련 부분만** 발췌·정리했다. Kiosk 전용 내용은 `frontend/kiosk/docs/`를 참고한다.
> 최종 갱신: 2026-07-31 (최초 작성)

---

## 0. 문서 정보

| 항목 | 내용 |
|---|---|
| 서비스명 | Billisan (빌리산) — 얼굴 온디바이스 인증 기반 공유 우산 대여 서비스 |
| 소속 | SSAFY 15기 A201팀, AIoT 공통 프로젝트 |
| 담당 | Frontend 1 (Hyejin) — 모바일 앱(React Native) + 키오스크 겸임 |
| 참고 노션 | 06번: `https://app.notion.com/p/5c5a5037c20c82c58172011ff3961651` · 09번: `https://app.notion.com/p/736a5037c20c824ba430014fd569c825` · 12-R DB: `https://app.notion.com/p/24ca5037c20c83c2840d81e3905413ff` |

**주의**: 06번·09번 문서 사이에 시점 차이로 인한 불일치가 일부 있다(예: WEATHER-001 활성 여부, App 화면 개수 8개 vs 9개). 발견된 불일치는 아래 각 절에 명시했다. 12-R은 실제 API Method/Path/DTO의 권위 문서이며 이번에 13개 App API 전량을 원문으로 확인했다.

---

## 1. 서비스 범위 (Kiosk와 공통)

- 실제 대여소 1개, 독립 슬롯 3~5개 기준
- 사용자는 Kiosk에서 얼굴 인증 후 슬롯 기반으로 우산을 대여·반납 (App은 이 과정에 직접 관여하지 않음)
- App은 **조회 중심**: 로그인 후 현재 대여·이용 내역·미정산·결제·알림·챗봇을 제공
- App은 얼굴 인증·슬롯 제어·장치 제어를 전혀 다루지 않는다 — Kiosk/Pi/Orin 전용 영역

---

## 2. Frontend 1 (Hyejin) App 담당 범위

### 2.1 담당 화면·API (문서1 §3.1·§3.2)

| 화면 | API | 핵심 책임 |
|---|---|---|
| 로그인·로그아웃 | `AUTH-001`, `AUTH-002`, `USER-001` | 대학 계정 `login_id + password` 로그인, 세션·만료·최소 계정 표시. 비밀번호 해시 처리·비교는 Spring 전용 |
| 홈·현재 대여·내역 | `HOME-001`, `RENTAL-001`, `RENTAL-002` | 서버 상태 표시, 금액·기한·자격을 클라이언트에서 재계산하지 않음 |
| 미정산·결제 | `SETTLEMENT-001`, `PAYMENT-001~003` | 중복 제출 차단, 결제창 복귀 후 서버 최종 상태 재조회 |
| 알림·날씨 | `ALERT-001`, `WEATHER-001` | 파생 알림·논리 예보 표시, P0 알림 읽음 서버 저장 없음 |
| 챗봇 | `CHATBOT-001` | 메시지·응답·Timeout·정적 FAQ Fallback·허용 CTA |

### 2.2 P0 제외 (문서1 §3.3)

> 원문: "App 지도·대여소 목록·상세, 알림 읽음 저장, 문의, 비밀번호 재설정, 대여 연장·분실/파손 사용자 신고는 `DEFERRED_NOT_CONTRACTED`다. P0 Method·Path·DTO·오류를 만들지 않는다."

즉 아래 기능은 화면(UI)만 있을 수 있고, **API를 만들지 않는다**:
- 대여소 지도(`SCR-APP-STATION-001`, P1)
- 알림 읽음 저장(서버 영속)
- 문의 접수
- 비밀번호 재설정
- 대여 연장
- 분실·파손 사용자 신고

### 2.3 App이 담당하지 않는 것 (Kiosk/Orin/Admin 전용)
- 얼굴 검출·임베딩·매칭
- 대여 자격 최종 판정, 슬롯 원자적 선정
- GPIO·센서·모터·LED·잠금·팬 제어
- 파손 AI 추론
- 관리자 파손 최종 판정
- 결제 승인 결과의 서버 검증(App은 재조회만 함)

---

## 3. 기술 스택

| 항목 | 값 | 확정 상태 |
|---|---|---|
| 프레임워크 | React Native | **확정**(문서1 §0.0, §2.4) |
| 실행 환경 | Expo (Expo Go) | 팀 자체 결정 — 문서1·09번 어디에도 "Expo" 언급 없음. 문서와 충돌하지 않으나 **문서 근거는 아님**, RN 사용 자체만 확정 |
| 언어 | TypeScript | 확정 |
| 전역 상태 | Zustand | **확정**(문서1 §8 변경이력 "Zustand 전역 상태 관리 확정") |
| 서버 상태 | TanStack Query 계열 | **후보** — "11 Working Day 안의 학습 비용·요청 취소·재시도·캐시 무효화·중복 제출 방지·타입 공유 가능성을 기준으로 팀이 선택" (문서1 §2.4). 임의로 확정하지 않는다 |
| 폼 | React Hook Form 계열 | **후보** |
| 스키마 검증 | Zod 계열 | **후보** |
| 반응형 기준 | "모바일 안전 영역(Safe Area)" 기준, 큰 버튼·큰 글자·단계별 한 행동 | 문서1 §2.6 |

> ⚠️ RN/Expo SDK 버전, 정확한 최소 지원 OS 버전 등은 두 문서 어디에도 없다 — 미확인. 팀이 별도로 확정해야 한다.

---

## 4. App 화면 카탈로그 (SCR-APP-*)

문서2 §19.2(최신 재집계) 기준 **9개 화면**(§5.3 초기 집계는 8개였으나 `SCR-APP-STATION-DETAIL-001` 추가로 갱신됨), Variant 37개.

| Screen ID | 이름 | 우선순위 | 판정 | 비고 |
|---|---|---|---|---|
| `SCR-APP-AUTH-001` | 로그인 | P0 | `CONFIRMED_P0 · DEC-031` | Variant: `DEFAULT`, `INVALID_CREDENTIALS`. 비밀번호 찾기는 P0에서 숨김 |
| `SCR-APP-HOME-001` | 홈 | P0 | `UI_ONLY_CHANGE` | 중앙 콘텐츠 영역 목적 확정(DEC-SF-014, "현재 상태·지도 조합") |
| `SCR-APP-STATION-001` | 대여소 지도 | **P1** | `NOT_ACTIVE_P0` | 지도 SDK 미정, 목록 fallback 필수 |
| `SCR-APP-STATION-DETAIL-001` | 대여소 상세 | 신규 | `NEW_SCREEN_ID · REVIEW_REQUIRED` | Variant: `DEFAULT/LOADING/ERROR/STALE` |
| `SCR-APP-MYPAGE-001` | 마이페이지 | P0(핵심 조회) | 혼합 | `ACTIVE_RENTAL`/`OVERDUE_UNSETTLED`는 P0, `EXTENSION_CONFIRM`/`INCIDENT_REPORT_CONFIRM`은 `DEFERRED_NOT_CONTRACTED` |
| `SCR-APP-HISTORY-001` | 이용 내역 | P0 | `KEEP_BASELINE` | Variant: `ALL/ACTIVE/COMPLETED/EMPTY/CARD_EXPANDED`. 페이지네이션 여부는 Phase 2에서 데이터 규모 보고 결정 |
| `SCR-APP-NOTIFICATION-001` | 알림 | 보류 | `DEFERRED_NOT_CONTRACTED`/`NOT_ACTIVE_P0` | 서버 영속 읽음 없음. `ALERT-001`은 로컬 읽음만 |
| `SCR-APP-INQUIRY-001` | 문의 접수 | 보류 | `NOT_ACTIVE_P0` | 외부 링크/이메일 이동으로 축소되면 API 자체가 불필요해질 수 있음 |
| `SCR-APP-CHATBOT-001` | 빌리 챗봇 | **P0 확정 신규 기능** | `IN_SCOPE · UPSTREAM_SYNC_REQUIRED` | Variant: `INITIAL/ANSWERING/ANSWERED/FALLBACK_FAQ/TIMEOUT/ERROR` |

### 4.1 로그인(`SCR-APP-AUTH-001`) 상세
- Variant `DEFAULT`: 대학 계정 식별자(`login_id`, 예: 학교 이메일) + 비밀번호 입력, 로그인 버튼. 비밀번호 찾기는 숨김(POST_P0)
- Variant `INVALID_CREDENTIALS`: "자격 증명 오류와 네트워크·서버 오류를 분리하되 계정 존재 여부는 노출하지 않는다"
- 성공 시: Spring이 `login_id`로 계정을 조회하고 `PasswordEncoder`로 평문·해시를 비교한 경우에만 토큰 발급 → 홈 이동, 비밀번호 상태 즉시 삭제

### 4.2 홈(`SCR-APP-HOME-001`) 상세
- 날씨 실패가 대여·정산 정보 전체 실패로 확장되지 않게 한다(부분 성공 원칙)
- 중앙 공백 영역: "현재 상태·지도 조합"으로 확정(GAP-004, §19.3)

### 4.3 마이페이지(`SCR-APP-MYPAGE-001`) 상세
- 결제 복귀 후 화면 문구만 성공으로 바꾸지 말고 Spring의 `SETTLEMENT`·`PAYMENT_ATTEMPT`를 재조회한다

---

## 5. App Public REST API 13개 (12-R 원문 확인, 2026-07-30)

공통 규칙: 모든 요청에 `X-Request-Id`(UUID), `Accept: application/json` 필수. 인증 필요 API는 `Authorization: Bearer <USER token>`. 응답은 `data` 래퍼 안에 담김. 쓰기 계열(`PAYMENT-001/002`)은 `Idempotency-Key` 헤더로 멱등성 보장.

### 5.1 `AUTH-001` 사용자 로그인
- `POST /api/v1/auth/login`
- Request Body: `identifier`(String, 대학 계정 형식), `password`(String, 평문 — App은 해시 생성 금지·처리 직후 메모리 삭제)
- Response: `accessToken`, `tokenType`(항상 `Bearer`), `expiresAt`, `user{userId, displayName, schoolName, faceRegistered}`
  - `user.userId`는 `USER_ACCOUNT.user_ref`(불투명 UUID)이며 9자리 학번(`user_id`)이 아니다
  - `user.faceRegistered`는 Spring에 투영된 얼굴 등록 여부일 뿐, 얼굴 원본/Vector 정보 아님
- 오류: `INVALID_CREDENTIALS`, `AUTHENTICATION_FAILED`
- 실제 대학 SSO·IdP 연동은 POST_P0. P0에는 `account_status` 없음

### 5.2 `AUTH-002` 사용자 로그아웃
- `POST /api/v1/auth/logout`
- Request Body 없음
- Response: `loggedOut: true`
- 동일 세션 반복 호출해도 결과 동일(멱등), 만료 세션 재요청도 `true`

### 5.3 `USER-001` 내 계정 조회
- `GET /api/v1/users/me`
- Response: `userId`(=`user_ref`), `displayName`, `schoolName`
- `studentNumber`, `loginId`, `role`, `accountStatus`, 얼굴 등록 여부, 비밀번호·해시는 응답하지 않는다
- 다른 사용자 ID를 Path/Query로 받지 않는다(본인 전용)

### 5.4 `HOME-001` 홈 요약 조회
- `GET /api/v1/app/home`
- Response:
  - `user{displayName, schoolName}`
  - `currentRental`(Object|null): `rentalId, status, checkoutStationName, rentedAt, dueAt, overdue(Boolean, 서버 계산), remainingFreeSeconds`
  - `pendingSettlement{count, totalAmount}`
  - `alertCount`
- 금액·연체 여부·기한은 App이 재계산하지 않는다. 날씨는 `WEATHER-001` 별도 호출

### 5.5 `RENTAL-001` 현재 대여 조회
- `GET /api/v1/rentals/current`
- Response: `rentalId, status, checkoutSlotId, checkoutSlotNumber, checkoutStation{stationId,name}, rentedAt, dueAt, overdueDays, calculatedOverdueAmount, latestReturnAttempt{returnAttemptId,status,returnSlotId,returnSlotNumber}|null, failureCode|null, failedAt|null`
- 활성 대여 없으면 HTTP 200 + `data:null`
- `status=FAILED`일 때만 `failureCode`/`failedAt`이 채워짐(그 외엔 둘 다 `null`) — DB의 `failure_message` 원문은 노출하지 않고 안정된 코드로만 투영

### 5.6 `RENTAL-002` 이용 내역 조회
- `GET /api/v1/rentals`
- Query: `status`(`ALL|ACTIVE|COMPLETED|LOST|FAILED`, 기본 `ALL`), `cursor`(불투명 문자열), `size`(기본 20, 최대 100)
- Response: `items[]{rentalId, status, checkoutStationName, returnStationName|null, rentedAt, endedAt|null, dueAt, settlementAmount, settlementStatus|null, failureCode|null, failedAt|null}`, `nextCursor|null`

### 5.7 `SETTLEMENT-001` 미정산 목록 조회
- `GET /api/v1/settlements/pending`
- Response: `items[]{settlementId, rentalId, reason(OVERDUE|LOSS|DAMAGE|ADJUSTMENT), amount, paidAmount, outstandingAmount, status(PENDING|PAID|CANCELLED), description, createdAt}`, `totalOutstandingAmount`, `rentalEligibilityBlocked`
- **대여당 정산은 최대 1행**, 사유 우선순위 `DAMAGE > LOSS > OVERDUE`, 총액 상한 7,000원(비합산). 상위 사유로 갱신해도 기존 `paidAmount` 보존

### 5.8 `PAYMENT-001` Toss 결제 시도 생성
- `POST /api/v1/settlements/{settlementId}/payment-attempts`
- Header: `Idempotency-Key`(UUID, `creationRequestId`와 동일)
- Request Body 없음(요청 금액 필드 없음 — 서버가 `outstandingAmount`로 확정)
- Response: `paymentAttemptId, settlementId, tossOrderId, amount, orderName, status(REQUESTED), successRedirectUri, failRedirectUri`
  - **딥링크 필드명 확정**: `successRedirectUri`/`failRedirectUri` (정확한 스킴 값 자체는 아직 문서에 없음 — Phase 진행 중 확인 필요)
- 동일 키·동일 정산 → 기존 응답 재사용. 동일 키·다른 정산 → `IDEMPOTENCY_CONFLICT`
- 오류: `SETTLEMENT_NOT_PAYABLE`, `PAYMENT_ALREADY_COMPLETED`, `PAYMENT_ATTEMPT_IN_PROGRESS`, `IDEMPOTENCY_CONFLICT`

### 5.9 `PAYMENT-002` Toss 결제 승인
- `POST /api/v1/payment-attempts/{paymentAttemptId}/confirm`
- Header: `Idempotency-Key`(Path의 `paymentAttemptId`와 동일)
- Request Body: `paymentKey`(Toss 승인 키 — 응답·일반 로그에 남기지 않음), `tossOrderId`, `amount`(서버 저장 금액과 일치해야 함)
- Response: `paymentAttemptId, status(SUCCEEDED|PROCESSING|FAILED), settlementStatus, paidAmount, outstandingAmount, approvedAt|null, rentalEligibility{eligible, blockingReasons[]}`
- 승인 응답 유실 시 새 승인 요청을 만들지 않고 `PAYMENT-003`으로 재조회

### 5.10 `PAYMENT-003` 결제 상태·공급자 재조정 조회
- `GET /api/v1/payment-attempts/{paymentAttemptId}`
- Response: `paymentAttemptId, status(REQUESTED|PROCESSING|SUCCEEDED|FAILED|CANCELLED), settlementStatus, paidAmount, outstandingAmount, reconciliationRequired(Boolean), errorCode|null, nextPollAfterMillis|null, updatedAt`
- 내부 `RECONCILIATION_REQUIRED` 상태는 외부엔 `status=PROCESSING` + `reconciliationRequired=true` + `errorCode=PAYMENT_RECONCILIATION_REQUIRED`로 투영(외부 status enum에 직접 추가 안 함)
- `outstandingAmount = max(SETTLEMENT.amount - SETTLEMENT.paid_amount, 0)`

### 5.11 `ALERT-001` 파생 알림 조회
- `GET /api/v1/app/alerts`
- Response: `items[]{alertKey("type:domainId"), type(RENTAL_DUE_SOON|UNSETTLED|RETURN_COMPLETED|ADMIN_REVIEW|RECOVERY_REQUIRED), title, message, occurredAt, read(P0에서는 App 로컬 상태), action{type(OPEN_MY_PAGE|OPEN_HISTORY|NONE), targetId|null}}`
- 서버 영속 읽음 처리(`ALERT-002`)는 `DEFERRED_NOT_CONTRACTED`

### 5.12 `WEATHER-001` 단기 강수 예보 조회
- `GET /api/v1/weather/short-term`
- Query: `latitude`, `longitude`(둘 다 필수)
- Response: `baseTime, items[]{forecastAt, precipitationProbability(0~100), condition(CLEAR|CLOUDY|RAIN|SNOW|UNKNOWN)}, provider`(공급자 미확정 시 `"UNDECIDED"`)
- ⚠️ 09번 문서(§14 DEC-SF-003)는 날씨 공급자·호출 경계를 여전히 미확정으로 분류(`NOT_ACTIVE_P0`) — API 계약 자체는 있으나 실제 공급자·구현 여부는 확인 필요

### 5.13 `CHATBOT-001` P0 메시지 처리
- `POST /api/v1/chatbot/messages`
- Request Body: `requestId`(UUID), `message`(String, 빈 문자열 금지), `screenContext`(Enum|null, 선택), `clientContext`(선택: `hasActiveRental`, `hasPendingSettlement` — 비민감 boolean 힌트, 권한 판단 근거로 사용 금지)
- Response: `messageId, answer, intent(RENTAL_GUIDE|RETURN_GUIDE|EXTENSION_GUIDE|LOSS_OR_DAMAGE_GUIDE|UNSETTLED_GUIDE|STATION_GUIDE|GENERAL_INQUIRY), action{type(OPEN_MAP|OPEN_MY_PAGE|OPEN_HISTORY|OPEN_INQUIRY|NONE), label}, source(STATIC_FAQ|EXTERNAL_LLM|STATIC_FAQ_FALLBACK), fallback(Boolean)`
- 오류: `CHATBOT_MESSAGE_REQUIRED`, `CHATBOT_MESSAGE_TOO_LONG`, `CHATBOT_UNSAFE_INPUT`, `CHATBOT_RATE_LIMITED`, `CHATBOT_PROVIDER_UNAVAILABLE`, `CHATBOT_PROVIDER_TIMEOUT`, `CHATBOT_RESPONSE_FAILED`
- 외부 LLM 장애·Timeout 후 정적 FAQ 매칭 시 HTTP 200 + `fallback=true`
- 결제·연장·신고·대여·반납 상태를 직접 변경하지 않는다(안내만)
- 질문·답변 원문 영속 저장 금지. 허용 로그는 `requestId,intent,latencyMs,providerStatus,fallback,errorCode,createdAt`뿐

---

## 6. 챗봇(`CHATBOT-001`) 화면 상세

- 통신 경계: App은 외부 LLM API를 직접 호출하지 않는다. `App → Spring Boot → FAQ/Intent 또는 외부 LLM` 경계를 따른다
- P0 지원 Intent(질문): 우산 대여 안내 / 반납 안내 / 대여 기간 연장 안내 / 분실·파손 신고 안내 / 미정산 확인·결제 안내 / 가까운 대여소 안내
- 허용 CTA: `OPEN_MAP | OPEN_MY_PAGE | OPEN_HISTORY | OPEN_INQUIRY | NONE`만 허용(다른 CTA 임의 추가 금지)
  - 분실/파손 → `OPEN_MY_PAGE`("마이페이지에서 신고하기")
  - 대여소 찾기 → `OPEN_MAP`("대여소 지도 열기")
  - 이력 확인 → `OPEN_HISTORY`("이용 내역 보기")
  - 추가 도움 → `OPEN_INQUIRY`("문의하기")
  - 분실·파손 질문은 반드시 신고 CTA만 사용, 연장 CTA와 혼용 금지
- 대화 이력은 화면 세션 메모리에서만 유지, 화면/앱 종료 시 폐기. `CHATBOT_CONVERSATION`/`CHATBOT_MESSAGE` 테이블 생성 안 함
- 전송·입력 금지 항목: 비밀번호, `password_hash`, Access Token, Toss `paymentKey`, 얼굴 이미지/임베딩/유사도, 대학 계정 식별자 원문, 카드번호, 계좌정보, 장치 Secret, 내부 SQL, Stack Trace

---

## 7. 결제(Toss Payments) 흐름

- Toss는 **테스트 환경**만 사용(정식 상용 PG 연동 아님)
- 핵심 원칙: "결제창 복귀 후 서버 최종 상태를 재조회한다" — 화면 문구만으로 성공 확정 금지
- 딥링크: `PAYMENT-001` 응답의 `successRedirectUri`/`failRedirectUri` 사용(스킴 값 자체는 Phase 진행 중 확인)
- 흐름: 마이페이지 진입 → 미정산 조회(`SETTLEMENT-001`) → 결제하기 → `PAYMENT-001`(시도 생성) → Toss 결제창 → App 복귀 → `PAYMENT-002`(승인) 또는 실패 시 `PAYMENT-003`(상태 재조회)로 확인

### 7.1 반납 기한·연체·미정산 표시 규칙 (09번 §17.1~17.2)

| 조건 | App 표시 | 금지 사항 |
|---|---|---|
| `now <= dueAt` | 무료 대여 중·잔여 시간 | 클라이언트가 정산 생성 금지 |
| `dueAt < now < dueAt+7일` | 연체 중·서버 산정 연체료 | 7,000원 초과 표시 금지 |
| 연체 7일 이상 | 분실 처리·7,000원 | 연체료와 분실금 합산 금지 |
| 관리자 파손 확정 | 파손 7,000원 | AI 결과만으로 비용 확정 금지 |
| 복수 사유 후보 | 단일 정산 행, 우선순위 `DAMAGE > LOSS > OVERDUE` | 정산 두 건·사유 금액 합산·기결제액 소실 금지 |

- `outstandingAmount = amount - paidAmount`, 결제하기는 미결제 잔액만 대상
- 시스템 시각 차이·클라이언트 계산만으로 비용·결제 완료를 확정하지 않는다

---

## 8. DECISION_REQUIRED / 미확정 항목 (09번 §14)

| ID | 항목 | 상태 | 영향 화면 |
|---|---|---|---|
| `DEC-SF-001`(=`DEC-031`) | App 인증 방식 | **RESOLVED**: P0는 대학 계정 `login_id+password`, Spring 서버 해시 비교 | AUTH |
| `DEC-SF-002` | 비밀번호 찾기 | P0 제거(`DEFERRED_NOT_CONTRACTED`) | AUTH |
| `DEC-SF-003` | 날씨 공급자·호출 경계 | **RESOLVED**(2026-08-06, 사용자 확인): 호출 경계는 App→Spring(`GET /api/v1/weather/short-term`, Bearer 인증, 다른 12개 API와 동일 패턴). 좌표는 실제 GPS가 아니라 캠퍼스 고정값(`37.501329, 127.039538`) 하드코딩(`src/api/weatherApi.ts`). 실제 외부 기상 공급자 자체는 Spring 내부 사항이라 App은 관여하지 않으며, 응답의 `provider` 값은 서버가 주는 그대로 표시(여전히 `"UNDECIDED"`일 수 있음) | HOME |
| `DEC-SF-004` | 지도 SDK·위치 권한 | **RESOLVED**(2026-08-09, 사용자 확인 — 2026-08-05 결정 번복): SVG 기반 가상 캠퍼스 지도 대신 **카카오맵 JS SDK 커스텀 타일셋**으로 전환. `react-native-webview`(Expo Go 호환 확인) 안에서 `source={{ html }}`로 생성한 HTML을 직접 주입 — 외부 호스팅(Netlify/EC2 등) 불필요(도메인 검증이 이 방식에서 통과함을 실기기로 확인). 커스텀 타일(사용자 제작, 512×512 PNG 14장 — 0단계 3×3/1단계 2×2/2단계 1×1)을 base64로 임베드해 `kakao.maps.Tileset`에 등록, 위경도가 아닌 자체 픽셀 좌표계 사용. 위치 권한은 여전히 불필요(가상 좌표계, 실제 GPS 무관) | STATION |
| `DEC-SF-005` | 대여 기한·연체 기준 | **RESOLVED**(2026-08-05, 사용자 확인): 대여 기한은 1일(24시간) 고정 — `dueAt = rentedAt + 24h` | MYPAGE·NOTIFICATION·HISTORY |
| `DEC-SF-006` | 연장 기능 | MVP 제외 확정(`DEFERRED_NOT_CONTRACTED`) | MYPAGE·CHATBOT |
| `DEC-SF-007` | 분실·파손 신고 | 문의 링크·관리자 처리로 축소 권장(`DEFERRED_NOT_CONTRACTED`) | MYPAGE |
| `DEC-SF-008` | 알림 영속성 | P0 도메인 파생·읽음 서버 저장 제거 | HOME·NOTIFICATION |
| `DEC-SF-009` | 문의 저장 기능 | 외부 링크 권장(`NOT_ACTIVE_P0`) | INQUIRY |
| `DEC-SF-014` | 홈 중앙 콘텐츠 | 확정: 현재 상태·지도 조합 | HOME |

**이 프로젝트 규칙**: 위 표에서 "미확정"인 항목은 임의로 확정하거나 추측으로 구현하지 않는다. 반드시 "미확정"으로 표시하고 진행한다(Kiosk CLAUDE.md와 동일 원칙).

---

## 9. App 전용 공통 규칙

### 9.1 Interaction 8종 (09번 §19.5)

| ID | 규칙 |
|---|---|
| `INT-APP-001` | Page Transition · 280ms · ease-in |
| `INT-APP-002` | Button Press · scale 1→0.97→1 |
| `INT-APP-003` | Card Elevation · 150ms |
| `INT-APP-004` | Pull-to-Refresh · Spinner 후 기존 GET 재호출(새 API 만들지 않음) |
| `INT-APP-005` | Bottom Sheet · open 320ms / close 240ms |
| `INT-APP-006` | Loading Transition · 240ms |
| `INT-APP-007` | Success · 400ms, 서버 성공 후에만 실행 |
| `INT-APP-008` | Error · Shake·Danger Flash · 300ms |

- Reduce Motion 활성화 시 과도한 이동·Bounce 최소화
- 화면 전환·전송 중 중복 Tap 차단
- Animation·Skeleton·Bottom Sheet 연출만을 위한 API는 만들지 않는다

### 9.2 상태 Chip 표시 규칙 (09번 §19.4)

| 표시 | 판정 기준 | 주의 |
|---|---|---|
| 대여중 | `RentalStatus ACTIVE\|RETURNING` | 표시용 View Model |
| 반납완료 | `RentalStatus COMPLETED` | 서비스 반영 완료만 사용 |
| 연체중 | 활성 대여 + 서버 시각이 `dueAt` 초과 | 새 RentalStatus Enum 생성 금지 |
| 미정산 | `SettlementStatus PENDING` | 서버 산정 금액 사용 |
| 정산완료 | `SettlementStatus PAID` | 결제 결과 재조회 |
| 분실·파손 | `RentalStatus LOST` 또는 관리자 확정 파손 | 신고 접수만으로 파손 확정 금지 |
| 취소됨 | `RentalStatus CANCELLED` | 정책 근거 필요 |

### 9.3 보안 공통 규칙
- App 로그인 평문 비밀번호는 제출 직후 화면 상태에서 제거, 로그·분석 이벤트·영속 저장소에 남기지 않는다
- `password_hash`는 프론트엔드 타입·응답·캐시에 절대 포함하지 않는다(Spring 전용)
- `accessToken`, Toss `paymentKey`는 로그·분석 이벤트에 남기지 않는다
- Kiosk 얼굴 인증 세션과 App 로그인 세션은 완전히 분리 — 상호 재사용 불가
- `.env` 파일은 수정하지 않는다

### 9.4 오류 처리 공통 원칙
- `retryable=false` 오류는 자동 재전송하지 않는다
- Timeout 후 새 키를 만들지 않고 기존 식별자로 최종 상태를 재조회한다
- 기술 오류·stack trace·token·`paymentKey`를 사용자 메시지·로그·일반 상태 저장소에 남기지 않는다
- 401(로그인 만료)과 403(권한 부족)을 구분한다
- 멱등성: 동일 키·동일 payload 재요청은 기존 결과 사용, `409 IDEMPOTENCY_CONFLICT`는 새 사용자 의사결정 없이 재전송하지 않는다
- 각 API별 세부 오류 코드는 §5의 개별 API 항목 참고(모두 12-R 원문 확인 완료)

---

## 10. 진행 원칙 (Kiosk와 공통)

1. 화면/코드 구현을 요청받으면 **항상 구조 파악 → 계획·리스크 정리 → 승인 대기 → 구현** 순서를 지킨다. 승인 전에는 파일을 만들거나 수정하지 않는다.
2. 노션 문서를 인용할 때는 몇 번 문서(06, 09, 12-R)의 어느 절인지 출처를 밝힌다.
3. `DECISION_REQUIRED`·`PROVISIONAL_API`·`DEC-XXX` 항목은 임의로 확정하거나 추측으로 구현하지 않는다.
4. 문서 간 내용이 충돌하면 12-R(API 계약) → 06번(FE 역할) → 09번(화면흐름) 순으로 우선하되, 충돌 사실을 먼저 알린다.
5. 한 번에 하나의 Phase(Screen ID)만 구현한다.
6. 구현 후 (Expo 프로젝트 초기화 이후) 실행 확인 및 수동 화면 확인을 실행한다.
7. 변경 사항은 `PROGRESS.md`에 기록한다.

---

## 12. 실제 Figma/와이어프레임과의 차이 (2026-07-31 확인)

사용자가 직접 그린 앱 와이어프레임(PDF, 16화면)을 확인한 결과, 노션 문서 기준으로 이번 Phase 범위에서 뺐던 기능들의 **완성된 화면이 이미 존재**한다. 아래는 그 목록이며, **MVP는 노션 문서 기준을 우선하기로 결정**(2026-07-31)했으므로 지금 당장 구현하지 않는다 — 단 아래 항목들을 나중에 만들 때는 이 와이어프레임을 그대로 따르면 된다.

| 기능 | 와이어프레임 상태 | 노션 문서 판정 | 처리 |
|---|---|---|---|
| 대여 연장(마이페이지 "연장하기" + 확인 모달) | 완성된 화면 있음 | `DEC-SF-006` MVP 제외 확정 | 보류, 와이어프레임 6번 페이지 참고 |
| 분실/파손 신고("신고하기" + 확인 모달) | 완성된 화면 있음 | `DEC-SF-007` DEFERRED_NOT_CONTRACTED | 보류, 와이어프레임 7번 페이지 참고 |
| 지도(대여소 검색·마커·상세, 하단 탭 포함) | 화면 3장 완성(지도/마커/상세) | P1, NOT_ACTIVE_P0 | 보류, 와이어프레임 9~11번 페이지 참고 |
| 문의하기(유형·제목·내용 네이티브 폼) | 완성된 화면 있음 | DEFERRED_NOT_CONTRACTED, 외부 링크로 축소 권장 | 보류, 와이어프레임 16번 페이지 참고 |
| 알림(목록·빈상태·모두읽음) | 완성된 화면 있음 | 서버 영속 없음, NOT_ACTIVE_P0 | 보류, 와이어프레임 12~13번 페이지 참고 |

> ⚠️ **정정(2026-07-31, 같은 날)**: 위 표의 "노션 문서 우선" 방침은 사용자가 곧바로 번복했다. **노션 문서와 와이어프레임이 충돌하면 일괄 규칙으로 정하지 않고, 그때그때 사용자에게 확인한다.** 아래는 그 첫 적용 사례다.

**하단 탭 구성**: 와이어프레임 기준으로 확정(2026-07-31) — **홈 / 지도 / 내역조회 / 마이페이지** 4탭. 챗봇(`SCR-APP-CHATBOT-001`)은 탭이 아니라 홈 화면 안의 플로팅 버튼으로 진입하는 구조이며, 이 부분은 홈 화면 구현(Phase 3)에서 마저 연결한다. 지도(`SCR-APP-STATION-001`)는 이제 핵심 내비게이션에 포함되므로, 노션 문서의 "P1·NOT_ACTIVE_P0" 판정과 별개로 최소 탭 자리(자리표시자 화면)는 유지한다 — 단 지도 화면의 실제 기능(검색·마커·상세)을 언제 구현할지는 아직 미정이며 착수 시 다시 확인한다.

연장·신고·문의·알림 4개 기능은 아직 미확인 상태로 남아있다 — 각각 실제 구현 착수 시점에 노션 문서를 따를지 와이어프레임을 따를지 다시 확인한다.

> 와이어프레임 원본은 사용자가 대화 중 PDF로 공유한 것이며 이 리포지토리에는 별도 저장하지 않았다 — 필요 시 사용자에게 재요청.

---

## 13. 변경 이력

| 일자 | 내용 |
|---|---|
| 2026-07-31 | 최초 작성. 06번(App 관련 부분)·09번(App 관련 부분) 노션 원문을 서브에이전트로 전량 확인 후 발췌, 12-R DB에서 App Public REST 13개 개별 페이지를 전량 원문 확인해 반영. Kiosk CLAUDE.md/PROJECT_GUIDE.md와 동일한 하네스 패턴으로 구축. |
| 2026-07-31(추가) | 사용자가 공유한 실제 앱 와이어프레임(PDF)과 대조 — 연장·신고·지도·문의·알림 화면이 이미 와이어프레임에 존재함을 확인. §12에 상세 기록. |
| 2026-07-31(정정) | "노션 문서 우선" 일괄 방침을 사용자가 번복 — 문서·와이어프레임 충돌은 그때그때 확인하기로 변경. 첫 적용으로 하단 탭 구성을 와이어프레임 기준(홈/지도/내역조회/마이페이지, 챗봇은 홈의 플로팅 버튼)으로 확정하고 코드에 반영. |
| 2026-08-05 | `DEC-SF-005`(대여 기한·연체 기준) 사용자 확인으로 RESOLVED — 대여 기한 1일(24시간) 고정. `rentalApi.ts`/`homeApi.ts`/`historyApi.ts` Mock 전부 `dueAt = rentedAt + 24h`로 통일. |
| 2026-08-05(추가) | `DEC-SF-004`(지도 SDK·위치 권한) 사용자 확인으로 RESOLVED — 실제 지도 SDK 대신 SVG 기반 가상 캠퍼스 지도로 확정(신규 의존성 없음). `app-screen-flow.md` §2.3의 `LOCATION_PERMISSION_DENIED`·`MAP_SDK_ERROR` Variant는 실제 GPS/지도 SDK 전제라 이 결정과 개념적으로 안 맞게 됨 — 실제 구현 시 반영 필요(참고: 이 결정은 노션 원문(09번 §14 DEC-SF-004)과 다른 방향이며, 사용자가 명시적으로 확정한 내용). |
| 2026-08-06 | `DEC-SF-003`(날씨 공급자·호출 경계) 사용자 확인으로 RESOLVED — 호출 경계는 App→Spring으로 확정(다른 12개 API와 동일 패턴), 좌표는 실제 GPS 대신 캠퍼스 고정값(`37.501329, 127.039538`) 하드코딩. `docs/plans/2026-08-06-api-integration.md`에서 날씨를 제외했던 두 이유(좌표 수집 방법 없음·호출 경계 미확정)가 모두 해소되어 `weatherApi.ts`를 Mock에서 실제 호출로 전환. |
| 2026-08-09 | `DEC-SF-004`(지도 SDK) 재번복 — 2026-08-05에 확정했던 SVG 가상 캠퍼스 지도를 카카오맵 JS SDK 커스텀 타일셋(`react-native-webview` + `source={{ html }}`)으로 교체. PLAN `docs/plans/2026-08-08-SCR-APP-STATION-001.md` 참고. 기존 `CampusMap.tsx`/`useCampusMapGesture.ts`/`mapGeometry.ts`/`StationMarker.tsx` 삭제, `KakaoMapWebView.tsx`/`kakaoTiles.generated.ts` 신규. 구현 중 확인된 핵심 사실: (1) 카카오 JS SDK는 `source={{ html }}`로 로컬 생성한 HTML에서도 도메인 검증을 통과해 외부 호스팅이 전혀 불필요, (2) `projectionId: null` 좌표계는 left-bottom이 원점이라 이미지 좌표(y=0이 위)를 그대로 쓰면 안 되고 뒤집어야 함(문서에 명시돼 있으나 처음엔 놓쳐서 카카오 실제 지도가 비쳐 보이는 버그로 이어짐), (3) fallback 타일은 투명이 아니라 불투명이어야 함(투명이면 밑에 깔린 카카오 실제 지도가 노출됨). 타일은 사용자가 직접 제작한 512×512 PNG 14장(0단계 3×3·1단계 2×2·2단계 1×1, `limit=ceil(3/2^z)` 카카오 공식 샘플 공식) — 원본은 `src/assets/campus-tiles/`, 앱에는 base64로 임베드. 드래그(팬)는 켜두기로 확정(범위 벗어나면 빈 화면 노출 감수). |

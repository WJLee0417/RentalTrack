# Billisan App 화면 흐름 명세 (09번 문서 발췌 · App 전용)

> 원본: 노션 "09. Billisan Kiosk·App 와이어프레임 기반 화면 흐름 명세 v3.0"
> 이 파일은 원본 중 Kiosk(SCR-KSK-*) 관련 부분을 제외하고 App(SCR-APP-*) 관련 부분만 발췌함.
> 원본 URL: https://app.notion.com/p/736a5037c20c824ba430014fd569c825
> 발췌 근거: 서브에이전트가 python 슬라이싱으로 원문 전량(130,123자)을 읽어 App 관련 내용만 추출(2026-07-31)
> App 기본 화면 9개(§19.2 최신 재집계 기준, §5.3 초기 집계는 8개였음), Variant 37개

---

## 1. 화면 ID 체계

```
SCR-APP-AUTH-001
SCR-APP-HOME-001
SCR-APP-STATION-001
SCR-APP-STATION-DETAIL-001
SCR-APP-MYPAGE-001
SCR-APP-HISTORY-001
SCR-APP-NOTIFICATION-001
SCR-APP-INQUIRY-001
SCR-APP-CHATBOT-001
```

---

## 2. 화면 카탈로그

### 2.1 SCR-APP-AUTH-001 (로그인)

| Variant | 진입 조건 | 종료 조건 | Requirement | Story | Policy | 우선순위 | 판정 |
|---|---|---|---|---|---|---|---|
| `DEFAULT` | 비인증 | 인증 성공 또는 종료 | `FR-AUTH-001` | `US-01-001` | `POL-AUTH-006` | P0 | `CONFIRMED_P0 · DEC-031` |
| `INVALID_CREDENTIALS` | 인증 실패 | 재시도 | — | — | `POL-AUTH-006, POL-NOTICE-002` | P0 | `CONFIRMED_P0 · DEC-031` |

**화면 구성**: 대학 계정 식별자(`login_id`, 예: 학교 이메일) 입력, 비밀번호 입력, 로그인 버튼. 비밀번호 찾기는 P0에서 숨긴다(POST_P0).

**동작**: Spring이 `login_id`로 계정을 조회하고 서버의 Spring Security `PasswordEncoder`로 입력 평문과 `password_hash`를 비교한 경우에만 토큰을 발급한다. App은 홈으로 이동하고 비밀번호 상태를 즉시 삭제한다.

**오류 문구 원칙**: 자격 증명 오류와 네트워크·서버 오류를 분리하되 계정 존재 여부는 노출하지 않는다.

**API**: `AUTH-001`(로그인), `AUTH-002`(로그아웃, 별도 진입점)

---

### 2.2 SCR-APP-HOME-001 (홈)

| Variant | 진입 조건 | 종료 조건 | Requirement | Story | 우선순위 | 판정 |
|---|---|---|---|---|---|---|
| `DEFAULT` | 인증 성공 | 하단 탭·알림·지도 이동 | `FR-APP-001` | `US-05-001~002` | P0 | `UI_ONLY_CHANGE` |

**화면 구성**: 사용자 표시 정보, 현재 대여 요약, 미정산 요약, 6시간 강수 예보 카드, 알림 아이콘, 중앙 콘텐츠 영역(현재 상태·지도 조합으로 확정 — GAP-004, §19.3).

**상태**: `Partial Success` — 날씨 실패가 대여·정산 정보 전체 실패로 확장되지 않게 한다.

**API**: `USER-001 | HOME-001 | WEATHER-001` (§19.6 화면-API 매핑)

---

### 2.3 SCR-APP-STATION-001 (대여소 지도) — **P1, NOT_ACTIVE_P0**

| Variant | 우선순위 | 판정 |
|---|---|---|
| `MAP_WITH_STATIONS` | P1 | `NOT_ACTIVE_P0` |
| `NO_NEARBY_STATIONS` | P1 | `NOT_ACTIVE_P0` |
| `STATION_SELECTED_BOTTOM_SHEET` | P1 | `NOT_ACTIVE_P0` |
| `LOCATION_PERMISSION_DENIED` | P1 | `NOT_ACTIVE_P0` |
| `MAP_SDK_ERROR` | P1 | `NOT_ACTIVE_P0` |
| `STATION_DATA_ERROR` | P1 | `NOT_ACTIVE_P0` |
| `STALE_DATA` | P1 | `NOT_ACTIVE_P0` |

- 위치 권한 거절 시 생략·캠퍼스 기본 중심으로 처리
- 지도 SDK 실패 시 텍스트 목록 제공(fallback 필수)
- 지도 SDK 자체가 `DEC-SF-004`로 미확정 — 임의로 특정 SDK를 채택하지 않는다

**API**: `STATION-001`(§19.6에 등장하나 §3.1의 확정 13개에는 없음 — P1이라 P0 계약 대상 아님)

---

### 2.4 SCR-APP-STATION-DETAIL-001 (대여소 상세) — 신규(§19.2 재집계 시 추가)

| Variant | 판정 |
|---|---|
| `DEFAULT` | `NEW_SCREEN_ID · REVIEW_REQUIRED` |
| `LOADING` | `NEW_SCREEN_ID · REVIEW_REQUIRED` |
| `ERROR` | `NEW_SCREEN_ID · REVIEW_REQUIRED` |
| `STALE` | `NEW_SCREEN_ID · REVIEW_REQUIRED` |

**API**: `STATION-002`(§19.6, P0 확정 13개에는 없음)

---

### 2.5 SCR-APP-MYPAGE-001 (마이페이지)

| Variant | 진입 조건 | 종료 조건 | 우선순위 | 판정 |
|---|---|---|---|---|
| `ACTIVE_RENTAL` | 활성 RENTAL 존재 | 연장·신고·문의·로그아웃 | P0(핵심 조회) + 후속 액션은 보류 | `NOT_ACTIVE_P0`(후속 액션) |
| `EXTENSION_CONFIRM` | 연장 선택 | 취소 또는 연장 결과 | `DEFERRED_NOT_CONTRACTED` | `NOT_ACTIVE_P0`(`CAND-APP-EXTENSION-001`) |
| `INCIDENT_REPORT_CONFIRM` | 신고 선택 | 취소 또는 접수 결과 | `DEFERRED_NOT_CONTRACTED` | `NOT_ACTIVE_P0`(`CAND-APP-INCIDENT-001`) |
| `OVERDUE_UNSETTLED` | 미정산 Settlement 존재 | 결제 진입·상태 재조회 | `FR-APP-003`, P0 | `NOT_ACTIVE_P0`(원문 표기 그대로 — 조회는 P0, 표기상 판정은 NOT_ACTIVE_P0로 병기됨) |

**동작 원칙**: 결제 복귀 후 화면 문구만 성공으로 바꾸지 말고 Spring의 `SETTLEMENT`·`PAYMENT_ATTEMPT`를 재조회한다.

**API**: `USER-001 | RENTAL-001 | SETTLEMENT-001 | PAYMENT-001~003` (연장·신고 API는 근거 확정 전까지 만들지 않음)

---

### 2.6 SCR-APP-HISTORY-001 (이용 내역)

| Variant | 진입 조건 | 우선순위 | 판정 |
|---|---|---|---|
| `ALL` | 내역 탭 | P0 | `KEEP_BASELINE` |
| `ACTIVE` | 대여 중 필터 | P0 | `KEEP_BASELINE` |
| `COMPLETED` | 반납 완료 필터 | P0 | `KEEP_BASELINE` |
| `EMPTY` | (§19.2 재집계 추가) | P0 | `KEEP_BASELINE` |
| `CARD_EXPANDED` | (§19.2 재집계 추가) | P0 | `KEEP_BASELINE` |

- 페이지네이션 여부는 Phase 2에서 데이터 규모에 따라 결정한다(현재 임의로 cursor 방식 확정하지 않되, `RENTAL-002` API 자체는 cursor 기반으로 이미 계약돼 있음 — PROJECT_GUIDE.md §5.6 참고)

**API**: `RENTAL-002`

---

### 2.7 SCR-APP-NOTIFICATION-001 (알림) — 보류

| Variant | 판정 |
|---|---|
| `LIST_WITH_UNREAD` | `DEFERRED_NOT_CONTRACTED`/`NOT_ACTIVE_P0` |
| `EMPTY` | 〃 |
| `SWIPE_ACTION` | 〃 |

- ERD v0.5에서 알림 엔터티 제거 — 즉시 알림 또는 화면 상태로만 처리
- 영속 알림 목록·읽음 상태는 `NOT_ACTIVE_P0`
- `Loading`, `List`, `Empty`, `Error`, `Partial Read Failure` 상태 정의

**API**: `ALERT-001`(빈 배열·로컬 Swipe만 지원, 서버 영속 읽음은 `ALERT-002 DEFERRED_NOT_CONTRACTED`)

---

### 2.8 SCR-APP-INQUIRY-001 (문의 접수) — 보류

| Variant | 판정 |
|---|---|
| `CREATE_FORM` | `NOT_ACTIVE_P0` |
| `SUBMITTING` | 〃 |
| `SUBMITTED` | 〃 |
| `VALIDATION_ERROR` | 〃 |
| `SERVER_ERROR` | 〃 |

- 단순 외부 문의 링크·이메일 이동으로 축소되면 `NO_API_REQUIRED`가 될 수 있다

**API**: `INQUIRY-001`(접수 방식 `DEFERRED_NOT_CONTRACTED`), `CAND-APP-INQUIRY-LIST-001`(문의 상태 조회)는 `OUT_OF_SCOPE`

---

### 2.9 SCR-APP-CHATBOT-001 (빌리 챗봇) — **P0 확정 신규 기능**

| Variant | 설명 |
|---|---|
| `INITIAL` | 추천 질문과 자유 입력을 함께 표시 |
| `ANSWERING` | 사용자 메시지 즉시 표시 + Assistant 처리 중 상태 |
| `ANSWERED` | 답변 + 허용된 CTA 표시 |
| `FALLBACK_FAQ` | 외부 LLM 장애 시 정적 FAQ 답변임을 과도한 기술 노출 없이 표시 |
| `TIMEOUT` | 입력 보존 + 재시도/문의 CTA |
| `ERROR` | 입력 보존 + 재시도/문의 CTA |

**검증 상태(§19.7 원문)**:
```
USER_DECISION_OVERRIDE = CHATBOT_P0
UPSTREAM_SYNC_REQUIRED = true
CHATBOT_PRIORITY = P0
CHATBOT_SCREEN_STATUS = IN_SCOPE
CHATBOT_API_STATUS = CONFIRMED_API
CHATBOT_PROVIDER_STATUS = PROVISIONAL_ALLOWED
CHATBOT_HISTORY_PERSISTENCE = false
CHATBOT_FALLBACK = REQUIRED
CHATBOT_P0_GATE_STATUS = PASSED
```

상세 규칙(Intent·CTA·민감정보 금지 목록)은 `PROJECT_GUIDE.md` §6 참고.

**API**: `CHATBOT-001`

---

## 3. App E2E 흐름 (결제, 09번 §7.5 mermaid 원문)

```
User->>App: 마이페이지 진입
App->>Spring: 현재 대여 조회(RENTAL-001)
App->>Spring: 미정산 조회(SETTLEMENT-001)
Spring-->>App: 현재 대여 + 미정산 목록
alt 미정산 없음
    App-->>User: ACTIVE_RENTAL 또는 Empty
else PENDING Settlement 존재
    App-->>User: OVERDUE_UNSETTLED·결제 버튼
    User->>App: 결제하기
    App->>Spring: 결제 시도 생성(PAYMENT-001)
    Spring-->>App: Toss 결제 진입 정보(successRedirectUri/failRedirectUri 포함)
    App->>Toss: 테스트 결제
    Toss-->>App: 성공·실패·취소 반환
    App->>Spring: 결제 승인(PAYMENT-002) 또는 상태 재조회(PAYMENT-003)
    Spring-->>App: PAYMENT_ATTEMPT·SETTLEMENT 최종 상태
    App-->>User: 최종 상태 기준 화면 갱신
end
```

---

## 4. 문서 간 유의할 불일치

1. **App 화면 개수**: §5.3(초기 집계) "8개 화면·14 Variant" vs §19.2(최신) "9개 화면·37 Variant" — 이 문서는 §19.2를 기준으로 삼는다.
2. **WEATHER-001 활성 여부**: 06번은 확정 13개 API에 포함, 09번은 관련 후보(`CAND-APP-WEATHER-001`, `CAND-EXTERNAL-WEATHER-001`)를 `NOT_ACTIVE_P0`로 판정. API 계약(12-R) 자체는 존재가 확인됐으나 실제 공급자·구현 여부는 `DEC-SF-003`처럼 미확정 — 구현 시 재확인 필요.
3. **STATION-001/002, INQUIRY-001**: §19.6 화면-API 매핑 표에는 등장하지만 06번의 확정 13개 목록에는 없음 — 화면 카탈로그상 P1/보류이기 때문으로 정합적으로 해석됨(§4 참고).

---

## 변경 이력

- 2026-07-31: 09번 노션 원문(v3.0)에서 App(SCR-APP-*) 관련 부분만 서브에이전트가 전량 슬라이싱해 발췌, 최초 작성.

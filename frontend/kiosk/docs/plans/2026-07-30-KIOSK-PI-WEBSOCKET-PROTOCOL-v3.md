# PLAN: Kiosk↔Pi WebSocket 프로토콜 전면 도입 (12-I v3.0 기준)

> 대응 문서: Notion "12-I. Billisan 장치 메시지·내부 인터페이스 계약 DB v3.0 (1)" (`Kiosk↔Pi WebSocket` 분류, Operation 7개 상세 페이지 전체 조사 완료, 2026-07-30)
> **문서 자체 상태: `DOCUMENT_STATUS=REVIEW_REQUIRED`, `RELEASE_STATUS=NOT_RELEASED`, `VERSION_CHANGE_CLASSIFICATION=BREAKING_CONTRACT_AND_ARCHITECTURE_CHANGE`** — 아직 팀 공식 승인 전 문서다.
> **아직 승인 전 — 이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## 0. 왜 이 PLAN이 필요한가 (충돌 고지)

`PROJECT_GUIDE.md`(우리 코드베이스 지침서, v2.0 기준)는 다음을 전제로 한다:
- `DEC-024`: Kiosk FE는 **Spring Kiosk REST API 7개만** 호출한다.
- `DEC-032`: 얼굴 인증 트리거("준비되었습니다" 클릭) **1곳에 한해서만** Pi에 직접 WebSocket 연결하는 예외.

그런데 오늘 조사한 **Notion "12-I. 장치 메시지·내부 인터페이스 계약 DB v3.0"**은 이와 배치된다:
- `PI_LOCAL=7`(Kiosk↔Pi WebSocket) 카테고리가 별도로 존재하고, **Kiosk의 7개 Operation 전부**(`KSK-STATION-001`, `KSK-SESSION-001`, `KSK-AUTH-001`, `KSK-SESSION-002`, `KSK-RENT-001`, `KSK-RETURN-001`, `KSK-CANCEL-001`)가 `ws[s]://{piHost}:{kioskWsPort}/ws/v1/kiosk` **WebSocket 하나**로 통합되어 있다.
- 문서 자체가 "v2.1 Kiosk Local REST 상세 원문 — `SUPERSEDED_REFERENCE`"라고 명시 — REST 방식을 명시적으로 폐기 처리한 것으로 보인다.
- 이 12-I 문서가 `DEC-024`를 대체하는 새로운 공식 결정인지, 아니면 아직 검토 중인 초안인지는 **문서 메타데이터(`REVIEW_REQUIRED · NOT_RELEASED`)만으로는 확정할 수 없다** — 반드시 팀(백엔드/임베디드) 확인 필요.

**지금 겪고 있는 실제 증상과의 연결고리**: `piSocket.ts`가 최소 연결 확인용으로 보낸 `{"type":"START_FACE_AUTH"}`에 대해, 실서버(`ws://70.12.247.56:5000`)가 `{"stage":"AUTH_STARTED", ...}`로 응답하고 있다. 이 필드명(`stage`/`AUTH_STARTED`)은 12-I 문서의 정식 스키마(`status`/`AUTH_PROCESSING`, Envelope 구조)와 정확히 일치하진 않지만, **여러 메시지가 순차로 오는 구조라는 점은 일치**한다 — 즉 우리 쪽이 "첫 메시지만 받고 끝"으로 구현한 것 자체가 이 프로토콜과 근본적으로 안 맞는다.

---

## 1. 조사 결과: Operation 7개 전체 스키마 요약

공통: 모든 Request는 Envelope(`schemaVersion, messageType, messageId, operationId, sessionId, requestId, correlationId, sentAt, expiresAt, sequence, payload`)로 감싸고, 흐름은 **Request → ACK → (Event 0회 이상) → Result | Error** 순서. 공통 Error 메시지 타입은 `KIOSK.OPERATION.ERROR`.

| Operation ID | Request | ACK | Event | Result | 주요 Error Code | Timeout |
|---|---|---|---|---|---|---|
| `KSK-STATION-001` 대여소 상태 조회 | `KIOSK.STATION_SUMMARY.REQUEST` | `...ACK` | 없음 | `...RESULT` | `STATION_SUMMARY_UNAVAILABLE`, `SNAPSHOT_STALE` 등 | ACK 2s / Result 3s |
| `KSK-SESSION-001` 세션 열기 | `KIOSK.SESSION.OPEN.REQUEST` | `...ACK` | `...EVENT`(RECOVERING) | `...RESULT` | `SESSION_CONFLICT`, `KIOSK_WEBSOCKET_UNAVAILABLE` 등 | ACK 2s / Result 3s |
| `KSK-AUTH-001` 얼굴 인증 | `KIOSK.FACE_AUTH.REQUEST` | `...ACK` | `...EVENT`(GUIDANCE\|AUTH_PROCESSING) | `...RESULT` | `FACE_NOT_DETECTED`, `AUTHENTICATION_FAILED`, `ORIN_WEBSOCKET_UNAVAILABLE` 등 | ACK 2s / Terminal 15s |
| `KSK-SESSION-002` 세션 상태 조회 | `KIOSK.SESSION.STATE.REQUEST` | `...ACK` | 없음 | `...RESULT`(kioskStep 등 대량 필드) | `SESSION_NOT_FOUND`, `SESSION_EXPIRED` 등 | ACK 2s / Result 3s |
| `KSK-RENT-001` 우산 대여 | `KIOSK.RENT.REQUEST` | `...ACK` | `...EVENT`(RENT_SLOT_READY 등) | `...RESULT`(rentalSummary) | `SLOT_NOT_AVAILABLE`, `UNLOCK_FAILED` 등 | ACK 2s / 물리 30s / 전체 35s |
| `KSK-RETURN-001` 우산 반납 | `KIOSK.RETURN.REQUEST` | `...ACK` | `...EVENT`(INSPECTION_GUIDE\~SPRING_COMMITTING) | `...RESULT`(inspectionResult) | `INSPECTION_FAILED`, `LOCK_FAILED` 등 | ACK 2s / AI검수 30s / 전체 90s |
| `KSK-CANCEL-001` 세션 취소 | `KIOSK.SESSION.CANCEL.REQUEST` | `...ACK` | 없음 | `...RESULT`(CANCELLED) | `CANCEL_NOT_ALLOWED`, `PHYSICAL_OPERATION_IN_PROGRESS` 등 | ACK 2s / Result 3s |

**세션 흐름 핵심**: `KSK-SESSION-001`이 반환한 `sessionId` 하나를 이후 `KSK-AUTH-001` → `KSK-RENT-001`(또는 `KSK-RETURN-001`) → 필요 시 `KSK-CANCEL-001`까지 **계속 재사용**한다. 즉 지금처럼 얼굴 인증만 별도 연결로 끝내는 구조가 아니라, **하나의 세션이 대여/반납 전체 여정을 관통**한다.

각 Operation의 필드 단위 상세(모든 payload 필드명·타입·enum 값)는 이번 조사에서 전부 확보했으며, 구현 단계 PLAN에서 타입 정의로 옮길 예정.

---

## 2. 현재 코드와의 격차

| 현재 상태 | 격차 |
|---|---|
| `api/stationApi.ts` — Mock, Spring REST 흉내 | `KSK-STATION-001` WebSocket으로 교체 필요 |
| `api/eligibilityApi.ts`, `api/rentalApi.ts` | `KSK-SESSION-001`+`KSK-RENT-001`로 통합 필요 (자격 검증이 별도 API가 아니라 `KIOSK.FACE_AUTH.RESULT`의 `eligibility` 필드로 옴) |
| `api/faceAuthApi.ts`, `api/piSocket.ts`(`requestFaceAuthStart`) | 하나의 `KSK-AUTH-001` Envelope 왕복으로 통합, 지금처럼 "연결 확인 + Mock 결과 판정" 이원 구조 폐기 |
| `api/returnEligibilityApi.ts`, `api/inspectionApi.ts`, `api/returnApi.ts` | `KSK-RETURN-001` 하나로 통합 (활성 대여 확인·AI 검수·슬롯 배정·완료가 전부 이 Operation의 Event/Result 안에 있음) |
| `kioskFlowStore`(currentScreen) + `App.tsx` 로컬 state(mode/returnStep 등) | `KSK-SESSION-002.kioskStep`(`FACE_GUIDE\|AUTH_PROCESSING\|RENT_PROCESSING\|REMOVE_UMBRELLA\|INSPECTION_GUIDE\|RETURN_SLOT_GUIDE\|COMPLETED\|RECOVERY_REQUIRED\|FAILED`)을 화면 상태의 권위값으로 삼는 구조로 재설계 필요 |
| 세션 개념 자체 없음(화면마다 독립 Mock 호출) | 전역 WebSocket 연결 1개 + `sessionId` 전역 상태 관리 필요 |

**즉 이번 Phase 2/3에서 만든 Mock API 7개 대부분이 이 프로토콜 도입 시 폐기 또는 대체 대상이다.**

---

## 3. 구현 방향 (초안 — 아직 미확정 사항 다수, 4절 참고)

### 3.1 신규 공용 모듈
- `src/api/piKioskSocket.ts` — 단일 지속 WebSocket 연결 관리자
  - `connect()`: 앱 시작 시(또는 MAIN 화면 진입 시) 1회 연결
  - `sendOperation(operationId, messageType, payload, {sessionId})`: Envelope 생성 → 전송 → ACK 대기(Timeout별) → Event 콜백 → Result/Error Promise resolve/reject
  - `requestId`/`messageId`/`correlationId`는 `crypto.randomUUID()`로 생성 (기존 주석 처리된 코드의 `buildEnvelope`와 거의 동일 — 그대로 되살려 확장)
  - ACK 유실 시 같은 `requestId` + 새 `correlationId`로 재전송하는 로직 포함
- `src/types/piEnvelope.ts` (기존 `types/piProtocol.ts` 확장/교체) — Envelope 및 7개 Operation의 Request/ACK/Event/Result/Error payload 타입 전체 정의

### 3.2 세션 상태 관리
- `store/kioskSessionStore.ts`(신규) — `sessionId`, `mode`, 최신 `kioskStep`, `eligibility`, `checkoutSlot`/`returnSlot`, `rentalSummary` 등 `KSK-SESSION-002.RESULT` 형태를 그대로 보관
- 화면 라우팅을 `kioskStep` 값 기반으로 재구성 — 현재 `SCREEN_ID`/`ReturnStep` 로컬 state 조합을 이 `kioskStep` enum 매핑으로 대체

### 3.3 화면별 연동 지점 (예시, 상세는 승인 후 화면별 PLAN에서)
- MAIN 진입 시: `KSK-STATION-001`로 재고 조회(현재 `stationApi.ts` Mock 대체)
- 대여/반납 클릭: `KSK-SESSION-001`로 세션 오픈
- "준비되었습니다": `KSK-AUTH-001` 전송, Event로 `faceGuidance` 안내 갱신, Result로 `eligibility` 판정
- 대여 진행: `KSK-RENT-001`, Event로 슬롯 번호·진행 단계 갱신, Result로 완료 확정
- 반납 진행: `KSK-RETURN-001`, Event로 검수 단계·슬롯 번호 갱신, Result로 `inspectionResult` 확정
- 이탈/취소: `KSK-CANCEL-001` (단, 물리 동작 시작 후에는 `CANCEL_NOT_ALLOWED` — 취소 버튼 자체를 그 시점부터 숨기거나 비활성화해야 함)
- 재연결/폴링: `KSK-SESSION-002`로 언제든 권위 상태 재확인

---

## 4. DECISION_REQUIRED / 확인 필요 (임의로 확정하지 않음)

- **문서 승인 여부**: 12-I 문서 자체가 `REVIEW_REQUIRED · NOT_RELEASED`다. `DEC-024`를 대체하는 팀 공식 결정인지 백엔드/임베디드 담당자 확인 필수. **이게 확인되기 전까지는 아래 구현에 착수하지 않는다.**
- **실서버 프로토콜 일치 여부**: 지금 연결 중인 `70.12.247.56:5000` 서버가 실제로 이 Envelope 구조(`messageType`, `KIOSK.FACE_AUTH.REQUEST` 등)를 구현했는지 미확인 — 지금 관측된 `{"stage":"AUTH_STARTED",...}` 응답은 스펙과 필드명이 다르다. 서버가 아직 이 문서를 반영 중(WIP)일 수 있다.
- **`{piHost}:{kioskWsPort}`의 실제 값**: `.env`의 `VITE_PI_WS_URL`이 이 스펙의 `stationId`/`clientSessionKey` 등을 요구하는 새 핸드셰이크와 호환되는지 미확인.
- **팀 내 커뮤니케이션 필요**: 이 정도 규모(Mock API 7개 대부분 폐기, 세션 개념 신규 도입)는 한 번에 구현하기엔 리스크가 커서, **화면별로 쪼개서 별도 PLAN + 승인**을 받는 게 안전해 보인다(예: 얼굴 인증만 먼저, 그다음 대여, 그다음 반납).

---

## 5. 제안하는 다음 단계 (승인 대기)

1. **팀 확인**: 이 12-I 문서가 실제 채택된 기준인지, 백엔드/임베디드 담당자에게 확인
2. 확인되면, **`KSK-AUTH-001`(얼굴 인증) 하나만 먼저** 정식 프로토콜로 교체하는 좁은 범위의 PLAN을 별도로 작성 (현재 겪고 있는 문제를 가장 직접적으로 해결하는 범위)
3. 그 뒤 `KSK-STATION-001`(메인 화면 재고), `KSK-SESSION-001/002`(세션 관리), `KSK-RENT-001`, `KSK-RETURN-001`, `KSK-CANCEL-001` 순서로 화면별 PLAN을 이어서 진행

이번 문서는 **조사·설계 초안**이며, 팀 확인 전에는 이 내용대로 코드를 작성하지 않는다.

---

## 변경 이력

- 2026-07-30: 최초 작성. Notion 12-I DB의 Kiosk↔Pi WebSocket 7개 Operation 상세 페이지 전체 조사 반영.

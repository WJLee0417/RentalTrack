# PLAN: SCR-KSK-AUTH-001 — 얼굴 인증을 실제 Pi WebSocket 프로토콜(KSK-AUTH-001)로 교체

> 대응 문서: Notion "12-I. Billisan 장치 메시지·내부 인터페이스 계약 DB v3.0" — `KSK-SESSION-001`, `KSK-AUTH-001` 상세 페이지(2026-07-30 조사 완료)
> 상위 문서: `docs/plans/2026-07-30-KIOSK-PI-WEBSOCKET-PROTOCOL-v3.md`(7개 Operation 전체 조사·전체 로드맵) — 이번 PLAN은 그중 얼굴 인증 1개만 쪼갠 범위
> **아직 승인 전 — 이 문서 승인 전에는 코드를 작성하지 않는다.**

---

## ⚠️ 0. 실서버 프로토콜 정정 (2026-07-30, 팀 채팅 @hyeee513 제공) — 아래 1~8절보다 우선함

이 PLAN을 처음 쓸 때는 Notion "12-I" 문서의 **Envelope 기반 7-Operation WebSocket 계약**(ACK/Event/Result, `messageType`/`sessionId`/`correlationId` 등)을 기준으로 설계했다. 그런데 **실제 백엔드 담당자가 팀 채팅으로 공유한 실서버 스펙은 이것과 다르다** — Envelope 구조가 아니라 **훨씬 단순한 flat JSON 스트림**이다. 이번 대화에서 실제로 관측했던 `{"stage":"AUTH_STARTED",...}` 응답과 정확히 일치하므로, **이 0절 내용이 실제 구현 기준으로 우선한다.** 1~8절(Envelope 기반 설계)은 이 정정을 반영해 다시 손봐야 한다 — 아직 코드는 이 사실만 기록하고 설계를 다시 짜지 않은 상태.

### 메시지 형식
Envelope 없이 단순한 flat 구조: `{"stage": "...", ...}`. 예:
```json
{"stage": "GUIDANCE", "message": "얼굴을 가이드 안으로 맞춰주세요"}
```

### 전체 stage 순서
**성공 경로**: `AUTH_STARTED → (GUIDANCE ×N) → AUTH_SUCCEEDED → SLOT_REQUESTED → SLOT_ASSIGNED → PHYSICAL_DONE → COMMIT_REQUESTED → COMMIT_SUCCEEDED → DONE`
**실패 경로**: `AUTH_FAILED` / `SLOT_TIMEOUT` / `PHYSICAL_FAILED` / `RECOVERY_REQUIRED` 등을 거침
**항상 마지막은**: `{"stage": "DONE", "status": ..., "resultCode": ...}`

→ 이 하나의 스트림이 **얼굴 인증뿐 아니라 슬롯 배정·물리 잠금해제·Spring Commit까지(=대여 전체 흐름)** 다 담고 있다. 즉 이 연결 하나가 `KSK-AUTH-001`뿐 아니라 사실상 `KSK-RENT-001` 영역까지 커버한다 — Notion 12-I처럼 Operation별로 별도 요청을 보내는 구조가 아니라, **한 번 인증 요청을 시작하면 서버가 대여 완료까지 알아서 stage를 순차로 밀어주는 구조**로 보인다.

### `GUIDANCE` stage 상세
- 현재 나가는 문구 2종: `"카메라를 바라봐 주세요"`(얼굴 미검출) / `"얼굴을 가이드 안으로 맞춰주세요"`(얼굴이 가이드 타원 밖)
- 수신 타이밍: `AUTH_STARTED` ~ `AUTH_SUCCEEDED`/`AUTH_FAILED` 사이에만 옴
- 빈도: 상태가 바뀔 때만(최대 초당 1회), 처음부터 가이드 안에 있으면 **0회일 수도 있음**
- 처리: `message` 값을 그대로 안내 영역에 표시(`FaceGuideOverlay` 아래쪽 텍스트 추천), 다음 stage 오면 지우거나 교체

### 확장성 원칙
**모르는 `stage` 값은 에러 처리하지 말고 무시할 것** — 앞으로 stage가 계속 추가될 예정이라고 명시적으로 요청받음. `switch`문에 `default: return`(또는 아무 것도 안 함) 패턴 필요, "알 수 없는 상태" 에러 UI로 빠지면 안 됨.

### 이 정정이 1~8절 설계에 미치는 영향 (다음에 다시 정리 필요)
- §3 `piSocket.ts` 설계의 ACK/Event/Result 3단계 구조, Envelope 빌더(`buildEnvelope`) — **불필요해짐**. 실제로는 소켓 하나 열고 `onmessage`로 오는 flat JSON의 `stage` 필드만 보고 분기하면 됨.
- §5 에러 매핑표의 에러 코드(`FACE_NOT_DETECTED`, `ORIN_WEBSOCKET_UNAVAILABLE` 등, Notion 12-I 기준) — 실서버는 `AUTH_FAILED`/`resultCode` 같은 다른 이름 체계를 쓸 가능성이 높음. `resultCode`에 어떤 값들이 오는지 아직 확인 안 됨(추가 확인 필요).
- 세션(`KSK-SESSION-001`) 별도 오픈이 필요한지 여부도 재확인 필요 — 지금 관측된 흐름은 세션 열기 없이 바로 `AUTH_STARTED`부터 시작하는 것처럼 보임.
- **`SLOT_ASSIGNED`/`PHYSICAL_DONE`/`COMMIT_*`까지 이 스트림에 포함된다는 건, "얼굴 인증만" 범위로 쪼갠 게 무의미해질 수도 있다는 뜻** — 인증 성공 이후 화면(슬롯 안내·대여 완료)도 이 같은 연결의 이후 stage로 자연스럽게 이어받는 게 맞는 설계일 가능성이 큼. 범위를 다시 논의할 필요 있음.

---

## 1. 구현 목표

`AuthScreen`/`faceAuthStore`가 지금 쓰고 있는 "연결 확인용 임시 코드"(`piSocket.ts`의 `requestFaceAuthStart`, 첫 메시지만 받고 끝)와 "인증 결과 Mock"(`faceAuthApi.ts`)을 **실제 계약(`KSK-AUTH-001`)**으로 교체한다.

`KSK-AUTH-001`은 Envelope의 `sessionId`가 **ACTIVE 세션**이어야 하므로, 세션 없이는 인증 요청 자체를 보낼 수 없다. 따라서 이번 PLAN은 얼굴 인증만이 아니라 **`KSK-SESSION-001`(세션 열기)까지 최소 범위로 포함**한다 — "얼굴 인증만"의 실질적 최소 단위다.

**이번 범위에 포함**: 세션 열기 + 얼굴 인증 요청·ACK·Event·Result·Error 처리, 화면 상태 전환.
**이번 범위에서 제외**(8절 상세): 세션 복구(`KSK-SESSION-002`), 재연결, 대여/반납 실행(`KSK-RENT-001`/`KSK-RETURN-001`), 명시적 취소(`KSK-CANCEL-001`), `eligibility` 필드 소비(기존 Mock 유지).

---

## 2. 만들거나 수정할 파일 목록

| 구분 | 파일 |
|---|---|
| 신규 | `src/types/piEnvelope.ts` |
| 수정(전면 재작성) | `src/api/piSocket.ts` |
| 수정 | `src/store/faceAuthStore.ts` |
| 수정 | `src/screens/SCR-KSK-AUTH-001/AuthScreen.tsx` |
| 삭제 | `src/api/faceAuthApi.ts` (Mock 인증 결과 판정 — 실제 서버 Result로 대체되어 더 이상 필요 없음) |
| 삭제 | `src/types/faceAuth.ts`의 `FACE_AUTH_RESULT`(Mock 전용 enum) — `AUTH_SCREEN_VARIANT`는 유지 |
| 변경 없음(재사용 확인) | `src/types/piProtocol.ts`(기존 `PiEnvelope` 타입, 필요시 `piEnvelope.ts`로 흡수) |

---

## 3. 각 파일의 역할

### `types/piEnvelope.ts` (신규)
Envelope 공통 구조 + `KSK-SESSION-001`/`KSK-AUTH-001` 두 Operation의 Request/ACK/Event/Result/Error payload 타입을 12-I 조사 결과 그대로 정의.

```ts
export interface PiEnvelope<TPayload> {
  schemaVersion: string
  messageType: string
  messageId: string
  operationId: string
  sessionId: string | null
  requestId: string
  correlationId: string
  sentAt: string
  expiresAt: string
  sequence: number
  payload: TPayload
}

// KSK-SESSION-001
export interface SessionOpenRequest {
  mode: 'RENT' | 'RETURN'
  stationId: string
  clientSessionKey: string
  lastKnownSessionId: string | null
  operationRequestIds: { faceAuthRequestId: null; rentalRequestId: null; returnRequestId: null }
}
export interface SessionOpenResult {
  sessionId: string
  mode: 'RENT' | 'RETURN'
  status: 'ACTIVE' | 'RECOVERING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'RECOVERY_REQUIRED'
  expiresAt: string
}

// KSK-AUTH-001
export interface FaceAuthRequest { authRequestId: string }
export interface FaceAuthEvent {
  sessionId: string
  authRequestId: string
  status: 'GUIDANCE' | 'AUTH_PROCESSING'
  guidanceCode?: string
  progressMode: 'INDETERMINATE'
}
export interface FaceAuthResult {
  sessionId: string
  authRequestId: string
  authenticated: boolean
  eligibility: unknown // 이번 범위에서는 소비하지 않음(4절)
  kioskStep: string
  userMessageCode: string | null
  retryable: boolean
}

export interface OperationError {
  errorCode: string
  message?: string
  retryable?: boolean
}
```

### `api/piSocket.ts` (전면 재작성)
- `buildEnvelope()` — 기존에 주석 처리돼 있던 구현을 그대로 되살림(거의 수정 불필요했음이 이번 조사로 확인됨)
- `getClientSessionKey()` — 기존 구현 그대로(sessionStorage, 비사용자 식별 키)
- 모듈 내부에 **단일 WebSocket 연결**을 유지하는 간단한 연결 관리자(`ensureSocket()`) — 이미 열려있으면 재사용, 없으면 새로 연다
- `runOperation<TReq, TEvent, TResult>(operationId, requestMessageType, sessionId, payload, onEvent?)` — 범용 실행기:
  1. Envelope 생성 후 전송
  2. `{requestMessageType}` → `.ACK`로 치환한 타입의 메시지를 ACK Timeout(2초) 안에 기다림
  3. `.EVENT`로 치환한 타입이 오면 `onEvent` 콜백 호출하며 계속 대기(0회 이상)
  4. `.RESULT` 또는 `KIOSK.OPERATION.ERROR`가 오면 각각 resolve/reject
  5. Operation별 Terminal Timeout(`KSK-SESSION-001`=3초, `KSK-AUTH-001`=15초)이 지나면 reject
- `openKioskSession(mode: 'RENT' | 'RETURN'): Promise<SessionOpenResult>` — `KSK-SESSION-001` 호출
- `requestFaceAuth(sessionId: string, onEvent?: (event: FaceAuthEvent) => void): Promise<FaceAuthResult>` — `KSK-AUTH-001` 호출
- 기존에 있던 "TEMP 최소 연결 확인용" 코드와 "아직 스펙 미확정이라 주석 처리"였던 Envelope 코드는 전부 제거(이번에 스펙이 확인됐으므로 주석 보존할 이유가 없어짐)

### `store/faceAuthStore.ts` (수정)
`startCapture`+`authenticateFace` 2단계 구조를 **`start(mode, onAuthenticated)` 하나**로 합친다 — 지금까지는 "카메라 화면 전환"과 "인증 결과 판정"을 클라이언트 타이머(`AUTO_CAPTURE_DELAY_MS`)가 이어줬는데, 실제 프로토콜에서는 서버가 비동기로 알려주는 이벤트가 그 역할을 대신하기 때문이다.

```ts
interface FaceAuthState {
  variant: AuthScreenVariant
  sessionId: string | null
  start: (mode: 'RENT' | 'RETURN', onAuthenticated: () => void) => Promise<void>
  retry: () => void
  reset: () => void
}
```
- `start`: `openKioskSession(mode)` → `sessionId` 저장 → `variant: FACE_CAPTURE` → `requestFaceAuth(sessionId, onEvent)` 호출
  - `onEvent`에서 `status === 'AUTH_PROCESSING'`이면 `variant: FACE_PROCESSING`으로 전환(`GUIDANCE` 상태는 계속 `FACE_CAPTURE` 유지)
  - Result `authenticated === true` → `onAuthenticated()` 콜백 호출(variant는 그대로 두고 호출부가 다음 화면으로 전환하는 기존 패턴 유지)
  - Result `authenticated === false` 또는 Error → 5절 에러 매핑표에 따라 `FACE_NOT_DETECTED`/`FACE_NOT_MATCHED`로 전환
  - 연결 실패·ACK Timeout 등 통신 자체 실패 → 콘솔 로깅 후 `GUIDE`로 복귀(기존 동작 유지)

### `screens/SCR-KSK-AUTH-001/AuthScreen.tsx` (수정)
- `FACE_CAPTURE` 진입 시 클라이언트가 1.5초 뒤 `authenticateFace()`를 호출하던 `useEffect` **제거** — 이제 `startCapture` 시점에 이미 요청이 나가 있고, 서버 Event/Result가 오는 대로 스토어가 알아서 `variant`를 바꾼다
- `FaceAuthGuideScreen`의 `onAction`을 `() => start(mode, onAuthenticated)`로 변경
- 홈 복귀 안전장치(`AUTH_TIMEOUT_MS`)는 유지하되, 서버 Terminal Timeout(15초)보다는 넉넉하게(예: 20초) 조정 — 정상적인 서버 응답을 우리 쪽 타이머가 먼저 끊어버리지 않도록

---

## 4. 상태 관리 방식

- `sessionId`는 `faceAuthStore`에만 보관하고 이번 범위에서는 다른 곳(App.tsx)으로 노출하지 않는다 — 대여/반납 실행(`KSK-RENT-001`/`KSK-RETURN-001`)은 다음 단계 PLAN에서 세션을 이어받는 방식을 다시 설계한다.
- `KIOSK.FACE_AUTH.RESULT`의 `eligibility` 필드는 **이번 범위에서 소비하지 않는다**. 인증 성공(`authenticated: true`) 이후에는 지금처럼 `App.tsx`의 기존 Mock `checkRentalEligibility()`/`checkActiveRentalForReturn()`을 그대로 호출한다 — 실제 자격 데이터가 이미 왔는데 안 쓰고 Mock을 또 부르는 게 어색하지만, 범위를 얼굴 인증 하나로 좁히기 위한 의도적 선택이다. 자격 검증을 실제로 통합하는 건 다음 단계(대여/반납 PLAN)에서 다룬다.
- `mode`는 이미 `App.tsx`가 갖고 있으므로 `AuthScreen`이 `store.start(mode, onAuthenticated)` 호출 시 그대로 전달한다(`mode` prop은 이미 있음, 추가 배선 불필요).

---

## 5. 오류·상태 매핑 (Mock 데이터 구조 대신 — 이제 실제 서버 응답이므로)

| 서버 Result/Error | 화면 Variant | 비고 |
|---|---|---|
| `authenticated: true` | (변경 없음, `onAuthenticated()` 호출) | `eligibility` 필드는 무시 |
| Error `FACE_NOT_DETECTED` | `FACE_NOT_DETECTED` | 기존 UI 그대로 |
| Error `MULTIPLE_FACES` | `FACE_NOT_DETECTED` | 기존 UI 재사용(문구는 그대로, 정교화는 추후) |
| Error `AUTHENTICATION_FAILED` | `FACE_NOT_MATCHED` | 기존 UI 그대로 |
| Error `USER_NOT_ELIGIBLE` | `FACE_NOT_MATCHED` | 임시 매핑 — 원래는 "얼굴은 맞는데 자격 없음"이라 문구가 안 맞을 수 있음(리스크로 기록) |
| Error `ORIN_WEBSOCKET_UNAVAILABLE` / `ORIN_RESULT_TIMEOUT` / `MQTT_BROKER_UNAVAILABLE` | `FACE_NOT_DETECTED` | 임시 매핑 — 실제로는 "카메라 자체를 못 씀"이라 별도 안내가 맞지만, 이번 범위에서는 새 Variant를 만들지 않고 기존 것 재사용 |
| Error `COMMAND_EXPIRED` | `FACE_NOT_DETECTED` | 재시도 유도 |
| ACK Timeout(2초 안에 응답 없음) | `GUIDE` 복귀 | 기존 catch 동작과 동일 |
| Terminal Timeout(15초 안에 Result 없음) | `FACE_NOT_DETECTED` | 재시도 유도 |

이 표는 **임시 매핑**이다. 서버의 `userMessageCode`를 실제 사용자 문구로 변환하는 공식 테이블이 아직 없어서, 기존에 있던 두 화면(미검출/미매칭)에 최대한 합리적으로 욱여넣은 것 — 서버 쪽 `userMessageCode` 스펙이 확정되면 재정리 필요(7절 리스크).

---

## 6. 테스트 방법 (수동 확인 기준)

1. `.env`의 `VITE_PI_WS_URL`이 실제 Flask 서버(`ws://70.12.247.56:5000/ws/v1/kiosk/`)를 가리키는 상태에서 `npm run dev`
2. 메인 → 대여(또는 반납) → "준비되었습니다" 클릭 → 브라우저 콘솔에서 `KIOSK.SESSION.OPEN.REQUEST` 전송, `KIOSK.SESSION.OPEN.RESULT` 수신 확인
3. 이어서 `KIOSK.FACE_AUTH.REQUEST` 전송, ACK 수신 후 카메라 화면(`FACE_CAPTURE`) 전환 확인
4. 서버가 실제로 Event(`GUIDANCE`/`AUTH_PROCESSING`)를 보내면 `AUTH_PROCESSING` 수신 시 로딩 화면(`FACE_PROCESSING`)으로 전환되는지 확인
5. 서버가 `KIOSK.FACE_AUTH.RESULT`(`authenticated: true`)를 보내면 다음 화면(대여/반납 진행)으로 정상 진입하는지 확인
6. (서버 쪽에서 재현 가능하다면) `authenticated: false` 또는 각 Error Code별로 화면이 5절 매핑표대로 전환되는지 확인
7. "이전" 클릭 시 세션이 남아있는 채로 홈으로 돌아가는 것에 대한 서버 쪽 영향은 이번 범위에서 다루지 않으므로, 반복 테스트 시 서버 쪽에 세션이 계속 쌓이지 않는지 정도만 육안 확인(문제 있으면 8절 CANCEL 처리를 앞당겨야 함)
8. 이전에 겪었던 "Invalid frame header" 재현 여부 확인 — 정식 프로토콜로 보내면 서버가 다른 응답을 줄 가능성이 있음

이번엔 Mock이 아니라 실서버 연동이라 `npm run build`는 통과해야 하지만, 실제 동작 확인은 **서버가 켜져 있어야만 가능**하다 — 서버 준비 안 된 시점에는 코드 리뷰까지만 하고 실측은 서버 가동 후로 미룸.

---

## 7. 예상 리스크

- **12-I 문서 자체가 `REVIEW_REQUIRED · NOT_RELEASED`**: 지금 구현해도 서버 쪽 실제 구현이 이 스펙과 다를 수 있다(이미 관측된 `{"stage":"AUTH_STARTED"}` 필드명 불일치가 그 증거). 서버가 계속 스펙과 다르게 응답하면 이 PLAN대로 구현해도 안 될 수 있음 — 그 경우 실제 서버 로그/응답을 기준으로 스펙을 역으로 맞춰야 한다.
- **5절 에러 매핑이 정교하지 않음**: 기존 화면 2개(미검출/미매칭)에 7개 에러 코드를 욱여넣어서, 실제로는 다른 원인인데 같은 화면·문구가 뜰 수 있다.
- **`stationId` 여전히 임시값**: 실제 Station UUID가 확정 안 됨(`TEMP_STATION_ID` 패턴 유지) — 서버가 이 값을 검증하면(`STATION_MISMATCH`) 매번 실패할 수 있다.
- **15초 Terminal Timeout 동안 사용자 대기 경험**: 지금은 1.5~3초짜리 Mock 딜레이였는데, 실제로는 최대 15초까지 기다릴 수 있어 로딩 화면에 진행 안내가 부족하면 사용자가 멈춘 걸로 오해할 수 있음(기존 `LoadingScreen`의 연출용 진행률 카운트업이 그대로 쓰이긴 하지만 99%에서 15초까지 머물 수 있다는 뜻).
- **`faceAuthApi.ts` 삭제로 인한 회귀 가능성**: 혹시 다른 곳에서 이 Mock을 참조하고 있었다면 빌드 에러로 드러날 것(사전 grep 확인 필요, 구현 시 체크).

---

## 8. 이번 범위에서 하지 않을 일

- `KSK-SESSION-002`(세션 상태 조회) — 재연결·폴링 복구는 다음 단계.
- `KSK-RENT-001`/`KSK-RETURN-001`/`KSK-CANCEL-001` — 대여/반납 실행과 명시적 취소는 별도 PLAN.
- `KIOSK.FACE_AUTH.RESULT`의 `eligibility` 필드 소비 — 기존 Mock 자격 검증 그대로 유지.
- 세션 만료(`expiresAt`) 처리, 재시도 횟수 제한 — `DEC-SF-010` 등 여전히 `DECISION_REQUIRED`.
- 에러 코드-사용자 문구 정식 매핑 테이블 — 서버 쪽 `userMessageCode` 스펙 확정 전까지 5절의 임시 매핑으로 진행.
- WebSocket 재연결/하트비트 로직 — 연결이 끊기면 이번 범위에서는 그냥 실패 처리(GUIDE 복귀)로 끝낸다.

---

## 변경 이력

- 2026-07-30: 최초 작성, 승인 대기 중.
- 2026-07-30: 0절 추가 — 팀 채팅(@hyeee513)으로 받은 실서버 실제 프로토콜(flat `{stage,...}` JSON 스트림, Envelope 아님) 기록. 1~8절(Notion 12-I Envelope 기반 설계)과 충돌하며, 0절이 우선. 1~8절 재설계는 다음 승인 전 별도 작업 필요.

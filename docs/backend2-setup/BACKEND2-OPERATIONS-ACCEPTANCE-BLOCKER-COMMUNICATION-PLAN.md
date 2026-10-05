# Billisan Backend 2 운영 인수 Blocker 소통 및 의사결정 계획

## 문서 정보

| 항목 | 내용 |
| --- | --- |
| 문서명 | Billisan Backend 2 운영 인수 Blocker 소통 및 의사결정 계획 |
| 용도 | Notion 회의 준비, Slack 역할별 요청, Jira 작업 생성, MR Blocker 해소 근거, 최종 인수 감사 |
| 기준 상태 | `PARTIALLY_BLOCKED` |
| 기준 브랜치 | `S15P11A201-55-be-db` |
| 기준 검증 | 로컬 40/40, 전체 인수 40/44 |
| 작성 원칙 | 결정·구현·인수 증거 분리, 미확정 값 임의 구현 금지 |

이 문서는 API, 인증, 결제, AI 검색 규격을 대신 결정하지 않는다. 팀이
누구와 무엇을 논의하고 어떤 기록과 실행 증거를 남겨야 각 Blocker를
종료할 수 있는지 정의한다.

## 6.1 전체 요약

```text
현재 상태:
PARTIALLY_BLOCKED

전체 Blocker:
4개

즉시 회의 필요:
3개 — BLOCKER-01, BLOCKER-02, BLOCKER-03

비동기 확인 가능:
1개 — BLOCKER-04의 모델 명세·평가 자료 수집을 먼저 진행

기술 결정 필요:
4개

실물·외부 시스템 검증 필요:
2개 — BLOCKER-02 실물 Pi, BLOCKER-03 Toss Sandbox/선택된 외부 환경
```

로컬에서 이미 확인한 사실은 다음과 같다.

- JDK 21.0.11, Gradle Wrapper 9.5.1에서 6 suites, 57 tests가 실패·오류·skip
  없이 통과했다.
- MySQL 8.0 Flyway V1~V3, 9개 업무 테이블, 멱등성, 슬롯 동시성,
  Snapshot 조정, Seed와 조회가 검증됐다.
- PostgreSQL+pgvector Extension과 현재 `face_profile` 9개 컬럼이
  검증됐다.
- 운영 Controller와 공통 HTTP 계약, 실물 Pi 인증·동작, Toss 외부 승인,
  embedding 검색 규격은 검증되지 않았다.
- Mock·DB 테스트의 통과를 운영 Side Effect 성공으로 간주하지 않는다.

### 상태 판정 규칙

| 상태 | 적용 조건 |
| --- | --- |
| `CONFIRMED` | 결정 문서, 지정된 담당자·승인자, 구현, 인수 증거가 모두 존재 |
| `PROVISIONAL` | 팀 합의는 기록됐지만 구현 또는 인수 증거가 부족 |
| `BLOCKED` | 결정자·핵심 계약·실물 또는 외부 검증 중 하나 이상 부재 |
| `OUT_OF_SCOPE` | PM 또는 팀의 공식 MVP 제외 결정과 기록이 존재 |

네 Blocker는 현재 모두 `BLOCKED`다.

## 6.2 Blocker 소통 매트릭스

실제 이름은 PM이 지정한다. 별도 DBA Reviewer나 QA가 없다면 PM이 해당
책임을 맡을 팀원을 지정해야 하며, 작성자와 Reviewer는 가능하면 분리한다.

| ID | Blocker | Owner | Decision Maker | Reviewer | Contributor | 현재 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| BLOCKER-01 | 운영 Controller·HTTP 응답 계약 | Backend 3, Backend 2 | Backend 3 API 기술 책임 역할; 소유권 충돌은 PM | Frontend, QA/통합 테스트 담당 | Embedded, Backend 2, PM | `BLOCKED` |
| BLOCKER-02 | 실물 Pi 인증·명령 Side Effect | Embedded, Backend 2 | Embedded 장치 기술 책임 역할 + Backend 2 Gateway 기술 책임 역할 | QA/통합 테스트 담당 | Backend 1, Backend 3, PM | `BLOCKED` |
| BLOCKER-03 | Toss 외부 승인 Side Effect | Backend 3 | Backend 3 결제 기술 책임 역할; MVP 범위는 PM | 결제 Reviewer 또는 QA/통합 테스트 담당 | Backend 2, Frontend, PM | `BLOCKED` |
| BLOCKER-04 | embedding·vector 검색 규격 | Backend 1, Backend 2 | Backend 1 모델 책임 역할 + Backend 2 DB 책임 역할 | AI 모델 Reviewer, DBA Reviewer | PM | `BLOCKED` |

Owner는 구현과 증거 수집을 끝까지 추적한다. Decision Maker는 선택지를
확정하고 결정 기록을 승인한다. Reviewer는 결정과 결과를 독립적으로
검토한다. Contributor는 입력 자료와 연동 구현을 제공하되 최종 결정을
대신하지 않는다.

## 의존성 및 소통 순서 분석

### 의존 관계

```text
Controller 소유권·공통 HTTP 계약
├─ Pi 인증 실패, ACK, 물리 실패, Timeout 응답에 영향
├─ Toss 승인·재요청·복구 응답에 영향
└─ Frontend와 관리자 웹의 소비 DTO에 영향

Pi 인증·명령 계약
└─ Device Gateway Controller, commandId/eventId 오류·상태 매핑에 영향

Toss Side Effect 계약
└─ payment_attempt/settlement 상태 전이와 외부 식별자 UNIQUE 사용 방식에 영향

AI 모델 규격
└─ face_profile embedding 컬럼 규격, 거리 연산, index와 재등록 정책에 영향
```

BLOCKER-04는 운영 HTTP 계약에 직접 의존하지 않는다. 따라서 Backend 1의
모델 명세와 평가 자료 수집은 BLOCKER-01 회의와 동시에 시작한다.
BLOCKER-02와 BLOCKER-03도 인증 방식·MVP 범위의 사전 자료 수집은 즉시
가능하지만, 최종 응답 DTO와 오류 매핑은 BLOCKER-01 결정 뒤 확정한다.

### 권장 실행 순서

| 순서 | Blocker | 선행 결정 | 후속 작업 | 병렬 가능 여부 |
| --- | --- | --- | --- | --- |
| 0 | 공통 준비 | PM이 역할별 실제 담당자·승인자·Reviewer 지정 | Decision Task와 회의 초대 생성 | 다른 모든 작업의 관리 선행 조건 |
| 1 | BLOCKER-01 | 역할 지정 | Controller·DTO·Error 계약 구현과 테스트 | BLOCKER-04 자료 수집과 병렬 |
| 1-P | BLOCKER-04 | Backend 1 모델 Owner 지정 | 평가·benchmark 후 DB Migration 결정 | BLOCKER-01과 완전 병렬 가능 |
| 2-A | BLOCKER-02 | Device Gateway 소유권과 공통 응답 골격 | 인증·통신 구현, 실물 Pi 검증 | BLOCKER-03과 병렬 가능 |
| 2-B | BLOCKER-03 | 결제 API 소유권, PM의 MVP 범위 | Toss Client·복구 구현, Sandbox 검증 | BLOCKER-02와 병렬 가능 |
| 3 | 공통 인수 | 각 Decision Task `CONFIRMED`, 구현 완료 | 통합 테스트와 Acceptance Evidence 정리 | 네 Blocker 증거 수집 병렬 |
| 4 | 최종 감사 | Reviewer 승인 증거 | `PARTIALLY_BLOCKED` 재판정 | 모든 Blocker 종료 후 수행 |

## 6.3 Blocker별 소통 정리

### BLOCKER-01 — 운영 Controller·HTTP 응답 계약

#### 현재 확인된 사실

- Backend 2에는 MySQL 기반 application service, repository, 장치 계약
  fixture와 HTTP Mock이 있다.
- requestId, commandId, eventId, 결제 승인 식별자의 DB·service 멱등성은
  실제 MySQL에서 검증됐다.
- 슬롯 없음, 멱등 payload 충돌, Snapshot 오류를 표현하는 application
  exception과 결과는 존재한다.
- 운영 Controller, 공통 응답 Wrapper, 전역 예외 매퍼, 확정 OpenAPI는
  현재 저장소 근거로 확인되지 않는다.
- Frontend가 승인한 운영 Response DTO와 오류 형식은 없다.

#### 현재 확인되지 않은 사실

- 사용자·관리자·결제·Device Gateway Controller의 최종 소유자
- Device Gateway를 외부 장치용 Controller로 노출할지 내부 service
  경계로 유지할지
- 공통 응답 Wrapper와 성공·실패 body 규칙
- HTTP Status Code, Error Code, message와 field error 형식
- 인증·권한 실패와 장치 Timeout의 표현
- replay, payload conflict, 슬롯 경합과 관리자 확인 필요의 응답 규칙
- OpenAPI의 단일 소유 위치와 버전 변경 절차

#### Endpoint·Controller 소유권 워크시트

경로와 Owner가 승인되기 전에는 `TBD`를 실제 endpoint처럼 사용하지 않는다.

| API 경계 | 현재 근거 | Endpoint/Operation | Controller Owner | Service/Data Owner | Consumer | 상태 |
| --- | --- | --- | --- | --- | --- | --- |
| 사용자 대여·반납 | Backend 2 DB·멱등성 core만 확인 | `TBD` | `TBD` | `TBD` | Frontend | `BLOCKED` |
| 관리자 재고·슬롯 조회 | Backend 2 Repository projection 확인 | `TBD` | `TBD` | `TBD` | 관리자 Frontend | `BLOCKED` |
| 관리자 복구 승인 | Snapshot 안전 상태만 확인 | `TBD` | `TBD` | `TBD` | 관리자 Frontend | `BLOCKED` |
| 장치 Heartbeat | HTTP fixture 존재 | `TBD` | `TBD` | `TBD` | Embedded | `BLOCKED` |
| 장치 센서·잠금 결과 | HTTP fixture와 eventId core 존재 | `TBD` | `TBD` | `TBD` | Embedded | `BLOCKED` |
| Pi 부팅 Snapshot | 계약 fixture와 조정 service 존재 | `TBD` | `TBD` | `TBD` | Embedded | `BLOCKED` |
| AI 인증·파손 metadata | HTTP fixture 존재 | `TBD` | `TBD` | `TBD` | Backend 1/Embedded | `BLOCKED` |
| 결제 승인·상태 조회 | DB 제약·멱등성 core만 확인 | `TBD` | `TBD` | `TBD` | Frontend | `BLOCKED` |

#### 관련 역할

| 구분 | 역할 | 책임 |
| --- | --- | --- |
| Owner | Backend 3 | 사용자·관리자·결제 등 client-facing API 계약 작성 |
| Owner | Backend 2 | Device Gateway 및 데이터 정합성 service 계약 작성 |
| Decision Maker | Backend 3 API 기술 책임 역할 | 공통 응답·오류 규칙 최종 승인 |
| Decision Support | PM | Controller 소유권 충돌과 MVP 범위 조정 |
| Reviewer | Frontend | 소비 가능한 DTO·오류 처리 승인 |
| Reviewer | QA/통합 테스트 담당 | 명세와 실행 결과 일치 검증 |
| Contributor | Embedded | 장치 ACK·실패·Timeout에 필요한 정보 제공 |

#### 팀에 확인할 핵심 질문

1. 사용자 앱과 관리자 웹이 호출하는 Controller를 Backend 3이 소유하고
   Backend 2가 service/repository 계약을 제공한다면, 각 endpoint의
   책임·변경 승인·장애 대응 경계를 어떻게 나눌 것인가?
2. Pi가 직접 호출하는 Device Gateway endpoint를 Backend 2가 소유한다면
   Backend 3의 인증·공통 응답 모듈을 어떻게 공유하고 버전 충돌을
   방지할 것인가?
3. 공통 Wrapper를 사용한다면 성공·실패·field validation·trace ID를 어떤
   필드로 표현하고, stream 또는 빈 body 응답에도 동일하게 적용할 것인가?
4. 기존 결과를 재사용하는 멱등 replay와 같은 ID의 다른 payload 충돌을
   클라이언트가 어떻게 구분하고 어떤 후속 행동을 해야 하는가?
5. 슬롯 후보 없음, 순간적인 동시성 경합, Pi Timeout, 물리 잠금 실패,
   Snapshot 조정 필요를 각각 재시도 가능·불가능·관리자 조치 필요로 어떻게
   분류할 것인가?
6. 존재하지 않는 stationId/slotId와 인증된 장치가 소유하지 않은
   stationId/slotId를 같은 오류로 볼지, 보안과 운영 관점에서 어떻게
   구분할 것인가?
7. Error Code를 어느 역할이 생성·승인하고, Frontend와 Embedded의
   switch/case 호환성을 어떤 문서와 테스트로 보호할 것인가?
8. OpenAPI를 어느 저장소·파일에서 단일 진실 공급원으로 관리하고,
   breaking change를 누가 승인할 것인가?

#### 결정해야 할 항목

| Decision ID | 결정 항목 | 선택지 | 권장안 | 최종 결정자 |
| --- | --- | --- | --- | --- |
| API-01 | Controller 소유권 | client-facing/edge 분리, 기능별 분리, 단일 Backend 소유 | 역할표에 맞춰 client-facing은 Backend 3, device-facing은 Backend 2로 분리하는 안을 `PROVISIONAL` 검토 | Backend 3 API 기술 책임 역할, 충돌 시 PM |
| API-02 | Device Gateway 경계 | 외부 Controller, 내부 service, 별도 gateway 배포 | 현재 Pi→Spring 계약상 외부 수신 경계는 필요하지만 배포 형태는 권장안 제시 불가 | Backend 2·Embedded 기술 책임 역할 |
| API-03 | 공통 응답 형식 | 기존 팀 Wrapper, 표준 오류 문서, endpoint별 body | 기존 전사·팀 규칙 확인 전 권장안 제시 불가 | Backend 3 API 기술 책임 역할 |
| API-04 | HTTP Status·Error Code | 공통 catalog, bounded-context catalog | 공통 분류와 도메인 세부 code의 2단 구성을 검토하되 실제 code/value는 미확정 | Backend 3, Frontend Reviewer |
| API-05 | 중복·충돌·Timeout 정책 | 기존 결과 응답, 별도 replay 표시, 충돌 응답, 비동기 상태 조회 | service의 `APPLIED`/`REPLAYED`/conflict 의미는 보존; HTTP 매핑은 권장안 제시 불가 | Backend 3·Backend 2 |
| API-06 | API 명세 공급원 | OpenAPI code-first, spec-first, Notion 표만 사용 | 실행 가능한 OpenAPI 한 곳을 단일 기준으로 하고 Notion은 링크하는 안 권장 | Backend 3 API 기술 책임 역할 |

#### 주요 실패 상황 결정표

값을 채우기 전에는 모두 `BLOCKED`다. Status Code와 Error Code는 이 문서가
확정하지 않는다.

| 상황 | 업무 결과 | 재시도 분류 | HTTP Status | Error Code | Response DTO Owner |
| --- | --- | --- | --- | --- | --- |
| 같은 requestId·같은 payload | 기존 결과 replay | 팀 결정 필요 | `TBD` | `TBD` | `TBD` |
| 같은 requestId·다른 payload | 멱등 충돌 | 수정 후 재요청 여부 결정 | `TBD` | `TBD` | `TBD` |
| 같은 commandId 재요청 | 저장된 명령 결과 replay | 팀 결정 필요 | `TBD` | `TBD` | `TBD` |
| 사용 가능한 슬롯 없음 | 업무 미생성 | 즉시/지연 재시도 결정 | `TBD` | `TBD` | `TBD` |
| 슬롯 동시성 충돌 | 한 요청만 성공 | 재조회·재시도 결정 | `TBD` | `TBD` | `TBD` |
| stationId/slotId 없음 | 대상 없음 | 보통 요청 수정, 최종 정책 결정 | `TBD` | `TBD` | `TBD` |
| Pi 응답 Timeout | 물리 결과 미확정 | 상태 조회·보상 정책 결정 | `TBD` | `TBD` | `TBD` |
| Pi 잠금 실패 | 물리 실패 | 장치/관리자 정책 결정 | `TBD` | `TBD` | `TBD` |
| 센서 상태 불일치 | 자동 정상화 금지 | 관리자 조정 필요 | `TBD` | `TBD` | `TBD` |
| Snapshot 조정 필요 | 안전 상태 전환 | 관리자 조정 필요 | `TBD` | `TBD` | `TBD` |
| 관리자 확인 필요 | 업무 자동 완료 금지 | 승인 API 필요 | `TBD` | `TBD` | `TBD` |

#### 필요한 구현 작업

| 작업 | 담당 | 선행 조건 | 산출물 |
| --- | --- | --- | --- |
| Controller 소유 경계 반영 | Backend 2·Backend 3 | API-01, API-02 | Controller 목록과 모듈/패키지 경계 |
| Request/Response DTO와 Validation | 각 Controller Owner | API-03~05 | DTO 코드, validation 규칙 |
| 예외·오류 매핑 | Backend 3, Backend 2 | Error catalog 승인 | 예외 handler와 error response |
| OpenAPI 반영 | 각 Controller Owner | DTO·오류 확정 | versioned OpenAPI |
| Frontend 소비 반영 | Frontend | OpenAPI 승인 | client model과 오류 처리 |
| Controller 통합 테스트 | 각 Owner, QA | 구현 완료 | MockMvc 또는 실제 HTTP 통합 테스트 |

#### 필요한 인수 증거

| 증거 | 생성 담당 | 검토 담당 | 완료 기준 |
| --- | --- | --- | --- |
| Controller 소유권 표 | PM, Backend 2·3 | 전체 관련 역할 | 모든 endpoint에 Owner·Reviewer 지정 |
| Endpoint/DTO/Error 표 | Backend 2·3 | Frontend, Embedded | TBD가 없고 승인 기록 존재 |
| OpenAPI | Controller Owner | Frontend, QA | 구현과 schema diff가 일치 |
| 주요 실패 HTTP 로그 | QA/통합 테스트 담당 | Backend 2·3 | 표의 각 시나리오가 확정 응답과 일치 |
| Controller 통합 테스트 결과 | Controller Owner | QA | 최소 운영 Controller 1개 이상, 성공·validation·중복·충돌 검증 |

#### 완료 정의

1. 모든 운영 endpoint에 Controller Owner와 Reviewer가 지정된다.
2. 공통 성공·실패 DTO, Status Code, Error Code와 validation 형식이 승인된다.
3. 중복·충돌·Timeout·관리자 확인 응답 정책이 명세에 존재한다.
4. Frontend와 Embedded가 각자 소비하는 DTO를 승인한다.
5. OpenAPI와 구현이 일치한다.
6. 운영 Controller 통합 테스트와 HTTP 증거가 존재한다.

이 중 결정만 끝났으면 `PROVISIONAL`, 구현과 증거까지 끝나야
`CONFIRMED`다.

#### 미결정 시 위험

- 같은 service 결과가 Controller마다 다른 HTTP 의미로 노출된다.
- Frontend가 replay를 실패로 오인하거나 충돌을 무한 재시도할 수 있다.
- Pi ACK, 물리 실패, Timeout이 하나의 성공/실패로 합쳐질 수 있다.
- Error Code 변경이 client 배포 후 발견될 수 있다.
- Controller 소유권 중복으로 동일 endpoint가 두 번 구현될 수 있다.

#### 목표 결정 기한

`PM이 Sprint 일정 기준으로 지정 필요`

### BLOCKER-02 — 실물 Pi 인증·명령 Side Effect

#### 현재 확인된 사실

- Pi→Spring 장치·AI 메타데이터용 HTTP Mock과 fixture가 존재한다.
- requestId, commandId, eventId, bootId의 용도가 분리돼 있다.
- Spring DB에서는 같은 commandId/eventId의 중복 Side Effect가 한 번만
  반영됨을 MySQL 동시 테스트로 확인했다.
- Snapshot 불일치는 자동 정상화하지 않고 `ADMIN_REVIEW` 또는
  `RECOVERY_REQUIRED`로 전환한다.
- 실물 Pi, 운영 장치 credential, 물리 잠금, 센서 신호와 실제 네트워크는
  검증하지 않았다.

#### 현재 확인되지 않은 사실

- 장치 identity, 인증·서명 방식, credential 발급·저장·회전·폐기
- stationId와 deviceId의 프로비저닝 및 소유권 검증
- Pi→Spring 및 Spring→Pi의 최종 protocol과 네트워크 경계
- Timeout, Retry, backoff와 offline 복구 정책
- Pi 측 commandId 중복 처리와 재부팅 후 기억 범위
- ACK와 물리 성공·실패의 정확한 시점과 payload
- 센서 debounce, 장치 오류 code, 실물 증거 수집 방식

#### 관련 역할

| 구분 | 역할 | 책임 |
| --- | --- | --- |
| Owner | Embedded | Pi 인증 정보, firmware protocol, 물리 동작·로그 구현 |
| Owner | Backend 2 | Device Gateway, command/event 멱등성, MySQL 상태 반영 |
| Decision Maker | Embedded·Backend 2 기술 책임 역할 | 인증·protocol·상태 전이 공동 승인 |
| Decision Support | PM | 장비·네트워크·일정과 역할 충돌 조정 |
| Reviewer | QA/통합 테스트 담당 | 실물 성공·실패·재전송 증거 검토 |
| Contributor | Backend 1 | AI 결과 전달 경계와 Jetson/Pi 연결 정보 |
| Contributor | Backend 3 | 외부 API 응답과 관리자 복구 흐름 연결 |

#### 팀에 확인할 핵심 질문

1. 각 Pi를 어떤 identity로 등록하고 stationId와 deviceId의 관계를 누가
   생성·변경·폐기하며, 잘못된 매핑 요청을 어떤 운영 절차로 조사할 것인가?
2. API Key, JWT, HMAC, mTLS 또는 사설망 조합 중 어떤 위협 모델과 장치
   성능·배포 제약을 근거로 인증 방식을 선택할 것인가?
3. credential 발급 주체, Pi 저장 위치, 로그 마스킹, 만료·회전, 분실 장치
   폐기와 재부팅 후 복구 절차를 어떻게 정의할 것인가?
4. replay 공격을 막기 위해 timestamp, nonce, 서명 범위 또는 토큰 만료를
   어떻게 사용할 것이며 시간 오차와 offline 장치 복구를 어떻게 처리할
   것인가?
5. Pi→Spring과 Spring→Pi에 HTTP, MQTT, WebSocket 중 어떤 protocol을
   선택하고, 연결 단절·NAT·사설망·메시지 순서 제약을 어떻게 해결할 것인가?
6. commandId가 같은 명령을 Pi가 다시 받았을 때 실행 중·완료·실패·기록
   유실 상태마다 어떤 결과를 반환하고 물리 동작 재실행을 어떻게 막을 것인가?
7. Command Accepted와 Physical Success 사이에 어떤 센서 확인이 필요하며,
   HTTP 성공을 물리 성공으로 오인하지 않도록 상태와 시간을 어떻게
   표현할 것인가?
8. Timeout 뒤 Spring이 명령을 재전송하기 전에 Pi의 현재 상태를 조회할지,
   관리자 확인으로 보낼지, 각 동작별 복구 정책을 어떻게 다르게 둘 것인가?
9. 재부팅 Snapshot의 full/partial 규칙, slot count 또는 schema version을
   어떻게 정의하고 일부 센서 장애를 대여소 전체 장애와 구분할 것인가?

#### 결정해야 할 항목

| Decision ID | 결정 항목 | 선택지 | 권장안 | 최종 결정자 |
| --- | --- | --- | --- | --- |
| PI-01 | 장치 identity·인증 | API Key, JWT, HMAC, mTLS, 조합 | 위협 모델·네트워크·secure storage 근거 전 권장안 제시 불가 | Embedded·Backend 2 |
| PI-02 | 통신 protocol | HTTP, MQTT, WebSocket, 혼합 | 실제 네트워크와 firmware 제약 전 권장안 제시 불가 | Embedded·Backend 2 |
| PI-03 | replay 방어 | timestamp, nonce, signature, token expiry | stationId만 신뢰하거나 사설망만으로 인증을 대체하지 않는 원칙 권장; 구체 방식 미확정 | Embedded·Backend 2 |
| PI-04 | Timeout·Retry | 동작별 timeout, 고정/지수 backoff, 상태 조회 우선 | 실물 동작 시간 측정 전 횟수·초 단위 권장안 제시 불가 | Embedded |
| PI-05 | 명령 상태 | accepted와 physical result 분리/통합 | 반드시 `Command Accepted`와 `Physical Success/Failure` 분리 | Embedded·Backend 2 |
| PI-06 | credential 수명 | 수동 발급, 자동 발급, 장기/단기 credential | Git·일반 로그 저장 금지와 회전·폐기 절차 필수; 저장 기술은 미확정 | Backend 2·Embedded |
| PI-07 | Snapshot 완전성 | full-only, partial 허용, schema version | 현재 안전 정책과 맞게 불완전 payload를 자동 정상으로 보지 않는 원칙 유지 | Embedded·Backend 2 |

#### 실물 Side Effect 동작별 워크시트

Embedded와 Backend 2가 동작별로 채운다. `Pi 수신/시작`은 ACK이고
`물리 성공 신호`와 동일하지 않다.

| 동작 | 명령·이벤트 식별자 | Pi 수신·물리 시작 | 물리 성공 신호 | 물리 실패·오류 | 결과 응답 | Spring 상태 반영 | Retry·최종 실패·관리자 전환 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 잠금 해제 | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` |
| 잠금 실행 | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` |
| 우산 제거 감지 | `TBD` | 해당 없음/`TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` |
| 우산 삽입 감지 | `TBD` | 해당 없음/`TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` |
| 센서 상태 조회 | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` | `TBD` |
| Heartbeat | `TBD` | 해당 없음/`TBD` | 장치 생존 신호 `TBD` | `TBD` | `TBD` | `TBD` | `TBD` |
| 부팅 Snapshot | bootId 사용 근거 존재 | 측정 시작 `TBD` | full 측정 완료 `TBD` | partial/센서 오류 `TBD` | `TBD` | 일치 유지·불일치 안전 전환 core 존재 | `TBD` |
| 장치 오류 보고 | `TBD` | 해당 없음/`TBD` | 해당 없음 | 오류 code `TBD` | `TBD` | `TBD` | `TBD` |

#### 필요한 구현 작업

| 작업 | 담당 | 선행 조건 | 산출물 |
| --- | --- | --- | --- |
| 장치 등록·프로비저닝 | Embedded, Backend 2 | PI-01, PI-06 | deviceId↔stationId 등록·폐기 흐름 |
| 요청 인증·replay 검증 | Backend 2, Embedded | PI-01, PI-03 | 인증 filter/client, clock/nonce 처리 |
| Pi–Spring transport | Embedded, Backend 2 | PI-02 | 양방향 client/server 또는 broker 설정 |
| command 상태 처리 | Embedded, Backend 2 | PI-04, PI-05 | accepted/physical result DTO와 상태 전이 |
| 장치 오류·Snapshot 처리 | Embedded, Backend 2 | PI-07, Error catalog | 오류 code, full Snapshot, recovery 연결 |
| credential·장치 로그 마스킹 | Embedded, Backend 2 | PI-06 | 보안 설정과 검사 테스트 |

#### 필요한 인수 증거

| 증거 | 생성 담당 | 검토 담당 | 완료 기준 |
| --- | --- | --- | --- |
| 인증 결정서와 위협 모델 | Backend 2·Embedded | PM, Reviewer | identity·발급·검증·회전·폐기 전 과정 기록 |
| protocol·Timeout·Retry 계약 | Embedded | Backend 2, QA | 동작별 값과 재부팅·offline 동작 명시 |
| commandId 실물 중복 로그 | Embedded | Backend 2, QA | 같은 commandId 2회에 물리 동작 1회 |
| eventId 중복 통합 로그 | Backend 2 | Embedded, QA | 같은 eventId 2회에 DB 전이 1회 |
| 잠금 성공·실패 영상/센서 로그 | Embedded | QA | accepted와 physical 결과 시간·상태 구분 |
| 실물 부팅 Snapshot 로그 | Embedded, Backend 2 | QA | 일치 유지, 불일치 안전 전환, 중복 no-op |
| Secret·로그 검사 | Backend 2·Embedded | Reviewer | credential, raw secret, 금지 payload 노출 0 |

#### 완료 정의

1. 인증 방식과 장치 identity 생명주기가 승인된다.
2. 양방향 protocol, Timeout, Retry와 offline 정책이 승인된다.
3. accepted와 physical success/failure가 계약·구현·로그에서 구분된다.
4. 같은 commandId의 실물 동작은 한 번만 발생한다.
5. 같은 eventId의 Spring 상태 반영은 한 번만 발생한다.
6. 실물 잠금 성공·실패와 Snapshot 일치·불일치 증거가 존재한다.
7. credential과 장치 로그 보안 검사가 통과한다.

결정·Mock만 있으면 `PROVISIONAL`, 실물 증거까지 있어야 `CONFIRMED`다.

#### 미결정 시 위험

- stationId 위조나 탈취 장치가 정상 Pi로 처리될 수 있다.
- Timeout 재전송으로 잠금장치가 두 번 작동할 수 있다.
- ACK를 물리 성공으로 처리해 DB와 실제 잠금 상태가 달라질 수 있다.
- Pi 재부팅 후 commandId 기억이 사라져 중복 실행될 수 있다.
- 불완전 Snapshot을 정상으로 처리하거나 전체 대여소를 불필요하게 막을 수 있다.

#### 목표 결정 기한

`PM이 Sprint 일정 기준으로 지정 필요`

### BLOCKER-03 — Toss 외부 승인 Side Effect

#### 현재 확인된 사실

- MySQL `payment_attempt`에는 주문·승인 식별자 UNIQUE와 상태 제약이 있다.
- 연결된 `settlement`와 payment 행을 잠그는 MySQL 멱등성 service 테스트가
  통과했다.
- 같은 승인 식별자의 순차·동시 DB 반영은 한 번만 수행된다.
- Toss Client, Sandbox 승인 호출, webhook, 운영 Controller는 현재 검증
  범위에 없다.
- 실제 Secret, 카드·개인 결제정보를 저장하거나 로그로 출력하지 않았다.

#### 현재 확인되지 않은 사실

- MVP의 Toss 사용 여부와 Sandbox/실결제 범위
- 승인, 취소, 환불, 부분 취소와 관리자 수동 청구 범위
- 실제 과금 사유와 과금 확정 주체
- orderId 생성·재사용 규칙과 transactionKey/webhook 사용 여부
- Toss 성공과 DB Commit 순서, 응답 유실·DB 실패 복구
- webhook 검증, 중복 수신과 순서 역전 처리
- Secret 저장소, key rotation, paymentKey 등 로그 마스킹 기준

#### 관련 역할

| 구분 | 역할 | 책임 |
| --- | --- | --- |
| Owner | Backend 3 | 결제 orchestration, Toss Client, Controller, 상태 전이 |
| Decision Maker | Backend 3 결제 기술 책임 역할 | 승인·취소·복구 기술 계약 |
| Decision Maker | PM | MVP 과금 사유와 Sandbox/실결제 범위 |
| Reviewer | 결제 Reviewer 또는 QA | 외부 Side Effect와 DB 정합성 증거 검토 |
| Contributor | Backend 2 | Migration, UNIQUE, transaction·repository 경계 |
| Contributor | Frontend | 결제창·버튼 재요청·Timeout UX |

#### 팀에 확인할 핵심 질문

1. MVP에서 연체·분실·파손·미반납·관리자 청구 중 어떤 사유를 자동 또는
   수동 결제로 포함하고, 제외 항목을 어떤 공식 결정으로 남길 것인가?
2. Sandbox만 인수 범위로 볼지 실결제까지 포함할지, 승인·취소·환불·부분
   취소 중 어느 Side Effect를 이번 Sprint에 검증할 것인가?
3. settlement가 청구 근거이고 payment_attempt가 외부 시도라면 여러
   payment attempt 허용, 최종 PAID 전이, 실패 후 재시도 관계를 어떻게
   정의할 것인가?
4. orderId를 누가 언제 생성하고 Frontend 재클릭·Backend 재시도에서
   기존 값을 재사용할지, 새로운 결제 시도와 어떻게 구분할 것인가?
5. Toss에서 승인은 성공했지만 Backend가 응답을 받지 못한 경우 어떤
   authoritative 조회와 식별자로 상태를 복구한 뒤 사용자에게 응답할 것인가?
6. 외부 승인 성공 뒤 DB Commit이 실패하면 자동 재조회·보상·관리자 확인
   중 어떤 절차를 적용하고 중복 승인 없이 어떻게 재실행할 것인가?
7. webhook을 사용한다면 서명 또는 출처 검증, 중복·순서 역전·지연 이벤트,
   기존 요청 응답과의 경쟁을 어떻게 처리할 것인가?
8. Secret Key와 paymentKey·transactionKey를 어느 저장소에 보관하고
   application/HTTP/audit 로그에서 어떤 마스킹 규칙으로 검증할 것인가?

#### 결정해야 할 항목

| Decision ID | 결정 항목 | 선택지 | 권장안 | 최종 결정자 |
| --- | --- | --- | --- | --- |
| PAY-01 | MVP 결제 범위 | 제외, Sandbox 승인, 실결제, 취소/환불 포함 | 제품 범위 근거 없으므로 권장안 제시 불가 | PM |
| PAY-02 | 과금 사유 | 연체, 분실, 파손, 미반납, 관리자 청구 조합 | 정책·법무·기획 근거 없으므로 권장안 제시 불가 | PM, Backend 3 |
| PAY-03 | 결제 상태 Owner | Backend 3 orchestration, 별도 결제 service | 역할표상 Backend 3 소유를 `PROVISIONAL` 권장, Backend 2는 DB 제약 Reviewer | Backend 3 |
| PAY-04 | PAYMENT 상태 전이 | 현재 enum 유지/확장, webhook 중심/동기 응답 중심 | 승인·취소 범위 전 권장안 제시 불가 | Backend 3 |
| PAY-05 | 외부 식별자 규칙 | orderId 재사용, 시도별 신규 ID, transactionKey 저장 | 기존 orderId/paymentKey UNIQUE 유지; 생성·수명은 미확정 | Backend 3, Backend 2 Reviewer |
| PAY-06 | 응답 유실·DB 실패 복구 | 외부 조회 후 조정, 보상 처리, 관리자 확인 | 외부 상태를 확인하지 않은 blind 재승인은 금지; 구체 API 흐름 미확정 | Backend 3 |
| PAY-07 | webhook | 미사용, 사용, 일부 이벤트만 사용 | MVP 범위와 공식 Toss 연동 설계 전 권장안 제시 불가 | Backend 3 |
| PAY-08 | Secret·로그 | 환경 변수, 배포 Secret 저장소, 별도 vault | Git·평문 로그 금지와 식별자 마스킹 필수; 저장 기술은 배포 환경 결정 | Backend 3, PM |

#### 결제 관계·실패 지점 워크시트

현재 물리 스키마에서 `payment_attempt.settlement_id`는 FK이고 UNIQUE가
아니므로 한 settlement에 여러 payment attempt를 저장할 수 있다.
이 구조가 공식 재시도 정책이라는 뜻은 아니며 Backend 3과 PM의 승인이
필요하다.

| 지점 | 현재 DB 근거 | 업무 규칙·Owner | 실패 시 복구 | 응답·Webhook 경쟁 | 상태 |
| --- | --- | --- | --- | --- | --- |
| settlement 생성 | rental/return attempt 연결 제약 존재 | `TBD` | `TBD` | 해당 없음 | `BLOCKED` |
| payment attempt 생성 | orderId UNIQUE 존재 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| Toss 승인 요청 | 외부 Client 없음 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| Toss 승인 성공 | paymentKey UNIQUE·DB core 존재 | `TBD` | DB 실패 조정 `TBD` | 동기 응답/webhook 우선순위 `TBD` | `BLOCKED` |
| 승인 응답 유실 | 검증 증거 없음 | `TBD` | 외부 상태 조회 방식 `TBD` | `TBD` | `BLOCKED` |
| 승인 실패·재시도 | DB 상태 후보만 존재 | `TBD` | 새/기존 attempt 규칙 `TBD` | `TBD` | `BLOCKED` |
| 취소·환불·부분 취소 | 범위 미확정 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| 관리자 재처리 | 범위 미확정 | `TBD` | `TBD` | `TBD` | `BLOCKED` |

#### 필요한 구현 작업

| 작업 | 담당 | 선행 조건 | 산출물 |
| --- | --- | --- | --- |
| 결제 API·orchestration | Backend 3 | PAY-01~05 | Controller, service, Toss Client |
| 상태·식별자 repository 연결 | Backend 3, Backend 2 Review | PAY-04~06 | transaction 경계, query, 필요 Migration |
| 응답 유실·보상 처리 | Backend 3 | PAY-06 | 외부 상태 조회·복구 흐름 |
| webhook 검증·멱등성 | Backend 3 | PAY-07 | webhook endpoint, 검증, 중복 처리 |
| Frontend 재요청 UX | Frontend | API 계약 | 중복 클릭 방지와 결과 재조회 |
| Secret·로그 마스킹 | Backend 3 | PAY-08 | 배포 설정과 자동 검사 |

#### 필요한 인수 증거

| 증거 | 생성 담당 | 검토 담당 | 완료 기준 |
| --- | --- | --- | --- |
| 결제 범위 결정서 | PM, Backend 3 | 관련 역할 | 승인·취소·환불·과금 사유가 포함/제외로 명시 |
| Toss sequence·상태 전이도 | Backend 3 | Backend 2, Reviewer | 외부 호출과 DB Commit·복구 순서 명시 |
| Sandbox 승인 로그 | Backend 3 | QA/결제 Reviewer | 선택된 범위의 실제 외부 Side Effect 식별 가능 |
| 중복 승인 방지 테스트 | Backend 3 | Backend 2, QA | 연속 클릭·Timeout·재요청·중복 webhook에서 승인 1회 |
| 응답 유실/DB 실패 복구 테스트 | Backend 3 | QA | 외부 상태와 DB가 최종 일치 |
| Secret·로그 검사 | Backend 3 | Reviewer | Secret·카드정보 원문 0, 식별자 마스킹 기준 통과 |

#### 완료 정의

1. PM이 MVP 과금 사유와 승인·취소·환불 범위를 공식 기록한다.
2. payment/settlement 상태와 외부 식별자 수명이 승인된다.
3. Toss Client와 DB transaction·복구 흐름이 구현된다.
4. 동일 결제의 연속 클릭·재요청·응답 유실·중복 webhook에서 외부 승인이
   한 번만 발생한다.
5. 선택한 범위의 Sandbox 또는 공식 인수 환경 Side Effect 로그가 존재한다.
6. Secret·결제정보·식별자 로그 정책이 검증된다.

MVP에서 제외한다면 PM의 공식 `OUT_OF_SCOPE` 결정이 있어야 한다. 단순히
외부 연동이 어렵다는 이유로 종료할 수 없다.

#### 미결정 시 위험

- DB 행은 한 번 갱신돼도 Toss 승인이 두 번 호출될 수 있다.
- Toss 승인 성공과 DB 실패 사이에서 사용자는 결제됐지만 미납으로 보일 수 있다.
- orderId 재사용 규칙 불일치로 정상 재요청이 충돌할 수 있다.
- webhook과 동기 응답이 경쟁해 상태가 역전될 수 있다.
- Secret 또는 결제 식별자가 로그와 MR에 노출될 수 있다.

#### 목표 결정 기한

`PM이 Sprint 일정 기준으로 지정 필요`

### BLOCKER-04 — embedding 차원·거리 함수·vector index 규격

#### 현재 확인된 사실

- PostgreSQL 17과 pgvector Extension이 로컬에서 확인됐다.
- `face_profile.embedding`은 차원을 강제하지 않는 `vector`로 생성돼 있다.
- `user_ref UUID`, 등록 상태, model/template version과 시간 컬럼이 존재한다.
- Spring은 PostgreSQL에 직접 연결하지 않으며 Cross-DB FK도 없다.
- 이름·학번·이메일·얼굴 이미지 경로는 PostgreSQL에 없다.
- embedding 차원, 정규화, 거리 함수, threshold와 index는 확정되지 않았다.

#### 현재 확인되지 않은 사실

- 얼굴 embedding 모델·정확한 버전·입력 전처리
- 출력 차원, dtype, L2 normalization 여부
- 동일인/타인 거리 분포와 threshold 산정 자료
- Cosine, Inner Product, Euclidean 중 모델에 맞는 거리 함수
- 예상 프로필 수, 검색 빈도, latency·정확도·메모리 목표
- HNSW/IVFFlat/무인덱스 선택과 파라미터
- 모델 변경 시 구 embedding 재사용·재등록·병행 검색 정책

#### 관련 역할

| 구분 | 역할 | 책임 |
| --- | --- | --- |
| Owner | Backend 1 | 모델 명세, 전처리·정규화, 평가와 threshold 근거 |
| Owner | Backend 2 | PostgreSQL 물리 규격, Migration, query와 benchmark |
| Decision Maker | Backend 1 모델 책임 역할 | 모델·차원·정규화·거리·threshold 승인 |
| Decision Maker | Backend 2 DB 책임 역할 | 컬럼·index·Migration·운영 기준 승인 |
| Reviewer | AI 모델 Reviewer | 모델 평가와 동일인/타인 결과 검토 |
| Reviewer | DBA Reviewer | pgvector DDL, index, EXPLAIN, rollback 검토 |
| Decision Support | PM | MVP 데이터 규모와 latency 목표 확정 |

#### 팀에 확인할 핵심 질문

1. 얼굴 embedding을 생성하는 정확한 모델·버전·전처리 pipeline은 무엇이며
   같은 입력에서 재현 가능한 명세와 checksum 또는 배포 artifact를 어떻게
   식별할 것인가?
2. 출력 차원과 dtype은 무엇이고 L2 normalization을 모델 내부·Backend 1
   코드·DB query 중 어디에서 적용하며 중복 또는 누락을 어떻게 검증할 것인가?
3. 선택할 거리 함수가 모델 학습·정규화 특성과 어떻게 연결되고, 다른
   연산자를 사용했을 때 순위와 threshold가 어떻게 달라지는가?
4. 인증 threshold를 어떤 비개인 검증 dataset과 동일인/타인 분포,
   false accept/false reject 목표로 산정하고 버전별로 어떻게 보관할 것인가?
5. MVP 예상 프로필 수, 검색 QPS, 등록·갱신 빈도, Jetson/DB 메모리와
   latency 목표가 얼마이며 exact scan으로 목표를 만족하는가?
6. index가 필요하다면 HNSW와 IVFFlat을 정확도·build 시간·메모리·운영
   복잡도 기준으로 어떻게 비교하고 어떤 EXPLAIN·benchmark로 승인할 것인가?
7. 모델 차원이나 전처리가 바뀔 때 구·신 embedding을 혼합 검색할지,
   별도 template version으로 격리할지, 전체 재등록할지 어떻게 결정할 것인가?
8. `VECTOR(n)` 제약을 언제 새 Migration으로 적용하고 기존 차원 불일치
   데이터의 검출·격리·rollback을 어떻게 수행할 것인가?

#### 결정해야 할 항목

| Decision ID | 결정 항목 | 선택지 | 권장안 | 최종 결정자 |
| --- | --- | --- | --- | --- |
| VEC-01 | 모델·버전·전처리 | Backend 1 후보 모델과 pipeline | 모델 명세 없이는 권장안 제시 불가 | Backend 1 모델 책임 역할 |
| VEC-02 | 차원·dtype·정규화 | 모델 출력값 기준 | 측정·코드 근거 없이는 숫자나 방식 권장 불가 | Backend 1 |
| VEC-03 | 거리 함수·threshold | Cosine, Inner Product, Euclidean | 정규화와 평가 결과 전 권장안 제시 불가 | Backend 1, AI Reviewer |
| VEC-04 | `VECTOR(n)` 적용 | 현재 무차원 유지, 차원 제약 Migration | 차원·호환 정책 확정 전 현재 무차원 유지; 확정 뒤 기존 SQL 수정 대신 새 Migration | Backend 2, DBA Reviewer |
| VEC-05 | vector index | 없음, HNSW, IVFFlat | 데이터 규모·latency benchmark 전에는 index 없음 유지 | Backend 2, DBA Reviewer |
| VEC-06 | 모델 버전 공존 | 전면 재등록, version별 격리, 제한적 병행 | 검색 정확도·운영비 근거 전 권장안 제시 불가 | Backend 1·2 |
| VEC-07 | metadata 규격 | model/template/threshold/dimension/metric version | 재현에 필요한 최소 metadata를 결정하되 컬럼 추가는 계약 후 | Backend 1·2 |

#### 모델·DB metadata 워크시트

| 논리 metadata | 현재 저장 상태 | Backend 1 제공 값·근거 | DB 저장 필요 여부 | 버전 호환 규칙 | 상태 |
| --- | --- | --- | --- | --- | --- |
| `model_version` | nullable 컬럼 존재 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| `template_version` | 기본값 컬럼 존재 | 의미·수명 `TBD` | 이미 존재, 규칙 `TBD` | `TBD` | `BLOCKED` |
| `embedding_dimension` | 별도 컬럼·차원 제약 없음 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| `distance_metric` | 저장 없음 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| `threshold_version` | 저장 없음 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| normalization version | 저장 없음 | `TBD` | `TBD` | `TBD` | `BLOCKED` |
| model artifact 식별자 | 저장 없음 | `TBD` | `TBD` | `TBD` | `BLOCKED` |

#### index 선택 게이트

| 판단 근거 | Index 없음 | HNSW | IVFFlat |
| --- | --- | --- | --- |
| 검토 시점 | exact scan이 latency 목표 충족 | 충분한 메모리와 근사 검색 정확도 요구가 입증됨 | training/list 운영과 데이터 규모 근거가 입증됨 |
| 필요한 증거 | 데이터 규모별 exact scan benchmark | build/query 메모리, recall, latency | training 상태, list/probe별 recall·latency |
| 현재 판정 | `PROVISIONAL` 기본 유지 후보 | `BLOCKED` | `BLOCKED` |

현재 `Index 없음`도 최종 규격 확정이 아니다. 성능 근거가 생길 때까지
임의 index를 추가하지 않는 안전한 임시 상태다.

#### 필요한 구현 작업

| 작업 | 담당 | 선행 조건 | 산출물 |
| --- | --- | --- | --- |
| 모델 명세와 test vector 제공 | Backend 1 | VEC-01~03 | model card, 전처리, sample embedding metadata |
| 거리·threshold 평가 | Backend 1 | 평가 dataset·목표 확정 | 동일인/타인 분포, 지표와 threshold version |
| PostgreSQL query prototype | Backend 2 | 차원·거리 확정 | 연산자와 query 테스트 |
| 규모별 benchmark | Backend 2, Backend 1 | 예상 데이터 규모·latency 목표 | exact/index 비교, EXPLAIN |
| 차원·metadata Migration | Backend 2 | VEC-04, VEC-07 | 새 Migration과 검증 SQL |
| index Migration | Backend 2 | VEC-05가 index 사용으로 확정된 경우만 | index DDL, build·rollback Runbook |
| 모델 교체·재등록 흐름 | Backend 1·2 | VEC-06 | version 호환·backfill·rollback 절차 |

#### 필요한 인수 증거

| 증거 | 생성 담당 | 검토 담당 | 완료 기준 |
| --- | --- | --- | --- |
| 모델 명세 | Backend 1 | AI Reviewer | 모델·버전·전처리·차원·dtype·정규화 명시 |
| 동일인/타인 평가 | Backend 1 | AI Reviewer, PM | threshold와 목표 지표의 근거 존재 |
| Cross-team 규격 문서 | Backend 1·2 | AI·DB Reviewer | dimension·metric·version 규칙 공동 승인 |
| pgvector query 테스트 | Backend 2 | Backend 1 | test vector의 예상 검색 결과 일치 |
| benchmark·EXPLAIN | Backend 2 | DBA Reviewer | 목표 데이터 규모에서 latency·정확도·메모리 기록 |
| Migration 검증 | Backend 2 | DBA Reviewer | 빈 DB·기존 DB 적용, 재실행·rollback 계획 검토 |
| 모델 교체 검증 | Backend 1·2 | QA | 구 embedding 격리/재등록 정책이 재현됨 |

#### 완료 정의

1. 모델·버전·전처리·차원·dtype·정규화가 승인된다.
2. 거리 함수와 threshold가 평가 자료로 승인된다.
3. `VECTOR(n)`과 metadata 규격이 공동 문서에 존재한다.
4. index 사용 여부가 예상 규모와 benchmark로 결정된다.
5. 필요한 Migration과 query 테스트가 통과한다.
6. 모델 변경 시 구 embedding 처리와 rollback·재등록 정책이 승인된다.
7. Backend 1, Backend 2, AI Reviewer, DBA Reviewer의 승인 기록이 존재한다.

vector Extension 존재만으로는 어떤 항목도 `CONFIRMED`가 되지 않는다.

#### 미결정 시 위험

- 다른 차원의 embedding이 한 컬럼에 혼재할 수 있다.
- 모델과 맞지 않는 거리 함수·threshold로 인증 오류가 증가할 수 있다.
- 근거 없는 index가 메모리와 운영 복잡도만 증가시킬 수 있다.
- 모델 교체 시 기존 사용자가 일괄 인증 불가 상태가 될 수 있다.
- 기존 Migration 수정으로 적용 환경별 checksum과 스키마가 달라질 수 있다.

#### 목표 결정 기한

`PM이 Sprint 일정 기준으로 지정 필요`

## 6.4 역할별 전달 메시지

아래 메시지는 역할 채널에 그대로 붙여 넣을 수 있다. 실제 담당자와
문서 링크만 치환한다.

### Backend 1

```text
[Backend 2 운영 인수 Blocker 협의 요청 — FACE embedding 규격]

Backend 2는 PostgreSQL 17+pgvector 기동, FACE_PROFILE 9개 컬럼,
user_ref UUID 논리 참조와 개인정보·이미지 경로 부재까지 검증했습니다.
현재 embedding은 차원을 강제하지 않는 vector이며 index는 없습니다.

BLOCKER-04 종료를 위해 모델명/버전, 입력 전처리, 출력 차원·dtype,
L2 정규화 여부, 후보 거리 함수, threshold 산정 근거, 모델 변경 가능성을
모델 명세로 부탁드립니다. 동일인·타인 평가 결과와 재현 가능한 test
vector metadata도 필요합니다. 숫자를 구두로만 전달하지 말고 모델
artifact/version과 연결해 주세요.

Backend 2는 이 자료를 받은 뒤 pgvector query prototype, 규모별
benchmark/EXPLAIN, VECTOR(n)·index Migration 필요 여부를 작성하겠습니다.
Backend 1·2와 AI/DB Reviewer가 같은 규격 문서를 승인하고 query·Migration
증거가 통과해야 Blocker를 종료합니다.

희망 방식: 먼저 Notion 결정표를 비동기로 작성한 뒤, 미결정 항목만
25분 AI/DB 회의에서 확정하고 싶습니다.
```

### Backend 3

```text
[Backend 2 운영 인수 Blocker 협의 요청 — API 계약과 Toss]

Backend 2는 MySQL Migration, requestId/commandId/eventId/결제 승인
식별자 멱등성, 슬롯 동시성, Snapshot 조정과 Repository 검증을
완료했습니다. 다만 운영 Controller·공통 HTTP 응답과 실제 Toss 호출은
구현·검증되지 않았습니다.

BLOCKER-01에서는 client-facing Controller 소유권, 공통 성공/실패 DTO,
Status/Error Code, validation·인증·권한 오류, replay·payload conflict·
슬롯 충돌·Pi Timeout 응답, OpenAPI Owner를 결정해 주세요.

BLOCKER-03에서는 PM과 함께 MVP 과금 사유 및 승인/취소/환불 범위를
확정하고, payment/settlement 상태 전이, orderId·paymentKey 수명,
응답 유실·DB 실패 복구, webhook, Secret·로그 정책을 작성해 주세요.

필요 산출물은 승인된 OpenAPI/Error 표, Toss sequence/상태 전이도,
Sandbox Side Effect 및 중복 승인 방지 테스트입니다. 실제 HTTP와 외부
승인 증거까지 있어야 Blocker를 종료합니다.

희망 방식: API·장치 계약 회의 30분, Toss 범위/복구 회의 25분을 분리하고
결정표는 회의 전 비동기로 작성해 주세요.
```

### Frontend

```text
[운영 API 응답 계약 Consumer Review 요청]

Backend 2는 로컬 DB·멱등성·동시성 검증을 완료했지만 운영 Response DTO와
Error Code는 확정하지 않았습니다. Frontend가 replay, payload conflict,
슬롯 없음/경합, Pi Timeout·잠금 실패, Snapshot 조정·관리자 확인을
화면과 재시도 UX에서 어떻게 구분해야 하는지 Consumer 관점 결정이
필요합니다.

Backend 3·2가 제안할 Controller 소유권, 공통 Wrapper, 성공/실패 DTO,
Status/Error 표를 검토하고 필드별 소비 방식, 재시도 가능 여부,
사용자 메시지와 관리자 이동 흐름을 승인해 주세요. Toss를 포함한다면
연속 클릭 방지, Timeout 후 결과 재조회, 승인 완료/미확정 UX도
정리해 주세요.

산출물은 승인 코멘트가 포함된 OpenAPI, Frontend 오류 처리 표,
대표 성공·실패 contract test입니다. Frontend 승인과 통합 테스트가
있어야 BLOCKER-01을 종료합니다.

희망 방식: 회의 전 오류 시나리오 표에 비동기 코멘트 후 30분
API·장치 계약 회의에서 충돌 항목만 결정하고 싶습니다.
```

### Embedded

```text
[실물 Pi 인증·명령 Side Effect 인수 협의 요청]

Backend 2는 장치 HTTP fixture, commandId/eventId DB 멱등성,
잠금·센서 event 상태 반영, 부팅 Snapshot 일치/불일치 조정을
MySQL에서 검증했습니다. 실물 Pi 인증과 물리 잠금 동작은 아직
검증하지 않았고 HTTP 성공을 물리 성공으로 간주하지 않습니다.

장치 identity와 deviceId↔stationId 등록, 인증·replay 방어,
credential 발급/저장/회전/폐기, 양방향 protocol, 동작별 Timeout·Retry,
Pi 측 commandId 중복 처리와 재부팅 복구를 결정해 주세요.
특히 Command Accepted와 Physical Success/Failure를 별도 상태·시간으로
정의해야 합니다.

필요 산출물은 인증 결정서, Pi–Spring 계약, command 상태 전이,
오류 code, 실물 잠금 성공·실패 로그/영상, 같은 commandId 2회에
물리 동작 1회, 같은 eventId 2회에 Spring 전이 1회, 실물 Snapshot
조정 증거입니다.

희망 방식: firmware 제약과 현재 메시지 예시를 회의 전에 공유하고,
Backend 2·3·Frontend와 30분 API·장치 계약 회의에서 경계를 확정한 뒤
실물 테스트 세션을 별도로 잡고 싶습니다.
```

### PM

```text
[Backend 2 운영 인수 4개 Blocker 의사결정 운영 요청]

Backend 2 로컬 검증은 40/40이며 전체 인수는 40/44입니다. 남은 항목은
운영 HTTP 계약, 실물 Pi Side Effect, Toss 외부 승인, embedding 검색
규격으로 Backend 2 단독 구현 대상이 아닙니다.

각 Blocker에 실제 Owner, Decision Maker, Reviewer를 지정해 주세요.
별도 DBA/QA가 없다면 작성자와 가능한 한 분리된 팀원을 Reviewer로
지정해야 합니다. 또한 Toss MVP 범위·과금 사유와 각 결정 기한을
Sprint 기준으로 확정해 주세요.

필요한 운영은 ① API·장치 계약 30분 회의, ② Toss 25분 회의,
③ AI/DB 25분 회의, ④ 4개 Blocker별 Decision/Implementation/
Integration Test/Acceptance Evidence Jira 분리입니다.

Blocker는 “논의 완료”가 아니라 승인 문서, 구현, 통합·실물·Sandbox·
benchmark 증거가 모두 있을 때만 CONFIRMED 처리합니다. MVP 제외는
반드시 OUT_OF_SCOPE 결정 기록이 있어야 합니다.

희망 방식: 담당자 지정과 Jira 생성부터 비동기로 완료한 뒤,
회의에서는 선택지와 충돌만 결정하도록 운영 부탁드립니다.
```

## 6.5 회의 안건

모든 회의는 사전 문서가 준비되지 않으면 결정 회의가 아니라 자료 수집
회의로 강등된다. 미확정 값을 현장에서 추측해 채우지 않는다.

### 회의 1 — API·장치 계약 정합성 회의

```text
참석:
Backend 2
Backend 3
Frontend
Embedded
QA/통합 테스트 담당

목표:
운영 Controller·HTTP 응답 계약
Pi 인증·장치 명령 계약의 결정 경계 확정

시간:
30분 이내
```

| 시간 | 안건 | 결정 목표 | 사전 준비 담당 |
| --- | --- | --- | --- |
| 0~3분 | 현재 40/44와 금지 추정 확인 | 회의 범위·결정권자 확인 | PM 또는 진행자 |
| 3~8분 | Controller 소유권 | client-facing, device-facing, admin 조회 Owner | Backend 2·3 |
| 8~14분 | 공통 HTTP 계약 | Wrapper, 오류 catalog Owner, OpenAPI 위치 | Backend 3, Frontend |
| 14~20분 | 실패 시나리오 | replay/conflict/슬롯 없음/Timeout/복구 응답의 결정 담당 | Backend 2·3, Frontend |
| 20~26분 | Pi 인증·명령 경계 | identity, protocol 후보, accepted/physical result 분리 | Embedded, Backend 2 |
| 26~29분 | 구현·통합 테스트 분담 | API·firmware·Frontend·QA 산출물 Owner | 전체 |
| 29~30분 | 미결정·기한 확인 | Decision Task, 승인자, PM 지정 기한 | PM |

사전 준비:

- Backend 2: 현재 service outcome, fixture, DB 상태 전이 요약
- Backend 3: 기존 공통 응답·예외 규칙과 Controller 후보 목록
- Frontend: 오류별 UX·재시도 요구
- Embedded: 현재 protocol·firmware 제약·물리 동작 시간 자료
- QA: 인수 증거 저장 위치와 실물 테스트 양식

### 회의 2 — 외부 시스템·AI 저장 규격 회의

#### 분리 판정

`분리 권장`

Toss는 Backend 3·PM의 제품/외부 결제 결정이고 embedding은 Backend 1·2의
모델/DB 결정이다. 공통 참석자만을 이유로 한 회의에 묶으면 각 분야의
증거를 검토할 시간이 부족하다. 다음 두 회의로 나눈다.

분리 전 요청에 정의된 공동 참석 기준은 Backend 1, Backend 2, Backend 3,
PM이다. 네 역할에는 두 회의의 결정 결과 링크를 모두 공유하되, 실제
결정 회의에는 아래처럼 필요한 Reviewer와 Consumer만 추가한다.

#### 회의 2-A — Toss 범위·복구 계약

```text
참석:
Backend 3
Backend 2
Frontend
PM
결제 Reviewer 또는 QA

시간:
25분 이내
```

| 시간 | 안건 | 결정 목표 | 사전 준비 담당 |
| --- | --- | --- | --- |
| 0~5분 | MVP 결제 범위 | 과금 사유, Sandbox/실결제, 승인·취소·환불 포함/제외 | PM, Backend 3 |
| 5~10분 | 상태·식별자 | settlement/payment 상태, orderId/paymentKey 수명 | Backend 3, Backend 2 |
| 10~16분 | 실패·복구 | 응답 유실, DB 실패, 중복 요청, webhook 경쟁 | Backend 3 |
| 16~20분 | 보안 | Secret 저장, 로그 마스킹, webhook 검증 | Backend 3, Reviewer |
| 20~24분 | 구현·인수 | Sandbox와 중복 승인 테스트 Owner | Backend 3, QA |
| 24~25분 | 미결정·기한 | Decision 기록과 PM 기한 | PM |

#### 회의 2-B — AI embedding·pgvector 규격

```text
참석:
Backend 1
Backend 2
PM
AI 모델 Reviewer
DBA Reviewer

시간:
25분 이내
```

| 시간 | 안건 | 결정 목표 | 사전 준비 담당 |
| --- | --- | --- | --- |
| 0~6분 | 모델 명세 | model/version/preprocess/dimension/dtype/normalization | Backend 1 |
| 6~12분 | 검색 품질 | 거리 함수, threshold, 동일인/타인 평가 근거 | Backend 1, AI Reviewer |
| 12~17분 | DB 규격 | VECTOR(n), metadata, 모델 버전 공존 | Backend 2 |
| 17~21분 | index 판단 | 데이터 규모·latency 목표와 benchmark 계획 | PM, Backend 2 |
| 21~24분 | Migration·재등록 | 새 Migration, rollback, 구 embedding 정책 | Backend 1·2, DBA |
| 24~25분 | 미결정·기한 | 자료 보완 Owner와 PM 기한 | PM |

## 6.6 Jira 작업 분해

실제 Jira Key는 생성하지 않는다. PM이 프로젝트 규칙에 따라 Task를
생성하고 연결한다.

| Jira 유형 | 제목 | 담당 | 선행 작업 | 완료 조건 |
| --- | --- | --- | --- | --- |
| Decision Task | `[BE] 운영 API Controller 소유권·공통 응답 계약 확정` | Backend 3, Backend 2 | 실제 Decision Maker 지정 | 승인된 소유권·DTO·Status/Error·OpenAPI 결정 |
| Implementation Task | `[BE] 운영 Controller·DTO·예외 매핑 구현` | 각 Controller Owner | API Decision `CONFIRMED` | 명세와 일치하는 구현 |
| Integration Test Task | `[QA/BE] 운영 API 성공·중복·충돌·Timeout 통합 테스트` | QA, Backend 2·3 | Controller 구현 | 필수 실패 시나리오 자동 테스트 통과 |
| Acceptance Evidence Task | `[QA] 운영 API OpenAPI·HTTP 인수 증거 정리` | QA/통합 테스트 담당 | 통합 테스트 | Frontend 승인·HTTP 로그·OpenAPI 링크 첨부 |
| Decision Task | `[EM/BE] Pi 인증·Protocol·commandId 처리 계약 확정` | Embedded, Backend 2 | 실제 Decision Maker 지정 | 인증·provisioning·protocol·retry·상태 전이 승인 |
| Implementation Task | `[EM/BE] Pi 인증과 accepted/physical result 흐름 구현` | Embedded, Backend 2 | Pi Decision `CONFIRMED` | credential·transport·상태 처리 구현 |
| Integration Test Task | `[EM/QA] 실물 잠금·센서·중복 command/event 통합 테스트` | Embedded, QA, Backend 2 | 실물 장비와 구현 | 성공·실패·중복·Snapshot 시나리오 통과 |
| Acceptance Evidence Task | `[EM/QA] Pi 실물 동작 로그·영상·보안 증거 정리` | Embedded, QA | 실물 테스트 | 로그·영상·DB 결과·Secret 검사 첨부 |
| Decision Task | `[BE/PM] Toss MVP 범위·상태·멱등성·복구 정책 확정` | Backend 3, PM | 결제 Decision Maker 지정 | 과금 사유·승인/취소/환불·복구·보안 승인 |
| Implementation Task | `[BE] Toss 승인·복구·Webhook 연동 구현` | Backend 3 | Toss Decision `CONFIRMED` | 선택된 범위의 Client·상태·복구 구현 |
| Integration Test Task | `[BE/QA] Toss Sandbox 승인·중복·응답 유실 검증` | Backend 3, QA | Sandbox와 구현 | 외부 승인 1회와 최종 DB 일치 |
| Acceptance Evidence Task | `[QA] Toss Side Effect·Secret 마스킹 인수 증거 정리` | QA/결제 Reviewer | 통합 테스트 | Sandbox/webhook 로그와 보안 검사 첨부 |
| Decision Task | `[AI/DB] FACE_PROFILE embedding·검색 규격 확정` | Backend 1, Backend 2 | 모델·DB Decision Maker 지정 | 차원·정규화·metric·threshold·index 결정 승인 |
| Implementation Task | `[AI/DB] pgvector query·Migration·모델 버전 정책 구현` | Backend 1, Backend 2 | Vector Decision `CONFIRMED` | AI 코드와 새 Migration 일치 |
| Integration Test Task | `[AI/DB] 얼굴 검색 품질·pgvector 성능·호환성 검증` | Backend 1, Backend 2 | 구현·평가 dataset | query 정확도, benchmark, EXPLAIN, 모델 교체 검증 |
| Acceptance Evidence Task | `[AI/DB] 모델 명세·평가·Migration 인수 증거 정리` | Backend 1, Backend 2 | 통합 테스트 | AI·DB Reviewer 공동 승인 첨부 |

### Jira 연결 규칙

- 각 Blocker의 Decision Task가 다른 세 Task의 선행 작업이다.
- Decision이 `PROPOSED`인 동안 Implementation Task는 설계 spike만 허용하고
  운영 규격을 확정하지 않는다.
- Integration Test Task는 Mock과 실물/외부 테스트를 구분한다.
- Acceptance Evidence Task가 승인되기 전 Blocker를 `CONFIRMED`로 닫지 않는다.
- MVP 제외 항목은 PM의 `OUT_OF_SCOPE` 결정 기록을 Evidence Task에 첨부한다.

## 6.7 결정 기록 템플릿

```markdown
## DEC-XXX — 결정 제목

### 상태

PROPOSED / CONFIRMED / REJECTED / SUPERSEDED

### 관련 Blocker

BLOCKER-XX

### 배경

현재 확인된 사실과 이 결정을 지금 내려야 하는 이유를 기록한다.

### 선택지

1. 선택지 A
2. 선택지 B
3. 선택지 C

각 선택지의 장점, 단점, 운영비, 호환성과 보안 영향을 기록한다.

### 최종 결정

결정 전에는 TBD로 두며 추정값을 입력하지 않는다.

### 결정 근거

API 명세, 모델 평가, 실물 로그, 외부 Sandbox 결과, benchmark 등
검증 가능한 근거 링크를 기록한다.

### 영향 범위

- API/DTO:
- Application:
- Database/Migration:
- Frontend:
- Embedded:
- AI:
- 운영/보안:

### 담당자

Owner 역할과 실제 담당자를 기록한다.

### 승인자

Decision Maker와 Reviewer를 기록한다.

### 결정일

실제 승인일을 기록한다. 예정일을 결정일로 쓰지 않는다.

### 후속 작업

- Implementation Task:
- Integration Test Task:
- Acceptance Evidence Task:

### 검증 증거

- 문서/코드:
- 테스트 결과:
- HTTP/장치/외부 로그:
- 영상 또는 benchmark:
- Reviewer 승인:

### Rollback 또는 대체 경로

결정을 되돌릴 조건과 데이터·API 호환 처리 방법을 기록한다.
```

## 6.8 Blocker 종료 체크리스트

### BLOCKER-01 — 운영 Controller·HTTP 응답 계약

- [ ] 모든 Controller와 endpoint Owner 확정
- [ ] client-facing과 device-facing 경계 확정
- [ ] 공통 성공·실패 Response DTO 확정
- [ ] Validation·인증·권한 실패 형식 확정
- [ ] HTTP Status와 Error Code 표 확정
- [ ] replay·payload conflict 응답 확정
- [ ] 슬롯 없음·경합·Pi Timeout·잠금 실패 응답 확정
- [ ] Snapshot 조정·관리자 확인 응답 확정
- [ ] OpenAPI 단일 기준 반영
- [ ] Frontend와 Embedded 소비 계약 승인
- [ ] 운영 Controller 통합 테스트 통과
- [ ] HTTP 요청·응답 인수 증거와 Reviewer 승인

모두 완료되면 `CONFIRMED`다. 결정만 끝났으면 `PROVISIONAL`이다.

### BLOCKER-02 — 실물 Pi 인증·명령 Side Effect

- [ ] Pi identity와 deviceId↔stationId provisioning 확정
- [ ] 인증·replay 방어 방식 확정
- [ ] credential 발급·저장·회전·폐기 확정
- [ ] Pi→Spring·Spring→Pi protocol 확정
- [ ] 동작별 Timeout·Retry·offline 복구 확정
- [ ] Command Accepted와 Physical Success/Failure 분리
- [ ] Pi 측 commandId 중복 정책 구현
- [ ] Spring 측 eventId 중복 정책 실물 연동 검증
- [ ] 실물 잠금 성공·실패 검증
- [ ] 센서 삽입·제거와 오류 검증
- [ ] 실물 부팅 Snapshot 일치·불일치·중복 검증
- [ ] 장치 로그·영상·DB 결과와 Secret 검사 승인

실물 증거가 없으면 `BLOCKED`를 유지한다.

### BLOCKER-03 — Toss 외부 승인 Side Effect

- [ ] PM의 MVP 결제 범위·과금 사유 결정
- [ ] 승인·취소·환불·부분 취소 포함/제외 결정
- [ ] payment/settlement 상태 전이 확정
- [ ] orderId/paymentKey/transactionKey 수명과 UNIQUE 정책 확정
- [ ] Toss 성공·DB Commit 순서 확정
- [ ] 응답 유실·DB 실패 복구 정책 확정
- [ ] webhook 사용·검증·중복·순서 정책 확정
- [ ] 결제 버튼 연속 클릭·재요청 중복 승인 방지 검증
- [ ] Toss Sandbox 또는 공식 인수 환경 Side Effect 확인
- [ ] Secret 저장과 결제 식별자 로그 마스킹 검증
- [ ] Reviewer 승인 증거 첨부

공식 `OUT_OF_SCOPE` 결정이 없다면 외부 검증 전 종료할 수 없다.

### BLOCKER-04 — embedding 차원·거리 함수·vector index

- [ ] 모델명·버전·artifact 식별자 확정
- [ ] 입력 전처리 확정
- [ ] 출력 차원·dtype 확정
- [ ] L2 normalization 위치·방식 확정
- [ ] 거리 함수 확정
- [ ] threshold와 동일인·타인 평가 근거 확보
- [ ] VECTOR(n)과 metadata 규격 확정
- [ ] MVP 데이터 규모·검색 QPS·latency 목표 확정
- [ ] index 없음/HNSW/IVFFlat 비교 benchmark 확보
- [ ] vector index 적용 여부와 근거 확정
- [ ] 모델 변경 시 구 embedding 격리·재등록 정책 확정
- [ ] 새 Migration·query·재실행·rollback 검증
- [ ] Backend 1·2와 AI·DB Reviewer 공동 승인

Extension 존재나 차원 미지정 vector 적용만으로 종료할 수 없다.

## 최종 Blocker 상태표

| Blocker | 현재 상태 | 다음 행동 | 필요한 상대 역할 | 종료 증거 |
| --- | --- | --- | --- | --- |
| 운영 Controller·HTTP 응답 계약 | `BLOCKED` | API·장치 회의 전 소유권/오류 표 작성 | Backend 3, Frontend, Embedded, PM, QA | 승인 OpenAPI, DTO/Error 표, Controller 통합 테스트, HTTP 로그 |
| 실물 Pi 인증·명령 Side Effect | `BLOCKED` | 인증/protocol 후보와 firmware 제약 제출 후 실물 테스트 설계 | Embedded, Backend 2, QA, PM | 인증 결정서, command/event 중복 로그, 잠금 성공·실패 영상, Snapshot 증거 |
| Toss 외부 승인 Side Effect | `BLOCKED` | PM 범위 결정과 Toss sequence·복구 초안 작성 | Backend 3, Frontend, PM, 결제 Reviewer/QA | Sandbox Side Effect, 중복 승인 방지, 복구 테스트, Secret 검사 |
| embedding·vector 검색 규격 | `BLOCKED` | Backend 1 모델 명세·평가 자료 비동기 제출 | Backend 1, Backend 2, AI Reviewer, DBA Reviewer, PM | 공동 규격, 평가 결과, benchmark/EXPLAIN, Migration·query 검증 |

## 즉시 실행 항목 3개

1. **PM이 실제 Owner·Decision Maker·Reviewer를 지정하고 Jira 16개 작업을
   생성한다.** 별도 DBA/QA가 없다면 대체 Reviewer를 명시하고 각
   Decision Task의 목표 기한을 Sprint 기준으로 지정한다.
2. **30분 API·장치 계약 회의를 예약하고 사전 입력을 강제한다.**
   Backend 2·3은 Controller 소유권과 실패 시나리오 표, Frontend는 소비
   요구, Embedded는 인증/protocol/물리 동작 제약을 회의 전에 작성한다.
3. **병렬 증거 수집을 시작한다.** Backend 1은 모델·차원·정규화·평가
   자료를 제출하고, Backend 3·PM은 Toss MVP 범위와 Sandbox/복구 초안을
   제출한다. 자료가 준비되면 Toss 25분, AI/DB 25분 회의를 분리해 연다.

# STEP-08 API·장치 작업 멱등성 보고서

## 판정

`PASSED`

동일한 반납 요청, 장치 명령, 장치 이벤트 및 결제 승인 재전송이 MySQL
업무 Row와 슬롯·정산 상태를 두 번 변경하지 않도록 최소 JDBC
트랜잭션 서비스를 구현했다. 순차 호출과 실제 2-thread 동시 호출을
Testcontainers MySQL 8.0에서 검증했으며 STEP-08 테스트 10개와 전체
테스트 30개가 모두 통과했다.

현재 저장소에는 운영 Controller, 장치 디스패처 및 Toss Client가 없다.
따라서 승인된 멱등 정책을 서비스·DB 경계에 구현했으며 HTTP 응답 매핑과
외부 시스템 Side Effect는 구현 범위를 명시적으로 분리했다.

## 선행 조건

| 항목 | 결과 |
| --- | --- |
| STEP-01 | `PASSED` |
| STEP-02 | `PASSED` |
| STEP-03 | `PASSED` |
| STEP-04 | `PASSED` |
| STEP-05 | `PASSED` |
| STEP-06 | `PASSED` |
| STEP-07 | `PASSED` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 작업 시작 시 HEAD | `3130d0153760c5aac6e7f8c77c02fd9547d72f86` |
| 작업 시작 시 작업 트리 | clean |

## 기존 구현과 제약 감사

STEP-08 시작 시 운영 Java 소스는 `BillisanApplication.java`뿐이었다.
Controller, Service, Repository, `@Transactional` 업무 경계 및 외부
Device/Toss Client는 없었다. 따라서 기존 결과 재사용 정책을 적용할
서비스 구현은 새로 필요했지만 운영 Endpoint와 응답 형식은 임의로
추가하지 않았다.

V1 Migration에는 다음 DB 방어선이 이미 존재했다.

| 경계 | DB 제약 | 근거 |
| --- | --- | --- |
| 반납 요청 | `return_attempt.request_id` UNIQUE | `uk_return_attempt_request_id` |
| 장치 명령 | `device_operation.command_id` UNIQUE | `uk_device_operation_command_id` |
| 장치 이벤트 | `device_operation.event_id` UNIQUE | `uk_device_operation_event_id` |
| 결제 주문 | `payment_attempt.toss_order_id` UNIQUE | `uk_payment_attempt_toss_order_id` |
| 결제 승인 | `payment_attempt.toss_payment_key` UNIQUE | `uk_payment_attempt_toss_payment_key` |

Migration은 관련 식별자와 계약이 확정된 상태였으며 운영 제약을
완화하거나 새 컬럼을 추정할 필요가 없었다. 따라서 V1은 수정하지 않고
추가 Migration도 만들지 않았다.

## 정책 근거

다음 권위 문서에서 재전송 정책을 확인했다.

- [API 명세 v1.0](https://app.notion.com/p/3a6443b01b1381d7982ad0f445732be3)
- [Backend 역할·책임 명세](https://app.notion.com/p/955443b01b1383ee932e81e2d9cf36df)
- [공통 정책 명세](https://app.notion.com/p/c5e443b01b138266aca3818e8431c07e)

공통 원칙은 동일 ID·동일 payload의 재전송은 저장된 결과를 반환하고,
동일 ID·다른 payload는 `IDEMPOTENCY_CONFLICT`로 거절하는 것이다.
`X-Request-Id`는 추적용이고 업무 멱등 키와 통합하지 않는다.

## 식별자별 처리 정책

| 식별자 | 동일 payload 재전송 | 다른 payload | Side Effect 경계 |
| --- | --- | --- | --- |
| `requestId` | 기존 `returnAttemptId`, 상태, 생성 시각 반환 | `IdempotencyConflictException` | 신규 시도일 때만 슬롯을 `RETURNING`으로 전이 |
| `commandId` | 기존 `operationId`와 저장 상태 반환 | `IdempotencyConflictException` | 호출자는 `APPLIED`일 때만 물리 명령 발행 |
| `eventId` | 저장 결과를 `REPLAYED`로 반환 | command/slot/result 메타데이터 불일치 시 충돌 | 신규 이벤트일 때만 작업 완료와 슬롯 물리 상태 갱신 |
| `toss_order_id` | 결제 시도에 저장된 주문 ID와 일치 확인 | 같은 결제 시도에 다른 주문 ID면 충돌 | DB UNIQUE는 주문 생성 중복의 최종 방어선 |
| `toss_payment_key` | 기존 성공 결제·PAID 정산 반환 | 다른 결제 시도에서 같은 key면 충돌 | 신규 승인일 때만 결제·정산 상태 변경 |

실패한 반납을 업무적으로 다시 시도할 때는 같은 `requestId`가 아니라
새 `requestId`와 새 `returnAttemptId`를 사용한다.

## 적용한 DB·서비스 방어선

### 반납 요청

`INSERT ... ON DUPLICATE KEY UPDATE`의 no-op upsert로 UNIQUE 경쟁을
원자화했다. 저장된 대여·반납 슬롯이 요청과 같은지 비교한 뒤 신규
Row인 경우에만 `AVAILABLE + EMPTY + LOCKED` 슬롯을 조건부
`RETURNING`으로 변경한다. 중복 요청은 슬롯을 다시 갱신하지 않는다.

### 장치 명령

`command_id` UNIQUE 위에서 no-op upsert를 사용하고 station, slot,
rental, returnAttempt, operationType을 저장 Row와 비교한다. 결과의
`Outcome.APPLIED`와 `Outcome.REPLAYED`를 분리해 향후 장치 디스패처가
물리 명령을 한 번만 발행할 수 있게 했다.

### 장치 이벤트

command 행을 `SELECT ... FOR UPDATE`로 잠근다. `event_id IS NULL`인
작업만 완료시키며 같은 Transaction에서 슬롯 물리 상태를 변경한다.
`event_id` UNIQUE 경쟁은 명시적 멱등 충돌 예외로 변환한다. 과거
eventId가 나중에 다시 도착해도 저장 결과만 반환하고 현재 슬롯 상태를
되돌리지 않는다.

### 결제 승인

payment 행과 연결 settlement 행을 순서대로 `FOR UPDATE`한다. 주문 ID,
금액, 승인 key를 검증한 뒤 `REQUESTED → SUCCEEDED`와
`PENDING → PAID`를 같은 Transaction에서 조건부 적용한다. 승인 key가
다른 결제에 이미 있거나 동시 UNIQUE 경쟁에서 패배하면 전체
Transaction을 롤백하고 멱등 충돌로 변환한다.

### 예외 흐름

`IdempotencyConflictException`은 key 종류와 값을 보존하는 명시적
RuntimeException이다. return/command 중복은 no-op upsert로 raw DB
예외를 피하고 event/payment의 UNIQUE 경쟁은 이 예외로 변환한다.
운영 Controller가 없으므로 현재 500 응답을 노출하는 Endpoint도 없다.
향후 예외 매퍼는 이 예외를 API 정책의 HTTP 409에 매핑해야 한다.

## Transaction 검증

네 서비스 진입점 모두 `@Transactional`이다.

| 검증 | 실제 결과 |
| --- | --- |
| 동일 `requestId`, 다른 슬롯 | 충돌, 두 번째 슬롯은 `AVAILABLE` 유지 |
| 동일 `commandId`, 다른 슬롯 | 충돌, 장치 작업 Row는 최초 1개 유지 |
| 기존 `eventId`를 다른 command에 사용 | 충돌, 두 번째 command는 event 없음·`REQUESTED` 유지 |
| 기존 payment key를 다른 결제에 사용 | 충돌, 두 번째 payment는 `REQUESTED`·key NULL 유지 |
| 결제 충돌 뒤 settlement | `PENDING` 유지 |
| 오래된 eventId 재전송 | 최신 슬롯 상태·`updated_at` 유지 |

위 결과로 충돌 예외가 발생한 Transaction에서 중간 Row 또는 상태
변경이 Commit되지 않음을 확인했다.

## MySQL 테스트 시나리오

| 테스트 | 호출 방식 | 결과 |
| --- | --- | --- |
| 같은 `requestId` | 순차 2회 | `APPLIED` 1, `REPLAYED` 1, Row 1 |
| 같은 `requestId` | 2-thread 동시 | `APPLIED` 1, `REPLAYED` 1, Row 1 |
| 같은 `commandId` | 순차 2회 | 기존 operation 반환, Row 1 |
| 같은 `commandId` | 2-thread 동시 | `APPLIED` 1, `REPLAYED` 1, Row 1 |
| 같은 `eventId` | 순차 2회 + 나중 재전송 | 슬롯 상태 재전이 없음 |
| 같은 `eventId` | 2-thread 동시 | 작업 완료·슬롯 갱신 1회 |
| 같은 승인 key | 순차 2회 | 결제·정산 시각 재갱신 없음 |
| 같은 승인 key | 2-thread 동시 | 결제 `SUCCEEDED`, 정산 `PAID` 각 1회 |
| 서로 다른 ID | 각 2개 | 모두 정상 처리 |
| 동일 ID·다른 payload | 네 경계 | 충돌 및 전체 롤백 |

H2는 사용하지 않았다. Flyway V1이 적용된 실제 Testcontainers
`mysql:8.0`에서 검증했다.

## 실행 명령과 결과

잘못된 시스템 `JAVA_HOME`은 변경하지 않고 명령 세션에서만 STEP-03의
검증 JDK 21 경로를 사용했다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat compileJava compileTestJava
```

결과: `BUILD SUCCESSFUL`.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat test `
  --tests com.ssafy.billisan.idempotency.IdempotencyServiceTests `
  --rerun-tasks
```

결과: STEP-08 테스트 10개, 실패 0, 오류 0, 건너뜀 0.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat clean build --rerun-tasks
```

결과: `BUILD SUCCESSFUL`.

| Test suite | 테스트 | 실패 | 오류 | 건너뜀 |
| --- | ---: | ---: | ---: | ---: |
| `BillisanApplicationTests` | 5 | 0 | 0 | 0 |
| `DeviceContractFixtureTest` | 15 | 0 | 0 | 0 |
| `IdempotencyServiceTests` | 10 | 0 | 0 | 0 |
| 합계 | 30 | 0 | 0 | 0 |

## 남은 위험과 후속 경계

| 항목 | 영향 | 후속 조치 |
| --- | --- | --- |
| 운영 Controller 부재 | 기존 결과 200·payload 충돌 409 미검증 | Controller 도입 시 응답 DTO와 예외 매퍼 통합 테스트 |
| 장치 디스패처 부재 | 실제 물리 명령 중복 미검증 | `Outcome.APPLIED`에만 outbox/dispatch 연결 |
| Toss Client 부재 | 외부 승인 호출 중복 미검증 | Toss 호출 전후 key 보존·조회 복구 정책 확정 |
| 이벤트 원본 상태 미보존 | occupancy/lock 값만 다른 동일 eventId strict 비교 불가 | 계약 승인 뒤 event history 또는 payload hash 검토 |
| 주문 생성 서비스 부재 | `toss_order_id` 서비스 재사용 정책 미구현 | 생성 API 구현 시 UNIQUE 충돌을 기존 결제로 변환 |
| 대여 단위 완료 직렬화 부재 | 서로 다른 requestId의 완료 경쟁은 별도 문제 | rental 행 잠금과 완료 시도 검사 구현 |

이번 단계에서는 불확정 이벤트 컬럼, outbox 테이블 또는 HTTP API를
추정해 추가하지 않았다.

## 변경 파일

- `backend/src/main/java/com/ssafy/billisan/idempotency/IdempotencyConflictException.java`
- `backend/src/main/java/com/ssafy/billisan/idempotency/IdempotencyRepository.java`
- `backend/src/main/java/com/ssafy/billisan/idempotency/IdempotencyService.java`
- `backend/src/test/java/com/ssafy/billisan/idempotency/IdempotencyServiceTests.java`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-08-IDEMPOTENCY.md`

Migration, Spring 설정 및 의존성은 변경하지 않았다. Commit, Push, MR도
수행하지 않았다.

## 추천 Commit 메시지

```text
S15P11A201-55 feat: MySQL 기반 멱등성 트랜잭션 방어선 추가
```

## 다음 실행 프롬프트

`STEP-09`

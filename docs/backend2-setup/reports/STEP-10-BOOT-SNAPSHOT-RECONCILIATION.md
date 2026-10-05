# STEP-10 Pi 부팅 Snapshot 조정 보고서

## 판정

`PASSED`

Pi 부팅 Snapshot과 MySQL의 현재 슬롯 상태를 한 Transaction에서 비교하고,
일치하면 업무·물리 상태를 유지하며 불일치하면 자동 정상화 없이
`ADMIN_REVIEW` 또는 `RECOVERY_REQUIRED`로 전환하는 application core를
구현했다. 알 수 없는 슬롯과 일부 슬롯 누락은 슬롯 하나의 불일치와 달리
장치·재고 범위 불일치로 분류해 대여소를 `MAINTENANCE/ERROR`로 전환한다.

Testcontainers MySQL 8.0 전용 통합 테스트 9개와 장치 계약 fixture 테스트
18개가 모두 통과했다. 범용 RecoveryTask, Snapshot 장기 이력, Pi DB,
PostgreSQL DataSource 및 존재하지 않는 슬롯 자동 생성은 추가하지 않았다.

## 선행 조건과 작업 기준

| 항목 | 확인 결과 |
| --- | --- |
| STEP-01~09 | 모두 `PASSED` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 작업 시작 HEAD | `f740d09ec0da06eca8923fee7766376621228c74` |
| 작업 시작 작업 트리 | clean |
| 운영 Snapshot Controller·DTO | 없음 |
| 승인 Endpoint | `POST /api/v1/edge/stations/{stationId}/boot-snapshots` |
| 테스트 DB | Testcontainers `mysql:8.0` |
| 영속성 방식 | Spring `JdbcTemplate` |
| Migration | Flyway V1·V2 적용 상태, 이번 단계 V3 추가 |

근거 우선순위는 현재 소스와 Migration, STEP-07 계약 문서와 fixture,
STEP-08 멱등성 기준, STEP-09 슬롯 상태 모델 순서로 적용했다.

## 확인된 Snapshot 계약

### 요청

```text
POST /api/v1/edge/stations/{stationId}/boot-snapshots
Idempotency-Key: <bootId>
X-Request-Id: <trace-id>
X-Device-Id: <deviceId>
```

| 필드 | 의미 |
| --- | --- |
| URI `stationId` | MySQL 비교 대상 대여소 |
| `bootId` | 한 Pi 부팅 인스턴스와 Snapshot 재전송 식별자 |
| `deviceId` | 측정 장치 식별자 |
| `measuredAt` | Pi가 물리 상태를 측정한 시각 |
| `slots[].slotId` | 물리 슬롯 식별자 |
| `slots[].slotNumber` | 대여소 내 물리 슬롯 번호 |
| `slots[].occupancyStatus` | `EMPTY`, `OCCUPIED`, `UNKNOWN` |
| `slots[].lockStatus` | `LOCKED`, `UNLOCKED`, `UNKNOWN`, `ERROR` |
| `slots[].sensorHealth` | `NORMAL`, `DEGRADED`, `ERROR` |

Snapshot의 업무 멱등 키는 `bootId`다. `X-Request-Id`는 추적용이며,
일반 API의 `requestId`나 장치 이벤트의 `eventId`를 Snapshot에 새로
추가하거나 대신 사용하지 않았다.

운영 Controller와 HTTP DTO·응답 Enum은 아직 없으므로 임의 생성하지
않았다. 확정 Endpoint와 요청 필드는 HTTP Mock으로 유지하고, 실제 상태
조정은 Controller에서 호출할 수 있는 transactional application service로
구현했다.

## 일치·불일치 규칙

비교는 `BootSnapshotPolicy`가 담당하며 DB 갱신과 분리했다.

| 비교 항목 | 일치 조건 | 불일치 근거 |
| --- | --- | --- |
| 슬롯 집합 | DB의 station 슬롯 ID 집합과 동일 | `UNKNOWN_SLOT`, `INCOMPLETE_SNAPSHOT` |
| 슬롯 번호 | 동일 `slotId`의 `slotNumber` 일치 | `SLOT_NUMBER_MISMATCH` |
| 점유 | `occupancy_status`와 `occupancyStatus` 일치 | `OCCUPANCY_MISMATCH` |
| 잠금 | `lock_status`와 `lockStatus` 일치 | `LOCK_MISMATCH` |
| 센서 건강 | `sensorHealth = NORMAL` | `SENSOR_UNHEALTHY` |

Pi 측정값은 비교 근거일 뿐 MySQL의 `occupancy_status`와 `lock_status`를
자동 덮어쓰지 않는다. 기존 슬롯을 자동 생성하지 않으며, 불일치 뒤
나중에 일치 Snapshot이 와도 관리자 승인 없이 기존 복구 상태를 해제하지
않는다.

## 상태 전이표

| 입력 | 조정 범위 | station | slot | 진행 중 return attempt |
| --- | --- | --- | --- | --- |
| 모든 슬롯 일치 | 없음 | 기존 상태 유지 | 기존 상태 유지 | 기존 상태 유지 |
| 알려진 슬롯의 점유·잠금·번호·센서 불일치 | 특정 슬롯 | 기존 상태 유지 | 해당 슬롯 `ADMIN_REVIEW` | 해당 슬롯 연결 건 `RECOVERY_REQUIRED` |
| unknown `slotId` 포함 | 장치 전체 | `MAINTENANCE/ERROR` | 기존 슬롯 전체 `ADMIN_REVIEW` | 연결 건 `RECOVERY_REQUIRED` |
| DB 슬롯 일부 누락 | 장치 전체 | `MAINTENANCE/ERROR` | 기존 슬롯 전체 `ADMIN_REVIEW` | 연결 건 `RECOVERY_REQUIRED` |
| 기존 복구 상태에서 새 일치 Snapshot | 기존 복구 범위 유지 | 자동 해제 없음 | 자동 해제 없음 | 자동 완료·정상화 없음 |
| 동일 Snapshot 재전송 | 없음 | 추가 갱신 없음 | 추가 갱신 없음 | 추가 갱신 없음 |

진행 중 반납은 `PROCESSING` 또는 `PHYSICAL_DONE`일 때만
`RECOVERY_REQUIRED`로 바꾸고 `failure_reason`에
`BOOT_SNAPSHOT_MISMATCH`를 저장한다. `COMPLETED`, `FAILED`를 되돌리지
않으며 AI 결과만으로 파손·과금을 확정하지 않는다.

## DB와 Transaction 경계

Flyway V3
`backend/src/main/resources/db/migration/V3__add_boot_snapshot_reconciliation_state.sql`
은 새 테이블 없이 다음 현재 상태만 추가한다.

| 테이블 | 추가 상태 |
| --- | --- |
| `station` | 마지막 `bootId`, canonical payload SHA-256, 측정 시각, 현재 복구 사유·bootId |
| `slot` | 현재 Snapshot 복구 사유·bootId |

station의 `last_boot_id`는 UNIQUE다. boot metadata 세 필드와 복구 metadata
두 필드는 각각 모두 NULL이거나 모두 값이 있도록 CHECK를 적용했다.

처리 순서는 다음과 같다.

1. 정규형 UUID, 측정 시각, 슬롯 목록과 Enum을 검증한다.
2. station 1행을 `FOR UPDATE`로 잠근다.
3. 해당 station의 슬롯을 `slot_number` 순으로 `FOR UPDATE`한다.
4. 같은 `bootId`면 저장 hash와 현재 payload hash를 비교한다.
5. 새 Snapshot이면 저장 상태와 관측 상태를 순수 정책 객체로 비교한다.
6. 마지막 boot metadata를 기록하고 필요한 안전 상태만 전이한다.
7. 모든 비교·기록·상태 전이는 한 `@Transactional` 경계에서 Commit한다.

Snapshot 장기 이력은 저장하지 않는다. 마지막 boot metadata와 아직
해소되지 않은 현재 복구 근거만 보존한다.

## 멱등성과 예외 처리

| 상황 | 결과 | Side Effect |
| --- | --- | --- |
| 새 `bootId` | `APPLIED` | 비교 결과에 따른 단일 상태 전이 |
| 같은 `bootId`·같은 payload | `REPLAYED` | 없음 |
| 같은 `bootId`·다른 payload | `IDEMPOTENCY_CONFLICT` | Transaction 롤백 |
| 마지막보다 늦지 않은 새 Snapshot | `STALE_SNAPSHOT` | Transaction 롤백 |
| 존재하지 않는 station | `UNKNOWN_STATION` | Transaction 롤백 |
| 잘못된 UUID·빈 slots·중복 slotId·잘못된 Enum | 입력 검증 오류 | Transaction 롤백 |

같은 `bootId`의 비교에는 station·device·측정 시각과 slotId 정렬 후 모든
슬롯 필드를 포함한 canonical SHA-256을 사용한다. 따라서 배열 순서만
달라진 동일 Snapshot은 같은 요청이며, 실제 필드가 달라지면 충돌이다.

## 테스트 결과

### STEP-10 MySQL 8.0 통합 테스트

| 시나리오 | 결과 |
| --- | --- |
| 모든 슬롯 일치 | `MATCHED`, 업무·물리 상태 유지 |
| 센서 점유 상태 불일치 | 해당 슬롯만 `ADMIN_REVIEW`, 반납 `RECOVERY_REQUIRED` |
| 잠금 상태 불일치 | 해당 슬롯만 `ADMIN_REVIEW`, DB 잠금 상태 미덮어쓰기 |
| 알려지지 않은 `slotId` | station 전체 복구, unknown 슬롯 행 0 |
| 잘못된 `stationId` | `UNKNOWN_STATION`, 기존 상태 무변경 |
| 동일 Snapshot 중복 | 첫 호출 `APPLIED`, 재전송 `REPLAYED`, timestamp 무변경 |
| 일부 슬롯 Payload 누락 | station 전체 복구, 누락 슬롯 근거 저장 |
| 복구 뒤 일치 Snapshot | 기존 수동복구 상태 유지, 자동 정상화 없음 |
| 같은 `bootId`의 다른 payload | `IDEMPOTENCY_CONFLICT`, 기존 상태 유지 |

결과: 9개 실행, 실패 0, 오류 0, 건너뜀 0.

### 장치 계약 fixture

기존 일치·점유 불일치 외에 잠금 불일치, unknown slot, 일부 슬롯 누락
fixture와 HTTP 요청을 추가했다.

결과: 18개 실행, 실패 0, 오류 0, 건너뜀 0.

### 전체 회귀 빌드

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
Set-Location C:\Users\SSAFY\S15P11A201\backend
.\gradlew.bat clean build --rerun-tasks
```

| Test suite | 테스트 | 실패 | 오류 | 건너뜀 |
| --- | ---: | ---: | ---: | ---: |
| `BillisanApplicationTests` | 5 | 0 | 0 | 0 |
| `DeviceContractFixtureTest` | 18 | 0 | 0 | 0 |
| `IdempotencyServiceTests` | 10 | 0 | 0 | 0 |
| `SlotAllocationServiceTests` | 6 | 0 | 0 | 0 |
| `BootSnapshotReconciliationServiceTests` | 9 | 0 | 0 | 0 |
| 합계 | 48 | 0 | 0 | 0 |

결과: `BUILD SUCCESSFUL` (9 actionable tasks, 9 executed).

### 실행 명령

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
Set-Location C:\Users\SSAFY\S15P11A201\backend
.\gradlew.bat test `
  --tests "com.ssafy.billisan.snapshot.BootSnapshotReconciliationServiceTests" `
  --tests "com.ssafy.billisan.devicecontract.DeviceContractFixtureTest" `
  --rerun-tasks
```

결과: `BUILD SUCCESSFUL`.

## 장치 전체 장애와 슬롯 불일치 구분

| 범위 | 판정 | 이유 |
| --- | --- | --- |
| `SLOT` | 서버에 알려진 슬롯의 물리 상태·번호·센서만 불일치 | 다른 슬롯의 사용 가능성을 불필요하게 차단하지 않음 |
| `STATION` | unknown slot 또는 기존 슬롯 누락 | station의 물리 inventory 자체를 신뢰할 수 없음 |

Heartbeat timeout, 장치 인증 실패, 통신 단절을 Snapshot 하나로 추정하지
않는다. 이들은 별도의 장치 전체 장애 정책이 확정되어야 한다.

## 남은 Embedded·Backend 협의

1. `deviceId`와 `stationId`의 프로비저닝·소유권 매핑 및 불일치 응답
2. `MATCHED`, `RECOVERY_REQUIRED`, `APPLIED`, `REPLAYED`의 HTTP 응답 DTO
   및 status code
3. API 계약에는 있으나 MySQL CHECK에는 없는 `lockStatus=UNKNOWN` 처리
4. firmware가 부분 Snapshot을 전송하지 않도록 원자적 측정 완료를
   보장할지, 슬롯 총수·schema version을 추가할지
5. `sensorHealth=DEGRADED`를 즉시 수동복구로 볼지 별도 임계치를 둘지
6. 관리자 확인 후 station·slot·return attempt 복구 상태를 해제하는
   승인 API, 권한 및 감사 기록 소유자
7. Heartbeat timeout과 재부팅 반복을 장치 전체 장애로 판정하는 임계치

운영 Controller·HTTP 응답, 장치 토큰 검증 및 관리자 해제 흐름은 위
합의 전 임의 구현하지 않았다.

## 변경 파일

- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotPolicy.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotReconciliationService.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotRejectedException.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotRepository.java`
- `backend/src/main/resources/db/migration/V3__add_boot_snapshot_reconciliation_state.sql`
- `backend/src/test/java/com/ssafy/billisan/snapshot/BootSnapshotReconciliationServiceTests.java`
- `backend/src/test/java/com/ssafy/billisan/devicecontract/DeviceContractFixtureTest.java`
- `backend/src/test/resources/device-contract/device-gateway.http`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-lock-mismatch.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-unknown-slot.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-partial.json`
- `docs/backend2-setup/DEVICE-CONTRACT-DRAFT.md`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-10-BOOT-SNAPSHOT-RECONCILIATION.md`

Commit, Push, MR은 수행하지 않았다.

## 추천 Commit 메시지

```text
S15P11A201-55 feat: 부팅 Snapshot 안전 조정 구현
```

## 다음 실행 프롬프트

`STEP-11`

# STEP-11 Seed와 Repository Query 검증 보고서

## 판정

`PASSED`

운영 Profile과 완전히 분리된 테스트 전용 Java fixture를 만들고,
Testcontainers MySQL 8.0에서 재고·슬롯 조회와 관리자 집계를 검증했다.
Fixture는 최소 시나리오를 반복 생성할 수 있으며 같은 DB에서 두 번
실행해도 Row가 누적되지 않는다.

운영 관리자·재고 API 계약과 Controller는 저장소에서 확인되지 않아
HTTP 계층을 추정하지 않았다. 확정된 MySQL 상태 모델과 STEP-09 후보
규칙을 사용하는 읽기 전용 JDBC Repository와 projection만 추가했다.

## 선행 조건

| 항목 | 결과 |
| --- | --- |
| STEP-01~10 | 모두 `PASSED` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 작업 시작 HEAD | `5d39e4e437c77ee7de656df73c815bf5ae165e0f` |
| 작업 시작 작업 트리 | clean |
| Migration | Flyway V1~V3 |
| 영속성 | Spring `JdbcTemplate` |
| 테스트 DB | Testcontainers `mysql:8.0` |
| ORM·Mapper | 없음 |

## 기존 테스트 데이터 관리 방식

현재 네 MySQL 통합 테스트는 각각 Testcontainers DB를 만들고 Java 테스트
코드에서 `JdbcTemplate`으로 FK 역순 DELETE와 INSERT를 수행한다.
`@Sql`, test Flyway location, `data.sql` 또는 운영 자동 Seed는 없다.

STEP-11도 같은 스타일을 따른다.

| 항목 | 구현 |
| --- | --- |
| Fixture | `Backend2QueryScenarioFixture` |
| 위치 | `src/test/java` |
| 전용 DB | `billisan_query_validation_test` |
| 초기화 | 모든 업무 테이블 FK 역순 DELETE |
| 데이터 키 | 결정적 가짜 UUID와 `fixture-*` 식별자 |
| 재실행 | 초기화 후 동일 INSERT, 누적 없음 |
| 운영 보호 | 연결 catalog가 전용 테스트 DB가 아니면 삭제 전 예외 |

main Flyway, `application.yaml`, `application-local.yaml`, Compose 및 운영
소스에는 Seed 실행 설정을 추가하지 않았다.

## 생성한 시나리오

모든 시각은 2026-07-27의 고정 `LocalDateTime`이며 MySQL session
time zone은 `+00:00`이다.

| 데이터 | 수 | 세부 |
| --- | ---: | --- |
| 사용자 | 2 | `fixture-user-01`, `fixture-admin-01`; 얼굴 등록 FALSE |
| 대여소 | 1 | `FIXTURE-STATION-01`, 비운영 이름·위치 |
| 슬롯 | 5 | 대여 가능, 대여 중, 반납 중, 관리자 확인, 빈 반납 후보 |
| 대여 | 4 | `ACTIVE`, `REQUESTED`, `RETURNING`, `COMPLETED` 각 1 |
| 반납 시도 | 3 | `PROCESSING`, `FAILED`, `COMPLETED` 각 1 |
| 장치 명령 | 3 | `REQUESTED`, `SUCCEEDED`, `FAILED` |
| 장치 이벤트 | 2 | 성공·실패 명령에 각각 가짜 event ID |

### 슬롯 상태

| 번호 | service | occupancy | lock | item condition | 의미 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `AVAILABLE` | `OCCUPIED` | `LOCKED` | `NORMAL` | 대여 가능 |
| 2 | `RENTING` | `OCCUPIED` | `LOCKED` | `NORMAL` | 대여 처리 중 |
| 3 | `RETURNING` | `EMPTY` | `LOCKED` | NULL | 반납 처리 중 |
| 4 | `ADMIN_REVIEW` | `OCCUPIED` | `ERROR` | `REVIEW_REQUIRED` | 관리자 확인 |
| 5 | `AVAILABLE` | `EMPTY` | `LOCKED` | NULL | 반납 후보 |

실사용자 이름·학번·이메일, 실제 장치 ID, 얼굴 이미지·embedding은
사용하지 않았다. 기존 `face_registered` 값도 모두 FALSE다.

## Repository 구현

구현 위치:
`com.ssafy.billisan.inventory.InventoryQueryRepository`

| 메서드 | 쿼리 결과 |
| --- | --- |
| `findStationInventorySummary` | station별 슬롯 상태 조건부 집계 |
| `findSlotsByStation` | slot number 순 projection 목록 |
| `findAdminActivitySummary` | 활성 대여·반납 결과·장치 명령·이벤트 집계 |
| `findResolvedReturnAttempts` | 실패·완료 반납 시도 목록 |
| `findDeviceOperations` | 장치 명령과 event ID 목록 |

확정 API가 없으므로 Controller, HTTP DTO, pagination 규격은 만들지
않았다.

## 조회·집계 검증 결과

| 검증 | 기대 | 실제 |
| --- | ---: | ---: |
| 대여소 전체 슬롯 | 5 | 5 |
| 대여 가능 슬롯 | 1 | 1 |
| 대여 중 슬롯 | 1 | 1 |
| 반납 처리 중 슬롯 | 1 | 1 |
| 관리자 확인 필요 슬롯 | 1 | 1 |
| 활성 대여 | 1 | 1 |
| 실패 반납 시도 | 1 | 1 |
| 완료 반납 시도 | 1 | 1 |
| 장치 명령 | 3 | 3 |
| 장치 이벤트 | 2 | 2 |

존재하지 않는 station의 두 summary 조회는 빈 결과를 반환한다. 슬롯
목록은 1~5번 순서와 DB 상태를 그대로 반환했으며, 반납 목록은
`FAILED`, `COMPLETED`만 조회했다. 장치 명령 수와 event ID가 있는
물리 이벤트 수를 혼용하지 않았다.

Fixture를 같은 테스트 DB에 두 번 실행한 뒤에도 사용자 2, 대여소 1,
슬롯 5, 대여 4, 반납 3, 장치 작업 3으로 동일했다.

## Query 성능 점검

| 항목 | 결과 |
| --- | --- |
| 재고 집계 | 조건부 `COUNT/SUM` 한 SQL |
| 관리자 집계 | station 기준 한 SQL, DB 내부 집계 |
| Entity 전체 로딩 | Entity 자체가 없으며 집계에 사용하지 않음 |
| 목록 projection | 필요한 컬럼만 명시적으로 SELECT |
| N+1 | Repository 메서드별 고정 SQL 1회, 후속 행별 조회 없음 |
| 슬롯 정렬 | DB `ORDER BY slot_number` |
| 반납·장치 정렬 | DB 업무 시각 + PK |
| 상태 필터 | Repository 내부 Enum 값을 bind |

MySQL `information_schema.statistics`에서 다음 index prefix를 실제
확인했다.

| 테이블 | index prefix |
| --- | --- |
| `slot` | `(station_id, slot_number)` |
| `rental` | `(checkout_slot_id, status)` |
| `return_attempt` | `(return_slot_id, status)` |
| `device_operation` | `(station_id)` |

집계는 Java Stream으로 전체 행을 세지 않고 DB의 숫자 projection만
반환한다. 날짜 범위 조건은 확정 요구사항에 없어 추가하지 않았다.
`DATETIME(6)`은 timezone을 저장하지 않으므로 기존 정책대로
UTC local time으로 해석하며, 테스트 session의 `@@session.time_zone`
결과 `+00:00`과 고정 UTC 완료 시각이 일치했다.

## 테스트 결과

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
Set-Location C:\Users\SSAFY\S15P11A201\backend
.\gradlew.bat test `
  --tests "com.ssafy.billisan.inventory.InventoryQueryRepositoryTests" `
  --rerun-tasks
```

결과: 9개 실행, 실패 0, 오류 0, 건너뜀 0,
`BUILD SUCCESSFUL`.

검증한 항목:

1. 최소 가짜 데이터와 금지 생체 데이터 부재
2. Fixture 두 번 실행 시 Row 수 불변
3. 전용 테스트 DB 외 catalog 실행 거부
4. 재고 상태별 집계
5. 슬롯 projection과 정렬
6. 관리자 집계
7. 실패·완료 반납 조회 및 시각
8. 장치 명령·이벤트 분리
9. station 범위 Query 인덱스

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
| `InventoryQueryRepositoryTests` | 9 | 0 | 0 | 0 |
| `SlotAllocationServiceTests` | 6 | 0 | 0 | 0 |
| `BootSnapshotReconciliationServiceTests` | 9 | 0 | 0 | 0 |
| 합계 | 57 | 0 | 0 | 0 |

결과: `BUILD SUCCESSFUL` (9 actionable tasks, 9 executed).

## 변경 파일

- `backend/src/main/java/com/ssafy/billisan/inventory/InventoryQueryRepository.java`
- `backend/src/test/java/com/ssafy/billisan/inventory/Backend2QueryScenarioFixture.java`
- `backend/src/test/java/com/ssafy/billisan/inventory/InventoryQueryRepositoryTests.java`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-11-SEED-AND-QUERY-VALIDATION.md`

Commit, Push, MR은 수행하지 않았다.

## 추천 Commit 메시지

```text
S15P11A201-55 feat: 재고 집계와 테스트 fixture 구현
```

## 다음 실행 프롬프트

`STEP-12`

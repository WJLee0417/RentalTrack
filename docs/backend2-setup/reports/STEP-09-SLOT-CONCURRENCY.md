# STEP-09 슬롯 동시성과 데이터 정합성 보고서

## 판정

`PASSED`

MySQL 8.0의 후보 슬롯 행 잠금과 조건부 상태 전이를 사용해 한 시점에
하나의 슬롯이 하나의 진행 중 대여 또는 반납 배정만 갖도록 구현했다.
슬롯 1개에 대한 동시 요청 2개는 정확히 1개만 성공했고, 슬롯 N개에
대한 N개/N+1개 동시 요청도 중복 없이 기대한 수만 성공했다.

Testcontainers MySQL 8.0 전용 테스트 6개와 전체 테스트 36개가 모두
통과했다. H2, Java `synchronized`, Thread sleep 및 `rental_id`
UNIQUE는 사용하지 않았다.

## 선행 조건

| 항목 | 결과 |
| --- | --- |
| STEP-01~08 | 모두 `PASSED` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 작업 시작 시 HEAD | `acc124f15cc66a9e5dc9c6aa4016cf60431a7652` |
| 작업 시작 시 작업 트리 | clean |
| MySQL | Testcontainers `mysql:8.0` |
| 영속성 방식 | Spring `JdbcTemplate` |
| Migration | Flyway V1 적용 상태 |

## 기존 슬롯 모델 감사

현재 저장소에는 Slot Entity와 Java 상태 Enum이 없다. 실제 상태 기준은
`V1__create_mysql_business_schema.sql`의 문자열 컬럼과 CHECK다.

| 컬럼 | 허용 상태 |
| --- | --- |
| `service_status` | `AVAILABLE`, `RENTING`, `RETURNING`, `ADMIN_REVIEW`, `MAINTENANCE`, `ERROR` |
| `occupancy_status` | `OCCUPIED`, `EMPTY`, `UNKNOWN` |
| `lock_status` | `LOCKED`, `UNLOCKED`, `ERROR` |
| `item_condition` | NULL 또는 `NORMAL`, `REVIEW_REQUIRED`, `DAMAGED`, `UNKNOWN` |

기존 `IdempotencyService.createReturnAttempt()`는 호출자가 지정한 슬롯을
`AVAILABLE + EMPTY + LOCKED` 조건으로 `RETURNING` 전환한다. UPDATE가
0행이면 전체 Transaction을 롤백하므로 같은 특정 슬롯의 이중 반납
배정은 이미 방어했다. 그러나 같은 대여소에서 후보를 자동 선정하는
Query와 대여 슬롯 배정 서비스는 없었다.

기존 구조에 JPA, MyBatis, `@Version`, 비관적 Repository Lock 또는 Java
Lock은 없었다.

## 핵심 불변식과 활성 소유자

한 슬롯에서 다음 활성 업무 Row의 합계는 최대 1이다.

| 업무 | 활성 소유자 조건 | 슬롯 상태 |
| --- | --- | --- |
| 대여 배정 | `rental.status = REQUESTED` | `slot.service_status = RENTING` |
| 반납 배정 | `return_attempt.status IN (PROCESSING, PHYSICAL_DONE, RECOVERY_REQUIRED)` | `slot.service_status = RETURNING` |

대여가 `ACTIVE`가 된 뒤 기존 checkout 슬롯은 물리 대여 처리가 끝난
슬롯이므로 활성 슬롯 소유자로 세지 않는다. 실패한 반납
`return_attempt.status = FAILED`도 새 시도를 차단하지 않는다.

## 선택한 동시성 전략

`비관적 후보 행 Lock + SKIP LOCKED + 조건부 UPDATE + 단일 Transaction`
전략을 선택했다.

1. 대여소와 후보 물리 상태를 만족하는 슬롯을 `slot_number` 순으로 찾는다.
2. `LIMIT 1 FOR UPDATE SKIP LOCKED`로 후보 한 행만 잠근다.
3. 같은 후보 Predicate를 포함한 조건부 UPDATE로 `RENTING` 또는
   `RETURNING`으로 전이한다.
4. 같은 Transaction에서 `rental` 또는 `return_attempt` Row를 생성한다.
5. 후보가 없거나 갱신 행 수가 1이 아니면 `SlotUnavailableException`으로
   종료한다.
6. 업무 Row 생성 실패 시 상태 전이도 함께 롤백한다.

`SKIP LOCKED`는 다른 요청이 이미 잠근 후보를 기다리지 않고 다음 후보를
탐색하므로, N개 슬롯에 대한 N개 요청이 대여소 행 Lock 없이 서로 다른
슬롯을 획득할 수 있다.

## 핵심 Query와 Lock 범위

### 대여 후보

```sql
SELECT slot_id
FROM slot FORCE INDEX (idx_slot_station_allocation_candidate)
WHERE station_id = ?
  AND service_status = 'AVAILABLE'
  AND occupancy_status = 'OCCUPIED'
  AND lock_status = 'LOCKED'
  AND item_condition = 'NORMAL'
ORDER BY slot_number
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

### 반납 후보

```sql
SELECT slot_id
FROM slot FORCE INDEX (idx_slot_station_allocation_candidate)
WHERE station_id = ?
  AND service_status = 'AVAILABLE'
  AND occupancy_status = 'EMPTY'
  AND lock_status = 'LOCKED'
  AND item_condition IS NULL
ORDER BY slot_number
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

두 Query는 대여소와 후보 상태가 모두 일치하는 슬롯 한 행을 업무
소유 대상으로 선정한다. 대여소 Row와 전역 자원은 잠그지 않는다.
InnoDB 기본 `REPEATABLE READ`에서는 선정 record 주변의 secondary index
next-key/gap lock이 생길 수 있으나, 전용 복합 인덱스로 스캔 범위를
해당 station·상태 prefix로 제한한다.

조건부 UPDATE는 조회와 같은 물리 상태를 다시 확인한다.

```sql
UPDATE slot
SET service_status = 'RENTING',
    updated_at = ?
WHERE slot_id = ?
  AND service_status = 'AVAILABLE'
  AND occupancy_status = 'OCCUPIED'
  AND lock_status = 'LOCKED'
  AND item_condition = 'NORMAL';
```

반납은 동일 구조에서 `EMPTY`, `item_condition IS NULL`,
`RETURNING`을 사용한다.

## Migration과 인덱스

Flyway V2
`V2__add_slot_allocation_candidate_index.sql`을 추가했다.

```sql
CREATE INDEX idx_slot_station_allocation_candidate
    ON slot (
        station_id,
        service_status,
        occupancy_status,
        lock_status,
        item_condition,
        slot_number
    );
```

station, 서비스 상태, 물리 상태를 동등 조건으로 좁힌 뒤
`slot_number` 순서로 첫 후보를 찾는다. 테스트는
`information_schema.statistics`에서 여섯 컬럼의 순서를 실제
확인했다. V1 테이블·CHECK·UNIQUE는 변경하지 않았고 `rental_id`
UNIQUE도 추가하지 않았다.

## 포기한 대안

| 대안 | 선택하지 않은 이유 |
| --- | --- |
| Java `synchronized` | 다중 인스턴스에서 공유되지 않아 분산 서버 안전성을 보장하지 못함 |
| Transaction 없는 조회 후 INSERT | 두 요청이 같은 AVAILABLE 슬롯을 읽는 TOCTOU 경쟁 발생 |
| 조건부 UPDATE만 사용 | 소유자 token 컬럼 없이 어떤 슬롯을 획득했는지 안전하게 회수하기 어려움 |
| 낙관적 Lock + 재시도 | Entity와 version 컬럼이 없으며 Migration·재시도 코드 범위가 더 큼 |
| 대여소 Row 비관적 Lock | 같은 대여소의 독립 슬롯까지 불필요하게 직렬화 |
| 모든 슬롯 전역 Lock | 처리량 저하 및 분산 환경 확장성 부족 |
| 활성 업무용 `rental_id` UNIQUE | 반납 실패 후 같은 대여의 새 시도 정책과 충돌 |
| 다형 소유자 컬럼 추가 | 확정 ERD에 없는 owner type/id를 추정하고 스키마를 확대해야 함 |

## MySQL 동시성 테스트

`ExecutorService`와 `CountDownLatch`로 모든 작업 thread가 준비된 뒤
동시에 시작되도록 했다. Thread sleep에는 의존하지 않았다.

| 시나리오 | 실행 | 결과 |
| --- | --- | --- |
| 슬롯 1개, 반납 요청 2개 | 2-thread | 성공 1, `SlotUnavailableException` 1 |
| 대여 슬롯 4개, 요청 4개 | 4-thread | 성공 4, 획득 slotId 4개 모두 다름 |
| 대여 슬롯 3개, 요청 4개 | 4-thread | 성공 3, 명시적 충돌 1 |
| Transaction 중 FK 실패 | 존재하지 않는 userId로 대여 Row INSERT | 예외 발생, 슬롯 `AVAILABLE`, 대여 Row 0 |
| 실패 반납 재시도 | 첫 시도 `FAILED` 후 새 requestId | 같은 rentalId로 2행, 활성 시도 1 |
| 후보 인덱스 | `information_schema.statistics` | 컬럼·순서 일치 |

### 활성 소유자 SQL 재검증

각 동시성 테스트 뒤 다음 SQL을 실행했다.

```sql
SELECT COUNT(*)
FROM (
    SELECT active.slot_id
    FROM (
        SELECT checkout_slot_id AS slot_id
        FROM rental
        WHERE status = 'REQUESTED'
        UNION ALL
        SELECT return_slot_id AS slot_id
        FROM return_attempt
        WHERE status IN (
            'PROCESSING',
            'PHYSICAL_DONE',
            'RECOVERY_REQUIRED'
        )
    ) active
    GROUP BY active.slot_id
    HAVING COUNT(*) > 1
) duplicate_owner;
```

실제 결과는 모든 시나리오에서 `0`이었다.

## Rollback 검증

대여 후보 슬롯을 `FOR UPDATE`로 획득하고 `RENTING`으로 갱신한 다음,
존재하지 않는 userId로 `rental` INSERT를 시도해 FK 오류를 발생시켰다.

| 검증 항목 | 결과 |
| --- | --- |
| 예외 | Spring `DataIntegrityViolationException` |
| 실패 대여 Row | 0 |
| 슬롯 상태 | `AVAILABLE`로 복원 |
| 같은 슬롯의 다음 정상 요청 | 성공 |
| 활성 중복 소유자 | 0 |

이는 슬롯 상태 전이와 업무 Row 생성이 같은 `@Transactional` 경계에
있어 중간 상태가 Commit되지 않음을 보여준다.

## 실행 결과

잘못된 시스템 `JAVA_HOME`은 변경하지 않고 명령 세션에서만 검증된 JDK
21 경로를 사용했다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat test `
  --tests com.ssafy.billisan.slot.SlotAllocationServiceTests `
  --rerun-tasks
```

결과: 6개 실행, 실패 0, 오류 0, 건너뜀 0,
`BUILD SUCCESSFUL`.

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
| `SlotAllocationServiceTests` | 6 | 0 | 0 | 0 |
| 합계 | 36 | 0 | 0 | 0 |

빈 Testcontainers DB에는 Flyway V1과 V2가 순서대로 적용됐다.

## 성능과 교착 위험

| 항목 | 평가 |
| --- | --- |
| Lock 단위 | 선정 슬롯 record 중심; InnoDB next-key/gap lock 가능 |
| 대여소 직렬화 | 없음 |
| 다른 대여소 영향 | 없음 |
| 경쟁 후보 처리 | 잠긴 행을 건너뛰고 다음 slot_number 탐색 |
| Transaction 내 다중 슬롯 Lock | 없음 |
| Lock 순서 | 슬롯 1행 → 해당 업무 Row INSERT |
| 교착 위험 | 낮음; 다중 슬롯 또는 역순 자원 획득 없음 |

`SKIP LOCKED` 특성상 다른 Transaction이 잠근 슬롯은 곧 롤백될
예정이어도 이번 요청에서는 후보가 아닌 것으로 보인다. 따라서 실제
여유 슬롯이 잠시 잠겨 있으면 일시적인 `SlotUnavailableException`이
발생할 수 있다. 짧은 API 재시도 정책이 필요하면 Controller 단계에서
팀 합의 후 제한적으로 추가해야 하며, 현재 서비스는 내부 sleep이나
무제한 재시도를 하지 않는다.

기존 특정 슬롯 반납 경로도 조건부 UPDATE 실패 시 전체 Transaction을
롤백하므로 새 자동 선정 경로와 동시에 실행돼도 이중 소유가 Commit되지
않는다. 향후 여러 슬롯을 한 업무에서 동시에 잠그게 되면 반드시
`slot_number` 오름차순의 단일 Lock 순서를 유지해야 한다.

## 변경 파일

- `backend/src/main/java/com/ssafy/billisan/slot/SlotAllocationService.java`
- `backend/src/main/java/com/ssafy/billisan/slot/SlotAllocationRepository.java`
- `backend/src/main/java/com/ssafy/billisan/slot/SlotUnavailableException.java`
- `backend/src/main/resources/db/migration/V2__add_slot_allocation_candidate_index.sql`
- `backend/src/test/java/com/ssafy/billisan/slot/SlotAllocationServiceTests.java`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-09-SLOT-CONCURRENCY.md`

Commit, Push, MR은 수행하지 않았다.

## 추천 Commit 메시지

```text
S15P11A201-55 feat: 슬롯 행 잠금 기반 동시 배정 구현
```

## 다음 실행 프롬프트

`STEP-10`

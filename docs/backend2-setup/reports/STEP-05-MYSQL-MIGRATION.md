# STEP-05 MySQL Migration 보고서

## 판정

`PASSED`

Notion ERD v1.0.1과 MySQL 8.0 물리 규칙을 기준으로 Flyway를 도입하고
MySQL 업무 테이블 9개를 생성했다. 빈 로컬 `billisan` DB에서 Migration
적용, 같은 DB 재실행, `information_schema` 메타데이터 조회, 반납 재시도
및 Testcontainers 기반 통합 테스트를 모두 통과했다.

## 선행 조건과 권위 근거

| 항목 | 결과 |
| --- | --- |
| STEP-01 | `PASSED` |
| STEP-02 | `PASSED` |
| STEP-03 | `PASSED` |
| STEP-04 | `PASSED` |
| Migration 선택 | `MIGRATION_TOOL=flyway` |
| 권위 ERD | Notion `08. Billisan ERD 및 데이터 아키텍처 문서 v1.0.1` |
| 기존 Migration 도구·파일 | 없음 |
| 기존 Entity·상태 Enum | 없음 |
| 기존 ORM·SQL Mapper | 없음 |
| 대상 MySQL | `8.0.46`, Docker Compose 서비스 `mysql`, `healthy` |
| 적용 전 업무 DB | `billisan`, 테이블 수 `0` |
| 운영 데이터 사용 | 사용하지 않음 |

Downloads의 v1.0 Markdown export는 v1.0.1보다 오래된 사본이므로 신규
DDL의 권위 근거로 사용하지 않았다. `origin/subPjt_1` 기술조사 문서도
사용자 지시에 따라 근거에서 제외했다.

## Migration 도구와 실행 주체

| 항목 | 적용 |
| --- | --- |
| Spring Boot | `4.1.0` |
| Flyway | `12.4.0`, Spring Boot 관리 버전 |
| MySQL JDBC | Connector/J `9.7.0`, Spring Boot 관리 버전 |
| Flyway MySQL 모듈 | `org.flywaydb:flyway-mysql:12.4.0` |
| Migration 위치 | `backend/src/main/resources/db/migration` |
| 버전 규칙 | `V<version>__<description>.sql` |
| 실행 주체 | Spring Boot 시작 시 단일 MySQL DataSource를 사용하는 Flyway |
| ORM·Mapper | 도입하지 않음 |
| PostgreSQL DataSource | 추가하지 않음 |

Spring Boot 공식 기본 위치인 `classpath:db/migration`과 버전 파일 규칙을
사용했다. JPA, Hibernate, MyBatis는 기존 코드와 팀 선택이 없으므로
도입하지 않았다.

## 생성된 Migration

### `V1__create_mysql_business_schema.sql`

| 논리 엔터티 | 물리 테이블 | 주요 관계·제약 |
| --- | --- | --- |
| `USER_ACCOUNT` | `user_account` | `login_id` UNIQUE, 역할·계정 상태 CHECK |
| `STATION` | `station` | `station_code` UNIQUE, 서비스·장치 상태 CHECK |
| `SLOT` | `slot` | `station` FK, `(station_id, slot_number)` UNIQUE, 빈 슬롯 품질 CHECK |
| `RENTAL` | `rental` | 사용자·인출 슬롯 FK, `rental_request_id` UNIQUE, `due_at` 포함 |
| `RETURN_ATTEMPT` | `return_attempt` | 대여·반납 슬롯 FK, `request_id` UNIQUE, `rental_id` 비고유 인덱스 |
| `DAMAGE_INSPECTION` | `damage_inspection` | `return_attempt_id` FK·UNIQUE, `request_id` UNIQUE |
| `SETTLEMENT` | `settlement` | 사용자·대여·반납 시도 FK, 대여·반납 시도별 UNIQUE |
| `PAYMENT` | `payment_attempt` | 정산 FK, Toss 주문 ID·결제 키 UNIQUE |
| `DEVICE_OPERATION` | `device_operation` | 대여소·슬롯·대여·반납 시도 FK, 명령·이벤트 ID UNIQUE |

모든 업무 테이블은 `InnoDB`, `utf8mb4`,
`utf8mb4_0900_ai_ci`로 생성됐다. UUID PK·FK는
`CHAR(36) CHARACTER SET ascii COLLATE ascii_bin`, 멱등·장치 식별자는
`VARCHAR(100) ... ascii_bin`, 업무 시각은 `DATETIME(6)`를 사용한다.

## 테이블·인덱스·UNIQUE 검증표

실제 로컬 `billisan` DB의 `information_schema` 조회 결과다.

| 검증 규칙 | 실제 메타데이터 | 판정 |
| --- | --- | --- |
| MySQL 업무 테이블 9개 | 9개, 별도 `flyway_schema_history` 1개 | `PASSED` |
| `return_attempt.request_id` UNIQUE | `uk_return_attempt_request_id`, `non_unique=0` | `PASSED` |
| `return_attempt.rental_id` 비고유 인덱스 | `idx_return_attempt_rental_status(rental_id,status)`, `non_unique=1` | `PASSED` |
| `return_attempt.rental_id` UNIQUE 금지 | 해당 컬럼의 UNIQUE 인덱스 수 `0` | `PASSED` |
| 동일 대여의 실패 후 새 반납 시도 | 같은 `rental_id`로 2행 생성 성공 | `PASSED` |
| `damage_inspection.return_attempt_id` 연결 | `fk_damage_inspection_return_attempt` | `PASSED` |
| 반납 시도당 검수 1건 | `uk_damage_inspection_return_attempt_id` | `PASSED` |
| AI 요청 멱등성 | `uk_damage_inspection_request_id` | `PASSED` |
| `device_operation.command_id` 중복 차단 | `uk_device_operation_command_id` | `PASSED` |
| `device_operation.event_id` 중복 차단 | nullable `uk_device_operation_event_id` | `PASSED` |
| Toss 주문 ID 중복 차단 | `uk_payment_attempt_toss_order_id` | `PASSED` |
| Toss 결제 키 중복 차단 | nullable `uk_payment_attempt_toss_payment_key` | `PASSED` |
| Toss 주문 ID 형식 | 길이 6~64와 `[A-Za-z0-9_-]` CHECK | `PASSED` |
| 금액 | `BIGINT UNSIGNED`와 0 이상 CHECK | `PASSED` |
| AI 신뢰도 | `DECIMAL(5,4)`와 0~1 CHECK | `PASSED` |
| FK 기본 동작 | 명시적 `ON UPDATE RESTRICT ON DELETE RESTRICT` | `PASSED` |

`return_attempt.rental_id` 검증은 단일 컬럼 중복 인덱스를 추가하지 않고
ERD의 최신 비고유 복합 인덱스인
`idx_return_attempt_rental_status(rental_id, status)`로 구현했다.
`rental_id`가 선두 컬럼이므로 대여별 조회에도 사용할 수 있다.

## 상태·금지 컬럼 검증

| 항목 | 결과 |
| --- | --- |
| 반납 상태 컬럼 | `return_attempt.status VARCHAR(30)`, NOT NULL |
| 반납 허용 상태 | `PROCESSING`, `PHYSICAL_DONE`, `COMPLETED`, `RECOVERY_REQUIRED`, `FAILED` |
| `process_status`·`processStatus` | 컬럼 수 `0` |
| MySQL 이미지·임베딩·vector 컬럼 | 컬럼 수 `0` |
| `payment_attempt.rental_request_id` | 없음 |
| `UMBRELLA`·`umbrella_id` | 테이블·컬럼 없음 |
| Java 상태 Enum | 기존 코드에 없음, 이번 단계에서 임의 생성하지 않음 |

`user_account.face_registered`는 ERD가 정의한 화면 표시용 Boolean이며
얼굴 이미지나 임베딩 저장 컬럼이 아니다. 얼굴 프로필·임베딩은 계속
Orin PostgreSQL+pgvector 책임으로 남는다.

## 반납 재시도 검증

로컬 DB에서 사용자·대여소·슬롯·대여와 다음 두 반납 시도를 하나의
트랜잭션으로 생성했다.

1. 첫 시도: 동일 `rental_id`, 상태 `FAILED`, 첫 `request_id`
2. 새 시도: 동일 `rental_id`, 상태 `PROCESSING`, 새 `request_id`

두 행이 함께 존재함을 확인했고 결과는
`SAME_RENTAL_ATTEMPT_COUNT_BEFORE_ROLLBACK=2`였다. 검증 후
`ROLLBACK`했으며 `VERIFICATION_ROWS_AFTER_ROLLBACK=0`으로 테스트
업무 데이터가 남지 않았음을 확인했다. 최종 점검에서도 9개 업무 테이블
각각의 행 수가 모두 `0`이었다.

같은 `request_id` 재사용은 Testcontainers 통합 테스트에서
`DuplicateKeyException`이 발생하는 것으로 검증했다.

완료(`COMPLETED`) 시도 대여당 최대 1건은 ERD가 DB UNIQUE가 아닌 Spring
트랜잭션과 `RENTAL` 행 `SELECT ... FOR UPDATE`의 책임으로 정했다. 현재
저장소에는 해당 서비스 코드가 없으므로 이번 Migration은 의도적으로
`rental_id UNIQUE`를 만들지 않았다.

## 실행한 Gradle·애플리케이션 명령

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat compileJava compileTestJava` | `BUILD SUCCESSFUL` |
| `.\gradlew.bat test` | 5개 실행, 실패·오류·건너뜀 0 |
| 첫 `bootRun` + `local` Profile | 빈 DB에 V1 1건 적용 성공 |
| 두 번째 `bootRun` + `local` Profile | `Schema billisan is up to date. No migration necessary.` |
| `.\gradlew.bat build --rerun-tasks` | 7개 Task 실행, `BUILD SUCCESSFUL` |

시스템 `JAVA_HOME`은 기존 오설정을 유지한 채 명령 세션에서만
`C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot`으로
보정했다.

최초 Testcontainers 실행은 `testcontainers/ryuk:0.14.0` 이미지 다운로드
중 Docker Registry 응답 `EOF`로 한 번 실패했다. 이는 컴파일·Migration
오류가 아니라 환경 이미지 다운로드 오류였다. 이미지를 Docker CLI로
받은 후 같은 테스트를 재실행해 성공했으며, 최종 전체 빌드에서도 다시
성공했다.

## 실행한 SQL 검증

- 적용 전 서버 버전·DB·문자셋·Collation·타임존·테이블 수 조회
- 적용 후 업무 테이블과 `flyway_schema_history` 조회
- `information_schema.statistics`의 핵심 UNIQUE·비고유 인덱스 조회
- `information_schema.key_column_usage`의 실제 FK 조회
- `information_schema.check_constraints`의 상태·금액·신뢰도·Toss ID
  CHECK 조회
- 핵심 UUID·멱등 ID·결제 ID의 타입·문자셋·Collation 조회
- 금지된 반납 상태 필드와 이미지·임베딩·vector 컬럼 수 조회
- 동일 대여의 실패 후 새 반납 시도 생성 및 `ROLLBACK`

## ERD 충돌

현재 권위 문서 v1.0.1 기준으로 Migration을 차단하는 충돌은 없다.

| 과거 v1.0 충돌 | v1.0.1 적용 |
| --- | --- |
| `return_attempt.rental_id` UNIQUE 여부 | UNIQUE 제거, 비고유 인덱스 확정 |
| 반납 영속 상태 불일치 | 5개 권위 상태로 확정, `REQUESTED` 비저장 |
| 결제 식별자 혼용 | `settlement_id`, `toss_order_id`, `toss_payment_key`로 확정 |
| `UMBRELLA` 잔존 참조 | MySQL P0에서 제거 |
| Pi SQLite 수치·테이블 잔존 | Pi 업무 DB 0개로 확정 |
| MySQL 물리 이름·타입 미확정 | v1.0.1 물리 규칙으로 확정 |

저장소에는 비교 대상 Entity와 Java Enum이 아직 없어 ERD와 코드의 필드
충돌은 발견되지 않았다. 향후 Entity를 만들 때 이 Migration을
물리 기준선으로 사용해야 한다.

## 변경 파일

- `backend/build.gradle`
- `backend/src/main/resources/db/migration/V1__create_mysql_business_schema.sql`
- `backend/src/test/java/com/ssafy/billisan/BillisanApplicationTests.java`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-05-MYSQL-MIGRATION.md`

## 추천 Commit 메시지

```text
S15P11A201-55 feat: Flyway MySQL 업무 스키마와 무결성 검증 추가
```

`ALLOW_GIT_COMMIT=false`에 따라 Commit, Push, MR은 수행하지 않았다.

## 다음 실행 프롬프트

`STEP-06`

# Billisan Backend 2 개발환경 최종 감사 보고서

## 1. 최종 판정

| 항목 | 판정 |
| --- | --- |
| 전체 판정 | `PARTIALLY_BLOCKED` |
| 로컬 필수 재검증 | `PASSED` — 40/40 |
| 실패한 빌드·테스트 | 없음 |
| 외부·팀 결정 차단 | 4개 인수 경계 |
| 감사 시각 | 2026-07-30 통합 구조 재검증 |

Backend 2의 로컬 개발 기준선, MySQL 8.0 Migration, 로컬
PostgreSQL+pgvector, 장치 계약 fixture, MySQL 멱등성·동시성·Snapshot,
테스트 전용 Seed와 조회는 현재 저장소에서 재현된다. 다만 운영
Controller/HTTP 응답, 실물 Pi 및 장치 인증·명령 Side Effect, Toss 실제
승인 Side Effect, 얼굴 모델의 embedding 차원·검색 인덱스가 확정되거나
실행되지 않았다. 로컬 환경 완료를 운영 통합 완료로 확대하지 않기 위해
전체 판정은 `PARTIALLY_BLOCKED`로 한다.

`ALLOW_GIT_COMMIT=false`에 따라 이번 감사에서는 Commit, Push, MR을
수행하지 않았다.

## 2. 감사 기준선

| 항목 | 실제 값 |
| --- | --- |
| 저장소 루트 | `C:\Users\SSAFY\S15P11A201` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 요구 작업 브랜치 | `S15P11A201-55-be-db` |
| Base Branch | `develop` |
| 현재 HEAD | `59b8cd2cb6a3c27625d709fe8048d9ddea461e1d` |
| `origin/develop` | `edd1b6e1de9697db1d8c9b7f15d0ce1aa82c808c` |
| Base 포함 여부 | `origin/develop`이 현재 HEAD의 ancestor, exit `0` |
| Base 대비 | ahead 15, behind 0 |
| 원격 작업 브랜치 대비 | ahead 79, behind 0 |
| 현재 작업 트리 | `backend` 단일 프로젝트 통합과 문서 정리 변경이 미커밋 상태 |
| JDK | Eclipse Temurin OpenJDK `21.0.11` LTS |
| Gradle Wrapper | `9.5.1` |
| Docker Engine | client/server `29.6.2` |
| Docker Compose | `v5.3.1` |
| MySQL | `8.0.46`, container `healthy` |
| PostgreSQL | `17.10`, container `healthy` |
| pgvector | image `0.8.5-pg17-bookworm`, Extension `vector` 확인 |

STEP-01/02의 “Wrapper·애플리케이션 없음” 기록은 당시 placeholder
저장소 시점의 역사적 결과다. 현재 HEAD에는 STEP-03 이후 Wrapper와
Spring Boot 애플리케이션이 추가되어 있으므로 최종 판정은 현재 파일과
새 실행 결과를 기준으로 한다. `origin/subPjt_1`의 기술조사 문서는
사용자 지시에 따라 감사 근거로 사용하지 않았다.

## 3. 필수 재검증 결과

| 번호 | 재검증 항목 | 판정 | 현재 실행 근거 |
| --- | --- | --- | --- |
| 1~3 | 작업 브랜치, Base, Git 상태 | `PASSED` | `git branch`, `merge-base`, `status`, `diff` |
| 4~6 | JDK 21, Wrapper 빌드, 전체 테스트 | `PASSED` | Temurin 21.0.11, Wrapper 9.5.1, `clean build --rerun-tasks` |
| 7~8 | 빈 DB Migration 및 재실행 안전성 | `PASSED` | Testcontainers MySQL 8.0에 V1~V18 적용 후 두 번째 migrate 0건 |
| 9 | 기준 업무 테이블 | `PASSED` | `createsFinalBusinessAndInternalTables()` |
| 10 | 필수 인덱스·UNIQUE | `PASSED` | `information_schema` 기반 `createsRequiredIndexesAndForeignKeys()` |
| 11 | MySQL 얼굴 이미지·embedding 부재 | `PASSED` | MySQL 메타데이터 기반 금지 컬럼 테스트 |
| 12~14 | docker-local, vector Extension, FACE_PROFILE | `PASSED` | pgvector apply 및 verification SQL 재실행 |
| 15 | Cross-DB userId 타입·형식 | `PASSED` | MySQL `user_account.user_ref CHAR(36)` ↔ PostgreSQL `user_ref UUID`, 표준 UUID 문자열 계약 |
| 16 | PostgreSQL 개인정보·이미지 경로 부재 | `PASSED` | 6개 컬럼 whitelist 검증 SQL |
| 17 | Spring PostgreSQL 직접 연결 부재 | `PASSED` | main 설정·의존성 정적 검색 |
| 18~21 | request/command/event/결제 승인 멱등성 | `PASSED` | MySQL 순차·2-thread 테스트 10개 |
| 22~25 | 슬롯 동시 배정·Rollback·반납 재시도 | `PASSED` | MySQL `SKIP LOCKED` 동시 테스트 6개 |
| 26~31 | 장치·AI·Snapshot Mock 및 금지 Payload | `PASSED` | fixture/계약 테스트 18개 |
| 32~35 | Seed 재실행·집계·조회·prod 차단 | `PASSED` | Testcontainers MySQL 조회 테스트 9개, main Seed 경로 없음 |
| 36 | Spring 두 번째 Orin DataSource | `PASSED` | PostgreSQL URL·driver·두 번째 DataSource 없음 |
| 37 | Pi SQLite·MySQL·Redis | `PASSED` | `ai/.gitkeep`, `iot/.gitkeep` 외 구현 없음 |
| 38 | `processStatus`·`process_status` | `PASSED` | main·Migration·fixture 0건, 거절 테스트 통과 |
| 39 | 실제 Secret·운영 주소 | `PASSED` | 추적 파일에는 빈 예제·가짜 test 값·localhost만 존재 |
| 40 | 얼굴 이미지·embedding 로그 | `PASSED` | main/AI/IoT 로그 출력 패턴 0건 |

로컬 필수 재검증은 40/40, 즉 100%다. 전체 인수는 아래 4개 비로컬
게이트가 남아 있어 40/44, 90.9%로 기록한다.

## 4. 빌드와 테스트

### 실행 결과

`backend`에서 JDK 21을 현재 프로세스에 지정한 뒤 루트 Wrapper로
`.\gradlew.bat clean test --no-daemon`과
`.\gradlew.bat bootJar --no-daemon`을 실행했다.

- 결과: 두 명령 모두 `BUILD SUCCESSFUL`
- suite: 7
- test: 62
- failure: 0
- error: 0
- skipped: 0
- Testcontainers DB: `mysql:8.0`

| Test suite | 테스트 | 결과 | 핵심 범위 |
| --- | ---: | --- | --- |
| `BillisanApplicationTests` | 6 | 6 passed | Context, 빈 DB V1~V18, 재실행, 12개 물리 테이블, 제약, 금지 컬럼 |
| `DeviceContractFixtureTest` | 18 | 18 passed | Heartbeat, 센서, 잠금, AI, Snapshot, Payload 금지 |
| `FaceProfileSyncOperationEntityTest` | 4 | 4 passed | FACE_PROFILE 동기화 작업 상태 전이 |
| `IdempotencyServiceTests` | 10 | 10 passed | 순차·동시 request/command/event/payment, Rollback |
| `InventoryQueryRepositoryTests` | 9 | 9 passed | Seed 2회, 재고·관리자 집계, 목록, 인덱스 |
| `SlotAllocationServiceTests` | 6 | 6 passed | 1:N 경합, N:N, N:N+1, Rollback, 반납 재시도 |
| `BootSnapshotReconciliationServiceTests` | 9 | 9 passed | 일치·불일치·unknown·누락·중복 Snapshot |

비활성화하거나 제외한 테스트는 없다.

## 5. 데이터베이스 검증

### MySQL

Testcontainers가 빈 DB를 생성했고 Flyway V1~V18 전체 적용과 같은 DB의
재실행 0건을 확인했다. 최종 schema version은 `v18`이다.

Flyway history를 제외한 물리 테이블은 12개다. 일반 업무·계정 테이블
11개와 내부 trigger guard 1개로 구분한다.

`admin_account`, `damage_inspection`, `device_operation`,
`face_profile_sync_operation`, `payment_attempt`, `rental`,
`return_attempt`, `settlement`, `slot`, `station`, `user_account`,
`settlement_payment_mutation_guard`

| 제약 | 실제 검증 |
| --- | --- |
| `return_attempt.request_id` | UNIQUE |
| `return_attempt.rental_id` | 비고유 `(rental_id, status)` 인덱스, UNIQUE 없음 |
| `damage_inspection.return_attempt_id` | UNIQUE 및 FK |
| `device_operation.command_id` | UNIQUE |
| `device_operation.event_id` | UNIQUE |
| `payment_attempt.toss_order_id` | UNIQUE |
| `payment_attempt.toss_payment_key` | UNIQUE |
| 같은 `rental_id` 재시도 | 실패 시도 뒤 두 번째 `return_attempt` 생성 성공 |
| 얼굴·vector 저장 | 이미지·embedding·vector 컬럼 0 |
| 반납 상태 이름 | `status`, legacy `process_status` 계열 0 |

로컬 Compose의 `mysql:8.0` 컨테이너도 `healthy`이고 서버 바이너리는
MySQL `8.0.46`이다.

### PostgreSQL+pgvector

| 항목 | 실제 검증 |
| --- | --- |
| 모드 | `docker-local` |
| image | `mirror.gcr.io/pgvector/pgvector:0.8.5-pg17-bookworm` + 확정 digest |
| 서버 | PostgreSQL `17.10` |
| 상태 | `healthy`, `127.0.0.1:5432` |
| Extension | `SELECT extname ...` 결과 `vector` |
| Migration 재실행 | Extension·table이 이미 존재하면 안전하게 skip |
| `face_profile` | 6개 whitelist 컬럼 |
| `user_ref` | PostgreSQL `UUID`, UNIQUE, Cross-DB FK 없음 |
| `embedding` | 차원 미지정 `vector`, NOT NULL |
| 금지 정보 | 이름·학번·이메일·이미지·경로 컬럼 0 |
| Spring 접근 | PostgreSQL driver/URL/Entity/Repository/DataSource 없음 |

## 6. 멱등성·동시성·Snapshot·조회 판정

- 동일 식별자와 동일 payload는 저장된 결과를 재사용하고, 다른 payload는
  `IdempotencyConflictException`으로 거절한다.
- requestId, commandId, eventId, 결제 승인 ID의 2-thread 테스트에서
  `APPLIED` 1건과 `REPLAYED` 1건만 발생했다.
- 슬롯은 대여소와 후보 상태를 만족하는 1행만
  `FOR UPDATE SKIP LOCKED`로 잠근다. 슬롯 1개와 요청 2개의 경합은 성공
  1건, 명시적 충돌 1건이었다.
- FK 예외 뒤 슬롯은 `AVAILABLE`로 Rollback되며, 실패한 반납 뒤 같은
  rentalId로 새 returnAttempt를 만들 수 있다.
- 일치 Snapshot은 기존 업무 상태를 유지한다. 물리 상태가 다르면
  자동 정상화하지 않고 슬롯 `ADMIN_REVIEW`, 연결 반납
  `RECOVERY_REQUIRED`로 전환한다.
- test-only Seed는 사용자 2명, 대여소 1개, 슬롯 5개, 대여·반납·장치
  시나리오를 만들며 두 번째 실행에도 Row 수가 누적되지 않는다.
- 재고 집계는 전체 5, 대여 가능 1, 대여 중 1, 반납 중 1, 관리자 확인 1을
  DB에서 계산한다. 관리자 집계는 활성 대여 1, 실패 반납 1, 완료 반납 1,
  장치 명령 3, 장치 이벤트 2다.

## 7. 실패·차단·주의 항목

### 실패

애플리케이션 빌드, 테스트, Compose config, 컨테이너 healthcheck,
pgvector Migration/verification 실패는 없다.

감사 중 로컬 MySQL에 별도 `docker compose exec ... mysql -e` 한 줄
메타데이터 조회를 시도했으나 PowerShell에서 Linux shell로 전달하는
인용부호가 깨져 SQL syntax exit `1`이 발생했다. 이 명령 결과는 판정
근거로 사용하지 않았고, 같은 메타데이터 검증은 실제 MySQL 8.0
Testcontainers JUnit에서 성공했다.

PowerShell 시작 시 로컬 `profile.ps1` 실행 정책 경고가 표시되지만
후속 명령의 exit code에는 영향을 주지 않았다. Runbook은
`powershell.exe -NoProfile -ExecutionPolicy Bypass`를 사용한다.

### 외부·팀 결정 차단

1. 운영 Controller/DTO와 HTTP 200/409·오류 응답 계약이 없어 실제 서버
   HTTP 요청은 검증하지 못했다.
2. 실물 Pi, deviceId↔stationId 프로비저닝, 장치 인증과 물리 명령 발행은
   fixture·DB 경계까지만 검증했다.
3. Toss sandbox/운영 승인 호출이 없어 외부 결제 Side Effect의 exactly-once
   결과는 검증하지 못했다.
4. embedding 차원, 거리 함수, vector 인덱스 및 모델 교체 정책이 미확정이다.

상세 결정 목록은
`docs/backend2-setup/BACKEND2-TEAM-DECISIONS-REQUIRED.md`에 기록한다.

## 8. Secret 및 금지 구조 검사

- 추적되는 env 파일은 `.env.example` 두 개뿐이며 실제 Secret 대신
  비민감 placeholder만 있다.
- `backend/.env`와 `infra/orin-postgres/.env`는 각각
  `.gitignore`에 의해 제외된다. 파일 내용은 감사 출력에 노출하지 않았다.
- test의 `billisan_test_password`와 HTTP Client의
  `mock-device-token`은 명시적인 가짜 값이다.
- 추적된 주소는 localhost, `127.0.0.1`, 공개 Gradle 문서 URL뿐이다.
- Spring main에는 MySQL DataSource 하나만 있다.
- AI/IoT 경로는 `.gitkeep` 외 구현이 없어 Pi SQLite·MySQL·Redis가 없다.
- main/AI/IoT에서 얼굴 이미지·embedding 로그 출력 패턴이 없다.
- main·Migration·fixture에 `processStatus` 또는 `process_status`가 없다.

## 9. 실행한 주요 명령

```powershell
git status --short --branch
git branch --show-current
git rev-parse HEAD
git rev-parse origin/develop
git merge-base --is-ancestor origin/develop HEAD
git rev-list --left-right --count origin/develop...HEAD
git log --oneline --no-merges origin/develop..HEAD
git diff --name-status origin/develop...HEAD

$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
java -version
javac -version
Push-Location .\backend
.\gradlew.bat --version
.\gradlew.bat clean test --no-daemon
.\gradlew.bat bootJar --no-daemon
Pop-Location

docker version
docker compose version
docker compose -f backend/docker-compose.yml config --quiet
docker compose -f backend/docker-compose.yml ps
docker compose -f infra/orin-postgres/compose.yaml config --quiet
docker compose -f infra/orin-postgres/compose.yaml ps
docker compose -f backend/docker-compose.yml exec -T db mysql --version
docker compose -f infra/orin-postgres/compose.yaml exec -T orin-postgres postgres --version

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-apply.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-verify.ps1

rg -n -i "jdbc:postgresql|r2dbc:postgresql|org\.postgresql|pgvector|postgresql|datasource" ...
rg -n -i "processStatus|process_status" ...
rg -n -i "(logger|log\.|print|println).*(face|image|embedding)" ...
rg -n -i "password|secret|token|api[_-]?key|authorization|bearer" ...
git ls-files "*env*"
git check-ignore -v backend/.env infra/orin-postgres/.env
```

JUnit XML은 `backend/build/test-results/test/TEST-*.xml`에서
suite, test, failure, error, skipped 합계를 다시 집계했다.

## 10. 변경 파일 전체 목록

현재 변경 목록은 중첩 프로젝트 제거에 따른 삭제와 루트 `backend`로의
이동을 함께 포함한다. 파일 수는 이동 감지·커밋 상태에 따라 달라지므로
MR 직전 `git status --short`와 `git diff --name-status` 결과를 권위로
사용한다.

### Spring·빌드·로컬 MySQL

- `backend/.env.example`
- `backend/.gitattributes`
- `backend/.gitignore`
- `backend/build.gradle`
- `backend/docker-compose.yml`
- `backend/gradle/wrapper/gradle-wrapper.jar`
- `backend/gradle/wrapper/gradle-wrapper.properties`
- `backend/gradlew`
- `backend/gradlew.bat`
- `backend/scripts/db-logs.ps1`
- `backend/scripts/db-start.ps1`
- `backend/scripts/db-status.ps1`
- `backend/scripts/db-stop.ps1`
- `backend/settings.gradle`
- `backend/src/main/java/com/ssafy/billisan/BillisanApplication.java`
- `backend/src/main/resources/application-local.yaml`
- `backend/src/main/resources/application.yaml`

### MySQL Migration·업무 구현·테스트

- `backend/src/main/resources/db/migration/V1__create_billisan_schema.sql`
- `backend/src/main/resources/db/migration/V2__add_idempotency_and_concurrency_constraints.sql`
- `backend/src/main/resources/db/migration/V3__align_return_slot_authority.sql`
- `backend/src/main/resources/db/migration/V4__add_damage_settlement_basis.sql`
- `backend/src/main/resources/db/migration/V5__add_amount_and_time_safety.sql`
- `backend/src/main/resources/db/migration/V6__add_rental_failure_summary.sql`
- `backend/src/main/resources/db/migration/V7__add_damage_inspection_processing_time.sql`
- `backend/src/main/resources/db/migration/V8__standardize_uuid_and_idempotency_key_types.sql`
- `backend/src/main/resources/db/migration/V9__standardize_varchar_and_enum_storage.sql`
- `backend/src/main/resources/db/migration/V10__add_ai_and_amount_consistency_checks.sql`
- `backend/src/main/resources/db/migration/V11__map_internal_and_toss_payment_statuses.sql`
- `backend/src/main/resources/db/migration/V12__replace_deprecated_binary_payment_status_cast.sql`
- `backend/src/main/resources/db/migration/V13__use_student_number_and_opaque_user_ref.sql`
- `backend/src/main/resources/db/migration/V14__enforce_cross_flow_integrity.sql`
- `backend/src/main/resources/db/migration/V15__block_nested_student_identity_evidence_keys.sql`
- `backend/src/main/resources/db/migration/V16__protect_payment_ledger_and_settlement_totals.sql`
- `backend/src/main/resources/db/migration/V17__freeze_settled_damage_review_audit.sql`
- `backend/src/main/resources/db/migration/V18__make_damage_settlement_basis_irreversible.sql`
- `backend/src/main/resources/db/legacy-erd-v2_1/V1__create_mysql_business_schema.sql`~`ARCHIVED_V4__align_schema_with_erd_v2_1.sql` (비실행 보관 자료)
- `backend/src/main/resources/db/reference/billisan_backend3_p0_p1_final_clean_schema.sql`
- `backend/src/main/java/com/ssafy/billisan/faceprofilesync/**`
- `backend/src/main/java/com/ssafy/billisan/idempotency/IdempotencyConflictException.java`
- `backend/src/main/java/com/ssafy/billisan/idempotency/IdempotencyRepository.java`
- `backend/src/main/java/com/ssafy/billisan/idempotency/IdempotencyService.java`
- `backend/src/main/java/com/ssafy/billisan/inventory/InventoryQueryRepository.java`
- `backend/src/main/java/com/ssafy/billisan/slot/SlotAllocationRepository.java`
- `backend/src/main/java/com/ssafy/billisan/slot/SlotAllocationService.java`
- `backend/src/main/java/com/ssafy/billisan/slot/SlotUnavailableException.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotPolicy.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotReconciliationService.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotRejectedException.java`
- `backend/src/main/java/com/ssafy/billisan/snapshot/BootSnapshotRepository.java`
- `backend/src/test/java/com/ssafy/billisan/BillisanApplicationTests.java`
- `backend/src/test/java/com/ssafy/billisan/devicecontract/DeviceContractFixtureTest.java`
- `backend/src/test/java/com/ssafy/billisan/faceprofilesync/FaceProfileSyncOperationEntityTest.java`
- `backend/src/test/java/com/ssafy/billisan/idempotency/IdempotencyServiceTests.java`
- `backend/src/test/java/com/ssafy/billisan/inventory/Backend2QueryScenarioFixture.java`
- `backend/src/test/java/com/ssafy/billisan/inventory/InventoryQueryRepositoryTests.java`
- `backend/src/test/java/com/ssafy/billisan/slot/SlotAllocationServiceTests.java`
- `backend/src/test/java/com/ssafy/billisan/snapshot/BootSnapshotReconciliationServiceTests.java`

### 장치 계약 fixture

- `backend/src/test/resources/device-contract/device-gateway.http`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-lock-mismatch.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-match.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-mismatch.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-partial.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-server-baseline.json`
- `backend/src/test/resources/device-contract/v1/boot-snapshot-unknown-slot.json`
- `backend/src/test/resources/device-contract/v1/damage-result.json`
- `backend/src/test/resources/device-contract/v1/face-auth-failure.json`
- `backend/src/test/resources/device-contract/v1/face-auth-success.json`
- `backend/src/test/resources/device-contract/v1/heartbeat-normal.json`
- `backend/src/test/resources/device-contract/v1/lock-failure.json`
- `backend/src/test/resources/device-contract/v1/lock-success.json`
- `backend/src/test/resources/device-contract/v1/slot-sensor-empty.json`
- `backend/src/test/resources/device-contract/v1/slot-sensor-occupied.json`

### PostgreSQL+pgvector

- `infra/orin-postgres/.env.example`
- `infra/orin-postgres/.gitignore`
- `infra/orin-postgres/compose.yaml`
- `infra/orin-postgres/migrations/001_enable_vector_extension.sql`
- `infra/orin-postgres/migrations/002_create_face_profile.sql`
- `infra/orin-postgres/scripts/OrinPostgres.Common.ps1`
- `infra/orin-postgres/scripts/pgvector-apply.ps1`
- `infra/orin-postgres/scripts/pgvector-verify.ps1`
- `infra/orin-postgres/verification/verify_face_profile.sql`

### 문서

- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/DEVICE-CONTRACT-DRAFT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/BACKEND2-SETUP-FINAL-REPORT.md`
- `docs/backend2-setup/BACKEND2-LOCAL-RUNBOOK.md`
- `docs/backend2-setup/BACKEND2-TEAM-DECISIONS-REQUIRED.md`
- `docs/backend2-setup/reports/STEP-01-WORKSPACE-BOOTSTRAP.md`
- `docs/backend2-setup/reports/STEP-02-REPOSITORY-AUDIT.md`
- `docs/backend2-setup/reports/STEP-03-SPRING-LOCAL-BASELINE.md`
- `docs/backend2-setup/reports/STEP-04-LOCAL-DATA-INFRA.md`
- `docs/backend2-setup/reports/STEP-05-MYSQL-MIGRATION.md`
- `docs/backend2-setup/reports/STEP-06-PGVECTOR-MIGRATION.md`
- `docs/backend2-setup/reports/STEP-07-DEVICE-CONTRACT-MOCK.md`
- `docs/backend2-setup/reports/STEP-08-IDEMPOTENCY.md`
- `docs/backend2-setup/reports/STEP-09-SLOT-CONCURRENCY.md`
- `docs/backend2-setup/reports/STEP-10-BOOT-SNAPSHOT-RECONCILIATION.md`
- `docs/backend2-setup/reports/STEP-11-SEED-AND-QUERY-VALIDATION.md`
- `docs/backend2-setup/reports/STEP-13-ERD-V2-1-ALIGNMENT.md`
- `docs/backend2-setup/reports/STEP-14-BACKEND3-P0-P1-SCHEMA.md`

## 11. Jira Task별 변경 파일 매핑

현재 작업은 Jira `S15P11A201-55` 하나에 귀속된다.

| 작업 묶음 | 주요 파일 |
| --- | --- |
| Spring local baseline | `build.gradle`, Wrapper, `application*.yaml`, `.env.example` |
| Local database infrastructure | `backend/docker-compose.yml`, `backend/scripts/db-*.ps1` |
| MySQL migration constraints | `backend/src/main/resources/db/migration` V1~V18, `BillisanApplicationTests` |
| pgvector migration | `infra/orin-postgres/**` |
| Device contract mocks | `DEVICE-CONTRACT-DRAFT.md`, HTTP/JSON fixture, 계약 테스트 |
| Idempotency tests | `idempotency/**`, `IdempotencyServiceTests` |
| Slot concurrency tests | `slot/**`, V2, `SlotAllocationServiceTests` |
| Snapshot reconciliation | `snapshot/**`, V3, Snapshot fixture/test |
| Local seed and query tests | `inventory/**`, fixture/query 테스트 |
| 구축 문서 | CONTEXT, PROGRESS, STEP-01~11, 최종 감사 문서 3개 |

다른 Jira 이슈 키로 귀속해야 할 파일이 있다면 MR 생성 전에 소유권을
재분류해야 한다.

## 12. 추천 Commit 분리안

Commit은 실행하지 않았다. 권장 순서는 다음과 같다.

1. `S15P11A201-55 chore: Spring local baseline 구성`
2. `S15P11A201-55 chore: MySQL 8.0 local database infrastructure 구성`
3. `S15P11A201-55 feat: Flyway MySQL migration constraints 추가`
4. `S15P11A201-55 chore: pgvector migration 검증 환경 추가`
5. `S15P11A201-55 test: Device contract mock 추가`
6. `S15P11A201-55 test: MySQL 멱등성 시나리오 추가`
7. `S15P11A201-55 test: 슬롯 동시성 시나리오 추가`
8. `S15P11A201-55 feat: 부팅 Snapshot 안전 조정 구현`
9. `S15P11A201-55 test: 로컬 Seed와 조회 집계 검증 추가`
10. `S15P11A201-55 docs: Backend 2 최종 감사와 로컬 Runbook 정리`

현재 브랜치에는 STEP-03~11에 대응하는 11개 기존 Commit이 이미 있으므로,
실제 MR에서는 무조건 재작성하지 말고 현재 이력이 리뷰 가능한지 먼저
확인한다.

## 13. 롤백 방법

- 애플리케이션·Migration 변경은 공유 브랜치에서 `reset --hard`하지 않고,
  병합된 Commit을 역순으로 `git revert <commit>`한다.
- 로컬 MySQL과 PostgreSQL 중지는 Runbook의 stop 명령을 사용한다.
  기본 stop은 named volume을 보존한다.
- Flyway V1~V18에는 down Migration이 없다. 운영 DB 롤백은 새 보정
  Migration 또는 검증된 백업 복원 절차로 수행하며 적용된 history를
  수동 삭제하지 않는다.
- 로컬 DB 전체 초기화는 데이터 손실 작업이다. 기본 스크립트에 포함하지
  않았으며 백업과 대상 Compose project 확인 뒤 사용자가 직접 승인해야 한다.
- 아직 Commit하지 않은 최종 감사 문서는 별도 백업 후 해당 파일만
  제거하고, `PROGRESS.md`는 `git restore --` 대상 파일을 정확히 지정해
  복구할 수 있다. `git clean`은 사용하지 않는다.

## 14. MR 대상과 리뷰 체크리스트

- source: `S15P11A201-55-be-db`
- target: `develop`
- Commit/Push/MR: 이번 단계 미수행

MR 전에 반드시 확인한다.

- [ ] `origin/develop` fetch 후 충돌과 ahead/behind 재확인
- [ ] 팀 결정 문서의 Blocker 소유자와 마감일 지정
- [ ] JDK 21에서 `clean build --rerun-tasks` 재통과
- [ ] Docker 사용 가능한 리뷰 환경에서 MySQL/pgvector 검증 재통과
- [ ] `.env`, 실제 비밀번호, 운영 IP/도메인이 diff에 없는지 확인
- [ ] V1~V18과 pgvector SQL을 DBA 또는 데이터 소유자가 리뷰
- [ ] 운영 Controller가 추가될 경우 200/409와 인증 계약 테스트 추가
- [ ] 실물 Pi·Toss 검증 결과를 별도 인수 증거로 첨부
- [ ] embedding 차원 확정 전 `VECTOR(n)`·vector index를 추가하지 않음
- [ ] `processStatus`·`process_status`, Spring PostgreSQL DataSource,
      Pi DB, 얼굴 이미지 Payload/로그가 재도입되지 않았는지 재검색
- [ ] `git diff --check`와 전체 변경 파일 소유권 확인

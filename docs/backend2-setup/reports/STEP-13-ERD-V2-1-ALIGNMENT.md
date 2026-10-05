# STEP-13 — ERD v2.1 Alignment

## 1. 단계 판정

`PASSED`

팀 결정에 따라 Notion `08 Billisan ERD v2.1`을
`RELEASED / CONFIRMED` 기준선으로 채택했다. 기존 코드를 이 기준에
맞게 변경했고 JDK 21 컴파일, Docker 기반 MySQL 전체 테스트,
기존 로컬 DB 승격, PostgreSQL 003 적용·재적용 검증을 완료했다.

## 2. 권위 기준

| 항목 | 값 |
| --- | --- |
| 문서 | `08 Billisan ERD v2.1` |
| Notion | `https://app.notion.com/p/2db443b01b138220904d01f4677e132b` |
| 상태 | 팀 결정에 따른 `RELEASED / CONFIRMED` |
| 작업 브랜치 | `S15P11A201-55-be-db` |
| 기준 브랜치 | `develop` |
| 점검 HEAD | `af79d483e558f8a72cd6b2d34086f6e12df12b4c` |

ERD v1.0.1 기준 STEP-05~12 보고서는 당시 검증 기록으로 보존한다.
스키마의 현재 사실이 충돌할 때는 이 보고서와 CONTEXT의 STEP-13 절을
우선한다.

## 3. ERD 비교 및 반영 결과

| 영역 | 기존 브랜치 상태 | ERD v2.1 기준 | 반영 |
| --- | --- | --- | --- |
| MySQL 테이블 | 업무 테이블 9개 | 동기화 작업 테이블 포함 10개 | V4에서 `face_profile_sync_operation` 생성 |
| 사용자 | `account_status`, 로그인 ID 중심 | `password_hash`, `name` 필수, 상태 제거 | V4 추가·이관·제거 |
| 대여소 boot | snapshot hash·recovery 컬럼 | `current_boot_id`, `boot_synced_at` | V4와 Snapshot Repository 교체 |
| 슬롯 모델 | `RENTING`, `RETURNING` 등 업무 진행 상태 혼재 | 물리 condition/service/occupancy/lock만 저장 | V4 상태 변환, Repository에서 업무 상태 파생 |
| 대여 기한 | `due_at NOT NULL` | `REQUESTED` 동안 NULL 허용 | V4 및 배정 서비스 수정 |
| 반납 슬롯 | `return_slot_id NOT NULL` | 물리 완료 전 NULL 허용 | V4·서비스·테스트 수정 |
| 파손 검사 | `updated_at` 없음 | `updated_at NOT NULL` | V4 추가·backfill |
| 정산 | 총 금액만 저장 | `paid_amount` 및 금액 불변식 | V4·결제 승인 갱신 |
| 결제 시도 | 생성 멱등 키·`PROCESSING` 없음 | `creation_request_id` UNIQUE, 갱신 시각, `PROCESSING` | V4·Repository·fixture 수정 |
| 장치 작업 | command/event 결과만 저장 | issued boot, terminal event와 물리 증거, 결과 미확정 상태 | V4·서비스·fixture 계약 수정 |
| PostgreSQL | `face_profile` 9개 컬럼, surrogate PK | `user_ref` PK의 6개 컬럼 | PostgreSQL 003과 검증 SQL 수정 |
| Spring 영속성 | JDBC 전용 | JPA 기본, Migration 스키마 validate | JPA 추가, 동기화 Entity/Repository 구현 |

## 4. 물리 규칙

### 4.1 MySQL

- 테이블·컬럼: `lower_snake_case`
- UUID: `CHAR(36) CHARACTER SET ascii COLLATE ascii_bin`
- 업무 시간: `DATETIME(6)`, 애플리케이션 기준 `UTC`
- 금액: `BIGINT UNSIGNED`
- 사용자별 얼굴 프로필 동기화 요청:
  `request_id` UNIQUE, `(user_id, sync_status)` 인덱스
- 슬롯 물리 불변식:
  - `EMPTY` occupancy이면 item condition도 `EMPTY`
  - `OCCUPIED`이면 item condition은 `EMPTY`가 아님
  - `UNKNOWN` occupancy이면 item condition도 `UNKNOWN`
  - `AVAILABLE`은 `EMPTY` 또는 `NORMAL`만 허용
- 대여·반납 처리 상태는 슬롯 컬럼이 아니라 `rental`,
  `return_attempt`, `device_operation` 행으로 판정
- 장치 terminal 성공·실패는 terminal event와 관측 물리 증거 없이는
  확정하지 않음

### 4.2 PostgreSQL

- `face_profile.user_ref UUID`가 PK이며 MySQL `user_id`의 논리 참조
- Cross-DB FK, 개인정보 복제, 이미지 경로 저장 없음
- `embedding VECTOR`는 차원을 지정하지 않음
- 차원·거리 함수·threshold·HNSW/IVFFlat 여부가 확정될 때 별도
  forward-only Migration으로 변경

## 5. 데이터 이관 정책

| 대상 | 정책 | 운영 전 확인 |
| --- | --- | --- |
| 기존 사용자 이름 | `login_id`를 임시 `name`으로 backfill | 사용자 프로필 원본으로 정정 |
| 기존 비밀번호 | `{migration-disabled}`로 backfill | 공식 비밀번호 재설정 필수 |
| 기존 슬롯 상태 | v2.1 허용 조합으로 보수적 변환 | ADMIN_REVIEW/OUT_OF_SERVICE 목록 검토 |
| 기존 결제 생성 키 | `legacy-{payment_attempt_id}` | 외부 Toss 원장과 대사 |
| 기존 장치 boot | station boot 또는 `legacy-{operation_id}` | 실제 Pi boot 동기화 |
| 증거 없는 기존 terminal 결과 | `OUTCOME_UNKNOWN` | 실물·운영 로그로 조정 |
| 기존 얼굴 등록 시각 | `updated_at`으로 backfill | Orin 원장과 대사 |

이관용 password 값은 로그인 가능한 공통 비밀번호가 아니며 Secret도
아니다. 임의 비밀번호 생성보다 계정 사용을 차단하고 재설정을 강제하는
방식이 안전하다.

## 6. 코드 변경

### 6.1 얼굴 프로필 동기화

`com.ssafy.billisan.faceprofilesync`에 JPA Entity, Repository, Service를
추가했다.

- `REGISTER`, `REREGISTER`, `DELETE`
- `REQUESTED → PROCESSING → SUCCEEDED/FAILED`
- 결과가 불명확하면 `RECONCILIATION_REQUIRED`
- 같은 `requestId`와 같은 내용은 기존 결과 재사용
- 성공 확정과 MySQL `face_registered` projection 갱신은 한 Transaction
- 성공 완료된 작업을 늦은 실패 메시지가 덮어쓰지 못함

### 6.2 슬롯·Snapshot·장치 작업

- 슬롯 후보에 `FOR UPDATE SKIP LOCKED`를 유지하되 슬롯에
  `RENTING`/`RETURNING`을 기록하지 않는다.
- 배정 가능 여부는 진행 중 rental/return/device operation 존재 여부로
  계산한다.
- 새 Pi boot Snapshot이 이전 boot의 미완료 명령을 발견하면
  `OUTCOME_UNKNOWN`으로 전환한다.
- Snapshot 불일치는 자동 정상화하지 않고 station/slot을 보수적인
  확인 불가 상태로 바꾼다.
- terminal event fixture는 command/boot/event와 관측 물리 증거를
  모두 전달한다.

## 7. 실행 결과

### 7.1 통과

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat compileTestJava --no-daemon
```

결과: `BUILD SUCCESSFUL`

```powershell
.\gradlew.bat test `
  --tests com.ssafy.billisan.faceprofilesync.FaceProfileSyncOperationEntityTest `
  --tests com.ssafy.billisan.devicecontract.DeviceContractFixtureTest `
  --rerun-tasks --no-daemon
```

결과: `BUILD SUCCESSFUL`, 22개 테스트 통과.

### 7.2 Docker 기반 전체 테스트

```powershell
.\gradlew.bat clean test --rerun-tasks --no-daemon
```

첫 실행에서 실제 회귀 실패 2건을 발견했다.

- 동시 `commandId` upsert 뒤 REPEATABLE READ snapshot으로 기존 행을
  읽지 못하는 문제
- slot 단위 Snapshot 복구를 station 단위 복구로 잘못 보고하는 문제

각각 upsert 후 locking read 사용과 station 복구 판정 기준 분리로
최소 수정했다. 관련 테스트를 먼저 재실행한 뒤 전체 테스트를 다시
실행했다.

최종 결과: `BUILD SUCCESSFUL`, 61개 테스트, 실패·오류·skip 0.

## 8. Docker 런타임 재검증 결과

```powershell
docker version
docker compose version

Set-Location C:\Users\SSAFY\S15P11A201\backend
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\db-start.ps1
docker compose ps

$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat clean test --rerun-tasks --no-daemon

Set-Location C:\Users\SSAFY\S15P11A201
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-apply.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-apply.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-verify.ps1
```

| 검증 | 실제 결과 |
| --- | --- |
| Docker | Desktop `4.83.0`, Engine/CLI `29.6.2`, Compose `5.3.1` |
| MySQL | `8.0.46`, `utf8mb4`, `utf8mb4_0900_ai_ci`, `+00:00`, healthy |
| 로컬 MySQL 승격 | 기존 V1에서 V2~V4 3건 적용 성공 |
| MySQL 재기동 | 현재 V4, `No migration necessary` |
| JPA 검증 | 두 기동 모두 schema validation 통과 |
| MySQL 테이블 | 업무 테이블 정확히 10개 |
| 필수 인덱스 | 반납 request UNIQUE·rental 비고유, face sync·payment 생성 키 UNIQUE |
| 금지 컬럼 | MySQL image·embedding·vector 0개, legacy `account_status` 0개 |
| 전체 테스트 | 61개, 실패·오류·skip 0 |
| PostgreSQL | `17.10`, healthy |
| pgvector | Extension `0.8.5` |
| PostgreSQL 재적용 | 001~003 연속 2회 성공 |
| `face_profile` | 정확히 6개 컬럼, `user_ref` PK, FK 없음 |

## 9. 추후 트러블슈팅으로 남긴 항목

| 항목 | 현재 상태 | 종료 증거 |
| --- | --- | --- |
| embedding 규격 | `BLOCKED` | Backend 1 모델 차원·metric·threshold 결정서 |
| 기존 사용자 프로필 보정 | `PROVISIONAL` | 이름 원본 대사와 비밀번호 재설정 완료 |
| 기존 terminal 장치 작업 대사 | `PROVISIONAL` | Pi/운영 로그 기반 `OUTCOME_UNKNOWN` 해소 |
| EC2·MQTT 운영 배치 | `OUT_OF_SCOPE` | 인프라/Embedded 계약과 배포 승인 후 별도 작업 |

## 10. 변경 파일 범위

- MySQL: Flyway V4, 스키마 검증 테스트, 관련 fixture
- PostgreSQL: Migration 003, 검증 SQL
- Spring: JPA 기준, FACE_PROFILE 동기화, 멱등성·슬롯·Snapshot·조회 정렬
- 계약: 장치 terminal event fixture 및 문서
- 문서: CONTEXT, PROGRESS, Runbook, 본 보고서

사용자가 작성한
`BACKEND2-OPERATIONS-ACCEPTANCE-BLOCKER-COMMUNICATION-PLAN.md`는
이번 구현에서 수정하지 않았다.

## 11. 추천 Commit 메시지

```text
feat: align backend data model with confirmed ERD v2.1
```

DB Migration과 애플리케이션 정렬을 분리한다면:

```text
feat: add ERD v2.1 forward-only database migrations
refactor: align backend workflows with ERD v2.1 state model
test: update contract and persistence fixtures for ERD v2.1
docs: record confirmed ERD v2.1 baseline and verification gaps
```

# Billisan Backend 2 개발환경 구축 진행 상태

## 전체 단계

| 단계 | 설명 | 상태 | 비고 |
| --- | --- | --- | --- |
| STEP-01 | Workspace Bootstrap | `PASSED` | 저장소, 브랜치, 도구 및 후속 작업 컨텍스트 확인 완료 |
| STEP-02 | Repository Audit | `PASSED` | 현재 기준선, 미병합 후보, 도입 필요 항목 및 충돌 식별 완료 |
| STEP-03 | Spring Local Baseline | `PASSED` | JDK 21, Wrapper, local Profile, Secret 분리 및 기본 빌드·테스트 검증 완료 |
| STEP-04 | Local Data Infrastructure | `PASSED` | Docker Compose와 MySQL 8.0 기동·Healthcheck 검증 완료 |
| STEP-05 | MySQL Migration | `PASSED` | Flyway V1, MySQL 업무 테이블 9개, 재실행·메타데이터·재시도 검증 완료 |
| STEP-06 | PostgreSQL+pgvector Migration | `PASSED` | 동일 digest 공식 cache로 CDN 오류 우회, apply 2회·Extension·메타데이터·경계 검증 완료 |
| STEP-07 | Device Contract Mock | `PASSED` | CONFIRMED Edge 경로, HTTP·JSON Mock 10종 및 독립 계약 테스트 15개 검증 완료 |
| STEP-08 | API·장치 작업 멱등성 | `PASSED` | MySQL UNIQUE·행 잠금·조건부 갱신과 순차·2-thread 동시성·롤백 테스트 10개 검증 완료 |
| STEP-09 | Slot Concurrency | `PASSED` | 후보 슬롯 행 SKIP LOCKED, 조건부 전이, 동시 배정·롤백·반납 재시도 테스트 6개 검증 완료 |
| STEP-10 | Boot Snapshot Reconciliation | `PASSED` | MySQL 상태 비교, 슬롯·대여소 복구 분리, bootId 멱등성 및 통합 테스트 9개 검증 완료 |
| STEP-11 | Seed and Query Validation | `PASSED` | test-only fixture 재실행 안전성, 재고·관리자 집계·목록 및 인덱스 테스트 9개 검증 완료 |
| STEP-12 | Final Audit and Handover | `PARTIALLY_BLOCKED` | 로컬 필수 재검증 40/40 통과, 운영 HTTP·실물 Pi·Toss·embedding 규격은 외부 결정 대기 |
| STEP-13 | ERD v2.1 Alignment | `PASSED` | 당시 전체 61개 테스트, MySQL V1→V4·재기동 no-op, PostgreSQL 001~003 2회 적용·메타데이터 검증 완료 |
| STEP-14 | Backend 3 P0-P1 Schema Replacement | `PASSED` | 공식 Flyway V1~V18, UTC, 신규 `billisan_p0_p1`, JPA 검증 및 재기동 no-op 확인 |

## STEP-01 체크리스트

- [x] 현재 작업 디렉터리 파일 및 Git 저장소 여부 확인
- [x] 기존 사용자 파일 보존
- [x] 입력 저장소 URL과 기존 `origin` 일치 확인
- [x] `git fetch --all --prune` 완료
- [x] 원격 `develop` 존재 확인
- [x] `develop` Checkout 및 `--ff-only` 최신 동기화
- [x] 접두사 없는 작업 브랜치 `S15P11A201-55-be-db` Checkout
- [x] Git 작업 트리 안전 상태 확인
- [x] CLI 및 확인 가능한 GUI 도구 탐지
- [x] `docs/backend2-setup` 및 `reports` 생성
- [x] STEP-01 컨텍스트와 보고서 작성
- [x] Commit, Push, MR 및 애플리케이션 코드 변경 미수행

## 현재 판정

`PASSED`

차단 사유는 없다. Gradle Wrapper, Docker, Docker Compose 및 일부 GUI 도구가 탐지되지 않았지만 STEP-01은 설치 단계가 아니라 현황 탐지 단계이므로 판정에 영향을 주지 않는다.

## STEP-02 체크리스트

- [x] STEP-01 선행 문서 3개 및 `PASSED` 판정 확인
- [x] 현재 브랜치와 `origin/develop`의 전체 추적 파일 확인
- [x] 프로젝트/멀티모듈/빌드/설정/테스트 진입점 조사
- [x] DB Driver, 영속성, Migration 및 Testcontainers 조사
- [x] MySQL/PostgreSQL/pgvector, Docker/Compose 및 스크립트 조사
- [x] Entity/Repository/Mapper/Gateway/DTO 패키지 조사
- [x] 장치 메시지 식별자, heartbeat, snapshot 및 동시성 제어 조사
- [x] 다중 DataSource와 잘못된 저장 위치 흔적 조사
- [x] 현재 기준선과 미병합 Spring Boot 초기화 후보 분리
- [x] `origin/subPjt_1` 기술조사 문서를 사용자 지시에 따라 근거에서 제외
- [x] 애플리케이션 코드와 설정 무수정
- [x] 감사 보고서와 기술 기준선 갱신

## STEP-02 판정

`PASSED`

현재 작업 브랜치가 placeholder 구조뿐이라는 사실과, Spring Boot 초기화
프로젝트가 별도 미병합 원격 브랜치에만 있다는 충돌을 확인했다. 빌드와
테스트의 현재 진입점은 없으며, 후보 브랜치가 통합될 경우의 진입점은
`backend/gradlew.bat`이다. 이 결과는 감사 실패가 아니라 다음
단계 전에 해소해야 할 기준선 공백으로 판정한다.

## STEP-03 체크리스트

- [x] STEP-01과 STEP-02 `PASSED` 확인
- [x] 기존 원격 Spring Boot 스캐폴드 재사용
- [x] Java Toolchain 21 유지 및 Temurin JDK 21 검증
- [x] 시스템 Gradle 대신 Gradle Wrapper 9.5.1 사용
- [x] 기존 Profile 부재 확인 후 `local` Profile 최소 추가
- [x] MySQL URL/사용자/비밀번호를 환경 변수 참조로 구성
- [x] 실제 소비 코드가 없는 Pi 관련 환경 변수 미추가
- [x] PostgreSQL 두 번째 DataSource 미추가
- [x] `.env.example` 비민감 예제 작성
- [x] 실제 `.env`와 `.env.*` ignore 확인
- [x] `compileJava`, 기본 테스트, local Profile 테스트, `build` 성공
- [x] 테스트 비활성화·삭제 또는 `@Disabled` 미사용
- [x] Commit, Push, MR 미수행

## STEP-03 판정

`PASSED`

기본 빌드와 테스트는 모두 성공했다. 시스템 `JAVA_HOME` 오설정은 명령
세션에서만 JDK 21 경로로 보정했으며 시스템 값을 변경하지 않았다.
Docker/MySQL과 JDBC Driver가 없어 실제 MySQL 연결은 이번 단계의 실행
범위에서 제외했고, 코드 실패로 분류하지 않았다.

## STEP-04 체크리스트

- [x] STEP-01~03 `PASSED` 확인
- [x] 기존 Compose와 개발 스크립트 우선 조사
- [x] MySQL `8.0` 서비스 최소 구성
- [x] 포트, DB명, 사용자, 비밀번호 환경 변수화
- [x] MySQL named volume 및 Healthcheck 구성
- [x] `utf8mb4`, `utf8mb4_0900_ai_ci`, `UTC` 기준 구성
- [x] 실제 비밀번호가 없는 `.env.example` 갱신
- [x] DB 시작·중지·상태·로그 PowerShell 스크립트 작성
- [x] 기본 스크립트에 Volume 삭제 명령 미포함
- [x] 미확정 PostgreSQL 모드를 `skip-until-confirmed`로 안전 처리
- [x] 확인되지 않은 pgvector image 미고정
- [x] Spring에 PostgreSQL URL/DataSource 미추가 확인
- [x] Redis 및 Pi용 DB 미추가 확인
- [x] Compose YAML과 PowerShell 구문 정적 검증
- [x] `docker compose config --quiet` 통과
- [x] MySQL `8.0.46` 기동 및 Healthcheck `healthy`
- [x] 호스트 TCP 3306, 업무 DB 접속, 문자셋·Collation·타임존 확인
- [x] Commit, Push, MR 미수행

## STEP-04 판정

`PASSED`

Compose YAML과 스크립트 정적 검증을 완료했으며, 사용자별 Docker
Desktop을 기동해 MySQL `8.0.46` 컨테이너, Healthcheck, 호스트 3306,
업무 DB 접속과 영속 Volume을 실제 확인했다. `POSTGRES_MODE` 및
`PGVECTOR_IMAGE`가 실제 값으로 치환되지 않아 PostgreSQL+pgvector
실행 구성은 만들지 않았다.

## STEP-05 체크리스트

- [x] STEP-01~04 상태 및 필수 선행 보고서 확인
- [x] 기존 Migration 도구와 파일 재조사
- [x] `MIGRATION_TOOL=flyway` 선택 확인
- [x] Notion ERD v1.0.1의 물리 이름, 제약 및 저장 책임 조사
- [x] Entity, 상태 Enum 및 네이밍 규칙 재조사
- [x] 과거 ERD의 반납 재시도, 상태, 결제 식별자 및 `UMBRELLA` 충돌 해소 확인
- [x] Docker Desktop·Compose와 MySQL 8.0 런타임 확인
- [x] Spring Boot 관리 버전의 Flyway·MySQL JDBC·Testcontainers 도입
- [x] 9개 업무 테이블을 생성하는 Flyway V1 Migration 작성
- [x] UUID·멱등 ID·시간·금액·상태의 MySQL 8.0 물리 규칙 적용
- [x] 필수 FK·UNIQUE·비고유 인덱스·CHECK 적용
- [x] Testcontainers MySQL 8.0 Migration 검증 테스트 추가
- [x] 빈 MySQL DB에 전체 Migration 적용
- [x] 같은 DB에서 Migration 재실행 no-op 확인
- [x] 실제 `information_schema` 인덱스·UNIQUE·FK·CHECK 검증
- [x] 같은 `rental_id`의 실패 후 새 반납 시도 생성 확인
- [x] 검증 데이터 `ROLLBACK` 및 잔존 행 0건 확인
- [x] `process_status`·`processStatus` 부재 확인
- [x] MySQL 이미지·임베딩·vector 컬럼 부재 확인
- [x] PostgreSQL 두 번째 DataSource 미추가
- [x] JPA·Hibernate·MyBatis 및 Entity·Enum 미추가
- [x] 운영 데이터 미사용
- [x] Commit, Push, MR 미수행

## STEP-05 판정

`PASSED`

Flyway V1을 빈 MySQL 8.0 `billisan` DB에 적용해 업무 테이블 9개를
생성했고, 같은 DB의 두 번째 실행이 no-op임을 확인했다.
`information_schema`에서 필수 UNIQUE·비고유 인덱스·FK·CHECK를
확인했으며, 같은 대여의 실패 후 새 반납 시도 2행을 생성한 뒤
`ROLLBACK`하여 검증 데이터가 남지 않았다. Testcontainers MySQL 8.0의
통합 테스트 5개와 최종 Gradle 전체 빌드도 모두 성공했다.

## STEP-06 체크리스트

- [x] CONTEXT, PROGRESS, STEP-02 및 STEP-04 보고서 확인
- [x] STEP-01~05 `PASSED` 확인
- [x] 기존 FACE_PROFILE 스키마·SQL·Python 모델·문서·Migration 검색
- [x] 기존 PostgreSQL 소유 위치 부재 확인
- [x] Notion ERD v1.0.1의 확정 FACE_PROFILE 필드 확인
- [x] Spring·Backend 1 AI 런타임과 분리된 `infra/orin-postgres` 경계 구성
- [x] `CREATE EXTENSION IF NOT EXISTS vector` Migration 작성
- [x] 차원 미지정 `VECTOR` 기반 FACE_PROFILE Migration 작성
- [x] `user_ref UUID` 논리 참조와 Cross-DB FK 금지 적용
- [x] 이름·학번·이메일·얼굴 이미지 경로 미저장
- [x] Spring 설정·Entity·Repository·의존성 무수정
- [x] Compose YAML을 Docker Compose로 정적 검증
- [x] PowerShell 스크립트 구문과 `UNCONFIRMED` 차단 동작 검증
- [x] 공식 pgvector `0.8.5` + PostgreSQL `17` image와 digest 확정
- [x] registry manifest digest와 `linux/amd64`, `linux/arm64` 확인
- [x] `.env.example`에 비민감 확정 image 기록
- [x] PostgreSQL 호스트 포트를 `127.0.0.1`로 제한
- [x] Docker Desktop 재시작 후 image pull 재검증
- [x] 실패한 apply 뒤 컨테이너·volume·network·완성 image 부재 확인
- [x] Docker Desktop proxy와 Containers proxy를 `No proxy`로 전환
- [x] CloudFront 직접 경로의 `EOF` 재현과 네트워크 원인 분리
- [x] `mirror.gcr.io`의 canonical image 동일 index digest 확인
- [x] 동일 digest image 다운로드 성공
- [x] Git ignore된 로컬 `.env` 생성과 비밀번호 추적 제외 확인
- [x] PostgreSQL+pgvector 컨테이너 기동 및 Healthcheck 확인
- [x] `vector` Extension `0.8.5` 실제 조회
- [x] FACE_PROFILE Migration 적용·재실행 no-op 확인
- [x] 실제 FACE_PROFILE 9개 컬럼과 제약 조회
- [x] PostgreSQL `17.10`과 localhost 5432·named volume 확인
- [x] MySQL `user_id CHAR(36) ascii_bin`과 PostgreSQL `user_ref UUID` 비교
- [x] 기존 MySQL 컨테이너 `healthy` 복구

## STEP-06 판정

`PASSED`

Docker Hub CloudFront 직접 경로의 `EOF`는 두 proxy를 비활성화한 뒤에도
재현됐지만, Google의 공식 Docker Hub cache에서 canonical image와 동일한
OCI digest를 확인해 안전하게 다운로드했다. PostgreSQL `17.10` +
pgvector `0.8.5` 컨테이너가 `healthy`이며 apply 2회, Extension,
FACE_PROFILE 9개 컬럼·제약 및 Cross-DB UUID 타입을 실제 검증했다.
Spring 두 번째 DataSource, Cross-DB FK 및 개인정보 컬럼은 없다.

## STEP-07 체크리스트

- [x] CONTEXT, PROGRESS 및 STEP-02 보고서 확인
- [x] STEP-01~06 `PASSED` 확인
- [x] 현재 저장소의 API 명세·Controller·DTO·Gateway·Jackson 설정 검색
- [x] `origin/subPjt_1` 기술조사 문서 제외
- [x] Notion API v1.0의 Pi → Spring CONFIRMED Edge Endpoint 확인
- [x] 운영 Controller·DTO를 임의 생성하지 않음
- [x] IntelliJ HTTP Client 요청 10개 작성
- [x] 얼굴 인증 성공·실패 Mock 작성
- [x] 파손 판정 메타데이터 Mock 작성
- [x] 잠금 성공·실패 Mock 작성
- [x] 슬롯 점유·비점유 센서 Mock 작성
- [x] 정상 Heartbeat Mock 작성
- [x] 부팅 Snapshot 일치·불일치 Mock 작성
- [x] 식별자 의미와 멱등 키 분리
- [x] 반납 공식 상태 필드를 `status`로 제한
- [x] `process_status`·`processStatus` 거절 테스트
- [x] 얼굴 이미지·embedding 금지 테스트
- [x] 누락·형식 오류 식별자 테스트
- [x] Snapshot 기준 상태 일치 0건·불일치 1건 재현
- [x] 독립 JUnit 계약 테스트 15개 통과
- [x] 기존 MySQL 통합 테스트를 포함한 전체 빌드 20개 테스트 통과
- [x] 실제 Secret·운영 장치 주소 미사용
- [x] 애플리케이션 코드·설정·Migration·의존성 무수정
- [x] Commit, Push, MR 미수행

## STEP-07 판정

`PASSED`

권위 API v1.0에서 Pi → Spring Edge Endpoint를 확인하고, 운영 구현이
없는 현재 저장소에는 Controller·DTO를 추가하지 않았다. 승인 경로를
사용하는 HTTP 요청과 JSON fixture로 여섯 논리 계약의 10개 시나리오를
재현했다. 계약 테스트 15개는 식별자·상태·금지 생체정보·Snapshot
일치/불일치를 검증하며 모두 통과했다. Heartbeat 세부 DTO, Snapshot
정상 응답 Enum, 장치 인증 및 API 문서의 `processStatus` 잔존은 운영
구현 전 해결할 팀 결정으로 분리했다.

## STEP-08 체크리스트

- [x] CONTEXT, PROGRESS, STEP-05 및 STEP-07 보고서 확인
- [x] STEP-01~07 `PASSED` 확인
- [x] Controller·Service·Repository·Transaction 기존 구현 조사
- [x] V1의 `request_id`, `command_id`, `event_id`, Toss ID UNIQUE 확인
- [x] Notion API·Backend 역할·공통 정책에서 재전송 정책 확인
- [x] `requestId`, `commandId`, `eventId`, 결제 식별자 의미 분리
- [x] 동일 ID·동일 payload는 기존 결과를 반환하는 정책 구현
- [x] 동일 ID·다른 payload는 명시적 충돌 예외로 거절
- [x] 반납·장치 명령 원자적 no-op upsert 구현
- [x] 장치 작업 행 잠금과 이벤트 조건부 완료 구현
- [x] 이벤트 재전송 시 슬롯 상태 재전이 방지
- [x] 결제 시도·정산 행 잠금과 조건부 상태 변경 구현
- [x] DB UNIQUE 예외를 멱등 충돌 예외로 변환
- [x] 같은 `requestId` 순차·2-thread 동시 테스트
- [x] 같은 `commandId` 순차·2-thread 동시 테스트
- [x] 같은 `eventId` 순차·2-thread 동시 테스트
- [x] 같은 결제 승인 식별자 순차·2-thread 동시 테스트
- [x] 서로 다른 ID 정상 처리 테스트
- [x] 충돌 뒤 중간 상태가 남지 않는 Transaction 롤백 테스트
- [x] Testcontainers MySQL 8.0 사용, H2 미사용
- [x] 기존 Migration·운영 제약 무수정
- [x] 전체 Gradle 빌드와 30개 테스트 통과
- [x] 테스트 비활성화·삭제 및 `@Disabled` 미사용
- [x] Commit, Push, MR 미수행

## STEP-08 판정

`PASSED`

확정 정책에 따라 동일 식별자·동일 payload는 저장된 결과를 반환하고,
다른 payload는 `IdempotencyConflictException`으로 거절한다. MySQL
UNIQUE, 원자적 upsert, `FOR UPDATE`, 조건부 갱신을 조합했으며
`requestId`, `commandId`, `eventId`, 결제 승인 key 각각의 2-thread
동시 호출에서도 신규 Side Effect는 한 번만 반영됐다. 운영 Controller와
외부 장치·Toss 호출은 아직 없으므로 HTTP 200/409 및 외부 Side Effect
검증은 다음 구현 경계로 남겼다.

## STEP-09 체크리스트

- [x] CONTEXT, PROGRESS, STEP-02 및 STEP-05 보고서 확인
- [x] STEP-01~08 `PASSED` 확인
- [x] Slot Entity·Enum·Query·Transaction 기존 구조 조사
- [x] 기존 자동 선정·비관적/낙관적 Lock 전략 부재 확인
- [x] 기존 특정 반납 슬롯 조건부 UPDATE 방어선 확인
- [x] 대여·반납 후보 슬롯 상태 규칙 확정
- [x] MySQL `FOR UPDATE SKIP LOCKED` 후보 행 잠금 구현
- [x] 동일 Predicate의 조건부 UPDATE 최종 방어 구현
- [x] 대여소·상태 후보 복합 인덱스 Flyway V2 추가
- [x] 대여소 행·전체 슬롯·Java 전역 Lock 미사용
- [x] 슬롯 잠금·상태 전이·업무 Row 생성 단일 Transaction 구성
- [x] `SlotUnavailableException` 명시적 충돌 구현
- [x] 슬롯 1개·동시 요청 2개에서 성공 1·충돌 1 확인
- [x] 슬롯 N개·요청 N개에서 중복 없이 전부 성공
- [x] 슬롯 N개·요청 N+1개에서 충돌 1건 확인
- [x] Transaction 중 FK 예외 발생 시 슬롯 Rollback 확인
- [x] 실패한 반납 뒤 같은 `rental_id`의 새 반납 시도 확인
- [x] 활성 대여·반납 업무의 중복 슬롯 SQL 결과 0건
- [x] Testcontainers MySQL 8.0 사용, H2 미사용
- [x] `ExecutorService`·`CountDownLatch` 사용, Thread sleep 미사용
- [x] `rental_id` UNIQUE 미추가
- [x] 전체 Gradle 빌드와 36개 테스트 통과
- [x] Commit, Push, MR 미수행

## STEP-09 판정

`PASSED`

대여소와 실제 후보 상태를 만족하는 슬롯 1행만
`FOR UPDATE SKIP LOCKED`로 획득하고, 같은 Predicate의 조건부 UPDATE와
업무 Row 생성을 하나의 Transaction으로 묶었다. 동시 요청은 한 슬롯을
중복 소유하지 않았고 가용 후보가 없으면 `SlotUnavailableException`으로
예측 가능하게 종료됐다. FK 실패 시 슬롯은 `AVAILABLE`로 롤백됐으며
`rental_id` 비고유 반납 재시도 정책도 유지된다.

## STEP-10 체크리스트

- [x] CONTEXT, PROGRESS, STEP-07, STEP-08 및 STEP-09 보고서 확인
- [x] STEP-01~09 `PASSED` 확인
- [x] 기존 Snapshot DTO·Controller 부재와 승인 Endpoint·fixture 확인
- [x] `bootId`를 Snapshot 전용 멱등 키로 사용
- [x] `requestId`·`eventId`를 Snapshot 식별자로 혼용하지 않음
- [x] `occupancyStatus`·`lockStatus`·`sensorHealth` 계약 보존
- [x] DB 상태와 Snapshot 비교를 독립 정책 객체로 분리
- [x] station·slot `FOR UPDATE`와 단일 Transaction 구성
- [x] 슬롯별 불일치를 해당 슬롯 `ADMIN_REVIEW`로 전환
- [x] 연결된 진행 중 반납을 `RECOVERY_REQUIRED`로 전환
- [x] unknown slot·일부 누락을 대여소 전체 장애로 분리
- [x] 불일치 시 물리 상태를 Snapshot 값으로 자동 덮어쓰지 않음
- [x] unknown slot을 자동 생성하지 않음
- [x] 동일 Snapshot 재전송의 반복 Side Effect 차단
- [x] 같은 `bootId`의 다른 payload를 멱등 충돌로 거절
- [x] 복구 상태 뒤 일치 Snapshot이 수동복구 상태를 해제하지 않음
- [x] 장기 Snapshot 이력·범용 RecoveryTask·Pi DB 미추가
- [x] 점유·잠금 불일치 및 unknown·누락 fixture 추가
- [x] Testcontainers MySQL 8.0 전용 통합 테스트 9개 통과
- [x] 계약 fixture 검증 18개 통과
- [x] 기존 회귀 테스트 포함 전체 48개 및 최종 Gradle 빌드 통과
- [x] Commit, Push, MR 미수행

## STEP-10 판정

`PASSED`

일치 Snapshot은 마지막 boot metadata만 기록하고 기존 슬롯·업무 상태를
유지한다. 알려진 슬롯의 물리 불일치는 해당 슬롯과 연결된 진행 중 반납만
수동복구 상태로 전환하며, unknown slot 또는 일부 슬롯 누락은 장치·재고
범위 불일치로 분류해 대여소 전체를 안전 상태로 전환한다. 동일 `bootId`
재전송은 추가 갱신 없이 `REPLAYED`되고, 다른 payload 재사용은 충돌로
거절된다. 운영 Controller·HTTP 응답 및 장치 프로비저닝 계약은 팀 합의
대상으로 남기고 임의 구현하지 않았다.

## STEP-11 체크리스트

- [x] CONTEXT, PROGRESS, STEP-05, STEP-09 및 STEP-10 보고서 확인
- [x] STEP-01~10 `PASSED` 확인
- [x] 기존 Java/JdbcTemplate/Testcontainers 테스트 데이터 방식 확인
- [x] 운영 관리자·재고 Repository 및 API 구현 부재 확인
- [x] `src/test` 전용 재사용 Java fixture 작성
- [x] 전용 테스트 DB catalog가 아니면 Seed 실행 거부
- [x] 사용자 2명·대여소 1개·슬롯 5개 가짜 시나리오 구성
- [x] 사용 가능·대여 중·반납 처리 중·관리자 확인 슬롯 구성
- [x] 활성 대여와 실패·진행·완료 반납 시도 구성
- [x] 장치 명령 3건·장치 이벤트 2건 구성
- [x] Seed 2회 실행 뒤 Row 수 불변 확인
- [x] 개인정보·얼굴 데이터·실제 장치 ID 미사용
- [x] main Flyway·local·prod Profile 자동 Seed 미추가
- [x] 대여소별 전체·대여 가능·대여 중·반납 중·관리자 확인 집계
- [x] 활성 대여와 실패·완료 반납 집계·목록 검증
- [x] 장치 명령·이벤트 조회·집계 검증
- [x] DB 조건부 집계, projection 조회 및 N+1 부재 점검
- [x] station 범위 조회의 실제 MySQL 인덱스 확인
- [x] MySQL session `+00:00`과 고정 UTC DATETIME 값 확인
- [x] Testcontainers MySQL 8.0 전용 테스트 9개 통과
- [x] 기존 회귀 테스트 포함 전체 57개 및 최종 Gradle 빌드 통과
- [x] Commit, Push, MR 미수행

## STEP-11 판정

`PASSED`

운영 Profile과 main Migration에서 분리된 테스트 전용 fixture가 최소
시나리오를 결정적으로 재생성하며, 두 번째 실행에도 데이터가 누적되지
않는다. 재고와 관리자 수치는 전체 Entity 로딩 없이 DB 집계로 계산되고,
슬롯·반납·장치 목록은 station 범위 projection Query로 실제 MySQL
상태를 반영한다. 운영 API 계약은 확인되지 않아 Controller는 추가하지
않았다.

## STEP-12 체크리스트

> 아래 수치와 V1~V3 표기는 STEP-12 당시 감사 이력이다. 현재 통합
> 기준은 STEP-14 및 CONTEXT의 현재 통합 기준선(V1~V18, 7 suites,
> 62 tests)을 따른다.

- [x] STEP-01~11 보고서와 현재 저장소 상태 교차검증
- [x] 작업 브랜치 `S15P11A201-55-be-db`와 Base `develop` 확인
- [x] `origin/develop` ancestor 및 작업 브랜치 원격 동기화 확인
- [x] Temurin JDK 21.0.11과 Gradle Wrapper 9.5.1 확인
- [x] `clean build --rerun-tasks` 성공
- [x] 6 suites, 57 tests, 실패·오류·skip 0 확인
- [x] 빈 Testcontainers MySQL 8.0에 Flyway V1~V3 적용
- [x] 같은 DB Migration 재실행 0건 확인
- [x] 업무 테이블 9개와 필수 UNIQUE·인덱스 확인
- [x] MySQL 금지 생체정보·legacy 상태 컬럼 부재 확인
- [x] MySQL `8.0.46` Compose container `healthy` 확인
- [x] PostgreSQL `docker-local` 모드와 container `healthy` 확인
- [x] pgvector apply 재실행과 Extension·FACE_PROFILE 검증 SQL 통과
- [x] Cross-DB UUID 규칙과 Spring PostgreSQL 직접 연결 부재 확인
- [x] 멱등성·동시성·Snapshot·Seed·조회 테스트 증거 집계
- [x] 장치·AI Mock과 금지 Payload 테스트 증거 집계
- [x] Pi DB, 두 번째 DataSource, legacy 상태, Secret, 운영 주소,
      얼굴·embedding 로그 정적 검색
- [x] 최종 보고서, 로컬 Runbook, 팀 결정 문서 작성
- [x] Commit, Push, MR 미수행

## STEP-12 판정

`PARTIALLY_BLOCKED`

현재 HEAD에서 로컬 필수 재검증 40개는 모두 통과했다. JDK 21과 Wrapper
빌드, MySQL 8.0 빈 DB Flyway V1~V3 및 재실행, pgvector Extension과
FACE_PROFILE, MySQL 기반 멱등성·슬롯 동시성·Snapshot·Seed·집계,
금지 구조 검색이 실제 실행 증거를 갖는다.

운영 Controller와 HTTP 응답, 실물 Pi 및 장치 인증·물리 명령 Side
Effect, Toss 외부 승인 Side Effect, embedding 차원·거리 함수·vector
index는 실제 검증할 계약 또는 외부 환경이 없다. 이 4개 경계를
`BLOCKED`로 분리하므로 전체 환경 인수 판정은 `PARTIALLY_BLOCKED`다.
로컬 개발 기준선 자체의 실패 항목은 없다.

## STEP-14 체크리스트

- [x] Backend 3 P0-P1 clean-install schema 적용 지침 확인
- [x] clean schema를 기존 Flyway DB에 직접 실행하지 않음
- [x] 원본 패키지 Flyway V1~V18과 저장소 파일 SHA-256 전수 일치
- [x] 이전 ERD v2.1 V1~V4를 Flyway 실행 위치 밖에 보존
- [x] MySQL Compose에 UTC와 trigger 생성 권한 설정
- [x] 기존 `billisan` DB와 named volume 보존
- [x] 신규 `billisan_p0_p1` 빈 DB 생성
- [x] Spring local 기동으로 V1~V18 18건 적용
- [x] Hibernate `ddl-auto=validate` 통과
- [x] 동일 DB 재기동 시 신규 Migration 0건 확인
- [x] 12개 물리 테이블과 9개 트리거 메타데이터 확인
- [x] MySQL 얼굴 이미지·embedding·vector 컬럼 0건 확인
- [x] 사용자 9자리 PK와 별도 `user_ref` 경계 반영
- [x] 장치 독립 원장과 결제 trigger 권한 경계 반영
- [x] MySQL 기반 멱등성·슬롯 동시성·Snapshot 테스트 통과
- [x] 테스트 전용 Seed·조회 Fixture를 최종 제약에 정렬
- [x] 루트 `backend` 통합 후 clean test와 전체 7 suites, 62 tests 통과
- [x] Commit, Push, MR 미수행

## STEP-14 판정

`PASSED`

공식 clean-install schema의 기준인 Flyway V1~V18을 빈
`billisan_p0_p1`에 적용하고 같은 DB에서 재기동 no-op을 확인했다.
기존 `billisan` DB는 자동 변환하거나 삭제하지 않았다. V13 이전 사용자
데이터의 9자리 학번 매핑은 추론할 수 없으므로, 기존 데이터 이전이
필요하면 팀 승인 매핑을 사용하는 별도 작업으로 분리한다.

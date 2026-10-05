# Billisan Backend 2 개발환경 컨텍스트

## 입력값

| 항목 | 값 |
| --- | --- |
| `REPOSITORY_URL` | `https://lab.ssafy.com/s15-webmobile3-sub1/S15P11A201.git` |
| `BASE_BRANCH` | `develop` |
| `JIRA_ISSUE_KEY` | `S15P11A201-55` |
| `WORK_BRANCH` | `S15P11A201-55-be-db` |
| `WORKSPACE_DIR` | `S15P11A201` |
| `TARGET_OS` | `Windows` |
| `PRIMARY_SHELL` | `PowerShell` |
| `ALLOW_GIT_COMMIT` | `false` |

> 브랜치 이름에는 `feature/`, `fix/` 등의 접두사를 사용하지 않는다. 작업 유형은 Jira 이슈 키로 확인한다.

## 현재 통합 기준선

아래 기준은 `develop` 병합과 중첩 프로젝트 제거를 마친 현재 작업 트리에
적용된다. STEP-02~13의 세부 내용은 당시 실행 이력이며, 현재 경로·DB
설정·Migration 범위가 충돌할 경우 이 절과 STEP-14 기준을 우선한다.

| 항목 | 현재 기준 |
| --- | --- |
| Spring 프로젝트 루트 | `backend` |
| Gradle Wrapper | `backend/gradlew.bat` |
| Compose | `backend/docker-compose.yml` |
| DB PowerShell 스크립트 | `backend/scripts/*.ps1` |
| Spring 설정 | `backend/src/main/resources/application.yaml`, `application-local.yaml` |
| Flyway | `backend/src/main/resources/db/migration`, V1~V18 |
| 로컬 MySQL DB | `billisan_p0_p1` |
| MySQL 사용자 변수 | `MYSQL_USERNAME` |
| DB 저장·세션 시간대 | UTC |
| 중첩 프로젝트 | `backend` 아래 별도 Spring/Gradle 프로젝트 없음 |
| 통합 검증 | 7 suites, 62 tests, 실패·오류·skip 0 |

공식 `mysql:8.0` 이미지가 컨테이너 초기화용 키로 요구하는
`MYSQL_USER`는 Compose 내부에서만 사용하고, 저장소의 외부 환경 변수
계약은 `MYSQL_USERNAME`으로 통일한다.

## 저장소 상태 (STEP-01 당시)

| 항목 | 값 |
| --- | --- |
| 저장소 루트 | `C:\Users\SSAFY\S15P11A201` |
| 원격 `origin` | `https://lab.ssafy.com/s15-webmobile3-sub1/S15P11A201.git` |
| 기준 브랜치 | `develop` |
| 작업 브랜치 | `S15P11A201-55-be-db` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 현재 커밋 | `5d39e4e437c77ee7de656df73c815bf5ae165e0f` |
| 원격 추적 브랜치 | `origin/S15P11A201-55-be-db` |
| 원격 대비 차이 | ahead `0`, behind `0` |
| 점검 시각 | `2026-07-26T21:12:47+09:00` |

현재 작업공간에는 입력 URL과 일치하는 저장소가 이미 Clone되어 있어 중복 Clone하지 않았다. `git fetch --all --prune`을 완료했고, `develop`을 `--ff-only`로 동기화한 결과 최신 상태였다. 강제 Reset, Clean, 강제 Checkout은 사용하지 않았다.

## 도구 탐지 결과 (STEP-01 당시)

| 도구 | 상태 | 탐지 결과 |
| --- | --- | --- |
| Git | 설치됨 | `git version 2.55.0.windows.2` |
| Java | 설치됨 | Eclipse Temurin OpenJDK `21.0.11+10-LTS` |
| Gradle Wrapper | 없음 | 저장소에서 `gradlew`, `gradlew.bat`, `gradle-wrapper.properties`를 찾지 못함 |
| Docker | 설치·기동됨 | 사용자별 Docker Desktop, Docker Engine/CLI `29.6.2` |
| Docker Compose | 설치됨 | `Docker Compose version v5.3.1` |
| Windows PowerShell | 설치됨 | `5.1.26100.8875` |
| PowerShell 7 (`pwsh`) | 탐지되지 않음 | `pwsh` 명령이 PATH에 없음 |

## GUI 도구 탐지 결과

GUI 도구는 자동 설치하지 않았다.

| 도구 | 상태 | 탐지 결과 |
| --- | --- | --- |
| IntelliJ IDEA | 설치됨 | `2026.2.0.1` (`262.8665.337`), `C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1` |
| DBeaver | 탐지되지 않음 | PATH, Windows 제거 프로그램 레지스트리, 일반 설치 경로 기준 |
| MySQL Workbench | 탐지되지 않음 | PATH, Windows 제거 프로그램 레지스트리, 일반 설치 경로 기준 |
| Postman | 탐지되지 않음 | PATH, Windows 제거 프로그램 레지스트리, 일반 설치 경로 기준 |

`탐지되지 않음`은 수행한 탐지 범위에서 설치를 확인하지 못했다는 의미이며, 시스템 전체에 절대적으로 존재하지 않음을 보장하지 않는다.

## STEP-02 기술 기준선 감사

### 감사 범위와 근거 우선순위

1. 현재 작업 브랜치 `S15P11A201-55-be-db`
2. 기준 브랜치 `origin/develop`
3. 미병합 원격 후보 브랜치 `origin/S15P11A201-50-be-springboot-setting`

`HEAD`, `origin/develop`, `origin/S15P11A201-55-be-db`는 모두
`ed68f97451b1327ea666bb3ba9732bfce84bd08b`로 동일하다. 현재 브랜치의
Git 추적 파일은 `README.md`와 각 작업 영역의 `.gitkeep` 6개뿐이다.
따라서 현재 브랜치에는 아직 빌드 가능한 Backend 애플리케이션이 없다.

사용자 지시에 따라 `origin/subPjt_1`의 기술조사 문서는 STEP-02 감사
근거와 결론에서 제외했다.

### 현재 확정 기준선

| 항목 | 현재 작업 브랜치 기준 |
| --- | --- |
| 저장소 구조 | `ai`, `backend`, `frontend/app`, `frontend/kiosk`, `frontend/web`, `iot` 작업 영역 |
| 빌드 멀티모듈 | Gradle/Maven 설정 파일이 없어 확정할 수 없음 |
| Spring Boot | 현재 브랜치에 프로젝트가 없어 버전 미확정 |
| Java Toolchain | 저장소 기준 미확정. 로컬 실행 JDK만 Temurin 21 |
| 빌드 DSL | 미확정 |
| Gradle Wrapper | 현재 브랜치에 없음 |
| DB/영속성 | Driver, ORM/Mapper, Migration, DataSource 모두 미도입 또는 미확정 |
| 애플리케이션 설정 | `application*` 파일 없음 |
| 컨테이너/스크립트 | Dockerfile, Compose, 개발 스크립트 없음 |
| 도메인 코드 | Entity, Repository, Mapper, Gateway, DTO 및 장치 메시지 코드 없음 |
| 테스트 | 테스트 소스, 테스트 프로파일, 빌드/테스트 진입점 없음 |

### 미병합 Spring Boot 초기화 후보

`origin/S15P11A201-50-be-springboot-setting`은 `origin/develop`보다
1커밋 앞서고 아직 병합되지 않았다. 이 브랜치는 다음 후보 기준을
포함하지만, 현재 작업 브랜치의 확정 기준으로 간주하지 않는다.

| 항목 | 후보 브랜치 근거 |
| --- | --- |
| 프로젝트 위치 | `backend` |
| 프로젝트 형태 | 단일 Gradle root project (`rootProject.name = 'billisan'`) |
| Spring Boot | `4.1.0` |
| Java Toolchain | `21` |
| Gradle DSL | Groovy DSL |
| Gradle Wrapper | 존재, Gradle `9.5.1` |
| 기본 패키지 | `com.ssafy.billisan` |
| 기본 설정 | `src/main/resources/application.yaml`에 애플리케이션 이름만 존재 |
| 테스트 | `@SpringBootTest` 기반 `contextLoads()` 1개 |
| 영속성/DB | 기본 starter와 test starter만 있어 DB 관련 의존성·설정 없음 |

### 후속 단계 선행 결정

- 미병합 Spring Boot 초기화 브랜치를 먼저 `develop` 또는 현재 작업
  브랜치에 통합할지 팀에서 결정해야 한다.
- Spring Boot `4.1.0`, Java `21`, Gradle `9.5.1`을 팀 기준으로 승인해야 한다.
- MySQL Driver와 JPA/MyBatis 선택, Migration 도구와 실행 주체를 결정해야 한다.
- MySQL, PostgreSQL, pgvector 버전과 로컬 Compose 구성을 결정해야 한다.
- Orin PostgreSQL 직접 연결 허용 여부와 Spring의 두 번째 DataSource
  필요 여부를 결정해야 한다.
- Device Gateway/DTO 패키지와 장치 메시지 식별자 계약을 결정해야 한다.
- 슬롯 동시성 제어와 테스트 DB/Testcontainers 전략을 결정해야 한다.

상세 근거와 검색 범위는
`docs/backend2-setup/reports/STEP-02-REPOSITORY-AUDIT.md`에 기록한다.

## STEP-03 Spring Local Baseline

STEP-02에서 확인한 기존 원격 스캐폴드를 새로 생성하지 않고 재사용했다.
해당 내용은 당시 실행 이력이며, 현재는 `develop` 병합 후 루트
`backend` 단일 프로젝트로 통합되어 있다.

| 항목 | 적용 기준 |
| --- | --- |
| 프로젝트 루트 | `backend` |
| Spring Boot | `4.1.0` |
| Java Toolchain | `21` |
| 검증 JDK | Eclipse Temurin `21.0.11+10-LTS` |
| Gradle | Wrapper `9.5.1`, Groovy DSL |
| 기본 Profile | 별도 활성 Profile 없음 |
| 로컬 Profile | `local` |
| 업무 DB 설정 | `local` Profile의 MySQL 환경 변수 참조 한 세트 |
| PostgreSQL DataSource | 추가하지 않음 |
| Pi 설정 | 실제 소비 코드가 없어 추가하지 않음 |

### local 환경 변수 계약

| 변수 | 용도 | 민감 여부 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` Profile 활성화 | 비민감 |
| `MYSQL_URL` | 로컬 MySQL JDBC URL | 환경별 값 |
| `MYSQL_USERNAME` | 로컬 MySQL 사용자 | 환경별 값 |
| `MYSQL_PASSWORD` | 로컬 MySQL 비밀번호 | Secret |

`.env.example`에는 비민감 예시만 두고, 실제 `.env`와 `.env.*`는
`.gitignore` 처리했다. `application-local.yaml`에는 비밀번호 기본값을
두지 않았다.

현재 스캐폴드에는 JDBC/MySQL Driver가 아직 없다. STEP-02에서
영속성 기술 선택이 미확정이므로 이번 단계에서는 의존성을 임의
도입하지 않았으며, MySQL DataSource 구성은 향후 Driver가 승인되어
추가될 때 사용할 단일 설정 계약으로만 확립했다.

### 검증 결과

- Wrapper: Gradle `9.5.1`, Launcher/Daemon JVM `21.0.11`
- `compileJava`: `BUILD SUCCESSFUL`
- 기본 `test`: `BUILD SUCCESSFUL`
- `local` Profile 강제 재실행 테스트: 1개 실행, 실패/오류/건너뜀 0
- `build`: `BUILD SUCCESSFUL`, 실행 Jar 생성
- 설정 바인딩 전용 테스트: 저장소에 없어 실행 대상 없음

시스템 `JAVA_HOME`은 존재하지 않는 JRE 경로를 가리키고 있어 명령
세션에서만 설치된 JDK 21 경로로 보정했다. 시스템 환경 변수 자체는
변경하지 않았다. Docker/MySQL이 탐지되지 않고 Driver도 아직 없어
실제 DB 연결은 검증하지 않았다.

## STEP-04 Local Data Infrastructure

### 입력값과 적용 모드

| 입력 | 전달값 | 적용 |
| --- | --- | --- |
| `MYSQL_VERSION` | `8.0` | MySQL image tag에 적용 |
| `POSTGRES_MODE` | `<docker-local\|shared-orin\|skip-until-confirmed>` | 실제 값이 아니므로 안전한 `skip-until-confirmed`로 처리 |
| `PGVECTOR_IMAGE` | `<팀 확정 이미지 또는 UNCONFIRMED>` | 미확정으로 처리, image 고정 안 함 |

### 구성 기준

| 항목 | 결과 |
| --- | --- |
| Compose | `backend/docker-compose.yml` |
| MySQL 서비스 | `db`, image `mysql:8.0`, 포트/DB/사용자/비밀번호 환경 변수화 |
| 영속성 | named volume `db-data` |
| Healthcheck | `mysqladmin ping`, 10초 간격, 최대 5회 재시도 |
| 문자셋 | `utf8mb4` |
| Collation | `utf8mb4_0900_ai_ci` |
| 타임존 | UTC, MySQL offset `+00:00` |
| PostgreSQL+pgvector | 서비스와 접속 스크립트 미생성 |
| Spring DB 경계 | `application-local.yaml`은 MySQL URL만 참조 |
| Redis/Pi DB | 추가하지 않음 |

기존 Compose와 DB 개발 스크립트가 없어 중복 생성 충돌은 없었다.
PowerShell 스크립트는 `backend/scripts`에 시작, 중지, 상태,
로그 조회 네 가지로 구성했다. 중지 스크립트는 `stop db`만 실행하며
Volume을 제거하지 않는다.

### 검증 상태

- Compose YAML 파싱: `PASSED`
- 필수 MySQL 키 및 단일 서비스 경계: `PASSED`
- PowerShell 스크립트 4개 구문 파싱: `PASSED`
- 로컬 `.env` 생성 및 Git ignore 확인: `PASSED`
- PostgreSQL URL의 Spring 설정 부재: `PASSED`
- `docker compose config --quiet`: `PASSED`
- MySQL 기동/Healthcheck: `PASSED`
- 실제 MySQL 버전: `8.0.46` (`mysql:8.0`)
- 호스트 포트: `127.0.0.1:3306` 연결 확인
- 영속 Volume: `db-data` → `/var/lib/mysql`
- 서버 기준: `utf8mb4`, `utf8mb4_0900_ai_ci`, `+00:00`

Docker Desktop은 사용자별 설치 경로에서 실행 중이다. 로컬 `.env`는
생성했으며 Git ignore 대상임을 확인했다. Secret 값은 문서나 추적
파일에 기록하지 않는다.

## ERD v1.0.1 구현 기준선

권위 원문은 Notion의 `08. Billisan ERD 및 데이터 아키텍처 문서
v1.0.1`이다. Downloads의 기존 v1.0 Markdown export는 변경 전 사본이므로
신규 Migration의 권위 근거로 사용하지 않는다.

| 항목 | 확정 기준 |
| --- | --- |
| 반납 재시도 | `return_attempt.rental_id` FK + 비고유 인덱스 |
| 반납 멱등성 | `return_attempt.request_id` UNIQUE |
| 반납 상태 | `PROCESSING`, `PHYSICAL_DONE`, `COMPLETED`, `RECOVERY_REQUIRED`, `FAILED` |
| 금지 필드 | `process_status`, `processStatus` |
| 결제 | `payment_attempt.settlement_id`, `toss_order_id`, `toss_payment_key` |
| 금지 결제 필드 | `payment_attempt.rental_request_id` |
| 개별 우산 | `UMBRELLA`, `umbrella_id` 미사용 |
| Pi 업무 DB | 0개 |
| 물리 이름 | MySQL 테이블·컬럼 `lower_snake_case` |
| UUID | `CHAR(36) CHARACTER SET ascii COLLATE ascii_bin` |
| Toss 주문 ID | `VARCHAR(64)`, NOT NULL, UNIQUE |
| Toss 결제 키 | `VARCHAR(200)`, NULL 허용, UNIQUE |
| 업무 시각 | `DATETIME(6)`, `UTC` |
| 금액 | `BIGINT UNSIGNED`, 0 이상 CHECK |
| FK 기본 동작 | `ON UPDATE RESTRICT ON DELETE RESTRICT` |

Notion 원문:
`https://app.notion.com/p/2db443b01b138220904d01f4677e132b`

## STEP-05 MySQL Migration 기준선

| 항목 | 확정 기준 |
| --- | --- |
| Migration 도구 | Flyway `12.4.0` |
| MySQL JDBC | Connector/J `9.7.0` |
| 실행 주체 | Spring Boot 시작 시 Flyway |
| Migration 위치 | `backend/src/main/resources/db/migration` |
| 최초 버전 | `V1__create_mysql_business_schema.sql` |
| MySQL | Docker `mysql:8.0`, 실제 `8.0.46` |
| 업무 테이블 | 9개 + `flyway_schema_history` |
| ORM·Mapper | 없음 |
| 테스트 DB | Testcontainers MySQL 8.0 |
| PostgreSQL DataSource | 없음 |

빈 로컬 `billisan` DB에 V1을 적용했고 같은 DB에서 다시 실행했을 때
추가 Migration 없이 최신 상태로 판정됐다. 실제 업무 테이블은
`user_account`, `station`, `slot`, `rental`, `return_attempt`,
`damage_inspection`, `settlement`, `payment_attempt`,
`device_operation`이다.

`return_attempt.request_id`는 UNIQUE이고
`return_attempt.rental_id`는
`idx_return_attempt_rental_status(rental_id, status)` 비고유 인덱스를
사용한다. 같은 대여의 실패 후 새 반납 시도를 만들 수 있다. 완료된
반납 시도를 대여당 최대 1개로 제한하는 책임은 DB UNIQUE가 아니라 향후
Spring 서비스 트랜잭션의 `RENTAL` 행 잠금과 기존 완료 시도 검사에 있다.

MySQL에는 `process_status`, `processStatus`, 얼굴 이미지, 얼굴 임베딩,
vector 컬럼을 만들지 않았다. `payment_attempt`에는
`rental_request_id`를 만들지 않았고 Toss 주문 ID와 결제 키를 분리했다.

검증 결과:

- Testcontainers MySQL 8.0 테스트: 5개 실행, 실패·오류·건너뜀 0
- 빈 DB Migration: V1 1건 적용 성공
- 같은 DB 재실행: no-op
- 실제 업무 테이블 수: 9
- 재시도 검증: 동일 `rental_id` 2행 생성 성공, 검증 후 `ROLLBACK`
- 최종 `build --rerun-tasks`: `BUILD SUCCESSFUL`

## STEP-06 Orin PostgreSQL+pgvector 경계

### 실행 모드와 소유 위치

| 항목 | 기준 |
| --- | --- |
| 요청 모드 | `POSTGRES_MODE=docker-local` |
| canonical image | `pgvector/pgvector:0.8.5-pg17-bookworm@sha256:d2ef61f42ef767baa5a1475393303cc235bcd92febd9d7014eddb48b41f3bad0` |
| local pull source | `mirror.gcr.io/pgvector/pgvector:0.8.5-pg17-bookworm` + 동일 digest |
| PostgreSQL 기준 | 실제 서버 `17.10` |
| pgvector 기준 | `0.8.5` |
| 지원 플랫폼 | `linux/amd64`, `linux/arm64` |
| 독립 인프라 루트 | `infra/orin-postgres` |
| Migration 실행 주체 | Orin PostgreSQL 운영자 또는 독립 PowerShell 스크립트 |
| Spring PostgreSQL 접근 | 금지, 두 번째 DataSource 없음 |
| Cross-DB Foreign Key | 금지 |
| 현재 실행 판정 | `PASSED` |

저장소에는 기존 FACE_PROFILE, PostgreSQL Migration, Python 모델 또는
Orin DB 소유 경로가 없었다. 따라서 Spring 프로젝트와 Backend 1 AI
실행환경에서 분리된 중립 인프라 경로에 확정 스키마 계약만 배치했다.
Spring Boot의 설정, Entity, Repository 및 의존성은 변경하지 않았다.

### FACE_PROFILE 물리 계약

권위 ERD v1.0.1의 논리 필드는 PostgreSQL에서 다음 물리 이름을 사용한다.

| 논리 이름 | PostgreSQL 물리 이름 | 타입·제약 |
| --- | --- | --- |
| `faceProfileId` | `face_profile_id` | `UUID`, PK |
| `userId` 논리 참조 | `user_ref` | `UUID`, NOT NULL, UNIQUE, FK 없음 |
| `embedding` | `embedding` | 차원 미지정 `VECTOR`, NOT NULL |
| 등록·활성 상태 | `registration_status` | `VARCHAR(20)`, `ACTIVE`/`INACTIVE` CHECK |
| 모델 버전 | `model_version` | `VARCHAR(50)`, NULL 허용 |
| 템플릿 버전 | `template_version` | `INTEGER`, NOT NULL, 기본값 1 |
| 등록 시각 | `registered_at` | `TIMESTAMPTZ`, NULL 허용 |
| 변경 시각 | `updated_at` | `TIMESTAMPTZ`, NOT NULL |
| 비활성 시각 | `deactivated_at` | `TIMESTAMPTZ`, NULL 허용 |

embedding 차원이 확정되지 않았으므로 임의의 `VECTOR(n)`과 vector
인덱스를 만들지 않는다. 이름, 학번, 이메일, 얼굴 원본 이미지와 그
경로도 저장하지 않는다.

### Cross-DB `userId` 규칙

| 경계 | 규칙 |
| --- | --- |
| MySQL 저장 | `user_account.user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin` |
| 전송 계약 | 소문자 하이픈 UUID 문자열 `8-4-4-4-12`, 총 36자 |
| PostgreSQL 저장 | `face_profile.user_ref UUID` |
| 변환 | Orin 경계에서 UUID로 파싱·검증한 뒤 저장하고, 외부 전송 시 표준 문자열로 직렬화 |
| 관계 | 값만 같은 논리 참조이며 물리 FK, Cross-DB JOIN, cascade 없음 |
| 카디널리티 | `user_ref` UNIQUE로 사용자당 현재 FACE_PROFILE 최대 1개 |
| 생명주기 | 사용자 삭제·비활성 동기화는 향후 확정할 API/이벤트 계약의 책임 |

MySQL의 대소문자 구분 `ascii_bin` 문자열과 PostgreSQL 네이티브 UUID의
표현 차이를 없애기 위해 쓰기 경계에서 정규형을 강제한다. PostgreSQL은
MySQL 사용자 개인정보를 복제하지 않고 `user_ref`만 보유한다.

### 런타임 검증 상태

팀이 공식 pgvector `0.8.5`, PostgreSQL `17` Bookworm image를
multi-platform index digest로 확정했다. Registry manifest에서 해당
digest와 `linux/amd64`, `linux/arm64` 하위 manifest를 확인했다.

Docker Hub의 `production.cloudfront.docker.com` blob 전송은 Docker
Desktop과 Containers proxy를 모두 `No proxy`로 바꾼 뒤에도 `EOF`가
재현됐다. Google Artifact Registry의 공식 Docker Hub cache인
`mirror.gcr.io`에서 canonical image와 동일한 OCI index digest를 확인한
뒤 이를 local pull source로 사용해 다운로드에 성공했다.

apply 스크립트를 같은 DB에 두 번 실행했고 두 번째 실행은 기존
Extension과 테이블을 안전하게 건너뛰었다. verify 스크립트와 실제
메타데이터 조회에서 다음을 확인했다.

- PostgreSQL `17.10`
- pgvector Extension `0.8.5`
- `public.face_profile` 9개 컬럼
- PK, `user_ref` UNIQUE, 등록 상태 CHECK
- Foreign Key와 개인정보·이미지 경로 컬럼 없음
- 컨테이너 `healthy`
- `127.0.0.1:5432` 연결
- named volume `billisan-orin-local_orin-postgres-data`

Git ignore된 로컬 `.env`를 생성했으며 추적 예제에는 비밀번호 값을
기록하지 않았다. Spring PostgreSQL DataSource는 계속 존재하지 않는다.

## STEP-08 API·장치 작업 멱등성 기준선

### 확정 처리 정책

Notion API v1.0, Backend 역할 명세 및 공통 정책 문서에서 다음 정책을
확인했다.

| 식별자 | 재전송 정책 | 충돌 정책 |
| --- | --- | --- |
| `requestId` | 동일 payload이면 저장된 반납 시도 반환 | 동일 ID·다른 대여/슬롯은 충돌 |
| `commandId` | 저장된 장치 작업 반환, 물리 발행은 `APPLIED`일 때만 허용 | 동일 ID·다른 명령 대상/유형은 충돌 |
| `eventId` | 저장된 처리 결과를 ACK하고 슬롯 상태를 다시 변경하지 않음 | 저장된 command/slot/결과 메타데이터와 다르면 충돌 |
| Toss 주문 ID | `toss_order_id` UNIQUE, 승인 시 결제 시도와 일치 검증 | 다른 주문 ID로 같은 결제 시도 승인 시 충돌 |
| Toss 승인 ID | 동일 `toss_payment_key`이면 기존 승인 결과 반환 | 다른 결제 시도가 같은 key를 사용하면 충돌 |

반납 실패 후 업무 재시도는 기존 `requestId`를 재사용하지 않고 새
`requestId`와 새 `returnAttemptId`를 사용한다. 추적 헤더
`X-Request-Id`와 업무 멱등 키는 통합하지 않는다.

### 구현 경계

| 항목 | 기준 |
| --- | --- |
| 서비스 | `com.ssafy.billisan.idempotency.IdempotencyService` |
| DB 접근 | Spring `JdbcTemplate`; JPA·MyBatis 추가 없음 |
| Transaction | 네 진입점 모두 `@Transactional` |
| 반납 동시성 | `request_id` UNIQUE + 원자적 no-op upsert |
| 장치 명령 동시성 | `command_id` UNIQUE + 원자적 no-op upsert |
| 장치 이벤트 동시성 | command 행 `FOR UPDATE`, `event_id IS NULL` 조건부 갱신, `event_id` UNIQUE |
| 결제 동시성 | payment·settlement 행 `FOR UPDATE`, 주문 ID·승인 key UNIQUE, 조건부 상태 갱신 |
| 중복 결과 | `Outcome.REPLAYED` |
| 신규 적용 | `Outcome.APPLIED` |
| payload 충돌 | `IdempotencyConflictException` |
| HTTP 매핑 | Controller가 없어 아직 없음; 향후 409 매핑 필요 |

V1 Migration에 필요한 UNIQUE가 이미 있어 새 Migration을 만들거나 운영
제약을 완화하지 않았다. 장치 발행·슬롯 전이·결제와 정산 갱신은
호출자가 `Outcome.APPLIED`를 기준으로 한 번만 수행하도록 경계를
명시한다.

### 검증 상태

- JDK: Eclipse Temurin `21.0.11+10-LTS`
- Gradle Wrapper: `9.5.1`
- 테스트 DB: Testcontainers `mysql:8.0`
- STEP-08 멱등성 테스트: 10개, 실패·오류·건너뜀 0
- 전체 테스트: 30개, 실패·오류·건너뜀 0
- 최종 `clean build --rerun-tasks`: `BUILD SUCCESSFUL`

2-thread 동시 호출은 `requestId`, `commandId`, `eventId`,
`toss_payment_key` 각각에서 `APPLIED` 1건과 `REPLAYED` 1건을 확인했다.
동일 ID·다른 payload 충돌 뒤 반납 슬롯, 장치 작업, 결제 시도 및 정산
상태에 중간 변경이 남지 않음을 실제 MySQL에서 확인했다.

### 남은 경계

- 운영 Controller가 없어 기존 결과 응답 200과 payload 충돌 409의 실제
  HTTP 매핑은 아직 검증하지 않았다.
- 실물 장치 디스패처와 Toss 외부 승인 호출이 없어 외부 Side Effect는
  이번 단계의 DB 트랜잭션 밖이다.
- V1은 이벤트의 command/slot/status/result/time을 보존하지만
  `occupancyStatus`·`lockStatus` 원본 payload 또는 hash를 보존하지 않는다.
  해당 두 값까지 strict conflict 판별하려면 계약 승인 후 이벤트 이력
  또는 payload fingerprint Migration이 필요하다.
- Toss 주문 생성 서비스와 대여 단위의 완료 반납 직렬화는 아직 없다.

## STEP-09 슬롯 동시 배정 기준선

### 슬롯 상태와 후보 규칙

현재 저장소에는 Slot Entity 또는 Java 상태 Enum이 없다. V1 Migration의
CHECK와 JDBC 문자열 상태가 실제 기준이다.

| 작업 | 후보 슬롯 | 획득 후 상태 |
| --- | --- | --- |
| 대여 | `AVAILABLE + OCCUPIED + LOCKED + NORMAL` | `RENTING` |
| 반납 | `AVAILABLE + EMPTY + LOCKED + item_condition NULL` | `RETURNING` |

한 슬롯의 `service_status`를 업무 소유권 mutex로 사용한다. 대여의 활성
배정 Row는 `rental.status = REQUESTED`, 반납의 활성 배정 Row는
`return_attempt.status IN (PROCESSING, PHYSICAL_DONE,
RECOVERY_REQUIRED)`로 해석한다.

### 동시성 전략

| 항목 | 기준 |
| --- | --- |
| 서비스 | `com.ssafy.billisan.slot.SlotAllocationService` |
| DB 접근 | 기존 방식과 같은 Spring `JdbcTemplate` |
| Transaction | 슬롯 잠금·상태 전이·업무 Row 생성 전체 `@Transactional` |
| 후보 선정 | `FOR UPDATE SKIP LOCKED`, `slot_number` 오름차순, 1행 |
| 잠금 범위 | station과 실제 후보 상태를 만족하는 슬롯 행 |
| 최종 방어 | 동일 상태 Predicate의 조건부 UPDATE 결과 1행 확인 |
| 충돌 | `SlotUnavailableException` |
| Java 전역 Lock | 사용하지 않음 |
| `rental_id` UNIQUE | 추가하지 않음 |

Flyway V2는 후보 조회의 잠금 스캔 범위를 줄이기 위해
`(station_id, service_status, occupancy_status, lock_status,
item_condition, slot_number)` 복합 인덱스를 추가한다. 선정 Query는 이
인덱스를 명시해 대여소 행이나 전체 슬롯을 잠그지 않는다.

### 검증 상태

- Testcontainers `mysql:8.0` STEP-09 테스트: 6개
- 1개 슬롯·2개 동시 반납 요청: 성공 1, 명시적 충돌 1
- N개 슬롯·N개 동시 대여 요청: 전부 성공, 서로 다른 슬롯
- N개 슬롯·N+1개 동시 대여 요청: 성공 N, 충돌 1
- FK 예외 뒤 슬롯 상태: `AVAILABLE`로 롤백
- 같은 `rental_id`의 실패 후 새 반납 시도: 2행 생성 성공
- 활성 업무 중복 슬롯 SQL: 0건
- 전체 테스트: 36개, 실패·오류·건너뜀 0
- 최종 `clean build --rerun-tasks`: `BUILD SUCCESSFUL`

`SKIP LOCKED`는 잠긴 후보를 기다리지 않으므로 짧은 경합에서도 일시적으로
사용 가능 슬롯 없음 응답이 날 수 있다. 이는 예측 가능한 충돌 정책이며,
향후 Controller는 `SlotUnavailableException`을 팀이 정한 409 또는
가용 슬롯 없음 응답으로 매핑해야 한다.

## STEP-10 Pi 부팅 Snapshot 조정 기준선

### 계약과 멱등성

| 항목 | 기준 |
| --- | --- |
| Endpoint 계약 | `POST /api/v1/edge/stations/{stationId}/boot-snapshots` |
| Snapshot 식별자 | Body `bootId`; Snapshot 전용 멱등 키 |
| 추적 식별자 | `X-Request-Id`; `bootId`와 분리 |
| 장치 식별자 | Body 및 `X-Device-Id`의 `deviceId`; station 소유 매핑은 미확정 |
| 슬롯 물리 상태 | `occupancyStatus`, `lockStatus`, `sensorHealth` |
| DB 비교 기준 | MySQL `slot.occupancy_status`, `slot.lock_status`, `slot_number` |
| 동일 재전송 | 같은 `bootId`·같은 canonical SHA-256이면 `REPLAYED` |
| 멱등 충돌 | 같은 `bootId`·다른 payload이면 `IDEMPOTENCY_CONFLICT` |
| 시간 역전 | 새 `bootId`의 `measuredAt`이 마지막 시각보다 늦지 않으면 `STALE_SNAPSHOT` |

Snapshot에는 `requestId`나 `eventId`를 새로 만들지 않는다. `bootId`는
한 Pi 부팅 인스턴스의 Snapshot 재전송을 식별하며, 다른 장치 이벤트의
`eventId` 및 API 업무 요청의 `requestId`를 대체하지 않는다.

### 조정 상태와 저장 범위

| 관측 결과 | 저장 전이 | 자동 변경하지 않는 값 |
| --- | --- | --- |
| 전 슬롯 일치 | 마지막 boot metadata와 last seen만 갱신 | 슬롯 업무·물리 상태 |
| 알려진 슬롯 상태 불일치 | 해당 슬롯 `ADMIN_REVIEW`; 연결된 진행 중 반납 `RECOVERY_REQUIRED` | `occupancy_status`, `lock_status` |
| unknown slot 또는 슬롯 누락 | station `MAINTENANCE/ERROR`; 기존 슬롯 전체 `ADMIN_REVIEW`; 진행 중 반납 `RECOVERY_REQUIRED` | unknown 슬롯 생성, 물리 상태 |
| 복구 상태 뒤 일치 Snapshot | 마지막 boot metadata만 갱신하고 기존 수동복구 상태 유지 | `ADMIN_REVIEW`, `RECOVERY_REQUIRED` |

Flyway V3는 station에 마지막 `bootId`, payload hash, 측정 시각과 현재
대여소 복구 근거를, slot에 현재 슬롯 복구 근거를 추가한다. Snapshot
장기 이력 테이블이나 범용 RecoveryTask 테이블은 만들지 않는다.
Snapshot 상태 갱신은 station과 해당 station 슬롯을 `FOR UPDATE`로 읽고
비교·기록·복구 전이를 한 `@Transactional` 경계에서 수행한다.

### 구현과 검증 경계

| 항목 | 기준 |
| --- | --- |
| 서비스 | `com.ssafy.billisan.snapshot.BootSnapshotReconciliationService` |
| 비교 정책 | `com.ssafy.billisan.snapshot.BootSnapshotPolicy` |
| DB 접근 | Spring `JdbcTemplate` |
| Migration | `V3__add_boot_snapshot_reconciliation_state.sql` |
| 테스트 DB | Testcontainers `mysql:8.0` |
| 운영 Controller·HTTP DTO | 없음; 확정 전 임의 생성하지 않음 |
| Pi 로컬 DB | 가정·추가하지 않음 |
| Snapshot 장기 저장소 | 없음 |
| Spring PostgreSQL 접근 | 없음 |

STEP-10 전용 MySQL 통합 테스트 9개는 일치, 점유 불일치, 잠금 불일치,
unknown slot, 잘못된 stationId, 동일 Snapshot 재전송, 일부 슬롯 누락,
복구 뒤 일치 Snapshot, 같은 `bootId`의 payload 충돌을 검증한다.
기존 회귀 테스트를 포함한 전체 48개 테스트가 실패·오류·건너뜀 없이
통과했고 `clean build --rerun-tasks`도 성공했다.

### 남은 Embedded·Backend 합의

- `deviceId`와 `stationId`의 프로비저닝·소유권 매핑 및 불일치 응답
- `MATCHED`, `RECOVERY_REQUIRED`, `REPLAYED`의 HTTP 응답 DTO와 status code
- `UNKNOWN` lock 상태를 API에서 유지할지 MySQL CHECK에 추가할지
- 부분 Snapshot을 전송 금지할지, firmware가 슬롯 총수·snapshot schema
  version을 함께 전송할지
- 관리자 확인 후 station·slot·return attempt 복구 상태를 해제하는 별도
  승인 API와 감사 주체
- 장치 전체 장애를 판단할 heartbeat timeout·sensor health 임계치

## STEP-11 Seed와 조회 기준선

### 테스트 데이터 관리

저장소의 기존 테스트는 Testcontainers MySQL 8.0과 테스트 Java 코드의
`JdbcTemplate` INSERT를 사용한다. 이 방식을 유지해
`Backend2QueryScenarioFixture`를 `src/test/java`에만 배치했다.

| 안전 경계 | 기준 |
| --- | --- |
| 소스 위치 | test source set 전용 |
| 실행 DB | `billisan_query_validation_test`만 허용 |
| 재실행 | FK 역순 DELETE 후 결정적 ID로 다시 INSERT |
| 운영 Profile | 자동 실행 경로 없음 |
| main Flyway | Seed Migration 없음 |
| 개인정보 | 가짜 login ID와 비운영 station 값만 사용 |
| 얼굴 데이터 | `face_registered=FALSE`, 이미지·embedding 없음 |
| 시간 | 고정 `LocalDateTime`, MySQL session `+00:00` |

Fixture는 연결 catalog가 전용 테스트 DB 이름과 다르면 데이터를 지우기
전에 실패한다. 따라서 local·prod Profile 시작으로 실행되지 않으며,
실수로 다른 DB에 주입할 수도 없다.

### Repository 조회 규칙

| 조회 | 물리 기준 |
| --- | --- |
| 전체 슬롯 | station의 모든 slot |
| 대여 가능 | `AVAILABLE + OCCUPIED + LOCKED + NORMAL` |
| 대여 중 | `service_status=RENTING` |
| 반납 처리 중 | `service_status=RETURNING` |
| 관리자 확인 | `service_status=ADMIN_REVIEW` |
| 활성 대여 | `rental.status=ACTIVE` |
| 해소된 반납 조회 | `return_attempt.status IN (FAILED, COMPLETED)` |
| 장치 명령 | station의 `device_operation` 전체 |
| 장치 이벤트 | `device_operation.event_id IS NOT NULL` |

구현 위치는
`com.ssafy.billisan.inventory.InventoryQueryRepository`다. 관리자·재고
API 계약과 Controller는 현재 저장소에 없으므로 HTTP 계층을 추정하지
않고 Repository와 projection record만 추가했다.

### Query 성능 기준

- 재고 집계는 조건부 `COUNT/SUM` 한 SQL에서 수행한다.
- 관리자 집계는 station을 기준으로 한 SQL 한 번에서 DB가 집계한다.
- 슬롯·반납·장치 목록은 필요한 컬럼만 선택하고 전체 Entity가 없다.
- 목록 정렬은 DB의 `slot_number` 또는 업무 시각과 PK로 수행한다.
- 상태 문자열은 Repository 내부 Enum에서 bind하여 SQL 곳곳에
  중복 하드코딩하지 않는다.
- `station_id`, `checkout_slot_id + status`,
  `return_slot_id + status` 조회를 지원하는 실제 인덱스를 MySQL
  `information_schema.statistics`에서 확인한다.

STEP-11 전용 Testcontainers MySQL 8.0 테스트 9개는 Seed 최소 시나리오,
2회 재실행 row 수 불변, 비테스트 DB 거절, 슬롯 집계·목록, 관리자 집계,
실패·완료 반납, 장치 명령·이벤트 분리, 실제 인덱스를 검증한다.
기존 회귀 테스트를 포함한 전체 57개 테스트가 실패·오류·건너뜀 없이
통과했고 `clean build --rerun-tasks`도 성공했다.

## STEP-13 ERD v2.1 확정 기준선

### 문서 상태와 우선순위

팀 결정에 따라 Notion `08 Billisan ERD v2.1`을
`RELEASED / CONFIRMED`로 간주한다.

`https://app.notion.com/p/2db443b01b138220904d01f4677e132b`

이 절과 STEP-13 보고서는 ERD v1.0.1을 근거로 작성된 STEP-05~12의
스키마 수·컬럼·상태 설명보다 우선한다. 기존 보고서는 당시 실행 증거를
보존하는 역사 기록이며 삭제하거나 소급 수정하지 않는다.

### MySQL 기준선

| 항목 | ERD v2.1 적용 기준 |
| --- | --- |
| Migration | 기존 V1~V3 보존 + `ARCHIVED_V4__align_schema_with_erd_v2_1.sql` (비실행 보관 자료) |
| 업무 테이블 | 기존 9개 + `face_profile_sync_operation`, 총 10개 |
| `user_account` | `password_hash`, `name` 필수; `account_status` 제거 |
| `station` | `current_boot_id`, `boot_synced_at` |
| `slot` | 물리 상태만 저장; 대여·반납 처리 중 상태는 업무 테이블에서 파생 |
| `rental.due_at` | `REQUESTED` 동안 NULL 허용 |
| `return_attempt.return_slot_id` | 물리 반납 전 NULL 허용 |
| `damage_inspection` | `updated_at` 필수 |
| `settlement` | `paid_amount` 추가, `paid_amount <= amount` |
| `payment_attempt` | `creation_request_id` UNIQUE, `updated_at`, `PROCESSING` |
| `device_operation` | 발행 boot 및 terminal event·물리 증거 저장, `OUTCOME_UNKNOWN` |

기존 사용자 행의 비밀번호 원문은 복구하거나 추정할 수 없다. V4는
`password_hash`를 `{migration-disabled}`로 채워 기존 계정을 로그인
불가능 상태로 보존하며, 운영 전 공식 비밀번호 재설정 흐름으로 교체해야
한다. 이는 실제 Secret이나 사용 가능한 공통 비밀번호가 아니다.

### PostgreSQL 기준선

| 항목 | ERD v2.1 적용 기준 |
| --- | --- |
| Migration | 기존 001~002 보존 + `003_align_face_profile_v2_1.sql` |
| `face_profile` | 정확히 6개 컬럼 |
| PK | `user_ref UUID` |
| 나머지 컬럼 | `embedding`, `model_version`, `template_version`, `registered_at`, `updated_at` |
| 제거 컬럼 | `face_profile_id`, `registration_status`, `deactivated_at` |
| Cross-DB FK | 없음 |
| Spring PostgreSQL DataSource | 없음 |
| vector 규격 | 차원·거리 함수·인덱스는 계속 미확정, 임의 고정하지 않음 |

### 애플리케이션 기준선

- Spring Data JPA를 기본 영속성 의존성으로 추가하고
  `hibernate.ddl-auto=validate`, `open-in-view=false`를 적용한다.
- 신규 `face_profile_sync_operation` CRUD와 행 잠금은 JPA Repository가
  담당한다.
- 슬롯 `FOR UPDATE SKIP LOCKED`, 조건부 상태 갱신, 멱등성 원자 갱신,
  관리자 집계처럼 SQL 의미가 핵심인 기존 경로는 명시적 JDBC 예외로
  유지한다.
- 슬롯의 `RENTING`, `RETURNING` 물리 상태는 제거하고
  `rental`, `return_attempt`, 활성 `device_operation`으로 업무 상태를
  계산한다.
- terminal 장치 이벤트에는 `commandId`, `bootId`, `eventId`,
  `eventType`, 관측 occupancy/lock, evidence schema version이 필요하다.

### 현재 검증 상태

- JDK 21 + Gradle Wrapper `compileTestJava`: `PASSED`
- Docker 비의존 테스트 22개: `PASSED`
  - 장치 계약 fixture 18개
  - FACE_PROFILE 동기화 상태 전이 4개
- Docker Desktop `4.83.0`, Engine/CLI `29.6.2`, Compose `5.3.1`
- MySQL `8.0.46`: Compose `healthy`, 기존 V1 Volume을 V4로 승격
- 같은 MySQL DB 재기동: 현재 버전 4, 신규 Migration 0건
- JPA `ddl-auto=validate`: 애플리케이션 2회 기동 모두 `PASSED`
- 전체 Testcontainers 포함 테스트: 62개, 실패·오류·skip 0
- PostgreSQL `17.10`, pgvector `0.8.5`: Compose `healthy`
- PostgreSQL 001~003 연속 2회 적용 및 verify SQL: `PASSED`
- `face_profile`: 정확히 6개 컬럼, `user_ref` PK, Cross-DB FK 없음

상세 비교, 변경 파일과 재검증 명령은
`docs/backend2-setup/reports/STEP-13-ERD-V2-1-ALIGNMENT.md`에 기록한다.

## STEP-14 Backend 3 P0-P1 최종 스키마 기준선

### 권위와 적용 방식

Backend 3 인계 파일
`billisan_backend3_p0_p1_final_realfinal_schema.sql`을 새 MySQL 기준선으로
채택했다. 이 파일은 빈 DB 전용이며 기존 Flyway DB에는 직접 실행하지
말라고 명시하므로, 같은 인계 패키지의 공식 Flyway V1~V18을
`backend/src/main/resources/db/migration`에 배치했다.
원본 clean-install SQL은 실행 경로와 분리된
`backend/src/main/resources/db/reference`에 동일 SHA-256으로
보관한다.

원본 18개와 저장소 18개의 파일명·SHA-256 비교 결과는 전부 일치한다.
이 절과 STEP-14 보고서는 STEP-05 및 STEP-13의 이전 MySQL 스키마
설명보다 우선한다. 기존 보고서는 당시의 실행 이력으로 보존한다.

### MySQL 최종 기준

| 항목 | 기준 |
| --- | --- |
| MySQL | 8.0.16+, 로컬 실제 8.0.46 |
| Flyway | V1~V18 |
| 로컬 DB | `billisan_p0_p1` |
| 시간대 | 모든 애플리케이션·DB 세션 UTC |
| 일반 업무·계정 테이블 | 11개 |
| 내부 trigger guard | `settlement_payment_mutation_guard` |
| 사용자 업무 ID | `user_account.user_id CHAR(9)`, 정확한 9자리 학번 |
| 외부·얼굴 경계 ID | `user_account.user_ref CHAR(36)`, 불투명 UUID |
| 장치 원장 | 대여·반납 FK 없는 독립 원장 |
| 결제 누적 권위 | MySQL trigger |
| MySQL 얼굴 데이터 | 이미지·embedding·vector 저장 금지 |

기존 ERD v2.1 V1~V4는 Flyway 검색 위치 밖의
`db/legacy-erd-v2_1`에 보존한다. 기존 local `billisan` DB와 named
volume도 삭제하지 않았으며, 새 DB에만 V1~V18을 적용했다.

### 기존 데이터 이전 경계

V13은 UUID 사용자 ID에서 실제 9자리 학번을 추론하지 않으며 사용자
테이블이 비어 있지 않으면 실패하도록 설계되어 있다. 기존 데이터를
이전해야 할 때는 팀이 승인한 다음 매핑이 먼저 필요하다.

```text
기존 UUID user_id -> 실제 9자리 user_id
기존 사용자 -> 새 불투명 user_ref
관련 rental 및 settlement 소유자 FK
```

가드를 우회하거나 임의 학번을 생성해 운영·공유 DB를 변환하지 않는다.

### 실행 검증

- MySQL Compose: `healthy`, `8.0.46`, session `+00:00`
- 신규 `billisan_p0_p1`: Flyway V1~V18 18건 적용
- 같은 DB 재기동: 현재 V18, 신규 적용 0건
- Hibernate schema validation: 통과
- 물리 테이블: 12개 + `flyway_schema_history`
- trigger: 9개
- MySQL 얼굴 이미지·embedding·vector 컬럼: 0개
- `clean build --rerun-tasks`: `PASSED`
- 7 suites, 62 tests, 실패·오류·skip 0

상세 근거는
`docs/backend2-setup/reports/STEP-14-BACKEND3-P0-P1-SCHEMA.md`에 기록한다.

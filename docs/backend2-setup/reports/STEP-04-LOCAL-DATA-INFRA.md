# STEP-04 Local Data Infrastructure 보고서

## 판정

`PASSED`

MySQL Compose와 PowerShell 스크립트의 정적 검증에 이어 사용자별
Docker Desktop을 기동하고 실제 MySQL 8.0 컨테이너와 Healthcheck까지
검증했다.

## 선행 조건

- STEP-01: `PASSED`
- STEP-02: `PASSED`
- STEP-03: `PASSED`
- 현재 브랜치: `S15P11A201-55-be-db`
- Commit/Push/MR: 수행하지 않음

## 입력값 판정

| 입력 | 전달값 | 판정 |
| --- | --- | --- |
| `MYSQL_VERSION` | `8.0` | 확정값으로 사용 |
| `POSTGRES_MODE` | `<docker-local\|shared-orin\|skip-until-confirmed>` | 미치환 placeholder |
| `PGVECTOR_IMAGE` | `<팀 확정 이미지 또는 UNCONFIRMED>` | 미확정 |

PostgreSQL 관련 입력이 실제 값으로 치환되지 않았으므로 임의로
`docker-local` 또는 `shared-orin`을 선택하지 않았다. 안전한 동작으로
`skip-until-confirmed`를 적용하고 PostgreSQL 서비스, 접속 변수 및
연결 검증 스크립트를 생성하지 않았다.

## 기존 구성 조사

현재 작업 트리 전체에서 다음 파일 패턴을 조사했다.

- `compose*.yml`, `compose*.yaml`
- `docker-compose*.yml`, `docker-compose*.yaml`
- `Dockerfile*`
- PowerShell/Shell/Batch 개발 스크립트

기존 DB Compose와 개발 스크립트는 없었다. 발견된 실행 스크립트는
Gradle Wrapper `backend/gradlew.bat`뿐이었다. 따라서 기존
Compose를 교체하거나 중복 생성하지 않고 Spring 프로젝트 루트에
`docker-compose.yml`을 추가했으며, 이후 `develop`의 Web·MQTT 구성과
같은 파일로 통합했다.

## 사용한 Compose

경로:

```text
backend/docker-compose.yml
```

현재 루트 Compose에는 `app`, `mosquitto`, `db`가 있으며, 이 단계의
MySQL 검증 대상 서비스는 `db`다. PostgreSQL은 이 Compose에 없다.

| 항목 | 구성 |
| --- | --- |
| image | `mysql:${MYSQL_VERSION:-8.0}` |
| host port | `${MYSQL_PORT:-3306}` |
| database | `${MYSQL_DATABASE:-billisan_p0_p1}` |
| application user | `${MYSQL_USERNAME:-billisan}` |
| application password | 필수 `${MYSQL_PASSWORD}` |
| root password | 필수 `${MYSQL_ROOT_PASSWORD}` |
| volume | `db-data:/var/lib/mysql` |
| character set | MySQL 8.0 기본 `utf8mb4` |
| collation | MySQL 8.0 기본 정책 |
| timezone | UTC, `--default-time-zone=+00:00` |
| healthcheck | `mysqladmin ping`, 10초 간격, 최대 5회 |
| restart | `unless-stopped` |

기존 STEP-03의 MySQL URL이 `serverTimezone=UTC`을 사용하므로
동일한 로컬 시간대 기준을 유지했다. 기존 문자셋 정책은 저장소에서
확인되지 않아 MySQL 8의 유니코드 기준으로 `utf8mb4`와
`utf8mb4_0900_ai_ci`를 명시했다.

## 환경 변수 예제

`backend/.env.example`에 다음 변수를 기록했다.

```text
SPRING_PROFILES_ACTIVE
MYSQL_ROOT_PASSWORD
MYSQL_DATABASE
MYSQL_PORT
MYSQL_URL
MYSQL_USERNAME
MYSQL_PASSWORD
```

`MYSQL_PASSWORD`와 `MYSQL_ROOT_PASSWORD`에는 실제 Secret이 아닌
`changeme` placeholder만 둔다. 실제 `.env`는 `.gitignore` 규칙으로
제외한다.

MySQL host port 또는 DB명을 기본값과 다르게 사용할 경우 Spring의
`MYSQL_URL`도 같은 값으로 맞춰야 한다.

## PowerShell 스크립트

| 목적 | 파일 | 내부 Compose 동작 |
| --- | --- | --- |
| DB 시작 | `backend/scripts/db-start.ps1` | `up -d --wait db` |
| DB 중지 | `backend/scripts/db-stop.ps1` | `stop db` |
| DB 상태 | `backend/scripts/db-status.ps1` | `ps db` |
| DB 로그 | `backend/scripts/db-logs.ps1` | `logs --tail ... db` |

모든 스크립트는 자신의 위치를 기준으로 프로젝트 루트를 계산하므로
어느 디렉터리에서 호출해도 동일한 `backend/docker-compose.yml`을
사용한다.
PowerShell scriptblock 파싱 결과 네 파일 모두 구문 오류가 없었다.

중지 스크립트는 컨테이너만 중지하며 named volume을 보존한다.
`docker compose down -v`는 어떤 스크립트에도 포함하지 않았다.

## PostgreSQL+pgvector 상태

적용 모드: `skip-until-confirmed`

- PostgreSQL/pgvector Compose 서비스 없음
- pgvector image 고정 없음
- shared Orin 접속 변수 없음
- 연결 검증 스크립트 없음
- Spring PostgreSQL URL 또는 두 번째 DataSource 없음

다음 중 하나가 명시적으로 전달되어야 PostgreSQL 작업을 재개할 수 있다.

1. `POSTGRES_MODE=docker-local`과 확정된 `PGVECTOR_IMAGE`
2. `POSTGRES_MODE=shared-orin`과 승인된 접속 변수 계약
3. `POSTGRES_MODE=skip-until-confirmed` 유지

## Spring과 Orin DB 경계

`backend/src/main/resources/application-local.yaml`을 검사한
결과 `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`만 참조한다.
PostgreSQL, pgvector, Redis 또는 Pi용 DB 설정은 없다.

Compose에도 MySQL 서비스만 있어 Spring 업무 DB와 Orin DB의 연결
경계가 유지된다.

## 검증 결과

| 검증 | 결과 | 근거 |
| --- | --- | --- |
| Compose YAML 파싱 | `PASSED` | SnakeYAML 2.6으로 파싱 |
| 서비스 경계 | `PASSED` | 루트 Compose `app`, `mosquitto`, `db`; PostgreSQL 서비스 없음 |
| 필수 MySQL 키 | `PASSED` | image, ports, volume, healthcheck 존재 |
| Secret 분리 | `PASSED` | 예제에는 비밀번호 값이 없고 실제 `.env`는 Git ignore |
| PowerShell 구문 | `PASSED` | 4개 스크립트 scriptblock 파싱 |
| Volume 삭제 방지 | `PASSED` | 스크립트에 `down -v` 없음 |
| Spring PostgreSQL 경계 | `PASSED` | 애플리케이션 설정에 PostgreSQL URL 없음 |
| `docker compose config --quiet` | `PASSED` | Docker Compose `v5.3.1` |
| MySQL 기동 | `PASSED` | `mysql:8.0`, 실제 서버 `8.0.46` |
| MySQL Healthcheck | `PASSED` | 컨테이너 상태 `healthy` |
| 호스트 포트 | `PASSED` | `127.0.0.1:3306` TCP 연결 성공 |
| 업무 DB 접속 | `PASSED` | 컨테이너 업무 사용자로 `billisan_p0_p1` 조회 |
| 문자셋·Collation | `PASSED` | `utf8mb4`, `utf8mb4_0900_ai_ci` |
| 타임존 | `PASSED` | session `+00:00` |
| 영속 Volume | `PASSED` | `db-data:/var/lib/mysql` |
| PostgreSQL 기동/Healthcheck | `NOT_APPLICABLE` | `skip-until-confirmed` |

## 실행 명령

PowerShell에서 다음 순서로 실행한다.

```powershell
Set-Location C:\Users\SSAFY\S15P11A201
docker compose -f .\backend\docker-compose.yml config --quiet
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\db-start.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\db-status.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\db-logs.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\db-logs.ps1 -Follow
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\db-stop.ps1
```

위 명령은 프로세스 단위로만 실행 정책을 우회한다. 시스템 또는 사용자
실행 정책은 변경하지 않는다.

`.env`에서 최소한 `MYSQL_PASSWORD`와 `MYSQL_ROOT_PASSWORD`를 실제
로컬 Secret으로 채워야 한다. `.env`는 Commit하지 않는다.

## 데이터 삭제 위험 명령

다음 명령은 named volume과 모든 로컬 MySQL 데이터를 삭제할 수 있다.
기본 스크립트에 포함하지 않았고 이번 단계에서 실행하지 않았다.

```powershell
docker compose -f .\backend\docker-compose.yml down -v
```

백업과 명시적 삭제 승인이 있을 때만 별도로 실행해야 한다.

## 변경 파일

- `backend/docker-compose.yml`
- `backend/.env.example`
- `backend/scripts/db-start.ps1`
- `backend/scripts/db-stop.ps1`
- `backend/scripts/db-status.ps1`
- `backend/scripts/db-logs.ps1`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-04-LOCAL-DATA-INFRA.md`

## 추천 Commit 메시지

```text
S15P11A201-55 chore: MySQL 로컬 Compose 기준선 구성
```

## 다음 실행 프롬프트

`STEP-05`

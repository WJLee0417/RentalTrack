# Billisan Backend 2 로컬 실행 Runbook

## 1. 기준 환경

- OS: Windows
- Shell: PowerShell
- JDK: Eclipse Temurin 21
- Build: Gradle Wrapper 9.5.1
- MySQL: Docker `mysql:8.0`
- PostgreSQL: Docker `pgvector/pgvector:0.8.5-pg17-bookworm`
- PostgreSQL 모드: `docker-local`

명령은 저장소 루트 `C:\Users\SSAFY\S15P11A201`에서 시작한다. 실제
비밀번호와 토큰은 Git에 기록하지 않는다.

## 2. 최초 준비

### JDK 21

현재 PowerShell 프로세스에만 JDK 21을 지정한다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

둘 다 `21`이어야 한다.

### 로컬 env

예제 파일을 복사하고 비민감 `changeme` placeholder를 각 개발자의 로컬
값으로 교체한다.

```powershell
Copy-Item `
  -LiteralPath .\backend\.env.example `
  -Destination .\backend\.env

Copy-Item `
  -LiteralPath .\infra\orin-postgres\.env.example `
  -Destination .\infra\orin-postgres\.env
```

확인 사항:

- `backend/.env`: `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`
- `infra/orin-postgres/.env`: `ORIN_POSTGRES_PASSWORD`
- pgvector image와 digest는 `.env.example` 값을 유지
- `.env` 두 파일은 Git ignore 대상

```powershell
git check-ignore -v `
  .\backend\.env `
  .\infra\orin-postgres\.env
```

명령이 두 파일의 ignore 규칙을 출력해야 한다. `.env` 내용을 터미널,
이슈, MR 또는 채팅에 붙여 넣지 않는다.

## 3. MySQL 시작·상태·로그·중지

PowerShell 실행 정책 영향을 피하려면 다음 형식을 사용한다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\backend\scripts\db-start.ps1

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\backend\scripts\db-status.ps1

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\backend\scripts\db-logs.ps1

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\backend\scripts\db-stop.ps1
```

정상 상태는 `billisan-db`이 `healthy`인 상태다. stop 스크립트는
named volume을 삭제하지 않는다.

Compose만 검증하려면 다음을 사용한다.

```powershell
docker compose -f .\backend\docker-compose.yml config --quiet
docker compose -f .\backend\docker-compose.yml ps
```

## 4. Spring local 실행

MySQL이 `healthy`인지 먼저 확인한다. `backend`으로 이동하고
local env 값을 현재 PowerShell 환경 변수로 설정한다.

```powershell
Set-Location .\backend

$env:SPRING_PROFILES_ACTIVE = 'local'
$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/billisan_p0_p1?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC'
$env:MYSQL_USERNAME = 'billisan'
$env:MYSQL_PASSWORD = '<로컬 비밀번호>'

.\gradlew.bat bootRun
```

`<로컬 비밀번호>`는 실제 로컬 값으로 치환하되 파일이나 문서에 Commit하지
않는다. Spring은 MySQL DataSource 하나만 사용한다. PostgreSQL URL을
Spring 환경 변수에 추가하지 않는다.

Spring 시작 시 Flyway가 Backend 3 P0-P1 인계 기준 V1~V18을 순서대로
적용한다. 빈 로컬 DB 이름은 `billisan_p0_p1`이며 모든 Spring·DB
세션은 UTC를 사용한다. 일반 업무·계정 테이블 11개와 내부 결제 trigger
guard 1개가 생성된다. 같은 DB에서 다시 시작하면 적용된 Migration은
재실행하지 않는다.

인계받은 `billisan_backend3_p0_p1_final_realfinal_schema.sql`은 빈 DB
clean-install 전용이다. Flyway가 관리하는 DB에는 이 파일을 직접
실행하지 않는다. 기존 local `billisan` DB는 자동 변환하거나 삭제하지
않으며, 이전이 필요하면 UUID 사용자 ID와 실제 9자리 학번의 팀 승인
매핑을 먼저 준비한다.

## 5. 빌드와 전체 테스트

`backend`에서 실행한다.

```powershell
.\gradlew.bat --version
.\gradlew.bat clean test --no-daemon
.\gradlew.bat bootJar --no-daemon
```

Docker Desktop이 실행 중이어야 Testcontainers MySQL 테스트를 수행할 수
있다. 정상 기준:

- 7 test suites
- 62 tests
- failures 0
- errors 0
- skipped 0

결과는 다음 경로에서 확인한다.

```text
backend/build/reports/tests/test/index.html
backend/build/test-results/test/TEST-*.xml
```

부분 재검증 명령:

```powershell
.\gradlew.bat test --tests com.ssafy.billisan.BillisanApplicationTests
.\gradlew.bat test --tests com.ssafy.billisan.devicecontract.DeviceContractFixtureTest
.\gradlew.bat test --tests com.ssafy.billisan.faceprofilesync.FaceProfileSyncOperationEntityTest
.\gradlew.bat test --tests com.ssafy.billisan.idempotency.IdempotencyServiceTests
.\gradlew.bat test --tests com.ssafy.billisan.slot.SlotAllocationServiceTests
.\gradlew.bat test --tests com.ssafy.billisan.snapshot.BootSnapshotReconciliationServiceTests
.\gradlew.bat test --tests com.ssafy.billisan.inventory.InventoryQueryRepositoryTests
```

H2 결과로 MySQL Migration·UNIQUE·Transaction·동시성을 대체하지 않는다.

## 6. PostgreSQL+pgvector

저장소 루트에서 실행한다.

```powershell
docker compose -f .\infra\orin-postgres\compose.yaml config --quiet

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-apply.ps1

powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\infra\orin-postgres\scripts\pgvector-verify.ps1
```

정상 기준:

- container `healthy`
- PostgreSQL 17
- `vector` Extension 조회
- `public.face_profile` 6개 컬럼
- `user_ref UUID` PRIMARY KEY
- `embedding vector`이며 차원 미지정
- Cross-DB FK 없음
- 이름·학번·이메일·이미지·경로 컬럼 없음

Docker Hub CDN에서 `EOF`가 재발하면 임의 태그로 바꾸지 않는다.
`.env.example`에 기록된 `mirror.gcr.io` 경로와 확정 digest를 사용한다.
canonical image와 digest가 달라지면 팀 승인 전 적용하지 않는다.

중지:

```powershell
docker compose -f .\infra\orin-postgres\compose.yaml down
```

기본 중지는 named volume을 보존한다.

## 7. 장치 계약 Mock

운영 Controller는 아직 없다. 아래 파일은 계약 재현과 fixture 검증용이다.

```text
backend/src/test/resources/device-contract/device-gateway.http
backend/src/test/resources/device-contract/v1/*.json
```

HTTP Client 파일의 `mock-device-token`은 가짜 값이다. 실제 장치 Token이나
운영 Base URL로 바꾼 파일을 Commit하지 않는다.

fixture 검증:

```powershell
Set-Location .\backend
.\gradlew.bat test `
  --tests com.ssafy.billisan.devicecontract.DeviceContractFixtureTest
```

## 8. Seed와 조회

Seed는 `src/test/java`의 `Backend2QueryScenarioFixture`에만 있다. 전용
Testcontainers DB `billisan_query_validation_test`가 아니면 실행을
거부한다.

```powershell
Set-Location .\backend
.\gradlew.bat test `
  --tests com.ssafy.billisan.inventory.InventoryQueryRepositoryTests
```

local 또는 prod Profile에 자동 Seed를 추가하지 않는다.

## 9. 문제 해결

### Docker Desktop이 실행되지 않음

```powershell
docker version
docker info
```

server 정보가 없으면 Docker Desktop을 시작한 뒤 다시 확인한다.

### PowerShell profile 실행 정책 경고

Repository 스크립트는 다음과 같이 실행한다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File <스크립트 경로>
```

시스템 ExecutionPolicy를 임의 변경할 필요는 없다.

### 포트 충돌

- MySQL: `backend/.env`의 `MYSQL_PORT`
- PostgreSQL: `infra/orin-postgres/.env`의 `ORIN_POSTGRES_PORT`

포트를 바꾸면 `MYSQL_URL` 또는 로컬 접속 도구의 포트도 맞춘다.

### Testcontainers image pull 실패

```powershell
docker pull mysql:8.0
docker image ls mysql
```

네트워크·registry 문제를 해결하지 못한 상태에서 테스트를 skip하거나
H2로 바꾸지 않는다.

## 10. 안전한 중지와 롤백

데이터를 보존하는 중지:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\backend\scripts\db-stop.ps1

docker compose -f .\infra\orin-postgres\compose.yaml down
```

코드 롤백은 공유 이력을 강제 Reset하지 않고 대상 Commit을 확인한 뒤
`git revert <commit>`으로 수행한다. Flyway history를 수동 삭제하거나
기존 V1~V18 파일을 수정하지 않는다. 이미 배포된 스키마 변경은 새 보정
Migration 또는 검증된 백업 복원으로 처리한다.

다음 명령은 named volume과 로컬 DB 데이터를 영구 삭제하는 위험 명령이다.
기본 절차로 실행하지 않는다.

```powershell
# 위험: 정확한 Compose project와 백업을 확인한 뒤 사용자가 직접 실행
docker compose -f .\backend\docker-compose.yml down -v
docker compose -f .\infra\orin-postgres\compose.yaml down -v
```

`git reset --hard`, `git clean -fd`, 강제 Checkout은 Runbook 절차에 없다.

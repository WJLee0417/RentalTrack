# STEP-06 PostgreSQL+pgvector Migration 보고서

## 판정

`PASSED`

공식 pgvector `0.8.5` + PostgreSQL `17` Bookworm image와 multi-platform
digest를 팀 확정값으로 채택했다. Docker Hub CloudFront 직접 다운로드는
계속 `EOF`였지만 Google의 공식 Docker Hub cache가 제공하는 동일 OCI
digest를 검증해 다운로드했다. 컨테이너 기동, Migration 2회 적용,
Extension·버전·FACE_PROFILE 메타데이터 조회를 모두 완료했다.

## 선행 조건

| 항목 | 결과 |
| --- | --- |
| STEP-01 | `PASSED` |
| STEP-02 | `PASSED` |
| STEP-03 | `PASSED` |
| STEP-04 | `PASSED` |
| STEP-05 | `PASSED` |
| 요청 모드 | `POSTGRES_MODE=docker-local` |
| canonical image | `pgvector/pgvector:0.8.5-pg17-bookworm@sha256:d2ef61f42ef767baa5a1475393303cc235bcd92febd9d7014eddb48b41f3bad0` |
| local pull source | `mirror.gcr.io/pgvector/pgvector:0.8.5-pg17-bookworm` + 동일 digest |
| PostgreSQL | 실제 서버 `17.10 (Debian 17.10-1.pgdg12+1)` |
| pgvector | `0.8.5` |
| 필수 플랫폼 | `linux/amd64`, `linux/arm64` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| Commit/Push/MR | 수행하지 않음 |

## 조사 범위와 권위 근거

현재 작업 트리에서 다음을 검색했다.

- `face_profile`, `FACE_PROFILE`, `faceProfile`
- PostgreSQL, pgvector, `CREATE EXTENSION`, `VECTOR`
- SQL·Python 파일과 모델·Migration 디렉터리
- Compose·Dockerfile·PowerShell·Shell 스크립트
- Spring 설정, DataSource, Entity, Repository 및 DB 의존성

기존 FACE_PROFILE 스키마, PostgreSQL Migration, Python 모델, Orin DB
소유 경로 및 PostgreSQL Compose는 발견되지 않았다. 문서의 PostgreSQL
언급과 STEP-04의 미확정 환경 변수 예제만 확인됐다. 이 결론은 현재
작업 트리 전체의 파일명과 텍스트 검색 범위에 한하며, 저장소 밖의
Backend 1·Orin 배포 저장소 부재를 단정하지 않는다.

필드 권위 근거는 Notion
`08. Billisan ERD 및 데이터 아키텍처 문서 v1.0.1`이다. 이 문서는
FACE_PROFILE의 9개 필드, ACTIVE/INACTIVE 저장 상태, 원본 얼굴 데이터와
개인정보 금지 및 MySQL `user_id`와의 논리 참조를 정의한다.

- Notion 원문:
  `https://app.notion.com/p/2db443b01b138220904d01f4677e132b`
- pgvector 공식 문서:
  `https://github.com/pgvector/pgvector`
- PostgreSQL UUID 공식 문서:
  `https://www.postgresql.org/docs/17/datatype-uuid.html`
- Google Artifact Registry Docker Hub cache:
  `https://docs.cloud.google.com/artifact-registry/docs/pull-cached-dockerhub-images`

## 소유 위치 결정

기존 소유 위치가 없으므로 다음 중립 경로를 사용했다.

```text
infra/orin-postgres
```

이 위치는 `backend`의 Spring 애플리케이션과 `ai`의 Backend 1
실행환경 어느 쪽에도 PostgreSQL 접근 책임을 추가하지 않는다. SQL은
Spring Flyway 경로에 넣지 않았으며 독립 PowerShell 스크립트가
컨테이너 내부 `psql`로 실행한다.

## 생성한 PostgreSQL 구성

| 목적 | 파일 |
| --- | --- |
| 비민감 환경 변수 계약 | `infra/orin-postgres/.env.example` |
| 실제 `.env` 제외 | `infra/orin-postgres/.gitignore` |
| 독립 PostgreSQL Compose | `infra/orin-postgres/compose.yaml` |
| vector Extension | `migrations/001_enable_vector_extension.sql` |
| FACE_PROFILE 생성 | `migrations/002_create_face_profile.sql` |
| 공통 환경·안전 검사 | `scripts/OrinPostgres.Common.ps1` |
| 기동·Migration 적용 | `scripts/pgvector-apply.ps1` |
| 스키마 검증 | `scripts/pgvector-verify.ps1` |
| Extension·메타데이터 SQL | `verification/verify_face_profile.sql` |

Compose는 `PGVECTOR_IMAGE`, DB명, 사용자, 비밀번호가 모두 명시돼야
하며 named volume과 `pg_isready` Healthcheck를 사용한다.
`.env.example`에는 동일 digest의 공식 cache pull source를 기록하고
비밀번호는 비워 뒀다. 실제 로컬 `.env`는 Git ignore 대상이다.
PostgreSQL 호스트 포트는 `127.0.0.1`에만 bind한다.
apply/verify 스크립트는 image가 비어 있거나 `UNCONFIRMED`이면 Docker
호출 전에 계속 중단한다.

## FACE_PROFILE 구조

물리 테이블은 `face_profile`이며 확정된 9개 컬럼만 포함한다.

| 논리 항목 | 물리 컬럼 | PostgreSQL 타입 | 제약 |
| --- | --- | --- | --- |
| `faceProfileId` | `face_profile_id` | `UUID` | PK, NOT NULL |
| `userId` | `user_ref` | `UUID` | NOT NULL, UNIQUE |
| embedding | `embedding` | `VECTOR` | NOT NULL, 차원 미지정 |
| 등록·활성 상태 | `registration_status` | `VARCHAR(20)` | NOT NULL, ACTIVE/INACTIVE CHECK |
| 모델 버전 | `model_version` | `VARCHAR(50)` | NULL 허용 |
| 템플릿 버전 | `template_version` | `INTEGER` | NOT NULL, 기본값 1 |
| 등록 시각 | `registered_at` | `TIMESTAMPTZ` | NULL 허용 |
| 갱신 시각 | `updated_at` | `TIMESTAMPTZ` | NOT NULL |
| 비활성 시각 | `deactivated_at` | `TIMESTAMPTZ` | NULL 허용 |

embedding 차원은 확정 자료에 없으므로 `VECTOR(n)`의 `n`을 임의로
정하지 않았다. pgvector는 차원 없는 `vector` 타입을 허용하므로
계약 확정 전의 컬럼 정의로 사용했지만, 같은 차원을 전제로 하는 vector
인덱스도 만들지 않았다. 모델 출력 차원과 검색 방식을 확정한 뒤 별도
Migration으로 타입 차원과 인덱스를 검토해야 한다.

UUID 자동 생성 기본값도 확정 자료에 없으므로 추가하지 않았다.
`face_profile_id`와 `user_ref` 값은 Orin 소유 쓰기 경계가 검증해
명시적으로 전달한다.

## Extension·Migration 검증 상태

| 검증 | 결과 | 근거 |
| --- | --- | --- |
| PowerShell 3개 파일 구문 | `PASSED` | `[ScriptBlock]::Create` |
| `UNCONFIRMED` image 차단 | `PASSED` | apply/verify 모두 종료 코드 1 |
| 확정 index digest | `PASSED` | registry 결과 `sha256:d2ef61f4...` 일치 |
| `linux/amd64` manifest | `PASSED` | `sha256:815bf537...` |
| `linux/arm64` manifest | `PASSED` | `sha256:555f6d1b...` |
| `docker compose config --quiet` | `PASSED` | Docker Compose v5.3.1 |
| Docker Desktop/Containers proxy | `PASSED` | UI에서 둘 다 `No proxy`, 저장값 `disabled` |
| canonical CloudFront pull | `ENVIRONMENT_BLOCKED` | 직접 blob 전송 `EOF` 지속 |
| 공식 cache digest | `PASSED` | canonical index·하위 manifest digest와 일치 |
| image pull | `PASSED` | `mirror.gcr.io`, image ID가 index digest와 일치 |
| 실제 apply 1회차 | `PASSED` | Extension·테이블 최초 생성 |
| 실제 apply 2회차 | `PASSED` | 기존 객체 skip, 오류 없음 |
| 컨테이너 기동·Healthcheck | `PASSED` | `healthy` |
| `CREATE EXTENSION vector` 적용 | `PASSED` | `vector` 존재 |
| Extension 조회 | `PASSED` | `vector 0.8.5` |
| FACE_PROFILE Migration 적용 | `PASSED` | `public.face_profile` 생성 |
| 실제 메타데이터·제약 조회 | `PASSED` | 9개 컬럼, PK·UNIQUE·CHECK |
| 호스트 연결·영속성 | `PASSED` | `127.0.0.1:5432`, named volume |

Docker Desktop proxy와 Containers proxy를 `System proxy`에서
`No proxy`로 바꿨지만 CloudFront 직접 pull의 `EOF`는 재현됐다.
Windows 시스템 프록시·PAC·WinHTTP가 모두 직접 연결이므로 Docker 설정이
아닌 현재 네트워크의 CloudFront blob 경로 문제로 분리했다.

Google Artifact Registry의 공식 Docker Hub cache에서 동일 tag를 조회한
결과 canonical index digest, `linux/amd64`, `linux/arm64` 및 attestation
manifest digest가 모두 일치했다. 따라서 image 내용이나 공급 버전을
변경하지 않고 전송 경로만 `mirror.gcr.io`로 바꿨다.

필수 조회 SQL은 검증 파일의 첫 문장으로 포함했다.

```sql
SELECT extname
FROM pg_extension
WHERE extname = 'vector';
```

검증 SQL은 추가로 다음을 실패 조건으로 검사한다.

- `vector` Extension 부재
- `public.face_profile` 부재
- 확정된 9개 이외 컬럼 존재
- `embedding`이 차원 미지정 `vector`가 아님
- 이름·학번·이메일·이미지·경로 계열 컬럼 존재
- Foreign Key 존재
- PK, `user_ref` UNIQUE, 등록 상태 CHECK 부재

실제 필수 쿼리 결과는 `vector` 1행이며 `extversion=0.8.5`다.
FACE_PROFILE은 9개 컬럼과 3개 제약을 반환했고 검증 DO block도
예외 없이 통과했다. 업무 행 수는 `0`이다.

## Cross-DB `userId` 논리 참조

| 항목 | MySQL | PostgreSQL |
| --- | --- | --- |
| 물리 컬럼 | `user_account.user_id` | `face_profile.user_ref` |
| 타입 | `CHAR(36)` | `UUID` |
| 문자 규칙 | `ascii`, `ascii_bin` | 네이티브 128-bit UUID |
| NULL | 금지 | 금지 |
| 고유성 | 사용자 PK | 사용자당 현재 프로필 1건 UNIQUE |
| 물리 FK | 해당 없음 | 만들지 않음 |

실제 MySQL 메타데이터는 `char(36)`, `ascii`, `ascii_bin`, NOT NULL이고
PostgreSQL `user_ref`는 `uuid`, NOT NULL이다.

서비스 간 전송 규칙은 소문자 하이픈 UUID 문자열
`xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` 36자로 고정한다. Orin 쓰기
경계는 입력 문자열을 UUID로 파싱해 유효성을 확인하고 PostgreSQL
`UUID`로 저장한다. 조회 응답은 같은 정규 문자열로 직렬화한다.

MySQL은 `ascii_bin`이라 문자열 대소문자를 구분하지만 PostgreSQL UUID는
표현이 아닌 UUID 값으로 비교한다. 따라서 모든 생성·전송 경계에서
소문자 정규형을 강제해 같은 UUID의 표현 불일치를 막는다.

`user_ref`는 논리 참조일 뿐 다음 기능을 제공하지 않는다.

- Cross-DB Foreign Key 또는 cascade
- MySQL과 PostgreSQL의 직접 JOIN
- PostgreSQL의 이름·학번·이메일 복제
- Spring Boot의 PostgreSQL 조회

사용자 비활성·삭제 시 FACE_PROFILE을 어떻게 비활성화할지는 향후
팀이 확정할 API 또는 이벤트 계약의 책임이다.

## 개인정보·Spring 경계 검증

Migration에는 이름, 학번, 이메일, 얼굴 원본 이미지, 얼굴 이미지 경로가
없다. 샘플 데이터도 만들지 않았다. embedding 차원이 미확정이어서
의미 있는 가짜 vector조차 안전하게 구성할 수 없고, 메타데이터 검증에는
샘플 행이 필요하지 않기 때문이다.

`backend`의 main 소스와 설정에는 PostgreSQL JDBC URL,
pgvector 접근, 두 번째 DataSource, FACE_PROFILE Entity 또는 Repository를
추가하지 않았다. MySQL Flyway Migration도 변경하지 않았다.

## 차단된 결정

| 결정 | 현재 상태 | 해제에 필요한 입력 |
| --- | --- | --- |
| PostgreSQL+pgvector image | 확정 | 공식 image + multi-platform digest |
| PostgreSQL 버전 | 실제 `17.10` 확인 | 완료 |
| pgvector 버전 | 실제 `0.8.5` 확인 | 완료 |
| Docker CDN 다운로드 | 공식 cache로 해결 | canonical과 동일 digest 유지 |
| embedding 차원 | 미확정 | 실제 얼굴 모델 출력 차원 |
| vector 인덱스·거리 함수 | 미확정 | 검색량·거리 metric·운영 요구 |
| 사용자 삭제·비활성 동기화 | 미확정 | 서비스 간 API/이벤트 계약과 재시도 정책 |
| 운영 Migration 소유 주체 | 임시로 독립 스크립트 | Orin 배포 저장소·CI/CD 책임자 확정 |

image와 버전 결정은 완료됐다. embedding 차원 미확정은 차원 없는
`VECTOR` 테이블 생성을 막지 않지만, 차원 고정과 vector 인덱스 생성을
계속 차단한다.

## 재실행 명령

PowerShell에서 다음을 실행한다.

```powershell
Set-Location C:\Users\SSAFY\S15P11A201\infra\orin-postgres
if (-not (Test-Path -LiteralPath .env)) {
    Copy-Item -LiteralPath .env.example -Destination .env
}
```

현재 로컬 `.env`는 생성돼 있고 Git ignore 대상이다. 새 환경에서는
`.env`의 로컬 Secret을 반드시 채운다.

```text
POSTGRES_MODE=docker-local
PGVECTOR_IMAGE=mirror.gcr.io/pgvector/pgvector:0.8.5-pg17-bookworm@sha256:d2ef61f42ef767baa5a1475393303cc235bcd92febd9d7014eddb48b41f3bad0
ORIN_POSTGRES_PORT=5432
ORIN_POSTGRES_DATABASE=billisan_face_local
ORIN_POSTGRES_USERNAME=billisan_face_local
ORIN_POSTGRES_PASSWORD=<로컬 Secret>
```

적용을 실행하고 검증한다. 이번 단계에서는 적용을 두 번 실행해
재실행 안전성까지 확인했다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\pgvector-apply.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\pgvector-apply.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\pgvector-verify.ps1
```

apply 스크립트는 `docker compose config`, 컨테이너
`up -d --wait`, Extension SQL, FACE_PROFILE SQL 순서로 실행한다.
동일 명령의 재실행은 기존 Extension과 테이블을 건너뛰며 성공했다.

Volume 삭제 명령은 스크립트에 포함하지 않았고 이번 단계에서도
실행하지 않았다.

## 변경 파일

- `infra/orin-postgres/.env.example`
- `infra/orin-postgres/.env` (로컬 전용, Git ignore)
- `infra/orin-postgres/compose.yaml`
- `docs/backend2-setup/CONTEXT.md`
- `docs/backend2-setup/PROGRESS.md`
- `docs/backend2-setup/reports/STEP-06-PGVECTOR-MIGRATION.md`

## 추천 Commit 메시지

```text
S15P11A201-55 chore: pgvector 로컬 Migration 검증 완료
```

`ALLOW_GIT_COMMIT=false`에 따라 Commit, Push, MR은 수행하지 않았다.

## 다음 실행 프롬프트

`STEP-07`

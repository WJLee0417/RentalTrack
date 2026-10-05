# STEP-14 Backend 3 P0-P1 스키마 교체

## 단계 판정

`PASSED`

Backend 3 인계용 최종 MySQL 스키마의 지시에 따라 clean-install SQL을
기존 Flyway DB에 직접 실행하지 않고, 같은 패키지의 공식 Flyway
V1~V18을 애플리케이션 Migration 기준선으로 교체했다. 기존 로컬
`billisan` 데이터베이스와 named volume은 삭제하지 않았으며,
`billisan_p0_p1` 데이터베이스를 새로 만들어 검증했다.

## 권위 입력

| 항목 | 값 |
| --- | --- |
| clean-install schema | `billisan_backend3_p0_p1_final_realfinal_schema.sql` |
| 저장소 보관 경로 | `backend/src/main/resources/db/reference/billisan_backend3_p0_p1_final_clean_schema.sql` |
| SHA-256 | `751A0864F369605BDE118844F6F331E688F366AC51E552E5A6654D0ADAEBA7DC` |
| 대상 | MySQL 8.0.16+ |
| 기준 Migration | Flyway V1~V18 |
| 세션 시간대 | UTC (`+00:00`) |
| 입력 문서의 적용 규칙 | 빈 DB는 clean schema, 기존 Flyway DB는 V1~V18 사용 |

가져온 18개 Migration은 원본 패키지
`p0_4_flyway/migrations`와 파일명 및 SHA-256을 전수 비교했으며 차이가
0건이다. 이전 ERD v2.1 기반 V1~V4는 실행 경로에서 제거하되 감사와
비교를 위해 `src/main/resources/db/legacy-erd-v2_1`에 보존했다.

## 최종 물리 기준

| 항목 | 확인 결과 |
| --- | --- |
| Flyway | V1~V18, 성공 이력 18건 |
| 일반 업무·계정 테이블 | 11개 |
| 내부 테이블 | `settlement_payment_mutation_guard` 1개 |
| 전체 물리 테이블 | 12개 + `flyway_schema_history` |
| 트리거 | 9개 |
| 사용자 업무 PK | `user_account.user_id CHAR(9)` |
| Cross-DB 식별자 | `user_account.user_ref CHAR(36)` UNIQUE |
| 장치 원장 | 대여·반납 FK가 없는 독립 `device_operation` |
| MySQL 얼굴 이미지·embedding·vector | 0개 |

물리 테이블은 다음과 같다.

```text
admin_account
damage_inspection
device_operation
face_profile_sync_operation
payment_attempt
rental
return_attempt
settlement
settlement_payment_mutation_guard
slot
station
user_account
```

## 애플리케이션 정렬

- FACE_PROFILE 동기화는 학번 `user_id`가 아닌 불투명 UUID
  `user_ref`만 사용한다.
- `sync_operation_id`가 동기화 원장의 PK이며 `template_version`은 모든
  작업에서 양수 필수다.
- 장치 원장은 스키마 정의대로 `rental_id`, `return_attempt_id`를
  저장하지 않는다.
- 결제 성공 시 settlement 누적 반영은 애플리케이션의 중복 UPDATE가
  아니라 DB 트리거가 단일 권위로 수행한다.
- 모든 Spring·JDBC 업무 시각과 MySQL 세션은 UTC를 사용한다.
- 슬롯 선정은 공식 생성 컬럼 UNIQUE와 `FOR UPDATE SKIP LOCKED`를 함께
  사용하며 제거된 후보 인덱스를 강제하지 않는다.
- 테스트 Fixture는 9자리 사용자 ID, 별도 UUID user_ref, 대여·반납
  상태 트리거와 감사시간 제약을 따른다.

## Docker·Migration 실행 결과

| 검증 | 결과 |
| --- | --- |
| Compose MySQL | `mysql:8.0`, 실제 `8.0.46`, `healthy` |
| MySQL session time zone | `+00:00` |
| 신규 DB | `billisan_p0_p1` |
| 기존 DB 보존 | `billisan`과 named volume 모두 존재 |
| 최초 Spring local 기동 | V1~V18 18건 적용 성공 |
| JPA schema validate | 성공 |
| 동일 DB 재기동 | 현재 V18, 신규 적용 0건 |
| 금지 얼굴 컬럼 메타데이터 검사 | 0건 |
| 전체 clean build | 성공 |
| 테스트 | 7 suites, 61 tests, failure/error/skip 0 |

`V6`, `V11` 실행 중 MySQL 8.0이 과거 `BINARY expr` 문법에 대한
deprecated warning을 출력했으나, V12가 해당 결제 상태 비교를
`CAST`로 교체하며 Migration 실패는 없었다.

## 실행 명령

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\backend\scripts\db-start.ps1

Set-Location .\backend
$env:SPRING_PROFILES_ACTIVE = 'local'
.\gradlew.bat bootRun --args='--spring.main.web-application-type=none'
.\gradlew.bat bootRun --args='--spring.main.web-application-type=none'
.\gradlew.bat clean build --rerun-tasks
```

실제 실행에서는 `.env`를 현재 프로세스 환경 변수로 읽었으며 Secret은
명령 출력, 보고서 또는 Git 추적 파일에 기록하지 않았다.

## 데이터 이전 경계

공식 V13은 기존 UUID 사용자 PK를 실제 9자리 학번으로 안전하게
추론할 수 없으므로 `user_account`가 비어 있을 때만 적용된다. 이번에는
기존 `billisan` DB를 자동 변환하지 않고 새 `billisan_p0_p1`에
적용했다.

기존 데이터가 필요한 환경은 다음 매핑을 팀 승인 후 별도 데이터
Migration으로 수행해야 한다.

```text
기존 user_id UUID -> 실제 9자리 user_id
기존 사용자 -> 새 불투명 user_ref UUID
관련 rental/settlement 소유자 FK
```

운영 또는 공유 DB에 clean-install SQL을 직접 실행하거나 V13의 빈 DB
가드를 우회해서는 안 된다.

## 변경 범위

- 공식 Flyway V1~V18
- Flyway 실행 경로 밖의 clean-install 참조 스키마
- 이전 V1~V4의 legacy 보관
- MySQL Compose·local Profile·환경 변수 예제의 UTC/DB 기준
- 새 DB를 기존 volume에 추가하는 PowerShell 시작 스크립트
- FACE_PROFILE 동기화·멱등성·슬롯·Snapshot 서비스의 스키마 정렬
- MySQL Testcontainers Fixture와 메타데이터 검증

Commit, Push, MR은 수행하지 않았다.

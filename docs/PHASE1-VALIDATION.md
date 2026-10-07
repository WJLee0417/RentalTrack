# Phase 1 Validation

## 최종 판정

`PASSED`

검증 기준 커밋:

```text
9dd3ba50ad4279166101c39ae782a817c26a8f30
test: accept mysql check constraint exception mapping
```

GitHub Actions `Backend Validation`의 네 번째 실행에서 MySQL 8.4 + Java 21 환경의 전체 검증이 통과했습니다.

## 검증 범위

Phase 1에서 확정한 9개 엔터티와 Flyway V1~V5가 같은 물리 스키마를 가리키는지 검증합니다.

- Flyway V1~V5 순차 적용
- Spring Boot 시작 시 Hibernate `ddl-auto=validate`
- Phase 1 핵심 테이블 9개 존재
- ConditionRecord의 `rental_contract_id + record_type` 중복 방지
- DamageRecord의 기존 손상 중복 매칭 방지
- RentalContract당 ReturnInspection 한 건 제한
- RentalProduct / RentalContract 상태 CHECK 제약

## 자동 검증

`Phase1SchemaIntegrationTest`는 MySQL 8.4를 대상으로 5개 시나리오를 검증합니다.

```text
Flyway V1~V5 / 핵심 테이블
ConditionRecord 중복 방지
DamageRecord 기존 손상 중복 매칭 방지
ReturnInspection 계약당 1건
상품·계약 상태 CHECK
```

## 검증 중 발견하고 수정한 문제

### 1. MySQL CHECK 제약과 AUTO_INCREMENT PK

첫 실행에서 V4의 self-match CHECK가 MySQL 8.4에서 허용되지 않아 Flyway가 중단되었습니다.

수정:
- DB CHECK에서 `damage_record.id` 직접 참조 제거
- `DamageRecord.markExisting()` 도메인 검증으로 self-match 방지 이동

### 2. 테스트 fixture 길이

다음 실행에서는 Flyway와 Spring context가 정상 기동된 뒤 테스트 fixture의 location/category code가 `VARCHAR(30)`을 초과했습니다.

수정:
- 테스트용 식별자를 16자리 compact UUID로 제한
- 스키마 제약을 느슨하게 변경하지 않음

### 3. MySQL CHECK 예외 매핑

마지막 남은 테스트는 CHECK 제약 자체는 정상 동작했지만 MySQL error 3819가 Spring에서 `DataIntegrityViolationException`이 아닌 상위 `DataAccessException` 계열로 번역되어 실패했습니다.

수정:
- 테스트 목적을 "CHECK 위반이 DB 접근 예외로 거부되는가"로 맞춰 `DataAccessException`을 검증

## GitHub Actions

`.github/workflows/backend-validation.yml`

- MySQL 8.4 service
- Java 21
- Gradle
- `./gradlew clean test --no-daemon --stacktrace`
- 테스트 보고서 artifact 업로드

작업 브랜치 push와 Backend 변경 Pull Request에서 자동 실행합니다.

## GitLab CI

루트 `.gitlab-ci.yml`이 `backend/ci.yml`을 include합니다.

GitLab CI 구성:

- `eclipse-temurin:21-jdk-jammy`
- MySQL 8.4 service
- MySQL readiness 확인
- 동일한 Gradle test 실행
- JUnit 및 HTML test report artifact

단, GitLab 프로젝트의 remote/mirror 연결과 Runner 실행은 저장소의 CI 파일과 별도 설정입니다. 현재 작업 환경에서는 GitLab 계정/프로젝트를 직접 조작하는 연결 수단이 없어 저장소 측 CI 구성까지만 완료했습니다.

# Phase 1 Validation

## 검증 목표

Phase 1에서 확정한 9개 엔터티와 Flyway V1~V5가 같은 물리 스키마를 가리키는지 검증합니다.

검증 대상:
- Flyway V1~V5 순차 적용
- Spring Boot 시작 시 Hibernate ddl-auto=validate
- Phase 1 핵심 테이블 9개 존재
- ConditionRecord의 rental_contract_id + record_type 중복 방지
- DamageRecord의 기존 손상 중복 매칭 방지
- RentalContract당 ReturnInspection 한 건 제한
- RentalProduct / RentalContract 상태 CHECK 제약

## 검증 중 발견한 문제

### 1. MySQL CHECK 제약과 AUTO_INCREMENT PK

첫 실행에서 V4의 self-match CHECK가 MySQL 8.4에서 허용되지 않아 Flyway가 중단되었습니다.

해결:
- DB CHECK에서 damage_record.id 직접 참조 제거
- DamageRecord.markExisting() 도메인 검증으로 self-match 방지 이동

### 2. 테스트 fixture 길이

두 번째 실행에서는 Flyway와 Spring context가 정상 기동된 뒤 테스트 fixture의 location/category code가 VARCHAR(30)을 초과해 실패했습니다.

해결:
- 테스트용 식별자를 16자리 compact UUID로 제한
- 스키마 제약을 느슨하게 바꾸지 않고 fixture를 실제 컬럼 계약에 맞춤

## 자동 검증

Phase1SchemaIntegrationTest는 MySQL 8.4를 대상으로 실행합니다.

## GitHub Actions

.github/workflows/phase1-validation.yml 에서 작업 브랜치 push 및 Pull Request 시 동일 검증을 실행합니다.

## GitLab CI

루트 .gitlab-ci.yml 이 backend/ci.yml 을 include합니다.

GitLab Runner가 이 저장소를 실행하는 환경에서는 MySQL 8.4 서비스와 Java 21을 사용해 ./gradlew clean test 를 수행합니다.

GitLab 프로젝트의 mirror/remote 연결 자체는 저장소 안의 CI 설정과 별도입니다.

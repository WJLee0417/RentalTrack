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

## 1차 CI 결과와 수정

첫 GitHub Actions 실행에서 Java 컴파일과 테스트 컴파일은 통과했지만 Flyway 적용 단계에서 실패했습니다.

원인은 MySQL CHECK 제약에 AUTO_INCREMENT PK인 damage_record.id를 직접 참조한 self-match 제약이었습니다. MySQL에서는 해당 형태를 허용하지 않으므로 DB CHECK를 제거하고 DamageRecord.markExisting()의 도메인 검증으로 옮겼습니다.

## 자동 검증

Phase1SchemaIntegrationTest는 MySQL 8.4를 대상으로 실행합니다.

## GitHub Actions

.github/workflows/phase1-validation.yml 에서 작업 브랜치 push 및 Pull Request 시 동일 검증을 실행합니다.

## GitLab CI

루트 .gitlab-ci.yml 이 backend/ci.yml 을 include합니다.

GitLab Runner가 이 저장소를 실행하는 환경에서는 MySQL 8.4 서비스와 Java 21을 사용해 ./gradlew clean test 를 수행합니다.

GitLab 프로젝트의 mirror/remote 연결 자체는 저장소 안의 CI 설정과 별도입니다.

# RentalTrack Backend

Spring Boot 기반 RentalTrack API 서버입니다.

## Stack

`Java 21` `Spring Boot` `Spring Data JPA` `Spring Security` `Flyway` `MySQL 8.4`

## Validation

Backend 변경은 GitHub Actions `Backend Validation`에서 MySQL 8.4와 Java 21 환경으로 자동 검증합니다.

```bash
./gradlew clean test --no-daemon --stacktrace
```

자동 검증 범위에는 다음 항목이 포함됩니다.

- Flyway migration / JPA `ddl-auto=validate`
- API 입력값 검증
- Rental 상태 전이
- 동일 상품 동시 대여 요청
- USER / OPERATOR 접근 정책
- 이미지 저장소 추상화와 업로드 처리
- JWT 발급과 인증 서비스
- 운영자 예측·추천 조회 및 의사결정

동일한 Backend 검증 흐름을 위한 GitLab CI 설정도 저장소에 함께 제공합니다.

```text
.gitlab-ci.yml
└─ backend/ci.yml
```

GitLab Runner에서의 실행 여부는 GitHub Actions 검증 결과와 별도로 관리합니다.

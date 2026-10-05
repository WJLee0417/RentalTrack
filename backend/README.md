# RentalTrack Backend

Spring Boot 기반 RentalTrack API 서버입니다.

## Validation

Backend 변경은 GitHub Actions `Backend Validation`과 GitLab CI `backend-validation`에서 MySQL 8.4와 Java 21 환경으로 검증합니다.

```bash
./gradlew clean test --no-daemon --stacktrace
```

현재 자동 검증 범위에는 Flyway/JPA schema validation, API 입력값 검증, Rental 상태 전이, 동시 대여 요청, 사용자/운영자 접근 정책이 포함됩니다.

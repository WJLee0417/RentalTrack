# RentalTrack Backend

Java 21 / MySQL 8.4. 저장소 루트 [README](../README.md)의 DB 준비 절차를 따른다.

```powershell
.\gradlew.bat test bootJar --no-daemon
.\gradlew.bat bootRun
```

- JPA `ddl-auto=validate`, Flyway V1~V11 순서 적용.
- `core/`의 WorkflowService가 계약을 먼저 잠그고 상태·기록·검수 변경을 직렬화. 상품 신청은 개별 상품 행을 잠근 뒤 활성 계약 유무 검사.
- 변경 트랜잭션은 READ_COMMITTED. 계약 잠금 후 조회하는 자료가 다른 트랜잭션 이전의 스냅샷에 머무르지 않도록 설정.
- 엔터티를 직접 JSON 응답으로 노출하지 않고 조회용 필드를 명시적으로 구성.
- 파일 저장 후 DB 트랜잭션이 롤백되면 신규 파일 삭제. 차수 복사 시 파일 키는 공유하고 사진·관찰·연결 레코드는 새 식별자로 복사.
- 인증 비밀번호는 ERD 확장 테이블 `member_credential`에 BCrypt 해시로 저장. JWT issuer는 `rentaltrack-erd-v2`로 구분해 기존 회원 ID 공간의 토큰 재사용 차단.

테스트 DB: `rentaltrack_test`, `rentaltrack_upgrade_test`. 후자는 V10 데이터가 있는 상태에서 V11 적용을 검증하는 전용 DB이며 테스트가 초기화한다.

[API](../docs/API.md) · [검증](../docs/VALIDATION.md) · [이전 안내](../docs/MIGRATION.md)

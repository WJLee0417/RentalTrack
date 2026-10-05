# STEP-07 Device Contract Mock 보고서

## 판정

`PASSED`

실제 Pi와 운영 Controller 없이도 승인된 Pi → Spring Edge Endpoint를
사용하는 10개 HTTP Mock 요청과 JSON fixture를 재현할 수 있다. 독립
JUnit 계약 테스트 15개가 모두 통과했으며 식별자, `status`, 금지
생체정보 필드를 자동 검증한다.

현재 저장소에는 운영 Device Gateway 구현이 없으므로 Controller·운영
DTO를 임의 생성하지 않았다. Payload가 완전히 확정되지 않은 부분은
구현 초안과 미확정 계약으로 명시했다.

## 선행 조건

| 항목 | 결과 |
| --- | --- |
| STEP-01 | `PASSED` |
| STEP-02 | `PASSED` |
| STEP-03 | `PASSED` |
| STEP-04 | `PASSED` |
| STEP-05 | `PASSED` |
| STEP-06 | `PASSED` |
| 현재 브랜치 | `S15P11A201-55-be-db` |
| 작업 시작 시 HEAD | `eb9b5b1cba3e4ad6158b201a845fc44045971622` |
| 시작 시 작업 트리 | clean |

사용자 지시에 따라 `origin/subPjt_1` 기술조사 문서는 조사 근거로
사용하지 않았다.

## 저장소 구현 감사

현재 추적 트리 전체에서 다음을 확인했다.

| 항목 | 결과 | 근거 |
| --- | --- | --- |
| 운영 Controller | 없음 | main Java는 `BillisanApplication.java` 한 개 |
| Device Gateway | 없음 | 패키지·클래스·키워드 검색 무결과 |
| 요청·응답 DTO | 없음 | Java/Kotlin 소스와 dto 경로 검색 무결과 |
| OpenAPI·Postman·HTTP | 기존 파일 없음 | 전체 파일명 검색 |
| Jackson 네이밍 설정 | 없음 | `application*.yaml` 및 소스 검색 |
| Spring Web | 없음 | `build.gradle` 및 `testRuntimeClasspath` |
| Bean Validation | 없음 | 의존성·annotation 검색 |
| Jackson | `3.1.4` 사용 가능 | Flyway Core 전이 의존성 |
| 테스트 방식 | JUnit Jupiter 6 + Testcontainers | `build.gradle`, 기존 통합 테스트 |

따라서 운영 API를 열지 않고 JUnit + JSON fixture를 계약 검증 방식으로
선택했다. Mock 테스트는 Spring Context나 DB를 시작하지 않는 독립
테스트라 실제 Pi 없이 실행된다.

## 실제 명세 비교

Notion
[10. Billisan Kiosk·App 및 공통 도메인 API 명세 v1.0](https://app.notion.com/p/3a6443b01b1381d7982ad0f445732be3)의
Pi → Spring 절에서 Base URL과 Endpoint를 확인했다.

| API ID | Method | Endpoint | 상태 |
| --- | --- | --- | --- |
| `EDGE-AUTH-001` | `POST` | `/api/v1/edge/kiosk-authentications` | `CONFIRMED_API` |
| `EDGE-INSPECT-001` | `POST` | `/api/v1/edge/return-attempts/{returnAttemptId}/inspection-results` | `CONFIRMED_API` |
| `EDGE-DEVICE-001` | `POST` | `/api/v1/edge/device-operations/{commandId}/events` | `CONFIRMED_API` |
| `EDGE-HEALTH-001` | `POST` | `/api/v1/edge/stations/{stationId}/heartbeats` | `CONFIRMED_API` |
| `EDGE-SNAPSHOT-001` | `POST` | `/api/v1/edge/stations/{stationId}/boot-snapshots` | `CONFIRMED_API` |

잠금 명령 결과와 슬롯 센서 이벤트는 별도 Endpoint를 추정하지 않고
`EDGE-DEVICE-001`의 `eventType`으로 구분했다.

## 생성한 Mock

| 논리 계약 | 시나리오 | fixture |
| --- | --- | --- |
| AI 얼굴 인증 | 성공 | `face-auth-success.json` |
| AI 얼굴 인증 | 실패 | `face-auth-failure.json` |
| AI 파손 판정 | `DAMAGED`, confidence·modelVersion | `damage-result.json` |
| 잠금 결과 | 성공 | `lock-success.json` |
| 잠금 결과 | 실패, `LOCK_TIMEOUT` | `lock-failure.json` |
| 슬롯 센서 | 점유 | `slot-sensor-occupied.json` |
| 슬롯 센서 | 비점유 | `slot-sensor-empty.json` |
| Heartbeat | 정상 최소 Body | `heartbeat-normal.json` |
| 부팅 Snapshot | 서버 기준과 일치 | `boot-snapshot-match.json` |
| 부팅 Snapshot | 서버 기준과 1개 슬롯 불일치 | `boot-snapshot-mismatch.json` |

IntelliJ HTTP Client 요청은
`backend/src/test/resources/device-contract/device-gateway.http`에
작성했다. 실제 토큰·운영 주소·개인정보는 없다.

## 식별자와 상태 검증

| 항목 | 적용 |
| --- | --- |
| 추적 ID | `X-Request-Id`; 업무 멱등 키와 분리 |
| API 요청 ID | 승인 DTO의 `authRequestId`, `inspectionRequestId` |
| 장치 명령 ID | `commandId`; 장치 이벤트 URI에만 사용 |
| Pi 이벤트 ID | `eventId`; 이벤트 Body와 `Idempotency-Key` |
| 반납 시도 ID | `returnAttemptId`; 파손 URI 및 관련 이벤트 상관관계 |
| 대여소·슬롯 | `stationId`, `slotId`; 물리 리소스 식별 |
| Snapshot | `bootId`; 부팅 인스턴스와 재전송 식별 |
| ID 형식 | 소문자 하이픈 UUID |
| 반납 상태 필드 | `status` |
| 반납 상태 값 | `PROCESSING`, `PHYSICAL_DONE`, `COMPLETED`, `RECOVERY_REQUIRED`, `FAILED` |
| 금지 상태 이름 | `process_status`, `processStatus` |

API v1.0의 파손 결과 예제에 남은 `processStatus`는 상위 Backend 역할
명세와 이번 STEP-07 규칙에 충돌한다. Fixture는 `status`로 교정했고,
테스트는 구 이름이 다시 들어오면 실패한다.

## DTO·Validation 변경

- 운영 DTO: 변경 없음
- 운영 Controller/Gateway: 변경 없음
- Bean Validation: 저장소에 없어 의존성 추가 없음
- 테스트 전용 계약 검증:
  `DeviceContractFixtureTest`

검증 테스트는 누락된 `authRequestId`·`eventId`, 잘못된 UUID, 범위를
벗어난 메타데이터, 금지 이미지·embedding 필드 및 구 상태 이름을
거절한다. 얼굴 인증 실패는 `userId = null`이어야 한다.

## 실행 결과

### 의존성 조사

```powershell
.\gradlew.bat dependencies --configuration testRuntimeClasspath
```

결과: `BUILD SUCCESSFUL`. Jackson Databind `3.1.4`, JUnit Jupiter
`6.0.3`, Spring Test `7.0.8`을 확인했다. Spring Web과 Bean Validation은
없다.

### 계약 테스트

```powershell
.\gradlew.bat --no-daemon test `
  --tests '*DeviceContractFixtureTest' `
  --rerun-tasks
```

결과:

| 항목 | 값 |
| --- | --- |
| 테스트 | 15 |
| 실패 | 0 |
| 오류 | 0 |
| 건너뜀 | 0 |
| Gradle | `BUILD SUCCESSFUL` |

첫 실행에서 Jackson 3 `deepCopy()`의 정적 반환 타입으로 인한 테스트
컴파일 오류 6건을 확인했고, 테스트 객체 변환을 명시해 수정했다. 같은
명령 재실행은 성공했다.

### 전체 회귀 빌드

```powershell
.\gradlew.bat --no-daemon build --rerun-tasks
```

결과: `BUILD SUCCESSFUL`.

| Test suite | 테스트 | 실패 | 오류 | 건너뜀 |
| --- | ---: | ---: | ---: | ---: |
| `BillisanApplicationTests` | 5 | 0 | 0 | 0 |
| `DeviceContractFixtureTest` | 15 | 0 | 0 | 0 |
| 합계 | 20 | 0 | 0 | 0 |

기존 Flyway·MySQL Testcontainers 통합 테스트도 함께 통과했다.

운영 Controller와 Spring Web이 없으므로 HTTP 요청을 실제 서버로
전송하지 않았다. 이 실패 가능성을 숨기기 위해 임시 Controller를
추가하지 않았고, HTTP 요청과 독립 계약 테스트로 검증 범위를 분리했다.

## 생체정보 금지 검증

모든 fixture에서 다음 필드를 재귀적으로 거절한다.

```text
image
imageBase64
imagePath
imageUrl
faceImage
embedding
faceEmbedding
```

얼굴 성공 fixture에는 최종 `userId`, `similarity`, `modelVersion`만
있고, 실패 fixture에는 `userId = null`이다. 파손 fixture에는
결과·confidence·modelVersion·errorCode 메타데이터만 있다.

## 미확정 계약과 충돌

| 항목 | 상태 | 다음 결정 |
| --- | --- | --- |
| `processStatus` 잔존 | 문서 충돌 | API v1.0 원문을 `status`로 정정 |
| `requestId` 물리 이름 | 공통 이름과 API의 구체 이름 충돌 | `requestId` 단일 이름 또는 현재 두 이름 승인 |
| Heartbeat 상세 Body·응답 | Endpoint만 확정 | 필드·ACK·보존·오프라인 전이 승인 |
| Snapshot 정상 응답 Enum | 불일치 응답만 예시 존재 | 정상 reconciliation 값 승인 |
| `LockStatus.UNKNOWN` | API 허용, MySQL V1 CHECK 미허용 | API 제거 또는 후속 Migration 결정 |
| 장치 토큰 | `PROVISIONAL_API` | 발급·회전·폐기·권한 영역 확정 |
| Controller·Gateway 패키지 | 저장소 구현 없음 | Backend 2·3 패키지와 책임 승인 |
| DTO Validation | 구현 없음 | Spring Web/Validation 도입 단계에서 적용 |

## 변경 파일

- `backend/src/test/java/com/ssafy/billisan/devicecontract/DeviceContractFixtureTest.java`
- `backend/src/test/resources/device-contract/device-gateway.http`
- `backend/src/test/resources/device-contract/v1/*.json`
- `docs/backend2-setup/DEVICE-CONTRACT-DRAFT.md`
- `docs/backend2-setup/reports/STEP-07-DEVICE-CONTRACT-MOCK.md`
- `docs/backend2-setup/PROGRESS.md`

애플리케이션 코드, 설정, DB Migration 및 의존성은 수정하지 않았다.
Commit, Push, MR도 수행하지 않았다.

## 추천 Commit 메시지

```text
S15P11A201-55 test: Pi-Spring 장치 계약 Mock 검증 추가
```

## 다음 실행 프롬프트

`STEP-08`

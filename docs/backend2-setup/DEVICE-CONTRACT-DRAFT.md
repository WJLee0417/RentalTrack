# Billisan Pi–Spring Device Contract 구현 초안

## 문서 상태

| 항목 | 상태 |
| --- | --- |
| Pi → Spring Base URL | `CONFIRMED`: `/api/v1/edge` |
| 아래 5개 Endpoint | `CONFIRMED_API` |
| 저장소 Controller·HTTP DTO | 아직 없음 |
| Snapshot application core | `IMPLEMENTED`: service·policy·MySQL state |
| 이 문서와 fixture | `ERD_V2_1_ALIGNED` |
| 운영 구현 승인 | Backend 2·3 및 Embedded 합의 필요 |

이 문서는 운영 Controller를 대신하지 않는다. STEP-10에서 부팅 Snapshot의
application service·정책·MySQL 상태 전이는 구현했지만 Controller,
HTTP DTO, Spring Web 및 Bean Validation은 아직 없다. 승인된 Endpoint와
상위 식별자·상태 규칙은 실행 가능한 Mock fixture로 고정한다.

근거 우선순위는 다음과 같다.

1. Notion
   [08. Billisan ERD 및 데이터 아키텍처 문서 v2.1](https://app.notion.com/p/2db443b01b138220904d01f4677e132b)
2. 현재 STEP-07 입력의 식별자와 `status` 규칙
3. Notion
   [10. Billisan Kiosk·App 및 공통 도메인 API 명세 v1.0](https://app.notion.com/p/3a6443b01b1381d7982ad0f445732be3)
4. Notion
   [05. 빌리산 백엔드 역할 명세서 v1.0](https://app.notion.com/p/955443b01b1383ee932e81e2d9cf36df)
5. 현재 MySQL Flyway V1~V4 물리 스키마
6. 저장소의 실제 Controller·DTO 구현. 현재 검색 범위에서는 없음

## 시스템 경계

- Kiosk FE는 Pi Local API를 호출한다.
- Pi만 Spring Edge API를 호출한다.
- Jetson Orin은 Pi에 얼굴 인증·파손 판정 메타데이터만 반환한다.
- Orin은 Spring을 직접 호출하지 않는다.
- Spring Payload에는 얼굴 원본, Base64, 이미지 경로·URL 및 embedding을
  넣지 않는다.
- Spring의 업무 DB는 MySQL 한 개이며 PostgreSQL 두 번째 DataSource를
  추가하지 않는다.

## 확인된 Pi → Spring Endpoint

| 논리 계약 | Method | Endpoint | Mock |
| --- | --- | --- | --- |
| AI 얼굴 인증 결과 | `POST` | `/api/v1/edge/kiosk-authentications` | 성공·실패 |
| AI 파손 판정 결과 | `POST` | `/api/v1/edge/return-attempts/{returnAttemptId}/inspection-results` | 파손 결과 |
| 잠금 명령 결과 | `POST` | `/api/v1/edge/device-operations/{commandId}/events` | 성공·실패 |
| 슬롯 센서 이벤트 | `POST` | `/api/v1/edge/device-operations/{commandId}/events` | 점유·비점유 |
| Heartbeat | `POST` | `/api/v1/edge/stations/{stationId}/heartbeats` | 정상 |
| Pi 부팅 Snapshot | `POST` | `/api/v1/edge/stations/{stationId}/boot-snapshots` | 일치·점유/잠금 불일치·unknown·누락 |

잠금과 슬롯 센서는 별도 운영 Endpoint를 추정하지 않고 승인된
`EDGE-DEVICE-001` 이벤트 Endpoint를 함께 사용한다.

## 공통 HTTP 규칙

```http
Accept: application/json
Content-Type: application/json
Authorization: Bearer <device-token>
X-Device-Id: <deviceId>
X-Request-Id: <trace-id>
Idempotency-Key: <logical-idempotency-key>
```

- `X-Request-Id`는 호출 추적 ID이며 `Idempotency-Key`와 같게 취급하지
  않는다.
- 실제 장치 토큰은 예제나 Git 추적 파일에 넣지 않는다.
- 재전송은 같은 논리 작업에 같은 `Idempotency-Key`를 사용한다.

## 식별자 물리 규칙

모든 예제 ID는 소문자 하이픈 UUID 문자열이다. MySQL의
`request_id`, `command_id`, `event_id`는 현재 `VARCHAR(100)`이지만 API
v1.0의 공통 ID 형식이 UUID이므로 Mock 검증은 정규형 UUID를 요구한다.

| 식별자 | 의미 | 전달 위치 | 멱등성 |
| --- | --- | --- | --- |
| `authRequestId` | 얼굴 인증 API 요청 ID. 공통 `requestId` 의미의 현재 승인된 구체 이름 | 얼굴 인증 Body | 해당 얼굴 인증 요청의 `Idempotency-Key` |
| `inspectionRequestId` | 파손 판정 API 요청 ID. 공통 `requestId` 의미의 현재 승인된 구체 이름 | 파손 결과 Body | 해당 파손 결과의 `Idempotency-Key` |
| `returnAttemptId` | 반납 시도 리소스 ID | 파손 결과 URI, 관련 장치 이벤트 Body | 요청 멱등 키로 사용하지 않음 |
| `commandId` | Spring이 발행한 장치 명령 ID | 장치 이벤트 URI | Pi가 새로 만들거나 `eventId` 대신 사용하지 않음 |
| `eventId` | Pi가 발행한 장치 결과·Heartbeat 이벤트 ID | 이벤트 Body | 장치 이벤트의 `Idempotency-Key`, MySQL UNIQUE |
| `stationId` | 대여소 ID | 인증 Body 또는 Station URI | 다른 ID를 대체하지 않음 |
| `slotId` | 물리 슬롯 ID | 장치 이벤트·Snapshot Body | 대여·반납·명령 ID와 구분 |
| `bootId` | 한 Pi 부팅 인스턴스 식별 | 장치 Terminal 이벤트·Snapshot Body | `DEVICE_OPERATION.issued_boot_id` 일치 검증과 Snapshot 세대 구분 |

`authRequestId`와 `inspectionRequestId`를 모두 `requestId`로 물리 통합할지는
팀 결정 사항이다. 현재 Mock은 CONFIRMED_API DTO의 이름을 보존하되 두
필드를 같은 의미 범주의 API 요청 ID로만 사용한다.

## 상태 규칙

반납 처리 상태의 공식 필드는 오직 `status`다.

```text
PROCESSING
PHYSICAL_DONE
COMPLETED
RECOVERY_REQUIRED
FAILED
```

`process_status`와 `processStatus`는 Payload, DTO, DB 컬럼의 새 공식
이름으로 사용할 수 없다. 현재 API v1.0의 `EDGE-INSPECT-001` 예시에
남은 `processStatus`는 상위 규칙과 충돌하므로 Mock에서는
`status`로 교정했다.

`occupancyStatus`, `lockStatus`, `reconciliationStatus`처럼 다른 상태
축을 명시하는 이름은 반납 `status`와 별도다.

## Mock fixture

HTTP 요청 진입점:

```text
backend/src/test/resources/device-contract/device-gateway.http
```

JSON fixture:

| 파일 | 재현 시나리오 |
| --- | --- |
| `face-auth-success.json` | 얼굴 인증 성공 메타데이터 |
| `face-auth-failure.json` | 얼굴 인증 실패, `userId = null` |
| `damage-result.json` | Jetson 파손 판정 메타데이터 |
| `slot-sensor-occupied.json` | 우산 삽입·슬롯 점유 |
| `slot-sensor-empty.json` | 우산 인출·슬롯 비점유 |
| `lock-success.json` | 잠금 성공 |
| `lock-failure.json` | 잠금 실패와 오류 코드 |
| `heartbeat-normal.json` | 정상 Heartbeat 최소 Body |
| `boot-snapshot-match.json` | 서버 Mock 기준과 일치 |
| `boot-snapshot-mismatch.json` | 서버 Mock 기준과 1개 슬롯 점유 불일치 |
| `boot-snapshot-lock-mismatch.json` | 1개 슬롯 잠금 불일치 |
| `boot-snapshot-unknown-slot.json` | 서버에 없는 slotId 포함 |
| `boot-snapshot-partial.json` | 서버 슬롯 중 일부 누락 |
| `boot-snapshot-server-baseline.json` | Snapshot 테스트용 서버 기준 상태 |

장치 결과 fixture 4개는 ERD v2.1의 Terminal 증거 계약을 따른다. 각각
`commandId`, `eventId`, `bootId`, `evidenceSchemaVersion`과 최종
`physicalState`를 포함하며 `eventType`은 `OPERATION_COMPLETED` 또는
`OPERATION_FAILED`만 사용한다. ACK는 물리 성공으로 간주하지 않는다.

Snapshot 일치 결과의 운영 응답 Enum은 승인 자료에서 확인되지 않아
HTTP 계약으로 확정하지 않았다. 내부 application result는 안전한 분기를
위해 `MATCHED`와 `RECOVERY_REQUIRED`, 처리 결과는 `APPLIED`와
`REPLAYED`를 사용한다. 이 값의 HTTP 응답 노출 방식은 Embedded·Backend
합의 전까지 미확정이다.

Heartbeat Endpoint는 승인됐지만 상세 Body가 API v1.0에 없다. 따라서
fixture는 중복 차단용 `eventId`, 장치 식별용 `deviceId`, 관측 시각
`measuredAt`만 포함한다. Firmware, uptime, 온도 등은 승인 전 추가하지
않는다.

## 자동 검증

```powershell
Set-Location backend
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
.\gradlew.bat --no-daemon test --tests '*DeviceContractFixtureTest' --rerun-tasks
```

검증 범위:

- JSON request fixture 13개의 구문과 필수 필드
- UUID 정규형과 ISO-8601 Offset DateTime
- 얼굴 성공·실패, 파손 결과, 잠금·센서 이벤트, Heartbeat, Snapshot
- Snapshot 일치·점유/잠금 불일치·unknown slot·일부 슬롯 누락
- `status` 허용 값
- 누락·잘못된 ID 거절
- 얼굴 이미지·embedding 및 `processStatus`·`process_status` 거절
- 추적용 `X-Request-Id`와 업무 멱등 키 분리
- CONFIRMED Edge Endpoint 경로

## 구현 전 해결할 충돌

| 항목 | 현재 근거 | 이 초안의 처리 | 운영 구현 전 결정 |
| --- | --- | --- | --- |
| 파손 결과 상태 이름 | API v1.0 예제 `processStatus`; 상위 역할·STEP-07은 금지 | `status`로 교정 | API 원문도 `status`로 정정 |
| 요청 ID 물리 이름 | 공통 규칙 `requestId`; API v1.0은 `authRequestId`, `inspectionRequestId` | 승인 DTO 이름 보존, 의미만 request ID로 통일 | 단일 `requestId` 이름으로 바꿀지 결정 |
| Heartbeat Body | Endpoint만 CONFIRMED | 최소 3필드 초안 | DTO와 응답·보존 정책 승인 |
| Snapshot 정상 응답 | 불일치 `RECOVERY_REQUIRED` 예시만 있음 | 내부 결과에 `MATCHED` 사용, HTTP DTO는 만들지 않음 | HTTP reconciliation Enum·status code 승인 |
| 잠금 상태 `UNKNOWN` | ERD v2.1 허용 | MySQL V4 CHECK와 계약 검증에 반영 | 해결됨 |
| 장치 인증 | Bearer 장치 토큰 + `X-Device-Id`는 PROVISIONAL | 비민감 Mock token만 사용 | 토큰 발급·회전·권한 방식 승인 |

## 운영 구현 금지선

- 팀 합의 전 새 운영 Controller·DTO를 이 초안만으로 만들지 않는다.
- 얼굴 이미지, embedding, 이미지 참조를 Payload나 로그에 넣지 않는다.
- `commandId`, `eventId`, 요청 ID를 같은 값이나 같은 책임으로 사용하지
  않는다.
- Snapshot 불일치를 자동 정상화하지 않는다.
- AI 결과만으로 파손·폐기·과금을 확정하지 않는다.

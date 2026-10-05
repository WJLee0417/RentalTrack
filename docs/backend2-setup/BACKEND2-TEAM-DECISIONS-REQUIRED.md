# Billisan Backend 2 팀 결정 필요 목록

## 사용 방법

아래 항목은 로컬 환경 실패가 아니라 운영 구현 또는 팀 간 계약이 없어
검증을 완료할 수 없는 경계다. 각 항목에 Owner, 결정 기한, 근거 문서와
Jira 이슈를 지정한 뒤 구현을 시작한다.

## P0 — 운영 구현 전 필수

| ID | 결정 | 현재 안전 기준 | 필요한 산출물 |
| --- | --- | --- | --- |
| TD-01 | 장치 API Controller·DTO·HTTP 응답 | 승인 Edge 경로와 fixture만 유지, 임의 Controller 생성 금지 | OpenAPI, DTO, Validation, HTTP 200/409/4xx 매핑 |
| TD-02 | 장치 인증·프로비저닝 | mock token만 사용, 운영 Token 저장 금지 | deviceId↔stationId 소유권, 인증 방식, key rotation |
| TD-03 | 외부 장치 명령 exactly-once | DB `command_id` 멱등성까지만 보장 | 실제 dispatch/ACK 재시도·timeout·outbox 경계 |
| TD-04 | Toss 결제 exactly-once | DB 주문·승인 ID UNIQUE까지만 보장 | 주문 생성 Transaction, sandbox 결과, 재시도·취소 정책 |
| TD-05 | FACE embedding 규격 | 차원 미지정 `VECTOR`, 인덱스 없음 | 차원, 거리 함수, 정규화, index 유형·튜닝 기준 |
| TD-06 | Orin DB 스키마 배포 주체 | Spring 직접 연결 금지, 로컬 스크립트 적용 | 운영 Owner, 권한, 배포·백업·Rollback 절차 |

## P1 — 장치·복구 계약

| ID | 결정 | 미확정 내용 | 결정하지 않을 때의 위험 |
| --- | --- | --- | --- |
| TD-07 | Heartbeat DTO | firmware/app version, uptime, 센서 health, timeout 기준 | 장치 전체 장애 탐지 불일치 |
| TD-08 | Snapshot 완전성 | full-only 여부, slot count/schema version, partial payload 처리 | 정상 슬롯까지 과도하게 격리하거나 누락을 정상으로 오인 |
| TD-09 | `UNKNOWN` 잠금 상태 | API 값 허용 여부와 MySQL CHECK 변경 여부 | 계약과 저장 상태 불일치 |
| TD-10 | 관리자 복구 해제 | station/slot/returnAttempt 해제 API, 감사 주체, 권한 | `ADMIN_REVIEW`·`RECOVERY_REQUIRED` 장기 고착 |
| TD-11 | 장치 전체 장애 임계값 | heartbeat timeout, sensor health 단계, station 상태 전이 | 특정 슬롯 장애와 전체 장애 혼동 |
| TD-12 | 이벤트 payload fingerprint | eventId 재사용 시 비교할 센서·잠금 원본 필드 | 동일 eventId의 변형 payload를 완전히 식별하지 못함 |

## P1 — API·업무 정책

| ID | 결정 | 현재 구현 | 필요한 결정 |
| --- | --- | --- | --- |
| TD-13 | 멱등 재전송 HTTP 응답 | service `APPLIED`/`REPLAYED`, conflict exception | replay 200/201, conflict 409, 응답 body |
| TD-14 | 슬롯 없음 응답 | `SlotUnavailableException` | HTTP status, error code, client retry/backoff |
| TD-15 | `SKIP LOCKED` 재시도 | 즉시 사용 가능 슬롯 없음으로 종료 | 짧은 재시도 허용 여부와 최대 횟수 |
| TD-16 | 관리자·재고 API | Repository projection만 존재 | Endpoint, 권한, paging, 정렬, 날짜 범위 |
| TD-17 | 반납 재시도 상한 | 같은 rentalId에 여러 실패 시도 허용 | 최대 횟수, 완료 뒤 추가 시도 차단 주체 |
| TD-18 | 장치 이벤트 모델 | `device_operation.event_id`에 저장 | 별도 이벤트 이력 필요 여부와 보존 기간 |

## P1 — Cross-DB 얼굴 프로필

| ID | 결정 | 현재 규칙 | 필요한 결정 |
| --- | --- | --- | --- |
| TD-19 | 사용자 생명주기 동기화 | MySQL ID ↔ PostgreSQL UUID 논리 참조, FK 없음 | 탈퇴·비활성화 이벤트와 재처리 |
| TD-20 | 모델·템플릿 버전 | nullable model version, template default 1 | 필수 여부, 호환성, 재등록 정책 |
| TD-21 | 얼굴 원본 보유 정책 | MySQL·PostgreSQL·Payload에 저장 금지 | AI 경계 내 임시 파일 수명, 삭제·감사 정책 |
| TD-22 | vector 검색 인덱스 | embedding 차원 확정 전 미생성 | HNSW/IVFFlat 선택, 운영 데이터 규모, rebuild 절차 |

## P2 — 운영·개발 프로세스

| ID | 결정 | 현재 상태 | 필요한 결정 |
| --- | --- | --- | --- |
| TD-23 | pgvector image 배포 경로 | mirror 경로와 canonical 동일 digest 사용 | CI/운영 registry mirror 및 공급망 정책 |
| TD-24 | Docker Desktop CDN 장애 | Docker Hub CloudFront 직접 pull은 `EOF` 재현 | 사내 proxy/registry cache 표준 |
| TD-25 | Testcontainers CI | 로컬 Docker에서 62개 통과 | CI Docker 권한, image cache, timeout |
| TD-26 | 시간 저장 정책 | MySQL DATETIME(6) UTC, session `+00:00`; API offset timestamp | API 경계 변환 회귀 테스트와 운영 모니터링 |
| TD-27 | Migration 승인 | Flyway V1~V18, pg SQL 001~003 | DBA 리뷰, checksum 고정, 운영 실행 계정 |

## 현재 확정된 물리 규칙

팀이 다시 충돌을 만들지 않도록 이미 적용·검증된 규칙은 유지한다.

- MySQL 물리 테이블은 snake_case 12개이며, 일반 업무·계정 테이블
  11개와 내부 trigger guard 1개로 구분한다.
- 사용자 업무 ID는 `user_account.user_id CHAR(9)`이고, Cross-DB 얼굴
  경계에는 `user_account.user_ref CHAR(36)`을 사용한다.
- PostgreSQL `face_profile.user_ref`는 native `UUID`다.
- Cross-DB 관계는 표준 하이픈 UUID 문자열 값으로만 연결하고 FK를 만들지
  않는다.
- `return_attempt.request_id`, `device_operation.command_id`,
  `device_operation.event_id`, 결제 주문·승인 ID는 각자 독립 UNIQUE다.
- `return_attempt.rental_id`는 UNIQUE가 아니며 재시도를 허용한다.
- 반납 공식 상태 필드는 `status`이고 `processStatus`·`process_status`를
  재도입하지 않는다.
- 얼굴 이미지와 embedding은 Spring Payload·MySQL·로그에 저장하지 않는다.
- Spring은 PostgreSQL에 직접 연결하지 않는다.
- Pi용 SQLite·MySQL·Redis를 추가하지 않는다.
- embedding 차원이 확정되기 전 `VECTOR(n)`이나 vector index를 만들지 않는다.

## 결정 기록 템플릿

각 결정은 다음 형식으로 Jira 또는 ADR에 남긴다.

```text
Decision ID:
Owner:
Due date:
Decision:
Alternatives:
Reason:
Affected API/schema/files:
Migration required:
Backward compatibility:
Test evidence:
Rollback:
```

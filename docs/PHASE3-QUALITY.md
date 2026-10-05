# Phase 3 Quality

## 1. API validation

입력값 검증을 API 경계에서 강화했습니다.

- ID: 양수만 허용
- page: 0 이상
- size: 1~100
- 상태 이미지 촬영 시각: 현재 또는 과거
- malformed body / enum / parameter: 400
- validation violation: 400

에러 응답은 기존 ApiErrorResponse 형식을 유지합니다.

## 2. 상태 전이 테스트

RentalContract와 RentalProduct의 정상 전이와 잘못된 전이를 도메인 단위 테스트로 검증합니다.

```text
REQUESTED
→ APPROVED
→ PICKED_UP
→ IN_USE
→ RETURNED
→ INSPECTING
→ COMPLETED
```

상품:

```text
AVAILABLE
→ RESERVED
→ RENTED
→ INSPECTION
→ AVAILABLE / MAINTENANCE
```

중간 단계를 건너뛰거나 같은 전이를 반복하면 IllegalStateException으로 거부합니다.

## 3. 서비스 계층 동시성

동일 RentalProduct에 두 사용자가 동시에 대여 신청하는 시나리오를 실제 MySQL 통합 테스트로 검증합니다.

RentalProduct 조회에 사용하는 PESSIMISTIC_WRITE lock을 기준으로 다음 결과를 보장합니다.

```text
동시 요청 2건
→ 성공 1건
→ 상태 충돌 거부 1건
→ RentalContract 1건만 생성
→ RentalProduct RESERVED
```

## 4. USER / OPERATOR 권한 분리

member_account에 role을 추가합니다.

```text
USER
OPERATOR
```

현재 접근 정책:

- 상품/지점 탐색: 공개
- 대여 신청: 본인 또는 OPERATOR
- 대여 상세/인수/이용 시작/반납: 계약 소유자 또는 OPERATOR
- BEFORE/AFTER 상태와 손상 입력: 계약 소유자 또는 OPERATOR
- 손상 비교: OPERATOR
- ReturnInspection 조회/확정: OPERATOR
- 회원 대여 이력: 본인 또는 OPERATOR

## 현재 인증 경계

현재 단계에서는 실제 로그인 토큰을 아직 도입하지 않았기 때문에 API가 `X-Actor-Id` 헤더로 호출 주체를 받습니다.

중요하게도 역할은 헤더에서 신뢰하지 않고 DB의 `member_account.role`을 조회해 결정합니다.

```text
X-Actor-Id
→ Member 조회
→ ACTIVE 확인
→ DB role 확인
→ 소유권 / OPERATOR 권한 확인
```

이 방식은 Phase 3의 권한 정책을 구현·검증하기 위한 임시 인증 경계입니다. 운영용 인증에서는 JWT/OAuth 등의 인증 결과에서 actor identity를 공급하도록 교체해야 합니다.

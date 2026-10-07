# Phase 3 Quality

## 1. API Validation

입력값 검증을 API 경계에서 수행합니다.

- ID: 양수만 허용
- page: 0 이상
- size: 1~100
- 상태 이미지 촬영 시각: 현재 또는 과거
- malformed body / enum / parameter: 400
- validation violation: 400

에러 응답은 공통 ApiErrorResponse 형식을 사용합니다.

## 2. State Transition Test

RentalContract와 RentalProduct의 정상 전이와 잘못된 전이를 도메인 단위 테스트로 검증합니다.

```text
RentalContract
REQUESTED
→ APPROVED
→ PICKED_UP
→ IN_USE
→ RETURNED
→ INSPECTING
→ COMPLETED

RentalProduct
AVAILABLE
→ RESERVED
→ RENTED
→ INSPECTION
→ AVAILABLE / MAINTENANCE
```

중간 단계를 건너뛰거나 같은 전이를 반복하면 잘못된 상태 전이로 거부합니다.

## 3. Concurrency

동일 RentalProduct에 두 사용자가 동시에 대여 신청하는 시나리오를 MySQL 통합 테스트로 검증합니다.

RentalProduct 조회 시 `PESSIMISTIC_WRITE` lock을 적용합니다.

```text
동시 요청 2건
→ 성공 1건
→ 상태 충돌 거부 1건
→ RentalContract 1건 생성
→ RentalProduct RESERVED
```

RentalContract와 ReturnInspection의 `@Version`은 상태 변경 충돌 감지를 보조합니다.

## 4. Authentication / Authorization

임시 `X-Actor-Id` 방식은 제거하고 이메일/비밀번호 로그인 + JWT access token으로 전환했습니다.

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

비밀번호는 BCrypt로 hash하고, JWT subject에는 Member ID만 저장합니다.

요청 처리 흐름:

```text
Authorization: Bearer <access token>
→ JWT 서명 / 만료 검증
→ Member 조회
→ ACTIVE 확인
→ DB role 확인
→ SecurityContext principal 생성
→ 소유권 / OPERATOR 권한 확인
```

role은 JWT claim이나 request parameter를 신뢰하지 않고 현재 DB 값을 기준으로 판단합니다.

접근 정책:

- 상품/지점 탐색: 공개
- 회원가입 / 로그인: 공개
- 대여 신청·상세·인수·이용 시작·반납: 인증 필요 + 계약 소유권 또는 OPERATOR
- BEFORE/AFTER 상태 및 손상 입력: 계약 소유자 또는 OPERATOR
- 손상 비교: OPERATOR
- ReturnInspection 조회/확정: OPERATOR
- 회원 대여 이력: 본인 또는 OPERATOR
- `/api/v1/operator/**`: OPERATOR

현재 Frontend는 access token을 localStorage에 저장합니다. refresh token과 서버 측 token revocation은 v1 범위에서 제외했습니다.

## 5. Image Storage

ConditionImage가 공개 URL을 직접 소유하지 않도록 저장 책임을 분리했습니다.

```text
Multipart image upload
→ ConditionImageStorageService
→ ImageStorage port
→ LocalFileImageStorage adapter
→ storageKey 반환
→ ConditionImage metadata 저장
```

DB에는 다음 메타데이터만 유지합니다.

- storageKey
- originalFilename
- contentType
- fileSize
- captureOrder
- viewpoint
- capturedAt

조회 응답의 content path는 API 경로를 동적으로 구성합니다. 현재 구현은 Local File adapter를 사용하며 storage interface를 통해 이후 object storage로 교체할 수 있습니다.

저장 루트는 `RENTALTRACK_STORAGE_ROOT`, 이미지 최대 크기는 `RENTALTRACK_MAX_IMAGE_BYTES`로 설정할 수 있습니다.

허용 형식은 JPEG, PNG, WEBP이며 기본 최대 크기는 10MB입니다. DB transaction이 rollback되면 같은 요청에서 저장한 파일도 정리합니다.

## 6. Automated Validation

Backend Validation은 Java 21 + MySQL 8.4 환경에서 다음을 자동 검증합니다.

- Flyway migration / JPA schema validation
- API validation
- Rental 상태 전이
- 동시 대여 요청
- USER / OPERATOR 접근 정책
- 이미지 저장소 업로드 처리
- Auth service / JWT 발급 검증
- 운영자 AI 조회·의사결정 API

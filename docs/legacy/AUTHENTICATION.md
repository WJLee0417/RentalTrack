# Authentication / JWT

RentalTrack은 Phase 6부터 임시 X-Actor-Id 인증을 제거하고 이메일/비밀번호 로그인 + JWT access token 구조를 사용합니다.

## API

POST /api/v1/auth/register
POST /api/v1/auth/login
GET /api/v1/auth/me

회원 가입은 USER role만 생성합니다.

비밀번호는 BCryptPasswordEncoder로 해시하여 member_account.password_hash에 저장합니다.

JWT에는 memberId만 subject로 넣습니다. 각 요청에서 토큰 서명과 만료를 검증한 뒤 member_account를 다시 조회하여 ACTIVE 상태와 현재 role을 확인합니다.

기본 access token 만료 시간은 30분입니다.

설정:
- RENTALTRACK_JWT_SECRET_BASE64
- RENTALTRACK_JWT_ACCESS_TOKEN_SECONDS

application.yaml의 기본 secret은 로컬/CI 개발용입니다. 배포 환경에서는 별도 secret을 사용해야 합니다.

## 공개 경로

- POST /api/v1/auth/register
- POST /api/v1/auth/login
- GET /api/v1/locations
- GET /api/v1/products
- GET /actuator/health
- GET /actuator/info

그 외 API는 인증이 필요하며 /api/v1/operator/** 는 OPERATOR role이 필요합니다.

계약 소유권 같은 세부 권한은 AccessControlService에서 AuthenticatedMember principal 기준으로 추가 검증합니다.

## Operator 계정 준비

공개 가입으로는 OPERATOR를 만들 수 없습니다. 로컬 데모에서는 회원 가입 후 DB에서 role을 OPERATOR로 변경합니다.

UPDATE member_account
SET role = 'OPERATOR'
WHERE email = 'operator@example.com';

JWT에는 role을 고정하지 않으므로 다음 요청부터 DB의 변경된 role이 반영됩니다.

## Frontend

운영자 Dashboard는 이메일/비밀번호 로그인 후 Authorization: Bearer <access token> 헤더로 API를 호출합니다.

현재 access token은 localStorage에 저장합니다. refresh token은 아직 구현하지 않았습니다.

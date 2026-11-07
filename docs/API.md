# API v2

인증은 기존 경로 `/api/v1/auth`를 사용한다. 업무 API는 `/api/v2`로 교체했다. JSON의 날짜·시각은 ISO local date-time, 서버 업무 시간대는 Asia/Seoul.

## 인증

```http
POST /api/v1/auth/register
Content-Type: application/json

{"loginId":"customer01","email":"customer01@example.com","name":"회원","password":"your-password"}
```

```http
POST /api/v1/auth/login
Content-Type: application/json

{"loginId":"customer01","password":"your-password"}
```

응답의 accessToken을 `Authorization: Bearer <token>`으로 전달. GET `/api/v1/auth/me`로 현재 회원 조회. 회원가입에 역할 필드를 보내도 권한을 부여하지 않음.

## 카탈로그·등록

| Method | 경로 | 권한·내용 |
| --- | --- | --- |
| GET | /api/v2/catalog/businesses | 공개, ACTIVE 사업자 |
| GET | /api/v2/catalog/items | 공개, 개별 상품과 requestable |
| GET | /api/v2/catalog/codes | 공개, 사용 중 코드 그룹·값·표시명 |
| POST | /api/v2/businesses | ADMIN, businessNo·businessName·contactEmail |
| POST | /api/v2/items | ADMIN, businessId·assetCode·itemName·categoryCd·description |

## 대여

| Method | 경로 | 입력·내용 |
| --- | --- | --- |
| POST | /api/v2/rentals | `{itemId}` 대여 신청 |
| GET | /api/v2/rentals | 현재 회원의 대여 이력 |
| GET | /api/v2/rentals/{id} | rental·conditions·inspection 집계 조회 |
| POST | /api/v2/rentals/{id}/start | `{rentalStartAt,rentalEndAt}` |
| POST | /api/v2/rentals/{id}/return | `{actualReturnAt}` |
| POST | /api/v2/rentals/{id}/cancel | REQUESTED 상태 취소 |

계약 소유 회원 또는 검수 담당자·관리자만 접근. 일반회원의 다른 회원 계약·사진 접근은 403.

## 상태 기록·사진·관찰

| Method | 경로 | 입력·내용 |
| --- | --- | --- |
| POST | /api/v2/rentals/{id}/conditions | `{recordStageCd,recordNote}` 최초 차수 |
| PATCH | /api/v2/conditions/{id} | `{recordNote}` 미확정 설명 수정 |
| POST | /api/v2/conditions/{id}/confirm | 사진 유무 검사 후 확정 |
| POST | /api/v2/conditions/{id}/revisions | `{recordNote}` 최신 확정 기록의 다음 차수 복사 |
| POST | /api/v2/conditions/{id}/photos | multipart: file, captureAngleCd, optional capturedAt |
| GET | /api/v2/photos/{id}/content | 권한 검사 후 image/png 또는 image/jpeg |
| POST | /api/v2/conditions/{id}/damages | 손상 관찰 추가 |
| PATCH | /api/v2/damages/{id} | 미확정 차수의 관찰 수정 |
| DELETE | /api/v2/damages/{id} | 미확정 차수의 관찰과 증거 연결 삭제 |
| POST | /api/v2/damages/{id}/photos | `{photoId,box}` 같은 기록의 증거 사진 연결 |

손상 입력: `damageLocationCd`, `damageTypeCd`, `severityCd`, `damageDescription`.

영역 형식:

```json
{"photoId":1,"box":{"x":0.1,"y":0.2,"width":0.3,"height":0.4}}
```

증거 연결의 box는 null 가능. 네 좌표를 지정한다면 모두 필수이고 이미지 안에 있어야 함. 확정 기록 수정과 검수 생성 후 기록 변경은 409.

## 검수·판정

| Method | 경로 | 입력·내용 |
| --- | --- | --- |
| POST | /api/v2/rentals/{id}/inspection | `{intakeConditionId,returnConditionId,inspectionNote}` 기준 기록 고정 |
| POST | /api/v2/inspections/{id}/begin | DRAFT → IN_PROGRESS |
| PUT | /api/v2/inspections/{id}/assessments | 반납 손상별 판정 등록·수정 |
| POST | /api/v2/inspections/{id}/complete | 미판정·HOLD 검사, 결과 계산, 상품·계약 상태 전환 |

검수 담당자·관리자만 변경 가능. 일반회원은 자신의 계약 상세에서 검수 결과 조회 가능.

```json
{"returnDamageId":2,"intakeDamageId":1,"assessmentResultCd":"EXISTING","assessmentReason":"인수 사진과 동일 위치의 손상"}
```

EXISTING에는 선택한 인수 기록의 손상 참조 필수. NEW/HOLD는 intakeDamageId=null. 반납 손상도 선택한 반납 기록에 속해야 함. API는 최종 결과를 임의 입력받지 않음.

## 이미지 분석 후보 결과

검수 담당자·관리자의 POST `/api/v2/photos/{id}/analyses`:

```json
{
  "analysisRunId":"4123aa21-9a21-4221-a121-123456789abc",
  "detectionSeq":1,
  "modelName":"external-model",
  "modelVersion":"v1",
  "predictedDamageTypeCd":"SCRATCH",
  "confidenceScore":0.9,
  "box":{"x":0.1,"y":0.2,"width":0.3,"height":0.4},
  "analyzedAt":"2026-11-07T16:00:00"
}
```

사진·실행·순번 조합 중복은 409. 후보 저장은 손상 관찰이나 판정 확정이 아님. 추론 실행 API는 제공하지 않음.

## 관리자 기존 이력 이전

ADMIN 전용 `/api/v2/admin/legacy-import`:

- GET `/source`: 원본 회원·상품·검수·위치 목록과 사업자·담당자 선택지. 비밀번호 해시는 응답하지 않음.
- POST `/preview`: 매핑 JSON을 받아 ready·planToken·pendingCounts·alreadyMappedCounts·issues·warnings 반환.
- POST `/apply`: `{manifest,planToken}`. 같은 계획 재시도는 기존 실행 결과 반환. 원본·대상·사진·매핑 변경 시 409.

입력과 상태 변환은 [이전 문서](MIGRATION.md) 참조. 이전된 계약·상태·사진·관찰·검수의 상세 응답에는 legacySource 메타데이터가 추가됨.

## 오류 응답

- 400: 잘못된 코드·입력·영역·파일
- 401: 인증 없음·만료/구버전 토큰·잘못된 로그인
- 403: 다른 회원의 계약·사진 또는 권한 부족
- 404: 대상 없음
- 409: 상태 충돌·확정 기록 변경·중복·DB 무결성 충돌

JWT 필터는 토큰 확인 이후의 애플리케이션 오류를 401로 바꾸지 않는다.

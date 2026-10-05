# Phase 2 Core Flow

## 현재 구현 범위

```text
상품/지점 탐색
→ 대여 신청
→ 상품 인수
→ 대여 전 상태 기록
→ 상태 이미지 메타데이터 등록
→ 실제 이용 시작
```

## 상태 전이

### RentalContract

```text
REQUESTED
→ APPROVED
→ PICKED_UP
→ IN_USE
```

### RentalProduct

```text
AVAILABLE
→ RESERVED
→ RENTED
```

상품은 인수 시점부터 사용자의 점유 상태이므로 `RENTED`로 전환합니다. 다만 RentalContract는 대여 전 상태 기록이 끝날 때까지 `PICKED_UP`에 머무르며, 실제 이용 시작 API가 호출된 뒤 `IN_USE`로 전환됩니다.

## API

```text
GET  /api/v1/locations
GET  /api/v1/products?locationId=&categoryId=

POST /api/v1/rentals
POST /api/v1/rentals/{id}/pickup

POST /api/v1/rentals/{id}/conditions/before
POST /api/v1/rentals/{id}/conditions/before/images
GET  /api/v1/rentals/{id}/conditions/before

POST /api/v1/rentals/{id}/start
GET  /api/v1/rentals/{id}
```

## 실제 이용 시작 조건

`POST /api/v1/rentals/{id}/start`는 다음 조건을 모두 만족해야 합니다.

- 계약 상태가 `PICKED_UP`
- `BEFORE_RENTAL ConditionRecord` 존재
- 해당 ConditionRecord에 이미지가 한 장 이상 존재
- `dueAt`이 이용 시작 시각보다 미래

이를 통해 상태 기록 없이 바로 이용 단계로 넘어가는 흐름을 차단합니다.

## 이미지 저장 범위

현재 Phase 2에서는 이미지 파일 자체를 업로드하지 않습니다.

Backend는 다음 메타데이터만 저장합니다.

- `imageUrl`
- `storageKey`
- `captureOrder`
- `viewpoint`
- `capturedAt`

Object Storage 업로드 및 presigned URL 발급은 Phase 3의 이미지 저장소 분리에서 구현합니다.

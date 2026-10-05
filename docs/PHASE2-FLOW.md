# Phase 2 Core Flow

## 현재 구현 범위

```text
상품/지점 탐색
→ 대여 신청
→ 상품 인수
→ BEFORE_RENTAL 상태 기록
→ BEFORE_RENTAL 이미지 업로드 및 메타데이터
→ 실제 이용 시작
→ 반납
→ AFTER_RENTAL 상태 기록
→ AFTER_RENTAL 이미지 업로드 및 메타데이터
→ DamageRecord 비교
→ ReturnInspection
→ 대여 완료
```

## RentalContract 상태

```text
REQUESTED
→ APPROVED
→ PICKED_UP
→ IN_USE
→ RETURNED
→ INSPECTING
→ COMPLETED
```

## RentalProduct 상태

```text
AVAILABLE
→ RESERVED
→ RENTED
→ INSPECTION
→ AVAILABLE 또는 MAINTENANCE
```

반납 시 상품의 `currentLocation`도 반납 지점으로 변경합니다.

## 반납 이후 API

```text
POST /api/v1/rentals/{id}/return

POST /api/v1/rentals/{id}/conditions/after
POST /api/v1/rentals/{id}/conditions/after/images
GET  /api/v1/rentals/{id}/conditions/after

POST /api/v1/rentals/{id}/damages/after
POST /api/v1/rentals/{id}/damages/compare
GET  /api/v1/rentals/{id}/damages/after

POST /api/v1/rentals/{id}/inspection
GET  /api/v1/rentals/{id}/inspection
```

## DamageRecord 비교

대여 전 손상은 `BASELINE`, 반납 후 입력된 손상은 처음에 `UNRESOLVED`로 저장합니다.

비교 시 현재 MVP에서는 다음 두 값이 같은 경우 기존 손상으로 매칭합니다.

```text
damageType
+
정규화된 damageLocation
```

매칭되면:

```text
EXISTING
→ matchedBeforeDamage 연결
```

매칭되지 않으면:

```text
NEW
```

이 비교는 손상 책임을 자동 확정하는 AI 판단이 아니라, 검수자가 확인할 후보를 정리하기 위한 규칙 기반 비교입니다.

## ReturnInspection

DamageRecord 비교가 끝나면 RentalContract는 `INSPECTING`으로 전환됩니다.

최종 결과:

```text
NO_NEW_DAMAGE
NEW_DAMAGE_CONFIRMED
REVIEW_REQUIRED
```

- `NO_NEW_DAMAGE`: 계약 완료 + 상품 AVAILABLE
- `NEW_DAMAGE_CONFIRMED`: 계약 완료 + 상품 MAINTENANCE
- `REVIEW_REQUIRED`: 계약 INSPECTING + 상품 INSPECTION 유지

`REVIEW_REQUIRED`로 저장된 검수 결과는 이후 최종 결과로 수정할 수 있습니다.


## 이미지 업로드 변경

Phase 3부터 ConditionImage API는 클라이언트가 임의의 `imageUrl`이나 `storageKey`를 전달하지 않습니다.

```text
multipart/form-data
- file
- captureOrder
- viewpoint
- capturedAt
```

Backend가 저장소를 선택하고 `storageKey`를 생성합니다. 조회 시에는 `contentPath`를 통해 권한 검사를 거친 이미지 내용을 반환합니다.

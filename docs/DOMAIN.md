# RentalTrack Domain v1.0

## 문제 정의

RentalTrack은 이동 중 갑자기 필요한 생활용품을 주변 대여 지점에서 찾고 대여·반납할 수 있도록 하며, 반복 대여되는 개별 상품의 상태·사진·손상 이력을 대여 건 단위로 추적하는 웹 서비스입니다.

## Phase 1 확정 도메인

### Member
서비스 이용자.

### RentalLocation
상품을 인수·반납할 수 있는 대여 지점.

### ProductCategory
보조배터리, 충전기, 멀티어댑터, 휴대용 선풍기, 접이식 카트 등의 상품 분류.

### RentalProduct
반복 대여되는 개별 상품.

```text
AVAILABLE
RESERVED
RENTED
INSPECTION
MAINTENANCE
RETIRED
```

### RentalContract
회원과 개별 상품 사이의 한 번의 대여 건.

```text
REQUESTED
→ APPROVED
→ IN_USE
→ RETURNED
→ INSPECTING
→ COMPLETED
```

예외:

```text
REJECTED
CANCELED
```

### ConditionRecord
한 RentalContract의 대여 전·후 상태 기록.

```text
BEFORE_RENTAL
AFTER_RENTAL
```

### ConditionImage
ConditionRecord에 속한 사진 메타데이터.

### DamageRecord
ConditionRecord에서 확인한 개별 손상.

```text
DamageComparisonResult

BASELINE
UNRESOLVED
EXISTING
NEW
```

`EXISTING`인 대여 후 손상은 `matchedBeforeDamage`를 통해 대응되는 대여 전 손상을 직접 가리킵니다.

### ReturnInspection
대여 전·후 상태와 DamageRecord 비교가 끝난 뒤 RentalContract 단위로 저장하는 최종 검수 결과.

```text
NO_NEW_DAMAGE
NEW_DAMAGE_CONFIRMED
REVIEW_REQUIRED
```

## 핵심 관계

```text
Member
  └─ RentalContract
       ├─ pickup RentalLocation
       ├─ return RentalLocation
       ├─ RentalProduct
       │    └─ ProductCategory
       ├─ BEFORE_RENTAL ConditionRecord
       │    ├─ ConditionImage[]
       │    └─ DamageRecord(BASELINE)[]
       ├─ AFTER_RENTAL ConditionRecord
       │    ├─ ConditionImage[]
       │    └─ DamageRecord[]
       │         ├─ EXISTING → BEFORE DamageRecord
       │         └─ NEW
       └─ ReturnInspection
```

## Phase 1 핵심 설계 원칙

1. 상품의 상태 이력을 단일 현재 값에 덮어쓰지 않는다.
2. 반복 대여 이력은 RentalContract로 분리한다.
3. 대여 전·후 상태는 같은 RentalContract 안에서 별도 ConditionRecord로 보존한다.
4. 사진은 ConditionRecord에 귀속시키고 DB에는 저장 위치와 메타데이터만 저장한다.
5. 손상은 DamageRecord로 구조화하고 대여 후 기존 손상은 대여 전 기록과 직접 연결한다.
6. ReturnInspection은 손상 개별 기록과 분리된 계약 단위 최종 검수 결과다.
7. 과거 계약·상태·손상·검수 이력을 깨뜨리는 물리 삭제를 기본 운영 방식으로 사용하지 않는다.
8. 손상 책임 판단을 AI가 자동 확정하지 않는다.
9. AI 수요 예측은 별도의 운영 의사결정 지원 기능으로 둔다.

상세 물리 모델은 [ERD.md](./ERD.md)를 기준으로 합니다.

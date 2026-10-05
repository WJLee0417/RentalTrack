# RentalTrack Domain v0.3

## 문제 정의

RentalTrack은 이동 중 갑자기 필요한 생활용품을 주변 대여 지점에서 찾고 대여·반납할 수 있도록 하며, 반복 대여되는 개별 상품의 상태와 이용 이력을 대여 건 단위로 추적하는 웹 서비스입니다.

## 현재 확정 도메인

### Member

서비스 이용자입니다.

```text
ACTIVE
SUSPENDED
WITHDRAWN
```

현재는 사용자 식별 정보만 담당하고 인증 자격 증명은 분리된 관심사로 둡니다.

### RentalLocation

상품을 인수·반납할 수 있는 대여 지점입니다.

### ProductCategory

상품 분류입니다.

예:

```text
POWER_BANK
CHARGER
MULTI_ADAPTER
PORTABLE_FAN
FOLDING_CART
```

### RentalProduct

반복 대여되는 개별 상품입니다.

```text
AVAILABLE
RESERVED
RENTED
INSPECTION
MAINTENANCE
RETIRED
```

### RentalContract

회원과 개별 상품 사이의 한 번의 대여 건입니다.

```text
REQUESTED
→ APPROVED
→ IN_USE
→ RETURNED
→ INSPECTING
→ COMPLETED
```

예외 상태:

```text
REJECTED
CANCELED
```

### ConditionRecord

한 RentalContract의 대여 전 또는 대여 후 상품 상태를 기록합니다.

```text
BEFORE_RENTAL
AFTER_RENTAL
```

핵심 원칙은 `RentalProduct.currentCondition` 같은 값을 계속 덮어쓰지 않는 것입니다.

```text
RentalContract #101
├─ BEFORE_RENTAL ConditionRecord
└─ AFTER_RENTAL ConditionRecord
```

각 대여 건의 전·후 기록을 별도로 남겨 반복 대여 시점 사이의 상태 변화를 추적합니다.

### ConditionImage

ConditionRecord에 속한 사진 메타데이터입니다.

이미지 바이너리는 DB에 넣지 않고 URL과 storage key를 저장합니다.

## 핵심 관계

```text
Member
  └─ RentalContract
       ├─ pickup RentalLocation
       ├─ return RentalLocation
       ├─ RentalProduct
       │    └─ ProductCategory
       ├─ BEFORE ConditionRecord
       │    └─ ConditionImage
       └─ AFTER ConditionRecord
            └─ ConditionImage
```

## 다음 도메인

### DamageRecord
ConditionRecord에서 확인한 손상 위치·종류·설명을 구조화합니다.

### ReturnInspection
대여 전·후 상태와 DamageRecord를 비교해 운영자가 최종 검수한 결과를 RentalContract 단위로 저장합니다.

## 설계 원칙

1. 상품 상태를 단일 현재 값만으로 표현하지 않는다.
2. 반복 대여 이력은 RentalContract로 분리한다.
3. 대여 전·후 상태는 동일 RentalContract 안에서 별도 ConditionRecord로 보존한다.
4. 사진은 ConditionRecord에 귀속시키며 DB에는 메타데이터만 저장한다.
5. 과거 계약과 상태 이력을 깨뜨리는 물리 삭제를 기본 운영 방식으로 사용하지 않는다.
6. 손상 책임 판단을 AI가 자동 확정하지 않는다.
7. AI 수요 예측은 운영 의사결정 지원에 사용한다.

상세 물리 모델은 [ERD.md](./ERD.md)를 기준으로 합니다.

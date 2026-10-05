# RentalTrack Domain v0.2

## 문제 정의

RentalTrack은 이동 중 갑자기 필요한 생활용품을 주변 대여 지점에서 찾고 대여·반납할 수 있도록 하며, 반복 대여되는 개별 상품의 상태와 이용 이력을 대여 건 단위로 추적하는 웹 서비스입니다.

## Phase 1 확정 도메인

### RentalLocation
상품을 인수·반납할 수 있는 대여 지점입니다.

- 물리 장치/슬롯을 표현하지 않습니다.
- 위치 탐색을 위해 주소와 위·경도를 가집니다.
- 운영 종료 시 삭제하지 않고 비활성화합니다.

### ProductCategory
보조배터리, 충전기, 멀티어댑터, 휴대용 선풍기, 접이식 카트 등 상품 분류입니다.

- 카테고리와 개별 대여 자산을 분리합니다.
- 운영 중단 시 삭제하지 않고 비활성화합니다.

### RentalProduct
반복 대여되는 개별 상품입니다.

예:

```text
ProductCategory = POWER_BANK
RentalProduct = PB-GANGNAM-0001
RentalProduct = PB-GANGNAM-0002
```

초기 상태:

```text
AVAILABLE
RESERVED
RENTED
INSPECTION
MAINTENANCE
RETIRED
```

### RentalContract
한 회원이 한 개별 상품을 한 번 대여하는 사건입니다.

기본 상태 흐름:

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

인수 지점과 반납 지점을 별도로 저장하여 다른 지점 반납을 수용합니다.

## Phase 1 후속 도메인

### Member
서비스 이용자. 현재 RentalContract에는 `memberId`만 먼저 두고 다음 migration에서 FK를 연결합니다.

### ConditionRecord
대여 전·후 상품 상태 기록.

- BEFORE_RENTAL
- AFTER_RENTAL

### ConditionImage
ConditionRecord에 연결되는 상태 사진.

### DamageRecord
특정 상태 기록에서 확인한 손상 정보.

### ReturnInspection
반납 이후 운영자가 대여 전·후 상태와 손상 이력을 확인한 결과.

## 핵심 관계

```text
Member
  └─ RentalContract
       ├─ pickup RentalLocation
       ├─ return RentalLocation
       ├─ RentalProduct
       │    └─ ProductCategory
       ├─ BEFORE ConditionRecord
       │    ├─ ConditionImage
       │    └─ DamageRecord
       ├─ AFTER ConditionRecord
       │    ├─ ConditionImage
       │    └─ DamageRecord
       └─ ReturnInspection
```

## 설계 원칙

1. 상품의 현재 상태만 덮어쓰지 않는다.
2. 동일 상품이 반복 대여되어도 RentalContract별 이력을 분리한다.
3. 지점·카테고리·상품은 과거 대여 이력 보존을 위해 물리 삭제를 기본 운영 방식으로 사용하지 않는다.
4. 상태·사진·손상은 다음 단계에서 반드시 RentalContract와 시점에 연결한다.
5. 손상 책임 판단을 AI가 자동 확정하지 않는다.
6. 수요 예측 결과는 운영 의사결정 지원 정보로 사용한다.

상세 물리 모델은 [ERD.md](./ERD.md)를 기준으로 합니다.

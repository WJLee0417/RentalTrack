# RentalTrack Domain v0.1

## 문제 정의

RentalTrack은 이동 중 갑자기 필요한 생활용품을 대여하는 과정에서 상품의 이용 가능 여부와 상태 이력을 함께 관리하는 웹 서비스입니다.

## 핵심 도메인

### Member
서비스 이용자.

### RentalLocation
상품을 인수·반납할 수 있는 대여 지점.

### ProductCategory
보조배터리, 충전기, 멀티어댑터, 휴대용 선풍기, 접이식 카트 등 상품 분류.

### RentalProduct
반복 대여되는 개별 상품. 동일한 카테고리라도 각각 별도 식별합니다.

### RentalContract
회원과 개별 상품 사이의 한 번의 대여 건.

초기 상태 후보:

```text
REQUESTED
→ APPROVED
→ IN_USE
→ RETURNED
→ INSPECTING
→ COMPLETED
```

예외 상태는 도메인 설계 단계에서 별도로 확정합니다.

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
       ├─ RentalLocation
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
2. 상태·사진·손상은 대여 건과 시점에 연결한다.
3. 동일 상품이 여러 번 대여되어도 대여 건별 이력을 분리한다.
4. 손상 책임 판단을 AI가 자동 확정하지 않는다.
5. 수요 예측 결과는 운영 의사결정 지원 정보로 사용한다.

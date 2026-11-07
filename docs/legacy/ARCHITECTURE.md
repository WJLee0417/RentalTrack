# RentalTrack Architecture v1.0

## Overall Architecture

```text
React + TypeScript
Operator Dashboard
        │
        │ REST / JWT
        ▼
Spring Boot
├─ Auth / Access Control
├─ Catalog
├─ Rental
├─ Condition / Damage / Inspection
└─ Forecast / Recommendation API
        │
        ▼
      MySQL
        ▲
        │
Python Batch
├─ Dataset Builder
├─ Historical Average
├─ Granite TTM-R3 Zero-shot
├─ Evaluation
└─ Replenishment Rule
```

RentalTrack은 웹 기반 렌탈 흐름과 운영 의사결정 지원 기능을 하나의 서비스 데이터 흐름으로 연결합니다.

## Backend Domain

RentalContract와 RentalProduct의 상태를 분리해 관리합니다.

```text
RentalContract
REQUESTED → APPROVED → PICKED_UP → IN_USE
→ RETURNED → INSPECTING → COMPLETED

RentalProduct
AVAILABLE → RESERVED → RENTED → INSPECTION
→ AVAILABLE or MAINTENANCE
```

대여 전·후 상태는 RentalContract에 귀속된 ConditionRecord로 보존합니다.

```text
RentalContract
├─ BEFORE_RENTAL ConditionRecord
│  ├─ ConditionImage[]
│  └─ DamageRecord[]
├─ AFTER_RENTAL ConditionRecord
│  ├─ ConditionImage[]
│  └─ DamageRecord[]
└─ ReturnInspection
```

상품의 현재 상태만 덮어쓰지 않기 때문에 반복 대여에서도 계약별 상태·사진·손상 이력을 구분할 수 있습니다.

## Concurrency

동일 상품에 대한 동시 대여 요청은 RentalProduct 조회 시 `PESSIMISTIC_WRITE` lock으로 직렬화합니다.

```text
동일 상품 동시 요청
→ RentalProduct row lock
→ 첫 요청이 AVAILABLE → RESERVED
→ 후속 요청은 변경된 상태 확인 후 거부
```

RentalContract와 ReturnInspection 등에는 `@Version`을 두어 상태 변경 충돌을 추가로 감지합니다.

## Authentication / Authorization

```text
email / password
→ BCrypt
→ JWT access token
→ JwtAuthenticationFilter
→ Member DB 조회
→ ACTIVE 상태 / 현재 role 확인
→ SecurityContext
```

JWT subject에는 Member ID만 저장합니다. 각 요청에서 DB의 현재 Member 상태와 role을 다시 조회해 USER / OPERATOR 권한을 판단합니다.

## Image Storage

```text
Multipart upload
→ ConditionImageStorageService
→ ImageStorage port
→ LocalFileImageStorage
→ storageKey + metadata
→ ConditionImage
```

현재는 Local File adapter를 사용합니다. DB에는 공개 URL 대신 `storage_key`, 원본 파일명, content type, file size와 촬영 메타데이터를 저장합니다.

## Data / AI

수요 예측은 사용자 요청 경로와 분리된 batch로 실행합니다.

```text
Rental history
→ hourly location × category dataset
→ Historical Average / Granite TTM-R3
→ chronological holdout evaluation
→ model_run
→ demand_forecast
→ inventory_recommendation
→ Spring Boot API
→ Operator Dashboard
```

실시간 inference server를 두지 않고 Python batch가 결과를 MySQL에 저장합니다. Spring Boot는 저장된 예측과 추천 결과를 조회해 운영자 Dashboard에 제공합니다.

예측 모델은 수요를 생성하고, 보충 수량과 위험도는 별도 운영 Rule에서 계산합니다. 최종 판단은 운영자가 `ACCEPTED / DISMISSED`로 기록합니다.

## v1 Scope

RentalTrack v1은 기존 물리 장치 중심 구조와 분리된 웹 렌탈 서비스입니다.

다음 요소는 현재 범위에 포함하지 않습니다.

- Raspberry Pi / Jetson
- MQTT / device slot state
- face authentication
- kiosk
- damage AI
- device reconciliation
- 실시간 ML inference server
- 추천 승인 이후 물리 재배치 자동 실행

# RentalTrack

RentalTrack은 이동 중 필요한 생활용품을 주변 대여 지점에서 이용할 수 있도록 만든 웹 기반 렌탈 서비스입니다.

대여 전·후 상태와 사진을 대여 건별로 기록해 상품 이력을 남기고, 지점·상품별 이용 데이터를 수요 예측으로 연결해 운영자의 재고 배치 판단까지 지원합니다.

## Service Flow

### Rental

```text
상품/지점 탐색
→ 대여 신청
→ 인수
→ BEFORE 상태/사진 기록
→ 이용 시작
→ 반납
→ AFTER 상태/사진 기록
→ 손상 비교
→ 반납 검수
→ 대여 완료
```

대여 계약과 상품의 상태를 분리해 관리합니다.

```text
RentalContract
REQUESTED → APPROVED → PICKED_UP → IN_USE
→ RETURNED → INSPECTING → COMPLETED

RentalProduct
AVAILABLE → RESERVED → RENTED → INSPECTION
→ AVAILABLE or MAINTENANCE
```

### Operations

```text
대여 이력
→ 시간 단위 수요 데이터
→ Historical Average / Granite TTM-R3
→ demand_forecast
→ 재고 부족 Rule
→ inventory_recommendation
→ 운영자 Dashboard
→ ACCEPTED / DISMISSED
```

## Architecture

```text
React + TypeScript
Operator Dashboard
        │
        │ REST / JWT
        ▼
Spring Boot
├─ Auth / Access Control
├─ Rental / Condition / Damage / Inspection
└─ Forecast / Recommendation API
        │
        ▼
      MySQL
        ▲
        │
Python Batch
├─ Synthetic Dataset
├─ Historical Average
├─ Granite TTM-R3 Zero-shot
└─ MAE / RMSE Evaluation
```

수요 예측은 사용자 요청마다 계산할 필요가 없다고 판단해 실시간 ML API를 별도로 두지 않았습니다. Python batch가 예측 결과를 MySQL에 저장하고 Spring Boot가 이를 조회해 운영자에게 제공합니다.

## Key Engineering Decisions

### 대여 건별 상태 이력 보존

상품의 현재 상태만 덮어쓰지 않고 대여 건마다 BEFORE / AFTER snapshot을 저장합니다.

```text
RentalContract
├─ BEFORE ConditionRecord
│  ├─ ConditionImage
│  └─ DamageRecord
├─ AFTER ConditionRecord
│  ├─ ConditionImage
│  └─ DamageRecord
└─ ReturnInspection
```

반복 대여에서도 각 계약의 상태·사진·손상 이력을 분리해 확인할 수 있습니다.

### 동시 대여 요청 제어

동일 상품에 여러 사용자가 동시에 대여를 요청하는 상황을 통합 테스트로 재현하고, `PESSIMISTIC_WRITE` 기반 잠금으로 하나의 요청만 성공하도록 처리했습니다.

### 이미지 저장소 분리

ConditionImage에는 저장 메타데이터를 보관하고 `ImageStorage` 인터페이스를 통해 파일 저장 책임을 분리했습니다. 현재는 Local File adapter를 사용하며 이후 object storage로 교체할 수 있습니다.

### JWT 인증과 권한

```text
email / password
→ BCrypt
→ JWT access token
→ Spring Security
→ Member ACTIVE / DB role 재확인
→ USER / OPERATOR 권한
```

JWT에는 Member ID만 subject로 저장하며 요청마다 DB의 현재 상태와 role을 다시 확인합니다.

### Forecast와 운영 Rule 분리

AI 모델은 수요만 예측합니다.

```text
predicted demand
+ available quantity
+ safety stock
→ recommended additional quantity
→ risk level
```

재고 보충 수량과 위험도는 운영 Rule에서 계산하고, 최종 판단은 운영자가 `ACCEPTED / DISMISSED`로 기록합니다.

## Demand Forecast Experiment

Hugging Face에 공개된 IBM Granite TTM-R3를 zero-shot으로 사용하고, 동일한 시간순 holdout에서 Historical Average baseline과 비교했습니다.

실험에는 운영 데이터가 아닌 재현 가능한 합성 대여 이력을 사용했습니다.

| Model | MAE | RMSE |
| --- | ---: | ---: |
| Historical Average | 1.111979 | 1.430918 |
| Granite TTM-R3 Zero-shot | **1.044682** | **1.410518** |

```text
Dataset: synthetic-v1-seed42-720h
Locations: 2
Categories: 2
Series: 4
Rows: 2,880
Holdout: final 24 hours
```

이번 합성 데이터 실행에서는 TTM-R3의 MAE와 RMSE가 모두 낮았습니다. 이 결과는 pipeline 검증을 위한 한 번의 합성 데이터 실행 결과이며, 실제 서비스 수요에 대한 일반화 성능을 의미하지 않습니다.

예측 결과와 실행 이력은 다음 테이블로 보존합니다.

```text
model_run
demand_forecast
inventory_recommendation
```

## Operator Dashboard

운영자는 Dashboard에서 다음 정보를 확인하고 판단을 기록할 수 있습니다.

- 최신 모델 실행 정보
- Historical Average / TTM-R3 MAE·RMSE 비교
- 지점·카테고리별 예측 수요
- OPEN recommendation 위험도 분포
- 추가 배치 권장 수량
- 우선 검토 recommendation
- ACCEPTED / DISMISSED 처리

## Tech Stack

**Backend**  
`Java 21` `Spring Boot` `Spring Data JPA` `Spring Security` `Flyway` `MySQL`

**Frontend**  
`React` `TypeScript` `Vite` `Axios`

**Data / AI**  
`Python` `Pandas` `Granite TTM-R3` `Hugging Face`

**Engineering**  
`Docker Compose` `GitHub Actions`

## Repository

```text
RentalTrack/
├─ backend/
├─ frontend/
│  └─ web/
├─ ai/
│  └─ demand_forecasting/
├─ docs/
├─ compose.yaml
└─ README.md
```

## Local Run

환경변수 예시를 복사합니다.

```bash
cp .env.example .env
```

MySQL과 Backend를 실행합니다.

```bash
docker compose up --build
```

Frontend를 실행합니다.

```bash
cd frontend/web
npm install
npm run dev
```

Vite 개발 서버의 `/api` 요청은 `http://localhost:8080`으로 proxy됩니다.

### Operator Demo

공개 회원가입은 USER role만 생성합니다.

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/auth/me
```

운영자 Dashboard를 확인하려면 로컬 DB에서 해당 계정의 role을 OPERATOR로 변경합니다.

```sql
UPDATE member_account
SET role = 'OPERATOR'
WHERE email = 'operator@example.com';
```

## Validation

GitHub Actions에서 영역별 검증을 분리했습니다.

```text
Backend Validation
→ Java 21 + MySQL 8.4
→ Flyway migration
→ Hibernate ddl-auto=validate
→ Unit / Integration Test

Frontend Validation
→ TypeScript / Vite build

AI Validation
→ Dataset / Baseline / Recommendation test
→ Synthetic baseline smoke test

Demand Forecast Experiment
→ Synthetic history
→ Historical Average / TTM-R3
→ MAE / RMSE
→ DB persistence
→ artifact export
```

## Current Scope

v1에서는 다음을 의도적으로 범위에서 제외했습니다.

- 실서비스 수요 데이터를 이용한 모델 성능 주장
- 합성 데이터 fine-tuning
- 실시간 ML inference server
- AI 기반 손상 책임 판정
- refresh token / token revocation
- S3·MinIO 등 object storage adapter
- 추천 승인 이후 상품 재배치의 자동 실행

현재 목표는 렌탈 도메인의 상태·이력 일관성을 유지하면서, 서비스에서 발생한 데이터를 수요 예측과 운영 의사결정으로 연결하는 전체 흐름을 재현하는 것입니다.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Authentication](docs/AUTHENTICATION.md)
- [AI Architecture](docs/AI-ARCHITECTURE.md)
- [Demand Forecast Experiment](docs/DEMAND-FORECAST-EXPERIMENT.md)
- [Phase 4 Experiment Result](docs/PHASE4-EXPERIMENT-RESULT.md)
- [Operator AI API](docs/OPERATOR-AI-API.md)
- [Operator Dashboard](docs/OPERATOR-DASHBOARD.md)
- [Roadmap](docs/ROADMAP.md)

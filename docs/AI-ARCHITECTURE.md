# RentalTrack AI Architecture

## 1. 역할

RentalTrack의 AI는 손상 판정이나 사용자 개인화 추천이 아니라
지점별 상품 수요를 예측해 운영자의 재고 배치 판단을 지원합니다.

```text
Spring Boot
  └─ rental_contract / rental_product / rental_location
                   │
                   ▼
            Python batch
          ├─ dataset builder
          ├─ historical average
          ├─ TTM-R3 zero-shot
          ├─ evaluation
          └─ replenishment rule
                   │
                   ▼
             MySQL results
          ├─ model_run
          ├─ demand_forecast
          └─ inventory_recommendation
                   │
                   ▼
             Spring Boot API
                   │
                   ▼
           Operator Dashboard
```

실시간 inference API는 두지 않습니다.
수요 예측은 사용자 요청마다 계산할 필요가 없으므로 배치 결과를 DB에 저장하는 구조로 시작합니다.

## 2. 모델 비교

### Historical Average

모든 AI 실험의 기준점입니다.

```text
location
× category
× day_of_week
× hour
→ average rental_count
```

### Granite TTM-R3

Hugging Face에 공개된 `ibm-granite/granite-timeseries-ttm-r3`를
zero-shot 방식으로 연결합니다.

초기 비교:

```text
Historical Average
vs
TTM-R3 Zero-shot
```

합성 데이터에 대한 fine-tuning은 초기 범위에서 제외합니다.
합성 패턴을 다시 학습해 성능이 높아지는 결과를 실제 서비스 일반화 성능처럼 해석하는 것을 피하기 위해서입니다.

## 3. Forecast grain

```text
RentalLocation
× ProductCategory
× hourly time bucket
```

기본 horizon:

```text
context    512 hours
forecast    24 hours
```

모델에 따라 필요한 context length는 실험 과정에서 조정할 수 있으며,
DB schema는 모델별 context length에 의존하지 않습니다.

## 4. Synthetic data

서비스 초기에는 충분한 운영 이력이 없으므로 재현 가능한 합성 대여 이력을 사용합니다.

```text
is_synthetic = true
```

를 명시적으로 포함하며 문서와 결과에서 운영 데이터와 구분합니다.

합성 데이터의 목적은 다음입니다.

- 데이터 파이프라인 검증
- Baseline 동작 검증
- TTM 연결 검증
- 평가 및 결과 저장 흐름 검증

실제 수요 수준을 주장하는 근거로 사용하지 않습니다.

## 5. Evaluation

시계열 순서를 보존합니다.

```text
Train ───────────────────┐
                         │ chronological boundary
                         └──── Test
```

초기 지표:

- MAE
- RMSE

TTM-R3가 baseline을 이기지 못하면 그 결과도 그대로 기록합니다.

## 6. Forecast와 Recommendation 분리

예측 모델과 운영 규칙은 별도 책임입니다.

```text
Model
→ predicted demand

Rule
→ available quantity
 + safety stock
 + predicted demand
→ risk level
→ recommended additional quantity

Operator
→ ACCEPTED / DISMISSED
```

따라서 `recommended_additional_quantity`를 AI 모델의 직접 출력으로 설명하지 않습니다.

## 7. Phase 4 DB

### model_run

어떤 데이터와 모델 실행에서 예측이 생성됐는지 추적합니다.

### demand_forecast

지점·카테고리별 forecast window의 예측 수요를 보존합니다.

### inventory_recommendation

예측 수요와 현재 가용 수량을 결합한 운영 추천을 저장합니다.

이 세 테이블은 model output의 provenance와 운영 판단 이력을 분리하기 위한 구조입니다.

# RentalTrack Demand Forecasting

RentalTrack Phase 4의 수요 예측 모듈입니다.

## 목표

`RentalLocation × ProductCategory × 1시간` 단위의 대여 수요를 예측하고,
예측 결과를 현재 가용 수량과 결합해 운영자용 재고 보충 추천으로 변환합니다.

AI가 재고 이동을 자동 결정하지 않습니다.

```text
Historical rental history
→ Hourly series
→ Forecast
→ Stock shortage rule
→ Operator recommendation
```

## 모델

### Baseline

`HistoricalAverageForecaster`

같은 지점·상품 카테고리의 동일 요일·시간대 평균을 사용합니다.
해당 슬롯 이력이 없으면 series 평균, 그마저 없으면 전체 평균으로 fallback합니다.

### Main model

`ibm-granite/granite-timeseries-ttm-r3`

초기 구현은 zero-shot forecasting으로 시작합니다.

현재 기본 설정:

```text
frequency         1h
context_length    512
prediction_length 24
target            rental_count
```

TTM 의존성과 모델 weight는 기본 CI에서 설치하거나 내려받지 않습니다.
TTM 실행이 필요한 환경에서만 optional dependency를 설치합니다.

```bash
pip install -e ".[ttm]"
```

## Dataset contract

필수 컬럼:

```text
timestamp
location_id
category_id
rental_count
available_quantity
is_synthetic
```

`is_synthetic=true`인 행은 데모/파이프라인 검증용 합성 데이터임을 의미합니다.

## Demo data

```bash
cd ai/demand_forecasting
pip install -e ".[test]"

python -m rentaltrack_ai.data.synthetic \
  --output artifacts/synthetic_rental_history.csv
```

합성 데이터는 운영 수요가 아닙니다. 시간·요일·지점·카테고리 패턴을 넣어
forecast pipeline을 재현하기 위한 데이터입니다.

## Baseline forecast

```bash
python -m rentaltrack_ai.pipeline \
  --input artifacts/synthetic_rental_history.csv \
  --model baseline \
  --output artifacts/baseline_forecast.csv
```

## TTM-R3 zero-shot

TTM optional dependency를 설치한 뒤:

```bash
python -m rentaltrack_ai.pipeline \
  --input artifacts/synthetic_rental_history.csv \
  --model ttm \
  --output artifacts/ttm_forecast.csv
```

## Evaluation policy

랜덤 split을 사용하지 않습니다.

```text
past ───────────────────────────────→ future

train period
                         test period
```

초기 평가 지표:

- MAE
- RMSE

모델 채택 기준은 TTM이라는 이유 자체가 아니라,
시간순 holdout에서 Historical Average baseline과 비교한 결과입니다.

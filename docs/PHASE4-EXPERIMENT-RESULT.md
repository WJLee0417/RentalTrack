# Phase 4 Experiment Result

검증 커밋:

```text
f4bfebc086ab9ca91ea38b83b52c79dc854b467a
fix: expand migration file variable in experiment workflow
```

GitHub Actions run:

```text
Demand Forecast Experiment #2
run id: 37298463071
result: SUCCESS
```

## Dataset

```text
dataset_version: synthetic-v1-seed42-720h
locations: 2
categories: 2
series: 4
hours per series: 720
rows: 2,880
holdout: final 24 hours
```

이 데이터는 서비스 운영 데이터가 아니라 pipeline 검증을 위해 생성한 합성 데이터입니다.

## 동일 holdout 비교

| Model | MAE | RMSE |
| --- | ---: | ---: |
| Historical Average | 1.111979 | 1.430918 |
| Granite TTM-R3 zero-shot | 1.044682 | 1.410518 |

이번 합성 데이터 실행에서는 TTM-R3 zero-shot이 MAE와 RMSE 모두 낮아 재고 추천에 사용할 모델로 선택되었습니다.

이 결과는 합성 데이터 한 번의 실행 결과이며, 실제 서비스 일반화 성능이나 우월성을 의미하지 않습니다.

## Persistence

MySQL service에 다음 결과가 저장되는 것을 확인했습니다.

```text
model_run
- baseline run: id 1 / SUCCEEDED
- TTM-R3 run: id 2 / SUCCEEDED

demand_forecast
- baseline: 4 series
- TTM-R3: 4 series

inventory_recommendation
- selected TTM-R3 forecast 기준 4건 생성
```

TTM-R3의 다음 24시간 총 예측 수요와 재고 추천:

| Location | Category | Predicted Demand | Available | Additional | Risk |
| ---: | ---: | ---: | ---: | ---: | --- |
| 1 | 1 | 31.2163 | 13 | 20 | HIGH |
| 1 | 2 | 40.5497 | 11 | 31 | HIGH |
| 2 | 1 | 41.1240 | 10 | 33 | HIGH |
| 2 | 2 | 56.4782 | 10 | 48 | HIGH |

추천 수량은 AI의 직접 출력이 아니라 다음 규칙으로 계산했습니다.

```text
ceil(predicted_demand)
+ safety_stock
- available_quantity
```

음수이면 0으로 보정합니다.

## CI artifact

GitHub Actions artifact `demand-forecast-experiment`에 다음 파일을 저장했습니다.

- experiment_summary.json / md
- metrics.csv
- baseline_holdout.csv
- ttm_holdout.csv
- baseline_future.csv
- ttm_future.csv
- recommendations.csv
- model_run.tsv
- demand_forecast.tsv
- inventory_recommendation.tsv

GitHub-hosted MySQL은 workflow 종료 후 사라지므로 DB 결과를 TSV로 함께 export해 검증 근거를 남깁니다.

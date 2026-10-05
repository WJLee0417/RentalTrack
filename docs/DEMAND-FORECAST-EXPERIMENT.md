# Demand Forecast Experiment

GitHub Actions에서 RentalTrack Phase 4의 전체 예측 파이프라인을 재현합니다.

## Pipeline

Synthetic history
→ chronological 24-hour holdout
→ Historical Average
→ Granite TTM-R3 zero-shot
→ same holdout MAE / RMSE
→ model_run persistence
→ next 24-hour demand_forecast persistence
→ winner selection by MAE, then RMSE
→ inventory_recommendation persistence
→ GitHub Actions artifact

## CI environment

- GitHub-hosted Ubuntu runner
- Python 3.11
- MySQL 8.4 service
- granite-tsfm
- Hugging Face model: ibm-granite/granite-timeseries-ttm-r3
- CPU inference

## Dataset

The workflow creates reproducible synthetic demand history with seed 42.

Current CI experiment size:

- 2 demo rental locations
- 2 demo product categories
- 4 hourly series
- 720 hours per series
- last 24 hours reserved as chronological holdout

The dataset is synthetic and must not be described as observed customer demand.

## Persistence

The experiment writes to the same Phase 4 schema used by the service:

- model_run
- demand_forecast
- inventory_recommendation

The workflow exports these tables to TSV files before the ephemeral MySQL service is destroyed.

## Artifact

GitHub Actions uploads:

- experiment_summary.md
- experiment_summary.json
- metrics.csv
- baseline_holdout.csv
- ttm_holdout.csv
- baseline_future.csv
- ttm_future.csv
- recommendations.csv
- model_run.tsv
- demand_forecast.tsv
- inventory_recommendation.tsv

from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
from pathlib import Path

import pandas as pd

from rentaltrack_ai.config import DEFAULT_CONFIG
from rentaltrack_ai.data.synthetic import generate_synthetic_history
from rentaltrack_ai.evaluation.metrics import mae, rmse
from rentaltrack_ai.evaluation.split import chronological_split
from rentaltrack_ai.models.historical_average import HistoricalAverageForecaster, build_future_index
from rentaltrack_ai.models.ttm_r3 import TtmR3Forecaster
from rentaltrack_ai.persistence.mysql_repository import DatabaseConfig, ForecastRepository
from rentaltrack_ai.recommendation.replenishment import recommend_replenishment


def utcnow_naive() -> datetime:
    return datetime.now(timezone.utc).replace(tzinfo=None)


def score_forecast(test: pd.DataFrame, forecast: pd.DataFrame):
    actual = test[
        ["timestamp", "location_id", "category_id", "rental_count"]
    ].rename(columns={"rental_count": "actual_demand"})
    scored = actual.merge(
        forecast,
        on=["timestamp", "location_id", "category_id"],
        how="inner",
        validate="one_to_one",
    )
    if len(scored) != len(actual):
        raise RuntimeError(
            f"forecast alignment failed: expected {len(actual)} rows, got {len(scored)}"
        )
    return (
        mae(scored["actual_demand"], scored["predicted_demand"]),
        rmse(scored["actual_demand"], scored["predicted_demand"]),
        scored,
    )


def aggregate_horizon(forecast: pd.DataFrame) -> pd.DataFrame:
    frequency = pd.Timedelta(DEFAULT_CONFIG.frequency)
    grouped = (
        forecast.groupby(["location_id", "category_id"], as_index=False)
        .agg(
            forecast_start_at=("timestamp", "min"),
            last_forecast_at=("timestamp", "max"),
            predicted_demand=("predicted_demand", "sum"),
        )
    )
    grouped["forecast_end_at"] = grouped["last_forecast_at"] + frequency
    return grouped.drop(columns=["last_forecast_at"])


def latest_inventory(history: pd.DataFrame) -> pd.DataFrame:
    return (
        history.sort_values("timestamp")
        .groupby(["location_id", "category_id"], as_index=False)
        .tail(1)[["location_id", "category_id", "available_quantity"]]
        .reset_index(drop=True)
    )


def build_recommendation_frame(
    horizon: pd.DataFrame,
    inventory: pd.DataFrame,
    safety_stock: int = 1,
) -> pd.DataFrame:
    merged = horizon.merge(
        inventory,
        on=["location_id", "category_id"],
        how="left",
        validate="one_to_one",
    )
    if merged["available_quantity"].isna().any():
        raise RuntimeError("inventory missing for one or more forecast series")

    rows = []
    for row in merged.itertuples(index=False):
        decision = recommend_replenishment(
            predicted_demand=float(row.predicted_demand),
            available_quantity=int(row.available_quantity),
            safety_stock=safety_stock,
        )
        rows.append(
            {
                "location_id": int(row.location_id),
                "category_id": int(row.category_id),
                "forecast_start_at": row.forecast_start_at,
                "forecast_end_at": row.forecast_end_at,
                **decision,
            }
        )
    return pd.DataFrame(rows)


def save_artifacts(
    output_dir: Path,
    history: pd.DataFrame,
    baseline_scored: pd.DataFrame,
    ttm_scored: pd.DataFrame,
    baseline_horizon: pd.DataFrame,
    ttm_horizon: pd.DataFrame,
    recommendations: pd.DataFrame,
    summary: dict,
) -> None:
    output_dir.mkdir(parents=True, exist_ok=True)
    history.to_csv(output_dir / "synthetic_history.csv", index=False)
    baseline_scored.to_csv(output_dir / "baseline_holdout.csv", index=False)
    ttm_scored.to_csv(output_dir / "ttm_holdout.csv", index=False)
    baseline_horizon.to_csv(output_dir / "baseline_future.csv", index=False)
    ttm_horizon.to_csv(output_dir / "ttm_future.csv", index=False)
    recommendations.to_csv(output_dir / "recommendations.csv", index=False)

    pd.DataFrame(
        [
            {
                "model": "historical_average",
                "mae": summary["baseline"]["mae"],
                "rmse": summary["baseline"]["rmse"],
            },
            {
                "model": "granite_ttm_r3",
                "mae": summary["ttm_r3_zero_shot"]["mae"],
                "rmse": summary["ttm_r3_zero_shot"]["rmse"],
            },
        ]
    ).to_csv(output_dir / "metrics.csv", index=False)

    (output_dir / "experiment_summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    markdown = (
        "# RentalTrack Demand Forecast Experiment\n\n"
        f"- Dataset: {summary['dataset_version']}\n"
        f"- Synthetic rows: {summary['synthetic_rows']:,}\n"
        f"- Series: {summary['series_count']}\n"
        f"- Holdout: {summary['holdout_hours']} hours\n"
        f"- Historical Average MAE: {summary['baseline']['mae']:.4f}\n"
        f"- Historical Average RMSE: {summary['baseline']['rmse']:.4f}\n"
        f"- TTM-R3 Zero-shot MAE: {summary['ttm_r3_zero_shot']['mae']:.4f}\n"
        f"- TTM-R3 Zero-shot RMSE: {summary['ttm_r3_zero_shot']['rmse']:.4f}\n"
        f"- Selected model: {summary['winner']}\n"
        f"- Recommendations: {summary['recommendation_count']}\n\n"
        "> Synthetic data is used only to validate the end-to-end pipeline. "
        "These metrics are not production-demand performance claims.\n"
    )
    (output_dir / "experiment_summary.md").write_text(markdown, encoding="utf-8")


def run(output_dir: Path, periods: int, seed: int) -> dict:
    repository = ForecastRepository(DatabaseConfig.from_env())
    location_ids, category_ids = repository.seed_demo_dimensions()

    dataset_version = f"synthetic-v1-seed{seed}-{periods}h"
    repository.clear_demo_results(dataset_version)

    history = generate_synthetic_history(
        start="2026-01-01",
        periods=periods,
        seed=seed,
        locations=location_ids,
        categories=category_ids,
    )
    train, test = chronological_split(
        history,
        test_hours=DEFAULT_CONFIG.prediction_length,
    )

    baseline_started = utcnow_naive()
    baseline_model = HistoricalAverageForecaster().fit(train)
    baseline_holdout_index = build_future_index(
        train,
        prediction_length=DEFAULT_CONFIG.prediction_length,
        frequency=DEFAULT_CONFIG.frequency,
    )
    baseline_holdout = baseline_model.predict(baseline_holdout_index)
    baseline_mae, baseline_rmse, baseline_scored = score_forecast(
        test,
        baseline_holdout,
    )
    baseline_completed = utcnow_naive()

    ttm_started = utcnow_naive()
    ttm_model = TtmR3Forecaster(DEFAULT_CONFIG).load()
    ttm_holdout = ttm_model.predict(train)
    ttm_mae, ttm_rmse, ttm_scored = score_forecast(test, ttm_holdout)
    ttm_completed = utcnow_naive()

    baseline_full = HistoricalAverageForecaster().fit(history)
    baseline_future = baseline_full.predict(
        build_future_index(
            history,
            prediction_length=DEFAULT_CONFIG.prediction_length,
            frequency=DEFAULT_CONFIG.frequency,
        )
    )
    ttm_future = ttm_model.predict(history)

    baseline_horizon = aggregate_horizon(baseline_future)
    ttm_horizon = aggregate_horizon(ttm_future)

    baseline_run_id = repository.save_model_run(
        model_name="historical_average",
        model_version="weekday-hour-v1",
        run_type="BASELINE",
        dataset_version=dataset_version,
        train_from=train["timestamp"].min().to_pydatetime(),
        train_to=train["timestamp"].max().to_pydatetime(),
        evaluation_from=test["timestamp"].min().to_pydatetime(),
        evaluation_to=test["timestamp"].max().to_pydatetime(),
        mae=baseline_mae,
        rmse=baseline_rmse,
        started_at=baseline_started,
        completed_at=baseline_completed,
    )
    ttm_run_id = repository.save_model_run(
        model_name="granite_ttm_r3",
        model_version=DEFAULT_CONFIG.model_path,
        run_type="TTM_ZERO_SHOT",
        dataset_version=dataset_version,
        train_from=train["timestamp"].min().to_pydatetime(),
        train_to=train["timestamp"].max().to_pydatetime(),
        evaluation_from=test["timestamp"].min().to_pydatetime(),
        evaluation_to=test["timestamp"].max().to_pydatetime(),
        mae=ttm_mae,
        rmse=ttm_rmse,
        started_at=ttm_started,
        completed_at=ttm_completed,
    )

    generated_at = utcnow_naive()
    baseline_ids = repository.save_forecasts(
        baseline_run_id,
        baseline_horizon,
        generated_at,
    )
    ttm_ids = repository.save_forecasts(
        ttm_run_id,
        ttm_horizon,
        generated_at,
    )

    if (ttm_mae, ttm_rmse) < (baseline_mae, baseline_rmse):
        winner = "granite_ttm_r3"
        winner_horizon = ttm_horizon
        winner_ids = ttm_ids
    else:
        winner = "historical_average"
        winner_horizon = baseline_horizon
        winner_ids = baseline_ids

    recommendations = build_recommendation_frame(
        winner_horizon,
        latest_inventory(history),
        safety_stock=1,
    )
    repository.save_recommendations(
        winner_ids,
        recommendations,
        generated_at,
        winner,
    )

    summary = {
        "dataset_version": dataset_version,
        "synthetic_rows": int(len(history)),
        "series_count": int(
            history[["location_id", "category_id"]].drop_duplicates().shape[0]
        ),
        "holdout_hours": DEFAULT_CONFIG.prediction_length,
        "baseline": {"mae": baseline_mae, "rmse": baseline_rmse},
        "ttm_r3_zero_shot": {"mae": ttm_mae, "rmse": ttm_rmse},
        "winner": winner,
        "recommendation_count": int(len(recommendations)),
        "model_run_ids": {
            "baseline": baseline_run_id,
            "ttm_r3_zero_shot": ttm_run_id,
        },
    }

    save_artifacts(
        output_dir,
        history,
        baseline_scored,
        ttm_scored,
        baseline_horizon,
        ttm_horizon,
        recommendations,
        summary,
    )
    return summary


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default="artifacts/experiment")
    parser.add_argument("--periods", type=int, default=24 * 60)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    result = run(Path(args.output_dir), periods=args.periods, seed=args.seed)
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()

from __future__ import annotations

import argparse
from pathlib import Path

import pandas as pd

from rentaltrack_ai.config import DEFAULT_CONFIG
from rentaltrack_ai.data.dataset import validate_history
from rentaltrack_ai.models.historical_average import (
    HistoricalAverageForecaster,
    build_future_index,
)
from rentaltrack_ai.models.ttm_r3 import TtmR3Forecaster


def forecast(
    history: pd.DataFrame,
    model: str = "baseline",
) -> pd.DataFrame:
    data = validate_history(history)

    if model == "baseline":
        forecaster = HistoricalAverageForecaster().fit(data)
        future = build_future_index(
            data,
            prediction_length=DEFAULT_CONFIG.prediction_length,
            frequency=DEFAULT_CONFIG.frequency,
        )
        return forecaster.predict(future)

    if model == "ttm":
        return TtmR3Forecaster(DEFAULT_CONFIG).load().predict(data)

    raise ValueError("model must be one of: baseline, ttm")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True)
    parser.add_argument("--model", choices=["baseline", "ttm"], default="baseline")
    parser.add_argument("--output", default="artifacts/forecast.csv")
    args = parser.parse_args()

    history = pd.read_csv(args.input)
    result = forecast(history, model=args.model)

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    result.to_csv(output, index=False)
    print(f"wrote {len(result):,} forecast rows to {output}")


if __name__ == "__main__":
    main()

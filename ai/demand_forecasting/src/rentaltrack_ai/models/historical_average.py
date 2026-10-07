from __future__ import annotations

import pandas as pd

from rentaltrack_ai.data.dataset import add_time_features, validate_history


class HistoricalAverageForecaster:
    """Baseline using same-series weekday/hour averages with safe fallbacks."""

    def __init__(self) -> None:
        self._series_slot_mean: pd.Series | None = None
        self._series_mean: pd.Series | None = None
        self._global_mean: float | None = None

    def fit(self, frame: pd.DataFrame) -> "HistoricalAverageForecaster":
        data = add_time_features(frame)

        self._series_slot_mean = data.groupby(
            ["location_id", "category_id", "day_of_week", "hour"]
        )["rental_count"].mean()

        self._series_mean = data.groupby(
            ["location_id", "category_id"]
        )["rental_count"].mean()

        self._global_mean = float(data["rental_count"].mean())
        return self

    def predict(self, future_index: pd.DataFrame) -> pd.DataFrame:
        if (
            self._series_slot_mean is None
            or self._series_mean is None
            or self._global_mean is None
        ):
            raise RuntimeError("fit() must be called before predict()")

        required = {"timestamp", "location_id", "category_id"}
        if not required.issubset(future_index.columns):
            raise ValueError(f"future_index requires columns: {sorted(required)}")

        result = future_index.copy()
        result["timestamp"] = pd.to_datetime(result["timestamp"])
        result["day_of_week"] = result["timestamp"].dt.dayofweek
        result["hour"] = result["timestamp"].dt.hour

        predictions: list[float] = []
        for row in result.itertuples(index=False):
            slot_key = (
                row.location_id,
                row.category_id,
                row.day_of_week,
                row.hour,
            )
            series_key = (row.location_id, row.category_id)

            slot_value = self._series_slot_mean.get(slot_key)
            if pd.notna(slot_value):
                prediction = float(slot_value)
            else:
                series_value = self._series_mean.get(series_key)
                prediction = (
                    float(series_value)
                    if pd.notna(series_value)
                    else self._global_mean
                )

            predictions.append(max(0.0, prediction))

        return result[["timestamp", "location_id", "category_id"]].assign(
            predicted_demand=predictions,
            model_name="historical_average",
        )


def build_future_index(
    history: pd.DataFrame,
    prediction_length: int,
    frequency: str = "1h",
) -> pd.DataFrame:
    data = validate_history(history)
    last_timestamp = data["timestamp"].max()
    future_timestamps = pd.date_range(
        start=last_timestamp + pd.Timedelta(frequency),
        periods=prediction_length,
        freq=frequency,
    )

    series = data[["location_id", "category_id"]].drop_duplicates()

    rows = [
        {
            "timestamp": timestamp,
            "location_id": series_row.location_id,
            "category_id": series_row.category_id,
        }
        for series_row in series.itertuples(index=False)
        for timestamp in future_timestamps
    ]
    return pd.DataFrame(rows)

from __future__ import annotations

import pandas as pd

from rentaltrack_ai.data.schema import REQUIRED_COLUMNS


def validate_history(frame: pd.DataFrame) -> pd.DataFrame:
    missing = [column for column in REQUIRED_COLUMNS if column not in frame.columns]
    if missing:
        raise ValueError(f"missing required columns: {missing}")

    data = frame.copy()
    data["timestamp"] = pd.to_datetime(data["timestamp"])

    if data[["location_id", "category_id", "timestamp"]].duplicated().any():
        raise ValueError("duplicate location/category/timestamp rows are not allowed")

    if (data["rental_count"] < 0).any():
        raise ValueError("rental_count must be non-negative")

    if (data["available_quantity"] < 0).any():
        raise ValueError("available_quantity must be non-negative")

    return data.sort_values(
        ["location_id", "category_id", "timestamp"]
    ).reset_index(drop=True)


def add_time_features(frame: pd.DataFrame) -> pd.DataFrame:
    data = validate_history(frame)
    data["hour"] = data["timestamp"].dt.hour
    data["day_of_week"] = data["timestamp"].dt.dayofweek
    data["is_weekend"] = data["day_of_week"].ge(5).astype(int)
    return data

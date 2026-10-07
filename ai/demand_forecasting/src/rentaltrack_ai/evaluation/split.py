from __future__ import annotations

import pandas as pd

from rentaltrack_ai.data.dataset import validate_history


def chronological_split(
    frame: pd.DataFrame,
    test_hours: int,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    if test_hours < 1:
        raise ValueError("test_hours must be positive")

    data = validate_history(frame)
    cutoff = data["timestamp"].max() - pd.Timedelta(hours=test_hours - 1)

    train = data[data["timestamp"] < cutoff].copy()
    test = data[data["timestamp"] >= cutoff].copy()

    if train.empty or test.empty:
        raise ValueError("split produced an empty train or test set")

    return train, test

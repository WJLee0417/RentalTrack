from __future__ import annotations

import numpy as np


def mae(actual, predicted) -> float:
    actual_values = np.asarray(actual, dtype=float)
    predicted_values = np.asarray(predicted, dtype=float)
    return float(np.mean(np.abs(actual_values - predicted_values)))


def rmse(actual, predicted) -> float:
    actual_values = np.asarray(actual, dtype=float)
    predicted_values = np.asarray(predicted, dtype=float)
    return float(np.sqrt(np.mean((actual_values - predicted_values) ** 2)))

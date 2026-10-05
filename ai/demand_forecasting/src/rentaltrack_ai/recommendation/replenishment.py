from __future__ import annotations

import math

import pandas as pd


def recommend_replenishment(
    predicted_demand: float,
    available_quantity: int,
    safety_stock: int = 1,
) -> dict:
    if predicted_demand < 0:
        raise ValueError("predicted_demand must be non-negative")
    if available_quantity < 0 or safety_stock < 0:
        raise ValueError("stock quantities must be non-negative")

    required = math.ceil(predicted_demand) + safety_stock
    additional = max(0, required - available_quantity)

    coverage = available_quantity - math.ceil(predicted_demand)
    if additional == 0:
        risk = "LOW"
    elif coverage >= -1:
        risk = "MEDIUM"
    else:
        risk = "HIGH"

    return {
        "predicted_demand": float(predicted_demand),
        "available_quantity": int(available_quantity),
        "safety_stock": int(safety_stock),
        "recommended_additional_quantity": int(additional),
        "risk_level": risk,
    }


def build_recommendations(
    forecasts: pd.DataFrame,
    inventory: pd.DataFrame,
    safety_stock: int = 1,
) -> pd.DataFrame:
    required_forecast = {
        "timestamp",
        "location_id",
        "category_id",
        "predicted_demand",
    }
    required_inventory = {
        "location_id",
        "category_id",
        "available_quantity",
    }

    if not required_forecast.issubset(forecasts.columns):
        raise ValueError("forecast columns are incomplete")
    if not required_inventory.issubset(inventory.columns):
        raise ValueError("inventory columns are incomplete")

    horizon = forecasts.groupby(
        ["location_id", "category_id"],
        as_index=False,
    ).agg(
        forecast_start_at=("timestamp", "min"),
        forecast_end_at=("timestamp", "max"),
        predicted_demand=("predicted_demand", "sum"),
    )

    merged = horizon.merge(
        inventory,
        on=["location_id", "category_id"],
        how="left",
        validate="one_to_one",
    )

    if merged["available_quantity"].isna().any():
        raise ValueError("inventory is missing for one or more forecast series")

    decisions = merged.apply(
        lambda row: recommend_replenishment(
            row["predicted_demand"],
            int(row["available_quantity"]),
            safety_stock,
        ),
        axis=1,
    )

    decision_frame = pd.DataFrame(decisions.tolist())
    return pd.concat(
        [merged.drop(columns=["predicted_demand", "available_quantity"]), decision_frame],
        axis=1,
    )

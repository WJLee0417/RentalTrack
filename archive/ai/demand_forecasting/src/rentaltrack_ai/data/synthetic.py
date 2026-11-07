from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import pandas as pd


def generate_synthetic_history(
    start: str = "2026-01-01",
    periods: int = 24 * 120,
    seed: int = 42,
    locations: tuple[int, ...] = (1, 2, 3),
    categories: tuple[int, ...] = (1, 2, 3, 4),
) -> pd.DataFrame:
    """Generate reproducible demo demand data.

    This is intentionally synthetic and must never be described as observed
    customer demand. Patterns are injected only to exercise the forecasting
    pipeline end to end.
    """
    rng = np.random.default_rng(seed)
    timestamps = pd.date_range(start=start, periods=periods, freq="h")

    rows: list[dict] = []
    for location_id in locations:
        for category_id in categories:
            base = 0.6 + 0.25 * location_id + 0.15 * category_id

            for timestamp in timestamps:
                hour = timestamp.hour
                weekday = timestamp.dayofweek
                is_weekend = weekday >= 5

                commute_peak = 1.8 if hour in {8, 9, 17, 18, 19} else 0.0
                lunch_peak = 0.7 if hour in {12, 13} else 0.0
                weekend_effect = 0.9 if is_weekend else 0.0

                location_effect = 0.35 * (location_id - 1)
                category_effect = 0.25 * (category_id - 1)

                expected = max(
                    0.05,
                    base
                    + commute_peak
                    + lunch_peak
                    + weekend_effect
                    + location_effect
                    + category_effect,
                )

                rental_count = int(rng.poisson(expected))
                nominal_stock = 10 + category_id * 2
                available_quantity = max(
                    0,
                    nominal_stock - int(round(expected)) + int(rng.integers(-2, 3)),
                )

                rows.append(
                    {
                        "timestamp": timestamp,
                        "location_id": location_id,
                        "category_id": category_id,
                        "rental_count": rental_count,
                        "available_quantity": available_quantity,
                        "is_synthetic": True,
                    }
                )

    return pd.DataFrame(rows).sort_values(
        ["location_id", "category_id", "timestamp"]
    ).reset_index(drop=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="artifacts/synthetic_rental_history.csv")
    parser.add_argument("--periods", type=int, default=24 * 120)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)

    frame = generate_synthetic_history(periods=args.periods, seed=args.seed)
    frame.to_csv(output, index=False)
    print(f"wrote {len(frame):,} synthetic rows to {output}")


if __name__ == "__main__":
    main()

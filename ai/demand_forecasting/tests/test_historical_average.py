import pandas as pd

from rentaltrack_ai.models.historical_average import (
    HistoricalAverageForecaster,
    build_future_index,
)


def test_historical_average_prefers_same_weekday_and_hour():
    timestamps = pd.date_range("2026-01-05 08:00:00", periods=24 * 14, freq="h")
    frame = pd.DataFrame(
        {
            "timestamp": timestamps,
            "location_id": 1,
            "category_id": 1,
            "rental_count": [
                10 if ts.dayofweek == 0 and ts.hour == 8 else 1
                for ts in timestamps
            ],
            "available_quantity": 5,
            "is_synthetic": True,
        }
    )

    model = HistoricalAverageForecaster().fit(frame)
    future = build_future_index(frame, prediction_length=24 * 7)
    forecast = model.predict(future)

    monday_8 = forecast[
        (forecast["timestamp"].dt.dayofweek == 0)
        & (forecast["timestamp"].dt.hour == 8)
    ]

    assert not monday_8.empty
    assert (monday_8["predicted_demand"] == 10).all()

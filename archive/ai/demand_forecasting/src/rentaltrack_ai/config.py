from dataclasses import dataclass


@dataclass(frozen=True)
class ForecastConfig:
    frequency: str = "1h"
    context_length: int = 512
    prediction_length: int = 24
    model_path: str = "ibm-granite/granite-timeseries-ttm-r3"
    target_column: str = "rental_count"
    timestamp_column: str = "timestamp"
    series_columns: tuple[str, str] = ("location_id", "category_id")


DEFAULT_CONFIG = ForecastConfig()

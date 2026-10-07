from __future__ import annotations

import numpy as np
import pandas as pd

from rentaltrack_ai.config import ForecastConfig
from rentaltrack_ai.data.dataset import validate_history


class TtmR3Forecaster:
    """Zero-shot Granite TTM-R3 adapter."""

    def __init__(self, config: ForecastConfig) -> None:
        self.config = config
        self._model = None
        self._torch = None

    def load(self) -> "TtmR3Forecaster":
        try:
            import torch
            from tsfm_public.toolkit.get_model import get_model
        except ImportError as exception:
            raise RuntimeError(
                "TTM dependencies are not installed. "
                "Install with: pip install -e '.[ttm]'"
            ) from exception

        self._torch = torch
        self._model = get_model(
            self.config.model_path,
            context_length=self.config.context_length,
            prediction_length=self.config.prediction_length,
        )
        self._model.eval()
        return self

    def predict(self, history: pd.DataFrame) -> pd.DataFrame:
        if self._model is None or self._torch is None:
            raise RuntimeError("load() must be called before predict()")

        data = validate_history(history)
        forecasts: list[pd.DataFrame] = []

        for (location_id, category_id), series in data.groupby(
            ["location_id", "category_id"],
            sort=True,
        ):
            ordered = series.sort_values("timestamp")
            if len(ordered) < self.config.context_length:
                raise ValueError(
                    "TTM-R3 requires at least "
                    f"{self.config.context_length} observations per series"
                )

            context = (
                ordered[self.config.target_column]
                .tail(self.config.context_length)
                .to_numpy(dtype=np.float32)
            )

            past_values = self._torch.tensor(
                context,
                dtype=self._torch.float32,
            ).reshape(
                1,
                self.config.context_length,
                1,
            )

            with self._torch.no_grad():
                output = self._model(past_values=past_values)

            predicted = (
                output.prediction_outputs[0, :, 0]
                .detach()
                .cpu()
                .numpy()[: self.config.prediction_length]
            )
            predicted = np.maximum(predicted, 0.0)

            start = ordered["timestamp"].max() + pd.Timedelta(
                self.config.frequency
            )
            timestamps = pd.date_range(
                start=start,
                periods=len(predicted),
                freq=self.config.frequency,
            )

            forecasts.append(
                pd.DataFrame(
                    {
                        "timestamp": timestamps,
                        "location_id": location_id,
                        "category_id": category_id,
                        "predicted_demand": predicted,
                        "model_name": "granite_ttm_r3_zero_shot",
                    }
                )
            )

        return pd.concat(forecasts, ignore_index=True)

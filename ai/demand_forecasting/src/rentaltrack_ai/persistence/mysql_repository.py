from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
import os

import mysql.connector
import pandas as pd


@dataclass(frozen=True)
class DatabaseConfig:
    host: str
    port: int
    database: str
    username: str
    password: str

    @classmethod
    def from_env(cls) -> "DatabaseConfig":
        return cls(
            host=os.getenv("AI_DB_HOST", "127.0.0.1"),
            port=int(os.getenv("AI_DB_PORT", "3306")),
            database=os.getenv("AI_DB_NAME", "rentaltrack_test"),
            username=os.getenv("AI_DB_USERNAME", "rentaltrack"),
            password=os.getenv("AI_DB_PASSWORD", "rentaltrack"),
        )


class ForecastRepository:
    def __init__(self, config: DatabaseConfig) -> None:
        self.config = config

    def connect(self):
        return mysql.connector.connect(
            host=self.config.host,
            port=self.config.port,
            database=self.config.database,
            user=self.config.username,
            password=self.config.password,
            autocommit=False,
        )

    def seed_demo_dimensions(self) -> tuple[tuple[int, ...], tuple[int, ...]]:
        locations = [
            ("AI_DEMO_STATION_A", "AI Demo Station A", "Synthetic demo location A", 37.5000000, 127.0000000),
            ("AI_DEMO_STATION_B", "AI Demo Station B", "Synthetic demo location B", 37.5100000, 127.0100000),
        ]
        categories = [
            ("AI_POWER_BANK", "Demo Power Bank", "Synthetic demand experiment category"),
            ("AI_PORTABLE_FAN", "Demo Portable Fan", "Synthetic demand experiment category"),
        ]

        with self.connect() as connection:
            cursor = connection.cursor()

            for row in locations:
                cursor.execute(
                    """
                    INSERT INTO rental_location
                        (code, name, address, latitude, longitude, active)
                    VALUES (%s, %s, %s, %s, %s, TRUE)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        address = VALUES(address),
                        latitude = VALUES(latitude),
                        longitude = VALUES(longitude),
                        active = TRUE
                    """,
                    row,
                )

            for row in categories:
                cursor.execute(
                    """
                    INSERT INTO product_category
                        (code, name, description, active)
                    VALUES (%s, %s, %s, TRUE)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        description = VALUES(description),
                        active = TRUE
                    """,
                    row,
                )

            connection.commit()

            location_ids = []
            for code, *_ in locations:
                cursor.execute(
                    "SELECT id FROM rental_location WHERE code = %s",
                    (code,),
                )
                location_ids.append(int(cursor.fetchone()[0]))

            category_ids = []
            for code, *_ in categories:
                cursor.execute(
                    "SELECT id FROM product_category WHERE code = %s",
                    (code,),
                )
                category_ids.append(int(cursor.fetchone()[0]))

            cursor.close()

        return tuple(location_ids), tuple(category_ids)

    def clear_demo_results(self, dataset_version: str) -> None:
        with self.connect() as connection:
            cursor = connection.cursor()

            cursor.execute(
                """
                DELETE ir
                FROM inventory_recommendation ir
                JOIN demand_forecast df ON df.id = ir.demand_forecast_id
                JOIN model_run mr ON mr.id = df.model_run_id
                WHERE mr.dataset_version = %s
                """,
                (dataset_version,),
            )
            cursor.execute(
                """
                DELETE df
                FROM demand_forecast df
                JOIN model_run mr ON mr.id = df.model_run_id
                WHERE mr.dataset_version = %s
                """,
                (dataset_version,),
            )
            cursor.execute(
                "DELETE FROM model_run WHERE dataset_version = %s",
                (dataset_version,),
            )

            connection.commit()
            cursor.close()

    def save_model_run(
        self,
        *,
        model_name: str,
        model_version: str,
        run_type: str,
        dataset_version: str,
        train_from: datetime,
        train_to: datetime,
        evaluation_from: datetime,
        evaluation_to: datetime,
        mae: float,
        rmse: float,
        started_at: datetime,
        completed_at: datetime,
    ) -> int:
        with self.connect() as connection:
            cursor = connection.cursor()
            cursor.execute(
                """
                INSERT INTO model_run (
                    model_name,
                    model_version,
                    run_type,
                    dataset_version,
                    train_from,
                    train_to,
                    evaluation_from,
                    evaluation_to,
                    mae,
                    rmse,
                    status,
                    started_at,
                    completed_at
                )
                VALUES (
                    %s, %s, %s, %s,
                    %s, %s, %s, %s,
                    %s, %s, 'SUCCEEDED', %s, %s
                )
                """,
                (
                    model_name,
                    model_version,
                    run_type,
                    dataset_version,
                    train_from,
                    train_to,
                    evaluation_from,
                    evaluation_to,
                    mae,
                    rmse,
                    started_at,
                    completed_at,
                ),
            )
            run_id = int(cursor.lastrowid)
            connection.commit()
            cursor.close()
            return run_id

    def save_forecasts(
        self,
        model_run_id: int,
        forecasts: pd.DataFrame,
        generated_at: datetime,
    ) -> dict[tuple[int, int], int]:
        forecast_ids: dict[tuple[int, int], int] = {}

        with self.connect() as connection:
            cursor = connection.cursor()

            for row in forecasts.itertuples(index=False):
                cursor.execute(
                    """
                    INSERT INTO demand_forecast (
                        model_run_id,
                        rental_location_id,
                        product_category_id,
                        forecast_start_at,
                        forecast_end_at,
                        predicted_demand,
                        generated_at
                    )
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """,
                    (
                        model_run_id,
                        int(row.location_id),
                        int(row.category_id),
                        row.forecast_start_at.to_pydatetime(),
                        row.forecast_end_at.to_pydatetime(),
                        float(row.predicted_demand),
                        generated_at,
                    ),
                )
                forecast_ids[
                    (int(row.location_id), int(row.category_id))
                ] = int(cursor.lastrowid)

            connection.commit()
            cursor.close()

        return forecast_ids

    def save_recommendations(
        self,
        forecast_ids: dict[tuple[int, int], int],
        recommendations: pd.DataFrame,
        generated_at: datetime,
        model_name: str,
    ) -> None:
        with self.connect() as connection:
            cursor = connection.cursor()

            for row in recommendations.itertuples(index=False):
                key = (int(row.location_id), int(row.category_id))
                cursor.execute(
                    """
                    INSERT INTO inventory_recommendation (
                        demand_forecast_id,
                        available_quantity,
                        safety_stock,
                        recommended_additional_quantity,
                        risk_level,
                        reason,
                        status,
                        generated_at
                    )
                    VALUES (%s, %s, %s, %s, %s, %s, 'OPEN', %s)
                    """,
                    (
                        forecast_ids[key],
                        int(row.available_quantity),
                        int(row.safety_stock),
                        int(row.recommended_additional_quantity),
                        str(row.risk_level),
                        (
                            f"{model_name} forecast for "
                            f"{row.forecast_start_at} ~ {row.forecast_end_at}"
                        ),
                        generated_at,
                    ),
                )

            connection.commit()
            cursor.close()

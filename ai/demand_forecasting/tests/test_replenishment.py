from rentaltrack_ai.recommendation.replenishment import recommend_replenishment


def test_replenishment_rule_separates_forecast_from_business_decision():
    result = recommend_replenishment(
        predicted_demand=7.2,
        available_quantity=3,
        safety_stock=1,
    )

    assert result["recommended_additional_quantity"] == 6
    assert result["risk_level"] == "HIGH"


def test_no_replenishment_when_stock_covers_forecast_and_safety_stock():
    result = recommend_replenishment(
        predicted_demand=3.1,
        available_quantity=6,
        safety_stock=1,
    )

    assert result["recommended_additional_quantity"] == 0
    assert result["risk_level"] == "LOW"

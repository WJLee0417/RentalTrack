from rentaltrack_ai.data.dataset import add_time_features, validate_history
from rentaltrack_ai.data.synthetic import generate_synthetic_history


def test_synthetic_history_is_reproducible_and_valid():
    first = generate_synthetic_history(periods=48, seed=7, locations=(1,), categories=(1,))
    second = generate_synthetic_history(periods=48, seed=7, locations=(1,), categories=(1,))

    assert first.equals(second)

    validated = validate_history(first)
    featured = add_time_features(validated)

    assert len(featured) == 48
    assert {"hour", "day_of_week", "is_weekend"}.issubset(featured.columns)
    assert featured["is_synthetic"].all()

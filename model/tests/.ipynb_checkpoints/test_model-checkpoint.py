import json

import pandas as pd

from src.config import DATASET_PATH, FEATURES, METRICS_PATH, MODEL_PATH
from src.predict import predict_productivity
from src.export_android import ANDROID_ASSET_PATH


def test_dataset_has_required_features() -> None:
    data = pd.read_csv(DATASET_PATH, nrows=5)
    assert set(FEATURES).issubset(data.columns)


def test_saved_model_and_metrics_exist() -> None:
    assert MODEL_PATH.exists()
    assert METRICS_PATH.exists()
    report = json.loads(METRICS_PATH.read_text(encoding="utf-8"))
    assert report["row_count"] == 20_000


def test_prediction_is_in_expected_range() -> None:
    sample = pd.read_csv(DATASET_PATH, nrows=1).iloc[0].to_dict()
    prediction = predict_productivity(sample)
    assert 0.0 <= prediction <= 100.0


def test_missing_feature_is_rejected() -> None:
    sample = pd.read_csv(DATASET_PATH, nrows=1).iloc[0].to_dict()
    sample.pop(FEATURES[0])
    try:
        predict_productivity(sample)
    except ValueError as error:
        assert "Missing required features" in str(error)
    else:
        raise AssertionError("A missing feature should raise ValueError")


def test_android_export_matches_python_model() -> None:
    spec = json.loads(ANDROID_ASSET_PATH.read_text(encoding="utf-8"))
    sample = pd.read_csv(DATASET_PATH, nrows=1).iloc[0].to_dict()
    score = spec["intercept"]
    for index, feature in enumerate(spec["numeric_features"]):
        standardized = (
            sample[feature] - spec["numeric_means"][index]
        ) / spec["numeric_scales"][index]
        score += standardized * spec["numeric_coefficients"][index]
    if sample["gender"] in spec["categorical_values"]:
        category_index = spec["categorical_values"].index(sample["gender"])
        score += spec["categorical_coefficients"][category_index]

    expected = predict_productivity(sample)
    actual = round(min(max(score, spec["score_min"]), spec["score_max"]), 2)
    assert actual == expected

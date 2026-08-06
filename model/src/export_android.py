"""Export the trained linear pipeline to a small Android-readable JSON asset."""

from __future__ import annotations

import json
from pathlib import Path

import joblib

try:
    from .config import CATEGORICAL_FEATURES, MODEL_PATH, NUMERIC_FEATURES
except ImportError:
    from config import CATEGORICAL_FEATURES, MODEL_PATH, NUMERIC_FEATURES

ANDROID_ASSET_PATH = (
    Path(__file__).resolve().parents[2]
    / "app"
    / "src"
    / "main"
    / "assets"
    / "productivity_model.json"
)


def export() -> Path:
    artifact = joblib.load(MODEL_PATH)
    pipeline = artifact["model"]
    preprocessor = pipeline.named_steps["preprocessor"]
    regressor = pipeline.named_steps["regressor"]
    numeric_pipeline = preprocessor.named_transformers_["numeric"]
    categorical_pipeline = preprocessor.named_transformers_["categorical"]
    imputer = numeric_pipeline.named_steps["imputer"]
    scaler = numeric_pipeline.named_steps["scaler"]
    encoder = categorical_pipeline.named_steps["encoder"]

    numeric_count = len(NUMERIC_FEATURES)
    categories = [str(value) for value in encoder.categories_[0]]
    payload = {
        "format_version": 1,
        "model_name": artifact["model_name"],
        "target": artifact["target"],
        "score_min": artifact["score_range"][0],
        "score_max": artifact["score_range"][1],
        "intercept": float(regressor.intercept_),
        "numeric_features": NUMERIC_FEATURES,
        "numeric_medians": [float(value) for value in imputer.statistics_],
        "numeric_means": [float(value) for value in scaler.mean_],
        "numeric_scales": [float(value) for value in scaler.scale_],
        "numeric_coefficients": [float(value) for value in regressor.coef_[:numeric_count]],
        "categorical_feature": CATEGORICAL_FEATURES[0],
        "categorical_values": categories,
        "categorical_coefficients": [
            float(value) for value in regressor.coef_[numeric_count : numeric_count + len(categories)]
        ],
        "prototype_notice": (
            "Experimental estimate trained on a synthetic public dataset; "
            "not a real-time distraction prediction."
        ),
    }
    ANDROID_ASSET_PATH.parent.mkdir(parents=True, exist_ok=True)
    ANDROID_ASSET_PATH.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    return ANDROID_ASSET_PATH


if __name__ == "__main__":
    print(export())


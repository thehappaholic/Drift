"""Command-line inference for the saved Drift productivity model."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd

try:
    from .config import FEATURES, MODEL_PATH
except ImportError:
    from config import FEATURES, MODEL_PATH


def predict_productivity(values: dict[str, object], model_path: Path = MODEL_PATH) -> float:
    missing = [feature for feature in FEATURES if feature not in values]
    if missing:
        raise ValueError(f"Missing required features: {missing}")
    artifact = joblib.load(model_path)
    frame = pd.DataFrame([{feature: values[feature] for feature in FEATURES}])
    prediction = artifact["model"].predict(frame)[0]
    return round(float(np.clip(prediction, 0.0, 100.0)), 2)


def main() -> None:
    parser = argparse.ArgumentParser(description="Predict a productivity score from JSON input")
    parser.add_argument("--input", required=True, help="Path to a JSON file containing model features")
    args = parser.parse_args()
    values = json.loads(Path(args.input).read_text(encoding="utf-8"))
    print(json.dumps({"productivity_score": predict_productivity(values)}, indent=2))


if __name__ == "__main__":
    main()


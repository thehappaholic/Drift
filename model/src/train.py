"""Train and evaluate Drift's prototype productivity regression model."""

from __future__ import annotations

import json
from datetime import datetime, timezone

import joblib
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.dummy import DummyRegressor
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

try:
    from .config import (
        ARTIFACTS_DIR,
        CATEGORICAL_FEATURES,
        DATASET_PATH,
        FEATURES,
        METRICS_PATH,
        MODEL_PATH,
        NUMERIC_FEATURES,
        TARGET,
    )
except ImportError:
    from config import (
        ARTIFACTS_DIR,
        CATEGORICAL_FEATURES,
        DATASET_PATH,
        FEATURES,
        METRICS_PATH,
        MODEL_PATH,
        NUMERIC_FEATURES,
        TARGET,
    )

RANDOM_STATE = 42


def load_dataset() -> pd.DataFrame:
    if not DATASET_PATH.exists():
        raise FileNotFoundError(f"Dataset not found: {DATASET_PATH}")

    data = pd.read_csv(DATASET_PATH)
    required = set(FEATURES + [TARGET])
    missing = sorted(required.difference(data.columns))
    if missing:
        raise ValueError(f"Dataset is missing required columns: {missing}")
    if data.empty:
        raise ValueError("Dataset contains no rows")
    return data


def make_preprocessor() -> ColumnTransformer:
    numeric = Pipeline(
        [
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
        ]
    )
    categorical = Pipeline(
        [
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("encoder", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
        ]
    )
    return ColumnTransformer(
        [
            ("numeric", numeric, NUMERIC_FEATURES),
            ("categorical", categorical, CATEGORICAL_FEATURES),
        ]
    )


def make_pipeline(regressor: object) -> Pipeline:
    return Pipeline([("preprocessor", make_preprocessor()), ("regressor", regressor)])


def evaluate(model: Pipeline, features: pd.DataFrame, target: pd.Series) -> dict[str, float]:
    predictions = np.clip(model.predict(features), 0.0, 100.0)
    return {
        "mae": round(float(mean_absolute_error(target, predictions)), 4),
        "rmse": round(float(mean_squared_error(target, predictions) ** 0.5), 4),
        "r2": round(float(r2_score(target, predictions)), 4),
    }


def train() -> dict[str, object]:
    data = load_dataset()
    x = data[FEATURES]
    y = data[TARGET]

    x_train, x_holdout, y_train, y_holdout = train_test_split(
        x, y, test_size=0.30, random_state=RANDOM_STATE
    )
    x_validation, x_test, y_validation, y_test = train_test_split(
        x_holdout, y_holdout, test_size=0.50, random_state=RANDOM_STATE
    )

    candidates = {
        "dummy_mean": DummyRegressor(strategy="mean"),
        "ridge": Ridge(alpha=1.0),
        "gradient_boosting": GradientBoostingRegressor(
            n_estimators=250, learning_rate=0.05, max_depth=3, random_state=RANDOM_STATE
        ),
        "random_forest": RandomForestRegressor(
            n_estimators=250,
            min_samples_leaf=2,
            max_features=0.8,
            n_jobs=-1,
            random_state=RANDOM_STATE,
        ),
    }

    trained: dict[str, Pipeline] = {}
    validation_metrics: dict[str, dict[str, float]] = {}
    for name, regressor in candidates.items():
        pipeline = make_pipeline(regressor)
        pipeline.fit(x_train, y_train)
        trained[name] = pipeline
        validation_metrics[name] = evaluate(pipeline, x_validation, y_validation)

    eligible = [name for name in candidates if name != "dummy_mean"]
    best_name = min(eligible, key=lambda name: validation_metrics[name]["mae"])
    best_model = trained[best_name]
    test_metrics = evaluate(best_model, x_test, y_test)

    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    artifact = {
        "model": best_model,
        "features": FEATURES,
        "target": TARGET,
        "score_range": [0.0, 100.0],
        "model_name": best_name,
    }
    joblib.dump(artifact, MODEL_PATH)

    synthetic_target_warning = validation_metrics["ridge"]["r2"] >= 0.999
    limitations = [
        "This is a prototype productivity model, not a real-time distraction model.",
        "The dataset contains cross-sectional records rather than per-user time-series events.",
        "Predictions should be presented as estimates, not objective judgments.",
    ]
    if synthetic_target_warning:
        limitations.append(
            "A linear model predicts the target almost perfectly, strongly indicating that "
            "productivity_score was generated from a fixed formula rather than measured in the real world."
        )

    report: dict[str, object] = {
        "created_at_utc": datetime.now(timezone.utc).isoformat(),
        "dataset": str(DATASET_PATH),
        "row_count": int(len(data)),
        "split_rows": {
            "train": int(len(x_train)),
            "validation": int(len(x_validation)),
            "test": int(len(x_test)),
        },
        "features": FEATURES,
        "target": TARGET,
        "validation_metrics": validation_metrics,
        "selected_model": best_name,
        "test_metrics": test_metrics,
        "synthetic_target_warning": synthetic_target_warning,
        "limitations": limitations,
    }
    METRICS_PATH.write_text(json.dumps(report, indent=2), encoding="utf-8")
    return report


if __name__ == "__main__":
    result = train()
    print(json.dumps(result, indent=2))

from pathlib import Path

MODEL_DIR = Path(__file__).resolve().parents[1]
DATASET_PATH = MODEL_DIR / "data" / "student_productivity_distraction_dataset_20000.csv"
ARTIFACTS_DIR = MODEL_DIR / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "productivity_model.joblib"
METRICS_PATH = ARTIFACTS_DIR / "metrics.json"

TARGET = "productivity_score"
DROP_COLUMNS = ["student_id", "final_grade"]
CATEGORICAL_FEATURES = ["gender"]
NUMERIC_FEATURES = [
    "age",
    "study_hours_per_day",
    "sleep_hours",
    "phone_usage_hours",
    "social_media_hours",
    "youtube_hours",
    "gaming_hours",
    "breaks_per_day",
    "coffee_intake_mg",
    "exercise_minutes",
    "assignments_completed",
    "attendance_percentage",
    "stress_level",
    "focus_score",
]
FEATURES = CATEGORICAL_FEATURES + NUMERIC_FEATURES


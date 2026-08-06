# Drift productivity model

This directory contains Drift's prototype regression model. It predicts a
`productivity_score` between 0 and 100 from student lifestyle and academic
features. It does **not** predict real-time distraction.

## Dataset

Place the Kaggle CSV at:

`data/student_productivity_distraction_dataset_20000.csv`

The training pipeline deliberately excludes `student_id` because it is only an
identifier and `final_grade` because it may not be known at prediction time.

## Set up

From the `model` directory on Windows PowerShell:

```powershell
python -m venv .venv
.\.venv\Scripts\python -m pip install -r requirements.txt
```

## Train

```powershell
.\.venv\Scripts\python -m src.train
```

Training compares a mean baseline, Ridge regression, Gradient Boosting, and
Random Forest. The best non-baseline model is selected using validation MAE and
evaluated once on the held-out test set.

Outputs:

- `artifacts/productivity_model.joblib` — preprocessing and trained model
- `artifacts/metrics.json` — split sizes, validation results, and final test metrics

## Android integration

`python -m src.export_android` exports the fitted preprocessing values and Ridge
coefficients to `app/src/main/assets/productivity_model.json`. Drift evaluates
that exact linear pipeline on-device through `ProductivityPredictor.kt`; Python
and a network connection are not required while the app is running.

The Dashboard keeps its real, rule-based Focus Score and displays the model as a
separate **Productivity estimate**. Inputs available from Drift use live/profile
data. Dataset fields Drift does not currently collect (coffee, exercise,
attendance, and stress) use documented training-baseline values, so this remains
an experimental demonstration rather than a real-time distraction predictor.

## Predict

```powershell
.\.venv\Scripts\python -m src.predict --input sample_input.json
```

## Test

```powershell
.\.venv\Scripts\python -m pytest -q
```

## Limitations

- The source data is cross-sectional, not a history of events from Drift users.
- Ridge regression reproduces the target almost perfectly. This strongly suggests
  that `productivity_score` was generated using a fixed formula, so the excellent
  test metrics do not demonstrate real-world predictive accuracy.
- The output is an experimental estimate and must not be presented as fact.
- A future distraction-risk model requires timestamped, consented usage and
  focus-session outcomes collected through Drift.

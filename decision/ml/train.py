import argparse
import json
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.calibration import CalibratedClassifierCV
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    brier_score_loss,
    classification_report,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler


# СПИСКИ ПРИЗНАКОВ
NUM_FEATURES = [
    "age_months",
    "revenue_annual",
    "employees_count",
    "amount",
    "term_months",
    "delinquencies_12m",
    "active_loans_count",
    "total_debt_amount",
    "current_debt_payments",
    "accounts_payable",
    "net_profit_monthly",
    "debt_load",
    "loan_to_revenue",
    "revenue_per_employee",
    "profit_margin_calc",
    "credit_line_utilization",
    "debt_service_coverage",
    "liquidity_ratio",
]

CAT_FEATURES = [
    "company_type",
    "industry",
    "product_id",
    "purpose",
    "business_status",
]

TARGET = "default"


def build_pipeline() -> Pipeline:
    """StandardScaler для чисел + OneHotEncoder для категорий + LogReg."""
    pre = ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), NUM_FEATURES),
            ("cat", OneHotEncoder(handle_unknown="ignore", sparse_output=False), CAT_FEATURES),
        ],
        remainder="drop",
    )
    clf = LogisticRegression(
        max_iter=2000,
        C=1.0,                     # L2-регуляризация, справляется с корреляцией 0.72
        class_weight="balanced",   # дисбаланс классов (дефолтов ~13%)
        solver="lbfgs",
        random_state=42,
    )
    return Pipeline([("pre", pre), ("clf", clf)])


def evaluate(y_true, p_pred, threshold: float = 0.5) -> dict:
    """Метрики: ROC-AUC, PR-AUC, Brier, precision/recall по порогу."""
    y_pred = (p_pred >= threshold).astype(int)
    report = classification_report(
        y_true, y_pred, output_dict=True, zero_division=0
    )
    return {
        "roc_auc": round(roc_auc_score(y_true, p_pred), 4),
        "pr_auc": round(average_precision_score(y_true, p_pred), 4),
        "brier": round(brier_score_loss(y_true, p_pred), 4),
        "precision_default": round(report["1"]["precision"], 4),
        "recall_default": round(report["1"]["recall"], 4),
        "f1_default": round(report["1"]["f1-score"], 4),
        "threshold": threshold,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="../data/synthetic.csv")
    parser.add_argument("--out", default="artifacts")
    parser.add_argument("--test-size", type=float, default=0.2)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--threshold", type=float, default=0.5)
    args = parser.parse_args()

    data_path = Path(args.data)
    if not data_path.exists():
        raise FileNotFoundError(
            f"Нет файла {data_path}. Сначала запустите: python make_data.py"
        )

    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)

    # 1. ЧТЕНИЕ
    df = pd.read_csv(data_path)
    print(f"Загружено: {df.shape[0]} строк, {df.shape[1]} колонок")
    print(f"Доля дефолтов: {df[TARGET].mean():.4f}")

    X = df[NUM_FEATURES + CAT_FEATURES]
    y = df[TARGET]

    # 2. SPLIT
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=args.test_size, stratify=y, random_state=args.seed
    )
    print(f"Train: {len(X_train)}, Test: {len(X_test)}")
    print(f"Train default rate: {y_train.mean():.4f}, "
          f"Test default rate: {y_test.mean():.4f}")

    # 3. БАЗОВАЯ МОДЕЛЬ (только debt_load)
    print("\n--- Базовая модель (только debt_load) ---")
    from sklearn.linear_model import LogisticRegression as LR
    base = LR(max_iter=1000, class_weight="balanced", random_state=42)
    base.fit(X_train[["debt_load"]], y_train)
    p_base = base.predict_proba(X_test[["debt_load"]])[:, 1]
    base_metrics = evaluate(y_test, p_base, args.threshold)
    print(json.dumps(base_metrics, indent=2, ensure_ascii=False))

    # 4. ОСНОВНАЯ МОДЕЛЬ
    print("\n--- Логистическая регрессия (все признаки) ---")
    pipe = build_pipeline()
    pipe.fit(X_train, y_train)

    # Калибровка: isotonic на train (cv=3), чтобы вероятности были честными
    calibrated = CalibratedClassifierCV(pipe, method="isotonic", cv=3)
    calibrated.fit(X_train, y_train)

    p_pred = calibrated.predict_proba(X_test)[:, 1]
    metrics = evaluate(y_test, p_pred, args.threshold)
    print(json.dumps(metrics, indent=2, ensure_ascii=False))

    # 5. СРАВНЕНИЕ
    print("\n--- Сравнение ---")
    print(f"  base ROC-AUC: {base_metrics['roc_auc']:.4f}")
    print(f"  full ROC-AUC: {metrics['roc_auc']:.4f}")
    print(f"  Δ: {metrics['roc_auc'] - base_metrics['roc_auc']:+.4f}")

    # 6. КОЭФФИЦИЕНТЫ (для объяснимости)
    print("\n--- Топ-15 коэффициентов (по модулю) ---")
    clf = calibrated.calibrated_classifiers_[0].estimator.named_steps["clf"]
    pre = calibrated.calibrated_classifiers_[0].estimator.named_steps["pre"]
    feature_names = pre.get_feature_names_out()
    coefs = clf.coef_[0]
    coef_df = pd.DataFrame({"feature": feature_names, "coef": coefs})
    coef_df["abs"] = coef_df["coef"].abs()
    print(coef_df.sort_values("abs", ascending=False).head(15)[["feature", "coef"]]
          .to_string(index=False))

    # 7. СОХРАНЕНИЕ
    model_path = out_dir / "model.pkl"
    features_path = out_dir / "features.pkl"
    metrics_path = out_dir / "metrics.json"

    joblib.dump(calibrated, model_path)
    joblib.dump({"num": NUM_FEATURES, "cat": CAT_FEATURES}, features_path)
    with open(metrics_path, "w", encoding="utf-8") as f:
        json.dump(
            {
                "base_model": base_metrics,
                "full_model": metrics,
                "n_train": len(X_train),
                "n_test": len(X_test),
                "default_rate": float(df[TARGET].mean()),
                "model_version": "logreg-v1",
            },
            f,
            indent=2,
            ensure_ascii=False,
        )

    print(f"\nСохранено:")
    print(f"  {model_path}")
    print(f"  {features_path}")
    print(f"  {metrics_path}")


if __name__ == "__main__":
    main()
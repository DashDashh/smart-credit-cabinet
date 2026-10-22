import json
from pathlib import Path
from typing import Optional

import joblib
import numpy as np
import pandas as pd

from fallback import fallback_scorecard
from schemas import DecisionRequest, Factor


ARTIFACTS_DIR = Path(__file__).resolve().parent / "ml" / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "model.pkl"
FEATURES_PATH = ARTIFACTS_DIR / "features.pkl"
METRICS_PATH = ARTIFACTS_DIR / "metrics.json"


_model = None
_features: Optional[dict] = None
_metrics: Optional[dict] = None
_load_error: Optional[str] = None


def load_model() -> bool:
    global _model, _features, _metrics, _load_error
    try:
        _model = joblib.load(MODEL_PATH)
        _features = joblib.load(FEATURES_PATH)
        if METRICS_PATH.exists():
            with open(METRICS_PATH, "r", encoding="utf-8") as f:
                _metrics = json.load(f)
        _load_error = None
        return True
    except Exception as e:
        _load_error = f"{type(e).__name__}: {e}"
        _model = None
        _features = None
        _metrics = None
        return False


def is_ml_available() -> bool:
    return _model is not None and _features is not None


def get_load_error() -> Optional[str]:
    return _load_error


def get_metrics() -> Optional[dict]:
    return _metrics


def get_model_version() -> str:
    if _metrics and "model_version" in _metrics:
        return _metrics["model_version"]
    return "fallback-v1"


# СБОРКА ПРИЗНАКОВ
def build_feature_row(req: DecisionRequest) -> dict:
    c = req.company
    f = req.financials
    h = req.credit_history
    a = req.application

    debt_load = min(max(f.current_debt_payments / max(f.revenue_monthly, 1.0), 0.0), 3.0)
    loan_to_revenue = a.amount / max(f.revenue_annual, 1.0)
    revenue_per_employee = f.revenue_annual / max(c.employees_count, 1)
    profit_margin_calc = (
        f.net_profit_monthly / f.revenue_monthly if f.revenue_monthly > 0 else 0.0
    )
    credit_line_utilization = min(
        max(h.total_debt_amount / max(f.revenue_annual * 0.5, 1.0), 0.0), 2.0
    )
    debt_service_coverage = (
        f.net_profit_monthly / f.current_debt_payments
        if f.current_debt_payments > 0 else 2.0
    )
    debt_service_coverage = min(max(debt_service_coverage, -2.0), 5.0)
    liquidity_ratio = min(
        max((f.revenue_monthly * 3) / max(f.accounts_payable + f.current_debt_payments, 1.0), 0.0),
        5.0,
    )

    return {
        "company_type": c.company_type.value,
        "industry": c.industry,
        "age_months": c.age_months,
        "employees_count": c.employees_count,
        "business_status": c.business_status.value,
        "revenue_monthly": f.revenue_monthly,
        "revenue_annual": f.revenue_annual,
        "current_debt_payments": f.current_debt_payments,
        "net_profit_monthly": f.net_profit_monthly,
        "accounts_payable": f.accounts_payable,
        "delinquencies_12m": h.delinquencies_12m,
        "active_loans_count": h.active_loans_count,
        "total_debt_amount": h.total_debt_amount,
        "tax_arrears": h.tax_arrears,
        "product_id": a.product_id.value,
        "amount": a.amount,
        "term_months": a.term_months,
        "purpose": a.purpose,
        "debt_load": debt_load,
        "loan_to_revenue": loan_to_revenue,
        "revenue_per_employee": revenue_per_employee,
        "profit_margin_calc": profit_margin_calc,
        "credit_line_utilization": credit_line_utilization,
        "debt_service_coverage": debt_service_coverage,
        "liquidity_ratio": liquidity_ratio,
    }


# ИНФЕРЕНС
def predict(req: DecisionRequest) -> tuple[float, float, list[Factor], bool]:
    """
    Возвращает (score_0_100, p_default, factors, fallback_used).

    Если ML недоступна или падает — использует fallback_scorecard.
    Никогда не возвращает ошибку.
    """
    if is_ml_available():
        try:
            row = build_feature_row(req)
            X = pd.DataFrame([row])[_features["num"] + _features["cat"]]
            p_default = float(_model.predict_proba(X)[0, 1])
            score = round((1.0 - p_default) * 100.0, 1)
            score = min(max(score, 0.0), 100.0)
            factors = _extract_factors(X, row)
            return score, p_default, factors, False
        except Exception:
            pass

    # fallback
    score, factors = fallback_scorecard(req)
    p_default = 1.0 - score / 100.0
    return score, p_default, factors, True


def _extract_factors(X: pd.DataFrame, row: dict) -> list[Factor]:
    try:
        calibrated = _model
        inner = calibrated.calibrated_classifiers_[0].estimator
        pre = inner.named_steps["pre"]
        clf = inner.named_steps["clf"]

        feature_names = pre.get_feature_names_out()
        coefs = clf.coef_[0]
        X_transformed = pre.transform(X)[0]
        contribs = X_transformed * coefs

        pairs = sorted(
            zip(feature_names, contribs),
            key=lambda x: abs(x[1]),
            reverse=True,
        )

        factors: list[Factor] = []
        for name, contrib in pairs[:5]:
            clean = _clean_feature_name(name)
            if clean is None:
                continue
            factors.append(Factor(
                name=clean,
                impact=round(float(contrib) * 10.0, 2),
                value=_get_value(clean, row),
            ))
        return factors
    except Exception:
        return []


def _clean_feature_name(raw: str) -> Optional[str]:
    if raw.startswith("num__"):
        return raw[5:]
    if raw.startswith("cat__"):
        return raw[5:].replace("_", "=", 1)
    return raw


def _get_value(clean_name: str, row: dict) -> float | str:
    if "=" in clean_name:
        base, cat = clean_name.split("=", 1)
        return row.get(base, cat)
    return float(row.get(clean_name, 0.0))
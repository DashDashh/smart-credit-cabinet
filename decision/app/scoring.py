import json
from pathlib import Path
from typing import Optional

import joblib
import numpy as np
import pandas as pd

from schemas import DecisionRequest, Factor


# ПУТИ К АРТЕФАКТАМ
ARTIFACTS_DIR = Path(__file__).resolve().parent / "ml" / "artifacts"
MODEL_PATH = ARTIFACTS_DIR / "model.pkl"
FEATURES_PATH = ARTIFACTS_DIR / "features.pkl"
METRICS_PATH = ARTIFACTS_DIR / "metrics.json"


# ГЛОБАЛЬНОЕ СОСТОЯНИЕ
_model = None
_features: Optional[dict] = None
_metrics: Optional[dict] = None
_load_error: Optional[str] = None


def load_model() -> bool:
    """
    Загружает модель и списки признаков.
    Возвращает True, если ML доступна, False — если нужно использовать fallback.
    """
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
    return "logreg-v1"


# СБОРКА ПРИЗНАКОВ
def build_feature_row(req: DecisionRequest) -> dict:
    """
    Собирает "сырой" dict признаков из DecisionRequest.
    Производные признаки считаются здесь же — чтобы не дублировать
    в rules.py и scoring.py.
    """
    c = req.company
    f = req.financials
    h = req.credit_history
    a = req.application

    # производные
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
        if f.current_debt_payments > 0
        else 2.0
    )
    debt_service_coverage = min(max(debt_service_coverage, -2.0), 5.0)
    liquidity_ratio = min(
        max(
            (f.revenue_monthly * 3)
            / max(f.accounts_payable + f.current_debt_payments, 1.0),
            0.0,
        ),
        5.0,
    )

    return {
        # компания
        "company_type": c.company_type.value,
        "industry": c.industry,
        "age_months": c.age_months,
        "employees_count": c.employees_count,
        "business_status": c.business_status.value,
        # финансы
        "revenue_monthly": f.revenue_monthly,
        "revenue_annual": f.revenue_annual,
        "current_debt_payments": f.current_debt_payments,
        "net_profit_monthly": f.net_profit_monthly,
        "accounts_payable": f.accounts_payable,
        # кредитная история
        "delinquencies_12m": h.delinquencies_12m,
        "active_loans_count": h.active_loans_count,
        "total_debt_amount": h.total_debt_amount,
        "tax_arrears": h.tax_arrears,
        # заявка
        "product_id": a.product_id.value,
        "amount": a.amount,
        "term_months": a.term_months,
        "purpose": a.purpose,
        # производные
        "debt_load": debt_load,
        "loan_to_revenue": loan_to_revenue,
        "revenue_per_employee": revenue_per_employee,
        "profit_margin_calc": profit_margin_calc,
        "credit_line_utilization": credit_line_utilization,
        "debt_service_coverage": debt_service_coverage,
        "liquidity_ratio": liquidity_ratio,
    }


# ИНФЕРЕНС
def predict(req: DecisionRequest) -> tuple[float, float, list[Factor]]:
    """
    Возвращает (score_0_100, p_default, factors).

    score = (1 - p_default) * 100.
    factors — топ-5 по модулю вклада в p_default.
    """
    if not is_ml_available():
        raise RuntimeError("ML model not loaded")

    row = build_feature_row(req)
    X = pd.DataFrame([row])[_features["num"] + _features["cat"]]

    # калиброванная вероятность
    p_default = float(_model.predict_proba(X)[0, 1])
    score = round((1.0 - p_default) * 100.0, 1)
    score = min(max(score, 0.0), 100.0)

    factors = _extract_factors(X, p_default)
    return score, p_default, factors


def _extract_factors(X: pd.DataFrame, p_default: float) -> list[Factor]:
    """
    Достаёт топ-5 факторов по модулю вклада в логит.

    Вклад = coefficient * standardized_value (для чисел)
            coefficient (для категорий).
    """
    try:
        # достаём внутренний estimator и preprocessor из калиброванного pipeline
        calibrated = _model
        inner = calibrated.calibrated_classifiers_[0].estimator
        pre = inner.named_steps["pre"]
        clf = inner.named_steps["clf"]

        feature_names = pre.get_feature_names_out()
        coefs = clf.coef_[0]

        # трансформируем X (numpy array)
        X_transformed = pre.transform(X)[0]
        contribs = X_transformed * coefs

        # сопоставляем имена и вклады
        pairs = list(zip(feature_names, contribs))
        # сортируем по модулю вклада
        pairs.sort(key=lambda x: abs(x[1]), reverse=True)

        factors: list[Factor] = []
        for name, contrib in pairs[:5]:
            # чистим имя: "num__debt_load" -> "debt_load", "cat__industry_food" -> "industry=food"
            clean = _clean_feature_name(name)
            if clean is None:
                continue
            factors.append(
                Factor(
                    name=clean,
                    impact=round(float(contrib) * 10.0, 2),
                    value=_get_value(clean),
                )
            )
        return factors
    except Exception:
        # если что-то пошло не так — возвращаем пустой список,
        # rules.py всё равно добавит policy-факторы
        return []


def _clean_feature_name(raw: str) -> Optional[str]:
    """'num__debt_load' -> 'debt_load'; 'cat__industry_food' -> 'industry=food'."""
    if raw.startswith("num__"):
        return raw[5:]
    if raw.startswith("cat__"):
        return raw[5:].replace("_", "=", 1)
    return raw


def _get_value(feature: str) -> float | str:
    """Пытается достать значение признака из последнего запроса."""
    # простая эвристика: если это категория — вернём строку, иначе 0
    # (в реальном проде лучше хранить последний row в контексте запроса)
    return 0.0
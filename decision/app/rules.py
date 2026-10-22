from schemas import (
    CompanyType,
    Conditions,
    DecisionRequest,
    DecisionResponse,
    DecisionType,
    Factor,
    RiskLevel,
)


# КОНСТАНТЫ: ПОРОГИ
# score: 100 — минимальный риск, 0 — максимальный.
APPROVE_THRESHOLD = 65.0           # score >= 65 - approved
CONDITIONS_THRESHOLD = 45.0        # 45 <= score < 65 - approved_with_conditions
NEEDS_INFO_THRESHOLD = 30.0        # 30 <= score < 45 - needs_info
# score < 30 - rejected


# КОНСТАНТЫ: ПРОДУКТЫ
PRODUCT_LIMITS = {
    "investment": {
        "max_amount": 30_000_000,
        "terms": [12, 24, 36, 60],
        "min_age_months": 12,
        "base_rate": 15.0,
    },
    "working_capital": {
        "max_amount": 5_000_000,
        "terms": [6, 12, 24],
        "min_age_months": 6,
        "base_rate": 17.5,
    },
    "micro_loan": {
        "max_amount": 300_000,
        "terms": [3, 6],
        "min_age_months": 3,
        "base_rate": 22.0,
    },
}


# 1. ЖЁСТКИЕ ПРОВЕРКИ (policy_checks)

def policy_checks(req: DecisionRequest) -> list[Factor]:
    """
    Жёсткие правила, которые не зависят от ML.

    Возвращает список факторов. Если хоть один фактор имеет impact <= -100,
    решение — rejected (hard block), без вызова ML.
    """
    factors: list[Factor] = []
    c = req.company
    a = req.application
    f = req.financials
    product = PRODUCT_LIMITS.get(a.product_id.value, {})

    # 1. Бизнес-статус
    if c.business_status.value != "active":
        factors.append(Factor(
            name="business_status_not_active",
            impact=-100.0,
            value=c.business_status.value,
        ))

    # 2. Возраст компании vs продукт
    min_age = product.get("min_age_months", 0)
    if c.age_months < min_age:
        factors.append(Factor(
            name="company_age_below_product_min",
            impact=-100.0,
            value=c.age_months,
        ))

    # 3. Сумма vs лимит продукта
    max_amount = product.get("max_amount", float("inf"))
    if a.amount > max_amount:
        factors.append(Factor(
            name="amount_above_product_limit",
            impact=-100.0,
            value=a.amount,
        ))

    # 4. Срок vs допустимые
    allowed_terms = product.get("terms", [])
    if allowed_terms and a.term_months not in allowed_terms:
        factors.append(Factor(
            name="term_not_allowed_for_product",
            impact=-100.0,
            value=a.term_months,
        ))

    # 5. Налоговые задолженности (мягкий штраф, не блок)
    if req.credit_history.tax_arrears:
        factors.append(Factor(
            name="tax_arrears",
            impact=-15.0,
            value=True,
        ))

    # 6. Расхождение выручки с ФНС (если есть официальная)
    # В MVP fns.official_revenue не передаётся, но если появится — проверим
    # (оставлено как TODO: если добавите поле в schemas)

    # 7. Отрицательная прибыль при большой нагрузке
    if f.net_profit_monthly < 0 and req.credit_history.current_debt_payments > 0:
        factors.append(Factor(
            name="negative_profit_with_debt",
            impact=-20.0,
            value=round(f.net_profit_monthly, 2),
        ))

    return factors


# 2. ПОРОГИ (score_to_decision)

def score_to_decision(score: float) -> DecisionType:
    """Маппинг score - решение по порогам."""
    if score >= APPROVE_THRESHOLD:
        return DecisionType.approved
    if score >= CONDITIONS_THRESHOLD:
        return DecisionType.approved_with_conditions
    if score >= NEEDS_INFO_THRESHOLD:
        return DecisionType.needs_info
    return DecisionType.rejected


def risk_level(score: float) -> RiskLevel:
    if score >= APPROVE_THRESHOLD:
        return RiskLevel.low
    if score >= CONDITIONS_THRESHOLD:
        return RiskLevel.medium
    return RiskLevel.high


# 3. УСЛОВИЯ (build_conditions)

def build_conditions(req: DecisionRequest, score: float) -> Conditions:
    """
    Предлагает изменённые условия для approved_with_conditions.

    Логика:
    - сумма: 70% от запрошенной, но не больше лимита продукта;
    - срок: ближайший допустимый из продукта;
    - ставка: базовая + премия за риск (чем ниже score, тем выше премия).
    """
    a = req.application
    product = PRODUCT_LIMITS[a.product_id.value]
    base_rate = product["base_rate"]

    # сумма: 70% от запрошенной, но не больше лимита
    new_amount = min(a.amount * 0.7, product["max_amount"])

    # срок: ближайший допустимый
    new_term = min(product["terms"], key=lambda t: abs(t - a.term_months))

    # ставка: базовая + премия за риск
    # при score=65 премия=0, при score=45 премия=+3.0
    premium = max(0.0, (APPROVE_THRESHOLD - score) / 10.0) * 1.5
    new_rate = round(base_rate + premium, 1)

    return Conditions(
        approved_amount=round(new_amount, -3),   # округляем до тысяч
        term_months=new_term,
        rate=new_rate,
        reason="amount_above_policy_or_risk_level",
    )


# 4. NEXT_STEP (маппинг на состояние заявки)

NEXT_STEP_MAP = {
    DecisionType.approved: "client_accept_terms",
    DecisionType.approved_with_conditions: "client_accept_conditions",
    DecisionType.needs_info: "bank_request_info",
    DecisionType.rejected: "notify_client_rejection",
    DecisionType.manual_review: "manual_review",
}


# 5. ГЛАВНАЯ ФУНКЦИЯ

def decide(req: DecisionRequest) -> DecisionResponse:
    """
    Принимает решение по заявке.

    Порядок:
    1. policy_checks — жёсткие стопы (без ML).
    2. scoring.predict — ML с fallback.
    3. score_to_decision — пороги.
    4. build_conditions — если approved_with_conditions.
    """
    from scoring import predict

    # 1. Жёсткие проверки
    policy = policy_checks(req)
    hard_block = any(f.impact <= -100 for f in policy)

    if hard_block:
        return DecisionResponse(
            score=0.0,
            risk_level=RiskLevel.high,
            decision=DecisionType.rejected,
            conditions=None,
            factors=policy,
            next_step=NEXT_STEP_MAP[DecisionType.rejected],
            model_version="policy-v1",
            fallback_used=False,
        )

    # 2. Скоринг (ML или fallback)
    score, p_default, ml_factors, fallback_used = predict(req)

    # 3. Объединяем факторы: policy + ML
    factors = policy + ml_factors

    # 4. Порог
    decision = score_to_decision(score)

    # 5. Условия (если нужны)
    conditions = None
    if decision == DecisionType.approved_with_conditions:
        conditions = build_conditions(req, score)

    # 6. Модель-версия
    if fallback_used:
        model_version = "fallback-v1"
    else:
        from scoring import get_model_version
        model_version = get_model_version()

    return DecisionResponse(
        score=round(score, 1),
        risk_level=risk_level(score),
        decision=decision,
        conditions=conditions,
        factors=factors,
        next_step=NEXT_STEP_MAP[decision],
        model_version=model_version,
        fallback_used=fallback_used,
    )
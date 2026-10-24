from typing import Optional

from .schemas import (
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
# score < 45 - rejected


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

    # 6. Отрицательная прибыль при наличии долга (мягкий штраф)
    if f.net_profit_monthly < 0 and f.current_debt_payments > 0:
        factors.append(Factor(
            name="negative_profit_with_debt",
            impact=-20.0,
            value=round(f.net_profit_monthly, 2),
        ))

    return factors


# 2. ПРОВЕРКА ПОЛНОТЫ ДАННЫХ (до ML)

def check_data_completeness(req: DecisionRequest) -> list[Factor]:
    """
    Проверяет полноту и корректность данных ДО скоринга.

    Возвращает список факторов. Если хоть один имеет
    impact <= -100 - needs_info (скоринг не запускается).
    """
    factors: list[Factor] = []
    c = req.company
    f = req.financials
    h = req.credit_history
    a = req.application

    # --- 1. Обязательные финансовые поля ---
    if f.revenue_monthly is None or f.revenue_monthly <= 0:
        factors.append(Factor(
            name="missing_revenue_monthly",
            impact=-100.0,
            value=0.0,
            description="Укажите месячную выручку — без неё невозможна оценка долговой нагрузки",
        ))
    if f.revenue_annual is None or f.revenue_annual <= 0:
        factors.append(Factor(
            name="missing_revenue_annual",
            impact=-100.0,
            value=0.0,
            description="Укажите годовую выручку — она нужна для расчёта долговой нагрузки и лимитов",
        ))
    if f.net_profit_monthly is None:
        factors.append(Factor(
            name="missing_net_profit",
            impact=-100.0,
            value=0.0,
            description="Укажите месячную чистую прибыль — она влияет на оценку покрытия долга",
        ))

    # --- 2. Несогласованность выручки (месячная vs годовая) ---
    if f.revenue_monthly and f.revenue_annual:
        ratio = f.revenue_annual / (f.revenue_monthly * 12)
        if ratio < 0.5 or ratio > 2.0:
            factors.append(Factor(
                name="revenue_mismatch",
                impact=-100.0,
                value=round(ratio, 2),
                description=(
                    f"Годовая выручка не согласуется с месячной "
                    f"(отношение {ratio:.2f}, ожидается 0.5–2.0). "
                    f"Проверьте данные или приложите подтверждающие документы"
                ),
            ))

    # --- 3. Сумма заявки vs выручка (грубая проверка корректности) ---
    if f.revenue_annual and a.amount > f.revenue_annual * 5:
        factors.append(Factor(
            name="amount_far_above_revenue",
            impact=-100.0,
            value=round(a.amount / f.revenue_annual, 2),
            description=(
                f"Запрошенная сумма превышает годовую выручку более чем в 5 раз. "
                f"Уточните сумму или приложите обоснование (бизнес-план, контракты)"
            ),
        ))

    # --- 4. Возраст компании ---
    if c.age_months is None or c.age_months < 0:
        factors.append(Factor(
            name="missing_age",
            impact=-100.0,
            value=0,
            description="Укажите, сколько месяцев существует бизнес — это влияет на оценку риска",
        ))

    # --- 5. Срок и сумма ---
    if a.term_months is None or a.term_months <= 0:
        factors.append(Factor(
            name="invalid_term",
            impact=-100.0,
            value=a.term_months,
            description="Укажите срок кредита в месяцах — он должен быть положительным",
        ))
    if a.amount is None or a.amount <= 0:
        factors.append(Factor(
            name="invalid_amount",
            impact=-100.0,
            value=a.amount,
            description="Укажите сумму кредита — она должна быть положительной",
        ))

    # --- 6. Кредитная история: просрочки без долга ---
    if h.delinquencies_12m > 0 and h.total_debt_amount == 0:
        factors.append(Factor(
            name="delinquencies_without_debt",
            impact=-100.0,
            value=h.delinquencies_12m,
            description=(
                f"У вас {h.delinquencies_12m} просрочек, но не указана сумма обязательств. "
                f"Уточните сумму или приложите кредитную историю"
            ),
        ))

    return factors


# 3. ПОРОГИ (score_to_decision)

def score_to_decision(score: float) -> DecisionType:
    """
    Маппинг score - решение по порогам.
    """
    if score >= APPROVE_THRESHOLD:
        return DecisionType.approved
    if score >= CONDITIONS_THRESHOLD:
        return DecisionType.approved_with_conditions
    return DecisionType.rejected


def risk_level(score: float) -> RiskLevel:
    if score >= APPROVE_THRESHOLD:
        return RiskLevel.low
    if score >= CONDITIONS_THRESHOLD:
        return RiskLevel.medium
    return RiskLevel.high


# 4. УСЛОВИЯ (build_conditions)

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


# 5. NEXT_STEP (маппинг на состояние заявки)

NEXT_STEP_MAP = {
    DecisionType.approved: "client_accept_terms",
    DecisionType.approved_with_conditions: "client_accept_conditions",
    DecisionType.needs_info: "bank_request_info",
    DecisionType.rejected: "notify_client_rejection",
    DecisionType.manual_review: "manual_review",
}


# 6. ГЛАВНАЯ ФУНКЦИЯ

def decide(req: DecisionRequest) -> DecisionResponse:
    """
    Принимает решение по заявке.

    Порядок:
    1. policy_checks           — жёсткие стопы (без ML).
    2. check_data_completeness — полнота данных (без ML).
    3. scoring.predict         — ML или fallback (только если данные полные).
    4. score_to_decision       — пороги 65/45.
    5. build_conditions        — если approved_with_conditions.
    """
    from .scoring import predict, get_model_version

    # 1. ЖЁСТКИЕ ПРОВЕРКИ
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

    # 2. ПРОВЕРКА ПОЛНОТЫ
    completeness = check_data_completeness(req)
    incomplete = any(f.impact <= -100 for f in completeness)
    if incomplete:
        return DecisionResponse(
            score=0.0,                     # score не определён
            risk_level=RiskLevel.medium,   # нейтрально
            decision=DecisionType.needs_info,
            conditions=None,
            factors=completeness,
            next_step=NEXT_STEP_MAP[DecisionType.needs_info],
            model_version="data-check-v1",
            fallback_used=False,
        )

    # 3. СКОРИНГ
    score, p_default, ml_factors, fallback_used = predict(req)

    # 4. ОБЪЕДИНЯЕМ ФАКТОРЫ
    factors = policy + ml_factors

    # 5. ПОРОГ
    decision = score_to_decision(score)

    # 6. УСЛОВИЯ
    conditions = None
    if decision == DecisionType.approved_with_conditions:
        conditions = build_conditions(req, score)

    a = req.application
    rate = None
    if decision == DecisionType.approved:
        rate = PRODUCT_LIMITS[a.product_id.value]["base_rate"]
    elif decision == DecisionType.approved_with_conditions:
        rate = conditions.rate

    # 7. MODEL_VERSION
    model_version = "fallback-v1" if fallback_used else get_model_version()

    return DecisionResponse(
        application_id=req.application_id,
        score=round(score, 1),
        risk_level=risk_level(score),
        decision=decision,
        conditions=conditions,
        rate=rate,
        factors=factors,
        next_step=NEXT_STEP_MAP[decision],
        model_version=model_version,
        fallback_used=fallback_used,
    )
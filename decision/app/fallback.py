from .schemas import CompanyType, DecisionRequest, Factor


# ============================================================
# КОНСТАНТЫ
# ============================================================

BASE_SCORE = 85.0

# Возраст: до +20 баллов (120 месяцев = 20 баллов)
AGE_MAX_POINTS = 20.0
AGE_MONTHS_PER_POINT = 6.0

# Долговая нагрузка: штраф до -30, бонус до +10
DEBT_LOAD_THRESHOLD = 0.3
DEBT_LOAD_PENALTY_PER_UNIT = 40.0
DEBT_LOAD_MAX_PENALTY = 30.0
DEBT_LOAD_BONUS_PER_UNIT = 30.0
DEBT_LOAD_MAX_BONUS = 12.0

# Loan to revenue: штраф до -25, бонус до +8
LTR_THRESHOLD = 0.2
LTR_PENALTY_PER_UNIT = 50.0
LTR_MAX_PENALTY = 25.0
LTR_BONUS_PER_UNIT = 40.0
LTR_MAX_BONUS = 10.0

# Просрочки: -6 за каждую, до -30
DELINQUENCY_PENALTY_PER = 6.0
DELINQUENCY_MAX_PENALTY = 30.0

# Использование кредитных линий: штраф до -20, бонус до +8
CREDIT_UTIL_THRESHOLD = 0.5
CREDIT_UTIL_PENALTY_PER_UNIT = 20.0
CREDIT_UTIL_MAX_PENALTY = 20.0
CREDIT_UTIL_BONUS_PER_UNIT = 20.0
CREDIT_UTIL_MAX_BONUS = 10.0

# Покрытие долга: штраф до -20, бонус до +10
DSC_THRESHOLD = 1.0
DSC_PENALTY_PER_UNIT = 20.0
DSC_MAX_PENALTY = 20.0
DSC_BONUS_PER_UNIT = 10.0
DSC_MAX_BONUS = 12.0

# Рентабельность: штраф до -15, бонус до +10
PROFIT_MARGIN_THRESHOLD = 0.0
PROFIT_MARGIN_PENALTY_PER_UNIT = 60.0
PROFIT_MARGIN_MAX_PENALTY = 15.0
PROFIT_MARGIN_BONUS_PER_UNIT = 60.0   # 0.15 маржи = +9
PROFIT_MARGIN_MAX_BONUS = 12.0

# Ликвидность: штраф до -10, бонус до +10
LIQUIDITY_THRESHOLD = 1.5
LIQUIDITY_PENALTY_PER_UNIT = 10.0
LIQUIDITY_MAX_PENALTY = 10.0
LIQUIDITY_BONUS_PER_UNIT = 4.0
LIQUIDITY_MAX_BONUS = 12.0

# Тип компании
IP_PENALTY = 5.0

# Налоговые задолженности
TAX_ARREARS_PENALTY = 8.0

# Отраслевые эффекты (те же, что в generator.INDUSTRY_EFFECT, но в баллах)
INDUSTRY_POINTS = {
    "it": 8.0,
    "services": 4.0,
    "trade": 0.0,
    "manufacturing": 2.0,
    "construction": -4.0,
    "food": -8.0,
}


# ============================================================
# FALLBACK SCORECARD
# ============================================================

def fallback_scorecard(req: DecisionRequest) -> tuple[float, list[Factor]]:
    """
    Возвращает (score_0_100, factors[]).

    Формулы производных — те же, что в generator.py (с тем же клиппингом).
    Scorecard симметрична: бонусы за хорошие значения, штрафы за плохие.
    """
    factors: list[Factor] = []
    score = BASE_SCORE

    c = req.company
    f = req.financials
    h = req.credit_history
    a = req.application

    # ---------- 1. ЖЁСТКИЕ СТОПЫ ----------
    if c.business_status.value != "active":
        factors.append(Factor(
            name="business_status", impact=-100.0, value=c.business_status.value
        ))
        return 0.0, factors

    # ---------- 2. ВОЗРАСТ (бонус) ----------
    age_pts = min(AGE_MAX_POINTS, c.age_months / AGE_MONTHS_PER_POINT)
    score += age_pts
    factors.append(Factor(name="company_age", impact=round(age_pts, 2), value=c.age_months))

    # ---------- 3. ДОЛГОВАЯ НАГРУЗКА (симметрично) ----------
    monthly_revenue = max(f.revenue_monthly, 1.0)
    debt_load = min(max(h.current_debt_payments / monthly_revenue, 0.0), 3.0)
    if debt_load > DEBT_LOAD_THRESHOLD:
        dl_effect = -min(
            DEBT_LOAD_MAX_PENALTY,
            (debt_load - DEBT_LOAD_THRESHOLD) * DEBT_LOAD_PENALTY_PER_UNIT,
        )
    else:
        dl_effect = min(
            DEBT_LOAD_MAX_BONUS,
            (DEBT_LOAD_THRESHOLD - debt_load) * DEBT_LOAD_BONUS_PER_UNIT,
        )
    score += dl_effect
    factors.append(Factor(name="debt_load", impact=round(dl_effect, 2), value=round(debt_load, 2)))

    # ---------- 4. LOAN TO REVENUE (симметрично) ----------
    ltr = a.amount / max(f.revenue_annual, 1.0)
    if ltr > LTR_THRESHOLD:
        ltr_effect = -min(LTR_MAX_PENALTY, (ltr - LTR_THRESHOLD) * LTR_PENALTY_PER_UNIT)
    else:
        ltr_effect = min(LTR_MAX_BONUS, (LTR_THRESHOLD - ltr) * LTR_BONUS_PER_UNIT)
    score += ltr_effect
    factors.append(Factor(name="loan_to_revenue", impact=round(ltr_effect, 2), value=round(ltr, 2)))

    # ---------- 5. ПРОСРОЧКИ (только штраф) ----------
    delinq_penalty = min(
        DELINQUENCY_MAX_PENALTY, h.delinquencies_12m * DELINQUENCY_PENALTY_PER
    )
    score -= delinq_penalty
    factors.append(Factor(
        name="delinquencies_12m", impact=round(-delinq_penalty, 2), value=h.delinquencies_12m
    ))

    # ---------- 6. ИСПОЛЬЗОВАНИЕ КРЕДИТНЫХ ЛИНИЙ (симметрично) ----------
    credit_util = min(max(h.total_debt_amount / max(f.revenue_annual * 0.5, 1.0), 0.0), 2.0)
    if credit_util > CREDIT_UTIL_THRESHOLD:
        cu_effect = -min(
            CREDIT_UTIL_MAX_PENALTY,
            (credit_util - CREDIT_UTIL_THRESHOLD) * CREDIT_UTIL_PENALTY_PER_UNIT,
        )
    else:
        cu_effect = min(
            CREDIT_UTIL_MAX_BONUS,
            (CREDIT_UTIL_THRESHOLD - credit_util) * CREDIT_UTIL_BONUS_PER_UNIT,
        )
    score += cu_effect
    factors.append(Factor(
        name="credit_line_utilization", impact=round(cu_effect, 2), value=round(credit_util, 2)
    ))

    # ---------- 7. ПОКРЫТИЕ ДОЛГА (симметрично) ----------
    dsc = (
        f.net_profit_monthly / f.current_debt_payments
        if f.current_debt_payments > 0 else 2.0
    )
    dsc = min(max(dsc, -2.0), 5.0)
    if dsc < DSC_THRESHOLD:
        dsc_effect = -min(DSC_MAX_PENALTY, (DSC_THRESHOLD - dsc) * DSC_PENALTY_PER_UNIT)
    else:
        dsc_effect = min(DSC_MAX_BONUS, (dsc - DSC_THRESHOLD) * DSC_BONUS_PER_UNIT)
    score += dsc_effect
    factors.append(Factor(
        name="debt_service_coverage", impact=round(dsc_effect, 2), value=round(dsc, 2)
    ))

    # ---------- 8. РЕНТАБЕЛЬНОСТЬ (симметрично) ----------
    profit_margin = (
        f.net_profit_monthly / f.revenue_monthly if f.revenue_monthly > 0 else 0.0
    )
    if profit_margin < PROFIT_MARGIN_THRESHOLD:
        pm_effect = -min(
            PROFIT_MARGIN_MAX_PENALTY,
            (PROFIT_MARGIN_THRESHOLD - profit_margin) * PROFIT_MARGIN_PENALTY_PER_UNIT,
        )
    else:
        pm_effect = min(PROFIT_MARGIN_MAX_BONUS, profit_margin * PROFIT_MARGIN_BONUS_PER_UNIT)
    score += pm_effect
    factors.append(Factor(
        name="profit_margin_calc", impact=round(pm_effect, 2), value=round(profit_margin, 3)
    ))

    # ---------- 9. ЛИКВИДНОСТЬ (симметрично) ----------
    liquidity = min(
        max((f.revenue_monthly * 3) / max(f.accounts_payable + f.current_debt_payments, 1.0), 0.0),
        5.0,
    )
    if liquidity < LIQUIDITY_THRESHOLD:
        liq_effect = -min(LIQUIDITY_MAX_PENALTY, (LIQUIDITY_THRESHOLD - liquidity) * LIQUIDITY_PENALTY_PER_UNIT)
    else:
        liq_effect = min(LIQUIDITY_MAX_BONUS, (liquidity - LIQUIDITY_THRESHOLD) * LIQUIDITY_BONUS_PER_UNIT)
    score += liq_effect
    factors.append(Factor(
        name="liquidity_ratio", impact=round(liq_effect, 2), value=round(liquidity, 2)
    ))

    # ---------- 10. ТИП КОМПАНИИ ----------
    if c.company_type == CompanyType.ip:
        score -= IP_PENALTY
        factors.append(Factor(name="company_type_ip", impact=-IP_PENALTY, value="ip"))

    # ---------- 11. НАЛОГОВЫЕ ЗАДОЛЖЕННОСТИ ----------
    if h.tax_arrears:
        score -= TAX_ARREARS_PENALTY
        factors.append(Factor(name="tax_arrears", impact=-TAX_ARREARS_PENALTY, value=True))

    # ---------- 12. ОТРАСЛЬ ----------
    industry_pts = INDUSTRY_POINTS.get(c.industry, 0.0)
    score += industry_pts
    factors.append(Factor(name="industry", impact=industry_pts, value=c.industry))

    # ---------- 13. КЛИППИНГ ----------
    score = max(0.0, min(100.0, score))
    return round(score, 1), factors
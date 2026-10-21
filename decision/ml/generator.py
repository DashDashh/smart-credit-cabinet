import numpy as np
import pandas as pd


# ---------- КОНСТАНТЫ ПРОДУКТОВ ----------
PRODUCTS = {
    "investment": {
        "min_amount": 500_000,
        "max_amount": 30_000_000,
        "terms": [12, 24, 36, 60],
        "min_age": 12,
        "base_rate": 15.0,
        "purposes": ["equipment", "expansion", "renovation"],
    },
    "working_capital": {
        "min_amount": 100_000,
        "max_amount": 5_000_000,
        "terms": [6, 12, 24],
        "min_age": 6,
        "base_rate": 17.5,
        "purposes": ["working_capital", "trade", "salary"],
    },
    "micro_loan": {
        "min_amount": 50_000,
        "max_amount": 300_000,
        "terms": [3, 6],
        "min_age": 3,
        "base_rate": 22.0,
        "purposes": ["working_capital", "marketing", "repair"],
    },
}


# ---------- ОТРАСЛЕВОЙ СДВИГ РИСКА ----------
INDUSTRY_EFFECT = {
    "it": -0.35,
    "services": -0.15,
    "trade": 0.0,
    "manufacturing": 0.10,
    "construction": 0.25,
    "food": 0.40,
}


def _sigmoid(z: np.ndarray) -> np.ndarray:
    return 1.0 / (1.0 + np.exp(-z))


def generate(
    n: int = 20_000,
    seed: int = 42,
    noise: float = 1.0,
    intercept: float = -4.7,
) -> pd.DataFrame:
    """
    Генерирует n строк синтетики.

    Параметры
    ----------
    n : int
        Количество строк.
    seed : int
        Seed для воспроизводимости.
    noise : float
        Сила шума в латентной переменной.
        1.0 — реалистично (AUC ~0.75–0.82),
        0.1 — модель выучит генератор наизусть (AUC ~0.99).
    intercept : float
        Базовая константа в латентной формуле. Крутите для настройки
        доли дефолтов: -3.3 → ~16%, -3.7 → ~13%, -4.1 → ~10%.
    """
    rng = np.random.default_rng(seed)

    # ============================================================
    # 1. КОМПАНИЯ
    # ============================================================
    company_type = rng.choice(["ip", "ooo"], size=n, p=[0.55, 0.45])
    industry = rng.choice(
        list(INDUSTRY_EFFECT),
        size=n,
        p=[0.15, 0.25, 0.25, 0.15, 0.12, 0.08],
    )
    # Возраст: gamma с перекосом к молодым (МСП)
    age_months = np.clip(
        rng.gamma(shape=2.0, scale=24.0, size=n), 1, 240
    ).astype(int)
    employees_count = np.clip(
        rng.lognormal(mean=1.5, sigma=1.0, size=n).astype(int), 0, 250
    )
    # Бизнес-статус: в основном активные, редко ликвидация
    business_status = rng.choice(
        ["active", "liquidating", "closed"], size=n, p=[0.95, 0.03, 0.02]
    )

    # ============================================================
    # 2. ФИНАНСЫ
    # ============================================================
    revenue_annual = np.exp(rng.normal(loc=16.0, scale=1.2, size=n))
    seasonality = rng.uniform(0.8, 1.2, size=n)
    revenue_monthly = revenue_annual / 12.0 * seasonality
    profit_margin = rng.normal(loc=0.08, scale=0.10, size=n)
    net_profit_monthly = revenue_monthly * profit_margin
    accounts_payable = revenue_monthly * rng.uniform(0.1, 0.8, size=n)
    active_loans_count = rng.poisson(lam=0.8, size=n)
    current_debt_payments = (active_loans_count + 1) * rng.uniform(
        20_000, 120_000, size=n
    )

    # ============================================================
    # 3. КРЕДИТНАЯ ИСТОРИЯ
    # ============================================================
    # Просрочки зависят от возраста и отрасли — закладываем корреляцию
    lam = np.maximum(
        0.05,
        1.2
        - 0.004 * age_months
        + np.array([INDUSTRY_EFFECT[i] for i in industry]) * 0.5,
    )
    delinquencies_12m = rng.poisson(lam)
    total_debt_amount = current_debt_payments * rng.uniform(12, 36, size=n)
    tax_arrears = rng.binomial(1, 0.07, size=n).astype(bool)

    # ============================================================
    # 4. ЗАЯВКА
    # ============================================================
    product_id = rng.choice(
        ["working_capital", "investment", "micro_loan"],
        size=n,
        p=[0.55, 0.30, 0.15],
    )
    amount = np.array(
        [
            rng.uniform(0.1, 0.5) * revenue_annual[i] * rng.uniform(0.5, 2.0)
            for i in range(n)
        ]
    )
    for i, p in enumerate(product_id):
        cfg = PRODUCTS[p]
        amount[i] = np.clip(amount[i], cfg["min_amount"], cfg["max_amount"])
    term_months = np.array([rng.choice(PRODUCTS[p]["terms"]) for p in product_id])
    purpose = np.array([rng.choice(PRODUCTS[p]["purposes"]) for p in product_id])

    # ============================================================
    # 5. ПРОИЗВОДНЫЕ ПРИЗНАКИ
    # ============================================================
    debt_load = np.clip(
        current_debt_payments / np.maximum(revenue_monthly, 1.0), 0, 3.0
    )
    loan_to_revenue = amount / np.maximum(revenue_annual, 1.0)
    revenue_per_employee = revenue_annual / np.maximum(employees_count, 1)
    profit_margin_calc = np.where(
        revenue_monthly > 0, net_profit_monthly / revenue_monthly, 0.0
    )

    credit_line_utilization = np.clip(
        total_debt_amount / np.maximum(revenue_annual * 0.5, 1.0), 0, 2.0
    )

    debt_service_coverage = np.where(
        current_debt_payments > 0,
        net_profit_monthly / current_debt_payments,
        2.0,
    )
    debt_service_coverage = np.clip(debt_service_coverage, -2.0, 5.0)

    liquidity_ratio = np.clip(
        (revenue_monthly * 3)
        / np.maximum(accounts_payable + current_debt_payments, 1.0),
        0,
        5.0,
    )

    # Логарифмируем скошенные признаки — стабилизирует вклад
    log_liquidity = np.log1p(liquidity_ratio)
    log_dsc = np.log1p(np.maximum(debt_service_coverage + 2.0, 0.0))  # сдвиг в >0

    # Центрируем по реальным медианам (а не по «красивым» числам)
    log_med_liquidity = np.log1p(np.median(liquidity_ratio))
    log_med_dsc = np.log1p(np.median(debt_service_coverage) + 2.0)

    # ============================================================
    # 6. ЛАТЕНТНЫЙ РИСК (веса из литературы)
    # ============================================================
    z = (
        intercept
        # --- КРЕДИТНАЯ ИСТОРИЯ (главный фактор, ~13% веса) ---
        + 0.70 * delinquencies_12m
        + 0.50 * (delinquencies_12m >= 2)
        + 0.30 * np.maximum(0, credit_line_utilization - 0.5)
        # --- ЛИКВИДНОСТЬ (двусторонняя, центрированная по медиане) ---
        - 0.25 * (log_liquidity - log_med_liquidity)
        - 0.30 * (log_dsc - log_med_dsc)
        # --- СРОК И СУММА (~8% веса) ---
        + 1.8 * (loan_to_revenue - 0.2)
        + 0.02 * np.maximum(0, term_months - 24)
        # --- РЕНТАБЕЛЬНОСТЬ (~6% веса) ---
        - 3.0 * profit_margin_calc
        # --- ВЫРУЧКА (развязывает ликвидность от дохода) ---
        - 0.25 * (np.log1p(revenue_monthly) - np.log1p(1_000_000))
        # --- ВОЗРАСТ И ТИП (умеренное влияние) ---
        - 0.006 * (age_months - 36)
        + 0.10 * (company_type == "ip")
        # --- ОТРАСЛЬ ---
        + np.array([INDUSTRY_EFFECT[i] for i in industry])
        # --- НЕЛИНЕЙНОСТИ (важны для демонстрации ценности ML) ---
        + 1.5 * np.maximum(0, debt_load - 0.6)
        + 0.8 * ((age_months < 12) & (debt_load > 0.4))
        # --- ЖЁСТКИЕ СТОПЫ ---
        + 5.0 * (np.array(business_status) != "active")
        + 0.4 * tax_arrears
        # --- ШУМ ---
        + rng.normal(0, noise, size=n)
    )
    p_default = _sigmoid(z)
    default = rng.binomial(1, p_default, size=n)

    # ============================================================
    # 7. СБОРКА DATAFRAME
    # ============================================================
    return pd.DataFrame(
        {
            # компания
            "company_type": company_type,
            "industry": industry,
            "age_months": age_months,
            "employees_count": employees_count,
            "business_status": business_status,
            # финансы
            "revenue_monthly": revenue_monthly,
            "revenue_annual": revenue_annual,
            "current_debt_payments": current_debt_payments,
            "net_profit_monthly": net_profit_monthly,
            "accounts_payable": accounts_payable,
            # кредитная история
            "delinquencies_12m": delinquencies_12m,
            "active_loans_count": active_loans_count,
            "total_debt_amount": total_debt_amount,
            "tax_arrears": tax_arrears,
            # заявка
            "product_id": product_id,
            "amount": amount,
            "term_months": term_months,
            "purpose": purpose,
            # производные
            "debt_load": debt_load,
            "loan_to_revenue": loan_to_revenue,
            "revenue_per_employee": revenue_per_employee,
            "profit_margin_calc": profit_margin_calc,
            "credit_line_utilization": credit_line_utilization,
            "debt_service_coverage": debt_service_coverage,
            "liquidity_ratio": liquidity_ratio,
            # таргет
            "default": default,
        }
    )


if __name__ == "__main__":
    # Прогон для проверки
    df = generate(n=20_000, seed=42, noise=1.0, intercept=-4.5)
    print("Shape:", df.shape)
    print("Доля дефолтов:", round(df["default"].mean(), 4))

    print("\nКорреляции с default (топ-10 по модулю):")
    corr = (
        df.select_dtypes(include=[np.number])
        .corr()["default"]
        .drop("default")
    )
    print(corr.abs().sort_values(ascending=False).head(10))

    print("\nЗнаковые корреляции (должны быть: + для риска, − для защиты):")
    for col in ["debt_load", "delinquencies_12m", "credit_line_utilization",
                "liquidity_ratio", "debt_service_coverage", "profit_margin_calc"]:
        print(f"  {col:25s} {df[col].corr(df['default']):+.3f}")

    print("\nКорреляция credit_line_utilization и debt_load:")
    print(round(df["credit_line_utilization"].corr(df["debt_load"]), 3))

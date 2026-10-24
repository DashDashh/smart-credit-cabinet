# Как принимается решение

Сервис `decision` проходит **пять этапов** в строгом порядке. Каждый этап может **остановить** конвейер — дальнейшие не выполняются.

```
┌─────────────────────────────────────────────────────────────┐
│ 1. policy_checks          — жёсткие стопы                   │
│    если hard_block → rejected (model_version=policy-v1)     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. check_data_completeness — полнота данных                 │
│    если incomplete → needs_info (model_version=data-check-v1)│
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. scoring.predict         — ML или fallback                │
│    score 0–100, factors[]                                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. score_to_decision       — пороги 65 / 45                 │
│    >= 65 → approved                                         │
│    >= 45 → approved_with_conditions                         │
│    <  45 → rejected                                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. build_conditions        — если approved_with_conditions  │
│    сумма 70%, ближайший срок, ставка = base + premium       │
└─────────────────────────────────────────────────────────────┘
```

## Эндпоинты

| Метод | Путь | Назначение | Вход | Выход | Коды |
|---|---|---|---|---|---|
| `POST` | `/decision` | Принять решение по заявке | `DecisionRequest` (JSON) | `DecisionResponse` (JSON) | 200, 422, 500 |
| `GET` | `/health` | Статус сервиса | — | `{status, ml_available, model_version, load_error}` | 200 |
| `GET` | `/` | Корень (проверка живости) | — | `{service, version, docs}` | 200 |

### `POST /decision`

**Вход (`DecisionRequest`):**

| Поле | Тип | Описание |
|---|---|---|
| `application_id` | ApplicationId | ID заявки |
| `company` | `Company` | Данные компании |
| `financials` | `Financials` | Финансы |
| `credit_history` | `CreditHistory` | Кредитная история |
| `application` | `Application` | Заявка |

**`Company`:**

| Поле | Тип | Описание |
|---|---|---|
| `company_type` | `ip` / `ooo` | Юридическая форма |
| `industry` | `str` | ОКВЭД-отрасль: `it`, `services`, `trade`, `manufacturing`, `construction`, `food` |
| `age_months` | `int` | Сколько месяцев существует бизнес |
| `employees_count` | `int` | Штат |
| `business_status` | `active` / `liquidating` / `closed` | Статус |

**`Financials`:**

| Поле | Тип | Описание |
|---|---|---|
| `revenue_monthly` | `float` | Месячная выручка |
| `revenue_annual` | `float` | Годовая выручка |
| `current_debt_payments` | `float` | Платежи по кредитам в месяц |
| `net_profit_monthly` | `float` | Чистая прибыль (может быть отрицательной) |
| `accounts_payable` | `float` | Обязательства перед поставщиками |

**`CreditHistory`:**

| Поле | Тип | Описание |
|---|---|---|
| `delinquencies_12m` | `int` | Просрочки за 12 месяцев |
| `active_loans_count` | `int` | Активные кредиты |
| `total_debt_amount` | `float` | Общая сумма обязательств |
| `tax_arrears` | `bool` | Налоговые задолженности |

**`Application`:**

| Поле | Тип | Описание |
|---|---|---|
| `product_id` | `investment` / `working_capital` / `micro_loan` | Продукт |
| `amount` | `float` | Сумма |
| `term_months` | `int` | Срок |
| `purpose` | `str` | Цель |

**Выход (`DecisionResponse`):**

| Поле | Тип | Описание |
|---|---|---|
| `application_id` | `ApplicationId` | ID заявки
| `score` | `float` (0–100) | Скор: 100 — без риска, 0 — максимальный |
| `risk_level` | `low` / `medium` / `high` | Уровень риска |
| `decision` | `approved` / `approved_with_conditions` / `needs_info` / `rejected` | Решение |
| `conditions` | `Conditions \| null` | Условия (если `approved_with_conditions`) |
| `rate` | `float \| null` | Ставка (для `approved` и `approved_with_conditions`) |
| `factors` | `list[Factor]` | Факторы с `name`, `impact`, `value`, `description` |
| `next_step` | `str` | Маппинг на состояние заявки в Java |
| `model_version` | `str` | `logreg-v1` / `fallback-v1` / `policy-v1` / `data-check-v1` |
| `fallback_used` | `bool` | `true`, если ML недоступна |

**Коды ответа:**

- `200` — решение принято;
- `422` — невалидный `DecisionRequest` (Pydantic);
- `500` — внутренняя ошибка.

## Этап 1: `policy_checks` — жёсткие стопы

**Что проверяется:**

| Проверка | Условие | `impact` | `description` |
|---|---|---|---|
| Бизнес-статус | `business_status != "active"` | −100 | «Компания в статусе 'X' — кредитование невозможно» |
| Возраст vs продукт | `age_months < min_age_months` | −100 | «Возраст N мес. меньше минимума (M мес.)» |
| Сумма vs лимит | `amount > max_amount` | −100 | «Сумма X ₽ превышает лимит (Y ₽)» |
| Срок vs допустимые | `term_months not in allowed_terms` | −100 | «Срок X мес. не входит в допустимые: [..]» |
| Налоговые задолженности | `tax_arrears == true` | −15 | «Есть налоговая задолженность — повышает риск» |
| Отрицательная прибыль при долге | `net_profit_monthly < 0 and current_debt_payments > 0` | −20 | «Отрицательная прибыль при наличии долга — риск кассового разрыва» |

**Правило:** если **хоть один** фактор имеет `impact <= -100` → `hard_block`.

**Результат `hard_block`:**

```json
{
  "score": 0.0,
  "risk_level": "high",
  "decision": "rejected",
  "conditions": null,
  "rate": null,
  "factors": [{"name": "business_status_not_active", "impact": -100.0, "value": "liquidating", "description": "Компания в статусе 'liquidating' — кредитование невозможно"}],
  "next_step": "notify_client_rejection",
  "model_version": "policy-v1",
  "fallback_used": false
}
```

**ML не вызывается** — экономим ресурсы и не даём «мусорный» score.

## Этап 2: `check_data_completeness` — полнота данных

**Что проверяется:**

| Проверка | Условие | `impact` |
|---|---|---|
| Месячная выручка | `revenue_monthly is None or <= 0` | −100 |
| Годовая выручка | `revenue_annual is None or <= 0` | −100 |
| Чистая прибыль | `net_profit_monthly is None` | −100 |
| Согласованность выручки | `revenue_annual / (revenue_monthly * 12)` вне [0.5, 2.0] | −100 |
| Сумма vs выручка | `amount > revenue_annual * 5` | −100 |
| Возраст | `age_months is None or < 0` | −100 |
| Срок | `term_months is None or <= 0` | −100 |
| Сумма | `amount is None or <= 0` | −100 |
| Просрочки без долга | `delinquencies_12m > 0 and total_debt_amount == 0` | −100 |

**Правило:** если **хоть один** фактор имеет `impact <= -100` → `incomplete`.

**Результат `incomplete`:**

```json
{
  "score": 0.0,
  "risk_level": "medium",
  "decision": "needs_info",
  "conditions": null,
  "rate": null,
  "factors": [{"name": "missing_revenue_monthly", "impact": -100.0, "value": 0.0, "description": "Укажите месячную выручку — без неё невозможна оценка долговой нагрузки"}],
  "next_step": "bank_request_info",
  "model_version": "data-check-v1",
  "fallback_used": false
}
```

**ML не вызывается** — данных недостаточно для оценки.

**Важно:** `needs_info` — **не порог score**, а результат data-check. Это исправление концептуальной ошибки: раньше `needs_info` был на шкале score (30–45), что смешивало «средний риск» и «неполные данные».

## Этап 3: `scoring.predict` — ML или fallback

**Что происходит:**

1. Собираются признаки из `DecisionRequest` + производные (`debt_load`, `loan_to_revenue`, `credit_line_utilization`, `debt_service_coverage`, `liquidity_ratio`, `profit_margin_calc`, `revenue_per_employee`).
2. Если ML доступна (`model.pkl` загружен) — `predict_proba` калиброванной логистической регрессии → `p_default`.
3. `score = (1 - p_default) * 100`.
4. Извлекаются `factors[]` (топ-5 по модулю вклада в логит) с `description`.

**Если ML недоступна** — вызывается `fallback_scorecard` (детерминированная балльная модель). `fallback_used = true`.

**Возвращает:** `(score, p_default, factors, fallback_used)`.

## Этап 4: `score_to_decision` — пороги

| `score` | `decision` | `risk_level` | `next_step` |
|---|---|---|---|
| `>= 65` | `approved` | `low` | `client_accept_terms` |
| `45 <= score < 65` | `approved_with_conditions` | `medium` | `client_accept_conditions` |
| `< 45` | `rejected` | `high` | `notify_client_rejection` |

**Важно:** `needs_info` **не** в этой таблице — он приходит из этапа 2.

## Этап 5: `build_conditions` — условия

**Только для `approved_with_conditions`.**

| Параметр | Формула | Пример |
|---|---|---|
| `approved_amount` | `min(amount * 0.7, max_amount)` | 2 000 000 → 1 400 000 |
| `term_months` | `min(allowed_terms, key=abs(t - term_months))` | 12 → 12 |
| `rate` | `base_rate + max(0, (65 - score) / 10) * 1.5` | score=58 → 17.5 + 1.05 = 18.6 |
| `reason` | `"amount_above_policy_or_risk_level"` | — |

## Матрица решений (сводка)

| Этап | Триггер | `decision` | `model_version` | ML вызвана? |
|---|---|---|---|---|
| 1. policy_checks | `impact <= -100` | `rejected` | `policy-v1` | ❌ |
| 2. data_completeness | `impact <= -100` | `needs_info` | `data-check-v1` | ❌ |
| 3–4. scoring + пороги | `score >= 65` | `approved` | `logreg-v1` / `fallback-v1` | ✅ |
| 3–4. scoring + пороги | `45 <= score < 65` | `approved_with_conditions` | `logreg-v1` / `fallback-v1` | ✅ |
| 3–4. scoring + пороги | `score < 45` | `rejected` | `logreg-v1` / `fallback-v1` | ✅ |

## Как читать `factors`

**`impact`** — вклад в **риск дефолта**:

- `impact < 0` — **снижает** риск (хорошо);
- `impact > 0` — **повышает** риск (плохо);
- `impact <= -100` — **жёсткий стоп** (hard_block) или **неполнота данных**.

**`description`** — человекочитаемое объяснение для клиента. Примеры:

- `debt_load` → «Долговая нагрузка: доля ежемесячных платежей по кредитам в выручке»;
- `missing_revenue_monthly` → «Укажите месячную выручку — без неё невозможна оценка долговой нагрузки»;
- `business_status_not_active` → «Компания в статусе 'liquidating' — кредитование невозможно».


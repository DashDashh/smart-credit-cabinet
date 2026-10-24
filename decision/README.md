# Decision Engine

Микросервис скоринга и принятия решения по кредитным заявкам МСП.

## Что делает

Принимает `DecisionRequest` (данные компании, финансы, кредитная история, параметры заявки),
возвращает `DecisionResponse` (score 0–100, decision, factors, conditions, rate).

Решения:

- `approved` — одобрено;
- `approved_with_conditions` — одобрено с изменёнными условиями (сумма/срок/ставка);
- `needs_info` — нужна доработка (неполные или несогласованные данные);
- `rejected` — отказ (ML-риск или жёсткий стоп).

Плюс: `factors[]` с человекочитаемым `description` — клиент и сотрудник видят, **почему** такое решение.

## Быстрый старт

### Docker

```bash
docker compose up --build
```

Сервис: http://localhost:8000

### Локально

```bash
cd decision

# 1. Виртуальное окружение
python -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate

# 2. Зависимости
pip install -r requirements.txt

# 3. Сгенерировать синтетику (20 000 строк, seed 42)
python ml/make_data.py --n 20000 --seed 42 --intercept -4.7

# 4. Обучить модель (LogReg + калибровка)
python ml/train.py

# 5. Запустить сервис
uvicorn app.main:app --reload --port 8000
```

Проверка: `curl http://localhost:8000/health`.

## Swagger / OpenAPI

- **Swagger UI:** http://localhost:8000/docs
- **ReDoc:** http://localhost:8000/redoc
- **OpenAPI JSON:** http://localhost:8000/openapi.json

## API

| Метод | Путь | Назначение | Вход | Выход | Коды |
|---|---|---|---|---|---|
| `POST` | `/decision` | Принять решение по заявке | `DecisionRequest` | `DecisionResponse` | 200, 422, 500 |
| `GET` | `/health` | Статус сервиса | — | `{status, ml_available, model_version, load_error}` | 200 |
| `GET` | `/` | Корень (проверка живости) | — | `{service, version, docs}` | 200 |

### Пример: `POST /decision`

```bash
curl -X POST http://localhost:8000/decision \
  -H "Content-Type: application/json" \
  -d @examples/sample_approved.json
```

Ответ:

```json
{
  "score": 99.8,
  "risk_level": "low",
  "decision": "approved",
  "conditions": null,
  "rate": 17.5,
  "factors": [
    {
      "name": "business_status=active",
      "impact": -25.93,
      "value": "active",
      "description": "Действующий бизнес — снижает риск"
    },
    {
      "name": "delinquencies_12m",
      "impact": -8.13,
      "value": 0,
      "description": "Количество просрочек за последние 12 месяцев"
    },
    {
      "name": "debt_load",
      "impact": -4.16,
      "value": 0.0667,
      "description": "Долговая нагрузка: доля платежей по кредитам в выручке"
    }
  ],
  "next_step": "client_accept_terms",
  "model_version": "logreg-v1",
  "fallback_used": false
}
```

### Пример: `GET /health`

```bash
curl http://localhost:8000/health
```

```json
{
  "status": "ok",
  "ml_available": true,
  "model_version": "logreg-v1",
  "load_error": null
}
```


## Как принимается решение

5 этапов в строгом порядке:

1. **`policy_checks`** — жёсткие стопы (бизнес-статус, возраст vs продукт, сумма vs лимит, срок vs допустимые). Если сработал → `rejected`, ML **не вызывается**.
2. **`check_data_completeness`** — полнота и корректность данных (выручка, прибыль, согласованность, просрочки без долга). Если не хватает → `needs_info`, ML **не вызывается**.
3. **`scoring.predict`** — ML-модель (калиброванная LogReg) или fallback-scorecard. Возвращает `score` 0–100 и `factors[]`.
4. **`score_to_decision`** — пороги: `>= 65` → `approved`, `>= 45` → `approved_with_conditions`, `< 45` → `rejected`.
5. **`build_conditions`** — если `approved_with_conditions`: сумма 70% от запрошенной, ближайший срок, ставка = базовая + премия за риск.

## Компоненты

| Файл | Назначение |
|---|---|
| `app/main.py` | FastAPI: эндпоинты, middleware, lifespan, CORS |
| `app/schemas.py` | Pydantic-модели (`DecisionRequest`, `DecisionResponse`, `Factor`, `Conditions`, `DecisionType`) |
| `app/scoring.py` | Загрузка модели, `predict_proba`, извлечение `factors[]` с `description` |
| `app/fallback.py` | Детерминированная scorecard, если ML недоступна |
| `app/rules.py` | `policy_checks`, `check_data_completeness`, `score_to_decision`, `build_conditions`, `decide` |
| `ml/generator.py` | Синтетика: латентная переменная + шум, веса из литературы (BWM+TOPSIS, Dainelli et al.) |
| `ml/make_data.py` | CLI: генерация CSV (`--n`, `--seed`, `--intercept`, `--out`) |
| `ml/train.py` | Обучение LogReg + `CalibratedClassifierCV`, метрики в `metrics.json` |

## Метрики модели

| Метрика | Baseline (`debt_load`) | Full (23 признака) |
|---|---|---|
| ROC-AUC | 0.692 | **0.873** |
| PR-AUC | 0.339 | **0.614** |
| Brier | 0.207 | **0.071** |
| Precision (default) | 0.272 | **0.757** |
| Recall (default) | 0.446 | 0.410 |
| F1 (default) | 0.338 | **0.532** |

Прирост ROC-AUC: **+0.18** (мультифакторная модель оправдана).

Fallback согласован с ML: Spearman **0.852**.

Метрики обновляются при `python ml/train.py` → `ml/artifacts/metrics.json`.


## Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `MODEL_PATH` | `ml/artifacts/model.pkl` | Путь к модели |
| `FEATURES_PATH` | `ml/artifacts/features.pkl` | Путь к спискам признаков |
| `METRICS_PATH` | `ml/artifacts/metrics.json` | Путь к метрикам |
| `APPROVE_THRESHOLD` | `65.0` | Порог `approved` |
| `CONDITIONS_THRESHOLD` | `45.0` | Порог `approved_with_conditions` |

Если `model.pkl` не найден — сервис **не падает**, а использует `fallback.py` (scorecard). `GET /health` покажет `ml_available: false`.

## Ограничения

- **Синтетика** — не реальные данные БКИ/ФНС.
- **LLM для документов** — не в MVP. Следующий шаг — гибрид: парсер + LLM как fallback для нестандартных документов.
- **`rate`** — возвращается для `approved` (базовая) и `approved_with_conditions` (base + premium). Для `needs_info` / `rejected` — `null`.
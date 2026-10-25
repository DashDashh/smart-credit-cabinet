# Smart Credit Cabinet

Прототип цифрового кредитного конвейера для малого и микробизнеса.
Хакатон «Альфа-Хак 2026».

## Что это

Клиент (предприниматель) подаёт заявку на кредит, банк обрабатывает её и принимает решение:
одобрено, одобрено с условиями, нужна доработка или отказ. Конвейер объединяет
клиентский путь и внутреннюю обработку заявки в единый процесс.

## Быстрый старт

```bash
docker compose up --build
```

После запуска:

| Сервис | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger (Backend API) | http://localhost:8080/swagger-ui/index.html# |
| Decision Engine | http://localhost:8000 |
| Swagger (decision) | http://localhost:8000/docs |

## Как принимается решение

5 этапов в строгом порядке:

1. **`policy_checks`** — жёсткие стопы (бизнес-статус, возраст vs продукт, сумма vs лимит, срок vs допустимые). Если сработал → `rejected`, ML **не вызывается**.
2. **`check_data_completeness`** — полнота и корректность данных. Если не хватает → `needs_info`, ML **не вызывается**.
3. **`scoring.predict`** — ML-модель (калиброванная логистическая регрессия) или fallback-scorecard. Возвращает `score` 0–100 и `factors[]`.
4. **`score_to_decision`** — пороги: `>= 65` → `approved`, `>= 45` → `approved_with_conditions`, `< 45` → `rejected`.
5. **`build_conditions`** — если `approved_with_conditions`: сумма 70% от запрошенной, ближайший срок, ставка = базовая + премия за риск.

## Метрики модели

| Метрика | Baseline (`debt_load`) | Full (23 признака) |
|---|---|---|
| ROC-AUC | 0.692 | **0.873** |
| PR-AUC | 0.339 | **0.614** |
| Brier | 0.207 | **0.071** |

Прирост ROC-AUC: **+0.18**. Fallback согласован с ML: Spearman **0.852**.

## Структура репозитория

```
smart-credit-cabinet/
├── README.md                    # этот файл
├── docker-compose.yml           # все сервисы
├── .env.example                 # переменные окружения
├── docs/
│   ├── ARCHITECTURE.md          # как сервисы связаны
│   ├── DECISION_LOGIC.md        # 5 этапов, матрица решений
│   ├── DEMO_SCENARIO.md         # 9 сценариев с шагами
│   ├── API.md                   # эндпоинты и примеры
│   ├── RESEARCH.md              # обоснование модели
│   ├── DATA.md                  # как генерируются данные
│   ├── MODEL_CARD.md            # model card
│   └── TESTING.md               # как проверить
├── decision/                    # Decision Engine (Python)
│   ├── README.md
│   ├── app/
│   ├── ml/
│   ├── Dockerfile
│   └── requirements.txt
├── backend/                     # Backend (Java)
│   ├── README.md
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
└── client/                      # Frontend (React)
    ├── README.md
    ├── src/
    ├── Dockerfile
    └── package.json
```

## Документация

- [Архитектура](docs/ARCHITECTURE.md) — как сервисы связаны
- [Логика принятия решения](docs/DECISION_LOGIC.md) — 5 этапов, матрица решений
- [Демо-сценарии](docs/DEMO_SCENARIO.md) — 9 сценариев с шагами
- [Research](docs/RESEARCH.md) — обоснование модели
- [Decision Engine](decision/README.md) — детали сервиса
- [Backend и БД](docs/BACK.md) - детали сервиса и схема БД
- [Frontend](docs/FRONTEND.md) - архитектура, маршруты и интеграция с API

## Ограничения (MVP)

- **Синтетика**, не реальные данные БКИ/ФНС. Моки интеграций — в backend.
- **LLM для документов** — не в MVP (next step).
- **Продуктовые лимиты** — в коде, не в БД (next step — `products.yaml`).

## Команда

| Роль | Зона ответственности |
|---|---|
| **Frontend** | ЛК клиента, ЛК сотрудника, отображение факторов и условий |
| **Backend** | Оркестратор, состояния заявки, моки ФНС/БКИ, БД |
| **Data Science** | Скоринг, decision engine, синтетика, метрики |

## Дальнейшие шаги

- GBM (LightGBM) как эксперимент — проверить прирост ROC-AUC.
- `DataCheckResult` в контракте — вынести data-check в отдельное поле.
- `products.yaml` — общие продуктовые лимиты для Java и Python.
- LLM для извлечения данных из документов (гибрид: парсер + LLM).
- Альтернативный скоринг (цифровой след, обороты по счёту).

---

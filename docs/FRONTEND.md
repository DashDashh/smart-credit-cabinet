# Frontend

Frontend приложения находится в `client/` и представляет собой React-приложение
на TypeScript, собираемое Vite и раздаваемое Nginx.

## Быстрый запуск

Из корня проекта:

```bash
docker compose up --build
```

После запуска:

- приложение: `http://localhost:3000`;
- backend API: `http://localhost:8080`;
- локальная разработка: `cd client && npm install && npm run dev`.

Для локального dev-сервера Vite запросы `/api` проксируются на
`http://localhost:8080`.

## Архитектура

Текущая структура следует части FSD без избыточного усложнения:

```text
client/src/
├── pages/
│   ├── AuthPage.tsx
│   ├── DashboardPage.tsx
│   ├── CatalogPage.tsx
│   └── ApplicationsPage.tsx
├── shared/
│   ├── api/index.ts
│   └── types/index.ts
├── App.tsx
├── App.css
└── main.tsx
```

- `pages` содержит законченные пользовательские сценарии;
- `shared/api` содержит типизированный HTTP-клиент и Basic Auth;
- `shared/types` содержит общие DTO и enum-типы;
- `App.tsx` отвечает за сессию, защищённые маршруты и role-based навигацию.

## Маршруты

| Путь | Назначение |
|---|---|
| `/login` | Авторизация |
| `/register` | Регистрация предпринимателя |
| `/profile` | Кабинет предпринимателя или заявки сотрудника банка |
| `/catalog` | Каталог кредитных продуктов |

Роль берётся из `AccountResponse.role` после `GET /api/accounts/me`.

- `ENTREPRENEUR`: профиль компании, финансовые данные, кредитная история,
  документы и собственные заявки;
- `BANK_EMPLOYEE`: входящие заявки, скоринговые решения, смена статусов и
  CRUD кредитных продуктов;
- другие роли используют базовый кабинет до появления отдельного сценария.

## Работа с backend API

### Сессия и профиль

```text
POST /api/auth/login
POST /api/auth/register
GET  /api/accounts/me
```

Сессия хранится в `localStorage` под ключом `alpha-credit-session`. Пароль
нужен клиенту для формирования Basic Auth заголовка при последующих запросах.

### Компании предпринимателя

```text
GET  /api/companies/my
GET  /api/companies/{id}
POST /api/companies
PUT  /api/companies/{id}
```

В кабинете есть форма создания и редактирования компании. Поля формы
соответствуют `CreateCompanyRequest` и `UpdateCompanyRequest`.

### Финансовые данные и кредитная история

```text
GET  /api/companies/{companyId}/financial-data
POST /api/companies/{companyId}/financial-data
PUT  /api/companies/{companyId}/financial-data

GET  /api/companies/{companyId}/credit-history
POST /api/companies/{companyId}/credit-history
PUT  /api/companies/{companyId}/credit-history
```

Фронтенд показывает полноту данных и позволяет создать или обновить оба
набора данных перед подачей заявки.

### Продукты

```text
GET  /api/products
GET  /api/products/{id}
POST /api/products              # BANK_EMPLOYEE
PUT  /api/products/{id}         # BANK_EMPLOYEE
```

Предприниматель выбирает продукт из backend-каталога. Сотрудник банка может
создать или отредактировать продукт из UI каталога.

### Заявки

```text
POST  /api/applications
GET   /api/applications
GET   /api/applications/{clientId}
GET   /api/applications/{productId}/{clientId}
PUT   /api/applications/{productId}/{clientId}
PATCH /api/applications/{productId}/{clientId}/status
GET   /api/applications/{productId}/{clientId}/withDecision
```

Предприниматель подаёт заявку из карточки продукта и видит свои заявки в
кабинете. Сотрудник банка получает список заявок, фильтрует его по статусу,
меняет статус и открывает результат скорингового решения.

В окне решения отображаются:

- score и уровень риска;
- решение модели;
- ставка и следующий шаг;
- условия одобрения;
- версия модели и признак fallback-скоринга.

## Документы предпринимателя

Document API в текущем backend отсутствует, поэтому frontend не показывает
фиктивный загрузчик и не сохраняет файлы в `localStorage`. Интерфейс будет
добавлен после появления backend-контракта для multipart-загрузки, списка
документов и серверных статусов проверки.

## Проверка

```bash
cd client
npm run lint
npm run build
```

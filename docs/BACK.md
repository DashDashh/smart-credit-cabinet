# Smart Credit Cabinet — Backend

Сервис кредитного скоринга для малого и среднего бизнеса. Принимает заявки на кредитные продукты, собирает данные о компании, финансах и кредитной истории, отправляет их в ML-сервис и сохраняет решение (скоринг, риск-уровень, условия). Предоставляет REST API для клиентов (предпринимателей), банковских сотрудников и администраторов.

## Стек

| Компонент | Версия |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.1 |
| Spring Framework | 7.0.9 |
| Spring Security | 7.1.1 |
| Spring Data JPA | 4.1.1 |
| Hibernate ORM | 7.4.5.Final |
| PostgreSQL | 17 (alpine) |
| Jackson | 3.1.5 (`tools.jackson.*`) |
| Lombok | 1.18.46 |
| springdoc-openapi | 3.1.1 |
| Maven | 3.9.x |

### Ключевые зависимости `pom.xml`

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-webmvc`
- `spring-boot-starter-security`
- `spring-boot-starter-validation`
- `spring-boot-http-client` (для `RestClient` / `HttpClientSettings`)
- `postgresql` (runtime)
- `lombok` (optional)
- `springdoc-openapi-starter-webmvc-ui:3.1.1`

## Swagger UI

```
http://localhost:8080/swagger-ui/index.html#/
```

JSON-спецификация: `http://localhost:8080/v3/api-docs`

## Схема базы данных

<img width="1958" height="1282" alt="Untitled (1)" src="https://github.com/user-attachments/assets/714831f3-3993-44f9-8053-88509653e65a" />


### Связи

| Связь | Тип | Описание |
|---|---|---|
| `account` → `company` | 1 : N | Один предприниматель владеет несколькими компаниями |
| `company` → `financial_data` | 1 : 1 | Финансовые показатели компании |
| `company` → `credit_history` | 1 : 1 | Кредитная история компании |
| `product` → `application` | 1 : N | Заявки по продукту |
| `company` → `application` | 1 : N | Заявки компании (клиента) |
| `application` → `decision` | 1 : 1 | Решение ML-сервиса по заявке |
| `decision` → `decision_conditions` | 1 : 0..1 | Условия одобрения (если `approved_with_conditions`) |

## Роли и доступ

| Роль | Возможности |
|---|---|
| `ENTREPRENEUR` | Создание/редактирование своих компаний, финансовых данных, кредитной истории; подача заявок; просмотр своих заявок; смена статуса `PENDING → CLOSED`/` APPROVED → ACTIVE` |
| `BANK_EMPLOYEE` | Создание/редактирование продуктов; просмотр всех заявок; смена статуса `PENDING → APPROVED`/`REJECTED` |
| `ADMIN` | Просмотр всех заявок; смена статуса `→ CLOSED` |

Аутентификация — сессионная (`JSESSIONID`), через `AuthenticationManager` и `AccountService implements UserDetailsService`. Регистрация и логин — `/api/auth/register`, `/api/auth/login`.

## Интеграция с ML-сервисом

- **Health-check**: `GET http://cabinet-ml:8000/health` → `{status, ml_available, model_version, load_error}`. Если `status != "ok"` — выбрасывается `503`.
- **Скоринг**: `POST http://cabinet-ml:8000/decision` с `DecisionRequest` (компания, финансы, кредитная история, заявка). Ответ — `DecisionResponse` (score, risk_level, decision, conditions, rate, factors, next_step, model_version, fallback_used).
- **Асинхронность**: запрос выполняется в `CompletableFuture.runAsync`, с `RetryTemplate` (3 попытки, экспоненциальная задержка).
- **Таймауты**: connect 3s, read 10s (`HttpClientSettings`).

## Mock Интеграция c сервисом кредитных историй
- Подразумевается обращение к сервису кредитных историй для проверки данных введенных пользователем
- Сейчас возвращает аппрув для всех

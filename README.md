## 🚀 Explore With Me — микросервисная архитектура

Проект представляет собой переработанную версию монолитного приложения "Explore With Me", 
разбитую на микросервисы для повышения масштабируемости, гибкости и удобства поддержки. 
Каждый микросервис отвечает за определённую бизнес-область и взаимодействует с другими через 
внутренние REST API с использованием OpenFeign.

> Приложение позволяет пользователям делиться информацией об интересных событиях и находить компанию для участия в них.

---
## Архитектура

### Микросервисы бизнес-логики (core)

| Сервис | Назначение |
|--------|-----------|
| `event-service` | Управление событиями: создание, публикация, поиск, обновление |
| `category-service` | Управление категориями событий |
| `user-service` | Администрирование пользователей |
| `request-service` | Обработка заявок на участие в событиях |
| `compilation-service` | Управление подборками событий |
| `comment-service` | Работа с комментариями к событиям |

### Инфраструктурные сервисы (infra)

| Сервис | Назначение |
|--------|-----------|
| `config-server` | Централизованное хранение конфигураций всех микросервисов |
| `discovery-server` (Eureka) | Сервис обнаружения и регистрации микросервисов |
| `gateway-server` | Единая точка входа для внешних клиентов (порт 8080), маршрутизация запросов |

### Сервис статистики (stats-service)

| Модуль | Назначение |
|--------|-----------|
| `stats-server` | Сбор и предоставление статистики просмотров событий |
| `stats-client` | Клиент для взаимодействия со stats-server (используется другими микросервисами) |
| `stats-dto` | Общие DTO для статистики |

### Общие модули

| Модуль | Назначение |
|--------|-----------|
| `common` | Общие DTO, исключения и глобальный обработчик ошибок |


---

## Внутреннее API

Взаимодействие между микросервисами осуществляется через **OpenFeign** с использованием **Eureka** для обнаружения сервисов. Для повышения отказоустойчивости реализованы **fallback-методы** и настроен **Resilience4j** (Circuit Breaker + Retry).

### event-service вызывает другие сервисы

**CategoryClient:**

`GET /internal/categories/{catId}` → `CategoryDto`

**UserClient:**

`GET /internal/users/{userId}` → `UserDto`

`GET /internal/users/{userId}/exists` → `Boolean`

**RequestClient:**

`GET /internal/requests/event/{eventId}?userId={userId}` → `List<ParticipationRequestDto>`

`PATCH /internal/requests/event/{eventId}/status` → `EventRequestStatusUpdateResult`

`GET /internal/requests/event/{eventId}/count?status={status}` → `Long`

### event-service предоставляет другим сервисам

**InternalEventController:**

`GET /internal/events/{eventId}/exists` → `Boolean`

`GET /internal/events/category/{categoryId}/exists` → `Boolean`

`GET /internal/events/{eventId}/published` → `Boolean`

`GET /internal/events/{eventId}/initiator/{userId}` → `Boolean`

`GET /internal/events/{eventId}/participantLimit` → `Integer`

`GET /internal/events/{eventId}/requestModeration` → `Boolean`

`GET /internal/events/{eventId}/short` → `EventShortDto`

### category-service предоставляет другим сервисам

**InternalCategoryController:**

`GET /internal/categories/{catId}` → `CategoryDto`

### user-service предоставляет другим сервисам

**InternalUserController:**

`GET /internal/users/{userId}/exists` → `Boolean`

`GET /internal/users/{userId}/name` → `String`

`GET /internal/users/{userId}` → `UserDto`

### request-service предоставляет другим сервисам

**InternalRequestController:**

`GET /internal/requests/event/{eventId}?userId={userId}` → `List<ParticipationRequestDto>`

`PATCH /internal/requests/event/{eventId}/status` → `EventRequestStatusUpdateResult`

`GET /internal/requests/event/{eventId}/count?status={status}` → `Long`

---

## Конфигурации

Конфигурации всех микросервисов хранятся в **config-server**:


infra/config-server/src/main/resources/config/

├── event-service.yml

├── category-service.yml

├── user-service.yml

├── request-service.yml

├── compilation-service.yml

├── comment-service.yml

└── stats-server.yml


Каждый сервис при старте загружает свою конфигурацию из Config Server. Локальные копии конфигураций также хранятся в `core/<service>/src/main/resources/config/` в качестве резервного варианта.  

---




## Внешнее API

Проект реализует публичный и административный API в соответствии со спецификацией Yandex Practicum:

**Основной сервис (event-service):**
[ewm-main-service-spec.json](https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/refs/heads/main/ewm-main-service-spec.json)

**Сервис статистики (stats-service):**
[ewm-stats-service-spec.json](https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/refs/heads/main/ewm-stats-service-spec.json)


---

## Надёжность

- **Resilience4j** — Circuit Breaker и Retry для всех Feign-клиентов
- **Fallback-методы** — возвращают безопасные значения по умолчанию, если целевой сервис недоступен
- **Eureka** — динамическое обнаружение сервисов, автоматическая регистрация

Конфигурация Resilience4j:
- `sliding-window-size: 10`
- `failure-rate-threshold: 50%`
- `wait-duration-in-open-state: 10s`
- `retry.max-attempts: 3`
- `retry.wait-duration: 1s`
- `timeout-duration: 5s`

---

## Запуск

1. Запустить `discovery-server` (Eureka, порт 8761)
2. Запустить `config-server` (порт 8888)
3. Запустить микросервисы в любом порядке:
  - `event-service`
  - `category-service`
  - `user-service`
  - `request-service`
  - `compilation-service`
  - `comment-service`
  - `stats-server`
4. Запустить `gateway-server` (порт 8080) — единая точка входа

Все сервисы автоматически регистрируются в Eureka и получают конфигурации из Config Server.
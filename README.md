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
| `common` | Общие DTO, исключения и глобальный обработчик ошибок |

### Инфраструктурные сервисы (infra)

| Сервис | Назначение |
|--------|-----------|
| `config-server` | Централизованное хранение конфигураций всех микросервисов |
| `discovery-server` (Eureka) | Сервис обнаружения и регистрации микросервисов |
| `gateway-server` | Единая точка входа для внешних клиентов (порт 8080), маршрутизация запросов |

### Сервис статистики и рекомендаций (stats-service)

| Модуль | Назначение |
|--------|-----------|
| `stats-server` | Сбор и предоставление статистики просмотров событий |
| `stats-client` | Клиенты для взаимодействия: gRPC (Collector, Analyzer) и REST (Stats) |
| `stats-dto` | Общие DTO для статистики |
| `stats-proto` | Protobuf-схемы (gRPC) для Collector и Analyzer |
| `stats-avro` | Avro-схемы для Kafka-сообщений |

## Рекомендательная система (на основе Apache Kafka)

| Сервис | Назначение |
|--------|-----------|
| `collector` | Принимает gRPC-сообщения о действиях пользователей, сериализует в Avro (бинарный формат) и отправляет в Kafka `stats.user-actions.v1` |
| `aggregator` | Читает действия из Kafka (десериализация Avro), вычисляет косинусное сходство мероприятий через дельты весов, отправляет результат в `stats.events-similarity.v1` |
| `analyzer` | Читает оба топика Kafka, обновляет PostgreSQL (таблицы `user_actions`, `event_similarities`), предоставляет gRPC API для рекомендаций |

### Поток данных

1. **Core-сервисы** (`event-service`, `request-service`) отправляют gRPC-запросы в `collector`
2. `collector` → Kafka `stats.user-actions.v1`
3. `aggregator` читает действия, вычисляет сходство → Kafka `stats.events-similarity.v1`
4. `analyzer` читает оба топика, сохраняет в PostgreSQL, отвечает на gRPC-запросы рекомендаций

### Алгоритм рекомендаций

- **Косинусное сходство** между событиями A и B: `S_min(A,B) / sqrt(S_A × S_B)`, где S_A, S_B — суммы весов действий пользователей, S_min — сумма минимальных весов общих пользователей
- **Веса действий:** `VIEW = 0.4`, `REGISTER = 0.8`, `LIKE = 1.0`
- **Обновление через дельты:** вместо полного пересчёта всех сумм при каждом действии используется инкрементальный подход (прирост сумм)
- **Рекомендации для пользователя:** выбор недавних событий → поиск похожих → предсказание оценки через взвешенное косинусное сходство

### Сериализация

- Avro-сообщения сериализуются напрямую через `SpecificDatumWriter`/`SpecificDatumReader` в байтовый массив
- Транспорт через Kafka: `ByteArraySerializer`/`ByteArrayDeserializer`
- Schema Registry не требуется — схемы встроены в JAR-файлы

### API рекомендаций (gRPC)

- `GetRecommendationsForUser` — персональные рекомендации
- `GetSimilarEvents` — похожие события (исключая уже просмотренные)
- `GetInteractionsCount` — суммарный вес взаимодействий для указанных событий
---

## Внутреннее API (REST)

Взаимодействие между core-микросервисами осуществляется через **OpenFeign** с использованием **Eureka** для обнаружения сервисов. Для повышения отказоустойчивости реализованы **fallback-методы** и настроен **Resilience4j** (Circuit Breaker + Retry).

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

## Внутреннее API (gRPC — рекомендательная система)

### Collector

**UserActionController.CollectUserAction**
- Вход: `UserActionProto` (user_id, event_id, action_type, timestamp)
- Выход: `Empty`

### Analyzer

**RecommendationsController.GetRecommendationsForUser**
- Вход: `UserPredictionsRequestProto` (user_id, max_results)
- Выход: поток `RecommendedEventProto` (event_id, score — предсказанная оценка)

**RecommendationsController.GetSimilarEvents**
- Вход: `SimilarEventsRequestProto` (event_id, user_id, max_results)
- Выход: поток `RecommendedEventProto` (event_id, score — коэффициент сходства)

**RecommendationsController.GetInteractionsCount**
- Вход: `InteractionsCountRequestProto` (event_id — список)
- Выход: поток `RecommendedEventProto` (event_id, score — сумма весов)

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

├── stats-server.yml

├── collector.yml

├── aggregator.yml

└── analyzer.yml


Каждый сервис при старте загружает свою конфигурацию из Config Server. Локальные копии конфигураций также хранятся в `core/<service>/src/main/resources/config/` в качестве резервного варианта.

---

## Внешнее API

Проект реализует публичный и административный API в соответствии со спецификацией Yandex Practicum:

**Основной сервис (event-service):**
[ewm-main-service-spec.json](https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/refs/heads/main/ewm-main-service-spec.json)

**Сервис статистики (stats-service):**
[ewm-stats-service-spec.json](https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/refs/heads/main/ewm-stats-service-spec.json)

### Новые эндпоинты (рекомендации)

`GET /events/recommendations` — рекомендации мероприятий для пользователя (заголовок `X-EWM-USER-ID`)

`PUT /events/{eventId}/like` — лайк мероприятия (заголовок `X-EWM-USER-ID`)

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

1. Запустить `docker-compose up` — поднимутся PostgreSQL, Zookeeper и Kafka
2. Запустить `discovery-server` (Eureka, порт 8761)
3. Запустить `config-server` (порт 8888)
4. Запустить микросервисы в любом порядке:
    - `event-service`
    - `category-service`
    - `user-service`
    - `request-service`
    - `compilation-service`
    - `comment-service`
    - `stats-server`
    - `collector`
    - `aggregator`
    - `analyzer`
5. Запустить `gateway-server` (порт 8080) — единая точка входа

Все сервисы автоматически регистрируются в Eureka и получают конфигурации из Config Server.
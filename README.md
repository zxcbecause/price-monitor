# 📉 price-monitor

![CI](https://github.com/zxcbecause/price-monitor/actions/workflows/ci.yml/badge.svg)

Service that tracks product prices on **Wildberries** and **Ozon**, keeps price history and sends a **Telegram alert** when a price drops below your target or falls sharply.

Built as an event-driven Spring Boot app: every price change is published to **Kafka**, so alerts, analytics or repricing can be added as independent consumers.

## How it works

```
             ┌──────────────┐   every 15 min    ┌────────────────────┐
  REST API ─▶│  PostgreSQL  │◀──────────────────│  PriceCheckService │──▶ WB / Ozon API
             │ products,    │   save snapshot   └─────────┬──────────┘
             │ history      │                             │ price changed
             └──────────────┘                             ▼
                                                 Kafka: price-changes
                                                          │
                                                          ▼
                                             PriceAlertListener + AlertPolicy
                                                          │ target reached / big drop
                                                          ▼
                                                    Telegram Bot API
```

- **Scheduler** checks all active products (`monitor.check-interval`, default 15 min). One broken product never stops the others.
- **History**: every check is stored as a snapshot; `/history` and `/stats` endpoints expose it.
- **Events**: a `PriceChangedEvent` is published on every change, keyed by product id (ordering per product is preserved).
- **Alert policy**: notify when the price is at/below the target, or dropped by ≥ N % in one step (per product or global default).
- **Money** is stored in kopecks (`long`) — no floating point errors.

## Stack

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL · Flyway · Spring Kafka · RestClient · springdoc OpenAPI · Testcontainers · Docker Compose · GitHub Actions

## Run

```bash
# optional: real Telegram alerts
export TELEGRAM_BOT_TOKEN=123:abc
export TELEGRAM_CHAT_ID=123456789

docker compose up --build
```

Swagger UI: http://localhost:8080/swagger-ui.html

Without a bot token alerts are written to the log, so the app works with zero setup.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/products` | Start tracking a product |
| `GET` | `/api/products` | List tracked products |
| `GET` | `/api/products/{id}` | One product with current price |
| `PATCH` | `/api/products/{id}` | Change target / threshold, pause tracking |
| `DELETE` | `/api/products/{id}` | Stop tracking (history is removed too) |
| `POST` | `/api/products/{id}/check` | Check the price right now |
| `GET` | `/api/products/{id}/history` | Price history |
| `GET` | `/api/products/{id}/stats` | Min / max / current |

```bash
curl -X POST localhost:8080/api/products \
  -H 'Content-Type: application/json' \
  -d '{"marketplace":"WILDBERRIES","sku":"123456789","targetPrice":1500,"dropThresholdPercent":15}'
```

Alert example:

```
📉 Цена снизилась: Кроссовки беговые
Wildberries · арт. 123456789
1 790 ₽ → 1 490 ₽ (−17%)
🎯 Цель 1 500 ₽ достигнута
https://www.wildberries.ru/catalog/123456789/detail.aspx
```

## Marketplaces

| Marketplace | Source | Notes |
|---|---|---|
| Wildberries | public card API | no keys needed, `sku` = article (nm) |
| Ozon | Seller API `/v5/product/info/prices` | needs `OZON_CLIENT_ID` / `OZON_API_KEY`, `sku` = `product_id` of your store |

Adding a marketplace = one new `MarketplaceClient` bean.

## Tests

```bash
mvn verify
```

- unit tests for alert policy, money handling, WB / Ozon response parsing, message format;
- integration test on real **PostgreSQL + Kafka in Testcontainers**: REST → check → DB → Kafka → listener → notification.

Docker must be running for the integration test.

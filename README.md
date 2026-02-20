# Price Tracker

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql)
![Build](https://github.com/adcsm2/price-tracker/actions/workflows/ci.yml/badge.svg)

Backend service that scrapes tech product prices (GPUs, laptops, monitors) across multiple Spanish online stores and tracks price history over time. Built as a portfolio project to demonstrate production-quality Spring Boot patterns.

## Features

- **Multi-site scraping** — Amazon ES and MediaMarkt ES, extensible via Strategy Pattern
- **Price history** — every scrape is recorded; full time-series per product
- **Cross-site unification** — the same product on Amazon and MediaMarkt shares one canonical `Product` row
- **Analytics** — price drops, price increases, trending products, cross-site comparisons
- **Price alerts** — set a target price per product; alert triggers when the price is reached
- **Scheduled jobs** — scraping runs automatically via Spring `@Scheduled`
- **Rate limiting** — Guava `RateLimiter` (2 req/s per site) to avoid bans

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     REST API (8080)                      │
│  /api/products  /api/jobs  /api/analytics  /api/alerts  │
└────────────────────────┬────────────────────────────────┘
                         │
           ┌─────────────▼─────────────┐
           │     ScrapingJobService     │
           │  (scheduled + on-demand)   │
           └─────────────┬─────────────┘
                         │
           ┌─────────────▼─────────────┐
           │      ScraperFactory        │
           └──────┬──────────┬─────────┘
                  │          │
         ┌────────▼──┐  ┌────▼──────────┐
         │  Amazon   │  │  MediaMarkt   │
         │  Scraper  │  │   Scraper     │
         │  (Jsoup)  │  │  (JSON-LD)    │
         └────────┬──┘  └────┬──────────┘
                  └────┬─────┘
          ┌────────────▼──────────────┐
          │  ProductUnificationService │
          │  (per-item transactions)   │
          └────────────┬──────────────┘
                       │
          ┌────────────▼──────────────┐
          │         PostgreSQL         │
          │  Product → ProductListing  │
          │       → PriceHistory       │
          │       → PriceAlert         │
          └───────────────────────────┘
```

### Data model

| Entity | Description |
|---|---|
| `Product` | Canonical product (one row per physical item, shared across sites) |
| `ProductListing` | Product presence on a specific site (URL, current price, stock) |
| `PriceHistory` | Append-only record of every scraped price with timestamp |
| `WebsiteSource` | Scraper configuration per site |
| `ScrapingJob` | Tracks each scraping run (status, item count, duration) |
| `PriceAlert` | User-defined price target; triggers when `currentPrice ≤ targetPrice` |

### Key design decisions

- [`ADR-001`](docs/architecture/ADR-001-scraper-strategy-pattern.md) — Strategy Pattern for scrapers
- [`ADR-002`](docs/architecture/ADR-002-rate-limiting.md) — Rate limiting with Guava
- [`ADR-003`](docs/architecture/ADR-003-product-unification.md) — Product unification model
- [`ADR-004`](docs/architecture/ADR-004-cloudflare-site-selection.md) — Site selection after Cloudflare blocking

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Scraping | Jsoup |
| Mapping | MapStruct |
| Boilerplate | Lombok |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Build | Maven |
| Containerisation | Docker + Docker Compose |

## Running locally

### Prerequisites

- Docker + Docker Compose
- Java 17
- Maven (or use the included `./mvnw`)

### 1. Configure environment

```bash
cp .env.example .env
# Edit .env — minimum required values:
# POSTGRES_USER=pricetracker
# POSTGRES_PASSWORD=secret
# POSTGRES_DB=pricetracker
# SPRING_DATASOURCE_USERNAME=pricetracker
# SPRING_DATASOURCE_PASSWORD=secret
```

### 2. Start the database

```bash
docker-compose up -d postgres
```

### 3. Run the application

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

### Run everything with Docker

```bash
docker-compose up --build
```

This builds the app image and starts both the database and the application.

## API reference

### Products

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/products` | List all products (`?name=`, `?category=`) |
| `GET` | `/api/products/{id}` | Get product by ID |
| `POST` | `/api/products` | Create product |
| `PUT` | `/api/products/{id}` | Update product |
| `DELETE` | `/api/products/{id}` | Soft-delete product |

### Scraping jobs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/scraping/jobs` | List all scraping jobs |
| `POST` | `/api/scraping/jobs` | Create a scraping job |
| `POST` | `/api/scraping/jobs/{id}/run` | Trigger a job manually |

### Analytics

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/analytics/price-drops` | Products with biggest price drops |
| `GET` | `/api/analytics/price-increases` | Products with biggest price increases |
| `GET` | `/api/analytics/trending` | Most scraped products recently |
| `GET` | `/api/analytics/compare/{productId}` | Cross-site price comparison |

### Price alerts

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/alerts` | Create a price alert |
| `GET` | `/api/alerts?email={email}` | List active alerts for a user |
| `DELETE` | `/api/alerts/{id}` | Delete an alert |

### Example: create and trigger a price alert

```bash
# Create an alert — notify when product 1 drops to or below 500€
curl -X POST http://localhost:8080/api/alerts \
  -H "Content-Type: application/json" \
  -d '{"userEmail": "you@example.com", "productId": 1, "targetPrice": 500.00}'

# Run a scraping job — alert triggers automatically if price ≤ 500€
curl -X POST http://localhost:8080/api/jobs/1/run
```

When triggered, the alert is logged:

```
PRICE ALERT: 'ASUS TUF RTX 4070' is now 489.00€ (target: 500.00€) — notifying you@example.com
```

### Example: create and run a scraping job

```bash
# Create a MediaMarkt job (sourceId=2)
curl -X POST http://localhost:8080/api/scraping/jobs \
  -H "Content-Type: application/json" \
  -d '{"keyword": "rtx 4070", "sourceId": 2}'

# Run it immediately
curl -X POST http://localhost:8080/api/scraping/jobs/1/run
```

## Tests

```bash
./mvnw test     # 65 tests (unit + integration)
./mvnw verify   # Tests + full build verification
```

Unit tests use Mockito. Integration tests (`ProductServiceIntegrationTest`) use Testcontainers to spin up a real PostgreSQL instance — no manual setup required.

## Scrapers

| Site | Method | Status |
|---|---|---|
| Amazon ES | Jsoup HTML parsing | Working (prices JS-rendered on some pages) |
| MediaMarkt ES | Jsoup + JSON-LD (`Schema.org/ItemList`) | Fully working |
| PCComponentes | — | Blocked by Cloudflare Interactive Challenge |

**Amazon note:** Amazon returns HTML, but product prices on individual pages are rendered client-side. The scraper reliably extracts names and URLs. A headless browser (Playwright/Selenium) would be needed for consistent price extraction — out of scope for this project.

**PCComponentes note:** Protected by Cloudflare's Interactive Challenge, which requires JavaScript execution to solve before serving content. Out of scope; the Strategy Pattern makes adding it later straightforward.

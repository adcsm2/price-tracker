# Price Tracker

Multi-site price tracker for tech products (GPUs, laptops, monitors) built with Spring Boot.
Monitors prices across Spanish online stores and stores historical price data.

## Stack

- **Java 17 + Spring Boot 3.5 + Maven**
- **PostgreSQL 15** (Docker for local dev)
- **Flyway** for database migrations
- **Jsoup** for HTML scraping
- **Guava RateLimiter** + **Spring Retry** for resilient scraping
- **MapStruct** for DTO mapping

## Run Locally

```bash
docker-compose up -d          # Start PostgreSQL
./mvnw spring-boot:run        # Start the app (port 8080)
./mvnw test                   # Run unit + integration tests
```

Credentials are read from environment variables. Copy `.env.example` to `.env` and fill in the values.

## Scrapers Implementados

| Site | Método | Estado |
|------|--------|--------|
| **Amazon.es** | Jsoup (HTML) | ✅ Funcionando |
| **MediaMarkt.es** | Jsoup + JSON-LD | ✅ Funcionando |
| **PCComponentes** | — | ❌ Bloqueado por Cloudflare |

### Nota sobre PCComponentes

PCComponentes protege tanto su HTML como su API interna con Cloudflare's
**Interactive Challenge** (`cType: interactive`), que requiere ejecución de
JavaScript para resolver el desafío antes de servir contenido.

Una implementación real requeriría Selenium o Playwright (ChromeDriver en
modo headless), lo que añade complejidad operativa significativa
(gestión del driver, mayor consumo de CPU/memoria, tiempos de respuesta
más lentos) sin aportar valor adicional al patrón Strategy ya demostrado
con Amazon y MediaMarkt.

### Nota sobre MediaMarkt

MediaMarkt usa React (SPA), por lo que el HTML renderizado no contiene
los datos de producto directamente. Sin embargo, la página incluye
**JSON-LD** (`<script type="application/ld+json">`) con el esquema
`ItemList` de Schema.org, que Jsoup puede extraer directamente.
Este enfoque es más robusto que los selectores CSS generados por
styled-components, que cambian en cada despliegue.

## Architecture

The project uses the **Strategy Pattern** for scrapers:

```
SiteScraper (interface)
├── AmazonScraper     → Jsoup HTML parsing
└── MediaMarktScraper → Jsoup + JSON-LD parsing
```

`ScraperFactory` maps `ScraperType` → `SiteScraper` at startup,
allowing new scrapers to be added without modifying existing code.

## API Endpoints

```
GET  /api/products                    List products (filters: category, keyword, minPrice, maxPrice)
POST /api/products                    Create product
GET  /api/products/{id}               Get product by id
PUT  /api/products/{id}               Update product
DELETE /api/products/{id}             Soft-delete product
```

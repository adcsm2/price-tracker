# Multi-Site Price Tracker - Estado Actual

**Última actualización**: 2025-02-12  
**Proyecto**: Multi-Site Price Tracker (Web Scraper)  
**Stack**: Java 17 + Spring Boot 3 + PostgreSQL  
**Objetivo**: Aprender web scraping, Spring Boot, CRUD avanzado, scheduled jobs

## ✅ Completado

Nada aún - proyecto por empezar.

## 🚧 En progreso

Ninguno.

## 📋 Roadmap de Fases

### Fase 1: Setup + Entities (2-3h) - SIGUIENTE
**Branch:** `feat/project-setup`

**Tareas:**
- Spring Boot initialization (Spring Initializr)
- Maven dependencies (JPA, PostgreSQL, Jsoup, Lombok, etc.)
- Docker Compose (PostgreSQL)
- Entities (Product, ProductListing, WebsiteSource, PriceHistory, ScrapingJob)
- Repositories (Spring Data JPA)
- application.yml configuration
- Flyway migrations
- README básico

**Duración estimada:** 2-3 horas

---

### Fase 2: Primer Scraper (Amazon) (3-4h)
**Branch:** `feat/amazon-scraper`

**Tareas:**
- SiteScraper interface
- AmazonScraper implementation con Jsoup
- Rate limiting (Guava RateLimiter - 2 req/sec)
- Retry logic (@Retryable, exponential backoff)
- ScrapedProductDTO
- Tests unitarios con HTML fixtures

**Duración estimada:** 3-4 horas

---

### Fase 3: CRUD de Products (2h)
**Branch:** `feat/product-crud`

**Tareas:**
- ProductService (create, read, update, delete, list)
- ProductController (REST endpoints)
- ProductDTO + MapStruct mapping
- Filtros: category, minPrice, maxPrice, keyword
- Soft delete implementation
- Tests de integración (TestContainers)

**Duración estimada:** 2 horas

---

### Fase 4: Segundo Scraper (PCComponentes) (2-3h)
**Branch:** `feat/pccomponentes-scraper`

**Tareas:**
- PCComponentesScraper implementation
- ScraperFactory (Strategy pattern)
- Product unification logic (mismo producto en varios sitios)
- ScraperType enum
- Tests

**Duración estimada:** 2-3 horas

---

### Fase 5: Scraping Jobs + Scheduling (2-3h)
**Branch:** `feat/scraping-jobs`

**Tareas:**
- ScrapingJobService
- ScrapingJobController
- POST /api/scraping/jobs (crear job)
- POST /api/scraping/jobs/{id}/run (ejecutar)
- @Scheduled jobs (daily scraping)
- Job status tracking
- Tests

**Duración estimada:** 2-3 horas

---

### Fase 6: Price History + Analytics (2h)
**Branch:** `feat/price-history-analytics`

**Tareas:**
- PriceHistory tracking automático
- Analytics endpoints:
  - GET /api/analytics/price-drops
  - GET /api/analytics/price-increases
  - GET /api/analytics/trending
  - GET /api/compare?productId=X
- Price comparison logic
- Tests

**Duración estimada:** 2 horas

---

### Fase 7: Price Alerts (Opcional - 1.5h)
**Branch:** `feat/price-alerts`

**Tareas:**
- PriceAlert entity + repository
- AlertService (create, check, trigger)
- Email notifications (Spring Mail)
- Scheduled alert checking
- Tests

**Duración estimada:** 1.5 horas

---

### Fase 8: Docs + Docker Final (1.5h)
**Branch:** `docs/finalization`

**Tareas:**
- README completo con badges
- Decisiones arquitectónicas:
  - 001-scraper-strategy-pattern.md
  - 002-rate-limiting.md
  - 003-product-unification.md
- Dockerfile multi-stage
- docker-compose.prod.yml
- GitHub Actions CI/CD
- Legal disclaimer

**Duración estimada:** 1.5 horas

---

## 🎯 Métricas objetivo

- **Tests**: 40-50 tests pasando
- **Cobertura**: >80% en services, >70% en controllers
- **Endpoints**: 15-20 funcionando
- **Sitios scrapeables**: 2 (Amazon + PCComponentes)
- **Decisiones arquitectónicas**: 3 documentadas

## 🔧 Stack confirmado

- Java 17
- Spring Boot 3.2+
- Spring Data JPA + Hibernate
- PostgreSQL 15 (Docker)
- Jsoup (HTML parsing)
- Guava RateLimiter
- Lombok + MapStruct
- JUnit 5 + Mockito + TestContainers
- Maven
- GitHub Actions

## ⏱️ Tiempo total estimado

**Total**: ~15-18 horas para proyecto completo

Desglose:
- Fases 1-6 (core): ~13-15 horas
- Fase 7 (opcional): +1.5 horas
- Fase 8 (docs): +1.5 horas

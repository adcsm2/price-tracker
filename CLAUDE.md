# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Multi-Site Price Tracker — a Spring Boot web scraper that tracks prices of tech products (GPUs, laptops, monitors) across multiple online stores (Amazon ES, PCComponentes). Portfolio project.

## Build & Run Commands

```bash
./mvnw spring-boot:run                    # Run the app
./mvnw test                               # Run all tests
./mvnw test -Dtest=ClassName              # Run a single test class
./mvnw test -Dtest=ClassName#methodName   # Run a single test method
./mvnw clean install                      # Full build
./mvnw verify                             # Tests + integration tests
docker-compose up -d                      # Start PostgreSQL
docker-compose down                       # Stop PostgreSQL
```

## Architecture

- **Java 17 + Spring Boot 3.5 + Maven**
- **PostgreSQL 15** via Docker for local dev
- **Strategy Pattern** for scrapers: `SiteScraper` interface with per-site implementations (`AmazonScraper`, `PCComponentesScraper`), wired via `ScraperFactory`
- **Product Unification**: `Product` (canonical) → `ProductListing` (per-site occurrence) → `PriceHistory` (price over time)
- **Rate Limiting**: Guava `RateLimiter` (2 req/sec per site), configured in `application.yml` under `scraper.rate-limit`
- **Retry**: Spring Retry with exponential backoff on scraping failures
- **DTO Mapping**: MapStruct

### Key Entities

`WebsiteSource` → `ProductListing` ← `Product` → `PriceHistory`, `PriceAlert`; `ScrapingJob` tracks scraping runs.

### Package Layout

`com.portfolio.pricetracker.{entity, repository, service, service.scraper, controller, dto, config}`

## Conventions

- Constructor injection (no field `@Autowired`)
- Lombok for boilerplate, MapStruct for DTO mapping
- Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`
- Branch naming: `feat/feature-name`, `fix/bug-name`
- Test naming: `should_ExpectedBehavior_When_StateUnderTest`
- Test fixtures in `src/test/resources/fixtures/`
- Credentials via environment variables (`.env` file, never hardcoded). `.env` is gitignored; `.env.example` is committed as template.

## Git Workflow

**ALWAYS target `develop` when creating PRs from feature branches. NEVER target `main` directly.**

```
feat/* → develop → main
```

- Feature branches are created from `develop`
- PRs from feature branches always use `--base develop` explicitly
- `develop → main` is a separate PR after the feature is validated on develop
- When creating PRs with `gh pr create`, ALWAYS pass `--base develop`:

```bash
gh pr create --base develop --title "..." --body "..."
```

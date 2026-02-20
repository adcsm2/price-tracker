# ADR-001: Strategy Pattern for Scrapers

**Status:** Accepted
**Date:** 2026-02
**Phase:** 2 (Amazon Scraper)

---

## Context

The project needs to scrape prices from multiple websites. Each site has a completely different HTML structure:
- Amazon: server-rendered HTML with `data-component-type` attributes
- MediaMarkt: React SPA with embedded JSON-LD
- PCComponentes: internal JSON API (blocked by Cloudflare)

The scraping logic for each site is independent and shares nothing beyond the interface (receive keyword, return list of products).

---

## Problem

How to organise the scraping code so it is extensible (easily add new sites) without modifying existing code?

---

## Options considered

### Option 1: Single method with if/else per site

```java
public List<ScrapedProductDTO> scrape(String keyword, ScraperType type) {
    if (type == AMAZON) {
        // Amazon logic
    } else if (type == MEDIAMARKT) {
        // MediaMarkt logic
    }
}
```

**Pros:** Simple initially, no abstractions.
**Cons:** Violates the Open/Closed Principle. Adding a site requires modifying this method. Hard to test in isolation. A bug in one site can affect others.

### Option 2: Inheritance (`AbstractScraper`)

```java
public abstract class AbstractScraper {
    protected abstract Document fetchPage(String url) throws IOException;
    public List<ScrapedProductDTO> scrape(String keyword) {
        Document doc = fetchPage(buildUrl(keyword));
        return parse(doc);
    }
}
```

**Pros:** Shares common code (rate limiting, retry).
**Cons:** Inheritance is rigid. If a scraper needs a radically different approach (RestTemplate instead of Jsoup, as PCComponentes initially required), it gets complicated. The fetch+parse logic can also differ significantly between sites.

### Option 3: Strategy Pattern with interface ✅

```java
public interface SiteScraper {
    ScraperType getScraperType();
    List<ScrapedProductDTO> scrape(String keyword, String category);
}
```

Each scraper is an independent `@Service` that implements the interface. `ScraperFactory` registers them automatically via Spring DI.

**Pros:** Extensible without modifying existing code. Each scraper is fully independent and testable. Spring injects all scrapers automatically. Clear separation of concerns.
**Cons:** More classes. No explicit code reuse between scrapers (though this is actually an advantage here).

---

## Decision

**Option 3: Strategy Pattern.**

The decisive criterion was extensibility and scraper heterogeneity. When it became clear that PCComponentes required RestTemplate instead of Jsoup (and then that even that was blocked), it was obvious that each site may need a completely different approach. Inheritance would have forced artificial coupling.

---

## Consequences

### Positive
- Adding a new scraper = create a `@Service` class implementing `SiteScraper`. Zero changes to existing code.
- Each scraper can be tested completely independently.
- Each site's code is self-contained: easy to read, maintain and debug.

### Negative
- Duplication of setup (RateLimiter, Retryable) in each scraper. Acceptable because each site may need different configuration.

---

## Implementation

```
SiteScraper (interface)
├── AmazonScraper (@Service)     → Jsoup HTML
└── MediaMarktScraper (@Service) → Jsoup + JSON-LD parsing

ScraperFactory (@Service)
└── Receives List<SiteScraper> via DI → Map<ScraperType, SiteScraper>
```

Spring automatically injects all `SiteScraper` beans into the list, with no manual registration.

---

## References

- [Strategy Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/strategy)
- [Open/Closed Principle](https://en.wikipedia.org/wiki/Open%E2%80%93closed_principle)
- See Learning 007 for implementation details

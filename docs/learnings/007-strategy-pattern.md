# Learning 007: Strategy Pattern

**Phase:** 2-4 (Amazon + MediaMarkt scrapers)
**Date:** 2026-02
**Technology:** Java, Spring Boot DI

---

## What is it?

The **Strategy Pattern** is a behavioural design pattern. It defines a family of algorithms, encapsulates each one in its own class, and makes them interchangeable. The client works with the interface, without knowing the concrete implementation.

---

## The problem it solves

Each website has different HTML/API. Without the Strategy Pattern:

```java
// ❌ Without pattern: if/else in the client code
public List<ScrapedProductDTO> scrape(String keyword, ScraperType type) {
    if (type == ScraperType.AMAZON) {
        // Amazon logic...
    } else if (type == ScraperType.MEDIAMARKT) {
        // MediaMarkt logic...
    } else if (type == ScraperType.NEW_SITE) {  // ← must modify this method
        // ...
    }
}
```

Every new site requires modifying existing code (violates the **Open/Closed Principle**).

---

## Implementation in the project

### The interface (the contract)

```java
// SiteScraper.java
public interface SiteScraper {
    String getSiteName();
    ScraperType getScraperType();
    List<ScrapedProductDTO> scrape(String keyword, String category);
}
```

### The concrete strategies

```java
// AmazonScraper.java
@Service
public class AmazonScraper implements SiteScraper {

    @Override
    public ScraperType getScraperType() { return ScraperType.AMAZON; }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        // Amazon-specific logic (Jsoup, HTML selectors)
    }
}

// MediaMarktScraper.java
@Service
public class MediaMarktScraper implements SiteScraper {

    @Override
    public ScraperType getScraperType() { return ScraperType.MEDIAMARKT; }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        // MediaMarkt-specific logic (Jsoup + JSON-LD)
    }
}
```

### The factory (ScraperFactory)

`ScraperFactory` uses Spring DI to automatically collect all beans implementing `SiteScraper`:

```java
// ScraperFactory.java
@Service
public class ScraperFactory {

    private final Map<ScraperType, SiteScraper> scrapers;

    public ScraperFactory(List<SiteScraper> scraperList) {
        this.scrapers = scraperList.stream()
                .collect(Collectors.toMap(
                        SiteScraper::getScraperType,
                        Function.identity()
                ));
    }

    public SiteScraper getScraper(ScraperType type) {
        SiteScraper scraper = scrapers.get(type);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper registered for: " + type);
        }
        return scraper;
    }

    public List<SiteScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}
```

**Key**: `ScraperFactory` receives `List<SiteScraper>` in its constructor. Spring automatically injects all beans implementing `SiteScraper`. No manual registration required.

### Usage from the client

```java
SiteScraper scraper = scraperFactory.getScraper(ScraperType.AMAZON);
List<ScrapedProductDTO> results = scraper.scrape("rtx 4070", "gpu");
```

---

## Adding a new site

To add support for a new site you only need to:

1. Add the value to the `ScraperType` enum
2. Create a `@Service` class implementing `SiteScraper`
3. ✅ Done. Spring registers it automatically in `ScraperFactory`

No changes to `ScraperFactory` or any existing code required.

---

## Comparison of alternatives

| Option | Pros | Cons |
|---|---|---|
| **Strategy Pattern** ✅ | Extensible, Open/Closed, testable | More classes |
| Giant if/else | Simple initially | Becomes unmanageable, violates OCP |
| Inheritance (`AbstractScraper`) | Shares common code | Rigid inheritance, hard to test |
| Plugins/reflection | Extensible without recompile | Very complex, overkill |

---

## References

- [Strategy Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/strategy)
- [Open/Closed Principle](https://en.wikipedia.org/wiki/Open%E2%80%93closed_principle)
- See ADR-001 for the full architectural decision
- `SiteScraper.java`, `AmazonScraper.java`, `MediaMarktScraper.java`, `ScraperFactory.java`

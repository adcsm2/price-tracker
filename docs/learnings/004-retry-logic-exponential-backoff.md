# Learning 004: Retry Logic with Exponential Backoff

**Phase:** 2 (Amazon Scraper)
**Date:** 2026-02
**Technology:** Spring Retry, `@Retryable`, `@Backoff`

---

## What is it?

**Retry with exponential backoff** is a resilience pattern: when an operation fails due to a transient error, it is retried automatically, waiting a time that grows exponentially between attempts.

```
Attempt 1 → fails → wait 1s
Attempt 2 → fails → wait 2s
Attempt 3 → fails → throw exception
```

---

## Why do we need it?

Scraping can fail for transient reasons:
- Occasional network timeout
- The server returns a temporary 503
- Traffic spike on the target server

Retrying automatically makes the scraper much more resilient without manual loop and counter code.

---

## Implementation with Spring Retry

### Enable in configuration

```java
// ScraperConfig.java
@Configuration
@EnableRetry  // ← Activates @Retryable support throughout the app
public class ScraperConfig {
    // ...
}
```

### Annotation on the method that can fail

```java
// AmazonScraper.java
@Retryable(
        retryFor = IOException.class,   // Only retries for this type
        maxAttempts = 3,                 // Maximum 3 attempts
        backoff = @Backoff(delay = 1000, multiplier = 2)  // 1s, 2s
)
public Document fetchSearchPage(String keyword) throws IOException {
    rateLimiter.acquire();
    return Jsoup.connect(searchUrl).get();
}
```

### Separation between fetch and parse

Important: `@Retryable` is on `fetchSearchPage()`, not on `scrape()`. This way only the network part is retried, not the parsing:

```java
@Override
public List<ScrapedProductDTO> scrape(String keyword, String category) {
    try {
        Document doc = fetchSearchPage(keyword);  // ← retry here
        return parseSearchResults(doc);            // ← no retry here
    } catch (IOException e) {
        log.error("Error scraping Amazon: {}", e.getMessage());
        return new ArrayList<>();
    }
}
```

---

## How `@Retryable` works internally

Spring Retry uses AOP (proxies). The class must be a Spring bean for it to work. If you call `fetchSearchPage()` from within the same class using `this.fetchSearchPage()`, the proxy is bypassed → retry does not work.

This is why `scrape()` calls `fetchSearchPage()` directly (not `this.`), but since it is called from the controller (through the proxy), the retry DOES work.

---

## Backoff configuration

```java
@Backoff(delay = 1000, multiplier = 2)
// Attempt 1 fails → wait 1000ms
// Attempt 2 fails → wait 2000ms (1000 × 2)
// Attempt 3 fails → throw the exception
```

With `maxDelay` the maximum wait time could be capped:
```java
@Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
```

---

## Graceful degradation

If all retries fail, the scraper returns an empty list instead of propagating the exception:

```java
} catch (IOException e) {
    log.error("Error scraping Amazon for keyword '{}': {}", keyword, e.getMessage());
    return new ArrayList<>();  // ← endpoint never fails, returns empty
}
```

---

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| **Spring Retry** ✅ | Spring-integrated, simple annotation | Requires `@EnableRetry` |
| Manual (try/catch loop) | No dependencies | Repetitive code, hard to maintain |
| Resilience4j | More features (circuit breaker, bulkhead) | More configuration, overkill for this case |

---

## References

- [Spring Retry Documentation](https://github.com/spring-projects/spring-retry)
- [Exponential Backoff and Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
- `AmazonScraper.java`, `MediaMarktScraper.java`

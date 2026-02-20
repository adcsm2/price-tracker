# Learning 003: Guava RateLimiter

**Phase:** 2 (Amazon Scraper)
**Date:** 2026-02
**Technology:** Google Guava 32.1.3

---

## What is it?

Guava's `RateLimiter` implements the **Token Bucket** algorithm: it generates tokens at a constant rate and consumers must acquire a token before proceeding. If no tokens are available, the thread blocks until one is generated.

---

## Why do we need it?

Bombarding a server with requests during web scraping can:
- Result in an IP ban
- Overload the target server (an ethical and legal concern)
- Generate 429 Too Many Requests responses

The solution is to limit the request rate to a configurable maximum per site.

---

## Implementation in the project

### Per-site configuration in application.yml

```yaml
scraper:
  rate-limit:
    amazon: 2.0       # 2 requests/second
    mediamarkt: 2.0   # 2 requests/second
```

### One RateLimiter per scraper

```java
// AmazonScraper.java
public class AmazonScraper implements SiteScraper {

    private final RateLimiter rateLimiter;

    public AmazonScraper(ScraperConfig scraperConfig) {
        double rate = scraperConfig.getRateLimitForSite("amazon");
        this.rateLimiter = RateLimiter.create(rate);  // tokens/second
    }

    public Document fetchSearchPage(String keyword) throws IOException {
        rateLimiter.acquire();  // Blocks until a token is available
        // ... makes the request
    }
}
```

### Centralised configuration

```java
// ScraperConfig.java
@ConfigurationProperties(prefix = "scraper")
public class ScraperConfig {

    private Map<String, Double> rateLimit;

    public double getRateLimitForSite(String site) {
        return rateLimit.getOrDefault(site, 2.0);  // Default: 2 req/sec
    }
}
```

---

## How Token Bucket works

```
Time:    0s    0.5s   1s    1.5s   2s
Tokens:  [■■]  [■■■]  [■■■■■] [■■]  [■■■]
          ↓                   ↓
        acquire()           acquire()
        (immediate)         (immediate)
```

With `RateLimiter.create(2.0)`:
- 2 tokens are generated per second
- `acquire()` consumes 1 token
- If no tokens available, waits until one is generated
- Tokens accumulate (up to a limit) if not consumed

---

## In tests: high rate to avoid slowing down

```java
@BeforeEach
void setUp() {
    ScraperConfig config = new ScraperConfig();
    config.setRateLimit(Map.of("amazon", 10.0));  // 10 req/sec in tests
    scraper = new AmazonScraper(config);
}
```

---

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| **Guava RateLimiter** ✅ | Simple, built-in token bucket, no extra deps | Single-process only (not distributed) |
| `Thread.sleep()` | Trivial | Not adaptive, blocks unnecessarily |
| `Semaphore` | Concurrency control | Does not limit by time, more complex |
| Bucket4j | Distributed (Redis), more features | Extra dependency, overkill for this project |
| Resilience4j RateLimiter | Spring integration | More config, overkill |

---

## Limitation: single-process only

If we scaled to multiple application instances, each would have its own `RateLimiter` and the actual limit would be `rate × nInstances`. For that scenario a distributed rate limiter (e.g. Bucket4j + Redis) would be needed.

For this project (single instance, portfolio), Guava is more than sufficient.

---

## References

- [Guava RateLimiter Javadoc](https://guava.dev/releases/32.0/api/docs/com/google/common/util/concurrent/RateLimiter.html)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- See ADR-002 for the decision to choose Guava over other options
- `AmazonScraper.java`, `MediaMarktScraper.java`, `ScraperConfig.java`

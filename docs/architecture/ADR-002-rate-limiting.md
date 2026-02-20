# ADR-002: Rate Limiting with Guava RateLimiter

**Status:** Accepted
**Date:** 2026-02
**Phase:** 2 (Amazon Scraper)

---

## Context

The scraper makes HTTP requests to third-party websites. Without rate control, it could:
- Trigger an IP ban from the target site
- Overload the target server (an ethical concern)
- Receive 429 Too Many Requests responses

We need a mechanism that limits the request rate to a configurable maximum per site.

---

## Problem

How to implement per-site rate limiting in a simple, configurable and correct way?

---

## Options considered

### Option 1: Fixed `Thread.sleep()`

```java
public Document fetchPage(String url) throws IOException {
    Thread.sleep(500);  // wait 500ms between requests
    return Jsoup.connect(url).get();
}
```

**Pros:** Trivial to implement, no dependencies.
**Cons:** Not adaptive. If the request takes 450ms, the total pause is 950ms, not 500ms. The delay is fixed regardless of load. With concurrency, `sleep` on one thread does not prevent others from making simultaneous requests.

### Option 2: Java `Semaphore`

```java
private final Semaphore semaphore = new Semaphore(1);  // 1 request at a time

public Document fetchPage(String url) throws IOException {
    semaphore.acquire();
    try {
        return Jsoup.connect(url).get();
    } finally {
        semaphore.release();
    }
}
```

**Pros:** Thread-safe, concurrency control.
**Cons:** Controls concurrency, not throughput. Does not guarantee a maximum of N requests per second, only that at most 1 runs at a time. Simulating rate limiting would require combining with sleep, increasing complexity.

### Option 3: Guava RateLimiter ✅

```java
private final RateLimiter rateLimiter = RateLimiter.create(2.0);

public Document fetchPage(String url) throws IOException {
    rateLimiter.acquire();  // Blocks until a token is available
    return Jsoup.connect(url).get();
}
```

**Pros:** Correct Token Bucket implementation. Thread-safe. Configurable (tokens/second). `acquire()` blocks for the minimum necessary time. Already included in Guava (dependency already present).

**Cons:** Only works within a single process (not distributed). Sufficient for this project (single instance).

### Option 4: Bucket4j with Redis

Distributed rate limiting using Redis as a shared token store.

**Pros:** Works across multiple application instances.
**Cons:** Requires Redis (extra infrastructure), additional dependency, much more configuration. Overkill for a single-instance portfolio project.

---

## Decision

**Option 3: Guava RateLimiter.**

Provides the correct behaviour (Token Bucket, thread-safe, configurable) at the lowest complexity cost. Guava was already on the classpath for another reason, so no new dependency is added.

---

## Consequences

### Positive
- 2 lines of code per scraper for correct rate limiting
- Configurable per site from `application.yml`
- Thread-safe with no additional code

### Negative
- Only works in a single process. If the project scales to multiple instances (out of portfolio scope), migration to Bucket4j + Redis would be needed.

---

## Configuration

```yaml
# application.yml
scraper:
  rate-limit:
    amazon: 2.0      # 2 req/sec
    mediamarkt: 2.0  # 2 req/sec
```

```java
// ScraperConfig.java
public double getRateLimitForSite(String site) {
    return rateLimit.getOrDefault(site, 2.0);  // Safe default
}
```

In tests, a high rate (10 req/sec) is configured to avoid slowing down the test suite.

---

## References

- [Guava RateLimiter](https://guava.dev/releases/32.0/api/docs/com/google/common/util/concurrent/RateLimiter.html)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- See Learning 003 for implementation details

# Learning 001: Jsoup HTML Parsing

**Phase:** 2 (Amazon Scraper)
**Date:** 2026-02
**Technology:** Jsoup 1.17.2

---

## What is it?

Jsoup is a Java library for parsing, extracting and manipulating HTML. It provides a jQuery-like API for navigating the DOM with CSS selectors.

---

## Why do we use it?

We needed to extract structured data (name, price, image, URL) from Amazon HTML pages. Jsoup allows making HTTP requests and parsing the resulting HTML in a single call, with intuitive CSS selectors.

---

## How it works in the project

### 1. Fetch + parse in a single call

```java
Document doc = Jsoup.connect(searchUrl)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
        .timeout(10_000)
        .header("Accept-Language", "es-ES,es;q=0.9")
        .header("Accept", "text/html,application/xhtml+xml")
        .get();
```

### 2. Element selection with CSS selectors

```java
// Select all Amazon search results
Elements items = doc.select("[data-component-type=s-search-result]");

// Extract product name
Element titleElement = item.selectFirst("h2 a span");
String name = titleElement.text().trim();

// Extract image
Element img = item.selectFirst(".s-image");
String imageUrl = img.attr("src");
```

### 3. Building absolute URLs

```java
Element link = item.selectFirst("h2 a");
String href = link.attr("href");
if (href.startsWith("/")) {
    return BASE_URL + href;  // Relative → absolute
}
return href;
```

---

## Key technique: HTML fixtures in tests

To avoid making real HTTP requests in tests, we save static HTML as fixtures:

```java
// AmazonScraperTest
Document doc = Jsoup.parse(
    getClass().getResourceAsStream("/fixtures/amazon_search.html"),
    "UTF-8",
    "https://www.amazon.es"
);
List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);
```

Advantages:
- Deterministic tests (not dependent on Amazon returning the same HTML)
- Fast tests (no network)
- Tests that do not consume real rate limit

---

## Important limitation found

Jsoup **does not execute JavaScript**. Several sites use React/Next.js and load content dynamically, so the HTML Jsoup receives is empty of data.

**Solutions depending on the case:**
- Find the site's internal JSON endpoints (browser API calls)
- Find embedded JSON-LD in the HTML (schema.org)
- Use Selenium/Playwright for JS rendering (much more complex)

See ADR-004 for how this was resolved for MediaMarkt.

---

## Most commonly used CSS selectors

| Selector | Example | Use |
|---|---|---|
| `[attr=value]` | `[data-component-type=s-search-result]` | Exact attribute |
| `.className` | `.a-price-whole` | CSS class |
| `parent child` | `h2 a span` | Descendant |
| `element` | `img` | By tag |
| `:first-child` | `h2:first-child` | Position |

---

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| **Jsoup** ✅ | Simple, no extra deps, fast | Does not execute JS |
| HtmlUnit | Executes some JS | Slow, unstable |
| Selenium | Full JS | ChromeDriver, slow, complex |
| Playwright | Full JS, more modern | Heavy dependencies |

---

## References

- [Jsoup Cookbook](https://jsoup.org/cookbook/)
- [CSS Selector Reference](https://jsoup.org/apidocs/org/jsoup/select/Selector.html)
- `AmazonScraper.java`, `MediaMarktScraper.java`

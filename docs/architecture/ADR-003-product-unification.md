# ADR-003: Product Unification Model

**Status:** Accepted
**Date:** 2026-02
**Phase:** 6 (Price History + Analytics)

---

## Context

The project scrapes the same physical product (e.g. "ASUS TUF RTX 4070") across multiple stores (Amazon, MediaMarkt). Without a unification strategy, we would have:

- One row per store with no relationship between them → impossible to compare prices across sites
- Price history disconnected from a canonical product
- Meaningless analytics (what is "the price" of a product if there are N rows?)

---

## Problem

How to model the fact that the same physical product appears in multiple stores, each with its own URL, price and availability, while maintaining a unified price history?

---

## Decision

Three-layer model:

```
Product (canonical)
  └── ProductListing (one row per store)
        └── PriceHistory (time-series record of each scrape)
```

**`Product`** represents the physical product. Created once, identified by name (case-insensitive). This is the entity on which alerts are created and analytics are computed.

**`ProductListing`** represents the presence of that product in a specific store. Has a unique constraint `(product_id, source_id)` to prevent duplicates. Stores `currentPrice`, `inStock`, `lastScrapedAt`.

**`PriceHistory`** records each scrape with a timestamp. Append-only, never modified. Enables calculating variations, trends and comparisons over time.

### Name-based matching strategy

When a new product is scraped:
1. Look up `ProductListing` by exact URL → if found, update price
2. If not found → look up `Product` by name (case-insensitive) → reuse if found
3. If no product exists → create new `Product` + `ProductListing`

This name matching allows "ASUS TUF RTX 4070" from Amazon and from MediaMarkt to share the same `Product` row, making their prices comparable via `PriceComparison`.

### Per-item transactions with TransactionTemplate

`ProductUnificationService.saveResults()` uses `TransactionTemplate.executeWithoutResult()` instead of `@Transactional` on the public method. Reason: using `@Transactional` + try/catch in the same method marks the transaction as `rollback-only` when a JPA exception is caught, and any attempt to continue throws `UnexpectedRollbackException`.

With `TransactionTemplate`, each item runs in its own independent transaction. A failure on item N does not affect items N+1..M.

---

## Rejected alternatives

### Flat table per scrape

```
scrape_results(id, site, name, price, url, scraped_at)
```

**Rejected** because it does not allow grouping the same product across sites, comparing prices between stores, or computing trends for a canonical product.

### URL-only matching

Without name matching, Amazon and MediaMarkt would always create separate `Product` rows even for the same physical item. The canonical model would lose its purpose.

### `@Transactional` + manual savepoint

More complex, not portable across JPA providers. `TransactionTemplate` is the idiomatic Spring solution for programmatic transactions.

---

## Consequences

**Positive:**
- Analytics and cross-site price comparisons are trivial to implement
- Price history is per canonical product, not per listing
- A failure saving one item does not cancel the whole batch

**Negative:**
- Name matching is fragile: "ASUS TUF RTX 4070 Ti" vs "ASUS TUF Gaming RTX 4070 Ti" would create separate products even if they are the same item
- Without name normalisation, cross-site unification in real production would be partial
- Acceptable for a portfolio project, but production would require fuzzy matching or an external product catalogue (EAN/ASIN)

# ADR-005: Soft Delete with deletedAt field

**Status:** Accepted
**Date:** 2026-02
**Phase:** 3 (Product CRUD)

---

## Context

The project needs a strategy for deleting products. A hard delete (`DELETE FROM products`) permanently destroys the record, which raises several problems:

- The `PriceHistory` and `ProductListing` for that product would become orphaned (FK violation or loss of historical data)
- No way to recover a product deleted by mistake
- No audit trail of which products existed and when they were removed

---

## Decision

Soft delete using a `deletedAt` field on the `Product` entity:

```java
@Column
private LocalDateTime deletedAt;  // null = active, non-null = deleted
```

The service sets `deletedAt = now()` instead of deleting the row. All repository queries filter by `WHERE deleted_at IS NULL`.

```java
// ProductService.java
public void delete(Long id) {
    Product product = productRepository.findActiveById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
}
```

```java
// ProductRepository.java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
List<Product> findAllActive();

@Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
Optional<Product> findActiveById(@Param("id") Long id);
```

---

## Rejected alternatives

| Option | Pros | Cons |
|---|---|---|
| **`deletedAt` timestamp** ✅ | Explicit, records when deleted, standard JPA | Must filter in every query |
| Hibernate `@SQLDelete + @Where` | Automatic, less code | Implicit magic, not JPA standard |
| `active: boolean` field | Semantic | Loses information about when it was deleted |
| Hard delete | Simple | No audit trail, breaks price history |

### Why not `@SQLDelete + @Where`

Hibernate allows automating soft deletes:

```java
@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
public class Product { ... }
```

Rejected because `@Where` is a Hibernate-proprietary annotation (not JPA standard) and hides the behaviour. Explicit JPQL queries make it clear that only active records are returned, with no implicit magic.

---

## Consequences

**Positive:**
- `PriceHistory` and `ProductListing` remain intact after deleting a product
- Products can be audited and recovered after deletion
- Referential integrity without destructive cascades

**Negative:**
- Every query must explicitly filter `deletedAt IS NULL` — if forgotten in a new query, deleted products reappear
- Convention: all repository methods that filter deleted records use the `Active` prefix (`findActiveById`, `findAllActive`) as a reminder

**Pitfalls to avoid:**
```java
// WRONG: returns the product even if deleted
productRepository.findById(id);

// CORRECT: returns active records only
productRepository.findActiveById(id);
```

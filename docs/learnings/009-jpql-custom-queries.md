# Learning 009: JPQL Custom Queries

**Phase:** 3 (Product CRUD)
**Date:** 2026-02
**Technology:** Spring Data JPA, JPQL, `@Query`

---

## What is it?

**JPQL** (Java Persistence Query Language) is JPA's query language. Similar to SQL but works on Java entities, not tables. Spring Data JPA allows defining JPQL queries with `@Query` when derived methods (`findByXxx`) are not sufficient.

---

## Why do we need it?

Spring Data derived methods are convenient for simple queries, but have limits:

```java
// Spring Data can derive this automatically
List<Product> findByCategory(String category);

// ❌ Spring Data CANNOT derive this (soft delete + optional combined filters)
List<Product> findAllActiveWithFilters(String category, String keyword, BigDecimal min, BigDecimal max);
```

---

## Queries implemented in ProductRepository

### Filter active only (excluding soft-deleted)

```java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
List<Product> findAllActive();

@Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
Optional<Product> findActiveById(@Param("id") Long id);
```

### Keyword search (case-insensitive)

```java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL " +
       "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
List<Product> searchByKeyword(@Param("keyword") String keyword);
```

**Key technique**: `:keyword IS NULL OR ...` makes the parameter optional. If `null` is passed, the keyword condition is ignored.

### Price filters

```java
@Query("SELECT p FROM Product p " +
       "JOIN p.listings l " +
       "WHERE p.deletedAt IS NULL " +
       "AND l.currentPrice >= :minPrice")
List<Product> findByMinPrice(@Param("minPrice") BigDecimal minPrice);

@Query("SELECT p FROM Product p " +
       "JOIN p.listings l " +
       "WHERE p.deletedAt IS NULL " +
       "AND l.currentPrice <= :maxPrice")
List<Product> findByMaxPrice(@Param("maxPrice") BigDecimal maxPrice);

@Query("SELECT p FROM Product p " +
       "JOIN p.listings l " +
       "WHERE p.deletedAt IS NULL " +
       "AND l.currentPrice BETWEEN :minPrice AND :maxPrice")
List<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice);
```

### JOIN in JPQL vs SQL

In JPQL you navigate through relationships defined on the entities, not table names:

```java
// JPQL: navigates through the @OneToMany "listings" relationship
"JOIN p.listings l"

// Equivalent SQL:
"JOIN product_listings l ON l.product_id = p.id"
```

---

## Combined filter logic in the Service

`ProductService` combines filters with conditional logic in Java:

```java
public List<ProductDTO> findAll(String category, String keyword,
                                 BigDecimal minPrice, BigDecimal maxPrice) {
    List<Product> products;

    if (keyword != null && !keyword.isBlank()) {
        products = productRepository.searchByKeyword(keyword);
    } else {
        products = productRepository.findAllActive();
    }

    return products.stream()
            .filter(p -> category == null || category.equals(p.getCategory()))
            .filter(p -> minPrice == null || hasListingWithMinPrice(p, minPrice))
            .filter(p -> maxPrice == null || hasListingWithMaxPrice(p, maxPrice))
            .map(productMapper::toDTO)
            .collect(Collectors.toList());
}
```

---

## JPQL vs native SQL

| Feature | JPQL | Native SQL (`nativeQuery=true`) |
|---|---|---|
| Syntax | Java entities | SQL tables |
| Portability | Database-independent | Depends on SQL dialect |
| DB-specific functions | Limited | All DB functions available |
| When to use | Most cases | Specific functions (window functions, JSONB, etc.) |

---

## Alternative: Criteria API and Specification

For highly dynamic filters, Spring Data supports `JpaSpecificationExecutor`:

```java
// Allows building dynamic queries programmatically
Specification<Product> spec = Specification
    .where(ProductSpecifications.isActive())
    .and(ProductSpecifications.hasCategory(category))
    .and(ProductSpecifications.hasKeyword(keyword));

List<Product> results = productRepository.findAll(spec);
```

More flexible but more verbose. For this project, the filters are simple enough for JPQL + Java logic.

---

## References

- [Spring Data JPA @Query](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query)
- [JPQL Reference](https://docs.oracle.com/javaee/7/tutorial/persistence-querylanguage.htm)
- `ProductRepository.java`, `ProductService.java`

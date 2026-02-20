# Learning 002: Spring Data JPA Relationships

**Phase:** 1 (Setup + Entities)
**Date:** 2026-02
**Technology:** Spring Data JPA, Hibernate, PostgreSQL

---

## What is it?

Spring Data JPA is an abstraction over Hibernate/JPA that simplifies data access. It allows defining relationships between entities with annotations and generates SQL automatically.

---

## Project data model

```
WebsiteSource ──< ProductListing >── Product
                         │
                    PriceHistory
```

A `Product` can have listings on multiple sites. Each `ProductListing` records the current price on a specific site. `PriceHistory` stores the price history over time.

---

## Relationships implemented

### @ManyToOne with FetchType.LAZY

```java
// ProductListing.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", nullable = false)
private Product product;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "source_id", nullable = false)
private WebsiteSource source;
```

**FetchType.LAZY**: The related entity is loaded only when the getter is accessed. Avoids N+1 queries and unnecessary data loading.

**FetchType.EAGER** (avoided): Always loads the related entity, even when not needed. Causes performance issues.

### @OneToMany with CascadeType

```java
// Product.java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
private List<ProductListing> listings;
```

`mappedBy = "product"` indicates that the FK is in the `ProductListing` table, not in `Product`. Without this, Hibernate would create an unnecessary join table.

### UNIQUE constraint on column combination

```java
// ProductListing.java
@Table(uniqueConstraints = @UniqueConstraint(
    columnNames = {"product_id", "source_id"}
))
```

Guarantees that there cannot be two listings for the same product on the same site.

---

## Flyway migrations vs ddl-auto

Key decision: we use Flyway to manage the schema, not `ddl-auto: create`.

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Only validates, does not modify the schema
  flyway:
    enabled: true
    locations: classpath:db/migration
```

With `validate`, Hibernate checks at startup that the DB schema matches the entities. If there is a discrepancy, it fails with a clear error.

```sql
-- V1__create_website_sources.sql
CREATE TABLE website_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    scraper_type VARCHAR(50) NOT NULL,
    ...
);
```

---

## Bug found: @Lob vs TEXT

```java
// ❌ Initial mistake
@Lob
private String errorMessage;  // Hibernate expects type 'oid' (CLOB)

// ✅ Fix
@Column(columnDefinition = "TEXT")
private String errorMessage;  // PostgreSQL TEXT type
```

`@Lob` on PostgreSQL maps to `oid`, but Flyway creates `TEXT`. The fix is to explicitly specify the type with `columnDefinition`.

---

## Soft delete pattern

```java
// Product.java
@Column
private LocalDateTime deletedAt;  // null = active, non-null = deleted

// ProductRepository.java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
List<Product> findAllActive();

// ProductService.java
public void delete(Long id) {
    Product product = findActiveById(id);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
}
```

See ADR-005 for more details on soft deletes.

---

## Testcontainers for integration tests

```java
@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pricetracker_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

See Learning 005 for more details on Testcontainers.

---

## Alternatives considered

| Option | Pros | Cons |
|---|---|---|
| **Spring Data JPA** ✅ | Spring standard, little boilerplate | ORM overhead |
| JOOQ | Type-safe SQL, performance | More verbose, learning curve |
| MyBatis | Full SQL control | Much more code |
| Plain JDBC | Maximum control | No abstraction, very verbose |

---

## References

- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- Entities: `Product.java`, `ProductListing.java`, `PriceHistory.java`
- Migrations: `src/main/resources/db/migration/`

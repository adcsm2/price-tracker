# Learning 005: Testcontainers

**Phase:** 3 (Product CRUD)
**Date:** 2026-02
**Technology:** Testcontainers 1.19+, JUnit 5

---

## What is it?

Testcontainers is a Java library that spins up real Docker containers during integration tests. It allows testing against a real PostgreSQL database instead of using an in-memory H2.

---

## Why do we use it?

Tests with H2 (in-memory database) have a fundamental problem: H2 is not PostgreSQL. Differences include:

- Different data types (e.g. `TEXT` vs `CLOB`)
- Different SQL dialect
- Different behaviour with constraints, indexes, etc.

With Testcontainers, integration tests use **exactly the same engine** as production.

---

## Implementation in the project

### Dependency in pom.xml

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### Test setup

```java
@SpringBootTest
@Testcontainers
@Transactional
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

    @Autowired
    private ProductService productService;
}
```

### Why `static`

The container is `static` so that it starts once for the entire class, not once per test. Starting a Docker container takes ~3-5 seconds; doing so per test would be prohibitive.

### `@Transactional` in tests

With `@Transactional` on the test class, each test runs inside a transaction that is rolled back at the end. This guarantees tests are independent and do not contaminate each other.

---

## Integration test example

```java
@Test
void should_CreateProduct_Successfully() {
    ProductDTO dto = new ProductDTO();
    dto.setName("NVIDIA RTX 4070 SUPER");
    dto.setCategory("gpu");

    ProductDTO saved = productService.create(dto);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("NVIDIA RTX 4070 SUPER");
}

@Test
void should_SoftDelete_And_NotReturnInFindAll() {
    ProductDTO dto = new ProductDTO();
    dto.setName("GTX 1080");
    ProductDTO saved = productService.create(dto);

    productService.delete(saved.getId());

    List<ProductDTO> active = productService.findAll(null, null, null, null);
    assertThat(active).noneMatch(p -> p.getId().equals(saved.getId()));
}
```

---

## Flyway with Testcontainers

Flyway automatically runs migrations when Spring starts in the test context. This guarantees that the test schema is always up to date with the same scripts used in production.

---

## Comparison with alternatives

| Option | Pros | Cons |
|---|---|---|
| **Testcontainers** ✅ | Real database, faithful to production | Requires Docker, slower than H2 |
| H2 in-memory | Very fast, no Docker | Different dialect, can hide bugs |
| Dedicated test DB | Persistent data | Test contamination, manual management |
| Mockito repository mock | Ultra fast | Does not test real DB integration |

---

## Execution time

In this project, integration tests take ~5-8 seconds (mostly starting the container the first time). Unit tests take <0.5s.

The strategy is:
- **Unit tests** (scrapers, parsers): no Spring, no DB, very fast
- **Integration tests** (ProductService): Testcontainers, test the full stack

---

## References

- [Testcontainers Documentation](https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/)
- [Spring Boot + Testcontainers](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers)
- `ProductServiceIntegrationTest.java`

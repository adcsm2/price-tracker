# Learning 002: Spring Data JPA Relationships

**Fase:** 1 (Setup + Entities)
**Fecha:** 2026-02
**Tecnología:** Spring Data JPA, Hibernate, PostgreSQL

---

## ¿Qué es?

Spring Data JPA es una abstracción sobre Hibernate/JPA que simplifica el acceso a datos. Permite definir relaciones entre entidades con anotaciones y genera SQL automáticamente.

---

## Modelo de datos del proyecto

```
WebsiteSource ──< ProductListing >── Product
                         │
                    PriceHistory
```

Un `Product` puede tener listings en varios sitios. Cada `ProductListing` registra el precio actual en un sitio concreto. `PriceHistory` guarda el histórico de precios.

---

## Relaciones implementadas

### @ManyToOne con FetchType.LAZY

```java
// ProductListing.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", nullable = false)
private Product product;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "source_id", nullable = false)
private WebsiteSource source;
```

**FetchType.LAZY**: La entidad relacionada se carga solo cuando se accede al getter. Evita N+1 queries y carga innecesaria de datos.

**FetchType.EAGER** (evitado): Carga la entidad relacionada siempre, incluso cuando no la necesitas. Causa problemas de rendimiento.

### @OneToMany con CascadeType

```java
// Product.java
@OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
private List<ProductListing> listings;
```

`mappedBy = "product"` indica que la FK está en la tabla de `ProductListing`, no en `Product`. Sin esto, Hibernate crearía una tabla intermedia innecesaria.

### Restricción UNIQUE en combinación de columnas

```java
// ProductListing.java
@Table(uniqueConstraints = @UniqueConstraint(
    columnNames = {"product_id", "source_id"}
))
```

Garantiza que no puede haber dos listings del mismo producto en el mismo sitio.

---

## Flyway migrations vs ddl-auto

Decisión clave: usamos Flyway para gestionar el schema, no `ddl-auto: create`.

```yaml
# application.yml
spring:
  jpa:
    hibernate:
      ddl-auto: validate   # Solo valida, no modifica el schema
  flyway:
    enabled: true
    locations: classpath:db/migration
```

Con `validate`, Hibernate comprueba al arrancar que el schema de la BD coincide con las entidades. Si hay diferencia, falla con error claro.

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

## Error encontrado: @Lob vs TEXT

```java
// ❌ Error inicial
@Lob
private String errorMessage;  // Hibernate espera tipo 'oid' (CLOB)

// ✅ Corrección
@Column(columnDefinition = "TEXT")
private String errorMessage;  // Tipo TEXT de PostgreSQL
```

`@Lob` en PostgreSQL mapea a `oid`, pero Flyway crea `TEXT`. La solución es especificar explícitamente el tipo con `columnDefinition`.

---

## Soft delete pattern

```java
// Product.java
@Column
private LocalDateTime deletedAt;  // null = activo, non-null = eliminado

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

Ver Learning 006 para más detalles sobre soft deletes.

---

## TestContainers para tests de integración

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

Ver Learning 005 para más detalles sobre TestContainers.

---

## Alternativas consideradas

| Opción | Pros | Contras |
|--------|------|---------|
| **Spring Data JPA** ✅ | Estándar Spring, poco boilerplate | ORM overhead |
| JOOQ | SQL tipado, rendimiento | Más verboso, curva de aprendizaje |
| MyBatis | Control total del SQL | Mucho más código |
| JDBC puro | Máximo control | Sin abstracción, verbosísimo |

---

## Referencias

- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- Entidades: `Product.java`, `ProductListing.java`, `PriceHistory.java`
- Migraciones: `src/main/resources/db/migration/`

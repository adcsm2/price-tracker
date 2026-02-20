# Learning 009: JPQL Custom Queries

**Fase:** 3 (Product CRUD)
**Fecha:** 2026-02
**Tecnología:** Spring Data JPA, JPQL, `@Query`

---

## ¿Qué es?

**JPQL** (Java Persistence Query Language) es el lenguaje de consultas de JPA. Similar a SQL pero trabaja sobre entidades Java, no sobre tablas. Spring Data JPA permite definir queries JPQL con `@Query` cuando los métodos derivados (`findByXxx`) no son suficientes.

---

## ¿Por qué lo necesitamos?

Los métodos derivados de Spring Data son convenientes para queries simples, pero tienen límites:

```java
// Spring Data puede derivar esto automáticamente
List<Product> findByCategory(String category);

// ❌ Spring Data NO puede derivar esto (soft delete + filtros opcionales combinados)
List<Product> findAllActiveWithFilters(String category, String keyword, BigDecimal min, BigDecimal max);
```

---

## Queries implementadas en ProductRepository

### Filtrar solo activos (sin soft delete)

```java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
List<Product> findAllActive();

@Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
Optional<Product> findActiveById(@Param("id") Long id);
```

### Búsqueda por keyword (case-insensitive)

```java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL " +
       "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
List<Product> searchByKeyword(@Param("keyword") String keyword);
```

**Técnica clave**: `:keyword IS NULL OR ...` hace el parámetro opcional. Si se pasa `null`, la condición del keyword se ignora.

### Filtros de precio

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

### JOIN en JPQL vs SQL

En JPQL se navega por las relaciones definidas en las entidades, no por nombres de tablas:

```java
// JPQL: navega por la relación @OneToMany "listings"
"JOIN p.listings l"

// SQL equivalente:
"JOIN product_listings l ON l.product_id = p.id"
```

---

## Lógica de filtros combinados en el Service

El `ProductService` combina los filtros con lógica condicional en Java:

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

## JPQL vs SQL nativo

| Característica | JPQL | SQL nativo (`nativeQuery=true`) |
|---------------|------|--------------------------------|
| Sintaxis | Entidades Java | Tablas SQL |
| Portabilidad | Independiente de BD | Depende del dialecto SQL |
| Funciones específicas | Limitadas | Todas las de la BD |
| Cuándo usarlo | Mayoría de casos | Funciones específicas (window functions, JSONB, etc.) |

---

## Alternativa: Criteria API y Specification

Para filtros muy dinámicos, Spring Data soporta `JpaSpecificationExecutor`:

```java
// Permite construir queries dinámicas programáticamente
Specification<Product> spec = Specification
    .where(ProductSpecifications.isActive())
    .and(ProductSpecifications.hasCategory(category))
    .and(ProductSpecifications.hasKeyword(keyword));

List<Product> results = productRepository.findAll(spec);
```

Más flexible pero más verboso. Para este proyecto, los filtros son suficientemente simples para JPQL + lógica Java.

---

## Referencias

- [Spring Data JPA @Query](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query)
- [JPQL Reference](https://docs.oracle.com/javaee/7/tutorial/persistence-querylanguage.htm)
- `ProductRepository.java`, `ProductService.java`

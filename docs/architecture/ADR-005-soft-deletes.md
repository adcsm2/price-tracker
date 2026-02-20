# ADR-005: Soft Delete con campo deletedAt

**Estado:** Aceptado
**Fecha:** 2026-02
**Fase:** 3 (Product CRUD)

---

## Contexto

El proyecto necesita una estrategia para eliminar productos. Un hard delete (`DELETE FROM products`) destruye el registro permanentemente, lo que plantea problemas:

- El `PriceHistory` y `ProductListing` de ese producto quedarían huérfanos (violación de FK o pérdida de datos históricos)
- Sin posibilidad de recuperar un producto eliminado por error
- Sin auditoría de qué productos existieron y cuándo se eliminaron

---

## Decisión

Soft delete mediante campo `deletedAt` en la entidad `Product`:

```java
@Column
private LocalDateTime deletedAt;  // null = activo, non-null = eliminado
```

El service setea `deletedAt = now()` en lugar de borrar la fila. Todas las queries del repository filtran `WHERE deleted_at IS NULL`.

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

## Alternativas descartadas

| Opción | Pros | Contras |
|---|---|---|
| **`deletedAt` timestamp** ✅ | Explícito, guarda cuándo se borró, estándar JPA | Hay que filtrar en cada query |
| `@SQLDelete + @Where` de Hibernate | Automático, menos código | Magia implícita, no es JPA estándar |
| Campo `active: boolean` | Semántico | Pierde información de cuándo se borró |
| Hard delete | Simple | Sin auditoría, rompe historial de precios |

### Por qué no `@SQLDelete + @Where`

Hibernate permite automatizar el soft delete:

```java
@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
public class Product { ... }
```

Se descartó porque `@Where` es una anotación propietaria de Hibernate (no JPA estándar) y oculta el comportamiento. Las queries JPQL explícitas dejan claro que solo devuelven registros activos, sin magia implícita.

---

## Consecuencias

**Positivas:**
- `PriceHistory` y `ProductListing` permanecen intactos tras eliminar un producto
- Posible auditoría y recuperación de productos eliminados
- Integridad referencial sin cascadas destructivas

**Negativas:**
- Todas las queries deben filtrar `deletedAt IS NULL` explícitamente — si se olvida en una query nueva, los productos borrados reaparecen
- Convención: todos los métodos del repository que filtran borrados usan el prefijo `Active` (`findActiveById`, `findAllActive`) para recordarlo

**Trampas a evitar:**
```java
// MAL: devuelve el producto aunque esté borrado
productRepository.findById(id);

// BIEN: solo devuelve activos
productRepository.findActiveById(id);
```

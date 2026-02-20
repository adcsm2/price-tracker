# Learning 006: Soft Deletes

**Fase:** 3 (Product CRUD)
**Fecha:** 2026-02
**Tecnología:** Spring Data JPA, JPQL

---

## ¿Qué es?

Un **soft delete** (borrado lógico) no elimina el registro físicamente de la base de datos. En su lugar, marca el registro con una timestamp de cuándo fue "borrado". Las queries normales filtran estos registros.

```sql
-- Hard delete (lo que NO hacemos)
DELETE FROM products WHERE id = 42;

-- Soft delete (lo que hacemos)
UPDATE products SET deleted_at = NOW() WHERE id = 42;
```

---

## ¿Por qué lo usamos?

1. **Auditoría**: Podemos ver qué productos existieron y cuándo se eliminaron
2. **Recuperación**: Posible restaurar un producto eliminado por error
3. **Integridad referencial**: Un `ProductListing` puede referenciar un producto eliminado sin romper FKs
4. **Historial de precios**: El `PriceHistory` de un producto borrado permanece intacto

---

## Implementación en el proyecto

### Campo en la entidad

```java
// Product.java
@Column
private LocalDateTime deletedAt;  // null = activo, non-null = eliminado
```

Sin `nullable = false` porque `null` es el estado normal (activo).

### Repository con queries que filtran borrados

```java
// ProductRepository.java
@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL")
List<Product> findAllActive();

@Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
Optional<Product> findActiveById(@Param("id") Long id);

@Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL " +
       "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
List<Product> searchByKeyword(@Param("keyword") String keyword);
```

### Service que realiza el soft delete

```java
// ProductService.java
public void delete(Long id) {
    Product product = productRepository.findActiveById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
}
```

### Controller que devuelve 204

```java
// ProductController.java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(@PathVariable Long id) {
    productService.delete(id);
}
```

---

## Errores comunes a evitar

### ❌ Error: usar el método `findById()` estándar tras soft delete

```java
// Mal: devuelve el producto aunque esté borrado
Optional<Product> p = productRepository.findById(id);
```

```java
// Bien: solo devuelve activos
Optional<Product> p = productRepository.findActiveById(id);
```

### ❌ Error: olvidar filtrar en el método `findAll()`

Si olvidas el filtro en alguna query personalizada, los productos borrados reaparecen. Solución: nombrar todos los métodos con `Active` para recordar que están filtrados.

---

## Alternativa: @SQLDelete + @Where de Hibernate

Hibernate permite automatizar el soft delete con anotaciones:

```java
@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
public class Product { ... }
```

Con esto, el `DELETE` de JPA se convierte en `UPDATE`, y `@Where` filtra automáticamente en todas las queries.

**Por qué no lo usamos:** `@Where` es una anotación de Hibernate (no JPA estándar) y estamos prefiriendo mantener el código más explícito y legible. Las queries JPQL personalizadas dejan claro que solo devuelven registros activos.

---

## Alternativas consideradas

| Opción | Pros | Contras |
|--------|------|---------|
| **Campo `deletedAt`** ✅ | Simple, explícito, guarda cuándo se borró | Hay que recordar filtrar en cada query |
| `@SQLDelete + @Where` | Automático, menos código | Magia de Hibernate, menos explícito |
| Campo `active: boolean` | Más semántico | Pierde información de cuándo se borró |
| Hard delete | Más simple | Sin auditoría, sin recuperación |

---

## Referencias

- [JPA Soft Delete Patterns](https://thorben-janssen.com/implement-soft-delete-hibernate/)
- `Product.java`, `ProductRepository.java`, `ProductService.java`

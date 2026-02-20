# ADR-003: Modelo de Unificación de Productos

**Estado:** Aceptado
**Fecha:** 2026-02
**Fase:** 6 (Price History + Analytics)

---

## Contexto

El proyecto scrape el mismo producto físico (ej. "ASUS TUF RTX 4070") en múltiples tiendas (Amazon, MediaMarkt). Sin una estrategia de unificación, tendríamos:

- Una fila por tienda, sin relación entre ellas → imposible comparar precios entre sitios
- Historial de precios desconectado por producto canónico
- Analytics sin sentido (¿cuál es "el precio" de un producto si existen N filas?)

---

## Problema

¿Cómo modelar que el mismo producto físico aparece en varias tiendas, cada una con su propia URL, precio y disponibilidad, manteniendo un historial de precios unificado?

---

## Decisión

Modelo de tres capas:

```
Product (canónico)
  └── ProductListing (una fila por tienda)
        └── PriceHistory (registro temporal de cada scrape)
```

**`Product`** representa el producto físico. Se crea una sola vez, identificado por nombre (case-insensitive). Es la entidad sobre la que se crean alertas y se calculan analytics.

**`ProductListing`** representa la presencia de ese producto en una tienda concreta. Tiene restricción única `(product_id, source_id)` para evitar duplicados. Almacena `currentPrice`, `inStock`, `lastScrapedAt`.

**`PriceHistory`** registra cada scrape con timestamp. Es append-only, nunca se modifica. Permite calcular variaciones, tendencias y comparativas en el tiempo.

### Estrategia de matching por nombre

Cuando se scrape un producto nuevo:
1. Buscar `ProductListing` por URL exacta → si existe, actualizar precio
2. Si no existe → buscar `Product` por nombre (case-insensitive) → reutilizar si existe
3. Si no existe producto → crear nuevo `Product` + `ProductListing`

Este matching por nombre permite que "ASUS TUF RTX 4070" de Amazon y de MediaMarkt compartan la misma fila de `Product`, y sus precios sean comparables vía `PriceComparison`.

### Transacciones por ítem con TransactionTemplate

`ProductUnificationService.saveResults()` usa `TransactionTemplate.executeWithoutResult()` en lugar de `@Transactional` en el método público. Motivo: si se usa `@Transactional` + try/catch en el mismo método, al capturar una excepción JPA la transacción ya está marcada como `rollback-only`, y cualquier intento de continuar lanza `UnexpectedRollbackException`.

Con `TransactionTemplate`, cada ítem se ejecuta en su propia transacción independiente. Un fallo en el ítem N no afecta a los ítems N+1..M.

---

## Alternativas descartadas

### Una tabla plana por scrape

```
scrape_results(id, site, name, price, url, scraped_at)
```

**Descartada** porque no permite agrupar el mismo producto entre sitios, comparar precios entre tiendas ni calcular tendencias de un producto canónico.

### Matching por URL solamente

Sin matching por nombre, Amazon y MediaMarkt siempre crearían `Product` distintos aunque sean el mismo artículo físico. El modelo canónico perdería su razón de ser.

### @Transactional + savepoint manual

Más complejo, no portable entre proveedores JPA. `TransactionTemplate` es la solución idiomática de Spring para transacciones programáticas.

---

## Consecuencias

**Positivas:**
- Analytics y comparativas de precio entre tiendas son triviales
- El historial de precios es por producto canónico, no por listing
- Un fallo al guardar un ítem no cancela todo el batch

**Negativas:**
- El matching por nombre es frágil: "ASUS TUF RTX 4070 Ti" vs "ASUS TUF Gaming RTX 4070 Ti" crearían productos distintos aunque sean el mismo
- Sin normalización de nombres, la unificación entre sitios en producción real sería parcial
- Solución aceptable para portfolio, pero en producción requeriría fuzzy matching o un catálogo de productos externo (EAN/ASIN)

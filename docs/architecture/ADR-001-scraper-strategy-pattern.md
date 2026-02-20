# ADR-001: Strategy Pattern para Scrapers

**Estado:** Aceptado
**Fecha:** 2026-02
**Fase:** 2 (Amazon Scraper)

---

## Contexto

El proyecto necesita scraper precios de múltiples sitios web. Cada sitio tiene una estructura HTML completamente diferente:
- Amazon: HTML server-rendered con atributos `data-component-type`
- MediaMarkt: SPA React con JSON-LD embebido
- PCComponentes: API JSON interna (bloqueada por Cloudflare)

La lógica de scraping de cada sitio es independiente y no tiene nada en común más allá de la interfaz (recibir keyword, devolver lista de productos).

---

## Problema

¿Cómo organizar el código de scraping para que sea extensible (añadir sitios nuevos fácilmente) sin modificar el código existente?

---

## Opciones consideradas

### Opción 1: Un método con if/else por sitio

```java
public List<ScrapedProductDTO> scrape(String keyword, ScraperType type) {
    if (type == AMAZON) {
        // lógica Amazon
    } else if (type == MEDIAMARKT) {
        // lógica MediaMarkt
    }
}
```

**Pros:** Simple al inicio, sin abstracciones
**Contras:** Viola Open/Closed Principle. Añadir un sitio requiere modificar este método. Difícil de testear en aislamiento. Un error en un sitio puede afectar a otros.

### Opción 2: Herencia (`AbstractScraper`)

```java
public abstract class AbstractScraper {
    protected abstract Document fetchPage(String url) throws IOException;
    public List<ScrapedProductDTO> scrape(String keyword) {
        Document doc = fetchPage(buildUrl(keyword));
        return parse(doc);
    }
}
```

**Pros:** Comparte código común (rate limiting, retry)
**Contras:** La herencia es rígida. Si un scraper necesita un enfoque radicalmente diferente (RestTemplate en vez de Jsoup, como PCComponentes inicialmente), se complica. Además, la lógica de fetch+parse puede ser muy diferente entre sitios.

### Opción 3: Strategy Pattern con interfaz ✅

```java
public interface SiteScraper {
    ScraperType getScraperType();
    List<ScrapedProductDTO> scrape(String keyword, String category);
}
```

Cada scraper es un `@Service` independiente que implementa la interfaz. `ScraperFactory` los registra automáticamente vía Spring DI.

**Pros:** Extensible sin modificar código existente. Cada scraper es totalmente independiente y testeable. Spring inyecta automáticamente todos los scrapers. Clara separación de responsabilidades.
**Contras:** Más clases. No hay reutilización explícita de código entre scrapers (aunque esto es una ventaja en este caso).

---

## Decisión

**Opción 3: Strategy Pattern.**

El criterio decisivo fue la extensibilidad y la heterogeneidad de los scrapers. Al descubrir que PCComponentes requería RestTemplate en lugar de Jsoup (y luego que ni eso funcionaba), quedó claro que cada sitio puede requerir un enfoque completamente diferente. La herencia habría forzado acoplamiento artificial.

---

## Consecuencias

### Positivas
- Añadir un scraper nuevo = crear una clase `@Service` que implemente `SiteScraper`. Cero cambios en código existente.
- Cada scraper se puede testear de forma completamente independiente.
- El código de cada sitio está autocontenido: fácil de leer, mantener y depurar.

### Negativas
- Duplicación de setup (RateLimiter, Retryable) en cada scraper. Aceptable porque cada sitio puede necesitar configuración diferente.

---

## Implementación

```
SiteScraper (interface)
├── AmazonScraper (@Service)     → Jsoup HTML
└── MediaMarktScraper (@Service) → Jsoup + JSON-LD parsing

ScraperFactory (@Service)
└── Recibe List<SiteScraper> por DI → Map<ScraperType, SiteScraper>
```

Spring inyecta automáticamente todos los beans `SiteScraper` en la lista, sin registro manual.

---

## Referencias

- [Strategy Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/strategy)
- [Open/Closed Principle](https://en.wikipedia.org/wiki/Open%E2%80%93closed_principle)
- Ver Learning 007 para detalles de implementación

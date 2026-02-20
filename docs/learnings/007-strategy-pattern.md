# Learning 007: Strategy Pattern

**Fase:** 2-4 (Amazon + MediaMarkt scrapers)
**Fecha:** 2026-02
**Tecnología:** Java, Spring Boot DI

---

## ¿Qué es?

El **Strategy Pattern** es un patrón de diseño de comportamiento. Define una familia de algoritmos, encapsula cada uno en su propia clase, y los hace intercambiables. El cliente trabaja con la interfaz, sin conocer la implementación concreta.

---

## El problema que resuelve

Cada sitio web tiene HTML/API distinto. Sin Strategy Pattern:

```java
// ❌ Sin patrón: if/else en el código cliente
public List<ScrapedProductDTO> scrape(String keyword, ScraperType type) {
    if (type == ScraperType.AMAZON) {
        // lógica Amazon...
    } else if (type == ScraperType.MEDIAMARKT) {
        // lógica MediaMarkt...
    } else if (type == ScraperType.NEW_SITE) {  // ← hay que modificar este método
        // ...
    }
}
```

Cada sitio nuevo requiere modificar el código existente (viola el **Open/Closed Principle**).

---

## Implementación en el proyecto

### La interfaz (el contrato)

```java
// SiteScraper.java
public interface SiteScraper {
    String getSiteName();
    ScraperType getScraperType();
    List<ScrapedProductDTO> scrape(String keyword, String category);
}
```

### Las estrategias concretas

```java
// AmazonScraper.java
@Service
public class AmazonScraper implements SiteScraper {

    @Override
    public ScraperType getScraperType() { return ScraperType.AMAZON; }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        // Lógica específica de Amazon (Jsoup, selectores HTML)
    }
}

// MediaMarktScraper.java
@Service
public class MediaMarktScraper implements SiteScraper {

    @Override
    public ScraperType getScraperType() { return ScraperType.MEDIAMARKT; }

    @Override
    public List<ScrapedProductDTO> scrape(String keyword, String category) {
        // Lógica específica de MediaMarkt (Jsoup + JSON-LD)
    }
}
```

### La fábrica (ScraperFactory)

`ScraperFactory` usa Spring DI para recoger automáticamente todos los beans que implementan `SiteScraper`:

```java
// ScraperFactory.java
@Service
public class ScraperFactory {

    private final Map<ScraperType, SiteScraper> scrapers;

    public ScraperFactory(List<SiteScraper> scraperList) {
        this.scrapers = scraperList.stream()
                .collect(Collectors.toMap(
                        SiteScraper::getScraperType,
                        Function.identity()
                ));
    }

    public SiteScraper getScraper(ScraperType type) {
        SiteScraper scraper = scrapers.get(type);
        if (scraper == null) {
            throw new IllegalArgumentException("No scraper registered for: " + type);
        }
        return scraper;
    }

    public List<SiteScraper> getAllScrapers() {
        return List.copyOf(scrapers.values());
    }
}
```

**Clave**: `ScraperFactory` recibe `List<SiteScraper>` en el constructor. Spring inyecta automáticamente todos los beans que implementen `SiteScraper`. No hay que registrar nada manualmente.

### Uso desde el cliente

```java
// Futuro ScrapingJobService.java
SiteScraper scraper = scraperFactory.getScraper(ScraperType.AMAZON);
List<ScrapedProductDTO> results = scraper.scrape("rtx 4070", "gpu");
```

---

## Añadir un nuevo sitio

Para añadir soporte de un sitio nuevo solo hay que:

1. Añadir el valor al enum `ScraperType`
2. Crear una clase `@Service` que implemente `SiteScraper`
3. ✅ Listo. Spring lo registra automáticamente en `ScraperFactory`

No hay que tocar `ScraperFactory` ni ningún código existente.

---

## Comparación de alternativas

| Opción | Pros | Contras |
|--------|------|---------|
| **Strategy Pattern** ✅ | Extensible, Open/Closed, testeable | Más clases |
| if/else gigante | Simple al inicio | Se vuelve inmanejable, viola OCP |
| Herencia (`AbstractScraper`) | Comparte código común | Herencia rígida, difícil de testear |
| Plugins/reflection | Extensible sin recompile | Muy complejo, overkill |

---

## Referencias

- [Strategy Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/strategy)
- [Open/Closed Principle](https://en.wikipedia.org/wiki/Open%E2%80%93closed_principle)
- Ver ADR-001 para la decisión arquitectónica completa
- `SiteScraper.java`, `AmazonScraper.java`, `MediaMarktScraper.java`, `ScraperFactory.java`

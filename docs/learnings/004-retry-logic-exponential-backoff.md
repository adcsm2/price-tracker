# Learning 004: Retry Logic con Exponential Backoff

**Fase:** 2 (Amazon Scraper)
**Fecha:** 2026-02
**Tecnología:** Spring Retry, `@Retryable`, `@Backoff`

---

## ¿Qué es?

El **retry con exponential backoff** es un patrón de resiliencia: cuando una operación falla por un error transitorio, se reintenta automáticamente esperando un tiempo que crece exponencialmente entre intentos.

```
Intento 1 → falla → espera 1s
Intento 2 → falla → espera 2s
Intento 3 → falla → lanza excepción
```

---

## ¿Por qué lo necesitamos?

El scraping puede fallar por razones transitorias:
- Timeout de red puntual
- El servidor devuelve 503 temporalmente
- Pico de tráfico en el servidor objetivo

Reintentar automáticamente hace el scraper mucho más robusto sin código manual de loops y contadores.

---

## Implementación con Spring Retry

### Habilitación en la configuración

```java
// ScraperConfig.java
@Configuration
@EnableRetry  // ← Activa el soporte de @Retryable en toda la app
public class ScraperConfig {
    // ...
}
```

### Anotación en el método que puede fallar

```java
// AmazonScraper.java
@Retryable(
        retryFor = IOException.class,   // Solo reintenta para este tipo
        maxAttempts = 3,                 // Máximo 3 intentos
        backoff = @Backoff(delay = 1000, multiplier = 2)  // 1s, 2s
)
public Document fetchSearchPage(String keyword) throws IOException {
    rateLimiter.acquire();
    return Jsoup.connect(searchUrl).get();
}
```

### Separación entre fetch y parse

Importante: `@Retryable` está en `fetchSearchPage()`, no en `scrape()`. Así se reintenta solo la parte de red, no el parsing:

```java
@Override
public List<ScrapedProductDTO> scrape(String keyword, String category) {
    try {
        Document doc = fetchSearchPage(keyword);  // ← retry aquí
        return parseSearchResults(doc);            // ← no retry aquí
    } catch (IOException e) {
        log.error("Error scraping Amazon: {}", e.getMessage());
        return new ArrayList<>();
    }
}
```

---

## Cómo funciona `@Retryable` internamente

Spring Retry usa AOP (proxies). La clase debe ser un bean Spring para que funcione. Si llamas al método `fetchSearchPage()` desde dentro de la misma clase con `this.fetchSearchPage()`, el proxy se salta → no funciona el retry.

Por eso, `scrape()` llama a `fetchSearchPage()` directamente (no `this.`), pero al ser llamado desde el controlador (a través del proxy), el retry SÍ funciona.

---

## Configuración de backoff

```java
@Backoff(delay = 1000, multiplier = 2)
// Intento 1 falla → espera 1000ms
// Intento 2 falla → espera 2000ms (1000 × 2)
// Intento 3 falla → lanza la excepción
```

Con `maxDelay` podría limitarse el tiempo máximo de espera:
```java
@Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
```

---

## Graceful degradation

Si todos los reintentos fallan, el scraper devuelve lista vacía en lugar de propagar la excepción:

```java
} catch (IOException e) {
    log.error("Error scraping Amazon for keyword '{}': {}", keyword, e.getMessage());
    return new ArrayList<>();  // ← nunca falla el endpoint, devuelve vacío
}
```

---

## Alternativas consideradas

| Opción | Pros | Contras |
|--------|------|---------|
| **Spring Retry** ✅ | Integrado en Spring, anotación simple | Requiere `@EnableRetry` |
| Manual (try/catch loop) | Sin dependencias | Código repetitivo, difícil de mantener |
| Resilience4j | Más features (circuit breaker, bulkhead) | Más configuración, overkill para este caso |
| Polly (C#) | N/A | Wrong language |

---

## Referencias

- [Spring Retry Documentation](https://github.com/spring-projects/spring-retry)
- [Exponential Backoff con Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
- `AmazonScraper.java`, `MediaMarktScraper.java`

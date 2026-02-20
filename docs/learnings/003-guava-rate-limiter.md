# Learning 003: Guava RateLimiter

**Fase:** 2 (Amazon Scraper)
**Fecha:** 2026-02
**Tecnología:** Google Guava 32.1.3

---

## ¿Qué es?

`RateLimiter` de Guava implementa el algoritmo **Token Bucket**: genera tokens a una tasa constante y los consumidores deben adquirir un token antes de proceder. Si no hay tokens disponibles, el hilo se bloquea hasta que haya uno.

---

## ¿Por qué lo necesitamos?

Al hacer web scraping, bombardear un servidor con requests puede:
- Resultar en IP ban
- Sobrecargar el servidor (problema ético y legal)
- Generar respuestas 429 Too Many Requests

La solución es limitar la velocidad de requests a un máximo configurable por sitio.

---

## Implementación en el proyecto

### Configuración por sitio en application.yml

```yaml
scraper:
  rate-limit:
    amazon: 2.0       # 2 requests/segundo
    mediamarkt: 2.0   # 2 requests/segundo
```

### Un RateLimiter por scraper

```java
// AmazonScraper.java
public class AmazonScraper implements SiteScraper {

    private final RateLimiter rateLimiter;

    public AmazonScraper(ScraperConfig scraperConfig) {
        double rate = scraperConfig.getRateLimitForSite("amazon");
        this.rateLimiter = RateLimiter.create(rate);  // tokens/segundo
    }

    public Document fetchSearchPage(String keyword) throws IOException {
        rateLimiter.acquire();  // Bloquea hasta que haya un token disponible
        // ... hace el request
    }
}
```

### Configuración centralizada

```java
// ScraperConfig.java
@ConfigurationProperties(prefix = "scraper")
public class ScraperConfig {

    private Map<String, Double> rateLimit;

    public double getRateLimitForSite(String site) {
        return rateLimit.getOrDefault(site, 2.0);  // Default: 2 req/sec
    }
}
```

---

## Cómo funciona el Token Bucket

```
Tiempo:  0s    0.5s   1s    1.5s   2s
Tokens:  [■■]  [■■■]  [■■■■■] [■■]  [■■■]
          ↓                   ↓
        acquire()           acquire()
        (inmediato)         (inmediato)
```

Con `RateLimiter.create(2.0)`:
- Se generan 2 tokens por segundo
- `acquire()` consume 1 token
- Si no hay tokens, espera hasta que se genere uno
- Los tokens se acumulan (hasta cierto límite) si no se consumen

---

## En tests: rate alto para no ralentizar

```java
@BeforeEach
void setUp() {
    ScraperConfig config = new ScraperConfig();
    config.setRateLimit(Map.of("amazon", 10.0));  // 10 req/sec en tests
    scraper = new AmazonScraper(config);
}
```

---

## Alternativas consideradas

| Opción | Pros | Contras |
|--------|------|---------|
| **Guava RateLimiter** ✅ | Simple, built-in token bucket, sin deps extra | Solo en-proceso (no distribuido) |
| `Thread.sleep()` | Trivial | No es adaptativo, bloquea innecesariamente |
| `Semaphore` | Control de concurrencia | No limita por tiempo, más complejo |
| Bucket4j | Distribuido (Redis), más features | Dependencia extra, overkill para este proyecto |
| Resilience4j RateLimiter | Integrado con Spring | Más config, overkill |

---

## Limitación: solo en-proceso

Si escaláramos a múltiples instancias de la aplicación, cada una tendría su propio `RateLimiter` y la limitación real sería `rate × nInstancias`. Para ese escenario habría que usar un rate limiter distribuido (ej: Bucket4j + Redis).

Para este proyecto (single instance, portfolio), Guava es más que suficiente.

---

## Referencias

- [Guava RateLimiter Javadoc](https://guava.dev/releases/32.0/api/docs/com/google/common/util/concurrent/RateLimiter.html)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- Ver ADR-002 para la decisión de elegir Guava frente a otras opciones
- `AmazonScraper.java`, `MediaMarktScraper.java`, `ScraperConfig.java`

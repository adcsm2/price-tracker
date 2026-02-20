# ADR-002: Rate Limiting con Guava RateLimiter

**Estado:** Aceptado
**Fecha:** 2026-02
**Fase:** 2 (Amazon Scraper)

---

## Contexto

El scraper hace requests HTTP a sitios de terceros. Sin control de velocidad, podría:
- Generar un ban de IP por parte del sitio
- Sobrecargar el servidor objetivo (problema ético)
- Recibir respuestas 429 Too Many Requests

Necesitamos un mecanismo que limite la tasa de requests a un máximo configurable por sitio.

---

## Problema

¿Cómo implementar rate limiting por sitio de forma simple, configurable y correcta?

---

## Opciones consideradas

### Opción 1: `Thread.sleep()` fijo

```java
public Document fetchPage(String url) throws IOException {
    Thread.sleep(500);  // espera 500ms entre requests
    return Jsoup.connect(url).get();
}
```

**Pros:** Trivial de implementar, sin dependencias.
**Contras:** No es adaptativo. Si el request tarda 450ms, la pausa total es 950ms, no 500ms. El delay es fijo independientemente de la carga. Si se usa concurrencia, `sleep` en un hilo no impide que otros hagan requests simultáneamente.

### Opción 2: `Semaphore` de Java

```java
private final Semaphore semaphore = new Semaphore(1);  // 1 request a la vez

public Document fetchPage(String url) throws IOException {
    semaphore.acquire();
    try {
        return Jsoup.connect(url).get();
    } finally {
        semaphore.release();
    }
}
```

**Pros:** Thread-safe, control de concurrencia.
**Contras:** Controla concurrencia, no velocidad. No garantiza un máximo de N requests por segundo, solo que hay máximo 1 a la vez. Para simular rate limiting habría que combinar con sleep, aumentando la complejidad.

### Opción 3: Guava RateLimiter ✅

```java
private final RateLimiter rateLimiter = RateLimiter.create(2.0);

public Document fetchPage(String url) throws IOException {
    rateLimiter.acquire();  // Bloquea hasta que haya un token
    return Jsoup.connect(url).get();
}
```

**Pros:** Implementa Token Bucket correctamente. Thread-safe. Configurable (tokens/segundo). `acquire()` bloquea el tiempo mínimo necesario. Ya incluida en Guava (ya teníamos la dependencia).

**Contras:** Solo funciona en un único proceso (no distribuido). Pero para este proyecto (una sola instancia) es suficiente.

### Opción 4: Bucket4j con Redis

Rate limiting distribuido usando Redis como almacén compartido de tokens.

**Pros:** Funciona con múltiples instancias de la aplicación.
**Contras:** Requiere Redis (infraestructura extra), dependencia adicional, mucha más configuración. Overkill para un proyecto de portfolio con una sola instancia.

---

## Decisión

**Opción 3: Guava RateLimiter.**

Es la opción que proporciona el comportamiento correcto (Token Bucket, thread-safe, configurable) con el menor coste de complejidad. Guava ya estaba en el classpath por otra razón, así que no añade dependencia nueva.

---

## Consecuencias

### Positivas
- 2 líneas de código por scraper para rate limiting correcto
- Configurable por sitio desde `application.yml`
- Thread-safe sin código adicional

### Negativas
- Solo funciona en un proceso. Si el proyecto escala a múltiples instancias (fuera del scope del portfolio), habría que migrar a Bucket4j + Redis.

---

## Configuración

```yaml
# application.yml
scraper:
  rate-limit:
    amazon: 2.0      # 2 req/seg
    mediamarkt: 2.0  # 2 req/seg
```

```java
// ScraperConfig.java
public double getRateLimitForSite(String site) {
    return rateLimit.getOrDefault(site, 2.0);  // Default seguro
}
```

En tests se configura con rate alto (10 req/seg) para no ralentizar la suite.

---

## Referencias

- [Guava RateLimiter](https://guava.dev/releases/32.0/api/docs/com/google/common/util/concurrent/RateLimiter.html)
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket)
- Ver Learning 003 para detalles de implementación

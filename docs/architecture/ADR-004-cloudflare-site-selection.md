# ADR-003: Selección de Segundo Sitio ante Bloqueo de Cloudflare

**Estado:** Aceptado
**Fecha:** 2026-02
**Fase:** 4 (Segundo Scraper)

---

## Contexto

La Fase 4 planificaba PCComponentes como segundo sitio a scraper. Durante el desarrollo se descubrió que PCComponentes bloquea los clientes HTTP no-browser con Cloudflare's **Interactive Challenge** (`cType: interactive`).

Historial del proceso:

1. **Intento 1**: Jsoup directo → Cloudflare lo bloquea (el HTML tiene un desafío JS en lugar de los datos)
2. **Intento 2**: Descubrir la API JSON interna del sitio (`/api/articles/search`) y acceder con RestTemplate → también bloqueado con 403 + Cloudflare challenge
3. **Diagnóstico**: El error de la respuesta confirma el tipo de bloqueo:
   ```
   ERROR: HttpClientErrorException$Forbidden - 403 Forbidden
   response: "cType: 'interactive'" ... "Enable JavaScript and cookies to continue"
   ```

El challenge interactivo de Cloudflare requiere ejecución de JavaScript real y resolución de cookies criptográficas. Ningún cliente HTTP estándar (Jsoup, RestTemplate, HttpClient) puede resolverlo.

---

## Problema

¿Cómo proceder con el segundo scraper dado que PCComponentes bloquea todos los clientes HTTP?

---

## Opciones consideradas

### Opción 1: Selenium/Playwright para PCComponentes

Usar un navegador headless (Chrome/Firefox) que ejecute JavaScript y resuelva el challenge automáticamente.

**Pros:** Solución técnicamente completa. PCComponentes sería funcional.
**Contras:**
- Requiere ChromeDriver o Playwright binaries (>100MB)
- Consume mucha más CPU/memoria que Jsoup
- Los requests tardan 3-5 segundos en lugar de ~200ms
- Cloudflare puede seguir detectando headless browsers y bloquearlos
- Añade complejidad operativa significativa sin aportar valor nuevo al patrón Strategy (ya demostrado con Amazon)

### Opción 2: Scraping API (ScraperAPI, BrightData)

Usar un servicio proxy que maneje el bypass de Cloudflare. El request va a través de su infraestructura.

**Pros:** No requiere cambios de arquitectura. Solo cambia la URL del request.
**Contras:** Dependencia de servicio externo de pago (aunque tiene free tier). Introduce latencia extra. Para un portfolio educativo, añade complejidad de configuración sin valor pedagógico claro.

### Opción 3: Cambiar al sitio PCComponentes → MediaMarkt y documentar el bloqueo ✅

Reemplazar PCComponentes con MediaMarkt ES, que:
- No usa Cloudflare Interactive Challenge
- Aunque es SPA (React), embebe datos estructurados en **JSON-LD** (`schema.org/ItemList`) que Jsoup puede extraer directamente sin ejecutar JS

Documentar el bloqueo de PCComponentes en el README como decisión arquitectónica consciente.

**Pros:**
- Scraper funcional con la misma arquitectura (Jsoup)
- Demuestra capacidad de adaptación ante obstáculos reales
- JSON-LD es más robusto que CSS selectors (no cambia con redesigns de UI)
- El bloqueo de PCComponentes es una realidad del sector que vale la pena documentar

**Contras:** PCComponentes no está en el portfolio final como sitio funcional.

### Opción 4: Mock/simulación de PCComponentes

Implementar un scraper que devuelva datos ficticios hardcodeados para demostrar el patrón.

**Pros:** Sin bloqueos, patrón demostrado.
**Contras:** Deshonesto en un proyecto de portfolio. No demuestra scraping real.

---

## Decisión

**Opción 3: MediaMarkt ES con JSON-LD.**

El criterio principal fue maximizar el valor del portfolio: demostrar que funciona algo real (scraping de verdad, contra un sitio real, con datos reales) y documentar honestamente el obstáculo de Cloudflare con sus implicaciones técnicas.

---

## Técnica de scraping elegida para MediaMarkt

MediaMarkt usa React (SPA), por lo que el HTML estándar no contiene los datos. Sin embargo, la página incluye un `<script type="application/ld+json">` con el schema `ItemList` de Schema.org:

```json
{
  "@context": "https://schema.org",
  "@type": "ItemList",
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "item": {
        "@type": "Product",
        "name": "ASUS TUF Gaming RTX 4070 SUPER",
        "offers": { "price": 619.00, "priceCurrency": "EUR" },
        "url": "https://www.mediamarkt.es/es/product/...",
        "image": "https://assets.mmsrg.com/..."
      }
    }
  ]
}
```

Jsoup extrae el tag `script[type=application/ld+json]`, Jackson parsea el JSON, y se navega la estructura `ListItem → item`.

**Ventaja sobre CSS selectors**: El JSON-LD es semántico y estable. Los class names generados por styled-components cambian en cada deploy. El JSON-LD raramente cambia porque es para SEO.

---

## Consecuencias

### Positivas
- Dos scrapers funcionales y reales en el portfolio
- Demuestra capacidad de resolver problemas imprevistos en web scraping
- MediaMarkt + JSON-LD es una técnica más robusta que CSS selectors para SPAs
- La documentación del Cloudflare blocking es valiosa como learning

### Negativas
- PCComponentes no está soportado. Si se quisiera añadir en el futuro, habría que evaluar Playwright o un scraping proxy

---

## Lecciones aprendidas

1. **Las APIs internas no son públicas**: Solo porque el browser puede acceder a `/api/articles/search` no significa que un cliente HTTP lo pueda. Cloudflare protege tanto el HTML como las XHR requests.

2. **JSON-LD como fuente de datos**: Los sitios modernos que usan SPAs a menudo incluyen JSON-LD para SEO. Es una técnica legítima y estable de extracción de datos.

3. **Documentar los obstáculos**: En un portfolio, explicar por qué algo no funciona (y mostrar que entiendes el problema) es tan valioso como mostrarlo funcionando.

---

## Referencias

- [Cloudflare Bot Management](https://www.cloudflare.com/products/bot-management/)
- [Schema.org ItemList](https://schema.org/ItemList)
- [JSON-LD Specification](https://json-ld.org/)
- Ver Learning 001 para técnicas de Jsoup
- `MediaMarktScraper.java`, `README.md`

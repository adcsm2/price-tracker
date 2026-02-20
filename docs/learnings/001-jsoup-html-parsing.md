# Learning 001: Jsoup HTML Parsing

**Fase:** 2 (Amazon Scraper)
**Fecha:** 2026-02
**Tecnología:** Jsoup 1.17.2

---

## ¿Qué es?

Jsoup es una librería Java para parsear, extraer y manipular HTML. Proporciona una API similar a jQuery para navegar el DOM con selectores CSS.

---

## ¿Por qué lo usamos?

Necesitábamos extraer datos estructurados (nombre, precio, imagen, URL) desde páginas HTML de Amazon. Jsoup permite hacer HTTP requests y parsear el HTML resultante en una sola llamada, con selectores CSS intuitivos.

---

## Cómo funciona en el proyecto

### 1. Fetch + parse en una llamada

```java
Document doc = Jsoup.connect(searchUrl)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)...")
        .timeout(10_000)
        .header("Accept-Language", "es-ES,es;q=0.9")
        .header("Accept", "text/html,application/xhtml+xml")
        .get();
```

### 2. Selección de elementos con CSS selectors

```java
// Seleccionar todos los resultados de búsqueda de Amazon
Elements items = doc.select("[data-component-type=s-search-result]");

// Extraer el nombre del producto
Element titleElement = item.selectFirst("h2 a span");
String name = titleElement.text().trim();

// Extraer imagen
Element img = item.selectFirst(".s-image");
String imageUrl = img.attr("src");
```

### 3. Construcción de URLs absolutas

```java
Element link = item.selectFirst("h2 a");
String href = link.attr("href");
if (href.startsWith("/")) {
    return BASE_URL + href;  // Relativa → absoluta
}
return href;
```

---

## Técnica clave: HTML fixtures en tests

Para no hacer HTTP requests reales en los tests, guardamos HTML estático como fixture:

```java
// AmazonScraperTest
Document doc = Jsoup.parse(
    getClass().getResourceAsStream("/fixtures/amazon_search.html"),
    "UTF-8",
    "https://www.amazon.es"
);
List<ScrapedProductDTO> products = scraper.parseSearchResults(doc);
```

Ventajas:
- Tests deterministas (no dependen de que Amazon devuelva lo mismo)
- Tests rápidos (sin red)
- Tests que no consumen rate limit real

---

## Limitación importante encontrada

Jsoup **no ejecuta JavaScript**. Varios sitios usan React/Next.js y cargan el contenido de forma dinámica, por lo que el HTML que devuelve Jsoup está vacío de datos.

**Soluciones según el caso:**
- Buscar endpoints JSON internos de la web (API calls del browser)
- Buscar JSON-LD embebido en el HTML (schema.org)
- Usar Selenium/Playwright para renderizado JS (mucho más complejo)

Ver ADR-003 para cómo se resolvió en MediaMarkt.

---

## Selectores CSS más usados

| Selector | Ejemplo | Uso |
|----------|---------|-----|
| `[attr=value]` | `[data-component-type=s-search-result]` | Atributo exacto |
| `.className` | `.a-price-whole` | Clase CSS |
| `parent child` | `h2 a span` | Descendiente |
| `element` | `img` | Por tag |
| `:first-child` | `h2:first-child` | Posición |

---

## Alternativas consideradas

| Opción | Pros | Contras |
|--------|------|---------|
| **Jsoup** ✅ | Simple, sin deps extra, rápido | No ejecuta JS |
| HtmlUnit | Ejecuta algo de JS | Lento, inestable |
| Selenium | JS completo | ChromeDriver, lento, complejo |
| Playwright | JS completo, más moderno | Deps pesadas |

---

## Referencias

- [Jsoup Cookbook](https://jsoup.org/cookbook/)
- [CSS Selector Reference](https://jsoup.org/apidocs/org/jsoup/select/Selector.html)
- `AmazonScraper.java`, `MediaMarktScraper.java`

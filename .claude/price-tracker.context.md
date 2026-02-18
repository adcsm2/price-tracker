# Multi-Site Price Tracker - Contexto del Proyecto

## 🎯 Propósito

Construir un web scraper multi-sitio para rastrear precios de productos tech (GPUs, laptops, etc.) en múltiples tiendas online (Amazon, PCComponentes).

**Parte de**: Portfolio de proyectos para demostrar competencias técnicas a recruiters.

**Objetivos de aprendizaje:**
- Web scraping con Jsoup
- HTTP requests y rate limiting
- Retry logic y error handling
- CRUD avanzado con relaciones
- Scheduled jobs
- Strategy pattern (scraper por sitio)
- Spring Boot REST API

---

## 🛠️ Stack técnico

### Core
- **Java 17**
- **Spring Boot 3.2+**
- **Spring Data JPA** (Hibernate)
- **PostgreSQL 15**
- **Maven** (build tool)

### Web Scraping
- **Jsoup** (HTML parsing)
- **RestTemplate** (HTTP requests)
- **Guava RateLimiter** (rate limiting)

### Testing
- **JUnit 5**
- **Mockito**
- **Spring Boot Test**
- **TestContainers** (tests con PostgreSQL)

### Utilities
- **Lombok** (reduce boilerplate)
- **MapStruct** (DTO mapping)

### Infraestructura
- **Docker** (PostgreSQL en desarrollo)
- **Docker Compose**
- **GitHub Actions** (CI)

---

## 📁 Estructura del proyecto

```
price-tracker/
├── src/main/java/com/portfolio/pricetracker/
│   ├── entity/
│   │   ├── Product.java
│   │   ├── ProductListing.java
│   │   ├── WebsiteSource.java
│   │   ├── PriceHistory.java
│   │   ├── ScrapingJob.java
│   │   └── PriceAlert.java
│   ├── repository/
│   │   ├── ProductRepository.java
│   │   ├── ProductListingRepository.java
│   │   ├── WebsiteSourceRepository.java
│   │   ├── PriceHistoryRepository.java
│   │   └── ScrapingJobRepository.java
│   ├── service/
│   │   ├── scraper/
│   │   │   ├── SiteScraper.java (interface)
│   │   │   ├── AmazonScraper.java
│   │   │   ├── PCComponentesScraper.java
│   │   │   └── ScraperFactory.java
│   │   ├── ProductService.java
│   │   ├── ScrapingJobService.java
│   │   └── PriceAlertService.java
│   ├── controller/
│   │   ├── ProductController.java
│   │   ├── ScrapingJobController.java
│   │   └── AnalyticsController.java
│   ├── dto/
│   │   ├── ProductDTO.java
│   │   ├── ScrapedProductDTO.java
│   │   └── PriceComparisonDTO.java
│   ├── config/
│   │   ├── ScraperConfig.java
│   │   └── SchedulingConfig.java
│   └── PriceTrackerApplication.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/ (Flyway migrations)
├── src/test/java/
├── docker-compose.yml
├── Dockerfile
├── .github/workflows/
│   └── ci.yml
├── .claude/
│   ├── context.md
│   └── CURRENT_STATUS.md
├── docs/
│   └── architecture/
│       ├── 001-scraper-strategy-pattern.md
│       ├── 002-rate-limiting.md
│       └── 003-product-unification.md
├── pom.xml
└── README.md
```

---

## 💾 Modelo de datos

### Entities y relaciones:

```java
// WebsiteSource (Amazon, PCComponentes, etc.)
@Entity
public class WebsiteSource {
  @Id @GeneratedValue
  private Long id;
  
  private String name;              // "Amazon ES"
  private String baseUrl;           // "https://www.amazon.es"
  
  @Enumerated(EnumType.STRING)
  private ScraperType scraperType;  // AMAZON, PCCOMPONENTES
  
  @Enumerated(EnumType.STRING)
  private SourceStatus status;      // ACTIVE, DISABLED, ERROR
  
  private LocalDateTime lastScrapedAt;
  private Integer successfulScrapes;
  private Integer failedScrapes;
  
  @CreationTimestamp
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  private LocalDateTime updatedAt;
}

// Product (producto unificado entre sitios)
@Entity
public class Product {
  @Id @GeneratedValue
  private Long id;
  
  @Column(nullable = false)
  private String name;              // "NVIDIA RTX 4070 SUPER"
  
  private String category;          // "gpu", "laptop", "monitor"
  private String imageUrl;
  
  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
  private List<ProductListing> listings;
  
  @OneToMany(mappedBy = "product")
  private List<PriceHistory> priceHistory;
  
  @CreationTimestamp
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  private LocalDateTime updatedAt;
  
  @Column(nullable = true)
  private LocalDateTime deletedAt; // Soft delete
}

// ProductListing (producto en un sitio específico)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
  columnNames = {"product_id", "source_id"}
))
public class ProductListing {
  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id", nullable = false)
  private WebsiteSource source;
  
  private String externalId;        // ID en el sitio externo
  
  @Column(nullable = false)
  private String url;
  
  private BigDecimal currentPrice;
  private String currency;          // "EUR"
  private Boolean inStock;
  
  private LocalDateTime lastScrapedAt;
  
  @OneToMany(mappedBy = "listing")
  private List<PriceHistory> priceHistory;
  
  @CreationTimestamp
  private LocalDateTime createdAt;
  
  @UpdateTimestamp
  private LocalDateTime updatedAt;
}

// PriceHistory (historial de precios)
@Entity
@Table(indexes = {
  @Index(name = "idx_listing_scraped", columnList = "listing_id,scraped_at")
})
public class PriceHistory {
  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", nullable = false)
  private ProductListing listing;
  
  @Column(nullable = false)
  private BigDecimal price;
  
  private Boolean inStock;
  
  @Column(nullable = false)
  private LocalDateTime scrapedAt;
}

// ScrapingJob (trabajo de scraping)
@Entity
public class ScrapingJob {
  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id")
  private WebsiteSource source;
  
  private String searchKeyword;     // "rtx 4070"
  private String category;          // "gpu"
  
  @Enumerated(EnumType.STRING)
  private JobStatus status;         // PENDING, RUNNING, COMPLETED, FAILED
  
  private Integer itemsFound;
  
  @Lob
  private String errorMessage;
  
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  
  @CreationTimestamp
  private LocalDateTime createdAt;
}

// PriceAlert (alertas de precio)
@Entity
public class PriceAlert {
  @Id @GeneratedValue
  private Long id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  private Product product;
  
  private String userEmail;
  private BigDecimal targetPrice;
  
  @Enumerated(EnumType.STRING)
  private AlertStatus status;       // ACTIVE, TRIGGERED, DISABLED
  
  private LocalDateTime triggeredAt;
  
  @CreationTimestamp
  private LocalDateTime createdAt;
}
```

---

## 🚀 API Endpoints

### 1. Website Sources
```
GET    /api/sources              # Listar sitios
POST   /api/sources              # Añadir sitio
GET    /api/sources/{id}         # Ver sitio
PUT    /api/sources/{id}         # Actualizar
DELETE /api/sources/{id}         # Eliminar
GET    /api/sources/{id}/stats   # Estadísticas
```

### 2. Scraping Jobs
```
POST   /api/scraping/jobs           # Crear job
GET    /api/scraping/jobs           # Listar jobs
GET    /api/scraping/jobs/{id}      # Ver job
POST   /api/scraping/jobs/{id}/run  # Ejecutar
DELETE /api/scraping/jobs/{id}      # Cancelar
```

### 3. Products (CRUD + Filtros)
```
GET    /api/products                       # Listar
GET    /api/products?category=gpu          # Filtrar categoría
GET    /api/products?minPrice=500          # Filtrar precio
GET    /api/products?keyword=rtx           # Buscar
GET    /api/products/{id}                  # Ver producto
GET    /api/products/{id}/listings         # Ver en qué sitios está
GET    /api/products/{id}/best-price       # Mejor precio
GET    /api/products/{id}/price-history    # Historial
PUT    /api/products/{id}                  # Actualizar
DELETE /api/products/{id}                  # Soft delete
```

### 4. Analytics
```
GET    /api/analytics/price-drops          # Mayores bajadas
GET    /api/analytics/price-increases      # Mayores subidas
GET    /api/analytics/trending             # Más scrapeados
GET    /api/analytics/cheapest-site        # Sitio más barato
GET    /api/compare?productId=1            # Comparar precios
```

### 5. Price Alerts (Opcional - Fase 7)
```
POST   /api/alerts        # Crear alerta
GET    /api/alerts        # Mis alertas
DELETE /api/alerts/{id}   # Eliminar
```

---

## 🔧 Estrategia de Web Scraping

### Strategy Pattern:

```java
public interface SiteScraper {
  String getSiteName();
  ScraperType getScraperType();
  List<ScrapedProductDTO> scrape(String keyword, String category);
  ProductDetailsDTO scrapeDetails(String url);
}

@Service
public class AmazonScraper implements SiteScraper {
  
  private final RateLimiter rateLimiter = RateLimiter.create(2.0); // 2 req/sec
  
  @Override
  public List<ScrapedProductDTO> scrape(String keyword, String category) {
    rateLimiter.acquire();
    
    String url = buildSearchUrl(keyword, category);
    Document doc = Jsoup.connect(url)
      .userAgent("Mozilla/5.0...")
      .timeout(10000)
      .get();
    
    Elements items = doc.select(".s-result-item");
    
    return items.stream()
      .map(this::parseProduct)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }
  
  private ScrapedProductDTO parseProduct(Element item) {
    // Extraer nombre, precio, URL, imagen
    // Selectores específicos de Amazon
  }
}

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
      throw new UnsupportedScraperException(type);
    }
    return scraper;
  }
}
```

### Rate Limiting:
- **Guava RateLimiter**: 2 requests/segundo por sitio
- Evita IP bans
- Configurable por sitio en `application.yml`

### Retry Logic:
```java
@Retryable(
  value = {IOException.class, HttpStatusException.class},
  maxAttempts = 3,
  backoff = @Backoff(delay = 1000, multiplier = 2)
)
public Document fetchPage(String url) throws IOException {
  return Jsoup.connect(url).get();
}
```

### Error Handling:
- Timeout: 10 segundos
- User-Agent rotation
- Logging de errores
- Graceful degradation

---

## 🧪 Estrategia de testing

### Tests unitarios:
```java
@Test
void testAmazonScraper_ParsesProductCorrectly() {
  String html = loadHtmlFixture("amazon_product.html");
  Document doc = Jsoup.parse(html);
  
  ScrapedProductDTO product = amazonScraper.parseProduct(doc);
  
  assertThat(product.getName()).contains("RTX 4070");
  assertThat(product.getPrice()).isGreaterThan(BigDecimal.ZERO);
}
```

### Tests de integración:
```java
@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {
  
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
  
  @Test
  void testCreateProduct_SavesSuccessfully() {
    ProductDTO dto = new ProductDTO("RTX 4070", "gpu");
    Product saved = productService.create(dto);
    
    assertThat(saved.getId()).isNotNull();
  }
}
```

### Cobertura objetivo:
- **>80%** en services y scrapers
- **>70%** en controllers
- Edge cases cubiertos

---

## 🔍 Decisiones arquitectónicas a documentar

### 1. Strategy Pattern para scrapers
**Problema:** Cada sitio tiene HTML diferente
**Decisión:** Interface SiteScraper + implementación por sitio
**Trade-off:** Más clases vs extensibilidad

### 2. Product Unification
**Problema:** Mismo producto en varios sitios
**Decisión:** Entity Product + ProductListing (relación 1-N)
**Trade-off:** Complejidad vs normalización

### 3. Rate Limiting
**Problema:** Evitar IP bans
**Decisión:** Guava RateLimiter (2 req/sec)
**Trade-off:** Velocidad vs seguridad

### 4. Jsoup vs Selenium
**Problema:** Cómo parsear HTML
**Decisión:** Jsoup (ligero, no JS rendering)
**Trade-off:** Simplicidad vs sitios con JS

### 5. Scheduled Jobs
**Problema:** Scraping automático diario
**Decisión:** Spring @Scheduled (cron)
**Trade-off:** Built-in vs Quartz (más potente)

---

## 📚 Convenciones del proyecto

### Java/Spring Boot
- Java 17 features (records, switch expressions)
- Lombok para reducir boilerplate
- MapStruct para DTO mapping
- Constructor injection (no @Autowired en campos)

### Git Workflow
- Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`
- Branch naming: `feat/feature-name`, `fix/bug-name`
- PRs descriptivos (sin fake reviews)

### Testing
- AAA pattern (Arrange, Act, Assert)
- Test names: `should_ExpectedBehavior_When_StateUnderTest`
- Fixtures en `src/test/resources/fixtures/`

### Docker
- **Desarrollo:** PostgreSQL en Docker, app local
- **CI/CD:** Todo en Docker
- docker-compose.yml para desarrollo
- Dockerfile multi-stage para producción

---

## 🪟 Comandos útiles

```bash
# Desarrollo
./mvnw spring-boot:run              # Correr app
./mvnw test                          # Tests
./mvnw clean install                 # Build completo

# Docker
docker-compose up -d                 # PostgreSQL
docker-compose logs -f postgres      # Ver logs
docker-compose down                  # Detener

# Database
docker exec -it price-tracker-db psql -U pricetracker -d pricetracker

# Testing
./mvnw test                          # Todos los tests
./mvnw test -Dtest=ProductServiceTest  # Test específico
./mvnw verify                        # Tests + integration tests
```

---

## ⚠️ Consideraciones legales/éticas

### Web Scraping:
- ✅ Respetar `robots.txt`
- ✅ Rate limiting agresivo (2 req/sec)
- ✅ User-Agent honesto
- ✅ Solo para fines educativos
- ✅ Disclaimer en README

### README Disclaimer:
```
⚠️ **Educational Purpose Only**

This project is built for learning purposes to demonstrate web scraping,
Spring Boot, and backend development skills. It should NOT be used for
commercial purposes without proper authorization from the scraped websites.

Always respect robots.txt and implement appropriate rate limiting.
```

---

## 🎓 Objetivos de aprendizaje

Este proyecto demuestra a recruiters:

1. **Web Scraping**: Jsoup, HTTP requests, HTML parsing
2. **Spring Boot**: REST API, JPA, scheduled jobs
3. **Design Patterns**: Strategy pattern, Factory
4. **Rate Limiting**: Guava RateLimiter, evitar bans
5. **Retry Logic**: Resilience4j, exponential backoff
6. **CRUD avanzado**: Relaciones 1-N, filtros, soft deletes
7. **Testing**: JUnit, Mockito, TestContainers
8. **Docker**: Containerización, docker-compose
9. **CI/CD**: GitHub Actions
10. **Git Workflow**: Feature branches, PRs, conventional commits

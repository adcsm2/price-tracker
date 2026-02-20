# Learning 005: TestContainers

**Fase:** 3 (Product CRUD)
**Fecha:** 2026-02
**Tecnología:** TestContainers 1.19+, JUnit 5

---

## ¿Qué es?

TestContainers es una librería Java que levanta contenedores Docker reales durante los tests de integración. Permite testear contra una base de datos PostgreSQL real en lugar de usar H2 en memoria.

---

## ¿Por qué lo usamos?

Los tests con H2 (base de datos en memoria) tienen un problema fundamental: H2 no es PostgreSQL. Las diferencias incluyen:

- Tipos de datos distintos (ej: `TEXT` vs `CLOB`)
- Dialecto SQL distinto
- Comportamiento diferente en constraints, índices, etc.

Con TestContainers, los tests de integración usan **exactamente el mismo motor** que producción.

---

## Implementación en el proyecto

### Dependencia en pom.xml

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

### Setup del test

```java
@SpringBootTest
@Testcontainers
@Transactional
class ProductServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pricetracker_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductService productService;
}
```

### Por qué `static`

El container es `static` para que se levante una sola vez para toda la clase, no una vez por test. Levantar un contenedor Docker tarda ~3-5 segundos, hacerlo por cada test sería prohibitivo.

### `@Transactional` en los tests

Con `@Transactional` en la clase de test, cada test se ejecuta dentro de una transacción que se hace rollback al final. Esto garantiza que los tests son independientes y no se contaminan entre sí.

---

## Ejemplo de test de integración

```java
@Test
void should_CreateProduct_Successfully() {
    ProductDTO dto = new ProductDTO();
    dto.setName("NVIDIA RTX 4070 SUPER");
    dto.setCategory("gpu");

    ProductDTO saved = productService.create(dto);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getName()).isEqualTo("NVIDIA RTX 4070 SUPER");
}

@Test
void should_SoftDelete_And_NotReturnInFindAll() {
    // Crear producto
    ProductDTO dto = new ProductDTO();
    dto.setName("GTX 1080");
    ProductDTO saved = productService.create(dto);

    // Eliminar (soft delete)
    productService.delete(saved.getId());

    // No debe aparecer en la lista
    List<ProductDTO> active = productService.findAll(null, null, null, null);
    assertThat(active).noneMatch(p -> p.getId().equals(saved.getId()));
}
```

---

## Flyway con TestContainers

Flyway ejecuta automáticamente las migraciones al arrancar Spring en el contexto de test. Esto garantiza que el schema de test siempre está actualizado con el mismo script que producción.

---

## Comparación con alternativas

| Opción | Pros | Contras |
|--------|------|---------|
| **TestContainers** ✅ | Base de datos real, fiel a producción | Requiere Docker, más lento que H2 |
| H2 in-memory | Rapidísimo, sin Docker | Dialecto diferente, puede ocultar bugs |
| BD de test dedicada | Datos persistentes | Contaminación entre tests, gestión manual |
| Mockito mock de repository | Ultra rápido | No testa la integración real con BD |

---

## Tiempo de ejecución

En este proyecto los tests de integración tardan ~5-8 segundos (mayormente levantar el contenedor la primera vez). Los tests unitarios tardan <0.5s.

La estrategia es:
- **Tests unitarios** (scrapers, parsers): sin Spring, sin BD, rapidísimos
- **Tests de integración** (ProductService): TestContainers, prueban la capa completa

---

## Referencias

- [TestContainers Documentation](https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/)
- [Spring Boot + TestContainers](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing.testcontainers)
- `ProductServiceIntegrationTest.java`

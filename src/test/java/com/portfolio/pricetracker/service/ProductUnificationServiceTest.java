package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.ScrapedProductDTO;
import com.portfolio.pricetracker.entity.*;
import com.portfolio.pricetracker.repository.PriceHistoryRepository;
import com.portfolio.pricetracker.repository.ProductListingRepository;
import com.portfolio.pricetracker.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductUnificationServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductListingRepository listingRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private PriceAlertService priceAlertService;

    @InjectMocks
    private ProductUnificationService service;

    private WebsiteSource amazonSource;

    @BeforeEach
    void setUp() {
        amazonSource = WebsiteSource.builder()
                .id(1L).name("Amazon ES").scraperType(ScraperType.AMAZON).build();

        // Make TransactionTemplate execute the consumer immediately (no real transaction needed).
        // lenient() avoids UnnecessaryStubbingException in tests that skip all items.
        lenient().doAnswer(inv -> {
            ((Consumer<?>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void should_CreateNewProductAndListing_When_UrlAndNameAreNew() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("ASUS RTX 4070")
                .price(new BigDecimal("599"))
                .url("https://www.amazon.es/dp/B001")
                .inStock(true)
                .build();

        Product savedProduct = Product.builder().id(10L).name("ASUS RTX 4070").build();

        when(listingRepository.findByUrl(any())).thenReturn(Optional.empty());
        when(productRepository.findByNameIgnoreCaseAndDeletedAtIsNull("ASUS RTX 4070"))
                .thenReturn(Optional.empty());
        when(productRepository.save(any())).thenReturn(savedProduct);
        when(listingRepository.findByProductIdAndSourceId(10L, 1L)).thenReturn(Optional.empty());
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(scraped), amazonSource);

        verify(productRepository).save(any(Product.class));
        verify(listingRepository).save(any(ProductListing.class));
        verify(priceHistoryRepository).save(any(PriceHistory.class));
    }

    @Test
    void should_UpdateExistingListing_When_UrlAlreadyExists() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("ASUS RTX 4070")
                .price(new BigDecimal("549"))
                .url("https://www.amazon.es/dp/B001")
                .inStock(true)
                .build();

        Product product = Product.builder().id(10L).name("ASUS RTX 4070").build();
        ProductListing existingListing = ProductListing.builder()
                .id(5L).product(product).source(amazonSource)
                .url("https://www.amazon.es/dp/B001")
                .currentPrice(new BigDecimal("599"))
                .build();

        when(listingRepository.findByUrl("https://www.amazon.es/dp/B001"))
                .thenReturn(Optional.of(existingListing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(scraped), amazonSource);

        ArgumentCaptor<ProductListing> listingCaptor = ArgumentCaptor.forClass(ProductListing.class);
        verify(listingRepository).save(listingCaptor.capture());
        assertThat(listingCaptor.getValue().getCurrentPrice()).isEqualByComparingTo("549");
        verify(productRepository, never()).save(any());
    }

    @Test
    void should_ReuseExistingProduct_When_NameMatchesAcrossSites() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("ASUS RTX 4070")
                .price(new BigDecimal("589"))
                .url("https://www.mediamarkt.es/product/asus-rtx4070")
                .inStock(true)
                .build();

        Product existingProduct = Product.builder().id(10L).name("ASUS RTX 4070").build();

        when(listingRepository.findByUrl(any())).thenReturn(Optional.empty());
        when(productRepository.findByNameIgnoreCaseAndDeletedAtIsNull("ASUS RTX 4070"))
                .thenReturn(Optional.of(existingProduct));
        when(listingRepository.findByProductIdAndSourceId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(scraped), amazonSource);

        verify(productRepository, never()).save(any());
        verify(listingRepository).save(any(ProductListing.class));
    }

    @Test
    void should_SkipItem_When_UrlIsNull() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("No URL product").price(new BigDecimal("100")).url(null).build();

        service.saveResults(List.of(scraped), amazonSource);

        verifyNoInteractions(listingRepository, productRepository, priceHistoryRepository, priceAlertService);
    }

    @Test
    void should_SkipItem_When_PriceIsNull() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("No price product").price(null).url("https://example.com").build();

        service.saveResults(List.of(scraped), amazonSource);

        verifyNoInteractions(listingRepository, productRepository, priceHistoryRepository, priceAlertService);
    }

    @Test
    void should_CallCheckAlerts_When_ItemSavedSuccessfully() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("RTX 4070").price(new BigDecimal("599"))
                .url("https://www.amazon.es/dp/B001").inStock(true).build();

        Product product = Product.builder().id(1L).name("RTX 4070").build();
        ProductListing listing = ProductListing.builder()
                .id(1L).product(product).source(amazonSource)
                .url("https://www.amazon.es/dp/B001").build();

        when(listingRepository.findByUrl(any())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(scraped), amazonSource);

        verify(priceAlertService).checkAlerts(1L, new BigDecimal("599"));
    }

    @Test
    void should_ContinueSaving_When_OneItemThrowsException() {
        ScrapedProductDTO failing = ScrapedProductDTO.builder()
                .name("Failing").price(new BigDecimal("100"))
                .url("https://bad.url").inStock(true).build();
        ScrapedProductDTO valid = ScrapedProductDTO.builder()
                .name("Valid").price(new BigDecimal("200"))
                .url("https://good.url").inStock(true).build();

        Product product = Product.builder().id(1L).name("Valid").build();
        ProductListing listing = ProductListing.builder()
                .id(1L).product(product).source(amazonSource)
                .url("https://good.url").build();

        // First call (failing item) throws; second call (valid item) executes normally
        doThrow(new RuntimeException("DB error"))
                .doAnswer(inv -> {
                    ((Consumer<?>) inv.getArgument(0)).accept(null);
                    return null;
                }).when(transactionTemplate).executeWithoutResult(any());

        when(listingRepository.findByUrl("https://good.url")).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(failing, valid), amazonSource);

        verify(listingRepository, times(1)).save(any());
        verify(priceHistoryRepository, times(1)).save(any());
    }

    @Test
    void should_UpdateListingUrl_When_SameProductSourceExistsWithDifferentUrl() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("ASUS RTX 4070").price(new BigDecimal("599"))
                .url("https://www.amazon.es/dp/B002") // new URL
                .inStock(true).build();

        Product existingProduct = Product.builder().id(10L).name("ASUS RTX 4070").build();
        ProductListing existingListing = ProductListing.builder()
                .id(5L).product(existingProduct).source(amazonSource)
                .url("https://www.amazon.es/dp/B001") // old URL
                .build();

        when(listingRepository.findByUrl("https://www.amazon.es/dp/B002")).thenReturn(Optional.empty());
        when(productRepository.findByNameIgnoreCaseAndDeletedAtIsNull("ASUS RTX 4070"))
                .thenReturn(Optional.of(existingProduct));
        when(listingRepository.findByProductIdAndSourceId(10L, 1L)).thenReturn(Optional.of(existingListing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(scraped), amazonSource);

        ArgumentCaptor<ProductListing> captor = ArgumentCaptor.forClass(ProductListing.class);
        verify(listingRepository).save(captor.capture());
        assertThat(captor.getValue().getUrl()).isEqualTo("https://www.amazon.es/dp/B002");
        verify(productRepository, never()).save(any());
    }

    @Test
    void should_SavePriceHistory_OnEverySuccessfulScrape() {
        ScrapedProductDTO scraped = ScrapedProductDTO.builder()
                .name("RTX 4070").price(new BigDecimal("599"))
                .url("https://www.amazon.es/dp/B001").inStock(true).build();

        Product product = Product.builder().id(1L).name("RTX 4070").build();
        ProductListing listing = ProductListing.builder()
                .id(1L).product(product).source(amazonSource)
                .url("https://www.amazon.es/dp/B001").build();

        when(listingRepository.findByUrl(any())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveResults(List.of(scraped), amazonSource);

        ArgumentCaptor<PriceHistory> historyCaptor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getPrice()).isEqualByComparingTo("599");
        assertThat(historyCaptor.getValue().getScrapedAt()).isNotNull();
    }
}

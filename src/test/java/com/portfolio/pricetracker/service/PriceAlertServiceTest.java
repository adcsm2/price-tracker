package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.CreateAlertRequest;
import com.portfolio.pricetracker.dto.PriceAlertDTO;
import com.portfolio.pricetracker.entity.AlertStatus;
import com.portfolio.pricetracker.entity.PriceAlert;
import com.portfolio.pricetracker.entity.Product;
import com.portfolio.pricetracker.repository.PriceAlertRepository;
import com.portfolio.pricetracker.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertServiceTest {

    @Mock private PriceAlertRepository alertRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AlertNotifier alertNotifier;

    @InjectMocks
    private PriceAlertService service;

    private Product product;
    private PriceAlert activeAlert;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("RTX 4070").build();

        activeAlert = PriceAlert.builder()
                .id(10L)
                .product(product)
                .userEmail("user@test.com")
                .targetPrice(new BigDecimal("500.00"))
                .status(AlertStatus.ACTIVE)
                .build();
    }

    @Test
    void should_CreateAlert_Successfully() {
        CreateAlertRequest request = new CreateAlertRequest("user@test.com", 1L, new BigDecimal("500.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(alertRepository.save(any())).thenAnswer(inv -> {
            PriceAlert a = inv.getArgument(0);
            a.setId(10L);
            return a;
        });

        PriceAlertDTO result = service.createAlert(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUserEmail()).isEqualTo("user@test.com");
        assertThat(result.getTargetPrice()).isEqualByComparingTo("500.00");
        assertThat(result.getStatus()).isEqualTo(AlertStatus.ACTIVE);
    }

    @Test
    void should_ThrowException_When_ProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAlert(new CreateAlertRequest("u@t.com", 99L, BigDecimal.TEN)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_TriggerAlert_When_PriceDropsBelowTarget() {
        when(alertRepository.findByProduct_IdAndStatus(1L, AlertStatus.ACTIVE))
                .thenReturn(List.of(activeAlert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAlerts(1L, new BigDecimal("450.00"));

        ArgumentCaptor<PriceAlert> captor = ArgumentCaptor.forClass(PriceAlert.class);
        verify(alertRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AlertStatus.TRIGGERED);
        assertThat(captor.getValue().getTriggeredAt()).isNotNull();
        verify(alertNotifier).notify(activeAlert, new BigDecimal("450.00"));
    }

    @Test
    void should_TriggerAlert_When_PriceEqualsTarget() {
        when(alertRepository.findByProduct_IdAndStatus(1L, AlertStatus.ACTIVE))
                .thenReturn(List.of(activeAlert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkAlerts(1L, new BigDecimal("500.00"));

        verify(alertNotifier).notify(any(), any());
    }

    @Test
    void should_NotTriggerAlert_When_PriceAboveTarget() {
        when(alertRepository.findByProduct_IdAndStatus(1L, AlertStatus.ACTIVE))
                .thenReturn(List.of(activeAlert));

        service.checkAlerts(1L, new BigDecimal("600.00"));

        verify(alertRepository, never()).save(any());
        verify(alertNotifier, never()).notify(any(), any());
    }

    @Test
    void should_ReturnUserAlerts() {
        when(alertRepository.findByUserEmailAndStatus("user@test.com", AlertStatus.ACTIVE))
                .thenReturn(List.of(activeAlert));

        List<PriceAlertDTO> result = service.getUserAlerts("user@test.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserEmail()).isEqualTo("user@test.com");
        assertThat(result.get(0).getProductName()).isEqualTo("RTX 4070");
    }

    @Test
    void should_DeleteAlert_Successfully() {
        when(alertRepository.existsById(10L)).thenReturn(true);

        service.deleteAlert(10L);

        verify(alertRepository).deleteById(10L);
    }

    @Test
    void should_ThrowException_When_DeletingNonExistentAlert() {
        when(alertRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAlert(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}

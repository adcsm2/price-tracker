package com.portfolio.pricetracker.service;

import com.portfolio.pricetracker.dto.CreateAlertRequest;
import com.portfolio.pricetracker.dto.PriceAlertDTO;
import com.portfolio.pricetracker.entity.AlertStatus;
import com.portfolio.pricetracker.entity.PriceAlert;
import com.portfolio.pricetracker.entity.Product;
import com.portfolio.pricetracker.repository.PriceAlertRepository;
import com.portfolio.pricetracker.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceAlertService {

    private final PriceAlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final AlertNotifier alertNotifier;

    @Transactional
    public PriceAlertDTO createAlert(CreateAlertRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.getProductId()));

        PriceAlert alert = PriceAlert.builder()
                .product(product)
                .userEmail(request.getUserEmail())
                .targetPrice(request.getTargetPrice())
                .build();

        return toDTO(alertRepository.save(alert));
    }

    public List<PriceAlertDTO> getUserAlerts(String userEmail) {
        return alertRepository.findByUserEmailAndStatus(userEmail, AlertStatus.ACTIVE)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAlert(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new EntityNotFoundException("Alert not found: " + id);
        }
        alertRepository.deleteById(id);
    }

    /**
     * Called after each scrape for a product.
     * Triggers any ACTIVE alerts where currentPrice <= targetPrice.
     */
    @Transactional
    public void checkAlerts(Long productId, BigDecimal currentPrice) {
        List<PriceAlert> activeAlerts = alertRepository.findByProduct_IdAndStatus(productId, AlertStatus.ACTIVE);
        for (PriceAlert alert : activeAlerts) {
            if (currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                triggerAlert(alert, currentPrice);
            }
        }
    }

    private void triggerAlert(PriceAlert alert, BigDecimal currentPrice) {
        alert.setStatus(AlertStatus.TRIGGERED);
        alert.setTriggeredAt(LocalDateTime.now());
        alertRepository.save(alert);
        alertNotifier.notify(alert, currentPrice);
        log.info("Alert {} triggered for product '{}' at {}€",
                alert.getId(), alert.getProduct().getName(), currentPrice);
    }

    private PriceAlertDTO toDTO(PriceAlert alert) {
        return PriceAlertDTO.builder()
                .id(alert.getId())
                .userEmail(alert.getUserEmail())
                .productId(alert.getProduct().getId())
                .productName(alert.getProduct().getName())
                .targetPrice(alert.getTargetPrice())
                .status(alert.getStatus())
                .triggeredAt(alert.getTriggeredAt())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}

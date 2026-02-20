package com.portfolio.pricetracker.repository;

import com.portfolio.pricetracker.entity.AlertStatus;
import com.portfolio.pricetracker.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    List<PriceAlert> findByUserEmailAndStatus(String userEmail, AlertStatus status);

    List<PriceAlert> findByProduct_IdAndStatus(Long productId, AlertStatus status);
}

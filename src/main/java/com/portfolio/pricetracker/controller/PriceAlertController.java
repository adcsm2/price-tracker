package com.portfolio.pricetracker.controller;

import com.portfolio.pricetracker.dto.CreateAlertRequest;
import com.portfolio.pricetracker.dto.PriceAlertDTO;
import com.portfolio.pricetracker.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PriceAlertDTO createAlert(@RequestBody CreateAlertRequest request) {
        return priceAlertService.createAlert(request);
    }

    @GetMapping
    public List<PriceAlertDTO> getUserAlerts(@RequestParam String email) {
        return priceAlertService.getUserAlerts(email);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(@PathVariable Long id) {
        priceAlertService.deleteAlert(id);
    }
}

package com.tendanz.pricing.controller;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.service.PricingService;
import com.tendanz.pricing.repository.QuoteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/quotes")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final PricingService pricingService;
    private final QuoteRepository quoteRepository;
    private final ObjectMapper objectMapper;

    /**
     * create new quote
     */
    @PostMapping
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody QuoteRequest request) {

        log.info("Creating quote for client: {}", request.getClientName());

        QuoteResponse response = pricingService.calculateQuote(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * get one quote by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable Long id) {
        log.info("Fetching quote with ID: {}", id);
        QuoteResponse response = pricingService.getQuote(id);
        return ResponseEntity.ok(response);
    }

    /**
     * get all quotes with filters (productId, minPrice)
     */
    @GetMapping
    public ResponseEntity<List<QuoteResponse>> getAllQuotes(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Double minPrice) {

        log.info("Fetching quotes with filters productId={}, minPrice={}", productId, minPrice);

        List<Quote> quotes;

        // simple filtering logic
        if (productId != null && minPrice != null) {
            quotes = quoteRepository.findByProductId(productId)
                    .stream()
                    .filter(q -> q.getFinalPrice().doubleValue() >= minPrice)
                    .toList();

        } else if (productId != null) {
            quotes = quoteRepository.findByProductId(productId);

        } else if (minPrice != null) {
            quotes = quoteRepository.findAll()
                    .stream()
                    .filter(q -> q.getFinalPrice().doubleValue() >= minPrice)
                    .toList();

        } else {
            quotes = quoteRepository.findAll();
        }

        // map to response
        List<QuoteResponse> responses = new ArrayList<>();

        for (Quote quote : quotes) {
            List<String> rules = deserializeRules(quote.getAppliedRules());

            QuoteResponse response = QuoteResponse.builder()
                    .quoteId(quote.getId())
                    .productName(quote.getProduct().getName())
                    .zoneName(quote.getZone().getName())
                    .clientName(quote.getClientName())
                    .clientAge(quote.getClientAge())
                    .basePrice(quote.getBasePrice())
                    .finalPrice(quote.getFinalPrice())
                    .appliedRules(rules)
                    .createdAt(quote.getCreatedAt())
                    .build();

            responses.add(response);
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * small helper to convert json -> list
     */
    private List<String> deserializeRules(String rulesJson) {
        try {
            return objectMapper.readValue(
                    rulesJson,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );
        } catch (Exception e) {
            log.error("error reading rules json", e);
            return new ArrayList<>();
        }
    }
}
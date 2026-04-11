package com.tendanz.pricing.service;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.PricingRule;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Quote;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.enums.AgeCategory;
import com.tendanz.pricing.repository.PricingRuleRepository;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final ProductRepository productRepository;
    private final ZoneRepository zoneRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final QuoteRepository quoteRepository;
    private final ObjectMapper objectMapper;

    /**
     * calc a quote based on business rules.
     */
    @Transactional
    public QuoteResponse calculateQuote(QuoteRequest request) {

        //Validate age
        if (request.getClientAge() < 18 || request.getClientAge() > 99) {
            throw new IllegalArgumentException("Client age must be between 18 and 99");
        }

        // 0 1. load product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with ID: " + request.getProductId()));

        // 2. load zone
        Zone zone = zoneRepository.findByCode(request.getZoneCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Zone not found with code: " + request.getZoneCode()));

        // 3. load pricing rule
        PricingRule pricingRule = pricingRuleRepository.findByProductId(product.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pricing rule not found for product ID: " + product.getId()));

        // 4. getermine age category
        AgeCategory ageCategory = AgeCategory.fromAge(request.getClientAge());

        // 5. get age factor
        BigDecimal ageFactor = getAgeFactor(pricingRule, ageCategory);

        // base & coeffs
        BigDecimal baseRate = pricingRule.getBaseRate();
        BigDecimal zoneCoeff = zone.getRiskCoefficient();

        // 6. calc final price
        BigDecimal finalPrice = baseRate
                .multiply(ageFactor)
                .multiply(zoneCoeff)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. build applied rules
        List<String> appliedRules = new ArrayList<>();

        appliedRules.add("Base price for product '" + product.getName() + "' = " + baseRate);
        appliedRules.add("Client age " + request.getClientAge()
                + " => category " + ageCategory + " (factor " + ageFactor + ")");
        appliedRules.add("Zone '" + zone.getName()
                + "' => coefficient " + zoneCoeff);
        appliedRules.add("Final price = " + baseRate + " × "
                + ageFactor + " × " + zoneCoeff + " = " + finalPrice);

        // 8. create and save quote
        Quote quote = Quote.builder()
                .product(product)
                .zone(zone)
                .clientName(request.getClientName())
                .clientAge(request.getClientAge())
                .basePrice(baseRate)
                .finalPrice(finalPrice)
                .appliedRules(convertRulesToJson(appliedRules))
                .build();

        quote = quoteRepository.save(quote);

        // 9. Return response
        return mapToResponse(quote, appliedRules);
    }

    /**
     * Get the age factor for a specific category
     */
    private BigDecimal getAgeFactor(PricingRule pricingRule, AgeCategory ageCategory) {
        return switch (ageCategory) {
            case YOUNG -> pricingRule.getAgeFactorYoung();
            case ADULT -> pricingRule.getAgeFactorAdult();
            case SENIOR -> pricingRule.getAgeFactorSenior();
            case ELDERLY -> pricingRule.getAgeFactorElderly();
        };
    }

    /**
     * Convert applied rules to JSON string
     */
    private String convertRulesToJson(List<String> rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("Error converting rules to JSON", e);
            return "[]";
        }
    }

    

    /**
     * Map Quote entity to response DTO.
     */
    private QuoteResponse mapToResponse(Quote quote, List<String> appliedRules) {
        return QuoteResponse.builder()
                .quoteId(quote.getId())
                .productName(quote.getProduct().getName())
                .zoneName(quote.getZone().getName())
                .clientName(quote.getClientName())
                .clientAge(quote.getClientAge())
                .basePrice(quote.getBasePrice())
                .finalPrice(quote.getFinalPrice())
                .appliedRules(appliedRules)
                .createdAt(quote.getCreatedAt())
                .build();
    }



    /**
     * Get quote by ID.
     */
    @Transactional
    public QuoteResponse getQuote(Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quote not found with ID: " + id));

        List<String> appliedRules = deserializeRules(quote.getAppliedRules());
        return mapToResponse(quote, appliedRules);
    }

    /**
     * Deserialize rules JSON to list
     */


    private List<String> deserializeRules(String rulesJson) {
        try {
            return objectMapper.readValue(
                    rulesJson,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );
        } catch (Exception e) {
            log.error("Error deserializing rules from JSON", e);
            return new ArrayList<>();
        }
    }
}
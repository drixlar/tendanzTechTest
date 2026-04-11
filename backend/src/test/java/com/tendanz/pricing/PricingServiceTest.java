package com.tendanz.pricing;

import com.tendanz.pricing.dto.QuoteRequest;
import com.tendanz.pricing.dto.QuoteResponse;
import com.tendanz.pricing.entity.PricingRule;
import com.tendanz.pricing.entity.Product;
import com.tendanz.pricing.entity.Zone;
import com.tendanz.pricing.repository.PricingRuleRepository;
import com.tendanz.pricing.repository.ProductRepository;
import com.tendanz.pricing.repository.QuoteRepository;
import com.tendanz.pricing.repository.ZoneRepository;
import com.tendanz.pricing.service.PricingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({PricingService.class, ObjectMapper.class})
class PricingServiceTest {

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @Autowired
    private QuoteRepository quoteRepository;

    private Product product;
    private Zone zone;
    private PricingRule pricingRule;

    @BeforeEach
    void setUp() {
        // basic test data setup
        product = Product.builder()
                .name("Test Auto Insurance")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(product);

        zone = Zone.builder()
                .code("TUN")
                .name("Grand Tunis")
                .riskCoefficient(BigDecimal.valueOf(1.20))
                .build();
        zoneRepository.save(zone);

        pricingRule = PricingRule.builder()
                .product(product)
                .baseRate(BigDecimal.valueOf(500.00))
                .ageFactorYoung(BigDecimal.valueOf(1.30))
                .ageFactorAdult(BigDecimal.valueOf(1.00))
                .ageFactorSenior(BigDecimal.valueOf(1.20))
                .ageFactorElderly(BigDecimal.valueOf(1.50))
                .createdAt(LocalDateTime.now())
                .build();
        pricingRuleRepository.save(pricingRule);
    }

    @Test
    void testCalculateQuoteForAdult() {
            // adult case -> should use factor 1.00
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("John")
                .clientAge(30)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(500.00), response.getBasePrice());
        assertEquals(BigDecimal.valueOf(600.00).setScale(2), response.getFinalPrice());
    }

    @Test
    void testCalculateQuoteForYoungClient() {

        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Young")
                .clientAge(20)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertEquals(BigDecimal.valueOf(780.00).setScale(2), response.getFinalPrice());
    }

    @Test
    void testCalculateQuoteForSeniorClient() {

        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Senior")
                .clientAge(50)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertEquals(BigDecimal.valueOf(720.00).setScale(2), response.getFinalPrice());
    }

    @Test
    void testCalculateQuoteWithInvalidProductId() {

        QuoteRequest request = QuoteRequest.builder()
                .productId(999L)
                .zoneCode("TUN")
                .clientName("Test")
                .clientAge(30)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.calculateQuote(request);
        });
    }

    @Test
    void testCalculateQuoteWithInvalidZoneCode() {

        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("XXX")
                .clientName("Test")
                .clientAge(30)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            pricingService.calculateQuote(request);
        });
    }

    @Test
    void testAgeBoundaries() {

        // 24 -> YOUNG
        QuoteRequest r1 = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("A")
                .clientAge(24)
                .build();

        QuoteResponse res1 = pricingService.calculateQuote(r1);
        assertEquals(BigDecimal.valueOf(780.00).setScale(2), res1.getFinalPrice());

        // 25 -> ADULT
        QuoteRequest r2 = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("B")
                .clientAge(25)
                .build();

        QuoteResponse res2 = pricingService.calculateQuote(r2);
        assertEquals(BigDecimal.valueOf(600.00).setScale(2), res2.getFinalPrice());
    }

    @Test
    void testGetQuoteById() {

        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("Stored User")
                .clientAge(30)
                .build();

        QuoteResponse created = pricingService.calculateQuote(request);

        QuoteResponse fetched = pricingService.getQuote(created.getQuoteId());

        assertEquals(created.getQuoteId(), fetched.getQuoteId());
        assertEquals(created.getFinalPrice(), fetched.getFinalPrice());
        assertEquals("Stored User", fetched.getClientName());
    }
}
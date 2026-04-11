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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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

    @BeforeEach
    void setUp() {
        quoteRepository.deleteAll();
        pricingRuleRepository.deleteAll();
        zoneRepository.deleteAll();
        productRepository.deleteAll();

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

        PricingRule pricingRule = PricingRule.builder()
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
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("TUN")
                .clientName("John")
                .clientAge(30)
                .build();

        QuoteResponse response = pricingService.calculateQuote(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("500.00"), response.getBasePrice());
        assertEquals(new BigDecimal("600.00"), response.getFinalPrice());
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
        assertNotNull(response);
        assertEquals(new BigDecimal("780.00"), response.getFinalPrice());
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
        assertNotNull(response);
        assertEquals(new BigDecimal("720.00"), response.getFinalPrice());
    }

    @Test
    void testCalculateQuoteWithInvalidProductId() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(999L)
                .zoneCode("TUN")
                .clientName("Test")
                .clientAge(30)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                pricingService.calculateQuote(request));
    }

    @Test
    void testCalculateQuoteWithInvalidZoneCode() {
        QuoteRequest request = QuoteRequest.builder()
                .productId(product.getId())
                .zoneCode("XXX")
                .clientName("Test")
                .clientAge(30)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                pricingService.calculateQuote(request));
    }

    @Test
    void testAgeBoundaries() {
        QuoteRequest r1 = QuoteRequest.builder()
                .productId(product.getId()).zoneCode("TUN")
                .clientName("A").clientAge(24).build();
        assertEquals(new BigDecimal("780.00"), pricingService.calculateQuote(r1).getFinalPrice());

        QuoteRequest r2 = QuoteRequest.builder()
                .productId(product.getId()).zoneCode("TUN")
                .clientName("B").clientAge(25).build();
        assertEquals(new BigDecimal("600.00"), pricingService.calculateQuote(r2).getFinalPrice());

        QuoteRequest r3 = QuoteRequest.builder()
                .productId(product.getId()).zoneCode("TUN")
                .clientName("C").clientAge(46).build();
        assertEquals(new BigDecimal("720.00"), pricingService.calculateQuote(r3).getFinalPrice());

        QuoteRequest r4 = QuoteRequest.builder()
                .productId(product.getId()).zoneCode("TUN")
                .clientName("D").clientAge(99).build();
        assertEquals(new BigDecimal("900.00"), pricingService.calculateQuote(r4).getFinalPrice());
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

        assertNotNull(fetched);
        assertEquals(created.getQuoteId(), fetched.getQuoteId());
        assertEquals(created.getFinalPrice(), fetched.getFinalPrice());
        assertEquals("Stored User", fetched.getClientName());
        assertEquals("Grand Tunis", fetched.getZoneName());
        assertEquals("Test Auto Insurance", fetched.getProductName());
    }
}
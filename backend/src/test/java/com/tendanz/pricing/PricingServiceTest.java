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

    // runs before every test - cleans the DB and sets up fresh test data

    @BeforeEach
    void setUp() {

    // clear in correct order to respect foreign keys
        quoteRepository.deleteAll();
        pricingRuleRepository.deleteAll();
        zoneRepository.deleteAll();
        productRepository.deleteAll();

    // create a simple product to test with
        product = Product.builder()
                .name("Test Auto Insurance")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .build();
        productRepository.save(product);

    // Tunis zone with 1.20 risk coefficient
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

    // age 30 = ADULT, factor 1.00 → 500 * 1.00 * 1.20 = 600.00
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

    // age 20 = YOUNG, factor 1.30 → 500 * 1.30 * 1.20 = 780.00
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

    // age 50 = SENIOR, factor 1.20 → 500 * 1.20 * 1.20 = 720.00
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

    // product ID 999 doesn't exist - service should throw
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

    // zone code XXX doesn't exist - service should throw
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

    // checks the age category boundaries:
    // 24 → YOUNG, 25 → ADULT, 46 → SENIOR, 99 → ELDERLY
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

    // makes sure we can retrieve a quote after saving it
    // and that all the fields come back correctly
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
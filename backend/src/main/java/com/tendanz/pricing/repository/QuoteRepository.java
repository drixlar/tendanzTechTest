package com.tendanz.pricing.repository;

import com.tendanz.pricing.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface QuoteRepository extends JpaRepository<Quote, Long> {

    // find by client name (simple derived query)
    List<Quote> findByClientName(String clientName);

    // find by product id
    List<Quote> findByProductId(Long productId);

    // custom query for price threshold
    @Query("SELECT q FROM Quote q WHERE q.finalPrice >= :minPrice")
    List<Quote> findQuotesWithMinPrice(@Param("minPrice") BigDecimal minPrice);
}
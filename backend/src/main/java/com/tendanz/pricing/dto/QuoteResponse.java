package com.tendanz.pricing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;


/**
 * DTO for quote response objects.
 * Contains all relevant quote information in a format suitable for API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteResponse {

    private Long quoteId;

    private String productName;

    private String zoneName;

    private String clientName;

    private Integer clientAge;

    private BigDecimal basePrice;

    private BigDecimal finalPrice;

    private List<String> appliedRules;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

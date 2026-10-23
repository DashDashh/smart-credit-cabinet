package com.hac.cabinet.dto.product;

import com.hac.cabinet.model.product.ProductPurpose;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    BigDecimal amount,
    int termMonths,
    BigDecimal rate,
    ProductPurpose purpose
) {
}

package com.hac.cabinet.dto.ml;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceApplication(
    String product_id,
    BigDecimal amount,
    Integer term_months,
    String purpose
) {
}

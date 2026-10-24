package com.hac.cabinet.dto.ml;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceApplication(
    String product_id,
    Double amount,
    Integer term_months,
    String purpose
) {
}

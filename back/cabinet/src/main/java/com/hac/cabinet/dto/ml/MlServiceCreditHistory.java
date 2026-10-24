package com.hac.cabinet.dto.ml;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceCreditHistory(
    Integer delinquencies_12m,
    Integer active_loans_count,
    BigDecimal total_debt_amount,
    Boolean tax_arrears
) {
}

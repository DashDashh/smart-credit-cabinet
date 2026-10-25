package com.hac.cabinet.dto.application;

import java.math.BigDecimal;

public record DecisionConditionsClientResponse(
    BigDecimal approvedAmount,
    int termMonths,
    BigDecimal rate,
    String reason
) {
}

package com.hac.cabinet.dto.ml;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceFinancialData(
    Double revenue_monthly,
    Double revenue_annual,
    Double current_debt_payments,
    Double net_profit_monthly,
    Double accounts_payable
) {
}

package com.hac.cabinet.dto.ml;

import java.math.BigDecimal;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceFinancialData(
    BigDecimal revenue_monthly,
    BigDecimal revenue_annual,
    BigDecimal current_debt_payments,
    BigDecimal net_profit_monthly,
    BigDecimal accounts_payable
) {
}

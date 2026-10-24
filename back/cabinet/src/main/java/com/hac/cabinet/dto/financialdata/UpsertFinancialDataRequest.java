package com.hac.cabinet.dto.financialdata;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpsertFinancialDataRequest(
    @NotNull @DecimalMin("0") BigDecimal revenueMonthly,
    @NotNull @DecimalMin("0") BigDecimal revenueAnnual,
    @NotNull @DecimalMin("0") BigDecimal currentDebtPayments,
    @NotNull BigDecimal netProfitMonthly,
    @NotNull @DecimalMin("0") BigDecimal accountsPayable
) {
}

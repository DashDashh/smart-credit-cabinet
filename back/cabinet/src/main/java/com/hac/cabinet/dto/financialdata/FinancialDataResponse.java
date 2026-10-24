package com.hac.cabinet.dto.financialdata;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialDataResponse(
    UUID id,
    BigDecimal revenueMonthly,
    BigDecimal revenueAnnual,
    BigDecimal currentDebtPayments,
    BigDecimal netProfitMonthly,
    BigDecimal accountsPayable,
    UUID companyId
) {
}

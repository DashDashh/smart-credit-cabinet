package com.hac.cabinet.dto.credithistory;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditHistoryResponse(
    UUID id,
    int delinquencies12m,
    int activeLoansCount,
    BigDecimal totalDebtAmount,
    boolean taxArrears,
    UUID companyId
) {
}

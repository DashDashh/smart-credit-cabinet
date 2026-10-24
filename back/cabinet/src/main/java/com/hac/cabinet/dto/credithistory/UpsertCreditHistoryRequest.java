package com.hac.cabinet.dto.credithistory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpsertCreditHistoryRequest(
    @Min(0) int delinquencies12m,
    @Min(0) int activeLoansCount,
    @NotNull @DecimalMin("0") BigDecimal totalDebtAmount,
    boolean taxArrears
) {
}

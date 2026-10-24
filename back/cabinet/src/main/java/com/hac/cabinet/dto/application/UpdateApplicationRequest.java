package com.hac.cabinet.dto.application;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateApplicationRequest(
    @NotNull @DecimalMin("0") BigDecimal amount,
    @Min(1) int termMonth,
    @NotBlank @Size(max = 255) String purpose
) {
}

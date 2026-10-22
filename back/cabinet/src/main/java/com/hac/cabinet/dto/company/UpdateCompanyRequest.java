package com.hac.cabinet.dto.company;

import com.hac.cabinet.model.company.BusinessStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCompanyRequest(
    @NotBlank @Size(max = 255) String industry,
    String name,
    LocalDate foundedAt,
    @Min(0) int employeesCount,
    @NotNull BusinessStatus businessStatus
) {
}

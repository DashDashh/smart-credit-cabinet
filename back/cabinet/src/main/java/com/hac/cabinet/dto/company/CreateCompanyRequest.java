package com.hac.cabinet.dto.company;

import com.hac.cabinet.model.company.BusinessStatus;
import com.hac.cabinet.model.company.CompanyType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCompanyRequest(
    @NotNull CompanyType type,
    String name,
    @NotBlank @Size(max = 255) String industry,
    LocalDate foundedAt,
    @Min(0) int employeesCount,
    @NotNull BusinessStatus businessStatus
) {
}

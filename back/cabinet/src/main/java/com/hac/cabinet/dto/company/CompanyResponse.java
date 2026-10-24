package com.hac.cabinet.dto.company;

import com.hac.cabinet.model.company.BusinessStatus;
import com.hac.cabinet.model.company.CompanyType;

import java.time.LocalDate;
import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    CompanyType type,
    String industry,
    LocalDate foundedAt,
    int employeesCount,
    BusinessStatus businessStatus,
    UUID ownerId
) {
}

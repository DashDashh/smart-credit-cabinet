package com.hac.cabinet.mapper.company;

import com.hac.cabinet.dto.company.CompanyResponse;
import com.hac.cabinet.dto.company.CreateCompanyRequest;
import com.hac.cabinet.dto.ml.MlServiceCompany;
import com.hac.cabinet.model.company.Company;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class CompanyMapper {

  public Company fromCreateRequest(CreateCompanyRequest request) {
    Company c = new Company();
    c.setName(request.name());
    c.setType(request.type());
    c.setIndustry(request.industry());
    c.setFoundedAt(request.foundedAt());
    c.setEmployeesCount(request.employeesCount());
    c.setStatus(request.businessStatus());
    return c;
  }

  public CompanyResponse toResponse(Company c) {
    return new CompanyResponse(
        c.getId(),
        c.getName(),
        c.getType(),
        c.getIndustry(),
        c.getFoundedAt(),
        c.getEmployeesCount(),
        c.getStatus(),
        c.getOwner() != null ? c.getOwner().getId() : null
    );
  }

  public MlServiceCompany toMlServiceCompany(Company company) {
    return new MlServiceCompany(
        company.getType().getStringExpression(),
        company.getIndustry(),
        ChronoUnit.MONTHS.between(company.getFoundedAt(), LocalDate.now()),
        company.getEmployeesCount(),
        company.getStatus().getStringExpression()
    );
  }
}

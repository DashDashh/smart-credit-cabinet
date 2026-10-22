package com.hac.cabinet.repository;

import com.hac.cabinet.model.company.FinancialData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FinancialDataRepository extends JpaRepository<FinancialData, UUID> {
  boolean existsByCompanyId(UUID companyId);
}

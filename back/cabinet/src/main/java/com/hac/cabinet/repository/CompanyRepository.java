package com.hac.cabinet.repository;

import com.hac.cabinet.model.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
  List<Company> findAllByOwnerId(UUID ownerId);
}

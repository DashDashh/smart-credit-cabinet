package com.hac.cabinet.service;

import com.hac.cabinet.dto.company.CompanyResponse;
import com.hac.cabinet.dto.company.CreateCompanyRequest;
import com.hac.cabinet.dto.company.UpdateCompanyRequest;
import com.hac.cabinet.mapper.company.CompanyMapper;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.repository.CompanyRepository;
import com.hac.cabinet.utils.CompanyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

  private final CompanyRepository companyRepository;
  private final AccountService accountService;
  private final CompanyMapper companyMapper;
  private final CompanyUtils companyUtils;

  @Transactional
  public CompanyResponse create(CreateCompanyRequest request, UUID accountId) {
    Account owner = accountService.getById(accountId);

    Company company = companyMapper.fromCreateRequest(request);
    company.setOwner(owner);

    return companyMapper.toResponse(companyRepository.save(company));
  }

  @Transactional(readOnly = true)
  public CompanyResponse getById(UUID id, UUID accountId) {
    Account account = accountService.getById(accountId);
    Company company;

    if (account.getRole().isEntrepreneur()) {
      company = companyUtils.findOwned(id, accountId);
    } else {
      company = companyUtils.getByIdOrThrow(id);
    }

    return companyMapper.toResponse(company);
  }

  @Transactional(readOnly = true)
  public List<CompanyResponse> getAllMine(UUID accountId) {
    Account owner = accountService.getById(accountId);
    return companyRepository.findAllByOwnerId(owner.getId()).stream()
        .map(companyMapper::toResponse)
        .toList();
  }

  @Transactional
  public CompanyResponse update(UUID id, UpdateCompanyRequest request, UUID accountId) {
    Company company = companyUtils.findOwned(id, accountId);

    company.setName(request.name());
    company.setIndustry(request.industry());
    company.setFoundedAt(request.foundedAt());
    company.setEmployeesCount(request.employeesCount());
    company.setStatus(request.businessStatus());

    return companyMapper.toResponse(companyRepository.save(company));
  }
}
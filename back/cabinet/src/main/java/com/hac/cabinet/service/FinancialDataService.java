package com.hac.cabinet.service;

import com.hac.cabinet.dto.financialdata.FinancialDataResponse;
import com.hac.cabinet.dto.financialdata.UpsertFinancialDataRequest;
import com.hac.cabinet.mapper.financialdata.FinancialDataMapper;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.model.company.FinancialData;
import com.hac.cabinet.repository.CompanyRepository;
import com.hac.cabinet.repository.FinancialDataRepository;
import com.hac.cabinet.utils.CompanyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialDataService {

  private final FinancialDataRepository financialDataRepository;
  private final CompanyRepository companyRepository;
  private final AccountService accountService;
  private final FinancialDataMapper financialDataMapper;
  private final CompanyUtils companyUtils;

  @Transactional
  public FinancialDataResponse create(UUID companyId, UpsertFinancialDataRequest request, UUID accountId) {
    Company company = companyUtils.findOwned(companyId, accountId);

    if (financialDataRepository.existsByCompanyId(companyId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Financial data already exists for company: " + companyId);
    }

    FinancialData data = financialDataMapper.fromRequest(request);
    data.setCompany(company);

    return financialDataMapper.toResponse(financialDataRepository.save(data));
  }

  @Transactional(readOnly = true)
  public FinancialDataResponse getByCompanyId(UUID companyId, UUID accountId) {
    Company company;
    Account account = accountService.getById(accountId);
    if (account.getRole().isEntrepreneur()) {
      company = companyUtils.findOwned(companyId, accountId);
    } else {
      company = companyUtils.getByIdOrThrow(companyId);
    }

    FinancialData data = company.getFinancialData();
    if (data == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Financial data not found for company: " + companyId);
    }

      return financialDataMapper.toResponse(data);
  }

  @Transactional
  public FinancialDataResponse update(UUID companyId, UpsertFinancialDataRequest request, UUID accountId) {
    Company company = companyUtils.findOwned(companyId, accountId);

    FinancialData data = company.getFinancialData();
    if (data == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Financial data not found for company: " + companyId);
    }

    data.setRevenueMonthly(request.revenueMonthly());
    data.setRevenueAnnual(request.revenueAnnual());
    data.setCurrentDebtPayments(request.currentDebtPayments());
    data.setNetProfitMonthly(request.netProfitMonthly());
    data.setAccountsPayable(request.accountsPayable());

    return financialDataMapper.toResponse(financialDataRepository.save(data));
  }
}

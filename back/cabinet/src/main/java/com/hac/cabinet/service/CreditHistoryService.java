package com.hac.cabinet.service;

import com.hac.cabinet.dto.credithistory.CreditHistoryResponse;
import com.hac.cabinet.dto.credithistory.UpsertCreditHistoryRequest;
import com.hac.cabinet.mapper.credithistory.CreditHistoryMapper;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.model.company.CreditHistory;
import com.hac.cabinet.repository.CreditHistoryRepository;
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
public class CreditHistoryService {

  private final CreditHistoryRepository creditHistoryRepository;
  private final CompanyUtils companyUtils;
  private final CreditHistoryMapper creditHistoryMapper;
  private final AccountService accountService;

  @Transactional
  public CreditHistoryResponse create(UUID companyId, UpsertCreditHistoryRequest request, UUID accountId) {
    Company company = companyUtils.findOwned(companyId, accountId);

    if (creditHistoryRepository.existsByCompanyId(companyId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Credit history already exists for company: " + companyId);
    }

    CreditHistory history = creditHistoryMapper.fromRequest(request);
    history.setCompany(company);

    return creditHistoryMapper.toResponse(creditHistoryRepository.save(history));
  }

  @Transactional(readOnly = true)
  public CreditHistoryResponse getByCompanyId(UUID companyId, UUID accountId) {
    Company company;
    Account account = accountService.getById(accountId);
    if (account.getRole().isEntrepreneur()) {
      company = companyUtils.findOwned(companyId, accountId);
    } else {
      company = companyUtils.getByIdOrThrow(companyId);
    }

    CreditHistory history = company.getCreditHistory();
    if (history == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Credit history not found for company: " + companyId);
    }

    return creditHistoryMapper.toResponse(history);
  }

  @Transactional
  public CreditHistoryResponse update(UUID companyId, UpsertCreditHistoryRequest request, UUID accountId) {
    Company company = companyUtils.findOwned(companyId, accountId);

    CreditHistory history = company.getCreditHistory();
    if (history == null) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Credit history not found for company: " + companyId);
    }

    history.setDelinquencies12m(request.delinquencies12m());
    history.setActiveLoansCount(request.activeLoansCount());
    history.setTotalDebtAmount(request.totalDebtAmount());
    history.setTaxArrears(request.taxArrears());

    return creditHistoryMapper.toResponse(creditHistoryRepository.save(history));
  }
}

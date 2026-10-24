package com.hac.cabinet.utils;

import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.repository.CompanyRepository;
import com.hac.cabinet.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CompanyUtils {

  private final CompanyRepository companyRepository;
  private final AccountService accountService;

  public Company findOwned(UUID companyId, UUID accountId) {
    Company company = getByIdOrThrow(companyId);

    Account account = accountService.getById(accountId);

    if (company.getOwner() == null
        || !company.getOwner().getId().equals(account.getId())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Company does not belong to current account");
    }

    return company;
  }

  public Company getByIdOrThrow(UUID companyId) {
    return companyRepository.findById(companyId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Company not found: " + companyId));
  }
}

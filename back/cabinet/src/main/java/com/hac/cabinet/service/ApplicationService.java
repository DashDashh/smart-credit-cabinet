package com.hac.cabinet.service;

import com.hac.cabinet.dto.application.ApplicationResponse;
import com.hac.cabinet.dto.application.ChangeApplicationStatusRequest;
import com.hac.cabinet.dto.application.CreateApplicationRequest;
import com.hac.cabinet.dto.application.UpdateApplicationRequest;
import com.hac.cabinet.mapper.application.ApplicationMapper;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.model.product.Product;
import com.hac.cabinet.repository.ApplicationRepository;
import com.hac.cabinet.repository.CompanyRepository;
import com.hac.cabinet.repository.ProductRepository;
import com.hac.cabinet.utils.CompanyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final ProductRepository productRepository;
  private final CompanyRepository companyRepository;
  private final ApplicationMapper applicationMapper;
  private final AccountService accountService;
  private final CompanyUtils companyUtils;

  @Transactional
  public ApplicationResponse create(UUID accountId, CreateApplicationRequest request) {
    ApplicationId id = new ApplicationId(request.productId(), request.clientId());

    if (applicationRepository.existsById(id)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Application already exists for product/client pair");
    }

    Product product = productRepository.findById(request.productId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Product not found: " + request.productId()));

    Account account = accountService.getById(accountId);
    if (!account.getRole().isCanCreateApplication()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "You don't have sufficient permissions to create application"
      );
    }
    Company client = companyUtils.findOwned(request.clientId(), account.getId());

    Application app = applicationMapper.fromCreateRequest(request, product, client);
    return applicationMapper.toResponse(applicationRepository.save(app));
  }

  @Transactional(readOnly = true)
  public ApplicationResponse getById(UUID accountId, UUID productId, UUID clientId) {
    Account account = accountService.getById(accountId);
    if (account.getRole().isEntrepreneur()) {
      companyUtils.findOwned(clientId, accountId);
    }
    return applicationMapper.toResponse(findById(productId, clientId));
  }

  @Transactional(readOnly = true)
  public List<ApplicationResponse> getAll(UUID accountId) {
    if (accountService.getById(accountId).getRole().isEntrepreneur()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Entrepreneur does not have access to the list of all applications"
      );
    }
    return applicationRepository.findAll().stream()
        .map(applicationMapper::toResponse)
        .toList();
  }

  @Transactional
  public ApplicationResponse update(
      UUID accountId,
      UUID productId,
      UUID clientId,
      UpdateApplicationRequest request
  ) {

    Account account = accountService.getById(accountId);
    if (!account.getRole().isCanCreateApplication()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "You don't have sufficient permissions to update application"
      );
    }
    companyUtils.findOwned(clientId, accountId);

    Application app = findById(productId, clientId);
    if (!app.getStatus().isCanBeUpdated()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Application can not be updated in this status"
      );
    }

    applicationMapper.applyUpdate(request, app);
    return applicationMapper.toResponse(applicationRepository.save(app));
  }

  @Transactional
  public ApplicationResponse changeStatus(
      UUID accountId,
      UUID productId,
      UUID clientId,
      ChangeApplicationStatusRequest request
  ) {
    Application app = findById(productId, clientId);
    if (!app.getStatus().canTransitTo(request.status())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Bad status transition"
      );
    }

    Account account = accountService.getById(accountId);
    if (account.getRole().isEntrepreneur()) {
      companyUtils.findOwned(clientId, account.getId());
    }
    if (!request.status().canTransitToByRole(account.getRole())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "You don't have sufficient permissions to do this transition"
      );
    }

    app.setStatus(request.status());
    return applicationMapper.toResponse(applicationRepository.save(app));
  }

  private Application findById(UUID productId, UUID clientId) {
    return applicationRepository.findById(new ApplicationId(productId, clientId))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Application not found for product " + productId + " and client " + clientId));
  }
}

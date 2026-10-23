package com.hac.cabinet.service;

import com.hac.cabinet.dto.application.ApplicationResponse;
import com.hac.cabinet.dto.application.ChangeApplicationStatusRequest;
import com.hac.cabinet.dto.application.CreateApplicationRequest;
import com.hac.cabinet.dto.application.UpdateApplicationRequest;
import com.hac.cabinet.mapper.application.ApplicationMapper;
import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.model.product.Product;
import com.hac.cabinet.repository.ApplicationRepository;
import com.hac.cabinet.repository.CompanyRepository;
import com.hac.cabinet.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

  @Transactional
  public ApplicationResponse create(CreateApplicationRequest request) {
    ApplicationId id = new ApplicationId(request.productId(), request.clientId());

    if (applicationRepository.existsById(id)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Application already exists for product/client pair");
    }

    Product product = productRepository.findById(request.productId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Product not found: " + request.productId()));

    Company client = companyRepository.findById(request.clientId())
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Client company not found: " + request.clientId()));

    Application app = applicationMapper.fromCreateRequest(request, product, client);
    return applicationMapper.toResponse(applicationRepository.save(app));
  }

  @Transactional(readOnly = true)
  public ApplicationResponse getById(UUID productId, UUID clientId) {
    return applicationMapper.toResponse(findById(productId, clientId));
  }

  @Transactional(readOnly = true)
  public List<ApplicationResponse> getAll() {
    return applicationRepository.findAll().stream()
        .map(applicationMapper::toResponse)
        .toList();
  }

  @Transactional
  public ApplicationResponse update(UUID productId, UUID clientId, UpdateApplicationRequest request) {
    Application app = findById(productId, clientId);
    applicationMapper.applyUpdate(request, app);
    return applicationMapper.toResponse(applicationRepository.save(app));
  }

  @Transactional
  public ApplicationResponse changeStatus(UUID productId, UUID clientId,
                                          ChangeApplicationStatusRequest request) {
    Application app = findById(productId, clientId);
    app.setStatus(request.status());
    return applicationMapper.toResponse(applicationRepository.save(app));
  }

  @Transactional
  public void delete(UUID productId, UUID clientId) {
    Application app = findById(productId, clientId);
    applicationRepository.delete(app);
  }

  private Application findById(UUID productId, UUID clientId) {
    return applicationRepository.findById(new ApplicationId(productId, clientId))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Application not found for product " + productId + " and client " + clientId));
  }
}

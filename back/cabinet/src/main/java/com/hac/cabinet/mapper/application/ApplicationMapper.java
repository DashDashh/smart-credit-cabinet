package com.hac.cabinet.mapper.application;

import com.hac.cabinet.dto.application.ApplicationResponse;
import com.hac.cabinet.dto.application.CreateApplicationRequest;
import com.hac.cabinet.dto.application.UpdateApplicationRequest;
import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import com.hac.cabinet.model.application.ApplicationStatus;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.model.product.Product;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

  public Application fromCreateRequest(CreateApplicationRequest request, Product product, Company client) {
    Application app = new Application();
    app.setId(new ApplicationId(request.productId(), request.clientId()));
    app.setProduct(product);
    app.setClient(client);
    app.setAmount(request.amount());
    app.setTermMonth(request.termMonth());
    app.setPurpose(request.purpose());
    app.setStatus(ApplicationStatus.PENDING);
    return app;
  }

  public void applyUpdate(UpdateApplicationRequest request, Application app) {
    app.setAmount(request.amount());
    app.setTermMonth(request.termMonth());
    app.setPurpose(request.purpose());
  }

  public ApplicationResponse toResponse(Application app) {
    return new ApplicationResponse(
        app.getId().getProductId(),
        app.getId().getClientId(),
        app.getAmount(),
        app.getTermMonth(),
        app.getPurpose(),
        app.getStatus()
    );
  }
}

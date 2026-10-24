package com.hac.cabinet.service;

import com.hac.cabinet.dto.ml.DecisionRequest;
import com.hac.cabinet.mapper.application.ApplicationMapper;
import com.hac.cabinet.mapper.company.CompanyMapper;
import com.hac.cabinet.mapper.credithistory.CreditHistoryMapper;
import com.hac.cabinet.mapper.financialdata.FinancialDataMapper;
import com.hac.cabinet.model.application.Application;
import com.hac.cabinet.model.application.ApplicationId;
import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlServiceDataCollector {

  private final ApplicationRepository applicationRepository;
  private final RestCommunicationService restCommunicationService;
  private final CompanyMapper companyMapper;
  private final FinancialDataMapper financialDataMapper;
  private final CreditHistoryMapper creditHistoryMapper;
  private final ApplicationMapper applicationMapper;


  @Transactional(readOnly = true)
  public void collectAndSend(ApplicationId applicationId) {
    Application application = applicationRepository.findFullById(applicationId)
        .orElseThrow();
    Company company = application.getClient();

    DecisionRequest decisionRequest = new DecisionRequest(
        "FOR TEST",//application.getId(),
        companyMapper.toMlServiceCompany(company),
        financialDataMapper.toMlServiceFinancialData(company.getFinancialData()),
        creditHistoryMapper.toMlServiceCreditHistory(company.getCreditHistory()),
        applicationMapper.toMlServiceApplication(application)
    );

    log.info("Data collected: sending request");
    restCommunicationService.requestDecision(decisionRequest);
  }
}

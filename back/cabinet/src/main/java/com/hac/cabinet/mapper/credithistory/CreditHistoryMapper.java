package com.hac.cabinet.mapper.credithistory;

import com.hac.cabinet.dto.credithistory.CreditHistoryResponse;
import com.hac.cabinet.dto.credithistory.UpsertCreditHistoryRequest;
import com.hac.cabinet.dto.ml.MlServiceCreditHistory;
import com.hac.cabinet.model.company.CreditHistory;
import org.springframework.stereotype.Component;

@Component
public class CreditHistoryMapper {

  public CreditHistory fromRequest(UpsertCreditHistoryRequest request) {
    CreditHistory history = new CreditHistory();
    history.setDelinquencies12m(request.delinquencies12m());
    history.setActiveLoansCount(request.activeLoansCount());
    history.setTotalDebtAmount(request.totalDebtAmount());
    history.setTaxArrears(request.taxArrears());
    return history;
  }

  public CreditHistoryResponse toResponse(CreditHistory history) {
    return new CreditHistoryResponse(
        history.getId(),
        history.getDelinquencies12m(),
        history.getActiveLoansCount(),
        history.getTotalDebtAmount(),
        history.isTaxArrears(),
        history.getCompany() != null ? history.getCompany().getId() : null
    );
  }

  public MlServiceCreditHistory toMlServiceCreditHistory(CreditHistory creditHistory) {
    return new MlServiceCreditHistory(
        creditHistory.getDelinquencies12m(),
        creditHistory.getActiveLoansCount(),
        creditHistory.getTotalDebtAmount(),
        creditHistory.isTaxArrears()
    );
  }
}

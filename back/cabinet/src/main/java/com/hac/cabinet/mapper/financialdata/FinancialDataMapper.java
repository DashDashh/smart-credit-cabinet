package com.hac.cabinet.mapper.financialdata;

import com.hac.cabinet.dto.financialdata.FinancialDataResponse;
import com.hac.cabinet.dto.financialdata.UpsertFinancialDataRequest;
import com.hac.cabinet.dto.ml.MlServiceFinancialData;
import com.hac.cabinet.model.company.FinancialData;
import org.springframework.stereotype.Component;

@Component
public class FinancialDataMapper {

  public FinancialData fromRequest(UpsertFinancialDataRequest request) {
    FinancialData data = new FinancialData();
    data.setRevenueMonthly(request.revenueMonthly());
    data.setRevenueAnnual(request.revenueAnnual());
    data.setCurrentDebtPayments(request.currentDebtPayments());
    data.setNetProfitMonthly(request.netProfitMonthly());
    data.setAccountsPayable(request.accountsPayable());
    return data;
  }

  public FinancialDataResponse toResponse(FinancialData data) {
    return new FinancialDataResponse(
        data.getId(),
        data.getRevenueMonthly(),
        data.getRevenueAnnual(),
        data.getCurrentDebtPayments(),
        data.getNetProfitMonthly(),
        data.getAccountsPayable(),
        data.getCompany() != null ? data.getCompany().getId() : null
    );
  }

  public MlServiceFinancialData toMlServiceFinancialData(FinancialData financialData) {
    return new MlServiceFinancialData(
        financialData.getRevenueMonthly(),
        financialData.getRevenueAnnual(),
        financialData.getCurrentDebtPayments(),
        financialData.getNetProfitMonthly(),
        financialData.getAccountsPayable()
    );
  }
}

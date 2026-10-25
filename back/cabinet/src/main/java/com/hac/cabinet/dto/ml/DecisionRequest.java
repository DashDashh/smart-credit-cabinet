package com.hac.cabinet.dto.ml;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DecisionRequest(
    MlServiceApplicationId application_id,
    MlServiceCompany company,
    MlServiceFinancialData financials,
    MlServiceCreditHistory credit_history,
    MlServiceApplication application
) {
}

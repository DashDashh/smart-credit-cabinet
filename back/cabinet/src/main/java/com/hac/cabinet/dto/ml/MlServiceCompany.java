package com.hac.cabinet.dto.ml;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MlServiceCompany(
    String company_type,
    String industry,
    Long age_months,
    Integer employees_count,
    String business_status
) {
}

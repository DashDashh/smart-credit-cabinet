package com.hac.cabinet.dto.ml;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DecisionResponse(
    Double score,
    RiskLevel risk_level,
    DecisionType decision,
    Optional<Conditions> conditions,
    Optional<Double> rate,
    List<Factor> factors,
    String next_step,
    String model_version,
    Boolean fallback_used
) {
  public enum RiskLevel { low, medium, high }
  public enum DecisionType { approved, approved_with_conditions, needs_info, rejected, manual_review }
  public record Conditions(
      BigDecimal approved_amount,
      Integer term_months,
      BigDecimal rate,
      String reason
  ) {}
  public record Factor(
      String name,
      BigDecimal impact,
      Object value,
      String description) {}
}

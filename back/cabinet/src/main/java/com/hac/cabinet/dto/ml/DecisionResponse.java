package com.hac.cabinet.dto.ml;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.hac.cabinet.model.decision.DecisionType;
import com.hac.cabinet.model.decision.RiskLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DecisionResponse(
    MlServiceApplicationId application_id,
    BigDecimal score,
    RiskLevel risk_level,
    DecisionType decision,
    Optional<Conditions> conditions,
    Optional<BigDecimal> rate,
    List<Factor> factors,
    String next_step,
    String model_version,
    Boolean fallback_used
) {

  @RequiredArgsConstructor
  @Getter
  public enum RiskLevel {
    low(com.hac.cabinet.model.decision.RiskLevel.LOW),
    medium(com.hac.cabinet.model.decision.RiskLevel.MEDIUM),
    high(com.hac.cabinet.model.decision.RiskLevel.HIGH);

    private final com.hac.cabinet.model.decision.RiskLevel equivalent;
  }

  @RequiredArgsConstructor
  @Getter
  public enum DecisionType {
    approved(com.hac.cabinet.model.decision.DecisionType.APPROVED),
    approved_with_conditions(com.hac.cabinet.model.decision.DecisionType.APPROVED_WITH_CONDITIONS),
    needs_info(com.hac.cabinet.model.decision.DecisionType.NEEDS_INFO),
    rejected(com.hac.cabinet.model.decision.DecisionType.REJECTED),
    manual_review(com.hac.cabinet.model.decision.DecisionType.MANUAL_REVIEW);

    private final com.hac.cabinet.model.decision.DecisionType equivalent;
  }
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

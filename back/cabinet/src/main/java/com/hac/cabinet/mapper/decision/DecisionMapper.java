package com.hac.cabinet.mapper.decision;

import com.hac.cabinet.dto.ml.DecisionResponse;
import com.hac.cabinet.dto.ml.MlServiceApplicationId;
import com.hac.cabinet.model.application.ApplicationId;
import com.hac.cabinet.model.decision.Decision;
import com.hac.cabinet.model.decision.DecisionConditions;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DecisionMapper {

  public Decision toDecision(DecisionResponse response) {
    Decision decision = new Decision();
    decision.setScore(response.score());
    decision.setRiskLevel(response.risk_level().getEquivalent());
    decision.setDecision(response.decision().getEquivalent());
    if (response.rate().isPresent()) {
      decision.setRate(response.rate().get());
    } else {
      decision.setRate(new BigDecimal(0));
    }
    decision.setNextStep(response.next_step());
    decision.setModelVersion(response.model_version());
    decision.setFallbackUsed(response.fallback_used());

    return decision;
  }

  public ApplicationId toApplicationId(MlServiceApplicationId source) {
    if (source == null) {
      return null;
    }
    ApplicationId target = new ApplicationId();
    target.setProductId(source.product_id());
    target.setClientId(source.client_id());
    return target;
  }

  public DecisionConditions toDecisionConditions(DecisionResponse.Conditions source) {
    DecisionConditions target = new DecisionConditions();
    target.setApprovedAmount(source.approved_amount());
    target.setTermMonths(
        source.term_months() != null
            ? source.term_months()
            : 0
    );
    target.setRate(source.rate());
    target.setReason(source.reason());

    return target;
  }
}

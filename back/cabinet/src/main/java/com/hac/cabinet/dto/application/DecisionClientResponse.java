package com.hac.cabinet.dto.application;

import com.hac.cabinet.model.decision.DecisionType;
import com.hac.cabinet.model.decision.RiskLevel;

import java.math.BigDecimal;

public record DecisionClientResponse (
    BigDecimal score,
    RiskLevel riskLevel,
    DecisionType decision,
    BigDecimal rate,
    String nextStep,
    String modelVersion,
    boolean fallbackUsed,
    DecisionConditionsClientResponse conditions
) {
}

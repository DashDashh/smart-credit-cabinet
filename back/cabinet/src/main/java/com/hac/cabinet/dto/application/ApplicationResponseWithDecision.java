package com.hac.cabinet.dto.application;

public record ApplicationResponseWithDecision(
    ApplicationResponse applicationResponse,
    DecisionClientResponse decisionClientResponse
) {
}

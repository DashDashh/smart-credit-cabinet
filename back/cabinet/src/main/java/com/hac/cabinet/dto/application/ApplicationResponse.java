package com.hac.cabinet.dto.application;

import com.hac.cabinet.model.application.ApplicationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplicationResponse(
    UUID productId,
    UUID clientId,
    BigDecimal amount,
    int termMonth,
    String purpose,
    ApplicationStatus status
) {
}

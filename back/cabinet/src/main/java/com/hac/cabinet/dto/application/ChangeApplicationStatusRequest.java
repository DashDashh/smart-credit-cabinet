package com.hac.cabinet.dto.application;

import com.hac.cabinet.model.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeApplicationStatusRequest(
    @NotNull ApplicationStatus status
) {
}

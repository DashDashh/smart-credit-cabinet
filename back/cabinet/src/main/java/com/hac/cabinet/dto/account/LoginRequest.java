package com.hac.cabinet.dto.account;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String login,
    @NotBlank String password
) {
}

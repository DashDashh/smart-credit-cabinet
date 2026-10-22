package com.hac.cabinet.dto.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(min = 3, max = 32) String login,
    @NotBlank @Email @Size(max = 254) String email
) {
}

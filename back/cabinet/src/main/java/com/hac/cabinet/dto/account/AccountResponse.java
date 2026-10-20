package com.hac.cabinet.dto.account;

import com.hac.cabinet.model.Role;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String login,
    String email,
    Role role,
    Instant createdAt,
    Instant updatedAt
) {
}

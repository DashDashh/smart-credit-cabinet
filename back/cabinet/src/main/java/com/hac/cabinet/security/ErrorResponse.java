package com.hac.cabinet.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class ErrorResponse {

  private final Instant timestamp;
  private final UUID errorId;
  private final String message;
  private final String path;
}

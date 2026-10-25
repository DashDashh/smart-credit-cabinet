package com.hac.cabinet.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      ResponseStatusException ex, WebRequest request
  ) {
    UUID id = UUID.randomUUID();
    logError(ex.getReason(), id, ex);
    return ResponseEntity
        .status(ex.getStatusCode())
        .body(
            new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                id,
                ex.getMessage(),
                request instanceof ServletWebRequest ? ((ServletWebRequest) request).getRequest().getRequestURI() : null
            )
        );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllExceptions(
      Exception ex, WebRequest request
  ) {
    UUID id = UUID.randomUUID();
    logError("INTERNAL SERVER ERROR", id, ex);
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                id,
                ex.getMessage(),
                request instanceof ServletWebRequest ? ((ServletWebRequest) request).getRequest().getRequestURI() : null
            )
        );
  }

  private void logError(String message, UUID id, Exception ex) {
    final String logMessage = "%s (errorId=%s): ".formatted(message, id);
    log.error(logMessage, ex);
  }
}

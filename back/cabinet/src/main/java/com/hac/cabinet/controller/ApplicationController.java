package com.hac.cabinet.controller;

import com.hac.cabinet.dto.application.*;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.service.ApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Управление заявками")
public class ApplicationController {

  private final ApplicationService applicationService;

  @PostMapping
  public ResponseEntity<ApplicationResponse> create(
      @AuthenticationPrincipal Account account,
      @Valid @RequestBody CreateApplicationRequest request
  ) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(applicationService.createAndSendRequest(account.getId(), request));
  }

  @GetMapping("/{productId}/{clientId}")
  public ResponseEntity<ApplicationResponse> getById(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID productId,
      @PathVariable UUID clientId
  ) {
    return ResponseEntity.ok(applicationService.getById(account.getId(), productId, clientId));
  }

  @GetMapping
  public ResponseEntity<List<ApplicationResponse>> getAll(
      @AuthenticationPrincipal Account account) {
    return ResponseEntity.ok(applicationService.getAll(account.getId()));
  }

  @GetMapping("/{clientId}")
  public ResponseEntity<List<ApplicationResponse>> getAllByCompany(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID clientId
  ) {
    return ResponseEntity.ok(applicationService.getAllByCompanyId(account.getId(), clientId));
  }

  @PutMapping("/{productId}/{clientId}")
  public ResponseEntity<ApplicationResponse> update(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID productId,
      @PathVariable UUID clientId,
      @Valid @RequestBody UpdateApplicationRequest request) {
    return ResponseEntity.ok(applicationService.update(account.getId(), productId, clientId, request));
  }

  @PatchMapping("/{productId}/{clientId}/status")
  public ResponseEntity<ApplicationResponse> changeStatus(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID productId,
      @PathVariable UUID clientId,
      @Valid @RequestBody ChangeApplicationStatusRequest request) {
    return ResponseEntity.ok(applicationService.changeStatus(account.getId(), productId, clientId, request));
  }

  @GetMapping("/{productId}/{clientId}/withDecision")
  @PreAuthorize("hasRole('BANK_EMPLOYEE')")
  public ResponseEntity<ApplicationResponseWithDecision> getWithDecision(
      @PathVariable UUID productId,
      @PathVariable UUID clientId
  ) {
    return ResponseEntity.ok(applicationService.getWithDecision(productId, clientId));
  }
}

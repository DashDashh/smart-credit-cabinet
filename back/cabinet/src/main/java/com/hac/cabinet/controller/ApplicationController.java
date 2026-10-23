package com.hac.cabinet.controller;

import com.hac.cabinet.dto.application.ApplicationResponse;
import com.hac.cabinet.dto.application.ChangeApplicationStatusRequest;
import com.hac.cabinet.dto.application.CreateApplicationRequest;
import com.hac.cabinet.dto.application.UpdateApplicationRequest;
import com.hac.cabinet.service.ApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
      @Valid @RequestBody CreateApplicationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(applicationService.create(request));
  }

  @GetMapping("/{productId}/{clientId}")
  public ResponseEntity<ApplicationResponse> getById(
      @PathVariable UUID productId,
      @PathVariable UUID clientId) {
    return ResponseEntity.ok(applicationService.getById(productId, clientId));
  }

  @GetMapping
  public ResponseEntity<List<ApplicationResponse>> getAll() {
    return ResponseEntity.ok(applicationService.getAll());
  }

  @PutMapping("/{productId}/{clientId}")
  public ResponseEntity<ApplicationResponse> update(
      @PathVariable UUID productId,
      @PathVariable UUID clientId,
      @Valid @RequestBody UpdateApplicationRequest request) {
    return ResponseEntity.ok(applicationService.update(productId, clientId, request));
  }

  @PatchMapping("/{productId}/{clientId}/status")
  public ResponseEntity<ApplicationResponse> changeStatus(
      @PathVariable UUID productId,
      @PathVariable UUID clientId,
      @Valid @RequestBody ChangeApplicationStatusRequest request) {
    return ResponseEntity.ok(applicationService.changeStatus(productId, clientId, request));
  }

  @DeleteMapping("/{productId}/{clientId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID productId,
      @PathVariable UUID clientId) {
    applicationService.delete(productId, clientId);
    return ResponseEntity.noContent().build();
  }
}

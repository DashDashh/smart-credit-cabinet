package com.hac.cabinet.controller;

import com.hac.cabinet.dto.credithistory.CreditHistoryResponse;
import com.hac.cabinet.dto.credithistory.UpsertCreditHistoryRequest;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.service.CreditHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies/{companyId}/credit-history")
@RequiredArgsConstructor
@Tag(name = "Управление кредитной историей")
public class CreditHistoryController {

  private final CreditHistoryService creditHistoryService;

  @PreAuthorize("hasRole('ENTREPRENEUR')")
  @PostMapping
  public ResponseEntity<CreditHistoryResponse> create(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID companyId,
      @Valid @RequestBody UpsertCreditHistoryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(creditHistoryService.create(companyId, request, account.getId()));
  }

  @GetMapping
  public ResponseEntity<CreditHistoryResponse> get(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID companyId) {
    return ResponseEntity.ok(creditHistoryService.getByCompanyId(companyId, account.getId()));
  }

  @PreAuthorize("hasRole('ENTREPRENEUR')")
  @PutMapping
  public ResponseEntity<CreditHistoryResponse> update(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID companyId,
      @Valid @RequestBody UpsertCreditHistoryRequest request) {
    return ResponseEntity.ok(creditHistoryService.update(companyId, request, account.getId()));
  }
}

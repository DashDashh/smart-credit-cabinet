package com.hac.cabinet.controller;

import com.hac.cabinet.dto.financialdata.FinancialDataResponse;
import com.hac.cabinet.dto.financialdata.UpsertFinancialDataRequest;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.service.FinancialDataService;
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
@RequestMapping("/api/companies/{companyId}/financial-data")
@RequiredArgsConstructor
@Tag(name = "Управление данными о финансах")
public class FinancialDataController {

  private final FinancialDataService financialDataService;

  @PreAuthorize("hasRole('ENTREPRENEUR')")
  @PostMapping
  public ResponseEntity<FinancialDataResponse> create(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID companyId,
      @Valid @RequestBody UpsertFinancialDataRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(financialDataService.create(companyId, request, account.getId()));
  }

  @GetMapping
  public ResponseEntity<FinancialDataResponse> get(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID companyId) {
    return ResponseEntity.ok(financialDataService.getByCompanyId(companyId, account.getId()));
  }

  @PreAuthorize("hasRole('ENTREPRENEUR')")
  @PutMapping
  public ResponseEntity<FinancialDataResponse> update(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID companyId,
      @Valid @RequestBody UpsertFinancialDataRequest request) {
    return ResponseEntity.ok(financialDataService.update(companyId, request, account.getId()));
  }
}

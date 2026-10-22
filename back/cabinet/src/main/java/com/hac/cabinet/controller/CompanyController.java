package com.hac.cabinet.controller;

import com.hac.cabinet.dto.company.CompanyResponse;
import com.hac.cabinet.dto.company.CreateCompanyRequest;
import com.hac.cabinet.dto.company.UpdateCompanyRequest;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.service.CompanyService;
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
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Управление компанией")
public class CompanyController {

  private final CompanyService companyService;

  @PostMapping
  @PreAuthorize("hasRole('ENTREPRENEUR')")
  public ResponseEntity<CompanyResponse> create(
      @AuthenticationPrincipal Account account,
      @Valid @RequestBody CreateCompanyRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(companyService.create(request, account.getId()));
  }

  @GetMapping("/{id}")
  public ResponseEntity<CompanyResponse> getById(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID id) {
    return ResponseEntity.ok(companyService.getById(id, account.getId()));
  }

  @GetMapping("/my")
  @PreAuthorize("hasRole('ENTREPRENEUR')")
  public ResponseEntity<List<CompanyResponse>> getAllMine(
      @AuthenticationPrincipal Account account) {
    return ResponseEntity.ok(companyService.getAllMine(account.getId()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ENTREPRENEUR')")
  public ResponseEntity<CompanyResponse> update(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCompanyRequest request) {
    return ResponseEntity.ok(companyService.update(id, request, account.getId()));
  }
}

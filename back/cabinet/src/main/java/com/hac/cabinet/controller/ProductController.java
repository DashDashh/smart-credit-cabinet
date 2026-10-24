package com.hac.cabinet.controller;

import com.hac.cabinet.dto.product.UpsertProductRequest;
import com.hac.cabinet.dto.product.ProductResponse;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Управление продуктами")
public class ProductController {

  private final ProductService productService;

  @PreAuthorize("hasRole('BANK_EMPLOYEE')")
  @PostMapping
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody UpsertProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(productService.create(request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getById(
      @AuthenticationPrincipal Account account,
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(productService.getById(account.getId(), id));
  }

  @GetMapping
  public ResponseEntity<List<ProductResponse>> getAll(@AuthenticationPrincipal Account account) {
    return ResponseEntity.ok(productService.getAll(account.getId()));
  }

  @PreAuthorize("hasRole('BANK_EMPLOYEE')")
  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpsertProductRequest request) {
    return ResponseEntity.ok(productService.update(id, request));
  }
}

package com.hac.cabinet.controller;

import com.hac.cabinet.dto.account.AccountResponse;
import com.hac.cabinet.dto.account.UpdateProfileRequest;
import com.hac.cabinet.mapper.account.AccountMapper;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Управление аккаунтом")
public class AccountController {

  private final AccountService accountService;
  private final AccountMapper accountMapper;

  // Получить свой профиль (по текущему аутентифицированному пользователю)
  @GetMapping("/me")
  public ResponseEntity<AccountResponse> getMyProfile(@AuthenticationPrincipal Account account) {
    return ResponseEntity.ok(
        accountService.getResponseByLogin(account.getUsername())
    );
  }

  // Обновить свой профиль (только login и email)
  @PutMapping("/me")
  public ResponseEntity<AccountResponse> updateMyProfile(
      @AuthenticationPrincipal Account account,
      @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok(
        accountMapper.toResponse(accountService.update(account, request))
    );
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  public ResponseEntity<List<AccountResponse>> getAll() {
    return ResponseEntity.ok(
        accountService.getAll().stream()
            .map(accountMapper::toResponse)
            .collect(Collectors.toList())
    );
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{id}")
  public ResponseEntity<AccountResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(accountMapper.toResponse(accountService.getById(id)));
  }
}

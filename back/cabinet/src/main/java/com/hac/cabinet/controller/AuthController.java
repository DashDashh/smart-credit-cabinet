package com.hac.cabinet.controller;

import com.hac.cabinet.dto.account.AccountResponse;
import com.hac.cabinet.dto.account.LoginRequest;
import com.hac.cabinet.dto.account.RegisterRequest;
import com.hac.cabinet.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Авторизация / регистрация")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AccountResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}

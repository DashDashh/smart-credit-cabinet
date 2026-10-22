package com.hac.cabinet.service;

import com.hac.cabinet.dto.account.AccountResponse;
import com.hac.cabinet.dto.account.LoginRequest;
import com.hac.cabinet.dto.account.RegisterRequest;
import com.hac.cabinet.mapper.account.AccountMapper;
import com.hac.cabinet.model.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AccountService accountService;
  private final AccountMapper accountMapper;
  private final AuthenticationManager authenticationManager;

  public AccountResponse register(RegisterRequest request) {
    return accountMapper.toResponse(
        accountService.createIfNotExist(request));
  }

  public AccountResponse login(LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.login(), request.password())
    );
    return accountMapper.toResponse(accountService.getByLogin(request.login()));
  }
}

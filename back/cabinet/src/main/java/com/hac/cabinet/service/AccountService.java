package com.hac.cabinet.service;

import com.hac.cabinet.dto.account.AccountResponse;
import com.hac.cabinet.dto.account.RegisterRequest;
import com.hac.cabinet.dto.account.UpdateProfileRequest;
import com.hac.cabinet.mapper.account.AccountMapper;
import com.hac.cabinet.model.account.Account;
import com.hac.cabinet.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService implements UserDetailsService {

  private final AccountRepository accountRepository;
  private final AccountMapper accountMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Account createIfNotExist(RegisterRequest request) {
    if (accountRepository.existsByEmail(request.email())) {
      throw new RuntimeException("Account with email %s already exists".formatted(request.email()));
    }

    Account account = accountMapper.fromRegisterRequest(request);

    Instant now = Instant.now();
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
    account.setDeletedAt(null);
    account.setPasswordHash(passwordEncoder.encode(request.password()));

    return accountRepository.save(account);
  }

  @Transactional(readOnly = true)
  public Account getById(UUID id) {
    return accountRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Account not found: " + id));
  }

  public AccountResponse getResponseByLogin(String login) {
    return accountMapper.toResponse(getByLogin(login));
  }

  @Transactional(readOnly = true)
  public Account getByLogin(String login) {
    return accountRepository.findByLogin(login)
        .orElseThrow(() -> new RuntimeException("Account not found: " + login));
  }

  @Transactional(readOnly = true)
  public Account getByEmail(String email) {
    return accountRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Account not found: " + email));
  }

  @Transactional(readOnly = true)
  public List<Account> getAll() {
    return accountRepository.findAll();
  }

  @Transactional
  public Account update(Account account, UpdateProfileRequest request) {
    Account existing = getById(account.getId());

    existing.setLogin(request.login());
    existing.setEmail(request.email());
    existing.setUpdatedAt(Instant.now());

    return accountRepository.save(existing);
  }

  @Transactional
  public void softDelete(UUID id) {
    Account existing = getById(id);
    existing.setDeletedAt(Instant.now());
    existing.setUpdatedAt(Instant.now());
    accountRepository.save(existing);
  }

  @Transactional
  public void hardDelete(UUID id) {
    accountRepository.deleteById(id);
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return accountRepository.findByLogin(username)
        .orElseThrow(() -> new RuntimeException("Account not found: " + username));
  }
}

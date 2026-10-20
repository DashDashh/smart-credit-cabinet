package com.hac.cabinet.mapper.account;

import com.hac.cabinet.dto.account.AccountResponse;
import com.hac.cabinet.dto.account.RegisterRequest;
import com.hac.cabinet.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
  public Account fromRegisterRequest(RegisterRequest request) {
    Account account = new Account();
    account.setEmail(request.email());
    account.setLogin(request.login());
    account.setRole(request.role());
    return account;
  }

  public AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getLogin(),
        account.getEmail(),
        account.getRole(),
        account.getCreatedAt(),
        account.getUpdatedAt()
    );
  }
}

package com.hac.cabinet.repository;

import com.hac.cabinet.model.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
  Optional<Account> findByLogin(String login);
  boolean existsByEmail(String email);

  Optional<Account> findByEmail(String email);
}

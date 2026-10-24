package com.hac.cabinet.model.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Role {
  ADMIN(false, false, true),
  BANK_EMPLOYEE(false, true, false),
  ENTREPRENEUR(true, false, true);

  private final boolean canCreateApplication;
  private final boolean canReachVerdict;
  private final boolean canCloseApplication;

  public boolean isEntrepreneur() {
   return this.equals(ENTREPRENEUR);
  }
}

package com.hac.cabinet.model.account;

public enum Role {
  ADMIN,
  BANK_EMPLOYEE,
  ENTREPRENEUR;

  public boolean isEntrepreneur() {
   return this.equals(ENTREPRENEUR);
  }
}

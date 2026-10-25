package com.hac.cabinet.model.company;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanyType {
  IP("ip"),
  OOO("ooo");

  private final String stringExpression;
}

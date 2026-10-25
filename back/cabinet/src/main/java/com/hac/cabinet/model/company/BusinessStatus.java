package com.hac.cabinet.model.company;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessStatus {
  ACTIVE("active"),
  LIQUIDATING("liquidating"),
  CLOSED("closed");

  private final String stringExpression;
}

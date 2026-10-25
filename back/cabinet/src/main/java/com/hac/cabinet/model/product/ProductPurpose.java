package com.hac.cabinet.model.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductPurpose {
  INVEST("investment"),
  REVOLVING("working_capital"),
  MICRO("micro_loan");

  private final String stringExpression;
}

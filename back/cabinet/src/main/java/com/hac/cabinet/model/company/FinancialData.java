package com.hac.cabinet.model.company;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "financial_data")
public class FinancialData {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @DecimalMin("0")
  @Column(name = "revenue_monthly", nullable = false, precision = 19, scale = 2)
  private BigDecimal revenueMonthly;

  @NotNull
  @DecimalMin("0")
  @Column(name = "revenue_annual", nullable = false, precision = 19, scale = 2)
  private BigDecimal revenueAnnual;

  @NotNull
  @DecimalMin("0")
  @Column(name = "current_debt_payments", nullable = false, precision = 19, scale = 2)
  private BigDecimal currentDebtPayments;

  @NotNull
  @Column(name = "net_profit_monthly", nullable = false, precision = 19, scale = 2)
  private BigDecimal netProfitMonthly;

  @NotNull
  @DecimalMin("0")
  @Column(name = "accounts_payable", nullable = false, precision = 19, scale = 2)
  private BigDecimal accountsPayable;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false, unique = true,
      foreignKey = @ForeignKey(name = "fk_financial_data_company"))
  private Company company;
}

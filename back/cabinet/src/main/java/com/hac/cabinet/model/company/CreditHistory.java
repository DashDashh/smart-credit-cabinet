package com.hac.cabinet.model.company;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
@Table(name = "credit_history")
public class CreditHistory {

  @Id
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @Min(0)
  @Column(name = "delinquencies_12m", nullable = false)
  private int delinquencies12m;

  @NotNull
  @Min(0)
  @Column(name = "active_loans_count", nullable = false)
  private int activeLoansCount;

  @NotNull
  @DecimalMin("0")
  @Column(name = "total_debt_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal totalDebtAmount;

  @NotNull
  @Column(name = "tax_arrears", nullable = false)
  private boolean taxArrears;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false, unique = true,
      foreignKey = @ForeignKey(name = "fk_credit_history_company"))
  private Company company;
}

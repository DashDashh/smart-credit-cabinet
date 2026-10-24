package com.hac.cabinet.model.decision;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "decision_conditions")
public class DecisionConditions {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @DecimalMin("0")
  @Column(name = "approved_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal approvedAmount;

  @NotNull
  @Min(1)
  @Column(name = "term_months", nullable = false)
  private int termMonths;

  @NotNull
  @DecimalMin("0")
  @Column(name = "rate", nullable = false, precision = 9, scale = 4)
  private BigDecimal rate;

  @NotBlank
  @Column(name = "reason", nullable = false, length = 1000)
  private String reason;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "decision_id", nullable = false, unique = true,
      foreignKey = @ForeignKey(name = "fk_decision_conditions_decision"))
  private Decision decision;
}

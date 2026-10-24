package com.hac.cabinet.model.decision;

import com.hac.cabinet.model.application.Application;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@Table(name = "decision")
public class Decision {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotNull
  @DecimalMin("0")
  @DecimalMax("100")
  @Column(name = "score", nullable = false, precision = 5, scale = 1)
  private BigDecimal score;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "risk_level", length = 16, nullable = false)
  private RiskLevel riskLevel;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "decision", length = 32, nullable = false)
  private DecisionType decision;

  @Column(name = "rate", precision = 9, scale = 4)
  private BigDecimal rate;

  @NotBlank
  @Column(name = "next_step", length = 64, nullable = false)
  private String nextStep;

  @NotBlank
  @Column(name = "model_version", length = 64, nullable = false)
  private String modelVersion;

  @NotNull
  @Column(name = "fallback_used", nullable = false)
  private boolean fallbackUsed;

  @OneToOne(mappedBy = "decision", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private DecisionConditions conditions;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns(
      value = {
          @JoinColumn(name = "application_product_id", referencedColumnName = "product_id", nullable = false),
          @JoinColumn(name = "application_client_id", referencedColumnName = "client_id", nullable = false)
      },
      foreignKey = @ForeignKey(name = "fk_decision_application")
  )
  private Application application;
}

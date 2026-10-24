package com.hac.cabinet.model.application;

import com.hac.cabinet.model.company.Company;
import com.hac.cabinet.model.product.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "application")
public class Application {

  @EmbeddedId
  private ApplicationId id;

  @NotNull
  @DecimalMin("0")
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Min(1)
  @Column(name = "term_month", nullable = false)
  private int termMonth;

  @NotBlank
  @Size(max = 255)
  @Column(name = "purpose", nullable = false)
  private String purpose;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 32, nullable = false)
  private ApplicationStatus status;

  @MapsId("productId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_application_product"))
  private Product product;

  @MapsId("clientId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "client_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_application_client"))
  private Company client;
}

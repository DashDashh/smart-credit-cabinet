package com.hac.cabinet.model.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Table(name = "product")
public class Product {

  @Id
  @GeneratedValue
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @DecimalMin("0")
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @NotNull
  @Min(1)
  @Column(name = "term_months", nullable = false)
  private int termMonths;

  @NotNull
  @DecimalMin("0")
  @Column(name = "rate", nullable = false, precision = 9, scale = 4)
  private BigDecimal rate;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "purpose", length = 32, nullable = false)
  private ProductPurpose purpose;
}

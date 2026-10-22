package com.hac.cabinet.model.company;

import com.hac.cabinet.model.account.Account;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "company")
public class Company {
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  @UuidGenerator(style = UuidGenerator.Style.RANDOM)
  UUID id;

  @Column(name = "name", updatable = false, nullable = false)
  String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 32, updatable = false, nullable = false)
  CompanyType type;

  @Column(name = "industry", nullable = false)
  String industry;

  @Column(name = "founded_at", nullable = false, updatable = false)
  private LocalDate foundedAt;

  @Column(name = "employees_count", nullable = false)
  int employeesCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "business_status", length = 32, nullable = false)
  BusinessStatus status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false,
      foreignKey = @ForeignKey(name = "fk_company_owner"))
  private Account owner;

  @OneToOne(mappedBy = "company", fetch = FetchType.LAZY,
      cascade = CascadeType.ALL, orphanRemoval = true)
  private FinancialData financialData;

  @OneToOne(mappedBy = "company", fetch = FetchType.LAZY,
      cascade = CascadeType.ALL, orphanRemoval = true)
  private CreditHistory creditHistory;

  public void setCreditHistory(CreditHistory creditHistory) {
    if (creditHistory == null) {
      if (this.creditHistory != null) {
        this.creditHistory.setCompany(null);
      }
    } else {
      creditHistory.setCompany(this);
    }
    this.creditHistory = creditHistory;
  }

  public void setFinancialData(FinancialData financialData) {
    if (financialData == null) {
      if (this.financialData != null) {
        this.financialData.setCompany(null);
      }
    } else {
      financialData.setCompany(this);
    }
    this.financialData = financialData;
  }
}

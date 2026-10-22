from enum import Enum
from typing import Literal, Optional

from pydantic import BaseModel, Field



# ENUMS
class CompanyType(str, Enum):
    ip = "ip"
    ooo = "ooo"


class BusinessStatus(str, Enum):
    active = "active"
    liquidating = "liquidating"
    closed = "closed"


class RiskLevel(str, Enum):
    low = "low"
    medium = "medium"
    high = "high"


class DecisionType(str, Enum):
    approved = "approved"
    approved_with_conditions = "approved_with_conditions"
    needs_info = "needs_info"
    rejected = "rejected"
    manual_review = "manual_review"   # fallback, если ML недоступна


class ProductId(str, Enum):
    investment = "investment"
    working_capital = "working_capital"
    micro_loan = "micro_loan"


# ВХОД: КОМПАНИЯ
class Company(BaseModel):
    company_type: CompanyType
    industry: str = Field(
        description="ОКВЭД-отрасль: it, services, trade, manufacturing, construction, food"
    )
    age_months: int = Field(ge=0, description="Сколько месяцев существует бизнес")
    employees_count: int = Field(ge=0)
    business_status: BusinessStatus = BusinessStatus.active


# ВХОД: ФИНАНСОВЫЕ ДАННЫЕ
class Financials(BaseModel):
    revenue_monthly: float = Field(ge=0)
    revenue_annual: float = Field(ge=0)
    current_debt_payments: float = Field(ge=0, description="Платежи по кредитам в месяц")
    net_profit_monthly: float = Field(description="Может быть отрицательной")
    accounts_payable: float = Field(ge=0, description="Обязательства перед поставщиками")


# ВХОД: КРЕДИТНАЯ ИСТОРИЯ
class CreditHistory(BaseModel):
    delinquencies_12m: int = Field(ge=0)
    active_loans_count: int = Field(ge=0)
    total_debt_amount: float = Field(ge=0)
    tax_arrears: bool = False

# ВХОД: ЗАЯВКА
class Application(BaseModel):
    product_id: ProductId
    amount: float = Field(gt=0)
    term_months: int = Field(gt=0)
    purpose: str = Field(
        description="Цель: equipment, expansion, renovation, working_capital, trade, salary, marketing, repair"
    )


# ВХОД: ЗАПРОС ЦЕЛИКОМ
class DecisionRequest(BaseModel):
    application_id: str
    company: Company
    financials: Financials
    credit_history: CreditHistory
    application: Application


# ВЫХОД: ФАКТОРЫ (объяснимость)
class Factor(BaseModel):
    name: str = Field(description="Название фактора, например 'debt_load'")
    impact: float = Field(
        description="Вклад в скор: плюс — снижает риск, минус — повышает"
    )
    value: float | int | str = Field(description="Значение признака")


# ВЫХОД: УСЛОВИЯ (при approved_with_conditions)
class Conditions(BaseModel):
    approved_amount: float
    term_months: int
    rate: float
    reason: str


# ВЫХОД: РЕШЕНИЕ
class DecisionResponse(BaseModel):
    score: float = Field(ge=0, le=100, description="Скор: 100 — без риска, 0 — максимальный риск")
    risk_level: RiskLevel
    decision: DecisionType
    conditions: Optional[Conditions] = None
    factors: list[Factor]
    next_step: str = Field(
        description="client_accept_terms | client_accept_conditions | bank_request_info | notify_client_rejection | manual_review"
    )
    model_version: str
    fallback_used: bool = False
CREATE TABLE IF NOT EXISTS account (
    id UUID NOT NULL,
    login VARCHAR(32) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (id),
    CONSTRAINT uq_account_login UNIQUE (login),
    CONSTRAINT uq_account_email UNIQUE (email),
    CONSTRAINT chk_account_role CHECK (role IN ('ADMIN', 'BANK_EMPLOYEE', 'ENTREPRENEUR'))
);

CREATE TABLE IF NOT EXISTS company (
    id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    industry VARCHAR(255) NOT NULL,
    founded_at DATE NOT NULL,
    employees_count INT NOT NULL,
    business_status VARCHAR(32) NOT NULL,
    owner_id UUID NOT NULL,
    CONSTRAINT pk_company PRIMARY KEY (id),
    CONSTRAINT fk_company_owner FOREIGN KEY (owner_id) REFERENCES account (id),
    CONSTRAINT chk_company_employees_count CHECK (employees_count >= 0),
    CONSTRAINT chk_company_type CHECK (type IN ('IP', 'OOO')),
    CONSTRAINT chk_company_business_status CHECK (business_status IN ('ACTIVE', 'LIQUIDATING', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS financial_data (
    id UUID NOT NULL,
    revenue_monthly NUMERIC(19, 2) NOT NULL,
    revenue_annual NUMERIC(19, 2) NOT NULL,
    current_debt_payments NUMERIC(19, 2) NOT NULL,
    net_profit_monthly NUMERIC(19, 2) NOT NULL,
    accounts_payable NUMERIC(19, 2) NOT NULL,
    company_id UUID NOT NULL,
    CONSTRAINT pk_financial_data PRIMARY KEY (id),
    CONSTRAINT uq_financial_data_company UNIQUE (company_id),
    CONSTRAINT fk_financial_data_company FOREIGN KEY (company_id)
        REFERENCES company (id) ON DELETE CASCADE,
    CONSTRAINT chk_financial_data_revenue_monthly CHECK (revenue_monthly >= 0),
    CONSTRAINT chk_financial_data_revenue_annual CHECK (revenue_annual >= 0),
    CONSTRAINT chk_financial_data_current_debt_payments CHECK (current_debt_payments >= 0),
    CONSTRAINT chk_financial_data_accounts_payable CHECK (accounts_payable >= 0)
);

CREATE TABLE IF NOT EXISTS credit_history (
    id UUID NOT NULL,
    delinquencies_12m INT NOT NULL,
    active_loans_count INT NOT NULL,
    total_debt_amount NUMERIC(19, 2) NOT NULL,
    tax_arrears BOOLEAN NOT NULL,
    company_id UUID NOT NULL,
    CONSTRAINT pk_credit_history PRIMARY KEY (id),
    CONSTRAINT uq_credit_history_company UNIQUE (company_id),
    CONSTRAINT fk_credit_history_company FOREIGN KEY (company_id)
        REFERENCES company (id) ON DELETE CASCADE,
    CONSTRAINT chk_credit_history_delinquencies CHECK (delinquencies_12m >= 0),
    CONSTRAINT chk_credit_history_active_loans CHECK (active_loans_count >= 0),
    CONSTRAINT chk_credit_history_total_debt CHECK (total_debt_amount >= 0)
);
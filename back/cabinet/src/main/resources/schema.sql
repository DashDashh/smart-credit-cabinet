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

CREATE TABLE IF NOT EXISTS product (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    term_months INT NOT NULL,
    rate NUMERIC(9, 4) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT chk_product_amount CHECK (amount >= 0),
    CONSTRAINT chk_product_term_months CHECK (term_months >= 1),
    CONSTRAINT chk_product_rate CHECK (rate >= 0),
    CONSTRAINT chk_product_purpose CHECK (purpose IN ('INVEST', 'REVOLVING', 'MICRO'))
);

CREATE TABLE IF NOT EXISTS application (
    product_id UUID NOT NULL,
    client_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    term_month INT NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT pk_application PRIMARY KEY (product_id, client_id),
    CONSTRAINT fk_application_product FOREIGN KEY (product_id)
        REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT fk_application_client FOREIGN KEY (client_id)
        REFERENCES company (id) ON DELETE CASCADE,
    CONSTRAINT chk_application_amount CHECK (amount >= 0),
    CONSTRAINT chk_application_term_month CHECK (term_month >= 1),
    CONSTRAINT chk_application_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'ACTIVE', 'CLOSED'))
);

CREATE TABLE IF NOT EXISTS decision (
    id UUID NOT NULL,
    application_product_id UUID NOT NULL,
    application_client_id UUID NOT NULL,
    score NUMERIC(5, 1) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    rate NUMERIC(9, 4),
    next_step VARCHAR(64) NOT NULL,
    model_version VARCHAR(64) NOT NULL,
    fallback_used BOOLEAN NOT NULL,
    CONSTRAINT pk_decision PRIMARY KEY (id),
    CONSTRAINT fk_decision_application FOREIGN KEY (application_product_id, application_client_id)
        REFERENCES application (product_id, client_id) ON DELETE CASCADE,
    CONSTRAINT chk_decision_score CHECK (score >= 0 AND score <= 100),
    CONSTRAINT chk_decision_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_decision_decision CHECK (decision IN (
        'APPROVED', 'APPROVED_WITH_CONDITIONS', 'NEEDS_INFO', 'REJECTED', 'MANUAL_REVIEW'
    ))
);

CREATE TABLE IF NOT EXISTS decision_conditions (
    id UUID NOT NULL,
    approved_amount NUMERIC(19, 2) NOT NULL,
    term_months INT NOT NULL,
    rate NUMERIC(9, 4) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    decision_id UUID NOT NULL,
    CONSTRAINT pk_decision_conditions PRIMARY KEY (id),
    CONSTRAINT uq_decision_conditions_decision UNIQUE (decision_id),
    CONSTRAINT fk_decision_conditions_decision FOREIGN KEY (decision_id)
        REFERENCES decision (id) ON DELETE CASCADE,
    CONSTRAINT chk_decision_conditions_approved_amount CHECK (approved_amount >= 0),
    CONSTRAINT chk_decision_conditions_term_months CHECK (term_months >= 1),
    CONSTRAINT chk_decision_conditions_rate CHECK (rate >= 0)
);
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
    CONSTRAINT chk_account_role CHECK (role IN ('ADMIN', 'BANK', 'ENTREPRENEUR'))
);
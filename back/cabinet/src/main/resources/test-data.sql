-- ============================================================
-- TEST DATA для демо-сценариев (см. DEMO_SCENARIO.md)
-- Пароль всех аккаунтов: "password"
-- BCrypt hash: $2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi
-- ============================================================

-- ---------- ACCOUNTS ----------
INSERT INTO account (id, login, email, password_hash, created_at, updated_at, deleted_at, role) VALUES
('a0000000-0000-0000-0000-000000000001', 'bank_emp',       'bank.emp@cabinet.local',  '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'BANK_EMPLOYEE'),
('a0000000-0000-0000-0000-000000000002', 'technosoft',     'owner@technosoft.local',  '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'ENTREPRENEUR'),
('a0000000-0000-0000-0000-000000000003', 'ivanov_cafe',    'ivanov@cafe.local',       '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'ENTREPRENEUR'),
('a0000000-0000-0000-0000-000000000004', 'petrov_build',   'petrov@build.local',      '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'ENTREPRENEUR'),
('a0000000-0000-0000-0000-000000000005', 'sidorov_liq',    'sidorov@liq.local',       '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'ENTREPRENEUR'),
('a0000000-0000-0000-0000-000000000006', 'promtech',       'owner@promtech.local',    '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'ENTREPRENEUR'),
('a0000000-0000-0000-0000-000000000007', 'kuznetsov_info', 'kuznetsov@info.local',    '$2a$10$mEFyrbu6W5yO2BVE06ioq.PKFc0fTkz7b.uJABHT65nPeOYxCeKDi', now(), now(), NULL, 'ENTREPRENEUR');

-- ---------- COMPANIES ----------
INSERT INTO company (id, type, name, industry, founded_at, employees_count, business_status, owner_id) VALUES
('c0000000-0000-0000-0000-000000000001', 'OOO', 'ООО «ТехноСофт»',                'it',            (CURRENT_DATE - INTERVAL '3 years'),  120, 'ACTIVE',      'a0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000002', 'IP',  'ИП Иванов (кафе)',                'food',          (CURRENT_DATE - INTERVAL '9 months'),  12, 'ACTIVE',      'a0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000003', 'IP',  'ИП Петров (стройка)',             'construction',  (CURRENT_DATE - INTERVAL '8 months'),  35, 'ACTIVE',      'a0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000004', 'IP',  'ИП Сидоров (ликвидация)',         'services',      (CURRENT_DATE - INTERVAL '2 years'),    5, 'LIQUIDATING', 'a0000000-0000-0000-0000-000000000005'),
('c0000000-0000-0000-0000-000000000005', 'OOO', 'ООО «ПромТех»',                   'manufacturing', (CURRENT_DATE - INTERVAL '4 years'),  250, 'ACTIVE',      'a0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-000000000006', 'IP',  'ИП Кузнецов (неполные данные)',   'trade',         (CURRENT_DATE - INTERVAL '1 year'),     8, 'ACTIVE',      'a0000000-0000-0000-0000-000000000007');

-- ---------- FINANCIAL DATA ----------
INSERT INTO financial_data (id, revenue_monthly, revenue_annual, current_debt_payments, net_profit_monthly, accounts_payable, company_id) VALUES
('f0000000-0000-0000-0000-000000000001', 1500000.00, 18000000.00,  100000.00,  400000.00,  200000.00, 'c0000000-0000-0000-0000-000000000001'),
('f0000000-0000-0000-0000-000000000002',  350000.00,  4200000.00,  245000.00,   -5000.00,  300000.00, 'c0000000-0000-0000-0000-000000000002'),
('f0000000-0000-0000-0000-000000000003',  200000.00,  2400000.00,  190000.00,  -30000.00,  500000.00, 'c0000000-0000-0000-0000-000000000003'),
('f0000000-0000-0000-0000-000000000004',  100000.00,  1200000.00,   50000.00,   10000.00,  100000.00, 'c0000000-0000-0000-0000-000000000004'),
('f0000000-0000-0000-0000-000000000005', 5000000.00, 60000000.00,  350000.00, 1200000.00,  800000.00, 'c0000000-0000-0000-0000-000000000005'),
('f0000000-0000-0000-0000-000000000006',       0.00,         0.00,       0.00,       0.00,       0.00, 'c0000000-0000-0000-0000-000000000006');

-- ---------- CREDIT HISTORY ----------
INSERT INTO credit_history (id, delinquencies_12m, active_loans_count, total_debt_amount, tax_arrears, company_id) VALUES
('d0000000-0000-0000-0000-000000000001', 0, 1,   500000.00, false, 'c0000000-0000-0000-0000-000000000001'),
('d0000000-0000-0000-0000-000000000002', 2, 2,  1500000.00, true,  'c0000000-0000-0000-0000-000000000002'),
('d0000000-0000-0000-0000-000000000003', 3, 2,  1500000.00, true,  'c0000000-0000-0000-0000-000000000003'),
('d0000000-0000-0000-0000-000000000004', 0, 0,        0.00, false, 'c0000000-0000-0000-0000-000000000004'),
('d0000000-0000-0000-0000-000000000005', 0, 1,  2000000.00, false, 'c0000000-0000-0000-0000-000000000005'),
('d0000000-0000-0000-0000-000000000006', 2, 0,        0.00, false, 'c0000000-0000-0000-0000-000000000006');

-- ---------- PRODUCTS ----------
INSERT INTO product (id, name, amount, term_months, rate, purpose) VALUES
('b0000000-0000-0000-0000-000000000001', 'Оборотный кредит',      2000000.00, 12, 17.5000, 'REVOLVING'),
('b0000000-0000-0000-0000-000000000002', 'Инвестиционный кредит', 30000000.00, 36, 14.0000, 'INVEST'),
('b0000000-0000-0000-0000-000000000003', 'Микрокредит',             500000.00,  6, 24.0000, 'MICRO');

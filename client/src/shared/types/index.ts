export type AccountRole = 'ADMIN' | 'BANK_EMPLOYEE' | 'ENTREPRENEUR' | string;

export interface Account {
    id: string;
    login: string;
    email: string;
    role: AccountRole;
    createdAt: string;
    updatedAt: string;
}
export interface Session {
    account: Account;
    login: string;
    password: string;
}
export type AuthMode = 'login' | 'register';
export type ProductPurpose = 'INVEST' | 'REVOLVING' | 'MICRO';
export type ApplicationStatus =
    'PENDING' | 'APPROVED' | 'REJECTED' | 'ACTIVE' | 'CLOSED';
export interface Product {
    id: string;
    name: string;
    amount: number;
    termMonths: number;
    rate: number;
    purpose: ProductPurpose;
}
export interface CreditApplication {
    productId: string;
    clientId: string;
    amount: number;
    termMonth: number;
    purpose: string;
    status: ApplicationStatus;
}
export interface Company {
    id: string;
    name: string;
    type: string;
    industry: string;
    foundedAt: string | null;
    employeesCount: number;
    businessStatus: string;
    ownerId: string;
}
export interface FinancialData {
    revenueMonthly: number;
    revenueAnnual: number;
    currentDebtPayments: number;
    netProfitMonthly: number;
    accountsPayable: number;
}
export interface CreditHistory {
    delinquencies12m: number;
    activeLoansCount: number;
    totalDebtAmount: number;
    taxArrears: boolean;
    id?: string;
    companyId?: string;
    isApproved?: boolean;
}
export type DecisionType =
    | 'APPROVED'
    | 'APPROVED_WITH_CONDITIONS'
    | 'NEEDS_INFO'
    | 'REJECTED'
    | 'MANUAL_REVIEW';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export interface DecisionConditions {
    approvedAmount: number;
    termMonths: number;
    rate: number;
    reason: string;
}
export interface Decision {
    score: number;
    riskLevel: RiskLevel;
    decision: DecisionType;
    rate: number | null;
    nextStep: string;
    modelVersion: string;
    fallbackUsed: boolean;
    conditions: DecisionConditions | null;
}
export interface ApplicationWithDecision {
    applicationResponse: CreditApplication;
    decisionClientResponse: Decision | null;
}

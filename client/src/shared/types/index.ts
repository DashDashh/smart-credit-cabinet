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

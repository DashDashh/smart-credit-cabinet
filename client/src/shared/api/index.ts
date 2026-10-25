import type {
    Account,
    AuthMode,
    Company,
    CreditApplication,
    CreditHistory,
    FinancialData,
    ApplicationWithDecision,
    Product,
    Session,
} from '../types';

const API_URL = import.meta.env.VITE_API_URL || '/api';
const sessionKey = 'alpha-credit-session';

export class ApiError extends Error {
    readonly status: number;
    readonly errorId?: string;

    constructor(message: string, status: number, errorId?: string) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.errorId = errorId;
    }
}

interface BackendError {
    message?: string;
    errorId?: string;
}

const parseBackendError = (body: string): BackendError | null => {
    try {
        return body ? (JSON.parse(body) as BackendError) : null;
    } catch {
        return null;
    }
};

const backendMessageMap: Array<[string, string]> = [
    [
        'FinancialData.getRevenueMonthly',
        'Для подачи заявки сначала заполните финансовые показатели компании.',
    ],
    [
        'financialData is null',
        'Для подачи заявки сначала заполните финансовые показатели компании.',
    ],
    [
        'Company not found',
        'Компания не найдена. Обновите данные компании в личном кабинете.',
    ],
    [
        'Application already exists',
        'Заявка на этот кредитный продукт уже существует.',
    ],
];

export const getUserErrorMessage = (
    error: unknown,
    fallback: string,
): string => {
    if (!(error instanceof Error)) return fallback;
    const knownMessage = backendMessageMap.find(([fragment]) =>
        error.message.includes(fragment),
    )?.[1];
    if (knownMessage) {
        return error instanceof ApiError && error.errorId
            ? `${knownMessage} Код ошибки: ${error.errorId}`
            : knownMessage;
    }
    if (error instanceof ApiError && error.errorId) {
        return `${fallback} Код ошибки: ${error.errorId}`;
    }
    if (error.message === 'Failed to fetch') {
        return 'Не удалось связаться с сервером. Проверьте подключение и попробуйте ещё раз.';
    }
    return fallback;
};

export const normalizeRole = (role: string | null | undefined) =>
    (role ?? '')
        .trim()
        .toUpperCase()
        .replace(/[\s-]+/g, '_');

export const normalizeAccount = (account: Account): Account => ({
    ...account,
    role: normalizeRole(account.role),
});

async function request<T>(
    path: string,
    options: RequestInit = {},
    session?: Session,
): Promise<T> {
    const headers = new Headers(options.headers);
    headers.set('Content-Type', 'application/json');
    if (session)
        headers.set(
            'Authorization',
            `Basic ${btoa(`${session.login}:${session.password}`)}`,
        );
    const response = await fetch(`${API_URL}${path}`, { ...options, headers });
    if (!response.ok) {
        const body = await response.text();
        const parsed = parseBackendError(body);
        throw new ApiError(
            parsed?.message || body || `Ошибка запроса (${response.status})`,
            response.status,
            parsed?.errorId,
        );
    }
    return response.json() as Promise<T>;
}

export const api = {
    authenticate: async (
        mode: AuthMode,
        data: { login: string; password: string; email?: string },
    ): Promise<Session> => {
        const account = await request<Account>(
            `/auth/${mode === 'login' ? 'login' : 'register'}`,
            {
                method: 'POST',
                body: JSON.stringify(
                    mode === 'login'
                        ? { login: data.login, password: data.password }
                        : { ...data, role: 'ENTREPRENEUR' },
                ),
            },
        );
        return {
            account: normalizeAccount(account),
            login: data.login,
            password: data.password,
        };
    },
    me: async (session: Session) =>
        normalizeAccount(await request<Account>('/accounts/me', {}, session)),
    products: {
        list: (session: Session) =>
            request<Product[]>('/products', {}, session),
        create: (session: Session, data: Omit<Product, 'id'>) =>
            request<Product>(
                '/products',
                { method: 'POST', body: JSON.stringify(data) },
                session,
            ),
        update: (session: Session, id: string, data: Omit<Product, 'id'>) =>
            request<Product>(
                `/products/${id}`,
                { method: 'PUT', body: JSON.stringify(data) },
                session,
            ),
    },
    applications: {
        list: (session: Session) =>
            request<CreditApplication[]>('/applications', {}, session),
        byCompany: (session: Session, companyId: string) =>
            request<CreditApplication[]>(
                `/applications/${companyId}`,
                {},
                session,
            ),
        create: (session: Session, data: Omit<CreditApplication, 'status'>) =>
            request<CreditApplication>(
                '/applications',
                { method: 'POST', body: JSON.stringify(data) },
                session,
            ),
        update: (
            session: Session,
            productId: string,
            clientId: string,
            data: Omit<CreditApplication, 'productId' | 'clientId' | 'status'>,
        ) =>
            request<CreditApplication>(
                `/applications/${productId}/${clientId}`,
                { method: 'PUT', body: JSON.stringify(data) },
                session,
            ),
        changeStatus: (
            session: Session,
            productId: string,
            clientId: string,
            status: CreditApplication['status'],
        ) =>
            request<CreditApplication>(
                `/applications/${productId}/${clientId}/status`,
                { method: 'PATCH', body: JSON.stringify({ status }) },
                session,
            ),
        withDecision: (session: Session, productId: string, clientId: string) =>
            request<ApplicationWithDecision>(
                `/applications/${productId}/${clientId}/withDecision`,
                {},
                session,
            ),
    },
    companies: {
        mine: (session: Session) =>
            request<Company[]>('/companies/my', {}, session),
        create: (session: Session, data: Omit<Company, 'id' | 'ownerId'>) =>
            request<Company>(
                '/companies',
                { method: 'POST', body: JSON.stringify(data) },
                session,
            ),
        update: (
            session: Session,
            id: string,
            data: Omit<Company, 'id' | 'type' | 'ownerId'>,
        ) =>
            request<Company>(
                `/companies/${id}`,
                { method: 'PUT', body: JSON.stringify(data) },
                session,
            ),
        financialData: (session: Session, companyId: string) =>
            request<FinancialData>(
                `/companies/${companyId}/financial-data`,
                {},
                session,
            ),
        createFinancialData: (
            session: Session,
            companyId: string,
            data: Omit<FinancialData, 'id' | 'companyId'>,
        ) =>
            request<FinancialData>(
                `/companies/${companyId}/financial-data`,
                { method: 'POST', body: JSON.stringify(data) },
                session,
            ),
        updateFinancialData: (
            session: Session,
            companyId: string,
            data: Omit<FinancialData, 'id' | 'companyId'>,
        ) =>
            request<FinancialData>(
                `/companies/${companyId}/financial-data`,
                { method: 'PUT', body: JSON.stringify(data) },
                session,
            ),
        creditHistory: (session: Session, companyId: string) =>
            request<CreditHistory>(
                `/companies/${companyId}/credit-history`,
                {},
                session,
            ),
        createCreditHistory: (
            session: Session,
            companyId: string,
            data: Omit<CreditHistory, 'id' | 'companyId' | 'isApproved'>,
        ) =>
            request<CreditHistory>(
                `/companies/${companyId}/credit-history`,
                { method: 'POST', body: JSON.stringify(data) },
                session,
            ),
        updateCreditHistory: (
            session: Session,
            companyId: string,
            data: Omit<CreditHistory, 'id' | 'companyId' | 'isApproved'>,
        ) =>
            request<CreditHistory>(
                `/companies/${companyId}/credit-history`,
                { method: 'PUT', body: JSON.stringify(data) },
                session,
            ),
    },
};
export const getStoredSession = (): Session | null => {
    try {
        const value = localStorage.getItem(sessionKey);
        if (!value) return null;
        const session = JSON.parse(value) as Session;
        return { ...session, account: normalizeAccount(session.account) };
    } catch {
        return null;
    }
};
export const saveSession = (session: Session) =>
    localStorage.setItem(sessionKey, JSON.stringify(session));
export const clearSession = () => localStorage.removeItem(sessionKey);

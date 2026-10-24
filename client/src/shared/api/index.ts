import type { Account, AuthMode, Session } from '../types';

const API_URL = import.meta.env.VITE_API_URL || '/api';
const sessionKey = 'alpha-credit-session';

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
        throw new Error(body || `Ошибка запроса (${response.status})`);
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

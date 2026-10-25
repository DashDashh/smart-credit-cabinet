import { useEffect, useState } from 'react';
import {
    Navigate,
    Outlet,
    Route,
    Routes,
    useLocation,
    useNavigate,
} from 'react-router-dom';
import {
    api,
    clearSession,
    getStoredSession,
    getUserErrorMessage,
    normalizeRole,
    saveSession,
} from './shared/api';
import type { Account, AuthMode, Session } from './shared/types';
import { AuthPage } from './pages/AuthPage';
import { CatalogPage } from './pages/CatalogPage';
import { DashboardPage } from './pages/DashboardPage';
import { ApplicationsPage } from './pages/ApplicationsPage';
import './App.css';

function App() {
    const [session, setSession] = useState<Session | null>(getStoredSession);
    const [account, setAccount] = useState<Account | null>(null);
    const [authMode, setAuthMode] = useState<AuthMode>('login');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (!session) return;
        api.me(session)
            .then(setAccount)
            .catch(() => {
                clearSession();
                setSession(null);
                setAccount(null);
            });
    }, [session]);

    const authenticate = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setLoading(true);
        setMessage('');
        const form = new FormData(event.currentTarget);
        try {
            const next = await api.authenticate(authMode, {
                login: String(form.get('login') || ''),
                password: String(form.get('password') || ''),
                email: String(form.get('email') || ''),
            });
            saveSession(next);
            setSession(next);
            setAccount(next.account);
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось выполнить запрос. Проверьте данные и попробуйте ещё раз.',
                ),
            );
        } finally {
            setLoading(false);
        }
    };

    if (!session || !account) {
        return (
            <Routes>
                <Route
                    path='/register'
                    element={
                        <AuthPage
                            mode='register'
                            setMode={setAuthMode}
                            onSubmit={authenticate}
                            loading={loading}
                            message={message}
                        />
                    }
                />
                <Route
                    path='*'
                    element={
                        <AuthPage
                            mode={authMode}
                            setMode={setAuthMode}
                            onSubmit={authenticate}
                            loading={loading}
                            message={message}
                        />
                    }
                />
            </Routes>
        );
    }

    return (
        <Routes>
            <Route
                element={
                    <ProtectedLayout
                        account={account}
                        onLogout={() => {
                            clearSession();
                            setSession(null);
                            setAccount(null);
                        }}
                    />
                }
            >
                <Route index element={<Navigate to='/profile' replace />} />
                <Route
                    path='/profile'
                    element={
                        normalizeRole(account.role) === 'BANK_EMPLOYEE' ? (
                            <ApplicationsPage session={session} />
                        ) : (
                            <DashboardPage
                                account={account}
                                session={session}
                            />
                        )
                    }
                />
                <Route
                    path='/catalog'
                    element={
                        <CatalogPage account={account} session={session} />
                    }
                />
                <Route path='*' element={<Navigate to='/profile' replace />} />
            </Route>
        </Routes>
    );
}

function ProtectedLayout({
    account,
    onLogout,
}: {
    account: Account;
    onLogout: () => void;
}) {
    const navigate = useNavigate();
    const currentLocation = useLocation();
    const isEmployee = normalizeRole(account.role) === 'BANK_EMPLOYEE';
    return (
        <div className='app-shell'>
            <header className='topbar'>
                <button
                    className='brand'
                    onClick={() => navigate('/profile')}
                    aria-label='На главную'
                >
                    <span className='brand-mark'>a</span>
                    <span>
                        Альфа<span className='brand-light'>Банк</span>
                    </span>
                </button>
                <nav>
                    <button
                        className={
                            currentLocation.pathname === '/profile'
                                ? 'nav-link active'
                                : 'nav-link'
                        }
                        onClick={() => navigate('/profile')}
                    >
                        {isEmployee ? 'Заявки' : 'Мой кабинет'}
                    </button>
                    <button
                        className={
                            currentLocation.pathname === '/catalog'
                                ? 'nav-link active'
                                : 'nav-link'
                        }
                        onClick={() => navigate('/catalog')}
                    >
                        Кредиты
                    </button>
                </nav>
                <div className='profile-menu'>
                    <span className='avatar'>
                        {account.login.slice(0, 1).toUpperCase()}
                    </span>
                    <span>{account.login}</span>
                    <button className='logout' onClick={onLogout}>
                        Выйти
                    </button>
                </div>
            </header>
            <main className='main-content'>
                <Outlet />
            </main>
            <footer className='footer'>
                <span>© 2026 Альфа-Банк</span>
                <span>Поддержка 8 800 200 00 00</span>
                <span>Всё под контролем</span>
            </footer>
        </div>
    );
}

export default App;

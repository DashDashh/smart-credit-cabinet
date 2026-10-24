import type { AuthMode } from '../shared/types';

export function AuthPage({
    mode,
    setMode,
    onSubmit,
    loading,
    message,
}: {
    mode: AuthMode;
    setMode: (mode: AuthMode) => void;
    onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
    loading: boolean;
    message: string;
}) {
    return (
        <div className='auth-layout'>
            <section className='auth-promo'>
                <button className='brand brand-white'>
                    <span className='brand-mark'>a</span>
                    <span>
                        Альфа<span className='brand-light'>Банк</span>
                    </span>
                </button>
                <div className='promo-copy'>
                    <span className='eyebrow'>Умный кредитный кабинет</span>
                    <h1>
                        Деньги должны
                        <br />
                        <em>работать</em> на вас
                    </h1>
                    <p>
                        Управляйте кредитами, платежами и планами в одном месте.
                    </p>
                </div>
                <div className='promo-card'>
                    <span>Ваши финансы</span>
                    <strong>под защитой</strong>
                    <span className='sparkle'>✦</span>
                </div>
            </section>
            <section className='auth-panel'>
                <div className='auth-form-wrap'>
                    <div className='mobile-brand'>
                        <span className='brand-mark'>a</span> Альфа
                        <span>Банк</span>
                    </div>
                    <span className='eyebrow'>Добро пожаловать</span>
                    <h2>
                        {mode === 'login'
                            ? 'Войти в кабинет'
                            : 'Создать кабинет'}
                    </h2>
                    <p className='muted'>
                        {mode === 'login'
                            ? 'Введите данные для входа'
                            : 'Это займёт меньше минуты'}
                    </p>
                    <div className='tabs'>
                        <button
                            className={mode === 'login' ? 'tab active' : 'tab'}
                            onClick={() => setMode('login')}
                        >
                            Вход
                        </button>
                        <button
                            className={
                                mode === 'register' ? 'tab active' : 'tab'
                            }
                            onClick={() => setMode('register')}
                        >
                            Регистрация
                        </button>
                    </div>
                    <form onSubmit={onSubmit}>
                        <label>
                            Логин
                            <input
                                name='login'
                                required
                                minLength={3}
                                placeholder='Например, alexey'
                            />
                        </label>
                        {mode === 'register' && (
                            <label>
                                Email
                                <input
                                    type='email'
                                    name='email'
                                    required
                                    placeholder='name@example.com'
                                />
                            </label>
                        )}
                        <label>
                            Пароль
                            <input
                                type='password'
                                name='password'
                                required
                                minLength={8}
                                placeholder='Не менее 8 символов'
                            />
                        </label>
                        {message && <div className='error'>{message}</div>}
                        <button className='primary-button' disabled={loading}>
                            {loading
                                ? 'Подождите…'
                                : mode === 'login'
                                  ? 'Войти'
                                  : 'Зарегистрироваться'}{' '}
                            <span>→</span>
                        </button>
                    </form>
                    <p className='legal'>
                        Продолжая, вы соглашаетесь с условиями использования
                        сервиса
                    </p>
                </div>
            </section>
        </div>
    );
}

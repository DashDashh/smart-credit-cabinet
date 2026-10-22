import type { Account } from '../shared/types';
import { useNavigate } from 'react-router-dom';

export function DashboardPage({ account }: { account: Account }) {
    const navigate = useNavigate();
    return (
        <>
            <div className='page-heading'>
                <div>
                    <span className='eyebrow'>Личный кабинет</span>
                    <h1>
                        Добрый день, {account.login}{' '}
                        <span className='wave'>✦</span>
                    </h1>
                    <p className='muted'>Здесь всё о ваших финансах</p>
                </div>
                <button
                    className='primary-button compact'
                    onClick={() => navigate('/catalog')}
                >
                    Подобрать кредит <span>→</span>
                </button>
            </div>
            <section className='dashboard-grid'>
                <div className='balance-card'>
                    <div className='card-top'>
                        <span>Доступный лимит</span>
                        <span className='card-dots'>•••</span>
                    </div>
                    <strong>850 000 ₽</strong>
                    <div className='limit-line'>
                        <span style={{ width: '64%' }} />
                    </div>
                    <div className='card-bottom'>
                        <span>Использовано 150 000 ₽</span>
                        <span>1 000 000 ₽</span>
                    </div>
                </div>
                <Stat
                    icon='↗'
                    label='Кредитный рейтинг'
                    value='Высокий'
                    note='Всё хорошо'
                    tone='green'
                />
                <Stat
                    icon='₽'
                    label='Ближайший платёж'
                    value='18 450 ₽'
                    note='до 25 июня 2026'
                    tone='purple'
                />
            </section>
            <section className='section-block'>
                <div className='section-title'>
                    <div>
                        <h2>Последние операции</h2>
                        <p className='muted'>Ваши движения по счетам</p>
                    </div>
                    <button className='text-button'>Все операции →</button>
                </div>
                <div className='operations'>
                    <Operation
                        icon='↗'
                        title='Пополнение счёта'
                        date='Сегодня, 12:40'
                        amount='+ 25 000 ₽'
                        positive
                    />
                    <Operation
                        icon='▣'
                        title='Платёж по кредиту'
                        date='Вчера, 09:15'
                        amount='− 18 450 ₽'
                    />
                    <Operation
                        icon='⌂'
                        title='Перевод на карту'
                        date='12 июня 2026'
                        amount='− 5 000 ₽'
                    />
                </div>
            </section>
            <section className='recommendation'>
                <div>
                    <span className='eyebrow'>Специально для вас</span>
                    <h2>Мечты ближе, чем кажется</h2>
                    <p>
                        Рассчитайте комфортный платёж и найдите подходящий
                        кредит.
                    </p>
                    <button
                        className='dark-button'
                        onClick={() => navigate('/catalog')}
                    >
                        Смотреть предложения
                    </button>
                </div>
                <div className='abstract-shape'>✦</div>
            </section>
        </>
    );
}

function Stat({
    icon,
    label,
    value,
    note,
    tone,
}: {
    icon: string;
    label: string;
    value: string;
    note: string;
    tone: string;
}) {
    return (
        <div className='stat-card'>
            <span className={`stat-icon ${tone}`}>{icon}</span>
            <span className='muted'>{label}</span>
            <strong>{value}</strong>
            <small>{note}</small>
        </div>
    );
}

function Operation({
    icon,
    title,
    date,
    amount,
    positive = false,
}: {
    icon: string;
    title: string;
    date: string;
    amount: string;
    positive?: boolean;
}) {
    return (
        <div className='operation'>
            <span className='operation-icon'>{icon}</span>
            <div>
                <strong>{title}</strong>
                <small>{date}</small>
            </div>
            <b className={positive ? 'positive' : ''}>{amount}</b>
        </div>
    );
}

const applications = [
    {
        name: 'Алексей Смирнов',
        type: 'Кредит наличными',
        amount: '850 000 ₽',
        date: 'Сегодня, 10:42',
        status: 'Новая',
        tone: 'new',
    },
    {
        name: 'Мария Волкова',
        type: 'Рефинансирование',
        amount: '1 200 000 ₽',
        date: 'Сегодня, 09:18',
        status: 'На проверке',
        tone: 'review',
    },
    {
        name: 'Дмитрий Кузнецов',
        type: 'Кредит наличными',
        amount: '450 000 ₽',
        date: 'Вчера, 16:35',
        status: 'Одобрена',
        tone: 'approved',
    },
    {
        name: 'Ольга Морозова',
        type: 'Ипотека на новостройку',
        amount: '6 500 000 ₽',
        date: 'Вчера, 14:07',
        status: 'Новая',
        tone: 'new',
    },
];

export function ApplicationsPage() {
    return (
        <>
            <div className='page-heading'>
                <div>
                    <span className='eyebrow'>Рабочий кабинет</span>
                    <h1>Заявки на кредит</h1>
                    <p className='muted'>
                        Заявки клиентов, направленные вам на рассмотрение
                    </p>
                </div>
                <div className='application-counter'>
                    <strong>4</strong>
                    <span>активные заявки</span>
                </div>
            </div>
            <section className='employee-summary'>
                <div>
                    <span className='stat-icon purple'>▣</span>
                    <span className='muted'>Всего заявок</span>
                    <strong>24</strong>
                </div>
                <div>
                    <span className='stat-icon green'>◷</span>
                    <span className='muted'>На проверке</span>
                    <strong>8</strong>
                </div>
                <div>
                    <span className='stat-icon orange'>✓</span>
                    <span className='muted'>Одобрено в этом месяце</span>
                    <strong>12</strong>
                </div>
            </section>
            <section className='section-block applications-section'>
                <div className='section-title'>
                    <div>
                        <h2>Входящие заявки</h2>
                        <p className='muted'>Обновлено сегодня в 12:40</p>
                    </div>
                    <button className='outline-button filter-button'>
                        Все статусы⌄
                    </button>
                </div>
                <div className='application-list'>
                    {applications.map((application) => (
                        <article
                            className='application-row'
                            key={`${application.name}-${application.date}`}
                        >
                            <span className='app-avatar'>
                                {application.name
                                    .split(' ')
                                    .map((part) => part[0])
                                    .join('')}
                            </span>
                            <div className='application-client'>
                                <strong>{application.name}</strong>
                                <small>
                                    {application.type} · {application.date}
                                </small>
                            </div>
                            <strong className='application-amount'>
                                {application.amount}
                            </strong>
                            <span
                                className={`application-status ${application.tone}`}
                            >
                                {application.status}
                            </span>
                            <button className='application-action'>
                                Открыть →
                            </button>
                        </article>
                    ))}
                </div>
            </section>
        </>
    );
}

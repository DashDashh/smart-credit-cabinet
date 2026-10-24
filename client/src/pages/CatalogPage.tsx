const credits = [
    {
        icon: '◒',
        title: 'Кредит наличными',
        description: 'На любые цели без залога',
        amount: 'до 5 000 000 ₽',
        rate: 'от 7,9%',
        term: 'до 7 лет',
        accent: 'blue',
    },
    {
        icon: '▣',
        title: 'Рефинансирование',
        description: 'Объедините кредиты в один',
        amount: 'до 3 000 000 ₽',
        rate: 'от 8,8%',
        term: 'до 5 лет',
        accent: 'violet',
    },
    {
        icon: '⌂',
        title: 'Ипотека на новостройку',
        description: 'Ваша квартира уже ближе',
        amount: 'до 30 000 000 ₽',
        rate: 'от 6%',
        term: 'до 30 лет',
        accent: 'orange',
    },
];

export function CatalogPage() {
    return (
        <>
            <div className='page-heading catalog-heading'>
                <div>
                    <span className='eyebrow'>Финансовые решения</span>
                    <h1>Выберите свой кредит</h1>
                    <p className='muted'>Простые условия и понятные платежи</p>
                </div>
                <div className='catalog-note'>
                    ✦ Индивидуальный подбор
                    <br />
                    <span>за 2 минуты</span>
                </div>
            </div>
            <div className='credit-list'>
                {credits.map((credit) => (
                    <article className='credit-card' key={credit.title}>
                        <div className={`credit-icon ${credit.accent}`}>
                            {credit.icon}
                        </div>
                        <h2>{credit.title}</h2>
                        <p>{credit.description}</p>
                        <div className='credit-details'>
                            <div>
                                <small>Сумма</small>
                                <strong>{credit.amount}</strong>
                            </div>
                            <div>
                                <small>Ставка</small>
                                <strong>{credit.rate}</strong>
                            </div>
                            <div>
                                <small>Срок</small>
                                <strong>{credit.term}</strong>
                            </div>
                        </div>
                        <button className='outline-button'>
                            Подробнее <span>→</span>
                        </button>
                    </article>
                ))}
            </div>
            <div className='calculator-banner'>
                <div>
                    <span className='eyebrow'>Не знаете, что выбрать?</span>
                    <h2>Рассчитайте свой комфортный платёж</h2>
                </div>
                <button className='primary-button compact'>
                    Рассчитать <span>→</span>
                </button>
            </div>
        </>
    );
}

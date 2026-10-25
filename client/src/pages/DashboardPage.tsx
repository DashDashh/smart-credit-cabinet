import { useEffect, useState } from 'react';
import type {
    Account,
    Company,
    CreditApplication,
    CreditHistory,
    FinancialData,
    Session,
} from '../shared/types';
import { api, getUserErrorMessage } from '../shared/api';
import { useNavigate } from 'react-router-dom';

export function DashboardPage({
    account,
    session,
}: {
    account: Account;
    session: Session;
}) {
    const navigate = useNavigate();
    const [company, setCompany] = useState<Company | null>(null);
    const [financialData, setFinancialData] = useState<FinancialData | null>(
        null,
    );
    const [creditHistory, setCreditHistory] = useState<CreditHistory | null>(
        null,
    );
    const [message, setMessage] = useState('');
    const [companyEditorOpen, setCompanyEditorOpen] = useState(false);
    const [financialEditorOpen, setFinancialEditorOpen] = useState(false);
    const [historyEditorOpen, setHistoryEditorOpen] = useState(false);
    const [applications, setApplications] = useState<CreditApplication[]>([]);
    const [companyForm, setCompanyForm] = useState({
        name: '',
        type: 'IP',
        industry: '',
        foundedAt: '',
        employeesCount: '0',
        businessStatus: 'ACTIVE',
    });
    const [financialForm, setFinancialForm] = useState({
        revenueMonthly: '',
        revenueAnnual: '',
        currentDebtPayments: '',
        netProfitMonthly: '',
        accountsPayable: '',
    });
    const [historyForm, setHistoryForm] = useState({
        delinquencies12m: '0',
        activeLoansCount: '0',
        totalDebtAmount: '0',
        taxArrears: false,
    });

    useEffect(() => {
        api.companies
            .mine(session)
            .then(async (companies) => {
                const firstCompany = companies[0];
                setCompany(firstCompany || null);
                if (firstCompany) {
                    setCompanyForm({
                        name: firstCompany.name || '',
                        type: firstCompany.type,
                        industry: firstCompany.industry,
                        foundedAt: firstCompany.foundedAt || '',
                        employeesCount: String(firstCompany.employeesCount),
                        businessStatus: firstCompany.businessStatus,
                    });
                    const companyApplications =
                        await api.applications.byCompany(
                            session,
                            firstCompany.id,
                        );
                    setApplications(companyApplications);
                }
                if (!firstCompany) return;
                const results = await Promise.allSettled([
                    api.companies.financialData(session, firstCompany.id),
                    api.companies.creditHistory(session, firstCompany.id),
                ]);
                if (results[0].status === 'fulfilled') {
                    setFinancialData(results[0].value);
                    setFinancialForm({
                        ...results[0].value,
                        revenueMonthly: String(results[0].value.revenueMonthly),
                        revenueAnnual: String(results[0].value.revenueAnnual),
                        currentDebtPayments: String(
                            results[0].value.currentDebtPayments,
                        ),
                        netProfitMonthly: String(
                            results[0].value.netProfitMonthly,
                        ),
                        accountsPayable: String(
                            results[0].value.accountsPayable,
                        ),
                    });
                }
                if (results[1].status === 'fulfilled') {
                    setCreditHistory(results[1].value);
                    setHistoryForm({
                        delinquencies12m: String(
                            results[1].value.delinquencies12m,
                        ),
                        activeLoansCount: String(
                            results[1].value.activeLoansCount,
                        ),
                        totalDebtAmount: String(
                            results[1].value.totalDebtAmount,
                        ),
                        taxArrears: results[1].value.taxArrears,
                    });
                }
            })
            .catch((error: unknown) =>
                setMessage(
                    getUserErrorMessage(
                        error,
                        'Не удалось загрузить данные компании. Попробуйте обновить страницу.',
                    ),
                ),
            );
    }, [session]);

    const saveCompany = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        try {
            const data = {
                name: companyForm.name,
                industry: companyForm.industry,
                foundedAt: companyForm.foundedAt || null,
                employeesCount: Number(companyForm.employeesCount),
                businessStatus: companyForm.businessStatus,
            };
            const saved = company
                ? await api.companies.update(session, company.id, data)
                : await api.companies.create(session, {
                      ...data,
                      type: companyForm.type,
                  });
            setCompany(saved);
            setCompanyEditorOpen(false);
            setMessage('Данные компании сохранены');
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось сохранить компанию. Проверьте заполненные поля.',
                ),
            );
        }
    };

    const saveFinancialData = async (
        event: React.FormEvent<HTMLFormElement>,
    ) => {
        event.preventDefault();
        if (!company) return;
        const data = Object.fromEntries(
            Object.entries(financialForm).map(([key, value]) => [
                key,
                Number(value),
            ]),
        ) as Omit<FinancialData, 'id' | 'companyId'>;
        try {
            const saved = financialData
                ? await api.companies.updateFinancialData(
                      session,
                      company.id,
                      data,
                  )
                : await api.companies.createFinancialData(
                      session,
                      company.id,
                      data,
                  );
            setFinancialData(saved);
            setFinancialEditorOpen(false);
            setMessage('Финансовые данные сохранены');
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось сохранить финансовые данные. Проверьте значения полей.',
                ),
            );
        }
    };

    const saveCreditHistory = async (
        event: React.FormEvent<HTMLFormElement>,
    ) => {
        event.preventDefault();
        if (!company) return;
        const data = {
            ...historyForm,
            delinquencies12m: Number(historyForm.delinquencies12m),
            activeLoansCount: Number(historyForm.activeLoansCount),
            totalDebtAmount: Number(historyForm.totalDebtAmount),
        };
        try {
            const saved = creditHistory
                ? await api.companies.updateCreditHistory(
                      session,
                      company.id,
                      data,
                  )
                : await api.companies.createCreditHistory(
                      session,
                      company.id,
                      data,
                  );
            setCreditHistory(saved);
            setHistoryEditorOpen(false);
            setMessage('Кредитная история сохранена');
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось сохранить кредитную историю. Проверьте значения полей.',
                ),
            );
        }
    };

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
            {message && <div className='notice'>{message}</div>}
            <section className='dashboard-grid'>
                <Stat
                    icon='↗'
                    label='Кредитный рейтинг'
                    value={
                        creditHistory ? 'Подтверждается' : 'Заполните данные'
                    }
                    note={
                        creditHistory
                            ? 'История загружена'
                            : 'Добавьте историю компании'
                    }
                    tone='green'
                />
                <Stat
                    icon='₽'
                    label='Финансовые данные'
                    value={financialData ? 'Заполнены' : 'Не заполнены'}
                    note={company?.name || 'Добавьте компанию'}
                    tone='purple'
                />
                <Stat
                    icon='▣'
                    label='Заявки'
                    value={String(applications.length)}
                    note='Данные из системы банка'
                    tone='orange'
                />
            </section>
            <section className='section-block'>
                <div className='section-title'>
                    <div>
                        <h2>Профиль компании</h2>
                        <p className='muted'>
                            Данные, которые банк использует для оценки заявки
                        </p>
                    </div>
                    <button
                        className='outline-button filter-button'
                        onClick={() => setCompanyEditorOpen(true)}
                    >
                        {company ? 'Изменить данные' : 'Добавить компанию'}
                    </button>
                </div>
                <div className='company-card'>
                    {company ? (
                        <>
                            <div>
                                <span className='company-avatar'>⌂</span>
                                <div>
                                    <strong>
                                        {company.name || 'Без названия'}
                                    </strong>
                                    <small>
                                        {company.industry} ·{' '}
                                        {company.employeesCount} сотрудников
                                    </small>
                                </div>
                            </div>
                            <span className='verified-badge'>
                                Данные указаны
                            </span>
                        </>
                    ) : (
                        <div className='empty-state'>
                            Компания ещё не добавлена. Добавьте её перед подачей
                            заявки.
                        </div>
                    )}
                </div>
            </section>
            {company && (
                <section className='section-block data-panels'>
                    <DataPanel
                        title='Финансовые показатели'
                        ready={Boolean(financialData)}
                        onEdit={() => setFinancialEditorOpen(true)}
                    >
                        <span>
                            Выручка в месяц:{' '}
                            {financialData
                                ? `${financialData.revenueMonthly.toLocaleString('ru-RU')} ₽`
                                : 'не заполнено'}
                        </span>
                        <span>
                            Чистая прибыль:{' '}
                            {financialData
                                ? `${financialData.netProfitMonthly.toLocaleString('ru-RU')} ₽`
                                : 'не заполнено'}
                        </span>
                    </DataPanel>
                    <DataPanel
                        title='Кредитная история'
                        ready={Boolean(creditHistory)}
                        onEdit={() => setHistoryEditorOpen(true)}
                    >
                        <span>
                            Активных кредитов:{' '}
                            {creditHistory?.activeLoansCount ?? 'не заполнено'}
                        </span>
                        <span>
                            Просрочек за 12 месяцев:{' '}
                            {creditHistory?.delinquencies12m ?? 'не заполнено'}
                        </span>
                    </DataPanel>
                </section>
            )}
            {company && (
                <section className='section-block'>
                    <div className='section-title'>
                        <div>
                            <h2>Мои заявки</h2>
                            <p className='muted'>
                                Статусы отправленных заявок на кредит
                            </p>
                        </div>
                    </div>
                    <div className='operations'>
                        {applications.map((application) => (
                            <div
                                className='operation'
                                key={`${application.productId}-${application.clientId}`}
                            >
                                <span className='operation-icon'>▣</span>
                                <div>
                                    <strong>{application.purpose}</strong>
                                    <small>
                                        {application.termMonth} месяцев
                                    </small>
                                </div>
                                <b>
                                    {application.amount.toLocaleString('ru-RU')}{' '}
                                    ₽
                                </b>
                                <span
                                    className={`application-status ${application.status.toLowerCase()}`}
                                >
                                    {application.status}
                                </span>
                            </div>
                        ))}
                        {!applications.length && (
                            <div className='empty-state'>
                                Вы ещё не отправляли заявки.
                            </div>
                        )}
                    </div>
                </section>
            )}
            <section className='recommendation'>
                <div>
                    <span className='eyebrow'>Следующий шаг</span>
                    <h2>Выберите подходящий продукт</h2>
                    <p>
                        Используйте подтверждённые данные компании при подаче
                        заявки.
                    </p>
                    <button
                        className='dark-button'
                        onClick={() => navigate('/catalog')}
                    >
                        Перейти к продуктам <span>→</span>
                    </button>
                </div>
                <div className='abstract-shape'>✦</div>
            </section>
            {companyEditorOpen && (
                <CompanyForm
                    form={companyForm}
                    setForm={setCompanyForm}
                    onSubmit={saveCompany}
                    onClose={() => setCompanyEditorOpen(false)}
                    existing={Boolean(company)}
                />
            )}
            {financialEditorOpen && (
                <FinancialForm
                    form={financialForm}
                    setForm={setFinancialForm}
                    onSubmit={saveFinancialData}
                    onClose={() => setFinancialEditorOpen(false)}
                />
            )}
            {historyEditorOpen && (
                <HistoryForm
                    form={historyForm}
                    setForm={setHistoryForm}
                    onSubmit={saveCreditHistory}
                    onClose={() => setHistoryEditorOpen(false)}
                />
            )}
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

function DataPanel({
    title,
    ready,
    onEdit,
    children,
}: {
    title: string;
    ready: boolean;
    onEdit: () => void;
    children: React.ReactNode;
}) {
    return (
        <article className='data-panel'>
            <div className='data-panel-heading'>
                <div>
                    <strong>{title}</strong>
                    <small>
                        {ready
                            ? 'Данные сохранены'
                            : 'Заполните для оценки заявки'}
                    </small>
                </div>
                <button className='text-button' onClick={onEdit}>
                    {ready ? 'Изменить' : 'Заполнить'}
                </button>
            </div>
            <div className='data-panel-values'>{children}</div>
        </article>
    );
}

function EditorModal({
    title,
    onClose,
    children,
    onSubmit,
}: {
    title: string;
    onClose: () => void;
    children: React.ReactNode;
    onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
    return (
        <div className='modal-backdrop' role='presentation'>
            <form className='modal-card' onSubmit={onSubmit}>
                <button type='button' className='modal-close' onClick={onClose}>
                    ×
                </button>
                <span className='eyebrow'>Данные компании</span>
                <h2>{title}</h2>
                {children}
                <button className='primary-button' type='submit'>
                    Сохранить <span>→</span>
                </button>
            </form>
        </div>
    );
}

function CompanyForm({
    form,
    setForm,
    onSubmit,
    onClose,
    existing,
}: {
    form: {
        name: string;
        type: string;
        industry: string;
        foundedAt: string;
        employeesCount: string;
        businessStatus: string;
    };
    setForm: React.Dispatch<
        React.SetStateAction<{
            name: string;
            type: string;
            industry: string;
            foundedAt: string;
            employeesCount: string;
            businessStatus: string;
        }>
    >;
    onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
    onClose: () => void;
    existing: boolean;
}) {
    return (
        <EditorModal
            title={existing ? 'Изменить компанию' : 'Добавить компанию'}
            onClose={onClose}
            onSubmit={onSubmit}
        >
            <label>
                Название
                <input
                    value={form.name}
                    onChange={(event) =>
                        setForm({ ...form, name: event.target.value })
                    }
                    placeholder='Название бизнеса'
                />
            </label>
            <label>
                Тип
                <select
                    value={form.type}
                    disabled={existing}
                    onChange={(event) =>
                        setForm({ ...form, type: event.target.value })
                    }
                >
                    <option value='IP'>ИП</option>
                    <option value='OOO'>ООО</option>
                </select>
            </label>
            <label>
                Отрасль
                <input
                    required
                    value={form.industry}
                    onChange={(event) =>
                        setForm({ ...form, industry: event.target.value })
                    }
                />
            </label>
            <label>
                Дата основания
                <input
                    type='date'
                    value={form.foundedAt}
                    onChange={(event) =>
                        setForm({ ...form, foundedAt: event.target.value })
                    }
                />
            </label>
            <label>
                Сотрудников
                <input
                    type='number'
                    min='0'
                    required
                    value={form.employeesCount}
                    onChange={(event) =>
                        setForm({ ...form, employeesCount: event.target.value })
                    }
                />
            </label>
            <label>
                Статус
                <select
                    value={form.businessStatus}
                    onChange={(event) =>
                        setForm({ ...form, businessStatus: event.target.value })
                    }
                >
                    <option value='ACTIVE'>Работает</option>
                    <option value='LIQUIDATING'>Ликвидируется</option>
                    <option value='CLOSED'>Закрыта</option>
                </select>
            </label>
        </EditorModal>
    );
}

function FinancialForm({
    form,
    setForm,
    onSubmit,
    onClose,
}: {
    form: {
        revenueMonthly: string;
        revenueAnnual: string;
        currentDebtPayments: string;
        netProfitMonthly: string;
        accountsPayable: string;
    };
    setForm: React.Dispatch<
        React.SetStateAction<{
            revenueMonthly: string;
            revenueAnnual: string;
            currentDebtPayments: string;
            netProfitMonthly: string;
            accountsPayable: string;
        }>
    >;
    onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
    onClose: () => void;
}) {
    const fields = [
        ['revenueMonthly', 'Выручка в месяц'],
        ['revenueAnnual', 'Выручка за год'],
        ['currentDebtPayments', 'Платежи по долгам'],
        ['netProfitMonthly', 'Чистая прибыль в месяц'],
        ['accountsPayable', 'Кредиторская задолженность'],
    ] as const;
    return (
        <EditorModal
            title='Финансовые показатели'
            onClose={onClose}
            onSubmit={onSubmit}
        >
            {fields.map(([key, label]) => (
                <label key={key}>
                    {label}
                    <input
                        type='number'
                        min='0'
                        required
                        value={form[key]}
                        onChange={(event) =>
                            setForm({ ...form, [key]: event.target.value })
                        }
                    />
                </label>
            ))}
        </EditorModal>
    );
}

function HistoryForm({
    form,
    setForm,
    onSubmit,
    onClose,
}: {
    form: {
        delinquencies12m: string;
        activeLoansCount: string;
        totalDebtAmount: string;
        taxArrears: boolean;
    };
    setForm: React.Dispatch<
        React.SetStateAction<{
            delinquencies12m: string;
            activeLoansCount: string;
            totalDebtAmount: string;
            taxArrears: boolean;
        }>
    >;
    onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
    onClose: () => void;
}) {
    return (
        <EditorModal
            title='Кредитная история'
            onClose={onClose}
            onSubmit={onSubmit}
        >
            <label>
                Просрочки за 12 месяцев
                <input
                    type='number'
                    min='0'
                    required
                    value={form.delinquencies12m}
                    onChange={(event) =>
                        setForm({
                            ...form,
                            delinquencies12m: event.target.value,
                        })
                    }
                />
            </label>
            <label>
                Активных кредитов
                <input
                    type='number'
                    min='0'
                    required
                    value={form.activeLoansCount}
                    onChange={(event) =>
                        setForm({
                            ...form,
                            activeLoansCount: event.target.value,
                        })
                    }
                />
            </label>
            <label>
                Общий долг
                <input
                    type='number'
                    min='0'
                    required
                    value={form.totalDebtAmount}
                    onChange={(event) =>
                        setForm({
                            ...form,
                            totalDebtAmount: event.target.value,
                        })
                    }
                />
            </label>
            <label className='checkbox-label'>
                <input
                    type='checkbox'
                    checked={form.taxArrears}
                    onChange={(event) =>
                        setForm({ ...form, taxArrears: event.target.checked })
                    }
                />{' '}
                Есть задолженность по налогам
            </label>
        </EditorModal>
    );
}

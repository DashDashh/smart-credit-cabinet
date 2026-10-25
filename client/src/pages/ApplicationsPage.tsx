import { useCallback, useEffect, useMemo, useState } from 'react';
import type {
    ApplicationStatus,
    ApplicationWithDecision,
    CreditApplication,
    Product,
    Session,
} from '../shared/types';
import { api, getUserErrorMessage } from '../shared/api';

const statusLabels: Record<ApplicationStatus, string> = {
    PENDING: 'Новая',
    APPROVED: 'Одобрена',
    REJECTED: 'Отклонена',
    ACTIVE: 'Активна',
    CLOSED: 'Закрыта',
};

export function ApplicationsPage({ session }: { session: Session }) {
    const [applications, setApplications] = useState<CreditApplication[]>([]);
    const [products, setProducts] = useState<Product[]>([]);
    const [statusFilter, setStatusFilter] = useState<'ALL' | ApplicationStatus>(
        'ALL',
    );
    const [message, setMessage] = useState('');
    const [selectedDecision, setSelectedDecision] =
        useState<ApplicationWithDecision | null>(null);

    const load = useCallback(
        () =>
            Promise.all([
                api.applications.list(session),
                api.products.list(session),
            ])
                .then(([nextApplications, nextProducts]) => {
                    setApplications(nextApplications);
                    setProducts(nextProducts);
                })
                .catch((error: unknown) =>
                    setMessage(
                        getUserErrorMessage(
                            error,
                            'Не удалось загрузить заявки. Попробуйте обновить страницу.',
                        ),
                    ),
                ),
        [session],
    );

    useEffect(() => {
        void load();
    }, [load]);

    const visibleApplications = useMemo(
        () =>
            statusFilter === 'ALL'
                ? applications
                : applications.filter(
                      (application) => application.status === statusFilter,
                  ),
        [applications, statusFilter],
    );

    const changeStatus = async (
        application: CreditApplication,
        status: ApplicationStatus,
    ) => {
        try {
            const updated = await api.applications.changeStatus(
                session,
                application.productId,
                application.clientId,
                status,
            );
            setApplications((current) =>
                current.map((item) =>
                    item.productId === updated.productId &&
                    item.clientId === updated.clientId
                        ? updated
                        : item,
                ),
            );
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось изменить статус заявки. Попробуйте ещё раз.',
                ),
            );
        }
    };

    const productName = (productId: string) =>
        products.find((product) => product.id === productId)?.name ||
        'Кредитный продукт';
    const showDecision = async (application: CreditApplication) => {
        try {
            setSelectedDecision(
                await api.applications.withDecision(
                    session,
                    application.productId,
                    application.clientId,
                ),
            );
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось загрузить решение по заявке. Попробуйте ещё раз.',
                ),
            );
        }
    };

    return (
        <>
            <div className='page-heading'>
                <div>
                    <span className='eyebrow'>Рабочий кабинет</span>
                    <h1>Заявки на кредит</h1>
                    <p className='muted'>Реальные заявки из системы банка</p>
                </div>
                <div className='application-counter'>
                    <strong>{applications.length}</strong>
                    <span>всего заявок</span>
                </div>
            </div>
            {message && <div className='notice'>{message}</div>}
            <section className='employee-summary'>
                <Summary
                    icon='▣'
                    tone='purple'
                    label='Всего заявок'
                    value={applications.length}
                />
                <Summary
                    icon='◷'
                    tone='orange'
                    label='На проверке'
                    value={
                        applications.filter((item) => item.status === 'PENDING')
                            .length
                    }
                />
                <Summary
                    icon='✓'
                    tone='green'
                    label='Одобрено'
                    value={
                        applications.filter(
                            (item) => item.status === 'APPROVED',
                        ).length
                    }
                />
            </section>
            <section className='section-block applications-section'>
                <div className='section-title'>
                    <div>
                        <h2>Входящие заявки</h2>
                        <p className='muted'>
                            Доступны действия согласно роли сотрудника
                        </p>
                    </div>
                    <select
                        className='status-select'
                        value={statusFilter}
                        onChange={(event) =>
                            setStatusFilter(
                                event.target.value as 'ALL' | ApplicationStatus,
                            )
                        }
                    >
                        <option value='ALL'>Все статусы</option>
                        {Object.entries(statusLabels).map(([value, label]) => (
                            <option key={value} value={value}>
                                {label}
                            </option>
                        ))}
                    </select>
                </div>
                <div className='application-list'>
                    {visibleApplications.map((application) => (
                        <article
                            className='application-row'
                            key={`${application.productId}-${application.clientId}`}
                        >
                            <span className='app-avatar'>
                                {application.clientId.slice(0, 2).toUpperCase()}
                            </span>
                            <div className='application-client'>
                                <strong>
                                    {productName(application.productId)}
                                </strong>
                                <small>
                                    Клиент {application.clientId.slice(0, 8)} ·{' '}
                                    {application.termMonth} мес.
                                </small>
                            </div>
                            <strong className='application-amount'>
                                {application.amount.toLocaleString('ru-RU')} ₽
                            </strong>
                            <span
                                className={`application-status ${application.status.toLowerCase()}`}
                            >
                                {statusLabels[application.status]}
                            </span>
                            <span className='application-actions'>
                                <button
                                    onClick={() =>
                                        void showDecision(application)
                                    }
                                >
                                    Решение
                                </button>
                                {application.status === 'PENDING' && (
                                    <>
                                        <button
                                            onClick={() =>
                                                void changeStatus(
                                                    application,
                                                    'APPROVED',
                                                )
                                            }
                                        >
                                            Одобрить
                                        </button>
                                        <button
                                            onClick={() =>
                                                void changeStatus(
                                                    application,
                                                    'REJECTED',
                                                )
                                            }
                                        >
                                            Отклонить
                                        </button>
                                    </>
                                )}
                            </span>
                        </article>
                    ))}
                    {!visibleApplications.length && (
                        <div className='empty-state'>
                            Заявок с выбранным статусом нет.
                        </div>
                    )}
                </div>
            </section>
            {selectedDecision && (
                <DecisionModal
                    data={selectedDecision}
                    productName={productName(
                        selectedDecision.applicationResponse.productId,
                    )}
                    onClose={() => setSelectedDecision(null)}
                />
            )}
        </>
    );
}

function DecisionModal({
    data,
    productName,
    onClose,
}: {
    data: ApplicationWithDecision;
    productName: string;
    onClose: () => void;
}) {
    const decision = data.decisionClientResponse;
    return (
        <div className='modal-backdrop' role='presentation'>
            <div className='modal-card decision-modal'>
                <button className='modal-close' onClick={onClose}>
                    ×
                </button>
                <span className='eyebrow'>Скоринговое решение</span>
                <h2>{productName}</h2>
                {decision ? (
                    <>
                        <div className='decision-score'>
                            <strong>{decision.score.toFixed(0)}</strong>
                            <span>
                                скоринг
                                <br />
                                из 100
                            </span>
                            <b
                                className={`risk-${decision.riskLevel.toLowerCase()}`}
                            >
                                {decision.riskLevel === 'LOW'
                                    ? 'Низкий риск'
                                    : decision.riskLevel === 'MEDIUM'
                                      ? 'Средний риск'
                                      : 'Высокий риск'}
                            </b>
                        </div>
                        <div className='decision-grid'>
                            <div>
                                <small>Решение</small>
                                <strong>
                                    {decision.decision ===
                                    'APPROVED_WITH_CONDITIONS'
                                        ? 'Одобрено с условиями'
                                        : decision.decision === 'APPROVED'
                                          ? 'Одобрено'
                                          : decision.decision === 'NEEDS_INFO'
                                            ? 'Нужны данные'
                                            : decision.decision ===
                                                'MANUAL_REVIEW'
                                              ? 'Ручная проверка'
                                              : 'Отказ'}
                                </strong>
                            </div>
                            <div>
                                <small>Ставка</small>
                                <strong>
                                    {decision.rate !== null
                                        ? `${decision.rate}%`
                                        : '—'}
                                </strong>
                            </div>
                            <div>
                                <small>Следующий шаг</small>
                                <strong>{decision.nextStep}</strong>
                            </div>
                            <div>
                                <small>Версия модели</small>
                                <strong>{decision.modelVersion}</strong>
                            </div>
                        </div>
                        {decision.conditions && (
                            <div className='decision-conditions'>
                                <strong>Условия предложения</strong>
                                <span>
                                    Сумма до{' '}
                                    {decision.conditions.approvedAmount.toLocaleString(
                                        'ru-RU',
                                    )}{' '}
                                    ₽
                                </span>
                                <span>
                                    {decision.conditions.termMonths} месяцев ·{' '}
                                    {decision.conditions.rate}%
                                </span>
                                <small>{decision.conditions.reason}</small>
                            </div>
                        )}
                        <small className='decision-footnote'>
                            {decision.fallbackUsed
                                ? 'Использован fallback-скоринг'
                                : 'Рассчитано ML-моделью'}
                        </small>
                    </>
                ) : (
                    <div className='empty-state'>
                        Для заявки ещё нет решения.
                    </div>
                )}
            </div>
        </div>
    );
}

function Summary({
    icon,
    tone,
    label,
    value,
}: {
    icon: string;
    tone: string;
    label: string;
    value: number;
}) {
    return (
        <div>
            <span className={`stat-icon ${tone}`}>{icon}</span>
            <span className='muted'>{label}</span>
            <strong>{value}</strong>
        </div>
    );
}

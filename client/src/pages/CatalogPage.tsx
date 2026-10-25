import { useEffect, useState } from 'react';
import type {
    Account,
    Product,
    ProductPurpose,
    Session,
} from '../shared/types';
import { ApiError, api, getUserErrorMessage } from '../shared/api';

const labels: Record<ProductPurpose, string> = {
    INVEST: 'Развитие бизнеса',
    REVOLVING: 'Оборотное финансирование',
    MICRO: 'Микрокредит',
};

export function CatalogPage({
    account,
    session,
}: {
    account: Account;
    session: Session;
}) {
    const [products, setProducts] = useState<Product[]>([]);
    const [companies, setCompanies] = useState<{ id: string; name: string }[]>(
        [],
    );
    const [selectedProduct, setSelectedProduct] = useState<Product | null>(
        null,
    );
    const [editingProduct, setEditingProduct] = useState<Product | null>(null);
    const [productEditorOpen, setProductEditorOpen] = useState(false);
    const [productForm, setProductForm] = useState({
        name: '',
        amount: '',
        termMonths: '',
        rate: '',
        purpose: 'INVEST' as ProductPurpose,
    });
    const [amount, setAmount] = useState('');
    const [termMonth, setTermMonth] = useState('');
    const [purpose, setPurpose] = useState('');
    const [message, setMessage] = useState('');
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const load = async () => {
            const nextProducts = await api.products.list(session);
            const nextCompanies =
                account.role === 'ENTREPRENEUR'
                    ? await api.companies.mine(session)
                    : [];
            setProducts(nextProducts);
            setCompanies(nextCompanies);
        };
        load()
            .catch((error: unknown) =>
                setMessage(
                    getUserErrorMessage(
                        error,
                        'Не удалось загрузить продукты. Попробуйте обновить страницу.',
                    ),
                ),
            )
            .finally(() => setLoading(false));
    }, [account.role, session]);

    const submitApplication = async (
        event: React.FormEvent<HTMLFormElement>,
    ) => {
        event.preventDefault();
        if (!selectedProduct || !companies[0]) {
            setMessage('Сначала добавьте компанию в личном кабинете');
            return;
        }
        try {
            const [financialResult] = await Promise.allSettled([
                api.companies.financialData(session, companies[0].id),
            ]);
            if (financialResult.status === 'rejected') {
                if (
                    financialResult.reason instanceof ApiError &&
                    financialResult.reason.status === 404
                ) {
                    setMessage(
                        'Перед подачей заявки заполните финансовые показатели компании в личном кабинете.',
                    );
                } else {
                    setMessage(
                        getUserErrorMessage(
                            financialResult.reason,
                            'Не удалось проверить финансовые данные. Попробуйте ещё раз.',
                        ),
                    );
                }
                setSelectedProduct(null);
                return;
            }
            await api.applications.create(session, {
                productId: selectedProduct.id,
                clientId: companies[0].id,
                amount: Number(amount),
                termMonth: Number(termMonth),
                purpose,
            });
            setMessage('Заявка отправлена на рассмотрение');
            setSelectedProduct(null);
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось отправить заявку. Проверьте данные компании и повторите попытку.',
                ),
            );
        }
    };

    const submitProduct = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        const data = {
            name: productForm.name,
            amount: Number(productForm.amount),
            termMonths: Number(productForm.termMonths),
            rate: Number(productForm.rate),
            purpose: productForm.purpose,
        };
        try {
            const saved = editingProduct
                ? await api.products.update(session, editingProduct.id, data)
                : await api.products.create(session, data);
            setProducts((current) =>
                editingProduct
                    ? current.map((item) =>
                          item.id === saved.id ? saved : item,
                      )
                    : [...current, saved],
            );
            setEditingProduct(null);
            setProductEditorOpen(false);
            setMessage(editingProduct ? 'Продукт обновлён' : 'Продукт создан');
        } catch (error) {
            setMessage(
                getUserErrorMessage(
                    error,
                    'Не удалось сохранить продукт. Проверьте заполненные поля.',
                ),
            );
        }
    };

    const openProductEditor = (product?: Product) => {
        setProductEditorOpen(true);
        setEditingProduct(product || null);
        setProductForm(
            product
                ? {
                      name: product.name,
                      amount: String(product.amount),
                      termMonths: String(product.termMonths),
                      rate: String(product.rate),
                      purpose: product.purpose,
                  }
                : {
                      name: '',
                      amount: '',
                      termMonths: '',
                      rate: '',
                      purpose: 'INVEST',
                  },
        );
    };

    return (
        <>
            <div className='page-heading catalog-heading'>
                <div>
                    <span className='eyebrow'>Финансовые решения</span>
                    <h1>Продукты для бизнеса</h1>
                    <p className='muted'>
                        Актуальные предложения из каталога банка
                    </p>
                </div>
                <div className='catalog-note'>
                    ✦ Подбор по данным
                    <br />
                    <span>вашей компании</span>
                </div>
            </div>
            {message && <div className='notice'>{message}</div>}
            {loading ? (
                <div className='empty-state'>Загружаем предложения…</div>
            ) : (
                <div className='credit-list'>
                    {products.map((product) => (
                        <article className='credit-card' key={product.id}>
                            <div
                                className={`credit-icon ${product.purpose === 'INVEST' ? 'blue' : product.purpose === 'MICRO' ? 'orange' : 'violet'}`}
                            >
                                ▣
                            </div>
                            <h2>{product.name}</h2>
                            <p>{labels[product.purpose]}</p>
                            <div className='credit-details'>
                                <div>
                                    <small>Сумма</small>
                                    <strong>
                                        до{' '}
                                        {product.amount.toLocaleString('ru-RU')}{' '}
                                        ₽
                                    </strong>
                                </div>
                                <div>
                                    <small>Ставка</small>
                                    <strong>{product.rate}%</strong>
                                </div>
                                <div>
                                    <small>Срок</small>
                                    <strong>
                                        до {product.termMonths} мес.
                                    </strong>
                                </div>
                            </div>
                            {account.role === 'ENTREPRENEUR' && (
                                <button
                                    className='outline-button'
                                    onClick={() => {
                                        setSelectedProduct(product);
                                        setAmount(String(product.amount));
                                        setTermMonth(
                                            String(product.termMonths),
                                        );
                                    }}
                                >
                                    Подать заявку <span>→</span>
                                </button>
                            )}
                            {account.role === 'BANK_EMPLOYEE' && (
                                <button
                                    className='outline-button'
                                    onClick={() => openProductEditor(product)}
                                >
                                    Редактировать <span>→</span>
                                </button>
                            )}
                        </article>
                    ))}
                    {!products.length && (
                        <div className='empty-state'>
                            В каталоге пока нет доступных продуктов.
                        </div>
                    )}
                </div>
            )}
            {account.role === 'BANK_EMPLOYEE' && (
                <button
                    className='primary-button product-create-button'
                    onClick={() => openProductEditor()}
                >
                    Добавить продукт <span>＋</span>
                </button>
            )}
            {selectedProduct && (
                <div className='modal-backdrop' role='presentation'>
                    <form className='modal-card' onSubmit={submitApplication}>
                        <button
                            type='button'
                            className='modal-close'
                            onClick={() => setSelectedProduct(null)}
                        >
                            ×
                        </button>
                        <span className='eyebrow'>Новая заявка</span>
                        <h2>{selectedProduct.name}</h2>
                        <label>
                            Сумма
                            <input
                                type='number'
                                min='0'
                                required
                                value={amount}
                                onChange={(event) =>
                                    setAmount(event.target.value)
                                }
                            />
                        </label>
                        <label>
                            Срок, месяцев
                            <input
                                type='number'
                                min='1'
                                required
                                value={termMonth}
                                onChange={(event) =>
                                    setTermMonth(event.target.value)
                                }
                            />
                        </label>
                        <label>
                            Цель финансирования
                            <input
                                required
                                value={purpose}
                                onChange={(event) =>
                                    setPurpose(event.target.value)
                                }
                                placeholder='Например, закупка оборудования'
                            />
                        </label>
                        <button className='primary-button' type='submit'>
                            Отправить заявку <span>→</span>
                        </button>
                    </form>
                </div>
            )}
            {productEditorOpen && (
                <div className='modal-backdrop' role='presentation'>
                    <form className='modal-card' onSubmit={submitProduct}>
                        <button
                            type='button'
                            className='modal-close'
                            onClick={() => {
                                setProductEditorOpen(false);
                                setEditingProduct(null);
                            }}
                        >
                            ×
                        </button>
                        <span className='eyebrow'>
                            {editingProduct
                                ? 'Редактирование'
                                : 'Новый продукт'}
                        </span>
                        <h2>Кредитный продукт</h2>
                        <label>
                            Название
                            <input
                                required
                                value={productForm.name}
                                onChange={(event) =>
                                    setProductForm({
                                        ...productForm,
                                        name: event.target.value,
                                    })
                                }
                            />
                        </label>
                        <label>
                            Лимит, ₽
                            <input
                                type='number'
                                min='0'
                                required
                                value={productForm.amount}
                                onChange={(event) =>
                                    setProductForm({
                                        ...productForm,
                                        amount: event.target.value,
                                    })
                                }
                            />
                        </label>
                        <label>
                            Срок, месяцев
                            <input
                                type='number'
                                min='1'
                                required
                                value={productForm.termMonths}
                                onChange={(event) =>
                                    setProductForm({
                                        ...productForm,
                                        termMonths: event.target.value,
                                    })
                                }
                            />
                        </label>
                        <label>
                            Ставка, %
                            <input
                                type='number'
                                min='0'
                                step='0.1'
                                required
                                value={productForm.rate}
                                onChange={(event) =>
                                    setProductForm({
                                        ...productForm,
                                        rate: event.target.value,
                                    })
                                }
                            />
                        </label>
                        <label>
                            Назначение
                            <select
                                className='status-select'
                                value={productForm.purpose}
                                onChange={(event) =>
                                    setProductForm({
                                        ...productForm,
                                        purpose: event.target
                                            .value as ProductPurpose,
                                    })
                                }
                            >
                                <option value='INVEST'>Развитие бизнеса</option>
                                <option value='REVOLVING'>
                                    Оборотное финансирование
                                </option>
                                <option value='MICRO'>Микрокредит</option>
                            </select>
                        </label>
                        <button className='primary-button' type='submit'>
                            Сохранить продукт <span>→</span>
                        </button>
                    </form>
                </div>
            )}
        </>
    );
}

import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import styles from './VipPage.module.css';
import Loader from '../components/Loader';
import { useAuthStore } from '../store/useAuthStore';
import { useToastStore } from '../store/useToastStore';
import { useVipStore } from '../store/useVipStore';

interface VipStatusResponse {
  active: boolean;
  expiresAt?: string;
  planId?: string;
  balanceStars?: number;
}

interface VipPurchaseResponse {
  active: boolean;
  expiresAt?: string;
}

const PLAN = {
  id: 'vip-monthly',
  title: 'VIP на 30 дней',
  priceStars: 1499,
  description: 'Подписка продлится автоматически через месяц',
};

const FEATURES = [
  { icon: '⚡️', text: 'Мгновенные суперконтакты и приоритетное место в выдаче' },
  { icon: '💬', text: 'Безлимитные суперлайки и возможность написать первыми' },
  { icon: '🎯', text: 'Рекомендации по интересам и расширенный фильтр' },
  { icon: '👀', text: 'Видите, кто лайкнул вас, до того как вы ответите' },
];

const FAQ_LINK = 'https://core.telegram.org/bots/webapps#telegram-stars';
const TOP_UP_LINK = 'https://t.me/Catalog/telegram-stars';

export default function VipPage() {
  const token = useAuthStore((s) => s.token);
  const showToast = useToastStore((s) => s.show);
  const setVipStatus = useVipStore((s) => s.setStatus);
  const isVip = useVipStore((s) => s.isVip);
  const expiresAt = useVipStore((s) => s.expiresAt);
  const queryClient = useQueryClient();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const authHeaders = useMemo<HeadersInit | undefined>(() => (token ? { Authorization: `Bearer ${token}` } : undefined), [token]);

  const purchaseHeaders = useMemo<HeadersInit>(
    () => ({
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }),
    [token],
  );

  const statusQuery = useQuery({
    queryKey: ['vip-status', token ?? 'guest'],
    queryFn: async (): Promise<VipStatusResponse> => {
      const res = await fetch('/api/stars/status', { headers: authHeaders });
      if (!res.ok) throw new Error('Не удалось получить статус');
      return res.json();
    },
    staleTime: 30_000,
  });

  useEffect(() => {
    if (statusQuery.data) {
      setVipStatus(statusQuery.data.active, statusQuery.data.expiresAt);
    }
  }, [setVipStatus, statusQuery.data]);

  const purchaseMutation = useMutation({
    mutationFn: async (): Promise<VipPurchaseResponse> => {
      const res = await fetch('/api/stars/purchase', {
        method: 'POST',
        headers: purchaseHeaders,
        body: JSON.stringify({ planId: PLAN.id }),
      });
      if (!res.ok) throw new Error('Оплата не прошла');
      return res.json();
    },
    onMutate: () => {
      setErrorMessage(null);
    },
    onSuccess: (data) => {
      setVipStatus(data.active, data.expiresAt);
      queryClient.invalidateQueries({ queryKey: ['vip-status'] });
      showToast('VIP активирован!');
    },
    onError: () => {
      setErrorMessage('Не удалось завершить оплату. Попробуйте позже или уточните баланс Stars.');
      showToast('Оплата не прошла');
    },
  });

  const tg = (window as any).Telegram?.WebApp;

  const handleTopUp = () => {
    if (tg?.openTelegramLink) {
      tg.openTelegramLink(TOP_UP_LINK);
    } else {
      window.open(TOP_UP_LINK, '_blank', 'noopener');
    }
  };

  const handleFaq = () => {
    if (tg?.openLink) {
      tg.openLink(FAQ_LINK);
    } else {
      window.open(FAQ_LINK, '_blank', 'noopener');
    }
  };

  const renderStatus = () => {
    if (statusQuery.isLoading) {
      return (
        <div className={styles.loaderWrap}>
          <Loader />
        </div>
      );
    }

    if (statusQuery.isError) {
      return <div className={styles.errorCard}>Не удалось загрузить статус VIP. Попробуйте обновить позже.</div>;
    }

    if (isVip) {
      return (
        <div className={styles.hero}>
          <span className={styles.statusBadge}>VIP активен</span>
          <p className={styles.statusText}>
            {expiresAt
              ? `Подписка действительна до ${new Intl.DateTimeFormat('ru-RU', {
                  day: 'numeric',
                  month: 'long',
                }).format(new Date(expiresAt))}`
              : 'Подписка активирована.'}
          </p>
        </div>
      );
    }

    return (
      <div className={styles.hero}>
        <h1 className={styles.heroTitle}>Откройте VIP доступ</h1>
        <p className={styles.heroSubtitle}>Суперлайки, продвинутый поиск и приоритет в выдаче прямо внутри Telegram Mini App.</p>
      </div>
    );
  };

  const buttonLabel = purchaseMutation.isPending
    ? 'Ожидание оплаты...'
    : isVip
      ? 'Вы уже VIP'
      : `Купить за ${PLAN.priceStars.toLocaleString('ru-RU')} Stars`;

  return (
    <div className={styles.page}>
      {renderStatus()}

      <section className={styles.planCard} aria-labelledby="vip-plan-label">
        <header className={styles.planHeader}>
          <div>
            <h2 className={styles.planTitle} id="vip-plan-label">
              {PLAN.title}
            </h2>
            <p className={styles.heroSubtitle}>{PLAN.description}</p>
          </div>
          <p className={styles.planPrice}>{PLAN.priceStars.toLocaleString('ru-RU')}★</p>
        </header>

        <ul className={styles.features}>
          {FEATURES.map((feature) => (
            <li key={feature.text} className={styles.featureItem}>
              <span className={styles.featureIcon} aria-hidden>
                {feature.icon}
              </span>
              <span>{feature.text}</span>
            </li>
          ))}
        </ul>

        <div className={styles.actions}>
          <button
            type="button"
            className={styles.purchaseButton}
            onClick={() => purchaseMutation.mutate()}
            disabled={purchaseMutation.isPending || statusQuery.isLoading || statusQuery.isError || isVip}
          >
            {buttonLabel}
          </button>
          <button type="button" className={styles.secondaryButton} onClick={handleTopUp}>
            Пополнить баланс Stars
          </button>
        </div>
        {errorMessage && <div className={styles.errorCard}>{errorMessage}</div>}
      </section>

      <section className={styles.faqCard} aria-labelledby="vip-faq-title">
        <h2 className={styles.faqTitle} id="vip-faq-title">
          Что такое Telegram Stars?
        </h2>
        <p className={styles.faqText}>
          Stars — внутренняя валюта Telegram для цифровых товаров. Оплата проходит через официальный биллинг, а возвраты и чеки
          доступны в Telegram. Подробнее в{' '}
          <button type="button" className={styles.link} onClick={handleFaq}>
            правилах оплаты
          </button>
          .
        </p>
      </section>
    </div>
  );
}

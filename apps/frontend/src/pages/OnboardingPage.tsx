import { FormEvent, useEffect, useMemo, useState } from 'react';
import styles from './OnboardingPage.module.css';
import { useToastStore } from '../store/useToastStore';
import { useAuthStore } from '../store/useAuthStore';
import DeviceStorage, { DeviceStorageError } from '../utils/deviceStorage';

interface OnboardingDraft {
  step: number;
  name: string;
  bio: string;
  interests: string;
  goals: string;
}

const DRAFT_KEY = 'onboarding-draft';

const defaultDraft: OnboardingDraft = {
  step: 0,
  name: '',
  bio: '',
  interests: '',
  goals: '',
};

export default function OnboardingPage() {
  const showToast = useToastStore((s) => s.show);
  const token = useAuthStore((s) => s.token);
  const [draft, setDraft] = useState<OnboardingDraft>(defaultDraft);
  const [isLoadingDraft, setIsLoadingDraft] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const authHeaders = useMemo<HeadersInit>(
    () => ({
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }),
    [token],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const stored = await DeviceStorage.getJSON<OnboardingDraft>(DRAFT_KEY);
        if (!cancelled && stored) {
          setDraft({ ...defaultDraft, ...stored });
        }
      } catch (error) {
        if (!cancelled) {
          const message = error instanceof DeviceStorageError ? error.message : 'Не удалось восстановить черновик';
          showToast(message);
        }
      } finally {
        if (!cancelled) setIsLoadingDraft(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [showToast]);

  const persistDraft = async (next: OnboardingDraft) => {
    try {
      await DeviceStorage.setJSON(DRAFT_KEY, next);
    } catch (error) {
      const message = error instanceof DeviceStorageError ? error.message : 'Не удалось сохранить черновик';
      showToast(message);
    }
  };

  const updateDraft = (patch: Partial<OnboardingDraft>) => {
    setDraft((prev) => {
      const next = { ...prev, ...patch };
      void persistDraft(next);
      return next;
    });
  };

  const handleNext = async () => {
    const next = Math.min(draft.step + 1, steps.length - 1);
    if (next !== draft.step) {
      const updated = { ...draft, step: next };
      setDraft(updated);
      await persistDraft(updated);
    }
  };

  const handleBack = async () => {
    const prev = Math.max(draft.step - 1, 0);
    if (prev !== draft.step) {
      const updated = { ...draft, step: prev };
      setDraft(updated);
      await persistDraft(updated);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const response = await fetch('/api/onboarding/complete', {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify({
          name: draft.name,
          bio: draft.bio,
          interests: draft.interests,
          goals: draft.goals,
        }),
      });
      if (!response.ok) {
        throw new Error('Ошибка отправки анкеты');
      }
      await DeviceStorage.removeItem(DRAFT_KEY);
      setDraft(defaultDraft);
      showToast('Онбординг завершён');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Не удалось завершить онбординг';
      showToast(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = [
    {
      title: 'Расскажите о себе',
      description: 'Как вас зовут и что вам нравится? Эти данные увидят будущие мэтчи.',
      content: (
        <fieldset className={styles.fieldset}>
          <label className={styles.label} htmlFor="name">
            Имя или никнейм
          </label>
          <input
            id="name"
            className={styles.input}
            placeholder="Например, Анна"
            value={draft.name}
            onChange={(event) => updateDraft({ name: event.target.value })}
          />

          <label className={styles.label} htmlFor="bio">
            Расскажите о себе
          </label>
          <textarea
            id="bio"
            className={styles.textarea}
            placeholder="Пара предложений о хобби, любимых местах или планах."
            rows={4}
            value={draft.bio}
            onChange={(event) => updateDraft({ bio: event.target.value })}
          />
        </fieldset>
      ),
      primaryAction: (
        <button type="button" className={styles.primary} onClick={handleNext}>
          Далее
        </button>
      ),
    },
    {
      title: 'Выберите интересы',
      description: 'Поделитесь тем, что вас вдохновляет. Это поможет подобрать совместимые знакомства.',
      content: (
        <fieldset className={styles.fieldset}>
          <label className={styles.label} htmlFor="interests">
            Интересы
          </label>
          <textarea
            id="interests"
            className={styles.textarea}
            placeholder="Например: походы, кулинария, стендап"
            rows={3}
            value={draft.interests}
            onChange={(event) => updateDraft({ interests: event.target.value })}
          />

          <label className={styles.label} htmlFor="goals">
            Цели знакомства
          </label>
          <textarea
            id="goals"
            className={styles.textarea}
            placeholder="Что вы ищете: общение, путешествия, серьёзные отношения"
            rows={3}
            value={draft.goals}
            onChange={(event) => updateDraft({ goals: event.target.value })}
          />
        </fieldset>
      ),
      primaryAction: (
        <button type="submit" className={styles.primary} disabled={isSubmitting}>
          Завершить онбординг
        </button>
      ),
    },
  ];

  if (isLoadingDraft) {
    return <div className="discovery-card">Загрузка черновика…</div>;
  }

  const currentStep = steps[draft.step];

  return (
    <form className={styles.page} onSubmit={handleSubmit}>
      <section className={styles.card}>
        <div className={styles.progress}>
          {steps.map((_, index) => (
            <span key={index} className={styles.dot} data-active={index === draft.step} aria-hidden />
          ))}
        </div>
        <h1>{currentStep.title}</h1>
        <p className={styles.helper}>{currentStep.description}</p>
        {currentStep.content}
        <div className={styles.actions}>
          {currentStep.primaryAction}
          {draft.step > 0 && (
            <button type="button" className={styles.secondary} onClick={handleBack}>
              Назад
            </button>
          )}
        </div>
      </section>
    </form>
  );
}


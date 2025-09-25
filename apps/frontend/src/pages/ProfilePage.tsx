import { FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import Loader from '../components/Loader';
import styles from './ProfilePage.module.css';
import { useAuthStore } from '../store/useAuthStore';
import { useToastStore } from '../store/useToastStore';
import DeviceStorage, { DeviceStorageError } from '../utils/deviceStorage';

interface ProfileResponse {
  username: string;
  displayName?: string;
  bio?: string;
  location?: string;
  interests?: string;
}

interface ProfileDraft {
  displayName: string;
  bio: string;
  location: string;
  interests: string;
}

const DRAFT_KEY = 'profile-draft';

const defaultDraft: ProfileDraft = {
  displayName: '',
  bio: '',
  location: '',
  interests: '',
};

export default function ProfilePage() {
  const token = useAuthStore((s) => s.token);
  const showToast = useToastStore((s) => s.show);
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ProfileDraft>(defaultDraft);
  const [isDraftLoading, setIsDraftLoading] = useState(true);
  const [lastSavedAt, setLastSavedAt] = useState<string | null>(null);
  const isInitialised = useRef(false);

  const headers = useMemo<HeadersInit>(
    () => ({
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }),
    [token],
  );

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: async (): Promise<ProfileResponse> => {
      const res = await fetch('/api/profile/me', { headers: token ? { Authorization: `Bearer ${token}` } : {} });
      if (!res.ok) throw new Error('Не удалось загрузить профиль');
      return res.json();
    },
  });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const stored = await DeviceStorage.getJSON<ProfileDraft>(DRAFT_KEY);
        if (!cancelled && stored) {
          setForm({ ...defaultDraft, ...stored });
        }
      } catch (error) {
        if (!cancelled) {
          const message = error instanceof DeviceStorageError ? error.message : 'Не удалось восстановить черновик профиля';
          showToast(message);
        }
      } finally {
        if (!cancelled) setIsDraftLoading(false);
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [showToast]);

  useEffect(() => {
    if (!profileQuery.data || profileQuery.isLoading) return;
    if (isInitialised.current) return;
    isInitialised.current = true;
    setForm((prev) => ({
      displayName: profileQuery.data?.displayName ?? prev.displayName ?? '',
      bio: profileQuery.data?.bio ?? prev.bio ?? '',
      location: profileQuery.data?.location ?? prev.location ?? '',
      interests: profileQuery.data?.interests ?? prev.interests ?? '',
    }));
  }, [profileQuery.data, profileQuery.isLoading]);

  const persistDraft = async (next: ProfileDraft) => {
    try {
      await DeviceStorage.setJSON(DRAFT_KEY, next);
      setLastSavedAt(new Date().toISOString());
    } catch (error) {
      const message = error instanceof DeviceStorageError ? error.message : 'Не удалось сохранить черновик профиля';
      showToast(message);
    }
  };

  const updateField = (patch: Partial<ProfileDraft>) => {
    setForm((prev) => {
      const next = { ...prev, ...patch };
      void persistDraft(next);
      return next;
    });
  };

  const mutation = useMutation({
    mutationFn: async (payload: ProfileDraft) => {
      const res = await fetch('/api/profile', {
        method: 'PUT',
        headers,
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error('Сохранить изменения не удалось');
      return res.json();
    },
    onSuccess: async () => {
      await DeviceStorage.removeItem(DRAFT_KEY);
      setLastSavedAt(null);
      showToast('Профиль обновлён');
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : 'Не удалось сохранить профиль';
      showToast(message);
    },
  });

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    mutation.mutate(form);
  };

  const handleDiscard = async () => {
    setForm(defaultDraft);
    setLastSavedAt(null);
    try {
      await DeviceStorage.removeItem(DRAFT_KEY);
      showToast('Черновик очищен');
    } catch (error) {
      const message = error instanceof DeviceStorageError ? error.message : 'Не удалось очистить черновик';
      showToast(message);
    }
  };

  if ((profileQuery.isLoading && isDraftLoading) || (!isInitialised.current && profileQuery.isLoading)) {
    return <Loader />;
  }

  if (profileQuery.isError) {
    return <div className={styles.page}>Не удалось загрузить профиль. Попробуйте позже.</div>;
  }

  return (
    <form className={styles.page} onSubmit={handleSubmit}>
      <section className={styles.card}>
        <div className={styles.header}>
          <h1 className={styles.title}>Редактирование профиля</h1>
          <p className={styles.muted}>Обновите информацию о себе — изменения сохранятся в Telegram и на сервере.</p>
        </div>

        <fieldset className={styles.fieldset}>
          <label className={styles.label} htmlFor="displayName">
            Имя
          </label>
          <input
            id="displayName"
            className={styles.input}
            value={form.displayName}
            placeholder="Например, Дмитрий"
            onChange={(event) => updateField({ displayName: event.target.value })}
          />

          <label className={styles.label} htmlFor="bio">
            О себе
          </label>
          <textarea
            id="bio"
            className={styles.textarea}
            rows={4}
            value={form.bio}
            placeholder="Расскажите, чем занимаетесь и что ищете"
            onChange={(event) => updateField({ bio: event.target.value })}
          />

          <label className={styles.label} htmlFor="location">
            Город
          </label>
          <input
            id="location"
            className={styles.input}
            value={form.location}
            placeholder="Например, Санкт-Петербург"
            onChange={(event) => updateField({ location: event.target.value })}
          />

          <label className={styles.label} htmlFor="interests">
            Интересы
          </label>
          <textarea
            id="interests"
            className={styles.textarea}
            rows={3}
            value={form.interests}
            placeholder="Музыка, спорт, путешествия"
            onChange={(event) => updateField({ interests: event.target.value })}
          />
        </fieldset>

        <div className={styles.actions}>
          <button type="submit" className={styles.primary} disabled={mutation.isPending}>
            Сохранить изменения
          </button>
          <button type="button" className={styles.secondary} onClick={handleDiscard}>
            Очистить черновик
          </button>
        </div>
        <div className={styles.meta}>
          <span>Текущий логин: {profileQuery.data?.username ?? '—'}</span>
          {lastSavedAt && <span>Черновик сохранён {new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit' }).format(new Date(lastSavedAt))}</span>}
        </div>
      </section>
    </form>
  );
}


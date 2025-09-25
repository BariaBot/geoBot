import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Loader from '../components/Loader';
import styles from './ProfilePage.module.css';
import { useAuthStore } from '../store/useAuthStore';
import { useToastStore } from '../store/useToastStore';
import DeviceStorage, { DeviceStorageError } from '../utils/deviceStorage';
import ProfileAboutFields, { ProfileAboutValue } from '../components/profile/ProfileAboutFields';
import ProfilePhotoManager from '../components/profile/ProfilePhotoManager';
import { DraftPhoto, createDraftPhoto, maxPhotoSizeBytes } from '../utils/photos';
import { useProfileMutations } from '../hooks/useProfileMutations';
import { useProfileMediaStore } from '../store/useProfileMediaStore';
import { ProfileData, ProfilePhoto, ProfileUpdatePayload } from '../types/profile';

const DRAFT_KEY = 'profile-draft';
const MAX_PHOTOS = 6;

type ProfileResponse = ProfileData;

interface ProfileDraft {
  displayName: string;
  bio: string;
  interests: string;
  goals: string;
  photos: DraftPhoto[];
  locationMode: 'auto' | 'manual';
  radiusKm: number;
  city: string;
  geo?: {
    lat: number;
    lon: number;
  };
}

const defaultDraft: ProfileDraft = {
  displayName: '',
  bio: '',
  interests: '',
  goals: '',
  photos: [],
  locationMode: 'auto',
  radiusKm: 10,
  city: '',
  geo: undefined,
};

const mapRemotePhotoToDraft = (photo: ProfilePhoto): DraftPhoto => ({
  id: photo.id,
  name: photo.id,
  type: 'image/jpeg',
  size: 0,
  dataUrl: photo.url,
  remoteUrl: photo.url,
  status: 'uploaded',
  isLocal: false,
});

const mapProfileToDraft = (data?: ProfileResponse): ProfileDraft => {
  if (!data) return { ...defaultDraft };
  const photos = (data.photos ?? [])
    .slice()
    .sort((a, b) => a.order - b.order)
    .map(mapRemotePhotoToDraft);
  return {
    displayName: data.displayName ?? '',
    bio: data.bio ?? '',
    interests: data.interests ?? '',
    goals: data.goals ?? '',
    photos,
    locationMode: data.preferences?.locationMode ?? 'auto',
    radiusKm: data.preferences?.radiusKm ?? 10,
    city: data.preferences?.city ?? '',
    geo: data.preferences?.geo,
  };
};

const sanitizeDraftForStorage = (draft: ProfileDraft): ProfileDraft => ({
  ...draft,
  photos: draft.photos,
});

const buildUpdatePayload = (draft: ProfileDraft): ProfileUpdatePayload => ({
  displayName: draft.displayName.trim(),
  bio: draft.bio.trim(),
  interests: draft.interests.trim(),
  goals: draft.goals.trim(),
  preferences: {
    locationMode: draft.locationMode,
    radiusKm: draft.radiusKm,
    city: draft.city.trim(),
    geo: draft.geo,
  },
});

export default function ProfilePage() {
  const token = useAuthStore((s) => s.token);
  const showToast = useToastStore((s) => s.show);
  const mediaStore = useProfileMediaStore();
  const [form, setForm] = useState<ProfileDraft>(defaultDraft);
  const formRef = useRef(form);
  const [isDraftLoading, setIsDraftLoading] = useState(true);
  const [lastSavedAt, setLastSavedAt] = useState<string | null>(null);
  const [isDetectingLocation, setIsDetectingLocation] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const isInitialised = useRef(false);

  const { updateProfile, uploadPhoto, reorderPhotos, deletePhoto } = useProfileMutations({ token });

  const profileQuery = useQuery({
    queryKey: ['profile'],
    queryFn: async (): Promise<ProfileResponse> => {
      const res = await fetch('/api/profile/me', { headers: token ? { Authorization: `Bearer ${token}` } : {} });
      if (!res.ok) throw new Error('Не удалось загрузить профиль');
      return res.json();
    },
  });

  const persistDraft = useCallback(async (draftValue: ProfileDraft) => {
    try {
      await DeviceStorage.setJSON(DRAFT_KEY, sanitizeDraftForStorage(draftValue));
      setLastSavedAt(new Date().toISOString());
    } catch (error) {
      const message = error instanceof DeviceStorageError ? error.message : 'Не удалось сохранить черновик профиля';
      showToast(message);
    }
  }, [showToast]);

  const applyDraft = useCallback(
    (producer: (prev: ProfileDraft) => ProfileDraft) => {
      setForm((prev) => {
        const next = producer(prev);
        void persistDraft(next);
        return next;
      });
    },
    [persistDraft],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const stored = await DeviceStorage.getJSON<ProfileDraft>(DRAFT_KEY);
        if (!cancelled && stored) {
          setForm({ ...defaultDraft, ...stored, photos: stored.photos ?? [] });
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
    formRef.current = form;
  }, [form]);

  useEffect(() => {
    const data = profileQuery.data;
    if (!data || profileQuery.isLoading || isInitialised.current) return;
    isInitialised.current = true;
    setForm((prev) => ({
      ...prev,
      ...mapProfileToDraft(data),
      photos: prev.photos.length ? prev.photos : mapProfileToDraft(data).photos,
    }));
  }, [profileQuery.data, profileQuery.isLoading]);

  useEffect(() => {
    if (form.locationMode !== 'auto' || form.geo || isDetectingLocation) return;
    if (!('geolocation' in navigator)) {
      showToast('Геолокация недоступна, заполните город вручную');
      applyDraft((prev) => ({ ...prev, locationMode: 'manual', geo: undefined }));
      return;
    }
    setIsDetectingLocation(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        applyDraft((prev) => ({
          ...prev,
          geo: { lat: pos.coords.latitude, lon: pos.coords.longitude },
          locationMode: 'auto',
        }));
        setIsDetectingLocation(false);
      },
      () => {
        showToast('Не удалось определить геолокацию, заполните город вручную');
        applyDraft((prev) => ({ ...prev, locationMode: 'manual', geo: undefined }));
        setIsDetectingLocation(false);
      },
      { enableHighAccuracy: true, timeout: 8000 },
    );
  }, [applyDraft, form.geo, form.locationMode, isDetectingLocation, showToast]);

  const updateDisplayName = (displayName: string) => applyDraft((prev) => ({ ...prev, displayName }));
  const updateAboutValues = (value: ProfileAboutValue) => applyDraft((prev) => ({ ...prev, ...value }));
  const updatePhotos = (updater: DraftPhoto[] | ((prev: DraftPhoto[]) => DraftPhoto[])) =>
    applyDraft((prev) => ({
      ...prev,
      photos: typeof updater === 'function' ? updater(prev.photos) : updater,
    }));

  const updateLocationValues = (patch: Partial<ProfileDraft>) => applyDraft((prev) => ({ ...prev, ...patch }));

  const handleLocationModeChange = (mode: 'auto' | 'manual') => {
    if (mode === form.locationMode) return;
    if (mode === 'auto') {
      setIsDetectingLocation(false);
      applyDraft((prev) => ({ ...prev, locationMode: 'auto' }));
    } else {
      applyDraft((prev) => ({ ...prev, locationMode: 'manual', geo: undefined }));
    }
  };

  const handleAddPhotos = async (files: File[]) => {
    const remainingSlots = MAX_PHOTOS - form.photos.length;
    const acceptedFiles = files.slice(0, remainingSlots);
    const newPhotos: DraftPhoto[] = [];
    for (const file of acceptedFiles) {
      if (file.size > maxPhotoSizeBytes) {
        showToast(`Файл ${file.name} превышает 1 MB и не будет добавлен.`);
        continue;
      }
      try {
        const draftPhoto = await createDraftPhoto(file);
        newPhotos.push(draftPhoto);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Не удалось прочитать файл';
        showToast(message);
      }
    }
    if (newPhotos.length) {
      updatePhotos((prev) => [...prev, ...newPhotos]);
    }
  };

  const handleRemovePhoto = async (photo: DraftPhoto) => {
    if (!photo.isLocal && photo.remoteUrl) {
      try {
        await deletePhoto.mutateAsync(photo.id);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Не удалось удалить фото';
        showToast(message);
        return;
      }
    }
    mediaStore.clear(photo.id);
    updatePhotos((prev) => prev.filter((item) => item.id !== photo.id));
  };

  const validateForm = () => {
    const issues: string[] = [];
    if (!form.displayName.trim()) issues.push('Укажите имя.');
    if (!form.bio.trim()) issues.push('Добавьте описание профиля.');
    if (!form.interests.trim()) issues.push('Укажите интересы.');
    if (form.photos.length === 0) issues.push('Добавьте хотя бы одно фото.');
    if (form.locationMode === 'manual' && !form.city.trim()) issues.push('Укажите город.');
    if (!form.radiusKm || form.radiusKm < 5) issues.push('Радиус поиска должен быть не меньше 5 км.');
    return issues;
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const validationIssues = validateForm();
    if (validationIssues.length) {
      validationIssues.forEach((issue) => showToast(issue));
      return;
    }
    setIsSaving(true);
    try {
      const payload = buildUpdatePayload(form);
      await updateProfile.mutateAsync(payload);

      let nextPhotos = formRef.current.photos;
      const pendingPhotos = nextPhotos.filter((photo) => photo.isLocal || !photo.remoteUrl);
      for (const photo of pendingPhotos) {
        mediaStore.setStatus(photo.id, 'uploading', { progress: 0 });
        try {
          const response = await uploadPhoto.mutateAsync(photo);
          nextPhotos = nextPhotos.map((item) =>
            item.id === photo.id
              ? {
                  ...item,
                  id: response.id,
                  dataUrl: response.url,
                  remoteUrl: response.url,
                  isLocal: false,
                  status: 'uploaded',
                }
              : item,
          );
          updatePhotos(nextPhotos);
        } catch (error) {
          mediaStore.setStatus(photo.id, 'error', { error: (error as Error).message });
          throw error;
        }
      }
      mediaStore.clear();

      const remoteIds = nextPhotos.filter((photo) => !photo.isLocal || photo.remoteUrl).map((photo) => photo.id);
      if (remoteIds.length) {
        try {
          await reorderPhotos.mutateAsync(remoteIds);
        } catch (error) {
          const message = error instanceof Error ? error.message : 'Не удалось обновить порядок фотографий';
          showToast(message);
        }
      }

      await DeviceStorage.removeItem(DRAFT_KEY);
      setLastSavedAt(null);
      showToast('Профиль обновлён');
    } catch (error) {
      if (error instanceof Error) {
        showToast(error.message);
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleDiscard = async () => {
    const data = profileQuery.data;
    const fallback = mapProfileToDraft(data);
    setForm(fallback);
    setLastSavedAt(null);
    await DeviceStorage.removeItem(DRAFT_KEY);
    mediaStore.clear();
  };

  const aboutValue: ProfileAboutValue = {
    bio: form.bio,
    interests: form.interests,
    goals: form.goals,
  };

  const isUploading = uploadPhoto.isPending;

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
        <div className={styles.fieldset}>
          <label className={styles.label} htmlFor="displayName">
            Имя
          </label>
          <input
            id="displayName"
            className={styles.input}
            value={form.displayName}
            placeholder="Например, Дмитрий"
            onChange={(event) => updateDisplayName(event.target.value)}
          />
        </div>
      </section>

      <section className={styles.card}>
        <h2 className={styles.title}>Фотографии</h2>
        <ProfilePhotoManager
          photos={form.photos}
          maxPhotos={MAX_PHOTOS}
          onUpload={handleAddPhotos}
          onRemove={handleRemovePhoto}
          onReorder={updatePhotos}
          isUploading={isUploading}
        />
      </section>

      <section className={styles.card}>
        <h2 className={styles.title}>О себе</h2>
        <ProfileAboutFields value={aboutValue} onChange={updateAboutValues} />
      </section>

      <section className={styles.card}>
        <h2 className={styles.title}>Предпочтения</h2>
        <div className={styles.fieldset}>
          <div className={styles.preferenceRow}>
            <span className={styles.label}>Способ определения местоположения</span>
            <div className={styles.toggleGroup}>
              <button
                type="button"
                className={styles.toggleButton}
                data-active={form.locationMode === 'auto'}
                onClick={() => handleLocationModeChange('auto')}
              >
                Геолокация
              </button>
              <button
                type="button"
                className={styles.toggleButton}
                data-active={form.locationMode === 'manual'}
                onClick={() => handleLocationModeChange('manual')}
              >
                Вручную
              </button>
            </div>
            {isDetectingLocation && form.locationMode === 'auto' && (
              <span className={styles.meta}>Определяем координаты…</span>
            )}
            {form.locationMode === 'auto' && form.geo && (
              <span className={styles.meta}>
                Координаты: {form.geo.lat.toFixed(3)}, {form.geo.lon.toFixed(3)}
              </span>
            )}
          </div>
          <div className={styles.preferenceRow}>
            <label className={styles.label} htmlFor="city">
              Город
            </label>
            <input
              id="city"
              className={styles.input}
              value={form.city}
              placeholder="Например, Санкт-Петербург"
              disabled={form.locationMode === 'auto'}
              onChange={(event) => updateLocationValues({ city: event.target.value })}
            />
          </div>
          <div className={styles.preferenceRow}>
            <label className={styles.label} htmlFor="radius">
              Радиус поиска (км)
            </label>
            <input
              id="radius"
              className={styles.input}
              type="number"
              min={5}
              max={200}
              value={form.radiusKm}
              onChange={(event) => updateLocationValues({ radiusKm: Number(event.target.value) })}
            />
          </div>
        </div>
        <p className={styles.meta}>Текущий логин: {profileQuery.data?.username ?? '—'}</p>
        {lastSavedAt && (
          <p className={styles.meta}>
            Черновик сохранён {new Intl.DateTimeFormat('ru-RU', { hour: '2-digit', minute: '2-digit' }).format(new Date(lastSavedAt))}
          </p>
        )}
      </section>

      <section className={styles.card}>
        <div className={styles.actions}>
          <button type="submit" className={styles.primary} disabled={isSaving}>
            {isSaving ? 'Сохраняем…' : 'Сохранить изменения'}
          </button>
          <button type="button" className={styles.secondary} onClick={handleDiscard}>
            Очистить черновик
          </button>
        </div>
      </section>
    </form>
  );
}

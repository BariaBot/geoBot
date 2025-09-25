import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './OnboardingPage.module.css';
import { useToastStore } from '../store/useToastStore';
import { useAuthStore } from '../store/useAuthStore';
import DeviceStorage, { DeviceStorageError } from '../utils/deviceStorage';
import { createDraftPhoto, dataUrlToFile, DraftPhoto, maxPhotoSizeBytes } from '../utils/photos';
import ProfileAboutFields from '../components/profile/ProfileAboutFields';
import ProfilePhotoManager from '../components/profile/ProfilePhotoManager';

const DRAFT_KEY = 'onboarding-draft';
const MAX_PHOTOS = 6;
const MIN_NAME_LENGTH = 2;
const MIN_AGE = 18;

type Gender = 'male' | 'female' | 'other' | '';

type LocationMode = 'auto' | 'manual';

type OnboardingDraft = {
  step: number;
  name: string;
  birthDate: string;
  gender: Gender;
  bio: string;
  interests: string;
  goals: string;
  photos: DraftPhoto[];
  locationMode: LocationMode;
  radiusKm: number;
  city: string;
  geo?: {
    lat: number;
    lon: number;
  };
};

const defaultDraft: OnboardingDraft = {
  step: 0,
  name: '',
  birthDate: '',
  gender: '',
  bio: '',
  interests: '',
  goals: '',
  photos: [],
  locationMode: 'auto',
  radiusKm: 10,
  city: '',
  geo: undefined,
};

const calculateAge = (birthDate: string) => {
  if (!birthDate) return 0;
  const birth = new Date(birthDate);
  if (Number.isNaN(birth.getTime())) return 0;
  const today = new Date();
  let age = today.getFullYear() - birth.getFullYear();
  const monthDiff = today.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age -= 1;
  }
  return age;
};

export default function OnboardingPage() {
  const showToast = useToastStore((s) => s.show);
  const token = useAuthStore((s) => s.token);
  const navigate = useNavigate();
  const [draft, setDraft] = useState<OnboardingDraft>(defaultDraft);
  const [isLoadingDraft, setIsLoadingDraft] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDetectingLocation, setIsDetectingLocation] = useState(false);

  const authHeaders = useMemo<HeadersInit>(
    () => ({
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      'Content-Type': 'application/json',
    }),
    [token],
  );

  const mediaHeaders = useMemo<HeadersInit>(() => ({ ...(token ? { Authorization: `Bearer ${token}` } : {}) }), [token]);

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

  useEffect(() => {
    if (draft.locationMode !== 'auto' || draft.geo || isDetectingLocation) return;
    if (!('geolocation' in navigator)) {
      showToast('Геолокация недоступна, заполните город вручную');
      setDraft((prev) => ({ ...prev, locationMode: 'manual' }));
      return;
    }
    setIsDetectingLocation(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setDraft((prev) => ({
          ...prev,
          geo: { lat: pos.coords.latitude, lon: pos.coords.longitude },
          locationMode: 'auto',
        }));
        setIsDetectingLocation(false);
      },
      () => {
        showToast('Не удалось определить геолокацию, заполните город вручную');
        setDraft((prev) => ({ ...prev, locationMode: 'manual', geo: undefined }));
        setIsDetectingLocation(false);
      },
      { enableHighAccuracy: true, timeout: 8000 },
    );
  }, [draft.geo, draft.locationMode, isDetectingLocation, showToast]);

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

  const errors = useMemo(() => {
    const currentErrors: Record<number, string[]> = {};

    const age = calculateAge(draft.birthDate);
    const baseErrors: string[] = [];
    if (draft.name.trim().length < MIN_NAME_LENGTH) baseErrors.push('Имя должно быть не короче 2 символов.');
    if (!draft.birthDate) baseErrors.push('Укажите дату рождения.');
    if (age && age < MIN_AGE) baseErrors.push('Онбординг доступен пользователям старше 18 лет.');
    if (!draft.gender) baseErrors.push('Выберите гендер.');
    if (baseErrors.length) currentErrors[0] = baseErrors;

    if (draft.photos.length === 0) currentErrors[1] = ['Добавьте хотя бы одно фото.'];

    const preferencesErrors: string[] = [];
    if (!draft.bio.trim()) preferencesErrors.push('Заполните описание о себе.');
    if (!draft.interests.trim()) preferencesErrors.push('Укажите интересы.');
    if (!draft.goals.trim()) preferencesErrors.push('Опишите цели знакомства.');
    if (preferencesErrors.length) currentErrors[2] = preferencesErrors;

    const locationErrors: string[] = [];
    if (draft.locationMode === 'manual') {
      if (!draft.city.trim()) locationErrors.push('Укажите город.');
    } else if (!draft.geo) {
      locationErrors.push('Не удалось определить геолокацию.');
    }
    if (!draft.radiusKm || draft.radiusKm < 5) locationErrors.push('Укажите радиус поиска (минимум 5 км).');
    if (locationErrors.length) currentErrors[3] = locationErrors;

    return currentErrors;
  }, [draft]);

  const isStepValid = (stepIndex: number) => !(stepIndex in errors);

  const handleNext = async () => {
    const nextStep = Math.min(draft.step + 1, steps.length - 1);
    if (!isStepValid(draft.step)) {
      errors[draft.step]?.forEach((err) => showToast(err));
      return;
    }
    if (nextStep !== draft.step) {
      const updated = { ...draft, step: nextStep };
      setDraft(updated);
      await persistDraft(updated);
    }
  };

  const handleBack = async () => {
    const prevStep = Math.max(draft.step - 1, 0);
    if (prevStep !== draft.step) {
      const updated = { ...draft, step: prevStep };
      setDraft(updated);
      await persistDraft(updated);
    }
  };

  const addPhotos = async (files: File[]) => {
    const remainingSlots = MAX_PHOTOS - draft.photos.length;
    const acceptedFiles = files.slice(0, remainingSlots);
    const photos: DraftPhoto[] = [];

    for (const file of acceptedFiles) {
      if (file.size > maxPhotoSizeBytes) {
        showToast(`Файл ${file.name} превышает 1 MB и не будет добавлен.`);
        continue;
      }
      try {
        const draftPhoto = await createDraftPhoto(file);
        photos.push(draftPhoto);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Не удалось прочитать файл';
        showToast(message);
      }
    }

    if (photos.length) {
      updateDraft({ photos: [...draft.photos, ...photos] });
    }
  };

  const handleRemovePhoto = (photo: DraftPhoto) => {
    updateDraft({ photos: draft.photos.filter((item) => item.id !== photo.id) });
  };

  const handleLocationModeToggle = (mode: LocationMode) => {
    updateDraft({ locationMode: mode, geo: mode === 'auto' ? draft.geo : undefined });
    if (mode === 'auto' && !draft.geo) {
      setIsDetectingLocation(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!isStepValid(3)) {
      errors[3]?.forEach((err) => showToast(err));
      return;
    }
    setIsSubmitting(true);
    try {
      const payload = {
        name: draft.name.trim(),
        birthDate: draft.birthDate,
        gender: draft.gender,
        bio: draft.bio.trim(),
        interests: draft.interests.trim(),
        goals: draft.goals.trim(),
        radiusKm: draft.radiusKm,
        location: draft.locationMode === 'auto' && draft.geo
          ? { mode: 'auto', lat: draft.geo.lat, lon: draft.geo.lon }
          : { mode: 'manual', city: draft.city.trim(), radiusKm: draft.radiusKm },
      };

      const response = await fetch('/api/profile/onboarding', {
        method: 'POST',
        headers: authHeaders,
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error('Не удалось сохранить профиль.');
      }

      for (const photo of draft.photos) {
        const file = await dataUrlToFile(photo.dataUrl, photo.name);
        const formData = new FormData();
        formData.append('file', file);
        const uploadResponse = await fetch('/api/profile/media', {
          method: 'POST',
          headers: mediaHeaders,
          body: formData,
        });
        if (!uploadResponse.ok) {
          throw new Error(`Не удалось загрузить фото ${photo.name}`);
        }
      }

      await DeviceStorage.removeItem(DRAFT_KEY);
      showToast('Профиль создан!');
      navigate('/');
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Не удалось завершить онбординг';
      showToast(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const steps = [
    {
      title: 'Основные данные',
      description: 'Расскажите о себе: имя, дата рождения и гендер видны только вам и используются для подбора мэтчей.',
      render: (
        <div className={styles.fieldset}>
          <label className={styles.label} htmlFor="name">
            Имя или никнейм
          </label>
          <input
            id="name"
            className={styles.input}
            value={draft.name}
            placeholder="Например, Юлия"
            onChange={(event) => updateDraft({ name: event.target.value })}
          />
          <div className={styles.twoColumns}>
            <div>
              <label className={styles.label} htmlFor="birthDate">
                Дата рождения
              </label>
              <input
                id="birthDate"
                className={styles.input}
                type="date"
                value={draft.birthDate}
                max={new Date().toISOString().split('T')[0]}
                onChange={(event) => updateDraft({ birthDate: event.target.value })}
              />
            </div>
            <div>
              <label className={styles.label} htmlFor="gender">
                Пол
              </label>
              <select
                id="gender"
                className={styles.input}
                value={draft.gender}
                onChange={(event) => updateDraft({ gender: event.target.value as Gender })}
              >
                <option value="">Выберите</option>
                <option value="female">Женский</option>
                <option value="male">Мужской</option>
                <option value="other">Другое</option>
              </select>
            </div>
          </div>
          {errors[0]?.map((err) => (
            <p key={err} className={styles.error}>
              {err}
            </p>
          ))}
        </div>
      ),
      primaryActionLabel: 'Далее',
    },
    {
      title: 'Добавьте фото',
      description: 'Минимум одно фото. Чем больше — тем выше шанс получить ответный лайк.',
      render: (
        <>
          <ProfilePhotoManager
            photos={draft.photos}
            maxPhotos={MAX_PHOTOS}
            onUpload={addPhotos}
            onRemove={handleRemovePhoto}
            onReorder={(photos) => updateDraft({ photos })}
          />
          {errors[1]?.map((err) => (
            <p key={err} className={styles.error}>
              {err}
            </p>
          ))}
        </>
      ),
      primaryActionLabel: 'Далее',
    },
    {
      title: 'Интересы и цели',
      description: 'Эта информация помогает подобрать совместимые анкеты и темы для начала общения.',
      render: (
        <ProfileAboutFields
          value={{ bio: draft.bio, interests: draft.interests, goals: draft.goals }}
          onChange={(value) => updateDraft(value)}
          errors={errors[2]}
        />
      ),
      primaryActionLabel: 'Далее',
    },
    {
      title: 'География знакомств',
      description: 'Уточните, где вы находитесь и на каком расстоянии готовы знакомиться.',
      render: (
        <div className={styles.fieldset}>
          <div className={styles.twoColumns}>
            <button
              type="button"
              className={styles.primary}
              disabled={draft.locationMode === 'auto' && isDetectingLocation}
              onClick={() => handleLocationModeToggle('auto')}
            >
              Использовать геолокацию
            </button>
            <button type="button" className={styles.secondary} onClick={() => handleLocationModeToggle('manual')}>
              Указать вручную
            </button>
          </div>
          {draft.locationMode === 'auto' ? (
            <p className={styles.helper}>
              {isDetectingLocation && 'Определяем ваше местоположение…'}
              {!isDetectingLocation && draft.geo && `Определено: ${draft.geo.lat.toFixed(3)}, ${draft.geo.lon.toFixed(3)}`}
              {!isDetectingLocation && !draft.geo && 'Не удалось определить координаты.'}
            </p>
          ) : (
            <div className={styles.twoColumns}>
              <div>
                <label className={styles.label} htmlFor="city">
                  Город
                </label>
                <input
                  id="city"
                  className={styles.input}
                  value={draft.city}
                  placeholder="Например, Екатеринбург"
                  onChange={(event) => updateDraft({ city: event.target.value })}
                />
              </div>
            </div>
          )}
          <div>
            <label className={styles.label} htmlFor="radius">
              Радиус поиска (км)
            </label>
            <input
              id="radius"
              className={styles.input}
              type="number"
              min={5}
              max={200}
              value={draft.radiusKm}
              onChange={(event) => updateDraft({ radiusKm: Number(event.target.value) })}
            />
          </div>
          {errors[3]?.map((err) => (
            <p key={err} className={styles.error}>
              {err}
            </p>
          ))}
        </div>
      ),
      primaryActionLabel: 'Завершить',
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
        <p className={styles.stepCaption}>{currentStep.description}</p>
        {currentStep.render}
        <div className={styles.actions}>
          {draft.step === steps.length - 1 ? (
            <button type="submit" className={styles.primary} disabled={isSubmitting}>
              {isSubmitting ? 'Сохраняем…' : currentStep.primaryActionLabel}
            </button>
          ) : (
            <button type="button" className={styles.primary} onClick={handleNext}>
              {currentStep.primaryActionLabel}
            </button>
          )}
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

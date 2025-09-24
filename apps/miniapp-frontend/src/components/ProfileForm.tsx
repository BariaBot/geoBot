import { FormEvent, useMemo } from 'react';
import type { ProfileDraft } from '../store/profile';

interface ProfileFormProps {
  draft: ProfileDraft;
  status: 'idle' | 'loading' | 'ready' | 'submitting' | 'error';
  error?: string;
  ctaLabel: string;
  onChange: (partial: Partial<ProfileDraft>) => void;
  onSubmit: () => Promise<void>;
}

export function ProfileForm({ draft, status, error, ctaLabel, onChange, onSubmit }: ProfileFormProps) {
  const isBusy = status === 'submitting';

  const interestsValue = useMemo(() => draft.interests.join(', '), [draft.interests]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit();
  };

  return (
    <form className="profile-form" onSubmit={handleSubmit}>
      <label className="profile-form__field">
        <span>Имя</span>
        <input
          type="text"
          value={draft.displayName}
          onChange={(event) => onChange({ displayName: event.target.value })}
          placeholder="Как к тебе обращаться?"
          required
        />
      </label>

      <label className="profile-form__field">
        <span>Био</span>
        <textarea
          value={draft.bio}
          onChange={(event) => onChange({ bio: event.target.value })}
          placeholder="Расскажи о себе"
          maxLength={1024}
          rows={3}
        />
      </label>

      <label className="profile-form__field">
        <span>Город</span>
        <input
          type="text"
          value={draft.city ?? ''}
          onChange={(event) => onChange({ city: event.target.value })}
          placeholder="Москва"
        />
      </label>

      <label className="profile-form__field">
        <span>Дата рождения</span>
        <input
          type="date"
          value={draft.birthday ?? ''}
          onChange={(event) => onChange({ birthday: event.target.value })}
        />
      </label>

      <label className="profile-form__field">
        <span>Интересы (через запятую)</span>
        <input
          type="text"
          value={interestsValue}
          onChange={(event) => {
            const values = event.target.value
              .split(',')
              .map((value) => value.trim())
              .filter((value) => value.length > 0);
            onChange({ interests: values });
          }}
          placeholder="Путешествия, музыка, бег"
        />
      </label>

      <div className="profile-form__coarse-loc">
        <label>
          <span>Широта</span>
          <input
            type="number"
            inputMode="decimal"
            step="0.0001"
            value={draft.latitude ?? ''}
            onChange={(event) => onChange({ latitude: event.target.value === '' ? undefined : Number(event.target.value) })}
          />
        </label>
        <label>
          <span>Долгота</span>
          <input
            type="number"
            inputMode="decimal"
            step="0.0001"
            value={draft.longitude ?? ''}
            onChange={(event) => onChange({ longitude: event.target.value === '' ? undefined : Number(event.target.value) })}
          />
        </label>
      </div>

      {error ? <p className="profile-form__error">{error}</p> : null}

      <button type="submit" className="profile-form__submit" disabled={isBusy || draft.displayName.trim().length < 2}>
        {isBusy ? 'Сохраняем…' : ctaLabel}
      </button>
    </form>
  );
}

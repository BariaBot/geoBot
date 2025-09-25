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

export const ProfileForm = ({
  draft,
  status,
  error,
  ctaLabel,
  onChange,
  onSubmit,
}: ProfileFormProps) => {
  const isBusy = status === 'submitting';

  const interestsValue = useMemo(() => draft.interests.join(', '), [draft.interests]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    await onSubmit();
  };

  return (
    <form className="profile-form" onSubmit={handleSubmit}>
      <label className="profile-form__field" htmlFor="profile-form-name">
        <span>Имя</span>
        <input
          id="profile-form-name"
          type="text"
          value={draft.displayName}
          onChange={(event) => {
            const { value } = event.target;
            onChange({ displayName: value });
          }}
          placeholder="Как к тебе обращаться?"
          required
        />
      </label>

      <label className="profile-form__field" htmlFor="profile-form-bio">
        <span>Био</span>
        <textarea
          id="profile-form-bio"
          value={draft.bio}
          onChange={(event) => {
            const { value } = event.target;
            onChange({ bio: value });
          }}
          placeholder="Расскажи о себе"
          maxLength={1024}
          rows={3}
        />
      </label>

      <label className="profile-form__field" htmlFor="profile-form-city">
        <span>Город</span>
        <input
          id="profile-form-city"
          type="text"
          value={draft.city ?? ''}
          onChange={(event) => {
            const { value } = event.target;
            onChange({ city: value });
          }}
          placeholder="Москва"
        />
      </label>

      <label className="profile-form__field" htmlFor="profile-form-birthday">
        <span>Дата рождения</span>
        <input
          id="profile-form-birthday"
          type="date"
          value={draft.birthday ?? ''}
          onChange={(event) => {
            const { value } = event.target;
            onChange({ birthday: value });
          }}
        />
      </label>

      <label className="profile-form__field" htmlFor="profile-form-interests">
        <span>Интересы (через запятую)</span>
        <input
          id="profile-form-interests"
          type="text"
          value={interestsValue}
          onChange={(event) => {
            const { value } = event.target;
            const values = value
              .split(',')
              .map((valuePart) => valuePart.trim())
              .filter((valuePart) => valuePart.length > 0);
            onChange({ interests: values });
          }}
          placeholder="Путешествия, музыка, бег"
        />
      </label>

      <div className="profile-form__coarse-loc">
        <label htmlFor="profile-form-latitude">
          <span>Широта</span>
          <input
            id="profile-form-latitude"
            type="number"
            inputMode="decimal"
            step="0.0001"
            value={draft.latitude ?? ''}
            onChange={(event) => {
              const { value } = event.target;
              onChange({ latitude: value === '' ? undefined : Number(value) });
            }}
          />
        </label>
        <label htmlFor="profile-form-longitude">
          <span>Долгота</span>
          <input
            id="profile-form-longitude"
            type="number"
            inputMode="decimal"
            step="0.0001"
            value={draft.longitude ?? ''}
            onChange={(event) => {
              const { value } = event.target;
              onChange({ longitude: value === '' ? undefined : Number(value) });
            }}
          />
        </label>
      </div>

      {error ? <p className="profile-form__error">{error}</p> : null}

      <button type="submit" className="profile-form__submit" disabled={isBusy || draft.displayName.trim().length < 2}>
        {isBusy ? 'Сохраняем…' : ctaLabel}
      </button>
    </form>
  );
};

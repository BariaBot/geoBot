import styles from './ProfileAboutFields.module.css';

export interface ProfileAboutValue {
  bio: string;
  interests: string;
  goals: string;
}

interface ProfileAboutFieldsProps {
  value: ProfileAboutValue;
  onChange: (value: ProfileAboutValue) => void;
  errors?: string[];
}

export function ProfileAboutFields({ value, onChange, errors }: ProfileAboutFieldsProps) {
  const update = (patch: Partial<ProfileAboutValue>) => onChange({ ...value, ...patch });

  return (
    <div className={styles.fieldset}>
      <label className={styles.label} htmlFor="profile-bio">
        О себе
      </label>
      <textarea
        id="profile-bio"
        className={styles.textarea}
        rows={4}
        placeholder="Пару предложений о том, чем занимаетесь и что любите."
        value={value.bio}
        onChange={(event) => update({ bio: event.target.value })}
      />

      <label className={styles.label} htmlFor="profile-interests">
        Интересы (через запятую)
      </label>
      <textarea
        id="profile-interests"
        className={styles.textarea}
        rows={3}
        placeholder="Например: серфинг, джаз, крафтовый кофе"
        value={value.interests}
        onChange={(event) => update({ interests: event.target.value })}
      />

      <label className={styles.label} htmlFor="profile-goals">
        Цели знакомства
      </label>
      <textarea
        id="profile-goals"
        className={styles.textarea}
        rows={3}
        placeholder="Что вы ищете: общение, путешествия, серьёзные отношения"
        value={value.goals}
        onChange={(event) => update({ goals: event.target.value })}
      />

      {errors?.map((error) => (
        <p key={error} className={styles.error}>
          {error}
        </p>
      ))}
    </div>
  );
}

export default ProfileAboutFields;

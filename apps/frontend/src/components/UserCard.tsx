import { memo } from 'react';

interface UserCardProps {
  username: string;
  bio?: string;
  onLike: () => void;
  onSkip: () => void;
}

function UserCard({ username, bio, onLike, onSkip }: UserCardProps) {
  const tg = (window as any).Telegram?.WebApp;
  const haptic = (type: 'like' | 'skip') =>
    tg?.HapticFeedback?.impactOccurred(type === 'like' ? 'soft' : 'light');

  return (
    <div className="card" role="group" aria-label={`Анкета ${username}`}>
      <div className="card-body">
        <h2>{username}</h2>
        {bio && <p>{bio}</p>}
      </div>
      <div className="card-actions">
        <button
          type="button"
          className="btn skip"
          onClick={() => {
            haptic('skip');
            onSkip();
          }}
        >
          ✖
          <span className="visually-hidden">Пропустить</span>
        </button>
        <button
          type="button"
          className="btn like"
          onClick={() => {
            haptic('like');
            onLike();
          }}
        >
          ❤
          <span className="visually-hidden">Лайк</span>
        </button>
      </div>
    </div>
  );
}

export default memo(UserCard);

import type { SwipeFeedItem } from '../api/swipes';

interface SwipeDeckProps {
  items: SwipeFeedItem[];
  loading: boolean;
  error?: string;
  onLike: (item: SwipeFeedItem) => void;
  onSkip: () => void;
  onRefresh: () => void;
}

export function SwipeDeck({ items, loading, error, onLike, onSkip, onRefresh }: SwipeDeckProps) {
  const current = items[0];

  if (loading && !current) {
    return <div className="swipe-deck__placeholder">Загружаем рекомендации…</div>;
  }

  if (!loading && !current) {
    return (
      <div className="swipe-deck__placeholder">
        <p>Лента пустая. Попробуй обновить.</p>
        <button type="button" onClick={onRefresh}>Обновить</button>
      </div>
    );
  }

  return (
    <div className="swipe-card">
      <div className="swipe-card__header">
        <h2>{current.displayName}</h2>
        {current.city ? <span>{current.city}</span> : null}
      </div>
      {current.bio ? <p className="swipe-card__bio">{current.bio}</p> : null}
      <div className="swipe-card__meta">
        {current.distanceMeters != null ? <span>{Math.round(current.distanceMeters / 1000)} км от тебя</span> : null}
        {current.lastSeen ? <span>был(а) онлайн {new Date(current.lastSeen).toLocaleString()}</span> : null}
      </div>
      {current.interests.length > 0 ? (
        <div className="swipe-card__tags">
          {current.interests.map((tag) => (
            <span key={tag} className="swipe-card__tag">#{tag}</span>
          ))}
        </div>
      ) : null}
      {error ? <p className="swipe-card__error">{error}</p> : null}
      <div className="swipe-card__actions">
        <button type="button" className="swipe-card__button swipe-card__button--skip" onClick={onSkip}>Пропустить</button>
        <button type="button" className="swipe-card__button swipe-card__button--like" onClick={() => onLike(current)}>Супер!</button>
      </div>
      <div className="swipe-card__progress">{items.length} карточек в очереди</div>
    </div>
  );
}

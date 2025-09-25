import type { SwipeQueueItemDto } from '../api/swipes';

interface SwipeDeckProps {
  items: SwipeQueueItemDto[];
  loading: boolean;
  error: string | null;
  undoAvailable: boolean;
  onLike: (item: SwipeQueueItemDto) => void;
  onDislike: (item: SwipeQueueItemDto) => void;
  onUndo: () => void;
  onRefresh: () => void;
}

export const SwipeDeck = ({
  items,
  loading,
  error = null,
  undoAvailable,
  onLike,
  onDislike,
  onUndo,
  onRefresh,
}: SwipeDeckProps) => {
  const current = items[0];

  if (loading && !current) {
    return <div className="swipe-deck__placeholder">Загружаем рекомендации…</div>;
  }

  if (!loading && !current) {
    return (
      <div className="swipe-deck__placeholder">
        <p>Лента пустая. Попробуй обновить.</p>
        <button type="button" onClick={onRefresh}>
          Обновить
        </button>
      </div>
    );
  }

  if (!current) {
    return null;
  }

  return (
    <div className="swipe-card">
      <div className="swipe-card__header">
        <h2>{current.profile.name}</h2>
        {current.profile.location?.cityName ? (
          <span>{current.profile.location.cityName}</span>
        ) : null}
      </div>
      {current.profile.bio ? (
        <p className="swipe-card__bio">{current.profile.bio}</p>
      ) : null}
      <div className="swipe-card__meta">
        {current.distanceKm != null ? (
          <span>{`${current.distanceKm.toFixed(1)} км от тебя`}</span>
        ) : null}
        {current.profile.updatedAt ? (
          <span>{`обновлено ${new Date(current.profile.updatedAt).toLocaleString()}`}</span>
        ) : null}
      </div>
      {current.profile.interests.length > 0 ? (
        <div className="swipe-card__tags">
          {current.profile.interests.map((tag) => (
            <span key={tag} className="swipe-card__tag">{`#${tag}`}</span>
          ))}
        </div>
      ) : null}
      {error ? <p className="swipe-card__error">{error}</p> : null}
      <div className="swipe-card__actions">
        <button
          type="button"
          className="swipe-card__button swipe-card__button--undo"
          onClick={onUndo}
          disabled={!undoAvailable}
        >
          Назад
        </button>
        <button
          type="button"
          className="swipe-card__button swipe-card__button--dislike"
          onClick={() => onDislike(current)}
        >
          Не моё
        </button>
        <button
          type="button"
          className="swipe-card__button swipe-card__button--like"
          onClick={() => onLike(current)}
        >
          Супер!
        </button>
      </div>
      <div className="swipe-card__progress">
        {`${items.length} карточек в очереди`}
      </div>
    </div>
  );
};

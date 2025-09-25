import { memo, useEffect, useMemo, useRef, useState } from 'react';
import styles from './UserCard.module.css';

interface UserCardProps {
  username?: string;
  age?: number;
  bio?: string;
  interests?: string[];
  photos?: string[];
  distanceKm?: number;
  isLoading?: boolean;
  onLike: () => void;
  onSkip: () => void;
}

const SWIPE_THRESHOLD_RATIO = 0.18;
const MAX_INTERESTS = 6;

const join = (...classes: Array<string | false | null | undefined>) =>
  classes.filter(Boolean).join(' ');

const formatDistance = (distanceKm?: number) => {
  if (typeof distanceKm !== 'number' || Number.isNaN(distanceKm)) return undefined;
  if (distanceKm < 1) {
    return `${Math.round(distanceKm * 1000)} м`;
  }
  return `${Math.round(distanceKm)} км`;
};

function SkeletonCard() {
  return (
    <div className={styles.card} data-loading="true">
      <div className={join(styles.skeletonBlock, styles.skeletonMedia)} />
      <div className={styles.body}>
        <div className={join(styles.skeletonBlock, styles.skeletonTitle)} />
        <div className={join(styles.skeletonBlock, styles.skeletonText)} />
        <div className={join(styles.skeletonBlock, styles.skeletonText)} style={{ width: '70%' }} />
      </div>
      <div className={join(styles.actions, styles.skeletonActions)}>
        <div className={join(styles.skeletonBlock, styles.skeletonCircle)} />
        <div className={join(styles.skeletonBlock, styles.skeletonCircle)} />
      </div>
    </div>
  );
}

function PlaceholderCard() {
  return (
    <div className={styles.card} data-placeholder="true">
      <div className={styles.placeholder}>
        <h2 className={styles.placeholderTitle}>Нет анкет поблизости</h2>
        <p className={styles.placeholderText}>Попробуйте обновить или расширить радиус поиска.</p>
      </div>
      <div className={styles.actions}>
        <button type="button" className={styles.btn} disabled aria-hidden>
          ✖
        </button>
        <button type="button" className={styles.btn} disabled aria-hidden>
          ❤
        </button>
      </div>
    </div>
  );
}

function UserCardComponent({
  username,
  age,
  bio,
  interests = [],
  photos = [],
  distanceKm,
  isLoading = false,
  onLike,
  onSkip,
}: UserCardProps) {
  const tg = (window as any).Telegram?.WebApp;
  const [activePhoto, setActivePhoto] = useState(0);
  const [dragRatio, setDragRatio] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const startXRef = useRef<number | null>(null);
  const pointerIdRef = useRef<number | null>(null);
  const widthRef = useRef<number>(1);
  const mediaRef = useRef<HTMLDivElement | null>(null);

  const safePhotos = useMemo(() => photos.filter(Boolean), [photos]);
  const displayedInterests = useMemo(
    () => interests.filter(Boolean).slice(0, MAX_INTERESTS),
    [interests],
  );
  const distanceLabel = formatDistance(distanceKm);

  useEffect(() => {
    setActivePhoto(0);
  }, [safePhotos.length]);

  if (isLoading) {
    return <SkeletonCard />;
  }

  if (!username) {
    return <PlaceholderCard />;
  }

  const totalPhotos = safePhotos.length;
  const transformed = `translateX(${(-activePhoto + (isDragging ? dragRatio : 0)) * 100}%)`;

  const handlePointerDown = (event: React.PointerEvent<HTMLDivElement>) => {
    if (totalPhotos <= 1) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    widthRef.current = mediaRef.current?.clientWidth ?? event.currentTarget.clientWidth;
    setIsDragging(true);
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLDivElement>) => {
    if (!isDragging || startXRef.current === null) return;
    const delta = event.clientX - startXRef.current;
    const rawRatio = delta / widthRef.current;
    const clampedRatio = Math.max(Math.min(rawRatio, 0.4), -0.4);
    setDragRatio(clampedRatio);
  };

  const completeSwipe = () => {
    if (!isDragging) return;
    const ratio = dragRatio;
    let nextIndex = activePhoto;

    if (ratio <= -SWIPE_THRESHOLD_RATIO && activePhoto < totalPhotos - 1) {
      nextIndex = activePhoto + 1;
    } else if (ratio >= SWIPE_THRESHOLD_RATIO && activePhoto > 0) {
      nextIndex = activePhoto - 1;
    }

    if (nextIndex !== activePhoto) {
      setActivePhoto(nextIndex);
      tg?.HapticFeedback?.impactOccurred('light');
    }

    setDragRatio(0);
    setIsDragging(false);
    startXRef.current = null;
    pointerIdRef.current = null;
  };

  const handlePointerUp = (event: React.PointerEvent<HTMLDivElement>) => {
    if (pointerIdRef.current !== null && event.currentTarget.hasPointerCapture(pointerIdRef.current)) {
      event.currentTarget.releasePointerCapture(pointerIdRef.current);
    }
    completeSwipe();
  };

  const handlePointerCancel = () => {
    completeSwipe();
  };

  const handleDotClick = (index: number) => {
    setActivePhoto(index);
    tg?.HapticFeedback?.impactOccurred('light');
  };

  const handleLike = () => {
    tg?.HapticFeedback?.impactOccurred('soft');
    onLike();
  };

  const handleSkip = () => {
    tg?.HapticFeedback?.impactOccurred('light');
    onSkip();
  };

  return (
    <div className={styles.card} data-dragging={isDragging}>
      {totalPhotos > 0 ? (
        <div
          ref={mediaRef}
          className={styles.media}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerCancel}
        >
          <div className={styles.mediaTrack} data-dragging={isDragging} style={{ transform: transformed }}>
            {safePhotos.map((src, index) => (
              <img key={src || index} src={src} alt="" className={styles.photo} draggable={false} />
            ))}
          </div>
          <div className={styles.overlay} aria-hidden />
          <div className={styles.header}>
            <div className={styles.title}>
              <span>{username}</span>
              {typeof age === 'number' && !Number.isNaN(age) && <span>{age}</span>}
            </div>
            <div className={styles.meta}>
              {distanceLabel && <span>{distanceLabel} от вас</span>}
              {totalPhotos > 1 && <span className={styles.badge}>фото {activePhoto + 1}/{totalPhotos}</span>}
            </div>
          </div>
          {totalPhotos > 1 && (
            <div className={styles.pagination} aria-label="Переключение фото">
              {safePhotos.map((_, index) => (
                <button
                  key={index}
                  type="button"
                  className={styles.dot}
                  data-active={index === activePhoto}
                  onClick={() => handleDotClick(index)}
                  aria-label={`Фото ${index + 1} из ${totalPhotos}`}
                  aria-pressed={index === activePhoto}
                />
              ))}
            </div>
          )}
        </div>
      ) : (
        <div className={styles.media}>
          <div className={styles.photoPlaceholder}>Фото появятся позже</div>
          <div className={styles.overlay} aria-hidden />
          <div className={styles.header}>
            <div className={styles.title}>
              <span>{username}</span>
              {typeof age === 'number' && !Number.isNaN(age) && <span>{age}</span>}
            </div>
            {distanceLabel && <div className={styles.meta}>{distanceLabel} от вас</div>}
          </div>
        </div>
      )}

      <div className={styles.body}>
        {bio ? <p className={styles.bio}>{bio}</p> : <p className={styles.bio}>Аватар скоро обновится. Скажите «привет» и узнайте друг друга ближе.</p>}
        {displayedInterests.length > 0 && (
          <ul className={styles.tags}>
            {displayedInterests.map((interest) => (
              <li key={interest} className={styles.tag}>
                #{interest}
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className={styles.actions}>
        <button type="button" className={join(styles.btn, styles.btnSkip)} onClick={handleSkip} aria-label="Пропустить">
          ✖
        </button>
        <button type="button" className={join(styles.btn, styles.btnLike)} onClick={handleLike} aria-label="Лайк">
          ❤
        </button>
      </div>
    </div>
  );
}

export default memo(UserCardComponent);

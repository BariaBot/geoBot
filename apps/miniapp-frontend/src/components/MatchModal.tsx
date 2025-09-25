import type { MatchDetails } from '../store/match';

interface MatchModalProps {
  match: MatchDetails | null;
  visible: boolean;
  onClose: () => void;
  onOpenChat: () => void;
}

export function MatchModal({ match, visible, onClose, onOpenChat }: MatchModalProps) {
  if (!visible || !match) {
    return null;
  }

  return (
    <div className="match-modal" role="dialog" aria-modal="true" aria-labelledby="match-modal-title">
      <div className="match-modal__backdrop" />
      <div className="match-modal__container">
        <header className="match-modal__header">
          <span className="match-modal__badge">Match</span>
          <h2 id="match-modal-title">Это взаимно! 🎉</h2>
          <p className="match-modal__subtitle">
            Вы с {match.name} понравились друг другу. Самое время написать первое сообщение.
          </p>
        </header>
        <footer className="match-modal__actions">
          <button
            type="button"
            className="match-modal__button match-modal__button--primary"
            onClick={onOpenChat}
          >
            Перейти в чат
          </button>
          <button
            type="button"
            className="match-modal__button match-modal__button--ghost"
            onClick={onClose}
          >
            Продолжить знакомиться
          </button>
        </footer>
        <p className="match-modal__hint">Мы отправим уведомление через Telegram бота, чтобы напомнить о матче.</p>
      </div>
    </div>
  );
}

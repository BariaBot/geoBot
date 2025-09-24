import type { ThemeSnapshot } from '../hooks/useTelegramBridge';

interface LoaderScreenProps {
  theme: ThemeSnapshot | null;
}

export const LoaderScreen = ({ theme }: LoaderScreenProps) => (
  <div
    className="loader-screen"
    style={{
      backgroundColor: theme?.palette.bgColor ?? '#0d1117',
      color: theme?.palette.textColor ?? '#f0f6fc',
    }}
  >
    <div className="loader-screen__spinner" />
    <p>Запускаем мини-приложение…</p>
  </div>
);

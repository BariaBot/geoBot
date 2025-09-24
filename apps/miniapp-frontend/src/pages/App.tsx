import { useEffect, useState } from 'react';
import { init } from '@telegram-apps/sdk';
import { useTelegramBridge } from '../hooks/useTelegramBridge';
import { useProfileStore } from '../store/profile';
import { LoaderScreen } from '../components/LoaderScreen';

const TELEGRAM_INIT_TIMEOUT_MS = 4000;

const App = () => {
  const [ready, setReady] = useState(false);
  const { initBridge, bridgeReady, themeParams } = useTelegramBridge();
  const initialiseProfile = useProfileStore((store) => store.initialiseFromDeviceStorage);

  useEffect(() => {
    const controller = new AbortController();
    const cleanupInit = init({ acceptCustomStyles: true });

    const timeout = setTimeout(() => {
      controller.abort();
      setReady(true);
    }, TELEGRAM_INIT_TIMEOUT_MS);

    const initialise = async () => {
      try {
        await initBridge(controller.signal);
        if (!controller.signal.aborted) {
          await initialiseProfile();
          setReady(true);
        }
      } finally {
        clearTimeout(timeout);
      }
    };

    initialise();

    return () => {
      controller.abort();
      cleanupInit();
      clearTimeout(timeout);
    };
  }, [initBridge, initialiseProfile]);

  if (!ready || !bridgeReady) {
    return <LoaderScreen theme={themeParams} />;
  }

  return (
    <div className="app-shell" data-theme={themeParams?.isDark ? 'dark' : 'light'}>
      <header className="app-shell__header">
        <h1>WAU Dating</h1>
        <p>Свайпай, чтобы встретить лучших людей рядом.</p>
      </header>
      <main className="app-shell__content">
        {/* TODO: заменить на навигацию по реальным маршрутам */}
        <p>Экран главной ленты появится здесь.</p>
      </main>
    </div>
  );
};

export default App;

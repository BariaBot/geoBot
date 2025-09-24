import { useState, useCallback } from 'react';
import { initMiniApp, initThemeParams, ThemeParams } from '@tma.js/sdk';

export interface ThemeSnapshot {
  palette: ReturnType<ThemeParams['getState']>;
  isDark: boolean;
}

interface TelegramBridgeState {
  initBridge: (signal?: AbortSignal) => Promise<void>;
  bridgeReady: boolean;
  themeParams: ThemeSnapshot | null;
}

export const useTelegramBridge = (): TelegramBridgeState => {
  const [bridgeReady, setBridgeReady] = useState(false);
  const [themeParams, setThemeParams] = useState<ThemeSnapshot | null>(null);

  const initBridge = useCallback(async (signal?: AbortSignal) => {
    const [miniApp, cleanupMiniApp] = initMiniApp();
    const [theme, cleanupTheme] = initThemeParams();

    const syncTheme = () => {
      setThemeParams({
        palette: theme.getState(),
        isDark: theme.isDark,
      });
    };

    syncTheme();
    const stopThemeListener = theme.on('change', syncTheme);

    miniApp.ready();
    setBridgeReady(true);

    const dispose = () => {
      stopThemeListener();
      cleanupTheme();
      cleanupMiniApp();
    };

    if (signal) {
      if (signal.aborted) {
        dispose();
        return;
      }

      signal.addEventListener('abort', dispose, { once: true });
    }
  }, []);

  return { initBridge, bridgeReady, themeParams };
};

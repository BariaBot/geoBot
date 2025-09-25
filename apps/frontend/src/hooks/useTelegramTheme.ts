import { useEffect } from 'react';
import { applyThemeTokens, DEFAULT_THEME_TOKENS } from '../theme/tokens';
import { getTelegramWebApp, readTelegramTheme } from '../theme/telegramTheme';

export function useTelegramTheme() {
  useEffect(() => {
    const root = document.documentElement;
    applyThemeTokens(root, DEFAULT_THEME_TOKENS);

    const webApp = getTelegramWebApp();
    if (!webApp) return undefined;

    const syncTheme = () => {
      applyThemeTokens(root, readTelegramTheme(webApp));
    };

    syncTheme();
    webApp.onEvent?.('themeChanged', syncTheme);

    return () => {
      webApp.offEvent?.('themeChanged', syncTheme);
    };
  }, []);
}

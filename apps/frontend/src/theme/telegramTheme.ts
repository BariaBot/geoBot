import { createThemeTokens, type TelegramThemeParams, type ThemeTokenMap } from './tokens';

export interface TelegramWebApp {
  themeParams?: TelegramThemeParams;
  onEvent?: (event: 'themeChanged', handler: () => void) => void;
  offEvent?: (event: 'themeChanged', handler: () => void) => void;
}

export function getTelegramWebApp(): TelegramWebApp | undefined {
  return (window as any)?.Telegram?.WebApp;
}

export function resolveTelegramTheme(tokensSource?: TelegramThemeParams): ThemeTokenMap {
  return createThemeTokens(tokensSource ?? {});
}

export function readTelegramTheme(webApp: TelegramWebApp | undefined): ThemeTokenMap {
  return resolveTelegramTheme(webApp?.themeParams ?? {});
}

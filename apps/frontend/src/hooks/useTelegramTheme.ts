import { useEffect } from 'react';

export function useTelegramTheme() {
  useEffect(() => {
    const tg = (window as any).Telegram?.WebApp;
    if (!tg) return;

    const applyTheme = () => {
      const params = tg.themeParams || {};
      document.body.style.setProperty('--tg-bg', params.bg_color || '#ffffff');
      document.body.style.setProperty('--tg-text', params.text_color || '#000000');
      document.body.style.setProperty('--tg-accent', params.button_color || '#0d6efd');
    };

    tg.onEvent('themeChanged', applyTheme);
    applyTheme();

    return () => {
      tg.offEvent('themeChanged', applyTheme);
    };
  }, []);
}

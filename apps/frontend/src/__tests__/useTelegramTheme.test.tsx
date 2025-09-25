import { cleanup, render } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useTelegramTheme } from '../hooks/useTelegramTheme';
import type { TelegramWebApp } from '../theme/telegramTheme';

document.documentElement.style.cssText = '';

const clearCssVariables = () => {
  const rootStyle = document.documentElement.style;
  Array.from(rootStyle).forEach((prop) => {
    rootStyle.removeProperty(prop);
  });
};

const readVar = (name: string) =>
  document.documentElement.style.getPropertyValue(name).trim();

function TestComponent() {
  useTelegramTheme();
  return null;
}

describe('useTelegramTheme', () => {
  afterEach(() => {
    cleanup();
  });

  beforeEach(() => {
    clearCssVariables();
    vi.resetAllMocks();
    delete (window as any).Telegram;
  });

  it('applies default token values without Telegram WebApp', () => {
    render(<TestComponent />);

    expect(readVar('--color-background')).toBe('#ffffff');
    expect(readVar('--color-accent')).toBe('#0d6efd');
    expect(readVar('--color-text-primary')).toBe('#0f0f0f');
  });

  it('syncs tokens from Telegram theme and reacts to themeChanged events', () => {
    const handlers = new Map<string, () => void>();
    const webApp: TelegramWebApp = {
      themeParams: {
        bg_color: '#101820',
        text_color: '#f8f9fa',
        button_color: '#2f80ed',
        button_text_color: '#ffffff',
      },
      onEvent: vi.fn((event, handler) => {
        handlers.set(event, handler);
      }),
      offEvent: vi.fn((event, handler) => {
        if (handlers.get(event) === handler) {
          handlers.delete(event);
        }
      }),
    };

    (window as any).Telegram = { WebApp: webApp };

    const { unmount } = render(<TestComponent />);

    expect(readVar('--color-background')).toBe('#101820');
    expect(readVar('--color-accent')).toBe('#2f80ed');

    webApp.themeParams = {
      bg_color: '#0b0d12',
      text_color: '#e0e3e6',
      button_color: '#ff6f61',
      button_text_color: '#ffffff',
    };
    handlers.get('themeChanged')?.();

    expect(readVar('--color-background')).toBe('#0b0d12');
    expect(readVar('--color-accent')).toBe('#ff6f61');

    unmount();
    expect(webApp.offEvent).toHaveBeenCalledTimes(1);
    expect(webApp.offEvent).toHaveBeenCalledWith('themeChanged', expect.any(Function));
  });
});

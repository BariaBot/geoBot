import {
  resolveCSSVariables,
  resolveThemeMode,
  type ThemeMode,
} from './tokens';
import type { ThemeSnapshot } from '../hooks/useTelegramBridge';

export const applyThemeTokens = (theme: ThemeSnapshot | null) => {
  const mode: ThemeMode = resolveThemeMode(theme);
  const variables = resolveCSSVariables(mode, theme);
  const htmlElement = document.documentElement;
  const rootElement = document.getElementById('root');
  const targets = [htmlElement, rootElement].filter(
    (element): element is HTMLElement => Boolean(element),
  );

  if (targets.length === 0) {
    return;
  }

  targets.forEach((element) => {
    Object.entries(variables).forEach(([name, value]) => {
      element.style.setProperty(name, String(value));
    });

    element.setAttribute('data-theme', mode);
  });
};

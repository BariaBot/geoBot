import { applyThemeTokens, resolveThemeMode, themeVar } from '../theme';
import type { ThemeSnapshot } from '../hooks/useTelegramBridge';

type PaletteOverrides = Partial<ThemeSnapshot['palette']>;

const buildTheme = (overrides: PaletteOverrides = {}): ThemeSnapshot => ({
  isDark: false,
  palette: {
    textColor: '#0b1f33',
    subtitleTextColor: '#244366',
    hintColor: '#8095ad',
    buttonColor: '#6fee9f',
    buttonTextColor: '#082032',
    linkColor: '#1dd3ff',
    bgColor: '#ffffff',
    secondaryBgColor: '#f2f6ff',
    sectionSeparatorColor: '#d5e2f4',
    destructiveTextColor: '#ff6cab',
    ...overrides,
  },
});

describe('resolveThemeMode', () => {
  it('defaults to dark when theme is not provided', () => {
    expect(resolveThemeMode(null)).toBe('dark');
  });

  it('reflects Telegram dark/light flag', () => {
    expect(resolveThemeMode({
      isDark: true,
      palette: {} as ThemeSnapshot['palette'],
    })).toBe('dark');
    expect(resolveThemeMode(buildTheme())).toBe('light');
  });
});

const resetDom = () => {
  document.body.innerHTML = '<div id="root"></div>';
  document.documentElement.removeAttribute('data-theme');
  document.documentElement.removeAttribute('style');
  const root = document.getElementById('root');
  root?.removeAttribute('data-theme');
  root?.removeAttribute('style');
};

describe('applyThemeTokens', () => {
  beforeEach(() => {
    resetDom();
  });

  it('applies fallback dark variables when theme is missing', () => {
    applyThemeTokens(null);

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.documentElement.style.getPropertyValue('--tg-color-text')).toBe('#f0f6fc');
  });

  it('updates CSS variables and data-theme for provided snapshot', () => {
    const theme = buildTheme();

    applyThemeTokens(theme);

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(document.documentElement.style.getPropertyValue('--tg-color-text')).toBe('#0b1f33');
    expect(document.documentElement.style.getPropertyValue('--tg-gradient-primaryFrom')).toBe('#6fee9f');
  });
});

describe('themeVar helper', () => {
  it('creates a nested CSS variable accessor', () => {
    expect(themeVar('color', 'text')).toBe('var(--tg-color-text)');
    expect(themeVar('spacing', 'xl')).toBe('var(--tg-spacing-xl)');
  });
});

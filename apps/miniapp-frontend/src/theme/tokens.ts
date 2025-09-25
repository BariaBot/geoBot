import type { ThemeSnapshot } from '../hooks/useTelegramBridge';

type TokenValue = string | number;

export type ThemeMode = 'light' | 'dark';

export interface ThemeDefinition {
  [token: string]: TokenValue | ThemeDefinition;
}

export type ThemeTokens = Record<ThemeMode, ThemeDefinition>;

type TokenPath = string[];

type TokenEntry = {
  path: TokenPath;
  value: TokenValue;
};

const ROOT_PREFIX = '--tg-';

const fallbackPalette: Record<ThemeMode, Record<string, string>> = {
  dark: {
    textColor: '#f0f6fc',
    subtitleTextColor: 'rgba(240, 246, 252, 0.7)',
    hintColor: 'rgba(240, 246, 252, 0.6)',
    buttonColor: '#6fee9f',
    buttonTextColor: '#082032',
    linkColor: '#1dd3ff',
    bgColor: '#0d1117',
    secondaryBgColor: 'rgba(255, 255, 255, 0.03)',
    sectionSeparatorColor: 'rgba(255, 255, 255, 0.08)',
    destructiveTextColor: '#ff6cab',
  },
  light: {
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
  },
};

const baseTokens: ThemeTokens = {
  dark: {
    surface: {
      background: '#0d1117',
      cardBackground: 'rgba(255, 255, 255, 0.03)',
      cardBorder: 'rgba(255, 255, 255, 0.08)',
      buttonGhostBg: 'rgba(255, 255, 255, 0.05)',
      buttonGhostBorder: 'rgba(255, 255, 255, 0.2)',
      inputBackground: 'rgba(17, 24, 39, 0.6)',
      badgeBg: 'rgba(255, 255, 255, 0.14)',
      placeholderBg: 'rgba(255, 255, 255, 0.15)',
      modalBorder: 'rgba(255, 255, 255, 0.18)',
      modalShadow: '0 24px 48px rgba(0, 0, 0, 0.3)',
    },
    gradient: {
      primaryFrom: '#6fee9f',
      primaryTo: '#1dd3ff',
      destructiveFrom: '#ff6cab',
      destructiveTo: '#7366ff',
      modalBackgroundFrom: 'rgba(111, 238, 159, 0.12)',
      modalBackgroundTo: 'rgba(29, 211, 255, 0.12)',
    },
    radius: {
      sm: '12px',
      md: '14px',
      lg: '16px',
      xl: '18px',
      pill: '999px',
      modal: '24px',
    },
    spacing: {
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '12px',
      xl: '16px',
      xxl: '24px',
    },
    typography: {
      fontFamily: "'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      headingWeight: 700,
      bodyWeight: 500,
    },
  },
  light: {
    surface: {
      background: '#ffffff',
      cardBackground: '#ffffff',
      cardBorder: '#d5e2f4',
      buttonGhostBg: '#e8f0fb',
      buttonGhostBorder: '#c1d4ee',
      inputBackground: '#f7fbff',
      badgeBg: '#e8f0fb',
      placeholderBg: '#eef5ff',
      modalBorder: '#d5e2f4',
      modalShadow: '0 18px 36px rgba(12, 40, 75, 0.18)',
    },
    gradient: {
      primaryFrom: '#6fee9f',
      primaryTo: '#1dd3ff',
      destructiveFrom: '#ff6cab',
      destructiveTo: '#7366ff',
      modalBackgroundFrom: 'rgba(255, 255, 255, 0.95)',
      modalBackgroundTo: 'rgba(225, 244, 255, 0.9)',
    },
    radius: {
      sm: '12px',
      md: '14px',
      lg: '16px',
      xl: '18px',
      pill: '999px',
      modal: '24px',
    },
    spacing: {
      xs: '4px',
      sm: '6px',
      md: '8px',
      lg: '12px',
      xl: '16px',
      xxl: '24px',
    },
    typography: {
      fontFamily: "'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
      headingWeight: 700,
      bodyWeight: 500,
    },
  },
};

const buildPaletteTokens = (mode: ThemeMode, theme: ThemeSnapshot | null): ThemeDefinition => {
  const palette = theme?.palette ?? {};
  const fallback = fallbackPalette[mode];

  return {
    text: palette.textColor ?? fallback.textColor,
    textSecondary: palette.subtitleTextColor ?? fallback.subtitleTextColor,
    hint: palette.hintColor ?? fallback.hintColor,
    accent: palette.buttonColor ?? fallback.buttonColor,
    accentForeground: palette.buttonTextColor ?? fallback.buttonTextColor,
    accentGradientFrom: palette.buttonColor ?? fallback.buttonColor,
    accentGradientTo: palette.linkColor ?? fallback.linkColor,
    destructive: palette.destructiveTextColor ?? fallback.destructiveTextColor,
    destructiveForeground: palette.buttonTextColor ?? fallback.buttonTextColor,
    background: palette.bgColor ?? fallback.bgColor,
    backgroundSecondary: palette.secondaryBgColor ?? fallback.secondaryBgColor,
    border: palette.sectionSeparatorColor ?? fallback.sectionSeparatorColor,
    overlay: mode === 'dark' ? 'rgba(8, 14, 24, 0.72)' : 'rgba(12, 40, 75, 0.32)',
    success: '#6fee9f',
    warning: '#ffb4b4',
  };
};

const flattenDefinition = (
  definition: ThemeDefinition,
  path: TokenPath = [],
): TokenEntry[] => Object.entries(definition)
  .flatMap(([key, value]) => {
    const currentPath = [...path, key];

    if (typeof value === 'object' && value !== null) {
      return flattenDefinition(value, currentPath);
    }

    return [{ path: currentPath, value } satisfies TokenEntry];
  });

const createVarName = (path: TokenPath) => `${ROOT_PREFIX}${path.join('-')}`;

function buildCSSVariables(definition: ThemeDefinition): Record<string, TokenValue> {
  return flattenDefinition(definition).reduce<Record<string, TokenValue>>((acc, entry) => {
    acc[createVarName(entry.path)] = entry.value;
    return acc;
  }, {});
}

export const resolveThemeDefinition = (
  mode: ThemeMode,
  theme: ThemeSnapshot | null,
): ThemeDefinition => {
  const base = baseTokens[mode];

  return {
    ...base,
    color: buildPaletteTokens(mode, theme),
  };
};

export function resolveCSSVariables(
  mode: ThemeMode,
  theme: ThemeSnapshot | null,
): Record<string, TokenValue> {
  return buildCSSVariables(resolveThemeDefinition(mode, theme));
}

export const resolveThemeMode = (theme: ThemeSnapshot | null): ThemeMode => {
  if (theme?.isDark) {
    return 'dark';
  }

  if (theme?.isDark === false) {
    return 'light';
  }

  return 'dark';
};

export const themeVar = (...path: TokenPath) => `var(${createVarName(path)})`;

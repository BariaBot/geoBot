const HEX_COLOR_REGEX = /^#(?:[0-9a-fA-F]{3}){1,2}$/;

export type ThemeTokenMap = Record<string, string>;

export interface TelegramThemeParams {
  bg_color?: string;
  text_color?: string;
  hint_color?: string;
  link_color?: string;
  button_color?: string;
  button_text_color?: string;
  secondary_bg_color?: string;
  header_bg_color?: string;
  subtitle_text_color?: string;
  accent_text_color?: string;
  destructive_text_color?: string;
  section_separator_color?: string;
}

function normalizeHex(hex?: string): string | undefined {
  if (!hex) return undefined;
  if (!HEX_COLOR_REGEX.test(hex)) return undefined;
  if (hex.length === 4) {
    return `#${hex
      .slice(1)
      .split('')
      .map((char) => char.repeat(2))
      .join('')}`.toLowerCase();
  }
  return hex.toLowerCase();
}

function toRgba(color: string, alpha: number): string {
  const normalized = normalizeHex(color);
  if (!normalized) return `rgba(0, 0, 0, ${Math.min(Math.max(alpha, 0), 1)})`;

  const r = parseInt(normalized.slice(1, 3), 16);
  const g = parseInt(normalized.slice(3, 5), 16);
  const b = parseInt(normalized.slice(5, 7), 16);

  return `rgba(${r}, ${g}, ${b}, ${Math.min(Math.max(alpha, 0), 1)})`;
}

const STATIC_TOKENS: ThemeTokenMap = {
  '--space-2xs': '4px',
  '--space-xs': '8px',
  '--space-sm': '12px',
  '--space-md': '16px',
  '--space-lg': '24px',
  '--space-xl': '32px',
  '--radius-sm': '8px',
  '--radius-md': '12px',
  '--radius-lg': '16px',
  '--radius-pill': '999px',
  '--size-bottom-bar-height': '56px',
  '--size-card-max-width': '320px',
  '--size-action-button': '48px',
  '--size-loader': '24px',
  '--icon-size-md': '20px',
  '--border-width-hairline': '1px',
  '--border-width-regular': '3px',
  '--duration-toast': '2s',
  '--font-family-base': "'Inter', 'SF Pro Text', 'Helvetica Neue', Arial, sans-serif",
  '--font-size-xs': '12px',
  '--font-size-sm': '14px',
  '--font-size-md': '16px',
  '--font-size-lg': '20px',
  '--line-height-tight': '1.25',
  '--line-height-base': '1.5',
  '--transition-base': '150ms ease-out',
};

function resolveColors(params: TelegramThemeParams = {}): ThemeTokenMap {
  const background = normalizeHex(params.bg_color) ?? '#ffffff';
  const surface = normalizeHex(params.secondary_bg_color) ?? '#f3f4f7';
  const surfaceElevated = normalizeHex(params.header_bg_color) ?? surface;
  const textPrimary = normalizeHex(params.text_color) ?? '#0f0f0f';
  const textSecondary =
    normalizeHex(params.subtitle_text_color) ??
    normalizeHex(params.hint_color) ??
    '#5b6166';
  const textMuted = normalizeHex(params.hint_color) ?? toRgba(textPrimary, 0.48);
  const accent =
    normalizeHex(params.button_color) ??
    normalizeHex(params.link_color) ??
    '#0d6efd';
  const accentContrast = normalizeHex(params.button_text_color) ?? '#ffffff';
  const likeColor = normalizeHex(params.accent_text_color) ?? accent;
  const destructive = normalizeHex(params.destructive_text_color) ?? '#ff3b30';
  const border = normalizeHex(params.section_separator_color) ?? toRgba(textPrimary, 0.08);
  const borderStrong = toRgba(textPrimary, 0.16);
  const skipColor = toRgba(textPrimary, 0.45);
  const overlaySoft = toRgba(textPrimary, 0.05);
  const overlayStrong = toRgba(textPrimary, 0.8);

  return {
    '--color-background': background,
    '--color-surface': surface,
    '--color-surface-elevated': surfaceElevated,
    '--color-border': border,
    '--color-border-strong': borderStrong,
    '--color-text-primary': textPrimary,
    '--color-text-secondary': textSecondary,
    '--color-text-muted': textMuted,
    '--color-text-invert': accentContrast,
    '--color-accent': accent,
    '--color-accent-contrast': accentContrast,
    '--color-like': likeColor,
    '--color-skip': skipColor,
    '--color-danger': destructive,
    '--color-overlay-soft': overlaySoft,
    '--color-overlay-strong': overlayStrong,
    '--color-toast-background': overlayStrong,
    '--shadow-xs': `0 1px 2px ${toRgba(textPrimary, 0.08)}`,
    '--shadow-sm': `0 4px 16px ${toRgba(textPrimary, 0.12)}`,
  };
}

export function createThemeTokens(params: TelegramThemeParams = {}): ThemeTokenMap {
  return {
    ...STATIC_TOKENS,
    ...resolveColors(params),
  };
}

export const DEFAULT_THEME_TOKENS = createThemeTokens();

export function applyThemeTokens(target: HTMLElement, tokens: ThemeTokenMap) {
  Object.entries(tokens).forEach(([key, value]) => {
    target.style.setProperty(key, value);
  });
}

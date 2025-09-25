import { create } from 'zustand';
import { withDeviceStorage } from '../services/deviceStorage';
import { trackEvent } from '../utils/analytics';
import { sendMatchInvite } from '../api/notifications';

const SEEN_MATCHES_KEY = 'wau:matches:seen';

export interface MatchDetails {
  matchId: string | null;
  targetTelegramId: number;
  name: string;
  createdAt: string;
}

interface ActiveMatch extends MatchDetails {
  key: string;
}

interface MatchState {
  activeMatch: ActiveMatch | null;
  hydrated: boolean;
  seenKeys: Set<string>;
  hydrate: () => Promise<void>;
  presentMatch: (match: MatchDetails) => Promise<void>;
  dismissMatch: () => void;
}

function buildMatchKey(match: MatchDetails): string {
  if (match.matchId && match.matchId.trim().length > 0) {
    return match.matchId;
  }
  return `telegram:${match.targetTelegramId}`;
}

async function persistSeenKeys(keys: Set<string>): Promise<void> {
  await withDeviceStorage(async (storage) => {
    storage.set(SEEN_MATCHES_KEY, JSON.stringify(Array.from(keys)));
  });
}

async function loadSeenKeys(): Promise<Set<string>> {
  return withDeviceStorage(async (storage) => {
    const raw = storage.get(SEEN_MATCHES_KEY);
    if (!raw) {
      return new Set<string>();
    }
    try {
      const parsed = JSON.parse(raw) as string[];
      return new Set(parsed);
    } catch (error) {
      console.warn('Failed to parse stored matches', error);
      return new Set<string>();
    }
  });
}

function triggerMatchHaptic(): void {
  if (typeof window === 'undefined') return;
  const telegram = (window as any)?.Telegram?.WebApp;
  try {
    telegram?.HapticFeedback?.notificationOccurred?.('success');
  } catch (error) {
    console.warn('Haptic feedback unavailable', error);
  }
}

export const useMatchStore = create<MatchState>((set, get) => ({
  activeMatch: null,
  hydrated: false,
  seenKeys: new Set<string>(),
  async hydrate() {
    if (get().hydrated) {
      return;
    }
    const seen = await loadSeenKeys();
    set({ seenKeys: seen, hydrated: true });
  },
  async presentMatch(match) {
    if (!get().hydrated) {
      await get().hydrate();
    }

    const key = buildMatchKey(match);
    const state = get();
    if (state.seenKeys.has(key)) {
      return;
    }

    const nextSeen = new Set(state.seenKeys);
    nextSeen.add(key);

    set({ activeMatch: { ...match, key }, seenKeys: nextSeen });
    await persistSeenKeys(nextSeen);

    triggerMatchHaptic();
    trackEvent('match_shown', {
      matchId: match.matchId,
      target: match.targetTelegramId,
    });

    try {
      await sendMatchInvite({
        matchId: match.matchId,
        targetTelegramId: match.targetTelegramId,
        targetName: match.name,
      });
    } catch (error) {
      console.warn('Failed to send match invite webhook', error);
    }
  },
  dismissMatch() {
    set({ activeMatch: null });
  },
}));

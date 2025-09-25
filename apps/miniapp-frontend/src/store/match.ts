import { create } from 'zustand';
import DeviceStorage, {
  DeviceStorageError,
  DEVICE_STORAGE_LIMIT_BYTES,
} from '../services/deviceStorage';
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

async function persistSeenKeys(keys: Set<string>): Promise<boolean> {
  try {
    await DeviceStorage.setJSON(SEEN_MATCHES_KEY, Array.from(keys));
    return true;
  } catch (error) {
    if (error instanceof DeviceStorageError && error.code === 'PAYLOAD_TOO_LARGE') {
      const limitMb = DEVICE_STORAGE_LIMIT_BYTES / (1024 * 1024);
      console.warn(`Seen matches list exceeds ${limitMb} MB, skipping persist.`, error);
      return false;
    }
    console.warn('Failed to persist seen match keys', error);
    return false;
  }
}

async function loadSeenKeys(): Promise<Set<string>> {
  try {
    const stored = await DeviceStorage.getJSON<string[]>(SEEN_MATCHES_KEY);
    return stored ? new Set(stored) : new Set<string>();
  } catch (error) {
    if (error instanceof DeviceStorageError) {
      console.warn('Failed to load matches from DeviceStorage', error);
    } else {
      console.warn('Unexpected error while loading matches', error);
    }
    return new Set<string>();
  }
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

    const previousSeen = new Set(state.seenKeys);
    const nextSeen = new Set(state.seenKeys);
    nextSeen.add(key);

    set({ activeMatch: { ...match, key }, seenKeys: nextSeen });
    const persisted = await persistSeenKeys(nextSeen);
    if (!persisted) {
      set({ seenKeys: previousSeen });
    }

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

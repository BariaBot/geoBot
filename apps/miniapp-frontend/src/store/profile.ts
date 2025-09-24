import { create } from 'zustand';
import { fetchProfile, updateProfile, type ProfilePayload, type ProfileResponse } from '../api/profile';
import { withDeviceStorage } from '../services/deviceStorage';
import { trackEvent } from '../utils/analytics';

const DRAFT_STORAGE_KEY = 'wau:profile-draft';

export interface ProfileDraft {
  displayName: string;
  bio: string;
  gender?: string;
  birthday?: string;
  city?: string;
  interests: string[];
  latitude?: number;
  longitude?: number;
}

interface ProfileState {
  profile: ProfileResponse | null;
  draft: ProfileDraft;
  status: 'idle' | 'loading' | 'ready' | 'submitting' | 'error';
  error?: string;
  initialise: () => Promise<void>;
  updateDraft: (partial: Partial<ProfileDraft>) => Promise<void>;
  submitDraft: () => Promise<void>;
  resetError: () => void;
}

const DEFAULT_DRAFT: ProfileDraft = {
  displayName: '',
  bio: '',
  interests: []
};

export const useProfileStore = create<ProfileState>((set, get) => ({
  profile: null,
  draft: DEFAULT_DRAFT,
  status: 'idle',
  error: undefined,
  async initialise () {
    set({ status: 'loading', error: undefined });

    const draftFromStorage = await withDeviceStorage(async (storage) => {
      const raw = storage.get(DRAFT_STORAGE_KEY);
      if (!raw) return null;
      try {
        return JSON.parse(raw) as ProfileDraft;
      } catch (error) {
        console.warn('Failed to parse stored draft', error);
        return null;
      }
    });

    if (draftFromStorage) {
      set({ draft: { ...DEFAULT_DRAFT, ...draftFromStorage } });
    }

    try {
      const profile = await fetchProfile();
      set({ profile, draft: deriveDraft(profile, get().draft), status: 'ready' });
      trackEvent('profile_loaded', { hasProfile: Boolean(profile.displayName) });
    } catch (error) {
      console.error('Failed to load profile', error);
      set({ status: 'error', error: error instanceof Error ? error.message : 'Unknown error' });
    }
  },
  async updateDraft (partial) {
    const nextDraft = { ...get().draft, ...partial };
    set({ draft: nextDraft });
    await withDeviceStorage(async (storage) => {
      storage.set(DRAFT_STORAGE_KEY, JSON.stringify(nextDraft));
    });
  },
  async submitDraft () {
    const current = get();
    if (current.status === 'submitting') return;

    set({ status: 'submitting', error: undefined });

    const payload: ProfilePayload = {
      displayName: current.draft.displayName,
      bio: current.draft.bio,
      gender: current.draft.gender,
      birthday: current.draft.birthday,
      city: current.draft.city,
      interests: current.draft.interests,
      latitude: current.draft.latitude,
      longitude: current.draft.longitude
    };

    try {
      const profile = await updateProfile(payload);
      set({ profile, draft: deriveDraft(profile, current.draft), status: 'ready' });
      trackEvent('profile_saved', { telegramId: profile.telegramId });
      await withDeviceStorage(async (storage) => {
        storage.set(DRAFT_STORAGE_KEY, JSON.stringify(get().draft));
      });
    } catch (error) {
      console.error('Failed to save profile', error);
      set({ status: 'error', error: error instanceof Error ? error.message : 'Unknown error' });
    }
  },
  resetError () {
    set({ error: undefined, status: get().profile ? 'ready' : 'idle' });
  }
}));

function deriveDraft(profile: ProfileResponse, existing: ProfileDraft): ProfileDraft {
  return {
    displayName: profile.displayName && profile.displayName.trim().length > 0
      ? profile.displayName
      : existing.displayName ?? '',
    bio: profile.bio ?? existing.bio ?? '',
    gender: profile.gender ?? existing.gender,
    birthday: profile.birthday ?? existing.birthday,
    city: profile.city ?? existing.city,
    interests: profile.interests?.length ? profile.interests : existing.interests,
    latitude: profile.location?.latitude ?? existing.latitude,
    longitude: profile.location?.longitude ?? existing.longitude
  };
}

import { create } from 'zustand';

export interface ProfileDraft {
  name: string;
  bio: string;
  interests: string[];
  birthday?: string;
}

interface ProfileState {
  draft: ProfileDraft;
  initialiseFromDeviceStorage: () => Promise<void>;
  updateDraft: (partial: Partial<ProfileDraft>) => void;
}

const DEFAULT_DRAFT: ProfileDraft = {
  name: '',
  bio: '',
  interests: [],
};

export const useProfileStore = create<ProfileState>((set) => ({
  draft: DEFAULT_DRAFT,
  initialiseFromDeviceStorage: async () => {
    // TODO: внедрить DeviceStorage/secureStorage после реализации gateway.
    set({ draft: DEFAULT_DRAFT });
  },
  updateDraft: (partial) => set((state) => ({ draft: { ...state.draft, ...partial } })),
}));

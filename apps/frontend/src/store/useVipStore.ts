import { create } from 'zustand';

interface VipState {
  isVip: boolean;
  expiresAt?: string;
  setStatus: (active: boolean, expiresAt?: string) => void;
  reset: () => void;
}

export const useVipStore = create<VipState>((set) => ({
  isVip: false,
  expiresAt: undefined,
  setStatus: (active, expiresAt) => set({ isVip: active, expiresAt }),
  reset: () => set({ isVip: false, expiresAt: undefined }),
}));

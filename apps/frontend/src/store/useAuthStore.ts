import { create } from 'zustand';

interface AuthState {
  token?: string;
  setToken: (t: string) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: undefined,
  setToken: (t) => set({ token: t }),
  clear: () => set({ token: undefined }),
}));


import { create } from 'zustand';

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';

interface UploadMeta {
  status: UploadStatus;
  error?: string;
  progress?: number;
}

interface ProfileMediaState {
  uploads: Record<string, UploadMeta>;
  setStatus: (id: string, status: UploadStatus, meta?: Partial<UploadMeta>) => void;
  clear: (id?: string) => void;
}

export const useProfileMediaStore = create<ProfileMediaState>((set) => ({
  uploads: {},
  setStatus: (id, status, meta) =>
    set((state) => ({
      uploads: {
        ...state.uploads,
        [id]: {
          status,
          error: meta?.error,
          progress: meta?.progress,
        },
      },
    })),
  clear: (id) =>
    set((state) => {
      if (!id) {
        return { uploads: {} };
      }
      const next = { ...state.uploads };
      delete next[id];
      return { uploads: next };
    }),
}));

export type { UploadStatus };

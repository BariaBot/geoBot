import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useToastStore } from '../store/useToastStore';
import { useProfileMediaStore } from '../store/useProfileMediaStore';
import { dataUrlToFile, DraftPhoto } from '../utils/photos';
import { ProfileUpdatePayload, UploadPhotoResponse } from '../types/profile';

interface UseProfileMutationsParams {
  token?: string;
}

const buildHeaders = (token?: string) => ({
  'Content-Type': 'application/json',
  ...(token ? { Authorization: `Bearer ${token}` } : {}),
});

const buildMediaHeaders = (token?: string) => ({ ...(token ? { Authorization: `Bearer ${token}` } : {}) });

export const useProfileMutations = ({ token }: UseProfileMutationsParams) => {
  const queryClient = useQueryClient();
  const showToast = useToastStore((s) => s.show);
  const mediaStore = useProfileMediaStore();

  const updateProfile = useMutation({
    mutationFn: async (payload: ProfileUpdatePayload) => {
      const res = await fetch('/api/profile/me', {
        method: 'PATCH',
        headers: buildHeaders(token),
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        throw new Error('Не удалось обновить профиль');
      }
      return res.json();
    },
    onSuccess: () => {
      showToast('Профиль обновлён');
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : 'Ошибка при сохранении профиля';
      showToast(message);
    },
  });

  const uploadPhoto = useMutation({
    mutationFn: async (draftPhoto: DraftPhoto): Promise<UploadPhotoResponse> => {
      mediaStore.setStatus(draftPhoto.id, 'uploading', { progress: 0 });
      const file = await dataUrlToFile(draftPhoto.dataUrl, draftPhoto.name);
      const formData = new FormData();
      formData.append('file', file);
      const res = await fetch('/api/profile/media', {
        method: 'POST',
        headers: buildMediaHeaders(token),
        body: formData,
      });
      if (!res.ok) {
        const message = `Не удалось загрузить фото ${draftPhoto.name}`;
        mediaStore.setStatus(draftPhoto.id, 'error', { error: message });
        throw new Error(message);
      }
      const data = (await res.json()) as UploadPhotoResponse;
      mediaStore.setStatus(draftPhoto.id, 'success');
      return data;
    },
    onError: (error: unknown, draftPhoto) => {
      const message = error instanceof Error ? error.message : `Не удалось загрузить фото ${draftPhoto.name}`;
      showToast(message);
    },
  });

  const reorderPhotos = useMutation({
    mutationFn: async (photoIds: string[]) => {
      const res = await fetch('/api/profile/media/order', {
        method: 'PATCH',
        headers: buildHeaders(token),
        body: JSON.stringify({ order: photoIds }),
      });
      if (!res.ok) {
        throw new Error('Не удалось обновить порядок фотографий');
      }
      return res.json();
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : 'Не удалось обновить порядок фотографий';
      showToast(message);
    },
  });

  const deletePhoto = useMutation({
    mutationFn: async (photoId: string) => {
      const res = await fetch(`/api/profile/media/${photoId}`, {
        method: 'DELETE',
        headers: buildHeaders(token),
      });
      if (!res.ok) {
        throw new Error('Не удалось удалить фото');
      }
    },
    onError: (error: unknown) => {
      const message = error instanceof Error ? error.message : 'Не удалось удалить фото';
      showToast(message);
    },
  });

  return {
    updateProfile,
    uploadPhoto,
    reorderPhotos,
    deletePhoto,
  };
};

export type { DraftPhoto };

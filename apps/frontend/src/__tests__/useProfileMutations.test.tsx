import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi, type Mock } from 'vitest';
import { useProfileMutations } from '../hooks/useProfileMutations';
import { useProfileMediaStore } from '../store/useProfileMediaStore';

const createWrapper = () => {
  const client = new QueryClient();
  function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
  }
  return Wrapper;
};

describe('useProfileMutations', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    globalThis.fetch = vi.fn();
    useProfileMediaStore.getState().clear();
  });

  it('successfully updates profile', async () => {
    (globalThis.fetch as unknown as Mock).mockResolvedValueOnce({ ok: true, json: async () => ({}) });
    const { result } = renderHook(() => useProfileMutations({ token: 'token' }), { wrapper: createWrapper() });

    result.current.updateProfile.mutate({
      displayName: 'Анна',
      bio: 'О себе',
      interests: 'музыка',
      goals: 'общение',
      preferences: { radiusKm: 15, locationMode: 'manual', city: 'Москва' },
    });

    await waitFor(() => expect(result.current.updateProfile.isSuccess).toBe(true));
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/profile/me', expect.any(Object));
  });

  it('updates upload status for photos', async () => {
    const draftPhoto = {
      id: 'local-1',
      name: 'photo.png',
      type: 'image/png',
      size: 10,
      dataUrl: 'data:image/png;base64,AAAA',
      status: 'idle' as const,
      isLocal: true,
    };
    (globalThis.fetch as unknown as Mock)
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'remote-1', url: 'https://example.com/p.png', order: 1 }) });
    const { result } = renderHook(() => useProfileMutations({ token: 'token' }), { wrapper: createWrapper() });

    result.current.uploadPhoto.mutate(draftPhoto);

    await waitFor(() => expect(result.current.uploadPhoto.isSuccess).toBe(true));
    expect(useProfileMediaStore.getState().uploads['local-1']).toMatchObject({ status: 'success' });
  });
});

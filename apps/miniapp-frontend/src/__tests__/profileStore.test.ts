import { beforeEach, describe, expect, it, vi } from 'vitest';
import DeviceStorage, { DeviceStorageError } from '../services/deviceStorage';
import { useProfileStore } from '../store/profile';

describe('useProfileStore', () => {
  const defaultDraft = {
    displayName: '',
    bio: '',
    interests: [] as string[],
  };

  beforeEach(() => {
    useProfileStore.setState({
      profile: null,
      draft: { ...defaultDraft },
      status: 'ready',
      error: undefined,
    });
  });

  it('surfaces payload-too-large errors when persisting draft', async () => {
    const limitError = new DeviceStorageError('Размер данных превышает 1 MB лимит CloudStorage', {
      code: 'PAYLOAD_TOO_LARGE',
    });
    const setJsonSpy = vi.spyOn(DeviceStorage, 'setJSON').mockRejectedValueOnce(limitError);

    await useProfileStore.getState().updateDraft({ bio: 'new bio' });

    const state = useProfileStore.getState();
    expect(state.status).toBe('error');
    expect(state.error).toMatch(/лимит/i);
    expect(state.draft).toEqual(defaultDraft);

    setJsonSpy.mockRestore();
  });
});

import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';
import DeviceStorage, { DeviceStorageError } from '../services/deviceStorage';
import { useMatchStore } from '../store/match';

vi.mock('../api/notifications', () => ({
  sendMatchInvite: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('../utils/analytics', () => ({
  trackEvent: vi.fn(),
}));

describe('useMatchStore', () => {
  let getJsonSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    getJsonSpy = vi.spyOn(DeviceStorage, 'getJSON').mockResolvedValue(null);
    useMatchStore.setState({
      activeMatch: null,
      hydrated: false,
      seenKeys: new Set<string>(),
    });
  });

  afterEach(() => {
    getJsonSpy.mockRestore();
    useMatchStore.setState({
      activeMatch: null,
      hydrated: false,
      seenKeys: new Set<string>(),
    });
  });

  it('reverts seen keys when CloudStorage overflows', async () => {
    const limitError = new DeviceStorageError('Размер данных превышает 1 MB лимит CloudStorage', {
      code: 'PAYLOAD_TOO_LARGE',
    });
    const setJsonSpy = vi.spyOn(DeviceStorage, 'setJSON').mockRejectedValueOnce(limitError);

    await useMatchStore.getState().presentMatch({
      matchId: 'match-1',
      targetTelegramId: 123,
      name: 'Test',
      createdAt: new Date().toISOString(),
    });

    const state = useMatchStore.getState();
    expect(state.seenKeys.size).toBe(0);
    expect(setJsonSpy).toHaveBeenCalled();

    setJsonSpy.mockRestore();
  });
});

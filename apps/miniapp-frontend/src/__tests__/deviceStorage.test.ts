import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';
import type { CloudStorage } from '@tma.js/sdk';
import { initCloudStorage } from '@tma.js/sdk';
import DeviceStorage, {
  DeviceStorageError,
  __internal as deviceStorageInternal,
  withDeviceStorage,
} from '../services/deviceStorage';

type CloudStorageInitResult = [CloudStorage, () => void];

const cloudStorageInitBehavior: { impl: () => CloudStorageInitResult } = {
  impl: () => {
    throw new Error('CloudStorage unavailable for tests');
  },
};

vi.mock('@tma.js/sdk', async () => {
  const actual = await vi.importActual<typeof import('@tma.js/sdk')>('@tma.js/sdk');
  return {
    ...actual,
    initCloudStorage: vi.fn(() => cloudStorageInitBehavior.impl()),
  };
});

describe('DeviceStorage (miniapp)', () => {
  const originalTelegram = (window as any).Telegram;

  beforeEach(() => {
    window.localStorage.clear();
    (window as any).Telegram = undefined;
    deviceStorageInternal.resetCloudStorageCache();
    vi.mocked(initCloudStorage).mockClear();
    cloudStorageInitBehavior.impl = () => {
      throw new Error('CloudStorage unavailable for tests');
    };
  });

  afterEach(() => {
    window.localStorage.clear();
    (window as any).Telegram = originalTelegram;
  });

  it('falls back to localStorage when CloudStorage is unavailable', async () => {
    await DeviceStorage.setItem('draft', 'local');
    expect(window.localStorage.getItem('draft')).toBe('local');

    const value = await DeviceStorage.getItem('draft');
    expect(value).toBe('local');

    await DeviceStorage.removeItem('draft');
    expect(window.localStorage.getItem('draft')).toBeNull();
  });

  it('supports CloudStorage CRUD operations', async () => {
    const deleteMock = vi.fn().mockResolvedValue(undefined);
    const getMock = vi.fn().mockImplementation(async (keys: string[]) => ({
      [keys[0]]: 'cloud-value',
    }));
    const setMock = vi.fn().mockResolvedValue(undefined);
    const supportsMock = vi.fn(() => true);

    cloudStorageInitBehavior.impl = () => ([
      {
        delete: deleteMock,
        get: getMock,
        set: setMock,
        supports: supportsMock,
      } as unknown as CloudStorage,
      vi.fn(),
    ]);

    (window as any).Telegram = { WebApp: {} };

    expect(DeviceStorage.isCloudStorageAvailable()).toBe(true);

    await DeviceStorage.setItem('draft', 'cloud-value');
    expect(setMock).toHaveBeenCalledWith('draft', 'cloud-value');

    const value = await DeviceStorage.getItem('draft');
    expect(value).toBe('cloud-value');

    await DeviceStorage.removeItem('draft');
    expect(deleteMock).toHaveBeenCalledWith('draft');
  });

  it('propagates CloudStorage errors', async () => {
    const cloudError = new Error('CloudStorage failure');
    cloudStorageInitBehavior.impl = () => ([
      {
        delete: vi.fn(),
        get: vi.fn(),
        set: vi.fn().mockRejectedValue(cloudError),
        supports: vi.fn(() => true),
      } as unknown as CloudStorage,
      vi.fn(),
    ]);

    (window as any).Telegram = { WebApp: {} };

    await expect(DeviceStorage.setItem('draft', 'value')).rejects.toMatchObject({
      message: 'Не удалось сохранить значение',
      name: 'DeviceStorageError',
      cause: cloudError,
    });
    expect(window.localStorage.getItem('draft')).toBeNull();
  });

  it('serialises and parses JSON helpers', async () => {
    await DeviceStorage.setJSON('json-key', { foo: 'bar' });
    const stored = window.localStorage.getItem('json-key');
    expect(stored).toBe('{"foo":"bar"}');

    const payload = await DeviceStorage.getJSON<{ foo: string }>('json-key');
    expect(payload).toEqual({ foo: 'bar' });
  });

  it('validates payload size', async () => {
    const oversized = 'x'.repeat(1024 * 1024 + 1);
    await expect(DeviceStorage.setItem('oversized', oversized)).rejects.toThrow(DeviceStorageError);
  });

  it('allows adapters through withDeviceStorage helper', async () => {
    const result = await withDeviceStorage(async (storage) => {
      await storage.set('adapter-key', 'value');
      const fetched = await storage.get('adapter-key');
      await storage.delete('adapter-key');
      return fetched;
    });

    expect(result).toBe('value');
    expect(window.localStorage.getItem('adapter-key')).toBeNull();
  });
});

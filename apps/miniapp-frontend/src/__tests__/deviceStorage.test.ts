import {
  afterEach,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';
import DeviceStorage, {
  DeviceStorageError,
  withDeviceStorage,
} from '../services/deviceStorage';

describe('DeviceStorage (miniapp)', () => {
  const originalTelegram = (window as any).Telegram;

  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
    (window as any).Telegram = undefined;
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
    const setItem = vi.fn(
      (
        key: string,
        value: string,
        cb: (err?: Error | null, result?: boolean) => void,
      ) => cb(null, true),
    );
    const getItem = vi.fn(
      (key: string, cb: (err?: Error | null, result?: string) => void) => cb(null, 'cloud-value'),
    );
    const removeItem = vi.fn(
      (key: string, cb: (err?: Error | null, result?: boolean) => void) => cb(null, true),
    );

    (window as any).Telegram = {
      WebApp: {
        CloudStorage: { setItem, getItem, removeItem },
      },
    };

    expect(DeviceStorage.isCloudStorageAvailable()).toBe(true);

    await DeviceStorage.setItem('draft', 'cloud-value');
    expect(setItem).toHaveBeenCalledWith(
      'draft',
      'cloud-value',
      expect.any(Function),
    );

    const value = await DeviceStorage.getItem('draft');
    expect(value).toBe('cloud-value');

    await DeviceStorage.removeItem('draft');
    expect(removeItem).toHaveBeenCalledWith('draft', expect.any(Function));
  });

  it('propagates CloudStorage errors', async () => {
    const error = new Error('CloudStorage failure');
    const setItem = vi.fn(
      (key: string, value: string, cb: (err?: Error | null) => void) => cb(error),
    );

    (window as any).Telegram = {
      WebApp: {
        CloudStorage: {
          setItem,
          getItem: vi.fn(),
          removeItem: vi.fn(),
        },
      },
    };

    await expect(DeviceStorage.setItem('draft', 'value')).rejects.toBe(error);
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

import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import DeviceStorage, { DeviceStorageError } from '../utils/deviceStorage';

describe('DeviceStorage', () => {
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

  it('falls back to localStorage when CloudStorage unavailable', async () => {
    await DeviceStorage.setItem('draft', 'hello');
    expect(window.localStorage.getItem('draft')).toBe('hello');

    const value = await DeviceStorage.getItem('draft');
    expect(value).toBe('hello');
  });

  it('uses CloudStorage API when available', async () => {
    const setItem = vi.fn((key: string, value: string, cb: (err?: Error | null, res?: boolean) => void) => cb(null, true));
    const getItem = vi.fn((key: string, cb: (err?: Error | null, res?: string) => void) => cb(null, 'cloud'));
    const removeItem = vi.fn((key: string, cb: (err?: Error | null, res?: boolean) => void) => cb(null, true));

    (window as any).Telegram = {
      WebApp: {
        CloudStorage: { setItem, getItem, removeItem },
      },
    };

    await DeviceStorage.setItem('draft', 'cloud');
    expect(setItem).toHaveBeenCalledWith('draft', 'cloud', expect.any(Function));

    const value = await DeviceStorage.getItem('draft');
    expect(value).toBe('cloud');

    await DeviceStorage.removeItem('draft');
    expect(removeItem).toHaveBeenCalledWith('draft', expect.any(Function));
  });

  it('throws when payload exceeds 1 MB', async () => {
    const oversized = 'x'.repeat(1024 * 1024 + 1);
    await expect(DeviceStorage.setItem('big', oversized)).rejects.toThrow(DeviceStorageError);
  });
});

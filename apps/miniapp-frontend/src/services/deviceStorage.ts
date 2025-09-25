import { CloudStorage as TelegramCloudStorage, initCloudStorage } from '@tma.js/sdk';

const ONE_MB = 1024 * 1024;

export class DeviceStorageError extends Error {
  constructor(message: string, options?: ErrorOptions) {
    super(message, options);
    this.name = 'DeviceStorageError';
  }
}

let cachedCloudStorage: TelegramCloudStorage | undefined;
let disposeCloudStorage: (() => void) | undefined;

const resetCloudStorageCache = () => {
  cachedCloudStorage = undefined;
  if (disposeCloudStorage) {
    disposeCloudStorage();
    disposeCloudStorage = undefined;
  }
};

const ensureCloudStorage = (): TelegramCloudStorage | undefined => {
  if (typeof window === 'undefined') {
    return undefined;
  }

  if (cachedCloudStorage) {
    return cachedCloudStorage;
  }

  const telegram = (window as any)?.Telegram;
  if (!telegram?.WebApp) {
    return undefined;
  }

  try {
    const [cloudStorage, cleanup] = initCloudStorage();
    if (!cloudStorage.supports('set')
      || !cloudStorage.supports('get')
      || !cloudStorage.supports('delete')) {
      cleanup();
      return undefined;
    }

    cachedCloudStorage = cloudStorage;
    disposeCloudStorage = cleanup;
  } catch (error) {
    logWarning('CloudStorage init failed', error);
  }

  return cachedCloudStorage;
};

const validatePayloadSize = (value: string) => {
  const bytes = new TextEncoder().encode(value);
  const { length } = bytes;
  if (length > ONE_MB) {
    throw new DeviceStorageError('Размер данных превышает 1 MB лимит CloudStorage');
  }
};

const localStorageAdapter = () => {
  if (typeof window === 'undefined' || !('localStorage' in window)) {
    throw new DeviceStorageError('localStorage недоступен');
  }
  return window.localStorage;
};

const logWarning = (message: string, error: unknown) => {
  if (error instanceof Error) {
    console.warn(message, error);
  } else {
    console.warn(message, new Error(String(error)));
  }
};

const isCloudStorageAvailableInternal = (): boolean => Boolean(getCloudStorage());
const getCloudStorage = (): TelegramCloudStorage | undefined => ensureCloudStorage();

const toDeviceStorageError = (message: string, error: unknown): DeviceStorageError => {
  if (error instanceof DeviceStorageError) {
    return error;
  }

  if (error instanceof Error) {
    return new DeviceStorageError(message, { cause: error });
  }

  return new DeviceStorageError(message);
};

const DeviceStorage = {
  async setItem(key: string, value: string): Promise<void> {
    validatePayloadSize(value);
    const storage = getCloudStorage();
    if (storage) {
      try {
        await storage.set(key, value);
        return;
      } catch (error) {
        logWarning('CloudStorage setItem failed', error);
        throw toDeviceStorageError('Не удалось сохранить значение', error);
      }
    }

    try {
      localStorageAdapter().setItem(key, value);
    } catch (error) {
      logWarning('localStorage set failed', error);
      throw toDeviceStorageError('localStorage set failed', error);
    }
  },

  async getItem(key: string): Promise<string | null> {
    const storage = getCloudStorage();
    if (storage) {
      try {
        const values = await storage.get([key]);
        if (Object.prototype.hasOwnProperty.call(values, key)) {
          return values[key];
        }
        return null;
      } catch (error) {
        logWarning('CloudStorage getItem failed', error);
        throw toDeviceStorageError('Не удалось получить значение', error);
      }
    }

    try {
      return localStorageAdapter().getItem(key);
    } catch (error) {
      logWarning('localStorage get failed', error);
      throw toDeviceStorageError('localStorage get failed', error);
    }
  },

  async removeItem(key: string): Promise<void> {
    const storage = getCloudStorage();
    if (storage) {
      try {
        await storage.delete(key);
        return;
      } catch (error) {
        logWarning('CloudStorage removeItem failed', error);
        throw toDeviceStorageError('Не удалось удалить значение', error);
      }
    }

    try {
      localStorageAdapter().removeItem(key);
    } catch (error) {
      logWarning('localStorage delete failed', error);
      throw toDeviceStorageError('localStorage delete failed', error);
    }
  },

  async setJSON<T>(key: string, payload: T): Promise<void> {
    const value = JSON.stringify(payload ?? null);
    await this.setItem(key, value);
  },

  async getJSON<T>(key: string): Promise<T | null> {
    const raw = await this.getItem(key);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T;
    } catch (error) {
      logWarning('DeviceStorage JSON parse failed', error);
      throw new DeviceStorageError('Ошибка парсинга JSON из DeviceStorage');
    }
  },

  async clear(keys: string[]): Promise<void> {
    await Promise.all(keys.map((key) => this.removeItem(key)));
  },

  isCloudStorageAvailable(): boolean {
    return isCloudStorageAvailableInternal();
  },
};

export default DeviceStorage;

export const __internal = {
  resetCloudStorageCache,
};

interface AsyncStorageAdapter {
  get: (key: string) => Promise<string | null>;
  set: (key: string, value: string) => Promise<void>;
  delete: (key: string) => Promise<void>;
}

const asyncAdapter: AsyncStorageAdapter = {
  get: (key) => DeviceStorage.getItem(key),
  set: (key, value) => DeviceStorage.setItem(key, value),
  delete: (key) => DeviceStorage.removeItem(key),
};

export async function withDeviceStorage<T>(
  callback: (storage: AsyncStorageAdapter) => Promise<T>,
): Promise<T> {
  return callback(asyncAdapter);
}

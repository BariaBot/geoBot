const ONE_MB = 1024 * 1024;

export class DeviceStorageError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'DeviceStorageError';
  }
}

interface CloudStorageCallback {
  (error?: Error | null, result?: string | boolean): void;
}

interface CloudStorage {
  setItem(key: string, value: string, callback: CloudStorageCallback): void;
  getItem(key: string, callback: CloudStorageCallback): void;
  removeItem(key: string, callback: CloudStorageCallback): void;
}

const getCloudStorage = (): CloudStorage | undefined => {
  const telegram = (window as any)?.Telegram;
  const { WebApp } = telegram ?? {};
  return WebApp?.CloudStorage as CloudStorage | undefined;
};

const promisify = <T>(handler: (callback: CloudStorageCallback) => void): Promise<T> => (
  new Promise((resolve, reject) => {
    handler((error, result) => {
      if (error) {
        reject(error);
        return;
      }
      resolve(result as T);
    });
  })
);

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

const DeviceStorage = {
  async setItem(key: string, value: string): Promise<void> {
    validatePayloadSize(value);
    const storage = getCloudStorage();
    if (storage) {
      try {
        await promisify<void>((callback) => storage.setItem(key, value, callback));
        return;
      } catch (error) {
        logWarning('CloudStorage setItem failed', error);
        throw error instanceof Error ? error : new DeviceStorageError('Не удалось сохранить значение');
      }
    }

    try {
      localStorageAdapter().setItem(key, value);
    } catch (error) {
      logWarning('localStorage set failed', error);
      throw error instanceof Error ? error : new DeviceStorageError('localStorage set failed');
    }
  },

  async getItem(key: string): Promise<string | null> {
    const storage = getCloudStorage();
    if (storage) {
      try {
        const value = await promisify<string | null>((callback) => storage.getItem(key, callback));
        return value ?? null;
      } catch (error) {
        logWarning('CloudStorage getItem failed', error);
        throw error instanceof Error ? error : new DeviceStorageError('Не удалось получить значение');
      }
    }

    try {
      return localStorageAdapter().getItem(key);
    } catch (error) {
      logWarning('localStorage get failed', error);
      throw error instanceof Error ? error : new DeviceStorageError('localStorage get failed');
    }
  },

  async removeItem(key: string): Promise<void> {
    const storage = getCloudStorage();
    if (storage) {
      try {
        await promisify<void>((callback) => storage.removeItem(key, callback));
        return;
      } catch (error) {
        logWarning('CloudStorage removeItem failed', error);
        throw error instanceof Error ? error : new DeviceStorageError('Не удалось удалить значение');
      }
    }

    try {
      localStorageAdapter().removeItem(key);
    } catch (error) {
      logWarning('localStorage delete failed', error);
      throw error instanceof Error ? error : new DeviceStorageError('localStorage delete failed');
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

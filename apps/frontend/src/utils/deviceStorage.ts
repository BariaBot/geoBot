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
  const tg = (window as any)?.Telegram?.WebApp;
  return tg?.CloudStorage as CloudStorage | undefined;
};

const withCloudStorage = <T>(fn: (storage: CloudStorage) => Promise<T>): Promise<T> => {
  const storage = getCloudStorage();
  if (!storage) {
    return Promise.reject(new DeviceStorageError('CloudStorage API недоступен'));
  }
  return fn(storage);
};

const promisify = <T>(handler: (callback: CloudStorageCallback) => void): Promise<T> =>
  new Promise((resolve, reject) => {
    handler((error, result) => {
      if (error) {
        reject(error);
        return;
      }
      resolve(result as T);
    });
  });

const validatePayloadSize = (value: string) => {
  const length = new TextEncoder().encode(value).length;
  if (length > ONE_MB) {
    throw new DeviceStorageError('Размер данных превышает 1 MB лимит CloudStorage');
  }
};

const local = () => {
  if (typeof window === 'undefined' || !('localStorage' in window)) {
    throw new DeviceStorageError('localStorage недоступен');
  }
  return window.localStorage;
};

export const DeviceStorage = {
  async setItem(key: string, value: string): Promise<void> {
    validatePayloadSize(value);
    if (getCloudStorage()) {
      await withCloudStorage<void>((storage) =>
        promisify<void>((callback) => storage.setItem(key, value, callback)),
      );
      return;
    }
    local().setItem(key, value);
  },

  async getItem(key: string): Promise<string | null> {
    if (getCloudStorage()) {
      try {
        const value = await withCloudStorage<string | null>((storage) =>
          promisify<string | null>((callback) => storage.getItem(key, callback)),
        );
        return value ?? null;
      } catch (error) {
        throw error instanceof Error ? error : new DeviceStorageError('Не удалось получить значение');
      }
    }
    return local().getItem(key);
  },

  async removeItem(key: string): Promise<void> {
    if (getCloudStorage()) {
      await withCloudStorage<void>((storage) =>
        promisify<void>((callback) => storage.removeItem(key, callback)),
      );
      return;
    }
    local().removeItem(key);
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
    } catch {
      throw new DeviceStorageError('Ошибка парсинга JSON из DeviceStorage');
    }
  },

  async clear(keys: string[]): Promise<void> {
    await Promise.all(keys.map((key) => this.removeItem(key)));
  },

  isCloudStorageAvailable(): boolean {
    return Boolean(getCloudStorage());
  },
};

export default DeviceStorage;
